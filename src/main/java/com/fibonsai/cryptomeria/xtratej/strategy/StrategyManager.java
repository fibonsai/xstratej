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

package com.fibonsai.cryptomeria.xtratej.strategy;

import com.fibonsai.cryptomeria.xtratej.event.TradingSignal;
import com.fibonsai.cryptomeria.xtratej.event.reactive.Fifo;
import com.fibonsai.cryptomeria.xtratej.event.series.impl.BooleanSingleTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.impl.BooleanSingleTimeSeries.BooleanSingle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class StrategyManager {

    private static final Logger log = LoggerFactory.getLogger(StrategyManager.class);
    private final ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = reentrantReadWriteLock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = reentrantReadWriteLock.writeLock();

    private final ArrayList<IStrategy> strategies = new ArrayList<>();
    private final Fifo<TradingSignal> tradingSignalConsumer;

    public StrategyManager(Fifo<TradingSignal> tradingSignalConsumer) {
        this.tradingSignalConsumer = tradingSignalConsumer;
    }

    public StrategyManager registerStrategy(IStrategy strategy) {
        writeLock.lock();
        try {
            strategies.add(strategy);
        } finally {
            writeLock.unlock();
        }
        return this;
    }

    public boolean run() {
        readLock.lock();
        if (strategies.isEmpty()) {
            return false;
        }
        try {
            CountDownLatch latch = new CountDownLatch(strategies.size());
            strategies.forEach(strategy -> {
                final String strategyName = strategy.name();
                final String strategyPair = strategy.symbol();
                final String strategySource = strategy.source();
                final TradingSignal.Signal signalType = switch (strategy.strategyType()) {
                    case ENTER -> TradingSignal.Signal.ENTER;
                    case EXIT -> TradingSignal.Signal.EXIT;
                    default -> TradingSignal.Signal.UNDEF;
                };

                Thread.startVirtualThread(() -> {
                    log.info("Executing {} strategy", strategyName);
                    strategy.activeRules().onSubscribe(latch::countDown).subscribe(temporalData -> {
                        var result = switch (temporalData) {
                            case BooleanSingleTimeSeries ts when ts.size() > 0 -> new BooleanSingle(ts.timestamp(), ts.values()[ts.size() - 1]);
                            case BooleanSingle bSingle -> bSingle;
                            default -> new BooleanSingle(0, false);
                        };
                        long timestamp;
                        if ((timestamp = result.timestamp()) > 0 && result.value()) {
                            if (log.isDebugEnabled()) {
                                log.debug("[{}] Strategy {}: Send trading signal", timestamp, strategyName);
                            }
                            var tradingSignal = new TradingSignal(timestamp, signalType, strategyName, strategyPair, strategySource);
                            tradingSignalConsumer.emitNext(tradingSignal);
                        }
                    });
                });
            });
            return latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
            return false;
        } finally {
            readLock.unlock();
        }
    }

}
