
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

import com.fibonsai.cryptomeria.xtratej.event.ITemporalData;
import com.fibonsai.cryptomeria.xtratej.event.reactive.Fifo;
import com.fibonsai.cryptomeria.xtratej.event.series.impl.BooleanSingleTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.impl.BooleanSingleTimeSeries.BooleanSingle;
import com.fibonsai.cryptomeria.xtratej.event.series.impl.SingleTimeSeries;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class AndRuleTest {

    private AutoCloseable closeable;

    private AndRule andRule;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        andRule = new AndRule();
        andRule.watch(new Fifo<>());
    }

    @AfterEach
    void finish() {
        try {
            closeable.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private BooleanSingleTimeSeries createBooleanSeries(String name, long timestamp, boolean... values) {
        BooleanSingle[] singles = new BooleanSingle[values.length];
        for (int i = 0; i < values.length; i++) {
            singles[i] = new BooleanSingle(timestamp + i, values[i]);
        }
        return new BooleanSingleTimeSeries(name, singles);
    }

    @Test
    void predicate_withAllTrue_shouldReturnTrue() {
        // Arrange
        ITemporalData series1 = createBooleanSeries("s1", 100L, true, true);
        ITemporalData series2 = createBooleanSeries("s2", 101L, true);
        ITemporalData[] input = {series1, series2};

        // Act
        Function<ITemporalData[], BooleanSingle[]> predicate = andRule.predicate();
        BooleanSingle[] result = predicate.apply(input);

        // Assert
        assertEquals(1, result.length);
        assertTrue(result[0].value());
        assertEquals(101L, result[0].timestamp());
    }

    @Test
    void predicate_withOneFalse_shouldReturnFalse() {
        // Arrange
        ITemporalData series1 = createBooleanSeries("s1", 100L, true, true);
        ITemporalData series2 = createBooleanSeries("s2", 101L, false);
        ITemporalData[] input = {series1, series2};

        // Act
        Function<ITemporalData[], BooleanSingle[]> predicate = andRule.predicate();
        BooleanSingle[] result = predicate.apply(input);

        // Assert
        assertEquals(1, result.length);
        assertFalse(result[0].value());
        assertEquals(101L, result[0].timestamp());
    }

    @Test
    void predicate_withAllFalse_shouldReturnFalse() {
        // Arrange
        ITemporalData series1 = createBooleanSeries("s1", 100L, false);
        ITemporalData series2 = createBooleanSeries("s2", 101L, false);
        ITemporalData[] input = {series1, series2};

        // Act
        Function<ITemporalData[], BooleanSingle[]> predicate = andRule.predicate();
        BooleanSingle[] result = predicate.apply(input);

        // Assert
        assertEquals(1, result.length);
        assertFalse(result[0].value());
        assertEquals(101L, result[0].timestamp());
    }

    @Test
    void predicate_shouldIgnoreNonBooleanITemporalData() {
        // Arrange
        ITemporalData series1 = createBooleanSeries("s1", 100L, true);
        ITemporalData series2 = new SingleTimeSeries("s2", new SingleTimeSeries.Single[]{new SingleTimeSeries.Single(101L, 1.0)});
        ITemporalData[] input = {series1, series2};

        // Act
        Function<ITemporalData[], BooleanSingle[]> predicate = andRule.predicate();
        BooleanSingle[] result = predicate.apply(input);

        // Assert
        assertEquals(1, result.length);
        assertTrue(result[0].value());
        assertEquals(100L, result[0].timestamp());
    }
}
