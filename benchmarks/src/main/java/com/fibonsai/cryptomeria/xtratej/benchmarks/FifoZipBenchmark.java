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

import com.fibonsai.cryptomeria.xtratej.event.reactive.Fifo;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 1, jvmArgs = {"-Xms2G", "-Xmx2G"})
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
public class FifoZipBenchmark {

    @Benchmark
    public Fifo<String[]> benchmarkZipTwoSources() {
        Fifo<String> r1 = new Fifo<>();
        Fifo<String> r2 = new Fifo<>();

        return Fifo.zip(r1, r2);
    }

    @Benchmark
    public Fifo<String[]> benchmarkZipThreeSources() {
        Fifo<String> r1 = new Fifo<>();
        Fifo<String> r2 = new Fifo<>();
        Fifo<String> r3 = new Fifo<>();

        return Fifo.zip(r1, r2, r3);
    }

    @Benchmark
    public Fifo<Integer[]> benchmarkZipFiveSources() {
        Fifo<Integer> r1 = new Fifo<>();
        Fifo<Integer> r2 = new Fifo<>();
        Fifo<Integer> r3 = new Fifo<>();
        Fifo<Integer> r4 = new Fifo<>();
        Fifo<Integer> r5 = new Fifo<>();

        return Fifo.zip(r1, r2, r3, r4, r5);
    }

    @Benchmark
    public void benchmarkZipWithEmission() throws InterruptedException {
        Fifo<String> r1 = new Fifo<>();
        Fifo<String> r2 = new Fifo<>();
        Fifo<String[]> zipped = Fifo.zip(r1, r2);

        // Subscribe to consume the zipped output
        CompletableFuture<String[]> resultFuture = new CompletableFuture<>();
        zipped.subscribe(resultFuture::complete);

        // Emit values
        r1.emitNext("value1");
        r2.emitNext("value2");

        // Wait briefly for completion
        Thread.sleep(1);
    }

    @Benchmark
    public void benchmarkZipHighThroughput() throws InterruptedException {
        Fifo<Integer> r1 = new Fifo<>();
        Fifo<Integer> r2 = new Fifo<>();
        Fifo<Integer[]> zipped = Fifo.zip(r1, r2);

        // Subscribe to consume the zipped output
        zipped.subscribe(arr -> {});

        // Emit many values quickly
        for (int i = 0; i < 100; i++) {
            r1.emitNext(i);
            r2.emitNext(i + 100);
        }

        Thread.sleep(1);
    }
}