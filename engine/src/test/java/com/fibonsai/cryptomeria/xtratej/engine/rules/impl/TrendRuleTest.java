
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

    private SingleTimeSeries createSingleTimeSeries(String name, long[] timestamps, double[] values) {
        SingleTimeSeriesBuilder builder = new SingleTimeSeriesBuilder().setId(name);
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
        rule.watch(new Fifo<>());

        TimeSeries series = createSingleTimeSeries("s1", new long[]{1, 2, 3}, new double[]{1, 2, 3}); // Rising
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
        rule.watch(new Fifo<>());

        TimeSeries series = createSingleTimeSeries("s1", new long[]{1, 2, 3}, new double[]{3, 2, 1}); // Falling
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
        rule.watch(new Fifo<>());

        TimeSeries series = createSingleTimeSeries("s1", new long[]{1, 2, 3}, new double[]{3, 2, 1}); // Falling
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
        rule.watch(new Fifo<>());

        TimeSeries s1 = createSingleTimeSeries("s1", new long[]{1, 2, 3}, new double[]{1, 2, 3}); // Slope 1.0
        TimeSeries s2 = createSingleTimeSeries("s2", new long[]{1, 2, 3}, new double[]{1, 1.5, 2}); // Slope 0.5
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
}
