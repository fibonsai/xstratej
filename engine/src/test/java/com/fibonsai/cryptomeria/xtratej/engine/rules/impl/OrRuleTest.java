
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

import com.fibonsai.cryptomeria.xtratej.event.reactive.Fifo;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.BooleanTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.TimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.builders.BooleanTimeSeriesBuilder;
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
        orRule.watch(new Fifo<>());
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
}
