package com.example.flashcards

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.text.Html
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import org.json.JSONObject

/** What a link preview shows. Only [url] and [site] are always known. */
class LinkPreview(
  val url: String,
  val site: String,
  val title: String?,
  val summary: String?,
  val image: ImageBitmap?,
  /** The site's icon standing in for a picture of the page: shown small and whole, not cropped. */
  val imageIsIcon: Boolean,
)

/**
 * Link previews: a page's title, summary and picture, read from the Open Graph tags (or plain
 * HTML) at the start of the page. Uses the platform's HTTP client, which keeps no cookies, and
 * requests nothing but the link itself and its picture. Previews are kept in memory and in the
 * cache directory (which Android may clear), so each link is fetched about once.
 */
object LinkPreviews {
  private const val MaxPageBytes = 512 * 1024
  private const val MaxImageBytes = 6 * 1024 * 1024
  /** Pictures are shrunk until their shorter side is about this many pixels. */
  private const val ThumbnailPx = 240
  private const val TimeoutMillis = 6_000
  /** After a failed fetch, the same link waits this long before it's tried again. */
  private const val RetryAfterMillis = 30_000L
  private const val StaleAfterMillis = 30L * 24 * 60 * 60 * 1000
  // Sites tailor pages to the browser asking; a browser-like identity gets the page a person sees.
  private const val UserAgent =
    "Mozilla/5.0 (Linux; Android) AppleWebKit/537.36 (KHTML, like Gecko) Mobile Safari/537.36"

  private val memory = LruCache<String, LinkPreview>(32)
  private val inFlight = ConcurrentHashMap<String, Deferred<LinkPreview?>>()
  private val failedAt = ConcurrentHashMap<String, Long>()
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  /** A preview already in memory, to draw without waiting. */
  fun cached(url: String): LinkPreview? = memory.get(url)

  /** The preview for [url], from memory, disk or the page itself; null when the page can't be reached. */
  suspend fun load(context: Context, url: String): LinkPreview? {
    memory.get(url)?.let { return it }
    failedAt[url]?.let { if (System.currentTimeMillis() - it < RetryAfterMillis) return null }
    val appContext = context.applicationContext
    // One fetch per link, however many cards on screen are waiting for it. The fetch runs in its
    // own scope, so a card scrolled away mid-fetch still leaves the preview cached for next time.
    val request =
      inFlight.computeIfAbsent(url) { scope.async { fromDisk(appContext, url) ?: fromPage(appContext, url) } }
    val preview = request.await()
    inFlight.remove(url, request)
    if (preview != null) memory.put(url, preview) else failedAt[url] = System.currentTimeMillis()
    return preview
  }

  /** The host of [url] without "www.", for naming a site that says nothing about itself. */
  fun hostOf(url: String): String =
    runCatching { URL(url).host }.getOrNull()?.removePrefix("www.")?.takeIf { it.isNotEmpty() } ?: displayUrl(url)

  private fun fromPage(context: Context, url: String): LinkPreview? {
    val page = fetch(httpsOf(url), accept = "text/html,application/xhtml+xml,image/*;q=0.8") ?: return null
    val picture: Bitmap?
    val preview: LinkPreview
    if (page.type.startsWith("image/")) {
      // The link is itself a picture.
      picture = decodeThumbnail(page.body)
      preview = LinkPreview(url, hostOf(page.finalUrl), null, null, picture?.asImageBitmap(), imageIsIcon = false)
    } else {
      val html = page.text()
      val meta = metaTags(html)
      val pictureUrl =
        meta["og:image"] ?: meta["og:image:url"] ?: meta["twitter:image"] ?: meta["twitter:image:src"] ?: meta["image"]
      picture =
        resolve(page.finalUrl, pictureUrl ?: iconHref(html) ?: "/favicon.ico")
          ?.let { fetch(it, accept = "image/*") }
          ?.takeIf { it.type.startsWith("image/") }
          ?.let { decodeThumbnail(it.body) }
      preview =
        LinkPreview(
          url = url,
          site = clean(meta["og:site_name"]) ?: hostOf(page.finalUrl),
          title = clean(meta["og:title"] ?: meta["twitter:title"] ?: titleTag(html)),
          summary =
            clean(meta["og:description"] ?: meta["twitter:description"] ?: meta["description"])?.let {
              if (it.length > 240) it.take(240).trimEnd() + "…" else it
            },
          image = picture?.asImageBitmap(),
          imageIsIcon = pictureUrl == null,
        )
    }
    toDisk(context, preview, picture)
    return preview
  }

  private class Fetched(val finalUrl: String, val type: String, val body: ByteArray) {
    /** The page as text, in the charset its headers or its own meta tag declare, else UTF-8. */
    fun text(): String {
      val declared =
        charsetIn(type) ?: charsetIn(String(body, 0, min(body.size, 4096), Charsets.ISO_8859_1).lowercase())
      val charset = declared?.let { runCatching { Charset.forName(it) }.getOrNull() } ?: Charsets.UTF_8
      return String(body, charset)
    }

    private fun charsetIn(text: String): String? = CharsetDeclaration.find(text)?.groupValues?.get(1)
  }

  private fun fetch(address: String, accept: String): Fetched? =
    runCatching {
        val connection = URL(address).openConnection() as HttpURLConnection
        try {
          connection.connectTimeout = TimeoutMillis
          connection.readTimeout = TimeoutMillis
          connection.instanceFollowRedirects = true
          connection.setRequestProperty("User-Agent", UserAgent)
          connection.setRequestProperty("Accept", accept)
          if (connection.responseCode !in 200..299) return@runCatching null
          val type = connection.contentType.orEmpty().lowercase()
          val isImage = type.startsWith("image/")
          val body =
            connection.inputStream.use { it.readUpTo(if (isImage) MaxImageBytes else MaxPageBytes, isPage = !isImage) }
              ?: return@runCatching null
          Fetched(connection.url.toString(), type, body)
        } finally {
          connection.disconnect()
        }
      }
      .getOrNull()

  /**
   * Up to [limit] bytes. A page stops early once its head has ended, since that's where the preview
   * tags are, and is cut at the limit; an image over the limit is skipped (null).
   */
  private fun InputStream.readUpTo(limit: Int, isPage: Boolean): ByteArray? {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(16 * 1024)
    while (true) {
      val count = read(buffer)
      if (count < 0) break
      out.write(buffer, 0, count)
      if (out.size() >= limit) return if (isPage) out.toByteArray().copyOf(limit) else null
      if (isPage && String(buffer, 0, count, Charsets.ISO_8859_1).contains("</head>", ignoreCase = true)) break
    }
    return out.toByteArray()
  }

  private val MetaTag = Regex("<meta\\b[^>]*>", RegexOption.IGNORE_CASE)
  private val LinkTag = Regex("<link\\b[^>]*>", RegexOption.IGNORE_CASE)
  private val TitleTag = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
  private val Attribute = Regex("""([\w:-]+)\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s"'>]+))""")
  private val CharsetDeclaration = Regex("""charset\s*=\s*["']?([\w-]+)""", RegexOption.IGNORE_CASE)
  private val Whitespace = Regex("\\s+")

  private fun attributes(tag: String): Map<String, String> =
    Attribute.findAll(tag).associate { match ->
      match.groupValues[1].lowercase() to (match.groups[2] ?: match.groups[3] ?: match.groups[4])!!.value
    }

  /** Open Graph, Twitter and plain meta tags by name; the first of each wins. */
  private fun metaTags(html: String): Map<String, String> {
    val found = HashMap<String, String>()
    for (tag in MetaTag.findAll(html)) {
      val attributes = attributes(tag.value)
      val name = (attributes["property"] ?: attributes["name"] ?: attributes["itemprop"])?.lowercase() ?: continue
      val content = attributes["content"]?.takeIf { it.isNotBlank() } ?: continue
      found.putIfAbsent(name, content)
    }
    return found
  }

  /** The best icon the page names: a touch icon (large) if there is one, else its favicon. */
  private fun iconHref(html: String): String? {
    var favicon: String? = null
    for (tag in LinkTag.findAll(html)) {
      val attributes = attributes(tag.value)
      val rel = attributes["rel"]?.lowercase() ?: continue
      val href = attributes["href"]?.takeIf { it.isNotBlank() } ?: continue
      if ("apple-touch-icon" in rel) return href
      if (favicon == null && "icon" in rel.split(' ')) favicon = href
    }
    return favicon
  }

  private fun titleTag(html: String): String? = TitleTag.find(html)?.groupValues?.get(1)

  /** Entities decoded and whitespace collapsed; null if nothing is left. */
  private fun clean(text: String?): String? =
    text
      ?.let { Html.fromHtml(it, Html.FROM_HTML_MODE_LEGACY).toString() }
      ?.replace(Whitespace, " ")
      ?.trim()
      ?.takeIf { it.isNotEmpty() }

  /** Android blocks unencrypted HTTP, and nearly every site serves HTTPS as well. */
  private fun httpsOf(url: String): String =
    if (url.startsWith("http://", ignoreCase = true)) "https://" + url.substring("http://".length) else url

  private fun resolve(base: String, href: String): String? =
    runCatching { URL(URL(base), href).toString() }
      .getOrNull()
      ?.takeIf { it.startsWith("https://", ignoreCase = true) || it.startsWith("http://", ignoreCase = true) }
      ?.let(::httpsOf)

  private fun decodeThumbnail(bytes: ByteArray): Bitmap? =
    runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val shorter = min(bounds.outWidth, bounds.outHeight)
        if (shorter <= 0) return@runCatching null
        var sample = 1
        while (shorter / (sample * 2) >= ThumbnailPx) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return@runCatching null
        val scale = ThumbnailPx.toFloat() / min(bitmap.width, bitmap.height)
        if (scale >= 1f) bitmap
        else
          Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).roundToInt().coerceAtLeast(1),
            (bitmap.height * scale).roundToInt().coerceAtLeast(1),
            true,
          )
      }
      .getOrNull()

  private fun cacheDir(context: Context) = File(context.cacheDir, "link-previews")

  private fun keyFor(url: String): String =
    MessageDigest.getInstance("SHA-1").digest(url.toByteArray()).joinToString("") { "%02x".format(it) }

  private fun fromDisk(context: Context, url: String): LinkPreview? =
    runCatching {
        val key = keyFor(url)
        val info = File(cacheDir(context), "$key.json")
        if (!info.exists() || System.currentTimeMillis() - info.lastModified() > StaleAfterMillis) {
          return@runCatching null
        }
        val json = JSONObject(info.readText())
        val picture = File(cacheDir(context), "$key.img").takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }
        LinkPreview(
          url = url,
          site = json.getString("site"),
          title = json.optString("title").ifEmpty { null },
          summary = json.optString("summary").ifEmpty { null },
          image = picture?.asImageBitmap(),
          imageIsIcon = json.optBoolean("imageIsIcon"),
        )
      }
      .getOrNull()

  private fun toDisk(context: Context, preview: LinkPreview, picture: Bitmap?) {
    runCatching {
      val dir = cacheDir(context).apply { mkdirs() }
      val key = keyFor(preview.url)
      // The picture first: the details file appearing is what marks the entry complete.
      picture?.let { bitmap ->
        File(dir, "$key.img").outputStream().use { out ->
          if (bitmap.hasAlpha()) bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
          else bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
      }
      val json =
        JSONObject()
          .put("site", preview.site)
          .put("title", preview.title.orEmpty())
          .put("summary", preview.summary.orEmpty())
          .put("imageIsIcon", preview.imageIsIcon)
      File(dir, "$key.json").writeText(json.toString())
    }
  }
}
