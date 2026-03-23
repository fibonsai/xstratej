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

package com.fibonsai.cryptomeria.xtratej.engine.strategy;

import com.fibonsai.cryptomeria.xtratej.engine.rules.RuleType;
import com.fibonsai.cryptomeria.xtratej.engine.rules.impl.AndRule;
import com.fibonsai.cryptomeria.xtratej.engine.rules.impl.LimitRule;
import com.fibonsai.cryptomeria.xtratej.engine.rules.impl.NotRule;
import com.fibonsai.cryptomeria.xtratej.engine.rules.impl.OrRule;
import com.fibonsai.cryptomeria.xtratej.engine.sources.SourceType;
import com.fibonsai.cryptomeria.xtratej.engine.sources.Subscriber;
import com.fibonsai.cryptomeria.xtratej.engine.strategy.IStrategy.StrategyType;
import com.fibonsai.cryptomeria.xtratej.engine.targets.Publisher;
import com.fibonsai.cryptomeria.xtratej.engine.targets.TargetType;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.TradingSignal;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.builders.DoubleTimeSeriesBuilder;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class StrategyTest {

    private static final Logger log = LoggerFactory.getLogger(StrategyTest.class);
    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    @Test
    public void createStrategyAndRun() {

        Subscriber source1 = SourceType.SIMULATED.builder().setName("flux1").setPublisher("test").build();
        Subscriber source2 = SourceType.SIMULATED.builder().setName("flux2").setPublisher("test").build();
        Subscriber source3 = SourceType.SIMULATED.builder().setName("flux3").setPublisher("test").build();

        // --------

        LimitRule limit1 = (LimitRule) RuleType.Limit.build();
        limit1.setMin(2.0).setMax(80.0);
        limit1.watch(source1, source2, source3);

        LimitRule limit2 = (LimitRule) RuleType.Limit.build();
        limit2.setMin(0.0).setMax(50.0);
        limit2.watch(source1, source2, source3);

        LimitRule limit3 = (LimitRule) RuleType.Limit.build();
        limit3.setLowerSourceId("flux1").setUpperSourceId("flux2");
        limit3.watch(source1, source2, source3);

        OrRule orRule1 = (OrRule) RuleType.Or.build();
        orRule1.watch(limit1, limit2);

        NotRule notRule1 = (NotRule) RuleType.Not.build();
        notRule1.watch(limit3);

        AndRule andRule1 = (AndRule) RuleType.And.build();
        andRule1.watch(orRule1, notRule1);

        Strategy strategyEnter = new Strategy("enter", "UNDEF", StrategyType.ENTER);

        strategyEnter.addSource(source1)
                    .addSource(source2)
                    .addSource(source3)
                    .setAggregatorRule(andRule1);

        // --------

        LimitRule limit4 = (LimitRule) RuleType.Limit.build();
        limit4.setMin(2.0).setMax(80.0);
        limit4.watch(source1, source2, source3);

        LimitRule limit5 = (LimitRule) RuleType.Limit.build();
        limit5.setMin(0.0).setMax(50.0);
        limit5.watch(source1, source2, source3);

        LimitRule limit6 = (LimitRule) RuleType.Limit.build();
        limit6.setLowerSourceId("flux1").setUpperSourceId("flux2");
        limit6.watch(source1, source2, source3);

        OrRule orRule2 = (OrRule) RuleType.Or.build();
        orRule2.watch(limit4, limit5);

        NotRule notRule2 = (NotRule) RuleType.Not.build();
        notRule2.watch(limit6);

        AndRule andRule2 = (AndRule) RuleType.And.build();
        andRule2.watch(orRule2, notRule2);

        Strategy strategyExit = new Strategy("exit", "UNDEF", StrategyType.EXIT);

        strategyExit.addSource(source1)
                    .addSource(source2)
                    .addSource(source3)
                    .setAggregatorRule(andRule2);

        // ---------

        Publisher publisher = TargetType.SIMULATED.builder().setName("simulated").build();
        StrategyManager strategyManager = new StrategyManager()
                .setPublisher(publisher)
                .registerStrategy(strategyEnter)
                .registerStrategy(strategyExit);

        int n = 100;

        CountDownLatch enterLatch = new CountDownLatch(1);
        CountDownLatch exitLatch = new CountDownLatch(1);
        AtomicReference<@Nullable TradingSignal> enterTradingSignal = new AtomicReference<>(null);
        AtomicReference<@Nullable TradingSignal> exitTradingSignal = new AtomicReference<>(null);
        publisher.subscribe(t -> {
            if (t.signal() == TradingSignal.Signal.ENTER) {
                enterTradingSignal.set(t);
                enterLatch.countDown();
            } else {
                exitTradingSignal.set(t);
                exitLatch.countDown();
            }
        });

        boolean allStrategiesActivated = strategyManager.run();

        for (int x=0; x<n; x++) {
            long timestamp = Instant.now().toEpochMilli();
            double value = x * 1.0D;
            Thread.startVirtualThread(() ->
                source1.toDirectFlux()
                        .emitNext(new DoubleTimeSeriesBuilder().setId("flux1").add(timestamp, value).build()));
        }

        for (int x=n-1; x>=0; x--) {
            long timestamp = Instant.now().toEpochMilli();
            double value = x * 1.0D;
            Thread.startVirtualThread(() ->
                source2.toDirectFlux()
                        .emitNext(new DoubleTimeSeriesBuilder().setId("flux2").add(timestamp, value).build()));
        }

        for (int x=0; x < n; x++) {
            long timestamp = Instant.now().toEpochMilli();
            double value = random.nextDouble(0.0, n);
            Thread.startVirtualThread(() ->
                source3.toDirectFlux()
                        .emitNext(new DoubleTimeSeriesBuilder().setId("flux3").add(timestamp, value).build()));
        }

        assertDoesNotThrow(() -> enterLatch.await(2, TimeUnit.SECONDS));
        assertDoesNotThrow(() -> exitLatch.await(2, TimeUnit.SECONDS));
        assertNotNull(enterTradingSignal.get());
        assertNotNull(exitTradingSignal.get());
        assertTrue(allStrategiesActivated);
    }

    @Test
    public void createStrategyFromJsonV2AndRun() throws IOException {
        Map<String, IStrategy> strategies;
        Publisher publisher = TargetType.SIMULATED.builder().setName("simulated").build();
        StrategyManager strategyManager = new StrategyManager().setPublisher(publisher);

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

        CountDownLatch enterLatch = new CountDownLatch(1);
        CountDownLatch exitLatch = new CountDownLatch(1);
        AtomicReference<@Nullable TradingSignal> enterTradingSignal = new AtomicReference<>(null);
        AtomicReference<@Nullable TradingSignal> exitTradingSignal = new AtomicReference<>(null);
        publisher.subscribe(t -> {
            if (t.signal() == TradingSignal.Signal.ENTER) {
                enterTradingSignal.set(t);
                enterLatch.countDown();
            } else {
                exitTradingSignal.set(t);
                exitLatch.countDown();
            }
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
                                source.toDirectFlux()
                                        .emitNext(new DoubleTimeSeriesBuilder().setId(sourceName).add(timestamp, value).build()));
                    }
                } else if (sourceName.equals("flux2") || sourceName.equals("flux5")) {
                    for (int x=n-1; x>=0; x--) {
                        long timestamp = Instant.now().toEpochMilli();
                        double value = x * 1.0D;
                        Thread.startVirtualThread(() ->
                                source.toDirectFlux()
                                        .emitNext(new DoubleTimeSeriesBuilder().setId(sourceName).add(timestamp, value).build()));
                    }
                } else {
                    for (int x=0; x < n; x++) {
                        long timestamp = Instant.now().toEpochMilli();
                        double value = random.nextDouble(0.0, n);
                        Thread.startVirtualThread(() ->
                                source.toDirectFlux()
                                        .emitNext(new DoubleTimeSeriesBuilder().setId(sourceName).add(timestamp, value).build()));
                    }
                }
            }
        }

        assertDoesNotThrow(() -> enterLatch.await(2, TimeUnit.SECONDS));
        assertDoesNotThrow(() -> exitLatch.await(2, TimeUnit.SECONDS));
        assertNotNull(exitTradingSignal.get());
        assertNotNull(enterTradingSignal.get());
        assertTrue(allStrategiesActivated);
    }
}
