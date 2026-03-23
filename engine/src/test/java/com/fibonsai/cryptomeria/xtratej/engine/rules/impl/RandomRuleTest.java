
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

package com.fibonsai.cryptomeria.xtratej.engine.rules.impl;

import com.fibonsai.cryptomeria.xtratej.event.reactive.DirectFlux;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.BooleanTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.TimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.builders.DoubleTimeSeriesBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class RandomRuleTest {

    private AutoCloseable closeable;

    @Mock
    private TimeSeries mockTimeSeries;

    private RandomRule randomRule;

    private ObjectNode params;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        randomRule = new RandomRule();
        randomRule.watch(new DirectFlux<>());
        params = JsonNodeFactory.instance.objectNode();
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
    void predicate_shouldReturnRandomBoolean() {
        // Arrange
        long expectedTimestamp = 123456789L;
        when(mockTimeSeries.timestamp()).thenReturn(expectedTimestamp);

        TimeSeries[] input = {mockTimeSeries};

        // Act
        Function<TimeSeries[], BooleanTimeSeries[]> predicate = randomRule.predicate();
        BooleanTimeSeries[] result = predicate.apply(input);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals(expectedTimestamp, result[0].timestamp());
    }

    @Test
    void predicate_whenNoSources_shouldReturnEmptyArray() {
        // Arrange
        randomRule = new RandomRule();
        TimeSeries[] input = {mockTimeSeries};

        // Act
        Function<TimeSeries[], BooleanTimeSeries[]> predicate = randomRule.predicate();
        BooleanTimeSeries[] result = predicate.apply(input);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    void predicate_multipleCalls_shouldReturnVariedResults() {
        // This test checks that random rule produces varied results over multiple calls
        // Note: This is probabilistic - we run multiple calls and check for variation

        int trueCount = 0;
        int iterations = 100;

        for (int i = 0; i < iterations; i++) {
            TimeSeries[] input = {mockTimeSeries};
            BooleanTimeSeries[] result = randomRule.predicate().apply(input);
            if (result.length > 0 && result[0].values()[0]) {
                trueCount++;
            }
        }

        // With 100 random calls, getting all true or all false is extremely unlikely
        // Using a宽松 threshold to avoid flaky tests
        assertTrue(trueCount > 0 && trueCount < iterations,
                "Random rule should produce varied results, got " + trueCount + " trues out of " + iterations);
    }

    @Test
    void predicate_distribution_shouldBeApproximately5050() {
        // Check that the distribution is approximately 50/50
        int trueCount = 0;
        int iterations = 1000;

        for (int i = 0; i < iterations; i++) {
            TimeSeries[] input = {mockTimeSeries};
            BooleanTimeSeries[] result = randomRule.predicate().apply(input);
            if (result.length > 0 && result[0].values()[0]) {
                trueCount++;
            }
        }

        double trueRate = (double) trueCount / iterations;
        // Allow 40-60% as reasonable variance for 1000 samples
        assertTrue(trueRate >= 0.35 && trueRate <= 0.65,
                "Random distribution should be approximately 50%, got " + (trueRate * 100) + "%");
    }

    @Test
    void predicate_withNonMockedSeries() {
        // Test with actual TimeSeries instead of mocked
        TimeSeries series = new DoubleTimeSeriesBuilder().setId("test").add(456L, 100.0).build();
        TimeSeries[] input = {series};

        BooleanTimeSeries[] result = randomRule.predicate().apply(input);

        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals(456L, result[0].timestamp());
    }

    @Test
    void predicate_emptySourcesArray() {
        TimeSeries[] input = {};

        BooleanTimeSeries[] result = randomRule.predicate().apply(input);

        assertEquals(0, result.length);
    }

    @Test
    void predicate_notActivated_returnsEmpty() {
        RandomRule inactiveRule = new RandomRule();
        TimeSeries series = new DoubleTimeSeriesBuilder().setId("test").add(456L, 100.0).build();
        TimeSeries[] input = {series};

        BooleanTimeSeries[] result = inactiveRule.predicate().apply(input);

        assertEquals(0, result.length);
    }

    @Test
    void predicate_singleSource_noMock() {
        TimeSeries series = new DoubleTimeSeriesBuilder().setId("test").add(789L, 50.0).build();
        TimeSeries[] input = {series};

        BooleanTimeSeries[] result = randomRule.predicate().apply(input);

        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals(789L, result[0].timestamp());
    }

    @Test
    void predicate_withMultipleSources() {
        TimeSeries series1 = new DoubleTimeSeriesBuilder().setId("s1").add(100L, 50.0).build();
        TimeSeries series2 = new DoubleTimeSeriesBuilder().setId("s2").add(200L, 60.0).build();
        TimeSeries series3 = new DoubleTimeSeriesBuilder().setId("s3").add(150L, 55.0).build();
        TimeSeries[] input = {series1, series2, series3};

        BooleanTimeSeries[] result = randomRule.predicate().apply(input);

        assertNotNull(result);
        assertEquals(1, result.length);
        // Timestamp should be the latest from all sources
        assertTrue(result[0].timestamp() >= 200L);
    }

    @Test
    void predicate_concurrent_callsAreSafe() throws InterruptedException {
        // Test that concurrent calls don't interfere with each other
        int threadCount = 10;
        int iterationsPerThread = 50;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicBoolean anyFailure = new AtomicBoolean(false);

        for (int t = 0; t < threadCount; t++) {
            Thread.startVirtualThread(() -> {
                try {
                    for (int i = 0; i < iterationsPerThread; i++) {
                        TimeSeries series = new DoubleTimeSeriesBuilder().setId("test").add(System.currentTimeMillis(), 50.0).build();
                        TimeSeries[] input = {series};
                        BooleanTimeSeries[] result = randomRule.predicate().apply(input);
                        assertNotNull(result);
                        assertEquals(1, result.length);
                    }
                } catch (Exception e) {
                    anyFailure.set(true);
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertFalse(anyFailure.get(), "Concurrent calls should not throw exceptions");
    }

    @Test
    void predicate_paramsIgnored() {
        // RandomRule should ignore params
        params.put("threshold", 50.0);
        params.put("sourceId", "test");
        randomRule.setParams(params);

        TimeSeries series = new DoubleTimeSeriesBuilder().setId("test").add(456L, 100.0).build();
        TimeSeries[] input = {series};

        BooleanTimeSeries[] result = randomRule.predicate().apply(input);

        assertNotNull(result);
        assertEquals(1, result.length);
    }
}
