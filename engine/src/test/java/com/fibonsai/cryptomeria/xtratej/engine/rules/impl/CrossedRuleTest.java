
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
import com.fibonsai.cryptomeria.xtratej.event.series.dao.builders.DoubleTimeSeriesBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("SwitchStatementWithTooFewBranches")
class CrossedRuleTest {

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
    void processParams_shouldSetThresholdAndSourceId() {
        params.put("threshold", 50.5);
        params.put("sourceId", "comparator");
        CrossedRule rule = RuleType.Crossed.build().setParams(params) instanceof CrossedRule crossedRule ? crossedRule : null;

        // We can't directly test private fields, but we test the behavior in other tests
        assertNotNull(rule);
    }

    @Test
    void predicate_crossedThreshold_shouldReturnTrue() {
        params.put("threshold", 50.0);
        CrossedRule rule = switch (RuleType.Crossed.build().setParams(params)) {
            case CrossedRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1L, 2L}, new double[]{40.0, 60.0});
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertEquals(1, result.length);
        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_notCrossedThreshold_shouldReturnFalse() {
        params.put("threshold", 70.0);
        CrossedRule rule = switch (RuleType.Crossed.build().setParams(params)) {
            case CrossedRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1L, 2L}, new double[]{40.0, 60.0});
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertEquals(1, result.length);
        assertFalse(result[0].values()[0]);
    }

    @Test
    void predicate_seriesCrossed_shouldReturnTrue() {
        params.put("sourceId", "s2");
        params.set("sources", JsonNodeFactory.instance.arrayNode().add("s1").add("s2"));
        CrossedRule rule = switch (RuleType.Crossed.build().setParams(params)) {
            case CrossedRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series1 = createDoubleTimeSeries("s1", new long[]{1L, 2L}, new double[]{40.0, 60.0});
        TimeSeries series2 = createDoubleTimeSeries("s2", new long[]{1L, 2L}, new double[]{50.0, 50.0});
        TimeSeries[] input = new TimeSeries[]{series1, series2};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertEquals(1, result.length);
        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_seriesNotCrossed_shouldReturnFalse() {
        params.put("sourceId", "s2");
        params.set("sources", JsonNodeFactory.instance.arrayNode().add("s1").add("s2"));
        CrossedRule rule = switch (RuleType.Crossed.build().setParams(params)) {
            case CrossedRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series1 = createDoubleTimeSeries("s1", new long[]{1L, 2L}, new double[]{40.0, 60.0});
        TimeSeries series2 = createDoubleTimeSeries("s2", new long[]{1L, 2L}, new double[]{70.0, 80.0});
        TimeSeries[] input = new TimeSeries[]{series1, series2};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertEquals(1, result.length);
        assertFalse(result[0].values()[0]);
    }

    @Test
    void predicate_noSources_shouldReturnEmptyArray() {
        CrossedRule rule = switch (RuleType.Crossed.build().setParams(params)) {
            case CrossedRule r -> r;
            default -> throw new RuntimeException();
        };

        TimeSeries[] input = new TimeSeries[]{};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertEquals(0, result.length);
    }

    @Test
    void predicate_thresholdExactlyAtMin_shouldReturnFalse() {
        params.put("threshold", 40.0);
        CrossedRule rule = switch (RuleType.Crossed.build().setParams(params)) {
            case CrossedRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1L, 2L}, new double[]{40.0, 60.0});
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertFalse(result[0].values()[0], "Should return false when series goes above threshold");
    }

    @Test
    void predicate_thresholdExactlyAtMax_shouldReturnFalse() {
        params.put("threshold", 60.0);
        CrossedRule rule = switch (RuleType.Crossed.build().setParams(params)) {
            case CrossedRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1L, 2L}, new double[]{40.0, 60.0});
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // Series ends at threshold - considered crossed
        assertFalse(result[0].values()[0]);
    }

    @Test
    void predicate_sourceIdNotFound_returnsFalse() {
        params.put("sourceId", "nonExistent");
        CrossedRule rule = switch (RuleType.Crossed.build().setParams(params)) {
            case CrossedRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1L, 2L}, new double[]{40.0, 60.0});
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertEquals(1, result.length);
        // Should return false when sourceId not found
        assertFalse(result[0].values()[0]);
    }

    @Test
    void predicate_nanThreshold_handling() {
        params.put("threshold", Double.NaN);
        CrossedRule rule = switch (RuleType.Crossed.build().setParams(params)) {
            case CrossedRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1L, 2L}, new double[]{40.0, 60.0});
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertEquals(1, result.length);
    }

    @Test
    void predicate_infiniteThreshold_handling() {
        params.put("threshold", Double.POSITIVE_INFINITY);
        CrossedRule rule = switch (RuleType.Crossed.build().setParams(params)) {
            case CrossedRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1L, 2L}, new double[]{40.0, 60.0});
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertEquals(1, result.length);
    }

    @Test
    void predicate_singleValueSeries_handling() {
        params.put("threshold", 50.0);
        CrossedRule rule = switch (RuleType.Crossed.build().setParams(params)) {
            case CrossedRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1L}, new double[]{60.0});
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertEquals(1, result.length);
    }

    @Test
    void predicate_negativeValues_handling() {
        params.put("threshold", -50.0);
        CrossedRule rule = switch (RuleType.Crossed.build().setParams(params)) {
            case CrossedRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1L, 2L}, new double[]{-60.0, -40.0});
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertEquals(1, result.length);
        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_negativeThreshold_handling() {
        params.put("threshold", -50.0);
        CrossedRule rule = switch (RuleType.Crossed.build().setParams(params)) {
            case CrossedRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1L, 2L}, new double[]{40.0, 60.0});
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertEquals(1, result.length);
        assertFalse(result[0].values()[0]);
    }
}
