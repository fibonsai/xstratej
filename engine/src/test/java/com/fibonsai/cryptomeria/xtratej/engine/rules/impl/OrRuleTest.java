
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

class OrRuleTest {

    private AutoCloseable closeable;

    private OrRule orRule;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        orRule = new OrRule();
        orRule.watch(new DirectFlux<>());
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
    void predicate_withAllTrue_shouldReturnTrue() {
        TimeSeries series1 = createBooleanSeries("s1", 100L, true);
        TimeSeries series2 = createBooleanSeries("s2", 101L, true);
        TimeSeries[] input = new TimeSeries[]{series1, series2};

        BooleanTimeSeries[] result = orRule.predicate().apply(input);

        assertEquals(1, result.length);
        assertTrue(result[0].values()[0]);
        assertEquals(101L, result[0].timestamp());
    }

    @Test
    void predicate_withOneTrue_shouldReturnTrue() {
        TimeSeries series1 = createBooleanSeries("s1", 100L, true);
        TimeSeries series2 = createBooleanSeries("s2", 101L, false);
        TimeSeries[] input = new TimeSeries[]{series1, series2};

        BooleanTimeSeries[] result = orRule.predicate().apply(input);

        assertEquals(1, result.length);
        assertTrue(result[0].values()[0]);
        assertEquals(101L, result[0].timestamp());
    }

    @Test
    void predicate_withAllFalse_shouldReturnFalse() {
        TimeSeries series1 = createBooleanSeries("s1", 100L, false);
        TimeSeries series2 = createBooleanSeries("s2", 101L, false);
        TimeSeries[] input = new TimeSeries[]{series1, series2};

        BooleanTimeSeries[] result = orRule.predicate().apply(input);

        assertEquals(1, result.length);
        assertFalse(result[0].values()[0]);
        assertEquals(101L, result[0].timestamp());
    }

    @Test
    void predicate_withFourSources_mixedValues() {
        TimeSeries series1 = createBooleanSeries("s1", 100L, true);
        TimeSeries series2 = createBooleanSeries("s2", 101L, false);
        TimeSeries series3 = createBooleanSeries("s3", 102L, false);
        TimeSeries series4 = createBooleanSeries("s4", 103L, true);
        TimeSeries[] input = new TimeSeries[]{series1, series2, series3, series4};

        BooleanTimeSeries[] result = orRule.predicate().apply(input);

        assertEquals(1, result.length);
        assertTrue(result[0].values()[0]);
        assertEquals(103L, result[0].timestamp());
    }

    @Test
    void predicate_withMultipleValuesPerSeries() {
        BooleanTimeSeries series1 = createBooleanSeries("s1", 100L, false, false, true);
        BooleanTimeSeries series2 = createBooleanSeries("s2", 101L, false, false, false);
        TimeSeries[] input = new TimeSeries[]{series1, series2};

        BooleanTimeSeries[] result = orRule.predicate().apply(input);

        assertEquals(1, result.length);
        assertTrue(result[0].values()[0]);
        assertEquals(101L + series1.values().length - 1, result[0].timestamp());
    }

    @Test
    void predicate_withNonBooleanTimeSeries_ignoresThem() {
        TimeSeries series1 = createBooleanSeries("s1", 100L, true);
        TimeSeries series2 = new DoubleTimeSeriesBuilder().setId("s2").add(101L, 50.0).build();
        TimeSeries[] input = new TimeSeries[]{series1, series2};

        BooleanTimeSeries[] result = orRule.predicate().apply(input);

        assertEquals(1, result.length);
        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_withAllNonBoolean_returnsEmpty() {
        TimeSeries series1 = new DoubleTimeSeriesBuilder().setId("s1").add(100L, 50.0).build();
        TimeSeries series2 = new DoubleTimeSeriesBuilder().setId("s2").add(101L, 60.0).build();
        TimeSeries[] input = new TimeSeries[]{series1, series2};

        BooleanTimeSeries[] result = orRule.predicate().apply(input);

        assertEquals(1, result.length);
        assertFalse(result[0].values()[0]);
    }

    @Test
    void predicate_withNullValueInArray_handling() {
        TimeSeries series1 = createBooleanSeries("s1", 100L, true);
        TimeSeries[] input = new TimeSeries[]{series1, null};

        BooleanTimeSeries[] result = orRule.predicate().apply(input);

        assertEquals(1, result.length);
        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_withEmptyArrayHandling() {
        TimeSeries[] input = new TimeSeries[]{};

        BooleanTimeSeries[] result = orRule.predicate().apply(input);

        assertEquals(0, result.length);
    }

    @Test
    void predicate_notActivated_returnsEmpty() {
        OrRule inactiveRule = new OrRule();
        TimeSeries series = createBooleanSeries("s1", 100L, true);
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = inactiveRule.predicate().apply(input);

        assertEquals(0, result.length);
    }
}
