
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
import com.fibonsai.cryptomeria.xtratej.event.series.dao.builders.BooleanTimeSeriesBuilder;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.builders.DoubleTimeSeriesBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;

class NotRuleTest {

    private AutoCloseable closeable;

    private NotRule notRule;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        notRule = new NotRule();
        notRule.watch(new DirectFlux<>());
    }

    @AfterEach
    void finish() {
        try {
            closeable.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private BooleanTimeSeries createBooleanSeries(String name, long timestamp, boolean... values) {
        BooleanTimeSeriesBuilder builder = new BooleanTimeSeriesBuilder().setId(name);
        for (int i = 0; i < values.length; i++) {
            builder.add(timestamp + i, values[i]);
        }
        return builder.build();
    }

    @Test
    void predicate_withTrue_shouldReturnFalse() {
        TimeSeries series = createBooleanSeries("s1", 100L, true);
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = notRule.predicate().apply(input);

        assertEquals(1, result.length);
        assertFalse(result[0].values()[0]);
        assertEquals(100L, result[0].timestamp());
    }

    @Test
    void predicate_withFalse_shouldReturnTrue() {
        TimeSeries series = createBooleanSeries("s1", 100L, false);
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = notRule.predicate().apply(input);

        assertEquals(1, result.length);
        assertTrue(result[0].values()[0]);
        assertEquals(100L, result[0].timestamp());
    }

    @Test
    void predicate_withMultipleTimeSeries_shouldReturnEmptyArray() {
        notRule = new NotRule();
        
        TimeSeries series1 = createBooleanSeries("s1", 100L, true);
        TimeSeries series2 = createBooleanSeries("s2", 101L, false);
        TimeSeries[] input = new TimeSeries[]{series1, series2};

        BooleanTimeSeries[] result = notRule.predicate().apply(input);

        assertEquals(0, result.length);
    }

    @Test
    void predicate_withNonBooleanTimeSeries_shouldReturnEmptyArray() {
        TimeSeries series = new DoubleTimeSeriesBuilder().setId("s1").add(100L, 1.0).build();
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = notRule.predicate().apply(input);

        assertEquals(0, result.length);
    }

    @Test
    void predicate_withEmptyArrayHandling() {
        TimeSeries[] input = new TimeSeries[]{};

        BooleanTimeSeries[] result = notRule.predicate().apply(input);

        assertEquals(0, result.length);
    }

    @Test
    void predicate_withNullValueInArray_handling() {
        TimeSeries series = createBooleanSeries("s1", 100L, true);
        TimeSeries[] input = new TimeSeries[]{series, null};

        BooleanTimeSeries[] result = notRule.predicate().apply(input);

        assertEquals(0, result.length);
    }

    @Test
    void predicate_withMultipleBooleanValues() {
        BooleanTimeSeries series = createBooleanSeries("s1", 100L, true, false, true);
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = notRule.predicate().apply(input);

        assertEquals(1, result.length);
    }

    @Test
    void predicate_notActivated_returnsEmpty() {
        NotRule inactiveRule = new NotRule();
        TimeSeries series = createBooleanSeries("s1", 100L, true);
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = inactiveRule.predicate().apply(input);

        assertEquals(0, result.length);
    }

    @Test
    void predicate_emptyValuesArray_handling() {
        BooleanTimeSeries series = new BooleanTimeSeriesBuilder().setId("s1").build();
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = notRule.predicate().apply(input);

        assertEquals(0, result.length);
    }

    @Test
    void predicate_withSingleFalseValue() {
        BooleanTimeSeries series = createBooleanSeries("s1", 100L, false);
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = notRule.predicate().apply(input);

        assertEquals(1, result.length);
        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_withSingleTrueValue() {
        BooleanTimeSeries series = createBooleanSeries("s1", 100L, true);
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = notRule.predicate().apply(input);

        assertEquals(1, result.length);
        assertFalse(result[0].values()[0]);
    }
}
