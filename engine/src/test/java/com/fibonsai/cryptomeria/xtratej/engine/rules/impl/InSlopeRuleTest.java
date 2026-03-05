
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

import com.fibonsai.cryptomeria.xtratej.engine.rules.RuleType;
import com.fibonsai.cryptomeria.xtratej.event.ITemporalData;
import com.fibonsai.cryptomeria.xtratej.event.reactive.Fifo;
import com.fibonsai.cryptomeria.xtratej.event.series.impl.BooleanSingleTimeSeries.BooleanSingle;
import com.fibonsai.cryptomeria.xtratej.event.series.impl.SingleTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.impl.SingleTimeSeries.Single;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.*;

class InSlopeRuleTest {

    private AutoCloseable closeable;

    private ObjectNode params;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
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

    private SingleTimeSeries createSingleTimeSeries(String name, long[] timestamps, double[] values) {
        Single[] singles = new Single[values.length];
        for (int i = 0; i < values.length; i++) {
            singles[i] = new Single(timestamps[i], values[i]);
        }
        return new SingleTimeSeries(name, singles);
    }

    @Test
    void processParams_shouldSetSlopes() {
        params.put("minSlope", 0.5);
        params.put("maxSlope", 1.5);
        InSlopeRule rule = RuleType.InSlope.build().setParams(params) instanceof InSlopeRule r ? r : null;
        assertNotNull(rule);
    }

    @Test
    void predicate_slopeWithinRange_shouldReturnTrue() {
        params.put("minSlope", 0.5);
        params.put("maxSlope", 1.5);
        InSlopeRule rule = switch (RuleType.InSlope.build().setParams(params)) {
            case InSlopeRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new Fifo<>());

        ITemporalData series = createSingleTimeSeries("s1", new long[]{1, 2, 3}, new double[]{1, 2, 3}); // Slope = 1.0
        ITemporalData[] input = new ITemporalData[]{series};

        BooleanSingle[] result = rule.predicate().apply(input);

        assertTrue(result[0].value());
    }

    @Test
    void predicate_slopeBelowMin_shouldReturnFalse() {
        params.put("minSlope", 1.5);
        InSlopeRule rule = switch (RuleType.InSlope.build().setParams(params)) {
            case InSlopeRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new Fifo<>());

        ITemporalData series = createSingleTimeSeries("s1", new long[]{1, 2, 3}, new double[]{1, 2, 3}); // Slope = 1.0
        ITemporalData[] input = new ITemporalData[]{series};

        BooleanSingle[] result = rule.predicate().apply(input);

        assertFalse(result[0].value());
    }

    @Test
    void predicate_slopeAboveMax_shouldReturnFalse() {
        params.put("maxSlope", 0.5);
        InSlopeRule rule = switch (RuleType.InSlope.build().setParams(params)) {
            case InSlopeRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new Fifo<>());

        ITemporalData series = createSingleTimeSeries("s1", new long[]{1, 2, 3}, new double[]{1, 2, 3}); // Slope = 1.0
        ITemporalData[] input = new ITemporalData[]{series};

        BooleanSingle[] result = rule.predicate().apply(input);

        assertFalse(result[0].value());
    }

    @Test
    void predicate_noSources_shouldReturnEmptyArray() {
        InSlopeRule rule = switch (RuleType.InSlope.build().setParams(params)) {
            case InSlopeRule r -> r;
            default -> throw new RuntimeException();
        };

        ITemporalData[] input = new ITemporalData[]{};

        BooleanSingle[] result = rule.predicate().apply(input);

        assertEquals(0, result.length);
    }
}
