
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
import com.fibonsai.cryptomeria.xtratej.event.reactive.Fifo;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.BooleanTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.SingleTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.TimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.builders.SingleTimeSeriesBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.*;

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

    private SingleTimeSeries createSingleTimeSeries(String name, long[] timestamps, double[] values) {
        SingleTimeSeriesBuilder builder = new SingleTimeSeriesBuilder().setId(name);
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
        rule.watch(new Fifo<>());

        TimeSeries series = createSingleTimeSeries("s1", new long[]{1L}, new double[]{15.0});
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
        rule.watch(new Fifo<>());

        TimeSeries series = createSingleTimeSeries("s1", new long[]{1L}, new double[]{5.0});
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
        rule.watch(new Fifo<>());

        TimeSeries series = createSingleTimeSeries("s1", new long[]{1L}, new double[]{15.0});
        TimeSeries lower = createSingleTimeSeries("lower", new long[]{1L}, new double[]{10.0});
        TimeSeries top = createSingleTimeSeries("top", new long[]{1L}, new double[]{20.0});
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
        rule.watch(new Fifo<>());

        TimeSeries series = createSingleTimeSeries("s1", new long[]{1L}, new double[]{25.0});
        TimeSeries lower = createSingleTimeSeries("lower", new long[]{1L}, new double[]{10.0});
        TimeSeries top = createSingleTimeSeries("top", new long[]{1L}, new double[]{20.0});
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
}
