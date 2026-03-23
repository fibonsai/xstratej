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
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class FalseRuleTest {

    private AutoCloseable closeable;

    private FalseRule falseRule;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        falseRule = new FalseRule();
        falseRule.watch(new DirectFlux<>());
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
    void predicate_alwaysReturnsFalse() {
        TimeSeries series = new DoubleTimeSeriesBuilder().setId("test").add(100L, 50.0).build();
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = falseRule.predicate().apply(input);

        assertEquals(1, result.length);
        assertFalse(result[0].values()[0]);
    }

    @Test
    void predicate_withMultipleSources_alwaysReturnsFalse() {
        TimeSeries series1 = new DoubleTimeSeriesBuilder().setId("s1").add(100L, 50.0).build();
        TimeSeries series2 = new DoubleTimeSeriesBuilder().setId("s2").add(101L, 60.0).build();
        TimeSeries series3 = new DoubleTimeSeriesBuilder().setId("s3").add(102L, 70.0).build();
        TimeSeries[] input = new TimeSeries[]{series1, series2, series3};

        BooleanTimeSeries[] result = falseRule.predicate().apply(input);

        assertEquals(1, result.length);
        assertFalse(result[0].values()[0]);
    }

    @Test
    void predicate_timestampAggregation_selectsLatestTimestamp() {
        TimeSeries series1 = new DoubleTimeSeriesBuilder().setId("s1").add(100L, 50.0).build();
        TimeSeries series2 = new DoubleTimeSeriesBuilder().setId("s2").add(200L, 60.0).build();
        TimeSeries series3 = new DoubleTimeSeriesBuilder().setId("s3").add(150L, 70.0).build();
        TimeSeries[] input = new TimeSeries[]{series1, series2, series3};

        BooleanTimeSeries[] result = falseRule.predicate().apply(input);

        assertEquals(200L, result[0].timestamp());
    }

    @Test
    void predicate_singleSource_timestampFromThatSource() {
        long expectedTimestamp = 42L;
        TimeSeries series = new DoubleTimeSeriesBuilder().setId("test").add(expectedTimestamp, 50.0).build();
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = falseRule.predicate().apply(input);

        assertEquals(expectedTimestamp, result[0].timestamp());
    }

    @Test
    void predicate_noSources_logsWarning() {
        TimeSeries[] input = new TimeSeries[]{};

        BooleanTimeSeries[] result = falseRule.predicate().apply(input);

        assertEquals(0, result.length);
    }

    @Test
    void predicate_noSources_returnsEmptyArray() {
        FalseRule rule = new FalseRule();

        TimeSeries[] input = new TimeSeries[]{};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertEquals(0, result.length);
    }

    @Test
    void predicate_withEmptyTimeSeries() {
        TimeSeries emptySeries = new DoubleTimeSeriesBuilder().setId("empty").build();
        TimeSeries[] input = new TimeSeries[]{emptySeries};

        BooleanTimeSeries[] result = falseRule.predicate().apply(input);

        assertEquals(1, result.length);
        assertFalse(result[0].values()[0]);
        assertEquals(0, result[0].timestamp());
    }

    @Test
    void predicate_mixedEmptyAndNonEmptySeries() {
        TimeSeries emptySeries = new DoubleTimeSeriesBuilder().setId("empty").build();
        TimeSeries nonEmptySeries = new DoubleTimeSeriesBuilder().setId("nonEmpty").add(100L, 50.0).build();
        TimeSeries[] input = new TimeSeries[]{emptySeries, nonEmptySeries};

        BooleanTimeSeries[] result = falseRule.predicate().apply(input);

        assertEquals(1, result.length);
        assertFalse(result[0].values()[0]);
        assertEquals(100L, result[0].timestamp());
    }

    @Test
    void predicate_ignoresNonBooleanTimeSeries() {
        TimeSeries doubleSeries = new DoubleTimeSeriesBuilder().setId("double").add(100L, 50.0).build();
        TimeSeries[] input = new TimeSeries[]{doubleSeries};

        BooleanTimeSeries[] result = falseRule.predicate().apply(input);

        assertEquals(1, result.length);
        assertFalse(result[0].values()[0]);
    }

    @Test
    void predicate_withMultiValueTimeSeries() {
        DoubleTimeSeriesBuilder builder = new DoubleTimeSeriesBuilder().setId("multi");
        for (int i = 0; i < 10; i++) {
            builder.add(100L, i * 10.0);
        }
        TimeSeries series = builder.build();
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = falseRule.predicate().apply(input);

        assertEquals(1, result.length);
        assertFalse(result[0].values()[0]);
    }

    @Test
    void predicate_notActivated_returnsEmptyArray() {
        FalseRule inactiveRule = new FalseRule();
        TimeSeries series = new DoubleTimeSeriesBuilder().setId("test").add(100L, 50.0).build();
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = inactiveRule.predicate().apply(input);

        assertEquals(0, result.length);
    }

    @Test
    void predicate_activated_returnsFalseWithTimestamp() {
        TimeSeries series = new DoubleTimeSeriesBuilder().setId("test").add(100L, 50.0).build();
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = falseRule.predicate().apply(input);

        assertEquals(1, result.length);
        assertFalse(result[0].values()[0]);
        assertEquals(100L, result[0].timestamp());
    }

    @Test
    void predicate_multipleCalls_sameBehavior() {
        TimeSeries series = new DoubleTimeSeriesBuilder().setId("test").add(100L, 50.0).build();
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result1 = falseRule.predicate().apply(input);
        BooleanTimeSeries[] result2 = falseRule.predicate().apply(input);
        BooleanTimeSeries[] result3 = falseRule.predicate().apply(input);

        for (BooleanTimeSeries[] result : new BooleanTimeSeries[][]{result1, result2, result3}) {
            assertEquals(1, result.length);
            assertFalse(result[0].values()[0]);
        }
    }
}
