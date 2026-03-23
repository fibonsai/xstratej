
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
class TrendRuleTest {

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
    void processParams_shouldSetSourceIdAndIsRising() {
        params.put("sourceId", "s2");
        params.put("isRising", true);
        TrendRule rule = RuleType.Trend.build().setParams(params) instanceof TrendRule r ? r : null;
        assertNotNull(rule);
    }

    @Test
    void predicate_isRisingTrue_withRisingTrend_shouldReturnTrue() {
        params.put("isRising", true);
        TrendRule rule = switch (RuleType.Trend.build().setParams(params)) {
            case TrendRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1, 2, 3}, new double[]{1, 2, 3}); // Rising
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[result[0].values().length - 1]);
    }

    @Test
    void predicate_isRisingTrue_withFallingTrend_shouldReturnFalse() {
        params.put("isRising", true);
        TrendRule rule = switch (RuleType.Trend.build().setParams(params)) {
            case TrendRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1, 2, 3}, new double[]{3, 2, 1}); // Falling
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertFalse(result[0].values()[0]);
    }

    @Test
    void predicate_isRisingFalse_withFallingTrend_shouldReturnTrue() {
        params.put("isRising", false);
        TrendRule rule = switch (RuleType.Trend.build().setParams(params)) {
            case TrendRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1, 2, 3}, new double[]{3, 2, 1}); // Falling
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_compareWithSource_shouldReturnCorrectTrend() {
        params.put("sourceId", "s2");
        params.put("isRising", true); // s1 slope > s2 slope
        params.set("sources", JsonNodeFactory.instance.arrayNode().add("s1"));
        TrendRule rule = switch (RuleType.Trend.build().setParams(params)) {
            case TrendRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries s1 = createDoubleTimeSeries("s1", new long[]{1, 2, 3}, new double[]{1, 2, 3}); // Slope 1.0
        TimeSeries s2 = createDoubleTimeSeries("s2", new long[]{1, 2, 3}, new double[]{1, 1.5, 2}); // Slope 0.5
        TimeSeries[] input = new TimeSeries[]{s1, s2};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_noSources_shouldReturnEmptyArray() {
        TrendRule rule = switch (RuleType.Trend.build().setParams(params)) {
            case TrendRule r -> r;
            default -> throw new RuntimeException();
        };

        TimeSeries[] input = new TimeSeries[]{};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertEquals(0, result.length);
    }

    @Test
    void predicate_sourceIdNotInSources_returnsFalse() {
        params.put("sourceId", "nonExistent");
        params.put("isRising", true);
        TrendRule rule = switch (RuleType.Trend.build().setParams(params)) {
            case TrendRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1, 2, 3}, new double[]{1, 2, 3});
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertEquals(1, result.length);
        assertFalse(result[0].values()[0]);
    }

    @Test
    void predicate_emptySeries_handling() {
        params.put("isRising", true);
        TrendRule rule = switch (RuleType.Trend.build().setParams(params)) {
            case TrendRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries emptySeries = createDoubleTimeSeries("s1", new long[]{}, new double[]{});
        TimeSeries[] input = new TimeSeries[]{emptySeries};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertEquals(1, result.length);
        assertFalse(result[0].values()[0]);
    }

    @Test
    void predicate_singleValueSeries_handling() {
        params.put("isRising", true);
        TrendRule rule = switch (RuleType.Trend.build().setParams(params)) {
            case TrendRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1}, new double[]{50.0});
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertEquals(1, result.length);
        assertFalse(result[0].values()[0]);
    }

    @Test
    void predicate_identicalValues_zeroSlope() {
        params.put("isRising", true);
        TrendRule rule = switch (RuleType.Trend.build().setParams(params)) {
            case TrendRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1, 2, 3, 4}, new double[]{50.0, 50.0, 50.0, 50.0});
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertEquals(1, result.length);
        // Zero slope is not > 0, so should return false for rising
        assertFalse(result[0].values()[0]);
    }

    @Test
    void predicate_negativeSlope_withIsRisingFalse() {
        params.put("isRising", false);
        TrendRule rule = switch (RuleType.Trend.build().setParams(params)) {
            case TrendRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1, 2, 3}, new double[]{3, 2, 1});
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertEquals(1, result.length);
        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_multipleSeries_withSourceId() {
        params.put("sourceId", "s2");
        params.put("isRising", true);
        TrendRule rule = switch (RuleType.Trend.build().setParams(params)) {
            case TrendRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries s1 = createDoubleTimeSeries("s1", new long[]{1, 2, 3}, new double[]{1, 2, 3}); // Rising
        TimeSeries s2 = createDoubleTimeSeries("s2", new long[]{1, 2, 3}, new double[]{1, 1.5, 2}); // Rising but lower
        TimeSeries s3 = createDoubleTimeSeries("s3", new long[]{1, 2, 3}, new double[]{1, 1.2, 1.4}); // Rising less
        TimeSeries[] input = new TimeSeries[]{s1, s2, s3};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertEquals(1, result.length);
        assertFalse(result[0].values()[0]);
    }

    @Test
    void predicate_negativeValues_handling() {
        params.put("isRising", true);
        TrendRule rule = switch (RuleType.Trend.build().setParams(params)) {
            case TrendRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1, 2, 3}, new double[]{-3, -2, -1});
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertEquals(1, result.length);
        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_verySmallSlope_handling() {
        params.put("isRising", true);
        TrendRule rule = switch (RuleType.Trend.build().setParams(params)) {
            case TrendRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1, 2, 3}, new double[]{0.0, 0.000001, 0.000002});
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertEquals(1, result.length);
        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_withNonDoubleSeries_ignoresThem() {
        params.put("isRising", true);
        TrendRule rule = switch (RuleType.Trend.build().setParams(params)) {
            case TrendRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        // Boolean series should be ignored
        TimeSeries booleanSeries = new BooleanTimeSeriesBuilder().setId("s1").add(1, true).add(2, false).build();
        TimeSeries[] input = new TimeSeries[]{booleanSeries};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertEquals(1, result.length);
        assertFalse(result[0].values()[0]);
    }
}
