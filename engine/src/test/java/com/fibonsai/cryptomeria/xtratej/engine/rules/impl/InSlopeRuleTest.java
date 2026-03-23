
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
import com.fibonsai.cryptomeria.xtratej.event.reactive.DirectFlux;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.BooleanTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.DoubleTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.TimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.builders.BooleanTimeSeriesBuilder;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.builders.DoubleTimeSeriesBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("SwitchStatementWithTooFewBranches")
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

    private DoubleTimeSeries createDoubleTimeSeries(String name, long[] timestamps, double[] values) {
        DoubleTimeSeriesBuilder builder = new DoubleTimeSeriesBuilder().setId(name);
        for (int i = 0; i < values.length; i++) {
            builder.add(timestamps[i], values[i]);
        }
        return builder.build();
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
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1, 2, 3}, new double[]{1, 2, 3}); // Slope = 1.0
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_slopeBelowMin_shouldReturnFalse() {
        params.put("minSlope", 1.5);
        InSlopeRule rule = switch (RuleType.InSlope.build().setParams(params)) {
            case InSlopeRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1, 2, 3}, new double[]{1, 2, 3}); // Slope = 1.0
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertFalse(result[0].values()[0]);
    }

    @Test
    void predicate_slopeAboveMax_shouldReturnFalse() {
        params.put("maxSlope", 0.5);
        InSlopeRule rule = switch (RuleType.InSlope.build().setParams(params)) {
            case InSlopeRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1, 2, 3}, new double[]{1, 2, 3}); // Slope = 1.0
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertFalse(result[0].values()[0]);
    }

    @Test
    void predicate_noSources_shouldReturnEmptyArray() {
        InSlopeRule rule = switch (RuleType.InSlope.build().setParams(params)) {
            case InSlopeRule r -> r;
            default -> throw new RuntimeException();
        };

        TimeSeries[] input = new TimeSeries[]{};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertEquals(0, result.length);
    }

    @Test
    void predicate_minSlopeOnly_noMax() {
        params.put("minSlope", 0.5);
        InSlopeRule rule = switch (RuleType.InSlope.build().setParams(params)) {
            case InSlopeRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1, 2, 3}, new double[]{1, 2, 3}); // Slope = 1.0
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_maxSlopeOnly_noMin() {
        params.put("maxSlope", 1.5);
        InSlopeRule rule = switch (RuleType.InSlope.build().setParams(params)) {
            case InSlopeRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1, 2, 3}, new double[]{1, 2, 3}); // Slope = 1.0
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_negativeSlope_belowMin() {
        params.put("minSlope", 0.5);
        InSlopeRule rule = switch (RuleType.InSlope.build().setParams(params)) {
            case InSlopeRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1, 2, 3}, new double[]{3, 2, 1}); // Slope = -1.0
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertFalse(result[0].values()[0]);
    }

    @Test
    void predicate_negativeSlope_withinNegativeRange() {
        params.put("minSlope", -2.0);
        params.put("maxSlope", -0.5);
        InSlopeRule rule = switch (RuleType.InSlope.build().setParams(params)) {
            case InSlopeRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1, 2, 3}, new double[]{3, 2, 1}); // Slope = -1.0
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_zeroSlope() {
        params.put("minSlope", 0.0);
        params.put("maxSlope", 1.0);
        InSlopeRule rule = switch (RuleType.InSlope.build().setParams(params)) {
            case InSlopeRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1, 2, 3}, new double[]{50.0, 50.0, 50.0}); // Slope = 0.0
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_verySmallSlope() {
        params.put("minSlope", 0.001);
        InSlopeRule rule = switch (RuleType.InSlope.build().setParams(params)) {
            case InSlopeRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1, 2, 3}, new double[]{0.0, 0.001, 0.002}); // Slope = 0.001
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_exactlyAtMinSlope() {
        params.put("minSlope", 1.0);
        params.put("maxSlope", 2.0);
        InSlopeRule rule = switch (RuleType.InSlope.build().setParams(params)) {
            case InSlopeRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1, 2, 3}, new double[]{1, 2, 3}); // Slope = 1.0
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_exactlyAtMaxSlope() {
        params.put("minSlope", 1.0);
        params.put("maxSlope", 1.0);
        InSlopeRule rule = switch (RuleType.InSlope.build().setParams(params)) {
            case InSlopeRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1, 2, 3}, new double[]{1, 2, 3}); // Slope = 1.0
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_noMinNoMax_shouldReturnFalse() {
        // When neither minSlope nor maxSlope is set, should return false
        InSlopeRule rule = switch (RuleType.InSlope.build().setParams(params)) {
            case InSlopeRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1, 2, 3}, new double[]{1, 2, 3});
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertFalse(result[0].values()[0]);
    }

    @Test
    void predicate_negativeValues_withinRange() {
        params.put("minSlope", -2.0);
        params.put("maxSlope", 2.0);
        InSlopeRule rule = switch (RuleType.InSlope.build().setParams(params)) {
            case InSlopeRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1, 2, 3}, new double[]{-1, 0, 1}); // Slope = 0.5
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_singleValueSeries() {
        params.put("minSlope", 0.0);
        params.put("maxSlope", 1.0);
        InSlopeRule rule = switch (RuleType.InSlope.build().setParams(params)) {
            case InSlopeRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1}, new double[]{50.0});
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertEquals(1, result.length);
        // Single value has undefined slope, should return false
        assertFalse(result[0].values()[0]);
    }

    @Test
    void predicate_twoValueSeries() {
        params.put("minSlope", 0.0);
        params.put("maxSlope", 10.0);
        InSlopeRule rule = switch (RuleType.InSlope.build().setParams(params)) {
            case InSlopeRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1, 2}, new double[]{10.0, 20.0}); // Slope = 10.0
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_withNonDoubleSeries_ignoresThem() {
        params.put("minSlope", 0.0);
        params.put("maxSlope", 1.0);
        InSlopeRule rule = switch (RuleType.InSlope.build().setParams(params)) {
            case InSlopeRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries booleanSeries = new BooleanTimeSeriesBuilder().setId("s1").add(1, true).build();
        TimeSeries[] input = new TimeSeries[]{booleanSeries};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertEquals(1, result.length);
        assertFalse(result[0].values()[0]);
    }

    @Test
    void predicate_emptySeriesHandling() {
        params.put("minSlope", 0.0);
        params.put("maxSlope", 1.0);
        InSlopeRule rule = switch (RuleType.InSlope.build().setParams(params)) {
            case InSlopeRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{}, new double[]{});
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertEquals(1, result.length);
        assertFalse(result[0].values()[0]);
    }

    @Test
    void predicate_multipleValues_withInRange() {
        params.put("minSlope", 0.5);
        params.put("maxSlope", 1.5);
        InSlopeRule rule = switch (RuleType.InSlope.build().setParams(params)) {
            case InSlopeRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        // Values with slope ~1.0
        DoubleTimeSeries series = createDoubleTimeSeries("s1",
                new long[]{1, 2, 3, 4, 5},
                new double[]{1, 2, 3, 4, 5});
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[0]);
    }
}
