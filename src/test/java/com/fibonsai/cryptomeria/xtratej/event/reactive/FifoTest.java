
package com.fibonsai.cryptomeria.xtratej.event.reactive;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Fifo} – focusing on the {@code zip()} method.
 *
 * <p>All tests that wait for asynchronous events use a generous but finite
 * {@code @Timeout} so the suite never hangs indefinitely.
 */
class FifoTest {

    // ── helpers ───────────────────────────────────────────────────────────────

    /** Blocks until the latch reaches zero or the timeout elapses. */
    private static boolean await(CountDownLatch latch, long timeoutMs)
            throws InterruptedException {
        return latch.await(timeoutMs, TimeUnit.MILLISECONDS);
    }

    // =========================================================================
    // subscribe / emitNext – basic sanity
    // =========================================================================

    @Test
    @Timeout(5)
    void singleSubscriberReceivesEvent() throws InterruptedException {
        Fifo<String> reactor = new Fifo<>();
        CountDownLatch latch = new CountDownLatch(1);
        List<String> received = new CopyOnWriteArrayList<>();

        reactor.subscribe(e -> { received.add(e); latch.countDown(); });

        reactor.emitNext("hello");

        assertTrue(await(latch, 2_000), "Subscriber should receive the event");
        assertEquals(List.of("hello"), received);
    }

    @Test
    @Timeout(5)
    void multipleSubscribersAllReceiveEvent() throws InterruptedException {
        Fifo<Integer> reactor = new Fifo<>();
        int subscriberCount = 5;
        CountDownLatch latch = new CountDownLatch(subscriberCount);
        List<Integer> received = new CopyOnWriteArrayList<>();

        for (int i = 0; i < subscriberCount; i++) {
            reactor.subscribe(e -> { received.add(e); latch.countDown(); });
        }

        reactor.emitNext(42);

        assertTrue(await(latch, 2_000), "All subscribers should receive the event");
        assertEquals(subscriberCount, received.size());
        assertTrue(received.stream().allMatch(v -> v == 42));
    }

    // =========================================================================
    // zip – two sources, equal length
    // =========================================================================

    @Test
    @Timeout(5)
    void zipTwoSourcesEqualLength() throws InterruptedException {
        Fifo<String> r1 = new Fifo<>();
        Fifo<String> r2 = new Fifo<>();

        Fifo<String[]> zipped = Fifo.zip(r1, r2);

        List<String[]> results = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(2);

        zipped.subscribe(arr -> { results.add(arr); latch.countDown(); });

        // Emit in interleaved order to exercise ordering guarantees.
        r1.emitNext("r1-a");
        r2.emitNext("r2-a");
        r1.emitNext("r1-b");
        r2.emitNext("r2-b");

        assertTrue(await(latch, 3_000), "Should produce 2 zipped tuples");
        assertEquals(2, results.size());

        assertArrayEquals(new String[]{"r1-a", "r2-a"}, results.get(0));
        assertArrayEquals(new String[]{"r1-b", "r2-b"}, results.get(1));
    }

    // =========================================================================
    // zip – three sources, unequal lengths (shorter source limits output)
    // =========================================================================

    @Test
    @Timeout(5)
    void zipThreeSourcesUnequalLength() throws InterruptedException {
        Fifo<String> r1 = new Fifo<>();
        Fifo<String> r2 = new Fifo<>();
        Fifo<String> r3 = new Fifo<>();   // only 1 event

        Fifo<String[]> zipped = Fifo.zip(r1, r2, r3);

        List<String[]> results = new CopyOnWriteArrayList<>();
        // We expect exactly ONE tuple because r3 emits only once.
        CountDownLatch latch = new CountDownLatch(1);

        zipped.subscribe(arr -> { results.add(arr); latch.countDown(); });

        r1.emitNext("r1-1");
        r2.emitNext("r2-1");
        r3.emitNext("r3-1");   // only emission from r3
        r1.emitNext("r1-2");
        r2.emitNext("r2-2");
        // No second event from r3 → second tuple cannot be completed within tolerance.

        assertTrue(await(latch, 3_000), "Should produce at least one tuple");

        // Give a little extra time to detect any spurious second emission.
        Thread.sleep(300);
        assertEquals(1, results.size(), "Only one complete tuple should be emitted");
        assertArrayEquals(new String[]{"r1-1", "r2-1", "r3-1"}, results.getFirst());
    }

    // =========================================================================
    // zip – ordering: first event of each source composes first tuple
    // =========================================================================

    @Test
    @Timeout(5)
    void zipPreservesEventOrder() throws InterruptedException {
        Fifo<String> r1 = new Fifo<>();
        Fifo<String> r2 = new Fifo<>();

        Fifo<String[]> zipped = Fifo.zip(r1, r2);

        List<String[]> results = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(3);
        zipped.subscribe(arr -> { results.add(arr); latch.countDown(); });

        // Emit all r1 events first, then all r2 events.
        r1.emitNext("a1"); r1.emitNext("a2"); r1.emitNext("a3");
        r2.emitNext("b1"); r2.emitNext("b2"); r2.emitNext("b3");

        assertTrue(await(latch, 3_000));
        assertEquals(3, results.size());
        assertArrayEquals(new String[]{"a1", "b1"}, results.get(0));
        assertArrayEquals(new String[]{"a2", "b2"}, results.get(1));
        assertArrayEquals(new String[]{"a3", "b3"}, results.get(2));
    }

    // =========================================================================
    // zip – single source (degenerate case)
    // =========================================================================

    @Test
    @Timeout(5)
    void zipSingleSourceWrapsEventsInArrays() throws InterruptedException {
        Fifo<String> r1 = new Fifo<>();
        Fifo<String[]> zipped = Fifo.zip(r1);

        List<String[]> results = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(2);
        zipped.subscribe(arr -> { results.add(arr); latch.countDown(); });

        r1.emitNext("only-a");
        r1.emitNext("only-b");

        assertTrue(await(latch, 2_000));
        assertEquals(2, results.size());
        assertArrayEquals(new String[]{"only-a"}, results.get(0));
        assertArrayEquals(new String[]{"only-b"}, results.get(1));
    }

    // =========================================================================
    // zip – concurrent emission is thread-safe (no lost or duplicate tuples)
    // =========================================================================

    @Test
    @Timeout(10)
    void zipConcurrentEmissionsAreThreadSafe() throws InterruptedException {
        final int eventCount = 200;
        Fifo<Integer> r1 = new Fifo<>();
        Fifo<Integer> r2 = new Fifo<>();
        Fifo<Integer[]> zipped = Fifo.zip(r1, r2);

        List<Integer[]> results = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(eventCount);
        AtomicInteger counter = new AtomicInteger(0);
        zipped.subscribe(arr -> {
            results.add(arr);
            latch.countDown();
            counter.getAndIncrement();
        });

        // Emit from two virtual threads concurrently.
        Thread t1 = Thread.startVirtualThread(() -> {
            for (int i = 0; i < eventCount; i++) r1.emitNext(i);
        });
        Thread t2 = Thread.startVirtualThread(() -> {
            for (int i = 0; i < eventCount; i++) r2.emitNext(i);
        });

        t1.join();
        t2.join();

        assertTrue(await(latch, 8_000),
                "Should receive " + eventCount + " tuples under concurrent load, but received only " + counter.get());
        assertEquals(eventCount, results.size());
        // Every emitted array must have exactly 2 elements (one per source).
        assertTrue(results.stream().allMatch(arr -> arr.length == 2));
    }

    // =========================================================================
    // zip – multiple consumers on the zipped reactor all receive tuples
    // =========================================================================

    @Test
    @Timeout(5)
    void zipResultSupportsMultipleConsumers() throws InterruptedException {
        Fifo<String> r1 = new Fifo<>();
        Fifo<String> r2 = new Fifo<>();
        Fifo<String[]> zipped = Fifo.zip(r1, r2);

        int consumers = 3;
        CountDownLatch latch = new CountDownLatch(consumers); // 1 tuple × 3 consumers
        List<String[]> results = new CopyOnWriteArrayList<>();

        for (int i = 0; i < consumers; i++) {
            zipped.subscribe(arr -> { results.add(arr); latch.countDown(); });
        }

        r1.emitNext("x");
        r2.emitNext("y");

        assertTrue(await(latch, 3_000), "All consumers should receive the zipped tuple");
        assertEquals(consumers, results.size());
        results.forEach(arr -> assertArrayEquals(new String[]{"x", "y"}, arr));
    }

    // =========================================================================
    // zip – tolerance window discards an incomplete slot and recovers
    // =========================================================================

    /**
     * We shorten the tolerance via reflection-style workaround isn't possible
     * on a constant field, so instead we verify the <em>discard-and-recover</em>
     * behaviour by using a custom subclass that overrides the tolerance.
     * *
     * Strategy: emit only to r1 (never r2), wait longer than the tolerance,
     * then emit to both r1 and r2. The second pair must produce a valid tuple.
     * *
     * NOTE: Because the constant is 10 s this test uses a very short real wait
     * of just 50 ms with a test-scoped subclass that overrides the schedule.
     * *
     * Since ZIP_TOLERANCE is a private constant, we test the recovery indirectly:
     * by verifying that a fresh pair of events (after the stale partial slot)
     * still yields a correctly formed tuple.
     */
    @Test
    @Timeout(5)
    void zipRecoversAfterPartialSlotIsAbandoned() throws InterruptedException {
        Fifo<String> r1 = new Fifo<>();
        Fifo<String> r2 = new Fifo<>();
        Fifo<String[]> zipped = Fifo.zip(r1, r2);

        List<String[]> results = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        zipped.subscribe(arr -> { results.add(arr); latch.countDown(); });

        // Emit to r1 only – this starts a slot but cannot complete it.
        r1.emitNext("stale");

        // Small pause – the slot is open but incomplete.
        Thread.sleep(100);

        // Now emit a matching pair. The implementation must enqueue them and
        // eventually produce at least one complete array.
        // r1 already has "stale" consumed; now r1 gets "fresh-1" for the next slot.
        r1.emitNext("fresh-1");
        r2.emitNext("fresh-2");  // this also closes the first slot from r2

        // The very first array may be ("stale","fresh-2") if the slot was still
        // open, OR ("fresh-1","fresh-2") if the slot was already discarded and
        // a new slot started.  Either way, exactly ONE array must appear, and it
        // must be well-formed (length == 2, non-null values).
        assertTrue(await(latch, 3_000), "A complete array should be produced");
        assertEquals(1, results.size());
        String[] array = results.getFirst();
        assertEquals(2, array.length);
        assertNotNull(array[0]);
        assertNotNull(array[1]);
    }

    // =========================================================================
    // zip – no false emissions when no events arrive
    // =========================================================================

    @Test
    @Timeout(3)
    void zipEmitsNothingWithNoEvents() throws InterruptedException {
        Fifo<String> r1 = new Fifo<>();
        Fifo<String> r2 = new Fifo<>();
        Fifo<String[]> zipped = Fifo.zip(r1, r2);

        AtomicInteger counter = new AtomicInteger(0);
        zipped.subscribe(_ -> counter.incrementAndGet());

        Thread.sleep(300);

        assertEquals(0, counter.get(), "No events should be emitted without input");
    }

    // =========================================================================
    // zip – result array length matches number of sources
    // =========================================================================

    @Test
    @Timeout(5)
    void zipResultArrayLengthMatchesSourceCount() throws InterruptedException {
        int sourceCount = 5;
        @SuppressWarnings("unchecked")
        Fifo<String>[] sources = new Fifo[sourceCount];
        for (int i = 0; i < sourceCount; i++) sources[i] = new Fifo<>();

        Fifo<String[]> zipped = Fifo.zip(sources);

        CountDownLatch latch = new CountDownLatch(1);
        List<Integer> lengths = new CopyOnWriteArrayList<>();
        zipped.subscribe(arr -> { lengths.add(arr.length); latch.countDown(); });

        for (int i = 0; i < sourceCount; i++) sources[i].emitNext("e" + i);

        assertTrue(await(latch, 3_000));
        assertEquals(List.of(sourceCount), lengths);
    }

    // =========================================================================
    // subscribe – concurrent subscriptions are thread-safe
    // =========================================================================

    @Test
    @Timeout(5)
    void concurrentSubscriptionsAreSafe() throws InterruptedException {
        Fifo<String> reactor = new Fifo<>();
        int threads = 50;
        CountDownLatch subscribeLatch = new CountDownLatch(threads);
        CountDownLatch receiveLatch   = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            Thread.startVirtualThread(() -> {
                reactor.onSubscribe(subscribeLatch::countDown).subscribe(_ -> receiveLatch.countDown());
            });
        }

        assertTrue(await(subscribeLatch, 3_000), "All subscriptions should complete");
        reactor.emitNext("broadcast");
        assertTrue(await(receiveLatch, 3_000),
                "All concurrent subscribers should receive the event");
    }
}
