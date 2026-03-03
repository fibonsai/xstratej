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
import com.fibonsai.cryptomeria.xtratej.event.series.impl.SingleTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.impl.SingleTimeSeries.Single;
import com.fibonsai.cryptomeria.xtratej.rules.RuleType;
import com.fibonsai.cryptomeria.xtratej.rules.impl.AndRule;
import com.fibonsai.cryptomeria.xtratej.rules.impl.LimitRule;
import com.fibonsai.cryptomeria.xtratej.rules.impl.NotRule;
import com.fibonsai.cryptomeria.xtratej.rules.impl.OrRule;
import com.fibonsai.cryptomeria.xtratej.sources.SourceType;
import com.fibonsai.cryptomeria.xtratej.sources.Subscriber;
import com.fibonsai.cryptomeria.xtratej.strategy.IStrategy.StrategyType;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class StrategyTest {

    private final ThreadLocalRandom random = ThreadLocalRandom.current();
    private final Fifo<TradingSignal> tradingSignalConsumer = new Fifo<>();

    @Test
    public void createStrategyAndRun() {

        Subscriber source1 = SourceType.SIMULATED.builder().setName("flux1").setPublisher("test").build();
        Subscriber source2 = SourceType.SIMULATED.builder().setName("flux2").setPublisher("test").build();
        Subscriber source3 = SourceType.SIMULATED.builder().setName("flux3").setPublisher("test").build();

        // --------

        LimitRule limit1 = (LimitRule) RuleType.Limit.build();
        limit1.setMin(2.0).setMax(80.0);

        LimitRule limit2 = (LimitRule) RuleType.Limit.build();
        limit2.setMin(0.0).setMax(50.0);

        LimitRule limit3 = (LimitRule) RuleType.Limit.build();
        limit3.setLowerSourceId("flux1").setUpperSourceId("flux2");

        OrRule orRule1 = (OrRule) RuleType.Or.build();
        orRule1.watch(Fifo.zip(limit1.results(), limit2.results()));

        NotRule notRule1 = (NotRule) RuleType.Not.build();
        notRule1.watch(Fifo.zip(limit3.results()));

        AndRule andRule1 = (AndRule) RuleType.And.build();
        andRule1.watch(Fifo.zip(orRule1.results(), notRule1.results()));

        Strategy strategyEnter = new Strategy("enter", "UNDEF", StrategyType.ENTER);

        strategyEnter.addSource(source1)
                    .addSource(source2)
                    .addSource(source3)
                    .setAggregatorRule(andRule1);

        // --------

        LimitRule limit4 = (LimitRule) RuleType.Limit.build();
        limit4.setMin(2.0).setMax(80.0);

        LimitRule limit5 = (LimitRule) RuleType.Limit.build();
        limit5.setMin(0.0).setMax(50.0);

        LimitRule limit6 = (LimitRule) RuleType.Limit.build();
        limit6.setLowerSourceId("flux1").setUpperSourceId("flux2");

        OrRule orRule2 = (OrRule) RuleType.Or.build();
        orRule2.watch(Fifo.zip(limit4.results(), limit5.results()));

        NotRule notRule2 = (NotRule) RuleType.Not.build();
        notRule2.watch(Fifo.zip(limit6.results()));

        AndRule andRule2 = (AndRule) RuleType.And.build();
        andRule2.watch(Fifo.zip(orRule2.results(), notRule2.results()));

        Strategy strategyExit = new Strategy("exit", "UNDEF", StrategyType.EXIT);

        strategyExit.addSource(source1)
                    .addSource(source2)
                    .addSource(source3)
                    .setAggregatorRule(andRule2);

        // ---------

        StrategyManager strategyManager = new StrategyManager(tradingSignalConsumer)
                .registerStrategy(strategyEnter)
                .registerStrategy(strategyExit);

        int n = 100;
        AtomicInteger counter = new AtomicInteger(1);
        AtomicLong lastUpdate = new AtomicLong(Instant.now().toEpochMilli());

        tradingSignalConsumer.subscribe(_ -> {
            counter.getAndIncrement();
            lastUpdate.set(Instant.now().toEpochMilli());
        });

        boolean allStrategiesActivated = strategyManager.run();

        for (int x=0; x<n; x++) {
            long timestamp = Instant.now().toEpochMilli();
            double value = x * 1.0D;
            Thread.startVirtualThread(() ->
                source1.toFifo()
                        .emitNext(new SingleTimeSeries("flux1", new Single[]{ new Single(timestamp, value)})));
        }

        for (int x=n-1; x>=0; x--) {
            long timestamp = Instant.now().toEpochMilli();
            double value = x * 1.0D;
            Thread.startVirtualThread(() ->
                source2.toFifo()
                        .emitNext(new SingleTimeSeries("flux2", new Single[]{ new Single(timestamp, value)})));
        }

        for (int x=0; x < n; x++) {
            long timestamp = Instant.now().toEpochMilli();
            double value = random.nextDouble(0.0, n);
            Thread.startVirtualThread(() ->
                source3.toFifo()
                        .emitNext(new SingleTimeSeries("flux3", new Single[]{ new Single(timestamp, value)})));
        }

        assertTrue(allStrategiesActivated);
        assertTrue(counter.get() > 0);
    }

    @Test
    public void createStrategyFromJsonV2AndRun() throws IOException {
        Map<String, IStrategy> strategies;
        StrategyManager strategyManager = new StrategyManager(tradingSignalConsumer);

        ObjectMapper mapper = new ObjectMapper();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("strategies_v2.json")) {
            JsonNode jsonNode = mapper.readValue(is, JsonNode.class);
            strategies = LoaderV2.fromJson(jsonNode);
        }

        for (var strategy: strategies.values()) {
            strategyManager.registerStrategy(strategy);
        }

        strategyManager.run();

        int n = 100;
        AtomicInteger counter = new AtomicInteger(1);
        AtomicLong lastUpdate = new AtomicLong(Instant.now().toEpochMilli());

        tradingSignalConsumer.subscribe(_ -> {
            counter.getAndIncrement();
            lastUpdate.set(Instant.now().toEpochMilli());
        });

        boolean allStrategiesActivated = strategyManager.run();

        for (var strategy: strategyManager.getStrategies()) {
            for (var source: strategy.getSources().values()) {
                String sourceName = source.name();
                if (sourceName.equals("flux1") || sourceName.equals("flux4")) {
                    for (int x=0; x<n; x++) {
                        long timestamp = Instant.now().toEpochMilli();
                        double value = x * 1.0D;
                        Thread.startVirtualThread(() ->
                                source.toFifo()
                                        .emitNext(new SingleTimeSeries(sourceName, new Single[]{ new Single(timestamp, value)})));
                    }
                } else if (sourceName.equals("flux2") || sourceName.equals("flux5")) {
                    for (int x=n-1; x>=0; x--) {
                        long timestamp = Instant.now().toEpochMilli();
                        double value = x * 1.0D;
                        Thread.startVirtualThread(() ->
                                source.toFifo()
                                        .emitNext(new SingleTimeSeries(sourceName, new Single[]{ new Single(timestamp, value)})));
                    }
                } else {
                    for (int x=0; x < n; x++) {
                        long timestamp = Instant.now().toEpochMilli();
                        double value = random.nextDouble(0.0, n);
                        Thread.startVirtualThread(() ->
                                source.toFifo()
                                        .emitNext(new SingleTimeSeries(sourceName, new Single[]{ new Single(timestamp, value)})));
                    }
                }
            }
        }

        assertTrue(allStrategiesActivated);
        assertTrue(counter.get() > 0);
    }
}
