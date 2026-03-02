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
import com.fibonsai.cryptomeria.xtratej.rules.impl.AndRule;
import com.fibonsai.cryptomeria.xtratej.rules.impl.LimitRule;
import com.fibonsai.cryptomeria.xtratej.rules.impl.NotRule;
import com.fibonsai.cryptomeria.xtratej.rules.impl.OrRule;
import com.fibonsai.cryptomeria.xtratej.sources.Subscriber;
import com.fibonsai.cryptomeria.xtratej.sources.impl.SimulatedSubscriber;
import com.fibonsai.cryptomeria.xtratej.strategy.IStrategy.StrategyType;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.JsonNodeFactory;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class StrategyTest {

    private final JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
    private final ThreadLocalRandom random = ThreadLocalRandom.current();
    private final Fifo<TradingSignal> tradingSignalConsumer = new Fifo<>();

    @Test
    public void createStrategyAndRun() {

        Subscriber source1 = new SimulatedSubscriber("flux1", SimulatedSubscriber.class.getSimpleName(), nodeFactory.nullNode(), new Fifo<>());
        Subscriber source2 = new SimulatedSubscriber("flux2", SimulatedSubscriber.class.getSimpleName(), nodeFactory.nullNode(), new Fifo<>());
        Subscriber source3 = new SimulatedSubscriber("flux3", SimulatedSubscriber.class.getSimpleName(), nodeFactory.nullNode(), new Fifo<>());

        // --------

        LimitRule limit1 = new LimitRule();
        limit1.setMin(2.0).setMax(80.0);

        LimitRule limit2 = new LimitRule();
        limit2.setMin(0.0).setMax(50.0);

        LimitRule limit3 = new LimitRule();
        limit3.setLowerSourceId("flux1").setUpperSourceId("flux2");

        OrRule orRule1 = new OrRule();
        orRule1.subscribe(Fifo.zip(limit1.results(), limit2.results()));

        NotRule notRule1 = new NotRule();
        notRule1.subscribe(Fifo.zip(limit3.results()));

        AndRule andRule1 = new AndRule();
        andRule1.subscribe(Fifo.zip(orRule1.results(), notRule1.results()));

        Strategy strategyEnter = new Strategy("enter", "UNDEF", StrategyType.ENTER);

        strategyEnter.addSource(source1)
                .addSource(source2)
                .addSource(source3)
                .setAggregatorRule(andRule1);

        // --------

        LimitRule limit4 = new LimitRule();
        limit4.setMin(2.0).setMax(80.0);

        LimitRule limit5 = new LimitRule();
        limit5.setMin(0.0).setMax(50.0);

        LimitRule limit6 = new LimitRule();
        limit6.setLowerSourceId("flux1").setUpperSourceId("flux2");

        OrRule orRule2 = new OrRule();
        orRule2.subscribe(Fifo.zip(limit4.results(), limit5.results()));

        NotRule notRule2 = new NotRule();
        notRule2.subscribe(Fifo.zip(limit6.results()));

        AndRule andRule2 = new AndRule();
        andRule2.subscribe(Fifo.zip(orRule2.results(), notRule2.results()));

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
