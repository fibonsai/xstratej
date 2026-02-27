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
import tools.jackson.databind.node.ObjectNode;

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

        ObjectNode properties1 = nodeFactory.objectNode();
        LimitRule limit1 = new LimitRule("limit1", properties1);
        limit1.setMin(2.0).setMax(80.0).setAllSources(true);

        ObjectNode properties2 = nodeFactory.objectNode();
        LimitRule limit2 = new LimitRule("limit2", properties2);
        limit2.setMin(0.0).setMax(50.0).setAllSources(true);

        ObjectNode properties3 = nodeFactory.objectNode();
        LimitRule limit3 = new LimitRule("limit3", properties3);
        limit3.setLowerSourceId("flux1").setUpperSourceId("flux2").setAllSources(true);

        OrRule orRule1 = new OrRule("orRule1", nodeFactory.nullNode());
        orRule1.addSourceId(limit1.name()).addSourceId(limit2.name()).setAllSources(false);

        NotRule notRule1 = new NotRule("notRule1", nodeFactory.nullNode());
        notRule1.addSourceId(limit3.name()).setAllSources(false);

        AndRule andRule1 = new AndRule("andRule1", nodeFactory.nullNode());
        andRule1.addSourceId(orRule1.name()).addSourceId(notRule1.name()).setAllSources(false);


        ObjectNode properties4 = nodeFactory.objectNode();
        LimitRule limit4 = new LimitRule("limit4", properties4);
        limit4.setMin(2.0).setMax(80.0).setAllSources(true);

        ObjectNode properties5 = nodeFactory.objectNode();
        LimitRule limit5 = new LimitRule("limit5", properties5);
        limit5.setMin(0.0).setMax(50.0).setAllSources(true);

        ObjectNode properties6 = nodeFactory.objectNode();
        LimitRule limit6 = new LimitRule("limit6", properties6);
        limit6.setLowerSourceId("flux1").setUpperSourceId("flux2").setAllSources(true);

        OrRule orRule2 = new OrRule("orRule2", nodeFactory.nullNode());
        orRule2.addSourceId(limit4.name()).addSourceId(limit5.name()).setAllSources(false);

        NotRule notRule2 = new NotRule("notRule2", nodeFactory.nullNode());
        notRule2.addSourceId(limit6.name()).setAllSources(false);

        AndRule andRule2 = new AndRule("andRule2", nodeFactory.nullNode());
        andRule2.addSourceId(orRule2.name()).addSourceId(notRule2.name()).setAllSources(false);

        Strategy strategyEnter = new Strategy("enter", "UNDEF", "UNDEF", StrategyType.ENTER);
        Strategy strategyExit = new Strategy("exit", "UNDEF", "UNDEF", StrategyType.EXIT);

        Subscriber source1 = new SimulatedSubscriber("flux1", nodeFactory.nullNode(), new Fifo<>());
        Subscriber source2 = new SimulatedSubscriber("flux2", nodeFactory.nullNode(), new Fifo<>());
        Subscriber source3 = new SimulatedSubscriber("flux3", nodeFactory.nullNode(), new Fifo<>());

        strategyEnter.addSource(source1)
                    .addSource(source2)
                    .addSource(source3)
                    .addRule(limit1)
                    .addRule(limit2)
                    .addRule(limit3)
                    .addRule(andRule1)
                    .addRule(orRule1)
                    .addRule(notRule1)
                    .setAggregatorRule(andRule1.name());

        strategyExit.addSource(source1)
                    .addSource(source2)
                    .addSource(source3)
                    .addRule(limit4)
                    .addRule(limit5)
                    .addRule(limit6)
                    .addRule(andRule2)
                    .addRule(orRule2)
                    .addRule(notRule2)
                    .setAggregatorRule(andRule2);

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
    public void createStretegyFromJsonAndRun() throws IOException {
        Map<String, IStrategy> strategies;
        StrategyManager strategyManager = new StrategyManager(tradingSignalConsumer);

        ObjectMapper mapper = new ObjectMapper();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("strategies.json")) {
            JsonNode jsonNode = mapper.readValue(is, JsonNode.class);
            strategies = Loader.fromJson(jsonNode);
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
            for (var source: strategy.getSources()) {
                String sourceName = source.name();
                switch (sourceName) {
                    case "flux1": {
                        for (int x=0; x<n; x++) {
                            long timestamp = Instant.now().toEpochMilli();
                            double value = x * 1.0D;
                            Thread.startVirtualThread(() ->
                                    source.toFifo()
                                            .emitNext(new SingleTimeSeries(sourceName, new Single[]{ new Single(timestamp, value)})));
                        }
                        break;
                    }
                    case "flux2": {
                        for (int x=n-1; x>=0; x--) {
                            long timestamp = Instant.now().toEpochMilli();
                            double value = x * 1.0D;
                            Thread.startVirtualThread(() ->
                                    source.toFifo()
                                            .emitNext(new SingleTimeSeries(sourceName, new Single[]{ new Single(timestamp, value)})));
                        }
                        break;
                    }
                    case "flux3": {
                        for (int x=0; x < n; x++) {
                            long timestamp = Instant.now().toEpochMilli();
                            double value = random.nextDouble(0.0, n);
                            Thread.startVirtualThread(() ->
                                    source.toFifo()
                                            .emitNext(new SingleTimeSeries(sourceName, new Single[]{ new Single(timestamp, value)})));
                        }
                        break;
                    }
                }
            }
        }

        assertTrue(allStrategiesActivated);
        assertTrue(counter.get() > 0);
    }
}
