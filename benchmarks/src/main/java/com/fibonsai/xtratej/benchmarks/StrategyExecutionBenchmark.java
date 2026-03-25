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

package com.fibonsai.xtratej.benchmarks;

import com.fibonsai.xtratej.engine.adapters.SourceType;
import com.fibonsai.xtratej.engine.adapters.TargetType;
import com.fibonsai.xtratej.engine.rules.RuleType;
import com.fibonsai.xtratej.engine.strategy.IStrategy;
import com.fibonsai.xtratej.engine.strategy.Strategy;
import com.fibonsai.xtratej.engine.strategy.StrategyManager;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1, jvmArgs = {"-Xms2G", "-Xmx2G"})
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
public class StrategyExecutionBenchmark {

    private StrategyManager strategyManager;
    private Strategy simpleStrategy;

    @Setup
    public void setup() {
        // Create a strategy manager
        strategyManager = new StrategyManager();

        // Create a simple strategy with simulated source
        simpleStrategy = new Strategy("simple", "SIM", IStrategy.StrategyType.ENTER);
        simpleStrategy.addSource(SourceType.SIMULATED.builder().setName("flux1").setPublisher("test").build());
        simpleStrategy.setAggregatorRule(RuleType.False.build());

        // Add the strategy to the manager
        strategyManager.registerStrategy(simpleStrategy);

        // Set a publisher
        strategyManager.setPublisher(TargetType.SIMULATED.builder().setName("test").build());
    }

    @Benchmark
    public boolean benchmarkSimpleStrategyExecution() {
        return strategyManager.run();
    }

    @Benchmark
    public StrategyManager benchmarkStrategyManagerCreation() {
        StrategyManager manager = new StrategyManager();
        manager.setPublisher(TargetType.SIMULATED.builder().setName("test").build());

        // Add multiple strategies
        for (int i = 0; i < 10; i++) {
            Strategy strategy = new Strategy("strategy" + i, "SYM" + i, IStrategy.StrategyType.ENTER);
            strategy.addSource(SourceType.SIMULATED.builder().setName("flux" + i).setPublisher("test").build());
            strategy.setAggregatorRule(RuleType.False.build());
            manager.registerStrategy(strategy);
        }

        return manager;
    }

    @Benchmark
    public boolean benchmarkMultipleStrategiesExecution() {
        StrategyManager manager = new StrategyManager();
        manager.setPublisher(TargetType.SIMULATED.builder().setName("test").build());

        // Add multiple strategies
        for (int i = 0; i < 5; i++) {
            Strategy strategy = new Strategy("strategy" + i, "SYM" + i, IStrategy.StrategyType.ENTER);
            strategy.addSource(SourceType.SIMULATED.builder().setName("flux" + i).setPublisher("test").build());
            strategy.setAggregatorRule(RuleType.False.build());
            manager.registerStrategy(strategy);
        }

        return manager.run();
    }

    @Benchmark
    public boolean benchmarkComplexRuleStrategyExecution() {
        StrategyManager manager = new StrategyManager();
        manager.setPublisher(TargetType.SIMULATED.builder().setName("test").build());

        Strategy strategy = new Strategy("complex", "CMP", IStrategy.StrategyType.ENTER);
        strategy.addSource(SourceType.SIMULATED.builder().setName("flux1").setPublisher("test").build());
        strategy.addSource(SourceType.SIMULATED.builder().setName("flux2").setPublisher("test").build());

        // Use a more complex rule combination
        strategy.setAggregatorRule(RuleType.Or.build());

        manager.registerStrategy(strategy);

        return manager.run();
    }
}