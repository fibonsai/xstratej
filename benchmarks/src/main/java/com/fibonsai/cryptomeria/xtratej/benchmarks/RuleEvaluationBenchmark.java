/*
 * Copyright (c) 2026 fibonsai.com
 * All rights reserved.
 *
 * This source is subject to the Apache License, Version 2.0.
 * Please see the LICENSE file for more information.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fibonsai.cryptomeria.xtratej.benchmarks;

import com.fibonsai.cryptomeria.xtratej.engine.rules.RuleType;
import com.fibonsai.cryptomeria.xtratej.engine.rules.impl.CrossedRule;
import com.fibonsai.cryptomeria.xtratej.engine.rules.impl.LimitRule;
import com.fibonsai.cryptomeria.xtratej.engine.rules.impl.TrendRule;
import com.fibonsai.cryptomeria.xtratej.event.reactive.DirectFlux;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.DoubleTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.TimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.builders.DoubleTimeSeriesBuilder;
import org.openjdk.jmh.annotations.*;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 1, jvmArgs = {"-Xms2G", "-Xmx2G"})
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
public class RuleEvaluationBenchmark {

    private CrossedRule crossedRule;
    private LimitRule limitRule;
    private TrendRule trendRule;
    private DirectFlux<TimeSeries[]> inputDirectFlux;
    private TimeSeries[] crossedInput;
    private TimeSeries[] limitInput;
    private TimeSeries[] trendInput;

    @Setup
    public void setup() {
        // Setup input FIFO
        inputDirectFlux = new DirectFlux<>();

        // Setup CrossedRule
        ObjectNode crossedParams = JsonNodeFactory.instance.objectNode();
        crossedParams.put("threshold", 50.0);
        crossedRule = (CrossedRule) RuleType.Crossed.build().setParams(crossedParams);
        crossedRule.watch(inputDirectFlux);

        // Setup LimitRule
        ObjectNode limitParams = JsonNodeFactory.instance.objectNode();
        limitParams.put("min", 10.0);
        limitParams.put("max", 90.0);
        limitRule = (LimitRule) RuleType.Limit.build().setParams(limitParams);
        limitRule.watch(inputDirectFlux);

        // Setup TrendRule
        ObjectNode trendParams = JsonNodeFactory.instance.objectNode();
        trendParams.put("isRising", true);
        trendRule = (TrendRule) RuleType.Trend.build().setParams(trendParams);
        trendRule.watch(inputDirectFlux);

        // Prepare input data
        DoubleTimeSeries crossedSeries = createDoubleTimeSeries("crossed", new long[]{1L, 2L}, new double[]{40.0, 60.0});
        crossedInput = new TimeSeries[]{crossedSeries};

        DoubleTimeSeries limitSeries = createDoubleTimeSeries("limit", new long[]{1L, 2L}, new double[]{50.0, 60.0});
        limitInput = new TimeSeries[]{limitSeries};

        DoubleTimeSeries trendSeries = createDoubleTimeSeries("trend", new long[]{1L, 2L, 3L}, new double[]{1.0, 2.0, 3.0});
        trendInput = new TimeSeries[]{trendSeries};
    }

    private DoubleTimeSeries createDoubleTimeSeries(String name, long[] timestamps, double[] values) {
        DoubleTimeSeriesBuilder builder = new DoubleTimeSeriesBuilder().setId(name);
        for (int i = 0; i < values.length; i++) {
            builder.add(timestamps[i], values[i]);
        }
        return builder.build();
    }

    @Benchmark
    public void benchmarkCrossedRuleEvaluation() throws InterruptedException {
        // Emit input to trigger rule evaluation
        inputDirectFlux.emitNext(crossedInput);
        // Wait briefly for processing
        Thread.sleep(1);
    }

    @Benchmark
    public void benchmarkLimitRuleEvaluation() throws InterruptedException {
        // Emit input to trigger rule evaluation
        inputDirectFlux.emitNext(limitInput);
        // Wait briefly for processing
        Thread.sleep(1);
    }

    @Benchmark
    public void benchmarkTrendRuleEvaluation() throws InterruptedException {
        // Emit input to trigger rule evaluation
        inputDirectFlux.emitNext(trendInput);
        // Wait briefly for processing
        Thread.sleep(1);
    }

    @Benchmark
    public void benchmarkComplexRuleChain() throws InterruptedException {
        // Simulate a complex rule chain evaluation
        inputDirectFlux.emitNext(crossedInput);
        inputDirectFlux.emitNext(limitInput);
        // Wait briefly for processing
        Thread.sleep(1);
    }
}