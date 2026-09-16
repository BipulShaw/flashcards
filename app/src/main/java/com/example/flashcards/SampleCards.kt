package com.example.flashcards

/** The deck. Shuffled once per launch by the feed. */
val sampleCards =
  listOf(
    Card(
      term = "Idempotency",
      gist = "Running it twice leaves the world exactly as running it once did.",
      details =
        "An idempotent operation is safe to retry, which is why HTTP defines PUT and DELETE that way and POST not. " +
          "In practice you make a write idempotent by attaching a client-generated key: a request ID the server records " +
          "the first time and recognizes on every duplicate. Without one, a network timeout leaves the caller unable to " +
          "tell whether to retry or to charge someone twice.",
    ),
    Card(
      term = "CAP Theorem",
      gist = "When the network splits, you choose consistency or availability — never both.",
      details =
        "The partition is not something you opt into; it is a fact of networks, so the only real choice is how to behave " +
          "during one. A CP system refuses requests it cannot confirm, while an AP system answers anyway and reconciles " +
          "afterwards. The theorem says nothing about normal operation, which is why \"we're an AP system\" usually " +
          "describes a handful of failure paths rather than a whole design.",
    ),
    Card(
      term = "Backpressure",
      gist = "A slow consumer's way of telling a fast producer to ease off.",
      details =
        "Without it, work piles up in an unbounded queue until latency balloons and the process dies of memory exhaustion. " +
          "Real systems propagate the signal backwards — TCP shrinks its window, a reactive stream requests n items, a " +
          "worker blocks on a bounded queue — until the original producer slows down. The alternative is load shedding: " +
          "dropping work deliberately, which beats collapsing under it.",
    ),
    Card(
      term = "Eventual Consistency",
      gist = "Every replica agrees, given a quiet moment and no new writes.",
      details =
        "It is a promise of convergence with no deadline attached. Users see the gap as a profile that still shows the old " +
          "name right after they saved the new one, which is why systems bolt on session guarantees like read-your-writes. " +
          "Conflict resolution is the hard part: last-write-wins quietly discards somebody's data, so CRDTs and version " +
          "vectors exist to merge instead of pick.",
    ),
    Card(
      term = "Circuit Breaker",
      gist = "Stop calling the service that is already failing.",
      details =
        "After a threshold of failures the breaker trips and subsequent calls fail instantly instead of waiting out a " +
          "timeout. That frees the caller's threads and gives the struggling dependency room to recover rather than being " +
          "hammered while it is down. After a cooldown the breaker half-opens, letting one trial request decide whether to " +
          "close again or keep failing fast.",
    ),
    Card(
      term = "Write-Ahead Log",
      gist = "Record what you are about to do, before you do it.",
      details =
        "The database appends the intended change to a durable log and flushes it to disk before touching the real data " +
          "pages. If the process dies mid-write, recovery replays the log to finish committed transactions and discards the " +
          "rest. It also turns scattered random writes into one sequential append, which is how a safety mechanism ends up " +
          "making the system faster.",
    ),
    Card(
      term = "Consistent Hashing",
      gist = "Add a server and only about 1/n of the keys have to move.",
      details =
        "Plain hash(key) % n remaps nearly every key when n changes, emptying the cache at exactly the moment you were " +
          "adding capacity to cope with load. Consistent hashing places servers and keys on the same ring, so a key belongs " +
          "to the next server clockwise and only one neighbour's share relocates. Virtual nodes — many ring positions per " +
          "physical server — keep the distribution from clumping.",
    ),
    Card(
      term = "Tail Latency",
      gist = "The p99 is the experience; the average is a comforting fiction.",
      details =
        "If one page makes a hundred backend calls, it waits on the slowest, so a one-in-a-hundred slow response becomes " +
          "the common case. Tails come from queueing, garbage collection and contention rather than from slow code, so " +
          "profiling the mean tells you nothing useful. Hedged requests — send a duplicate once the p95 has elapsed and take " +
          "whichever returns first — trim the tail for a few percent more load.",
    ),
    Card(
      term = "Race Condition",
      gist = "Correct code, wrong order, and only on the machine you cannot debug.",
      details =
        "Two threads interleave in a way neither author pictured, and count++ loses an increment because it was three " +
          "operations wearing the costume of one. They are hard to catch because the damaging interleaving is rare and " +
          "adding logging perturbs the timing enough to hide it. The fix is to make the ordering explicit — a lock, an " +
          "atomic, or a single owner for the data — not to hope the window stays narrow.",
    ),
    Card(
      term = "Bloom Filter",
      gist = "\"Definitely not here\" or \"probably here\", in a few bits per item.",
      details =
        "It hashes each item to k positions in a bit array and sets them; a lookup that finds any of those bits unset " +
          "proves the item was never added. False positives are possible, false negatives are not, which makes it a cheap " +
          "guard in front of an expensive lookup. Storage engines keep one per file to skip reading files that cannot hold " +
          "the key, trading a tunable error rate for a great deal of avoided I/O.",
    ),
  )
