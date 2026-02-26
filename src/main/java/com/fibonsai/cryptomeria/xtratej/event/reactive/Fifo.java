/*
 *  Copyright (c) 2026 fibonsai.com
 *  All rights reserved.
 *
 *  This source is subject to the Apache License, Version 2.0.
 *  Please see the LICENSE file for more information.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.fibonsai.cryptomeria.xtratej.event.reactive;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

public class Fifo<T> {

    private static final Logger log = LoggerFactory.getLogger(Fifo.class);

    private static final Duration DEFAULT_ZIP_TOLERANCE = Duration.ofSeconds(10);

    private final List<Consumer<T>> consumers = new ArrayList<>();
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    protected final ReentrantReadWriteLock.ReadLock  readLock  = readWriteLock.readLock();
    protected final ReentrantReadWriteLock.WriteLock writeLock = readWriteLock.writeLock();
    private Runnable onSubscribe = () -> {};

    public Fifo<T> onSubscribe(Runnable onSubscribe) {
        this.onSubscribe = onSubscribe;
        return this;
    }

    public void subscribe(Consumer<T> consumer) {
        writeLock.lock();
        try {
            consumers.add(consumer);
            onSubscribe.run();
        } finally {
            writeLock.unlock();
        }
    }

    public void emitNext(T event) {
        readLock.lock();
        try {
            CountDownLatch latch = new CountDownLatch(consumers.size());
            for (var consumer : consumers) {
                Thread.startVirtualThread(() -> {
                    consumer.accept(event);
                    latch.countDown();
                });
            }
            //noinspection ResultOfMethodCallIgnored
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        } finally {
            readLock.unlock();
        }
    }

    public static void empty() {
        new Fifo<Object>() {
            @Override
            public void subscribe(Consumer<Object> consumer) {
                if (log.isDebugEnabled()) {
                    log.debug("subscribing EMPTY {}", this.getClass().getSimpleName());
                }
            }

            @Override
            public void emitNext(Object event) {
                if (log.isDebugEnabled()) {
                    log.debug("emit object to EMPTY {}", this.getClass().getSimpleName());
                }
            }
        };
    }

    /**
     * Combines multiple {@code RealTimeReactor<T>} sources into a single
     * {@code RealTimeReactor<T[]>} that emits arrays of one event per source,
     * in source-declaration order.
     *
     * <p>Rules:
     * <ul>
     *   <li>A result array is only published once every source has contributed
     *       exactly one event to the current "slot".</li>
     *   <li>Events must all arrive within {@link #DEFAULT_ZIP_TOLERANCE}. If the window
     *       expires before the slot is complete, the partial slot is discarded
     *       and a fresh slot begins.</li>
     *   <li>Each source may have multiple events queued independently; they are
     *       consumed one-at-a-time, slot by slot.</li>
     * </ul>
     */
    @SafeVarargs
    public static <T> Fifo<T[]> zip(Fifo<T>... fifos) {
        return zip(DEFAULT_ZIP_TOLERANCE, fifos);
    }

    /**
     * Combines multiple {@code RealTimeReactor<T>} sources into a single
     * {@code RealTimeReactor<T[]>} that emits arrays of one event per source,
     * in source-declaration order.
     *
     * <p>Rules:
     * <ul>
     *   <li>A result array is only published once every source has contributed
     *       exactly one event to the current "slot".</li>
     *   <li>Events must all arrive within delayToleration. If the window
     *       expires before the slot is complete, the partial slot is discarded
     *       and a fresh slot begins.</li>
     *   <li>Each source may have multiple events queued independently; they are
     *       consumed one-at-a-time, slot by slot.</li>
     * </ul>
     */
    @SafeVarargs
    public static <T> Fifo<T[]> zip(Duration delayToleration, Fifo<T>... fifos) {

        final int n = fifos.length;
        final Fifo<T[]> result = new Fifo<>();

        // Per-source FIFO queues – populated by each source's virtual-thread callbacks.
        @SuppressWarnings("unchecked")
        final ConcurrentLinkedQueue<T>[] queues = new ConcurrentLinkedQueue[n];
        for (int i = 0; i < n; i++) {
            queues[i] = new ConcurrentLinkedQueue<>();
        }

        // Shared scheduler used to enforce the tolerance window.
        final ScheduledExecutorService scheduler =
                Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread t = Thread.ofVirtual().unstarted(r);
                    t.setDaemon(true);
                    return t;
                });

        // Coordinator object – all slot state lives here.
        final ZipCoordinator<T> coordinator =
                new ZipCoordinator<>(n, queues, result, scheduler, delayToleration);

        // Subscribe to every source reactor.
        for (int i = 0; i < n; i++) {
            final int sourceIndex = i;
            fifos[sourceIndex].subscribe(event -> {
                queues[sourceIndex].offer(event);
                coordinator.tryAdvance();
            });
        }

        return result;
    }

    /**
     * Manages the lifecycle of zip slots in a thread-safe manner,
     */
    static final class ZipCoordinator<Z> {

        private final int n;
        private final ConcurrentLinkedQueue<Z>[] queues;
        private final Fifo<Z[]> downstream;
        private final ScheduledExecutorService scheduler;
        private final Duration delayToleration;
        private final ExecutorService dispatchExecutor;

        // All fields below are guarded by 'slotLock'.
        private final ReentrantLock slotLock = new ReentrantLock();

        private List<@Nullable Z> currentSlot = List.of();  // partially-filled slot (length == n)
        private boolean[] slotFilled = new boolean[0];       // which indices have been filled
        private int filledCount;
        private boolean slotActive;
        @Nullable private ScheduledFuture<?> timeoutFuture = null;

        ZipCoordinator(int n,
                       ConcurrentLinkedQueue<Z>[] queues,
                       Fifo<Z[]> downstream,
                       ScheduledExecutorService scheduler,
                       Duration delayToleration) {
            this.n          = n;
            this.queues     = queues;
            this.downstream = downstream;
            this.scheduler  = scheduler;
            this.delayToleration = delayToleration;
            this.dispatchExecutor = Executors.newSingleThreadExecutor(Thread.ofVirtual().factory());
        }

        /**
         * Called whenever a new event lands in any queue.
         * Attempts to pull one item from every queue into the current slot.
         */
        void tryAdvance() {
            slotLock.lock();
            try {
                advanceUnderLock();
            } finally {
                slotLock.unlock();
            }
        }

        /** Initializes a fresh slot and arms the tolerance-window timer.
         *  Must be called while holding {@link #slotLock}. */
        private void openSlot() {
            currentSlot = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                currentSlot.add(null);
            }
            slotFilled  = new boolean[n];
            filledCount = 0;
            slotActive  = true;

            timeoutFuture = scheduler.schedule(() -> {
                slotLock.lock();
                try {
                    if (slotActive && filledCount < n) {
                        // Tolerance window expired – discard partial slot.
                        discardSlot();
                        // Immediately try to start the next slot if events are waiting.
                        advanceUnderLock();
                    }
                } finally {
                    slotLock.unlock();
                }
            }, delayToleration.toMillis(), TimeUnit.MILLISECONDS);
        }

        /**
         * Inner loop reused by the timeout callback and tryAdvance, already holding {@link #slotLock}.
         */
        private void advanceUnderLock() {
            // Drain as many complete slots as possible.
            while (true) {
                // Ensure we have an active slot.
                if (!slotActive) {
                    openSlot();
                }

                // Try to fill unfilled positions from their queues.
                for (int i = 0; i < n; i++) {
                    if (!slotFilled[i]) {
                        Z polled = queues[i].poll();

                        if (polled != null) {
                            currentSlot.set(i, polled);
                            slotFilled[i]  = true;
                            filledCount++;
                        }
                    }
                }

                // If slot is complete, publish and loop to open the next one.
                if (filledCount == n) {
                    publishSlot();
                } else {
                    // Slot still incomplete – leave it open (timeout already armed).
                    break;
                }
            }
        }

        /** Cancels the timer, emits the completed slot, then resets state.
         *  Must be called while holding {@link #slotLock}. */
        @SuppressWarnings("unchecked")
        private void publishSlot() {
            if (timeoutFuture != null) {
                timeoutFuture.cancel(false);
                timeoutFuture = null;
            }

            final Z typeToken = currentSlot.getFirst();
            if (typeToken == null) {
                log.warn("Cannot create zipped array: first event in slot is null.");
                discardSlot();
                return;
            }
            Z[] snapshot = currentSlot.toArray((Z[]) Array.newInstance(typeToken.getClass(), 0)); // unckecked, but ok

            slotActive  = false;
            currentSlot = List.of();
            slotFilled  = new boolean[0];
            filledCount = 0;

            // Emit sequentially on a virtual thread to preserve tuple order.
            dispatchExecutor.execute(() -> downstream.emitNext(snapshot));
        }

        /** Discards the current partial slot without publishing.
         *  Must be called while holding {@link #slotLock}. */
        private void discardSlot() {
            if (timeoutFuture != null) {
                timeoutFuture.cancel(false);
                timeoutFuture = null;
            }
            slotActive  = false;
            currentSlot = List.of();
            slotFilled  = new boolean[0];
            filledCount = 0;
        }

        /** Returns {@code true} if a slot is currently open (partially filled). */
        boolean isSlotActive() {
            slotLock.lock();
            try { return slotActive; }
            finally { slotLock.unlock(); }
        }

        /** Returns the number of positions filled in the current slot. */
        int currentFilledCount() {
            slotLock.lock();
            try { return filledCount; }
            finally { slotLock.unlock(); }
        }
    }
}