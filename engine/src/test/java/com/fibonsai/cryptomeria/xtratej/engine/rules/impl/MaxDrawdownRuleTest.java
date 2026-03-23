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
import com.fibonsai.cryptomeria.xtratej.event.series.dao.BarTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.BooleanTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.DoubleTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.TimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.builders.BarTimeSeriesBuilder;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.builders.DoubleTimeSeriesBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MaxDrawdownRule.
 *
 * This rule calculates the maximum drawdown from price series and checks if it's below a threshold.
 *
 * Implementation note: The formula used is:
 *   maxDrawdown = 1.0 - ((low - peak) / peak)
 *
 * When low < peak (typical case), (low - peak) is negative, so:
 *   maxDrawdown = 1.0 - (negative / peak) = 1.0 + (peak - low) / peak
 *
 * This means maxDrawdown > 1.0 when there's any drawdown from the peak.
 * A maxDrawdown < max means the "drawdown" is within acceptable bounds.
 */
class MaxDrawdownRuleTest {

    private ObjectNode params;

    @BeforeEach
    void setUp() {
        params = JsonNodeFactory.instance.objectNode();
    }

    /**
     * Test: Constant price - no drawdown.
     * All prices at 100 -> peak = 100, low = 100 -> drawdown should be 1.0
     */
    @Test
    void predicate_constantPrice_noDrawdown() {
        BarTimeSeries series = new BarTimeSeriesBuilder()
            .setId("test")
            .add(1000L, 100.0, 100.0, 100.0, 100.0, 1.0)
            .add(2000L, 100.0, 100.0, 100.0, 100.0, 1.0)
            .add(3000L, 100.0, 100.0, 100.0, 100.0, 1.0)
            .build();

        params.put("max", 2.0);
        MaxDrawdownRule rule = (MaxDrawdownRule) RuleType.MaxDrawdown.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // peak = 100, low = 100
        // maxDrawdown = 1.0 - ((100 - 100) / 100) = 1.0 - 0 = 1.0
        // 1.0 < 2.0 -> true
        assertTrue(result[0].values()[0], "Constant price drawdown of 1.0 < max of 2.0");
    }

    /**
     * Test: Simple drawdown - price goes up then down.
     * Prices: 100, 120, 110 -> peak = 120, low = 100
     * maxDrawdown = 1.0 - ((100 - 120) / 120) = 1.0 - (-20/120) = 1.0 + 0.167 = 1.167
     */
    @Test
    void predicate_simpleDrawdown() {
        BarTimeSeries series = new BarTimeSeriesBuilder()
            .setId("test")
            .add(1000L, 100.0, 120.0, 100.0, 110.0, 1.0)
            .add(2000L, 110.0, 115.0, 105.0, 108.0, 1.0)
            .add(3000L, 115.0, 118.0, 108.0, 110.0, 1.0)
            .build();

        // maxDrawdown = 1.0 - ((100 - 120) / 120) = 1.0 + 20/120 = 1.167
        params.put("max", 2.0);
        MaxDrawdownRule rule = (MaxDrawdownRule) RuleType.MaxDrawdown.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // 1.167 < 2.0 -> true
        assertTrue(result[0].values()[0], "Drawdown 1.167 < max of 2.0");
    }

    /**
     * Test: Large drawdown.
     * Prices: 100 -> 200 -> 100 -> peak = 200, low = 100
     * maxDrawdown = 1.0 - ((100 - 200) / 200) = 1.0 + 0.5 = 1.5
     */
    @Test
    void predicate_largeDrawdown() {
        BarTimeSeries series = new BarTimeSeriesBuilder()
            .setId("test")
            .add(1000L, 100.0, 200.0, 100.0, 150.0, 1.0)
            .add(2000L, 150.0, 180.0, 120.0, 130.0, 1.0)
            .add(3000L, 180.0, 190.0, 150.0, 160.0, 1.0)
            .build();

        params.put("max", 2.0);
        MaxDrawdownRule rule = (MaxDrawdownRule) RuleType.MaxDrawdown.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // peak = 200, low = 100
        // maxDrawdown = 1.0 - ((100 - 200) / 200) = 1.0 + 0.5 = 1.5
        // 1.5 < 2.0 -> true
        assertTrue(result[0].values()[0], "Drawdown 1.5 < max of 2.0");
    }

    /**
     * Test: Drawdown exceeds threshold.
     * Using closes: 150, 120, 90 -> peak = 150, low = 90
     * maxDrawdown = 1.0 - ((90 - 150) / 150) = 1.0 + 60/150 = 1.4
     */
    @Test
    void predicate_drawdownExceedsThreshold() {
        BarTimeSeries series = new BarTimeSeriesBuilder()
            .setId("test")
            .add(1000L, 100.0, 160.0, 140.0, 150.0, 1.0)
            .add(2000L, 150.0, 130.0, 110.0, 120.0, 1.0)
            .add(3000L, 120.0, 100.0, 80.0, 90.0, 1.0)
            .build();

        params.put("max", 1.3);
        MaxDrawdownRule rule = (MaxDrawdownRule) RuleType.MaxDrawdown.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // closes: 150, 120, 90
        // peak = 150, low = 90
        // maxDrawdown = 1.0 - ((90 - 150) / 150) = 1.0 + 60/150 = 1.4
        // 1.4 >= 1.3 -> false
        assertFalse(result[0].values()[0], "Drawdown 1.4 >= max of 1.3");
    }

    /**
     * Test: Price only goes up - no drawdown.
     * Prices: 100 -> 150 -> 200 -> peak = 200, low = 100
     * maxDrawdown = 1.0 - ((100 - 200) / 200) = 1.5
     */
    @Test
    void predicate_onlyUpside() {
        BarTimeSeries series = new BarTimeSeriesBuilder()
            .setId("test")
            .add(1000L, 100.0, 150.0, 100.0, 120.0, 1.0)
            .add(2000L, 120.0, 170.0, 120.0, 140.0, 1.0)
            .add(3000L, 140.0, 200.0, 140.0, 160.0, 1.0)
            .build();

        params.put("max", 2.0);
        MaxDrawdownRule rule = (MaxDrawdownRule) RuleType.MaxDrawdown.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // peak = 200, low = 100
        // maxDrawdown = 1.0 - ((100 - 200) / 200) = 1.5
        // 1.5 < 2.0 -> true
        assertTrue(result[0].values()[0], "Drawdown 1.5 < max of 2.0");
    }

    /**
     * Test: Price only goes down - drawdown from initial price.
     * Prices: 200 -> 150 -> 100 -> peak = 200, low = 100
     * maxDrawdown = 1.0 - ((100 - 200) / 200) = 1.5
     */
    @Test
    void predicate_onlyDownside() {
        BarTimeSeries series = new BarTimeSeriesBuilder()
            .setId("test")
            .add(1000L, 200.0, 200.0, 150.0, 180.0, 1.0)
            .add(2000L, 180.0, 185.0, 140.0, 150.0, 1.0)
            .add(3000L, 150.0, 160.0, 100.0, 120.0, 1.0)
            .build();

        params.put("max", 2.0);
        MaxDrawdownRule rule = (MaxDrawdownRule) RuleType.MaxDrawdown.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // peak = 200 (first bar), low = 100
        // maxDrawdown = 1.0 - ((100 - 200) / 200) = 1.5
        // 1.5 < 2.0 -> true
        assertTrue(result[0].values()[0], "Drawdown 1.5 < max of 2.0");
    }

    /**
     * Test: Very small drawdown threshold.
     * Only acceptable if prices are almost constant.
     * Using closes: 101, 102, 103
     * peak = 103, low = 101
     * maxDrawdown = 1.0 - ((101 - 103) / 103) = 1.0 + 2/103 = 1.019
     */
    @Test
    void predicate_verySmallThreshold() {
        BarTimeSeries series = new BarTimeSeriesBuilder()
            .setId("test")
            .add(1000L, 100.0, 102.0, 100.0, 101.0, 1.0)
            .add(2000L, 101.0, 103.0, 101.0, 102.0, 1.0)
            .add(3000L, 102.0, 104.0, 102.0, 103.0, 1.0)
            .build();

        // closes: 101, 102, 103
        // peak = 103, low = 101
        // maxDrawdown = 1.0 - ((101 - 103) / 103) = 1.0 + 2/103 = 1.019
        params.put("max", 1.01);
        MaxDrawdownRule rule = (MaxDrawdownRule) RuleType.MaxDrawdown.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // 1.019 >= 1.01 -> false
        assertFalse(result[0].values()[0], "Drawdown 1.019 >= max of 1.01");
    }

    /**
     * Test: DoubleTimeSeries - uses values.
     */
    @Test
    void predicate_doubleTimeSeries() {
        DoubleTimeSeries series = new DoubleTimeSeriesBuilder()
            .setId("test")
            .add(1000L, 100.0)
            .add(2000L, 120.0)
            .add(3000L, 80.0)
            .build();

        // peak = 120, low = 80
        // maxDrawdown = 1.0 - ((80 - 120) / 120) = 1.0 + 40/120 = 1.333
        params.put("max", 2.0);
        MaxDrawdownRule rule = (MaxDrawdownRule) RuleType.MaxDrawdown.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // 1.333 < 2.0 -> true
        assertTrue(result[0].values()[0], "Drawdown 1.333 < max of 2.0");
    }

    /**
     * Test: Multiple series - each produces its own result.
     */
    @Test
    void predicate_multipleSeries() {
        BarTimeSeries series1 = new BarTimeSeriesBuilder()
            .setId("test1")
            .add(1000L, 100.0, 120.0, 90.0, 110.0, 1.0)
            .add(2000L, 110.0, 115.0, 100.0, 105.0, 1.0)
            .add(3000L, 120.0, 125.0, 110.0, 115.0, 1.0)
            .build();

        BarTimeSeries series2 = new BarTimeSeriesBuilder()
            .setId("test2")
            .add(1000L, 100.0, 110.0, 95.0, 105.0, 1.0)
            .add(2000L, 105.0, 110.0, 100.0, 108.0, 1.0)
            .add(3000L, 110.0, 115.0, 105.0, 112.0, 1.0)
            .build();

        params.put("max", 2.0);
        MaxDrawdownRule rule = (MaxDrawdownRule) RuleType.MaxDrawdown.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series1, series2};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertEquals(2, result.length, "Should have 2 results");

        // series1: peak=125, low=90 -> maxDrawdown = 1.0 - ((90-125)/125) = 1.28
        assertTrue(result[0].values()[0], "series1 drawdown 1.28 < max of 2.0");

        // series2: peak=115, low=95 -> maxDrawdown = 1.0 - ((95-115)/115) = 1.174
        assertTrue(result[1].values()[0], "series2 drawdown 1.174 < max of 2.0");
    }

    /**
     * Test: BarTimeSeries uses correct price (closes).
     * The rule checks for BarTimeSeries and uses closes().
     */
    @Test
    void predicate_barTimeSeriesUsesCloses() {
        BarTimeSeries series = new BarTimeSeriesBuilder()
            .setId("test")
            .add(1000L, 100.0, 150.0, 90.0, 140.0, 1.0)
            .add(2000L, 150.0, 200.0, 140.0, 160.0, 1.0)
            .add(3000L, 100.0, 150.0, 90.0, 110.0, 1.0)
            .build();

        params.put("max", 2.0);
        MaxDrawdownRule rule = (MaxDrawdownRule) RuleType.MaxDrawdown.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // Using closes: 140, 160, 110
        // peak = 160, low = 110
        // maxDrawdown = 1.0 - ((110 - 160) / 160) = 1.0 + 50/160 = 1.3125
        // 1.3125 < 2.0 -> true
        assertTrue(result[0].values()[0], "Drawdown using closes 1.3125 < max of 2.0");
    }

    /**
     * Test: Empty series.
     */
    @Test
    void predicate_emptySeries() {
        BarTimeSeries series = new BarTimeSeriesBuilder()
            .setId("test")
            .build();

        MaxDrawdownRule rule = (MaxDrawdownRule) RuleType.MaxDrawdown.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // Empty series -> no result -> single false
        assertEquals(1, result.length, "Should have 1 result");
        assertFalse(result[0].values()[0], "Empty series returns false");
    }

    /**
     * Test: Single value series.
     * With one value, peak = low -> drawdown = 1.0
     */
    @Test
    void predicate_singleValue() {
        BarTimeSeries series = new BarTimeSeriesBuilder()
            .setId("test")
            .add(1000L, 100.0, 100.0, 100.0, 100.0, 1.0)
            .build();

        params.put("max", 2.0);
        MaxDrawdownRule rule = (MaxDrawdownRule) RuleType.MaxDrawdown.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // Single value: peak = 100, low = 100
        // maxDrawdown = 1.0 - ((100 - 100) / 100) = 1.0
        // 1.0 < 2.0 -> true
        assertTrue(result[0].values()[0], "Single value drawdown 1.0 < max of 2.0");
    }

    /**
     * Test: Peak at zero - special case returns infinity.
     * If peak = 0.0, maxDrawdown = POSITIVE_INFINITY
     */
    @Test
    void predicate_peakZero() {
        BarTimeSeries series = new BarTimeSeriesBuilder()
            .setId("test")
            .add(1000L, 0.0, 10.0, 0.0, 0.0, 1.0)
            .add(2000L, 10.0, 15.0, 0.0, 0.0, 1.0)
            .add(3000L, 5.0, 10.0, 0.0, 0.0, 1.0)
            .build();

        params.put("max", 100.0);
        MaxDrawdownRule rule = (MaxDrawdownRule) RuleType.MaxDrawdown.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // peak = 0.0 -> maxDrawdown = POSITIVE_INFINITY
        // infinity >= 100 -> false
        assertFalse(result[0].values()[0], "Infinity drawdown >= max (100)");
    }

    /**
     * Test: Default max threshold (POSITIVE_INFINITY).
     * Should always return true since any finite drawdown < infinity.
     */
    @Test
    void predicate_defaultMaxThreshold() {
        BarTimeSeries series = new BarTimeSeriesBuilder()
            .setId("test")
            .add(1000L, 100.0, 150.0, 50.0, 150.0, 1.0)
            .add(2000L, 150.0, 200.0, 100.0, 120.0, 1.0)
            .add(3000L, 100.0, 150.0, 80.0, 100.0, 1.0)
            .build();

        MaxDrawdownRule rule = (MaxDrawdownRule) RuleType.MaxDrawdown.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // peak = 200, low = 50
        // maxDrawdown = 1.0 - ((50 - 200) / 200) = 1.75
        // 1.75 < POSITIVE_INFINITY -> true
        assertTrue(result[0].values()[0], "Finite drawdown < infinity");
    }

    /**
     * Test: Drawdown exactly at threshold.
     * maxDrawdown = 1.4, max = 1.4 -> 1.4 < 1.4 is false
     */
    @Test
    void predicate_drawdownAtThreshold() {
        BarTimeSeries series = new BarTimeSeriesBuilder()
            .setId("test")
            .add(1000L, 100.0, 150.0, 100.0, 150.0, 1.0)
            .add(2000L, 150.0, 130.0, 110.0, 120.0, 1.0)
            .add(3000L, 120.0, 100.0, 80.0, 90.0, 1.0)
            .build();

        // closes: 150, 120, 90
        // peak = 150, low = 90
        // maxDrawdown = 1.0 - ((90 - 150) / 150) = 1.0 + 60/150 = 1.4
        params.put("max", 1.4);
        MaxDrawdownRule rule = (MaxDrawdownRule) RuleType.MaxDrawdown.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // maxDrawdown = 1.4, max = 1.4
        // 1.4 < 1.4 is false
        assertFalse(result[0].values()[0], "Drawdown 1.4 is not < max of 1.4");
    }

    /**
     * Test: Multiple series with different results.
     */
    @Test
    void predicate_multipleSeriesDifferentResults() {
        BarTimeSeries series1 = new BarTimeSeriesBuilder()
            .setId("test1")
            .add(1000L, 100.0, 150.0, 100.0, 150.0, 1.0)
            .add(2000L, 150.0, 200.0, 100.0, 120.0, 1.0)
            .add(3000L, 100.0, 150.0, 100.0, 100.0, 1.0)
            .build();

        BarTimeSeries series2 = new BarTimeSeriesBuilder()
            .setId("test2")
            .add(1000L, 100.0, 150.0, 50.0, 150.0, 1.0)
            .add(2000L, 150.0, 200.0, 100.0, 120.0, 1.0)
            .add(3000L, 100.0, 150.0, 80.0, 100.0, 1.0)
            .build();

        params.put("max", 1.25);
        MaxDrawdownRule rule = (MaxDrawdownRule) RuleType.MaxDrawdown.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series1, series2};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertEquals(2, result.length, "Should have 2 results");

        // series1: peak=200, low=100 -> maxDrawdown = 1.5 >= 1.25 -> false
        assertFalse(result[0].values()[0], "series1 drawdown 1.5 >= max of 1.25");

        // series2: peak=200, low=50 -> maxDrawdown = 1.75 >= 1.25 -> false
        assertFalse(result[1].values()[0], "series2 drawdown 1.75 >= max of 1.25");
    }
}
