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

import com.fibonsai.cryptomeria.xtratej.event.series.dao.BooleanTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.DoubleTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.builders.BooleanTimeSeriesBuilder;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.builders.DoubleTimeSeriesBuilder;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 1, jvmArgs = {"-Xms2G", "-Xmx2G"})
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
public class TimeSeriesBenchmark {

    @Benchmark
    public DoubleTimeSeries benchmarkBuildDoubleTimeSeries() {
        DoubleTimeSeriesBuilder builder = new DoubleTimeSeriesBuilder().setId("test");
        for (int i = 0; i < 100; i++) {
            builder.add(System.currentTimeMillis() + i, Math.random() * 100);
        }
        return builder.build();
    }

    @Benchmark
    public BooleanTimeSeries benchmarkBuildBooleanTimeSeries() {
        BooleanTimeSeriesBuilder builder = new BooleanTimeSeriesBuilder().setId("test");
        for (int i = 0; i < 100; i++) {
            builder.add(System.currentTimeMillis() + i, Math.random() > 0.5);
        }
        return builder.build();
    }

    @Benchmark
    public double[] benchmarkDoubleTimeSeriesValuesAccess() {
        DoubleTimeSeries series = createLargeDoubleTimeSeries();
        return series.values();
    }

    @Benchmark
    public long[] benchmarkDoubleTimeSeriesTimestampsAccess() {
        DoubleTimeSeries series = createLargeDoubleTimeSeries();
        return series.timestamps();
    }

    @Benchmark
    public double benchmarkDoubleTimeSeriesLatestValue() {
        DoubleTimeSeries series = createLargeDoubleTimeSeries();
        return series.values()[series.values().length - 1];
    }

    @Benchmark
    public BooleanTimeSeries benchmarkBuildBooleanTimeSeriesWithMixedValues() {
        BooleanTimeSeriesBuilder builder = new BooleanTimeSeriesBuilder().setId("mixed");
        for (int i = 0; i < 1000; i++) {
            builder.add(System.currentTimeMillis() + i, i % 2 == 0);
        }
        return builder.build();
    }

    @Benchmark
    public DoubleTimeSeries benchmarkBuildLargeDoubleTimeSeries() {
        DoubleTimeSeriesBuilder builder = new DoubleTimeSeriesBuilder().setId("large");
        for (int i = 0; i < 10000; i++) {
            builder.add(System.currentTimeMillis() + i, Math.random() * 100);
        }
        return builder.build();
    }

    private DoubleTimeSeries createLargeDoubleTimeSeries() {
        DoubleTimeSeriesBuilder builder = new DoubleTimeSeriesBuilder().setId("large");
        for (int i = 0; i < 1000; i++) {
            builder.add(System.currentTimeMillis() + i, Math.random() * 100);
        }
        return builder.build();
    }
}