# Assignment 1 — Speech-to-Text Web Application

A Spring Boot web application that records audio in the browser, transcribes it
via OpenAI's `gpt-4o-mini-transcribe` API, and exposes administrative endpoints
for uptime, usage statistics, and graceful shutdown.

## Architecture

The codebase is organized by responsibility, not by layer alone:

- `controller` — HTTP-facing endpoints (`TranscriptionController`, `HelloController`)
- `service` — business logic, including the `SpeechToTextClient` interface and its
  real implementation `OpenAiSpeechToTextClient`
- `admin` — server lifecycle and statistics (`ServerStats`, `TokenStats`,
  `ShutdownService`, `AdminController`)
- `dto` — plain data-holding records shaping request/response JSON
- `exception` — centralized error handling (`GlobalExceptionHandler`)

`TranscriptionService` depends on the `SpeechToTextClient` **interface**, not the
concrete OpenAI implementation directly. This lets tests substitute a fake, instant
implementation without touching production code or making real network calls.

## Concurrency approach

The assignment requires handling 200+ simultaneous blocking HTTP requests, each of
which spends most of its time waiting on OpenAI's network response rather than doing
CPU work. Two broad approaches exist: rewrite the whole call chain as fully
asynchronous/reactive code (e.g. `WebClient` + reactive types), or use Java's virtual
threads to keep the existing simple, blocking-style code but run it far more cheaply.

This project uses **virtual threads** (`spring.threads.virtual.enabled=true`),
enabled by Java 21+. A traditional thread maps directly to a limited OS resource;
Tomcat's default thread pool caps at 200, meaning 200 simultaneous slow requests
could exhaust it entirely. A virtual thread detaches from its underlying OS thread
whenever it blocks (exactly what happens while waiting on OpenAI), freeing that OS
thread for other work. This was chosen over a full reactive rewrite because it
preserves simple, readable, synchronous-style code while still solving the actual
scalability problem — and it is explicitly Spring's own recommended approach for
this class of problem (many concurrent, I/O-bound, blocking-style operations).

This was verified directly, not just assumed: `Thread.currentThread()` was logged
during development and confirmed to show `VirtualThread[...]` rather than a
traditional pooled thread name.

Thread-safety for shared mutable state (`TokenStats`'s running token counters,
`ShutdownService`'s shutdown flag) uses `AtomicLong`/`AtomicBoolean` rather than
manual synchronization — both provide atomic read-modify-write operations without
the overhead or deadlock risk of explicit locks, appropriate for simple counters
and flags under high concurrency.

## Testing strategy

- `ConcurrentTranscriptionLoadTest` — fires 250 simultaneous requests at
  `/api/transcribe` using a stub `SpeechToTextClient` (avoiding real API calls),
  asserting all succeed and complete well within a reasonable time bound. This
  demonstrates the virtual threads configuration genuinely handles the assignment's
  200+ concurrent request requirement.
- `TokenStatsConcurrencyTest` — releases 500 threads simultaneously via
  `CountDownLatch` to hammer `TokenStats`'s counters at the exact same instant,
  asserting the final total is exactly correct. This demonstrates the `AtomicLong`
  choice genuinely prevents lost updates under real concurrent access.
- `TranscriptionControllerTest` — functional correctness tests (successful
  transcription, empty file, upstream rejection) using a stub client, independent
  of any real OpenAI account or network access.
- `AdminControllerTest` — functional correctness tests for uptime, stats, and
  shutdown (including the 202-then-409 conflict behavior), with the shutdown
  service's actual JVM-terminating step overridden to a no-op for safe testing.

Using stubs throughout the test suite means the tests are fast, free, and
repeatable — they never depend on OpenAI's availability, latency, or cost.

## Security

The OpenAI API key is read exclusively from the `OPENAI_API_KEY` environment
variable via Spring's `@Value` injection, and is never hardcoded, logged, printed,
or returned in any API response. This was verified by searching the entire git
history and working tree for the `sk-` prefix common to all OpenAI keys, returning
no matches.

## Configuration

`application-local.properties` and `application-titan.properties` provide
environment-specific logging verbosity (DEBUG locally for development visibility,
WARN/INFO on TITAN for a quieter deployed environment), selected via
`spring.profiles.active`. This default can be overridden by the
`SPRING_PROFILES_ACTIVE` environment variable without any code or file changes,
satisfying the requirement to manage local/deployment differences through
configuration rather than commented-out code.

## Known limitations

- Token usage statistics (`TokenStats`) are held in memory and reset on restart,
  matching the YAML spec's stated behavior ("Counters reset when the server
  process restarts").
- The shutdown endpoint has no authentication, as explicitly permitted by the
  YAML spec ("Authentication is intentionally not specified").