
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
class LimitRuleTest {

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
    void predicate_withinMinMax_shouldReturnTrue() {
        params.put("min", 10.0);
        params.put("max", 20.0);
        LimitRule rule = switch (RuleType.Limit.build().setParams(params)) {
            case LimitRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1L}, new double[]{15.0});
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_belowMin_shouldReturnFalse() {
        params.put("min", 10.0);
        params.put("max", 20.0);
        LimitRule rule = switch (RuleType.Limit.build().setParams(params)) {
            case LimitRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1L}, new double[]{5.0});
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertFalse(result[0].values()[0]);
    }

    @Test
    void predicate_withinSourceBounds_shouldReturnTrue() {
        params.put("lowerSourceId", "lower");
        params.put("upperSourceId", "top");
        params.set("sources", JsonNodeFactory.instance.arrayNode().add("s1"));
        LimitRule rule = switch (RuleType.Limit.build().setParams(params)) {
            case LimitRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1L}, new double[]{15.0});
        TimeSeries lower = createDoubleTimeSeries("lower", new long[]{1L}, new double[]{10.0});
        TimeSeries top = createDoubleTimeSeries("top", new long[]{1L}, new double[]{20.0});
        TimeSeries[] input = new TimeSeries[]{series, lower, top};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_aboveTopSource_shouldReturnFalse() {
        params.put("lowerSourceId", "lower");
        params.put("upperSourceId", "top");
        params.set("sources", JsonNodeFactory.instance.arrayNode().add("s1"));
        LimitRule rule = switch (RuleType.Limit.build().setParams(params)) {
            case LimitRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1L}, new double[]{25.0});
        TimeSeries lower = createDoubleTimeSeries("lower", new long[]{1L}, new double[]{10.0});
        TimeSeries top = createDoubleTimeSeries("top", new long[]{1L}, new double[]{20.0});
        TimeSeries[] input = new TimeSeries[]{series, lower, top};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertFalse(result[0].values()[0]);
    }
    
    @Test
    void predicate_noSources_shouldReturnEmptyArray() {
        LimitRule rule = switch (RuleType.Limit.build().setParams(params)) {
            case LimitRule r -> r;
            default -> throw new RuntimeException();
        };

        TimeSeries[] input = new TimeSeries[]{};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertEquals(0, result.length);
    }

    @Test
    void predicate_exactlyAtMinBoundary_shouldReturnTrue() {
        params.put("min", 10.0);
        params.put("max", 20.0);
        LimitRule rule = switch (RuleType.Limit.build().setParams(params)) {
            case LimitRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1L}, new double[]{10.0});
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_exactlyAtMaxBoundary_shouldReturnTrue() {
        params.put("min", 10.0);
        params.put("max", 20.0);
        LimitRule rule = switch (RuleType.Limit.build().setParams(params)) {
            case LimitRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1L}, new double[]{20.0});
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_belowMinSource_shouldReturnFalse() {
        params.put("lowerSourceId", "lower");
        params.put("upperSourceId", "top");
        params.set("sources", JsonNodeFactory.instance.arrayNode().add("s1"));
        LimitRule rule = switch (RuleType.Limit.build().setParams(params)) {
            case LimitRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1L}, new double[]{5.0});
        TimeSeries lower = createDoubleTimeSeries("lower", new long[]{1L}, new double[]{10.0});
        TimeSeries top = createDoubleTimeSeries("top", new long[]{1L}, new double[]{20.0});
        TimeSeries[] input = new TimeSeries[]{series, lower, top};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertFalse(result[0].values()[0]);
    }

    @Test
    void predicate_minOnly_noMax_shouldReturnTrue() {
        params.put("min", 10.0);
        LimitRule rule = switch (RuleType.Limit.build().setParams(params)) {
            case LimitRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1L}, new double[]{15.0});
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_minOnly_belowValue_shouldReturnFalse() {
        params.put("min", 10.0);
        LimitRule rule = switch (RuleType.Limit.build().setParams(params)) {
            case LimitRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1L}, new double[]{5.0});
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertFalse(result[0].values()[0]);
    }

    @Test
    void predicate_maxOnly_noMin_shouldReturnTrue() {
        params.put("max", 20.0);
        LimitRule rule = switch (RuleType.Limit.build().setParams(params)) {
            case LimitRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1L}, new double[]{15.0});
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_maxOnly_aboveValue_shouldReturnFalse() {
        params.put("max", 20.0);
        LimitRule rule = switch (RuleType.Limit.build().setParams(params)) {
            case LimitRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1L}, new double[]{25.0});
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertFalse(result[0].values()[0]);
    }

    @Test
    void predicate_negativeValues_withinRange() {
        params.put("min", -10.0);
        params.put("max", 10.0);
        LimitRule rule = switch (RuleType.Limit.build().setParams(params)) {
            case LimitRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1L}, new double[]{-5.0});
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_negativeValues_belowRange() {
        params.put("min", -10.0);
        params.put("max", 10.0);
        LimitRule rule = switch (RuleType.Limit.build().setParams(params)) {
            case LimitRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1L}, new double[]{-15.0});
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertFalse(result[0].values()[0]);
    }

    @Test
    void predicate_largeValues_handling() {
        params.put("min", 1_000_000.0);
        params.put("max", 2_000_000.0);
        LimitRule rule = switch (RuleType.Limit.build().setParams(params)) {
            case LimitRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1L}, new double[]{1_500_000.0});
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_sourceIdNotFound_shouldReturnFalse() {
        params.put("lowerSourceId", "nonExistent");
        params.put("upperSourceId", "top");
        params.set("sources", JsonNodeFactory.instance.arrayNode().add("s1"));
        LimitRule rule = switch (RuleType.Limit.build().setParams(params)) {
            case LimitRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1L}, new double[]{15.0});
        TimeSeries top = createDoubleTimeSeries("top", new long[]{1L}, new double[]{20.0});
        TimeSeries[] input = new TimeSeries[]{series, top};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertEquals(1, result.length);
        assertFalse(result[0].values()[0]);
    }

    @Test
    void predicate_notActivated_returnsEmpty() {
        LimitRule inactiveRule = switch (RuleType.Limit.build().setParams(params)) {
            case LimitRule r -> r;
            default -> throw new RuntimeException();
        };

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1L}, new double[]{15.0});
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = inactiveRule.predicate().apply(input);

        assertEquals(0, result.length);
    }

    @Test
    void predicate_emptySeriesValues_handling() {
        params.put("min", 10.0);
        params.put("max", 20.0);
        LimitRule rule = switch (RuleType.Limit.build().setParams(params)) {
            case LimitRule r -> r;
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
    void predicate_nanValues_handling() {
        params.put("min", 10.0);
        params.put("max", 20.0);
        LimitRule rule = switch (RuleType.Limit.build().setParams(params)) {
            case LimitRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries series = createDoubleTimeSeries("s1", new long[]{1L}, new double[]{Double.NaN});
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertEquals(1, result.length);
    }

    @Test
    void predicate_multipleValuesInSeries() {
        params.put("min", 10.0);
        params.put("max", 20.0);
        LimitRule rule = switch (RuleType.Limit.build().setParams(params)) {
            case LimitRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        DoubleTimeSeries series = createDoubleTimeSeries("s1", new long[]{1L, 2L, 3L}, new double[]{15.0, 18.0, 12.0});
        TimeSeries[] input = new TimeSeries[]{series};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertEquals(1, result.length);
        assertTrue(result[0].values()[0]);
    }
}
