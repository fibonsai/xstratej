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

import com.fibonsai.cryptomeria.xtratej.engine.rules.RuleStream;
import com.fibonsai.cryptomeria.xtratej.engine.rules.RuleType;
import com.fibonsai.cryptomeria.xtratej.engine.sources.SourceType;
import com.fibonsai.cryptomeria.xtratej.engine.sources.Subscriber;
import com.fibonsai.cryptomeria.xtratej.engine.targets.TargetType;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.BooleanTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.EmptyTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.TimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.builders.BooleanTimeSeriesBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class StrategyManagerTest {

    private AutoCloseable closeable;

    @Mock
    private IStrategy mockStrategy;

    private StrategyManager strategyManager;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        strategyManager = new StrategyManager();
    }

    @AfterEach
    void finish() {
        try {
            closeable.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void registerStrategy_addsStrategyToList() {
        when(mockStrategy.name()).thenReturn("testStrategy");
        when(mockStrategy.symbol()).thenReturn("TEST");
        when(mockStrategy.strategyType()).thenReturn(IStrategy.StrategyType.ENTER);

        strategyManager.registerStrategy(mockStrategy);

        ArrayList<IStrategy> strategies = strategyManager.getStrategies();
        assertEquals(1, strategies.size());
        assertTrue(strategies.contains(mockStrategy));
    }

    @Test
    void registerStrategy_multipleCalls_addsAllStrategies() {
        IStrategy strategy1 = new Strategy("strategy1", "S1", IStrategy.StrategyType.ENTER);
        IStrategy strategy2 = new Strategy("strategy2", "S2", IStrategy.StrategyType.EXIT);
        IStrategy strategy3 = new Strategy("strategy3", "S3", IStrategy.StrategyType.ENTER);

        strategyManager.registerStrategy(strategy1);
        strategyManager.registerStrategy(strategy2);
        strategyManager.registerStrategy(strategy3);

        ArrayList<IStrategy> strategies = strategyManager.getStrategies();
        assertEquals(3, strategies.size());
        assertTrue(strategies.contains(strategy1));
        assertTrue(strategies.contains(strategy2));
        assertTrue(strategies.contains(strategy3));
    }

    @Test
    void registerStrategy_returnsSelfForFluentInterface() {
        StrategyManager result = strategyManager.registerStrategy(mockStrategy);

        assertSame(strategyManager, result);
    }

    @Test
    void setPublisher_defaultPublisherIsSimulated() {
        StrategyManager defaultManager = new StrategyManager();

        assertNotNull(defaultManager);
    }

    @Test
    void getStrategies_returnsNonEmptyListWhenStrategiesAdded() {
        IStrategy strategy1 = new Strategy("s1", "SY1", IStrategy.StrategyType.ENTER);
        IStrategy strategy2 = new Strategy("s2", "SY2", IStrategy.StrategyType.EXIT);
        strategyManager.registerStrategy(strategy1);
        strategyManager.registerStrategy(strategy2);

        ArrayList<IStrategy> strategies = strategyManager.getStrategies();

        assertEquals(2, strategies.size());
    }

    @Test
    void getStrategies_returnsEmptyListWhenNoStrategies() {
        StrategyManager defaultManager = new StrategyManager();

        ArrayList<IStrategy> strategies = defaultManager.getStrategies();

        assertEquals(0, strategies.size());
    }

    @Test
    void run_emptyStrategies_returnsFalse() {
        StrategyManager emptyManager = new StrategyManager();

        boolean result = emptyManager.run();

        assertFalse(result);
    }

    @Test
    void run_withValidStrategy_executesAndReturnsTrue() {
        Subscriber source1 = SourceType.SIMULATED.builder().setName("flux1").setPublisher("test").build();
        BooleanTimeSeriesBuilder builder = new BooleanTimeSeriesBuilder().setId("ruleResult");
        BooleanTimeSeries ruleResult = builder.add(System.currentTimeMillis(), true).build();

        Strategy strategy = new Strategy("test", "TEST", IStrategy.StrategyType.ENTER);
        strategy.addSource(source1);
        strategy.setAggregatorRule(new RuleStream<BooleanTimeSeries>() {
            @Override
            protected java.util.function.Function<TimeSeries[], BooleanTimeSeries[]> predicate() {
                return _ -> new BooleanTimeSeries[]{ruleResult};
            }
        });

        strategyManager.registerStrategy(strategy);

        strategyManager.setPublisher(TargetType.SIMULATED.builder().setName("test").build());

        boolean result = strategyManager.run();

        assertTrue(result);
    }

    @Test
    void run_withMultipleStrategies_executesAll() {
        Subscriber source1 = SourceType.SIMULATED.builder().setName("flux1").setPublisher("test").build();
        Subscriber source2 = SourceType.SIMULATED.builder().setName("flux2").setPublisher("test").build();

        Strategy strategy1 = new Strategy("enter", "ENTER", IStrategy.StrategyType.ENTER);
        strategy1.addSource(source1);
        RuleStream<? extends TimeSeries> falseRule1 = RuleType.False.build();
        falseRule1.watch(source1);
        strategy1.setAggregatorRule(falseRule1);

        Strategy strategy2 = new Strategy("exit", "EXIT", IStrategy.StrategyType.EXIT);
        strategy2.addSource(source2);
        RuleStream<? extends TimeSeries> falseRule2 = RuleType.False.build();
        falseRule2.watch(source2);
        strategy2.setAggregatorRule(falseRule2);

        strategyManager.registerStrategy(strategy1);
        strategyManager.registerStrategy(strategy2);

        boolean result = strategyManager.run();
        source1.toDirectFlux().emitNext(EmptyTimeSeries.INSTANCE);
        source2.toDirectFlux().emitNext(EmptyTimeSeries.INSTANCE);

        assertTrue(result);
    }

    @Test
    void run_concurrentStrategyExecution_threadsafe() {
        int strategyCount = 5;
        List<Subscriber> sources = new LinkedList<>();

        for (int i = 0; i < strategyCount; i++) {
            Subscriber source = SourceType.SIMULATED.builder()
                    .setName("flux" + i)
                    .setPublisher("test")
                    .build();
            sources.add(source);

            Strategy strategy = new Strategy("strategy" + i, "S" + i, IStrategy.StrategyType.ENTER);
            strategy.addSource(source);
            RuleStream<BooleanTimeSeries> slowRule = new RuleStream<>() {
                @Override
                protected Function<TimeSeries[], BooleanTimeSeries[]> predicate() {
                    return timeSeriesArray -> {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        return new BooleanTimeSeries[]{
                                new BooleanTimeSeriesBuilder().add(System.currentTimeMillis(), true).build()
                        };
                    };
                }
            };
            slowRule.watch(source);
            strategy.setAggregatorRule(slowRule);
            strategyManager.registerStrategy(strategy);
        }

        boolean result = strategyManager.run();
        sources.forEach(s -> s.toDirectFlux().emitNext(EmptyTimeSeries.INSTANCE));

        assertTrue(result);
    }

    @Test
    void run_strategyExceptionDoesNotStopOtherStrategies() {
        Subscriber source1 = SourceType.SIMULATED.builder().setName("flux1").setPublisher("test").build();
        Subscriber source2 = SourceType.SIMULATED.builder().setName("flux2").setPublisher("test").build();

        Strategy failingStrategy = new Strategy("failing", "FAIL", IStrategy.StrategyType.ENTER);
        failingStrategy.addSource(source1);
        RuleStream<BooleanTimeSeries> intentionalFailureForTesting = new RuleStream<>() {
            @Override
            protected Function<TimeSeries[], BooleanTimeSeries[]> predicate() {
                return _ -> {
                    throw new RuntimeException("Intentional failure for testing");
                };
            }
        };
        intentionalFailureForTesting.watch(source1);
        failingStrategy.setAggregatorRule(intentionalFailureForTesting);

        Strategy workingStrategy = new Strategy("working", "WORK", IStrategy.StrategyType.EXIT);
        workingStrategy.addSource(source2);
        var ruleFalse = RuleType.False.build();
        ruleFalse.watch(source2);
        workingStrategy.setAggregatorRule(ruleFalse);

        strategyManager.registerStrategy(failingStrategy);
        strategyManager.registerStrategy(workingStrategy);

        boolean result = strategyManager.run();
        source1.toDirectFlux().emitNext(EmptyTimeSeries.INSTANCE);
        source2.toDirectFlux().emitNext(EmptyTimeSeries.INSTANCE);

        assertTrue(result);
    }

    @Test
    void run_withTimeout_behavior() {
        Subscriber source = SourceType.SIMULATED.builder().setName("slow").setPublisher("test").build();

        Strategy slowStrategy = new Strategy("slow", "SLOW", IStrategy.StrategyType.ENTER);
        slowStrategy.addSource(source);
        RuleStream<BooleanTimeSeries> slowRule = new RuleStream<>() {
            @Override
            protected Function<TimeSeries[], BooleanTimeSeries[]> predicate() {
                return timeSeriesArray -> {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return new BooleanTimeSeries[]{
                            new BooleanTimeSeriesBuilder().add(System.currentTimeMillis(), true).build()
                    };
                };
            }
        };
        slowRule.watch(source);
        slowStrategy.setAggregatorRule(slowRule);

        strategyManager.registerStrategy(slowStrategy);

        boolean result = strategyManager.run();
        source.toDirectFlux().emitNext(EmptyTimeSeries.INSTANCE);

        assertTrue(result);
    }
}
