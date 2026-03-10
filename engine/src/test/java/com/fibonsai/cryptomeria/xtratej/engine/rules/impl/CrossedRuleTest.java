
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

    private SingleTimeSeries createSingleTimeSeries(String name, long[] timestamps, double[] values) {
        SingleTimeSeriesBuilder builder = new SingleTimeSeriesBuilder().setId(name);
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
        rule.watch(new Fifo<>());

        TimeSeries series = createSingleTimeSeries("s1", new long[]{1L, 2L}, new double[]{40.0, 60.0});
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
        rule.watch(new Fifo<>());

        TimeSeries series = createSingleTimeSeries("s1", new long[]{1L, 2L}, new double[]{40.0, 60.0});
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
        rule.watch(new Fifo<>());

        TimeSeries series1 = createSingleTimeSeries("s1", new long[]{1L, 2L}, new double[]{40.0, 60.0});
        TimeSeries series2 = createSingleTimeSeries("s2", new long[]{1L, 2L}, new double[]{50.0, 50.0});
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
        rule.watch(new Fifo<>());

        TimeSeries series1 = createSingleTimeSeries("s1", new long[]{1L, 2L}, new double[]{40.0, 60.0});
        TimeSeries series2 = createSingleTimeSeries("s2", new long[]{1L, 2L}, new double[]{70.0, 80.0});
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
}
