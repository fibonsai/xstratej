
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
import com.fibonsai.cryptomeria.xtratej.event.series.impl.BooleanSingleTimeSeries.BooleanSingle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

class RandomRuleTest {

    private AutoCloseable closeable;

    @Mock
    private ITemporalData mockTimeSeries;

    private RandomRule randomRule;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        randomRule = new RandomRule();
        randomRule.watch(new Fifo<>());
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

        ITemporalData[] input = {mockTimeSeries};

        // Act
        Function<ITemporalData[], BooleanSingle[]> predicate = randomRule.predicate();
        BooleanSingle[] result = predicate.apply(input);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals(expectedTimestamp, result[0].timestamp());
    }

    @Test
    void predicate_whenNoSources_shouldReturnEmptyArray() {
        // Arrange
        randomRule = new RandomRule();
        ITemporalData[] input = {mockTimeSeries};

        // Act
        Function<ITemporalData[], BooleanSingle[]> predicate = randomRule.predicate();
        BooleanSingle[] result = predicate.apply(input);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.length);
    }
}
