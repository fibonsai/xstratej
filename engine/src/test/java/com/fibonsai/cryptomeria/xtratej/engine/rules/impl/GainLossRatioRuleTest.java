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
import com.fibonsai.cryptomeria.xtratej.event.series.dao.MyOrdersTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.MyOrdersTimeSeries.OrderCondition;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.MyOrdersTimeSeries.OrderType;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.MyOrdersTimeSeries.TradeState;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.TimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.builders.DoubleTimeSeriesBuilder;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.builders.MyOrdersTimeSeriesBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import static com.fibonsai.cryptomeria.xtratej.event.series.dao.MyOrdersTimeSeries.BidAskSide.ASK;
import static com.fibonsai.cryptomeria.xtratej.event.series.dao.MyOrdersTimeSeries.BidAskSide.BID;
import static org.junit.jupiter.api.Assertions.*;

class GainLossRatioRuleTest {

    private ObjectNode params;

    @BeforeEach
    void setUp() {
        params = JsonNodeFactory.instance.objectNode();
    }

    /**
     * Test: Single gain - ratio = infinity (no losses)
     * Default params: ratioMin = -inf, ratioMax = +inf
     */
    @Test
    void predicate_singleGain_ratioInfinity() {
        // Profit = 120 - 100 = 20 > 0 -> gain
        MyOrdersTimeSeries series = new MyOrdersTimeSeriesBuilder().setId("id")
            .add(1000L, "order1", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(2000L, "order2", ASK, TradeState.FILLED, 120.0, 1.0, 1.0)
            .build();

        GainLossRatioRule rule = (GainLossRatioRule) RuleType.GainLossRatio.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // 1 gain, 0 loss -> ratio = infinity -> within default range
        assertTrue(result[0].values()[0], "Single gain (infinity ratio) within default range");
    }

    /**
     * Test: Single loss - ratio = 0
     */
    @Test
    void predicate_singleLoss_ratioZero() {
        // Profit = 80 - 100 = -20 < 0 -> loss
        MyOrdersTimeSeries series = new MyOrdersTimeSeriesBuilder().setId("id")
            .add(2000L, "order1", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(1000L, "order2", ASK, TradeState.FILLED, 80.0, 1.0, 1.0)
            .build();

        GainLossRatioRule rule = (GainLossRatioRule) RuleType.GainLossRatio.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // 0 gain, 1 loss -> ratio = 0 -> within default range
        assertTrue(result[0].values()[0], "Single loss (ratio=0) within default range");
    }

    /**
     * Test: One gain, one loss - ratio = 1.0
     */
    @Test
    void predicate_oneGainOneLoss_ratioOne() {
        // 2 separate positions:
        // Position 1: ASK 80, BID 100 = -20 (loss)
        // Position 2: ASK 120, BID 100 = +20 (gain)
        MyOrdersTimeSeries series = new MyOrdersTimeSeriesBuilder().setId("id")
            .add(1000L, "order1", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(2000L, "order2", ASK, TradeState.FILLED, 80.0, 1.0, 1.0)
            .add(3000L, "order3", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(4000L, "order4", ASK, TradeState.FILLED, 120.0, 1.0, 1.0)
            .build();

        GainLossRatioRule rule = (GainLossRatioRule) RuleType.GainLossRatio.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // 1 gain, 1 loss -> ratio = 1.0 -> within default range
        assertTrue(result[0].values()[0], "Ratio of 1.0 within default range");
    }

    /**
     * Test: Only gains - ratio = infinity
     */
    @Test
    void predicate_onlyGains_ratioInfinity() {
        // 3 positions all with profit:
        // Position 1: ASK 115, BID 100 = +15
        // Position 2: ASK 130, BID 100 = +30
        // Position 3: ASK 120, BID 100 = +20
        MyOrdersTimeSeries series = new MyOrdersTimeSeriesBuilder().setId("id")
            .add(1000L, "order1", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(2000L, "order2", ASK, TradeState.FILLED, 115.0, 1.0, 1.0)
            .add(3000L, "order3", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(4000L, "order4", ASK, TradeState.FILLED, 130.0, 1.0, 1.0)
            .add(5000L, "order5", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(6000L, "order6", ASK, TradeState.FILLED, 120.0, 1.0, 1.0)
            .build();

        params.put("ratioMin", 0.0);
        params.put("ratioMax", 100.0);
        GainLossRatioRule rule = (GainLossRatioRule) RuleType.GainLossRatio.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // 3 gains, 0 losses -> ratio = infinity -> exceeds max of 100
        assertFalse(result[0].values()[0], "Infinity ratio exceeds max of 100");
    }

    /**
     * Test: Only losses - ratio = 0
     */
    @Test
    void predicate_onlyLosses_ratioZero() {
        // 3 positions all with loss:
        // Position 1: ASK 95, BID 100 = -5
        // Position 2: ASK 85, BID 100 = -15
        // Position 3: ASK 90, BID 100 = -10
        MyOrdersTimeSeries series = new MyOrdersTimeSeriesBuilder().setId("id")
            .add(1000L, "order1", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(2000L, "order2", ASK, TradeState.FILLED, 95.0, 1.0, 1.0)
            .add(3000L, "order3", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(4000L, "order4", ASK, TradeState.FILLED, 85.0, 1.0, 1.0)
            .add(5000L, "order5", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(6000L, "order6", ASK, TradeState.FILLED, 90.0, 1.0, 1.0)
            .build();

        params.put("ratioMin", 0.5);
        GainLossRatioRule rule = (GainLossRatioRule) RuleType.GainLossRatio.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // 0 gains, 3 losses -> ratio = 0 -> below min of 0.5
        assertFalse(result[0].values()[0], "Ratio of 0 below min of 0.5");
    }

    /**
     * Test: Ratio within bounds [0.5, 2.0]
     * 2 gains, 2 losses = ratio 1.0
     */
    @Test
    void predicate_ratioWithinBounds() {
        MyOrdersTimeSeries series = new MyOrdersTimeSeriesBuilder().setId("id")
            .add(1000L, "order1", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(2000L, "order2", ASK, TradeState.FILLED, 70.0, 1.0, 1.0) // -30
            .add(3000L, "order3", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(4000L, "order4", ASK, TradeState.FILLED, 130.0, 1.0, 1.0) // +30
            .add(5000L, "order5", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(6000L, "order6", ASK, TradeState.FILLED, 80.0, 1.0, 1.0) // -20
            .add(7000L, "order7", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(8000L, "order8", ASK, TradeState.FILLED, 120.0, 1.0, 1.0) // +20
            .build();

        params.put("ratioMin", 0.5);
        params.put("ratioMax", 2.0);
        GainLossRatioRule rule = (GainLossRatioRule) RuleType.GainLossRatio.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // 2 gains, 2 losses -> ratio = 1.0 -> within [0.5, 2.0]
        assertTrue(result[0].values()[0], "Ratio of 1.0 within [0.5, 2.0]");
    }

    /**
     * Test: Ratio below minimum threshold
     * 1 gain, 2 losses = ratio 0.5
     */
    @Test
    void predicate_ratioBelowMin() {
        MyOrdersTimeSeries series = new MyOrdersTimeSeriesBuilder().setId("id")
            .add(1000L, "order1", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(2000L, "order2", ASK, TradeState.FILLED, 85.0, 1.0, 1.0)
            .add(3000L, "order3", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(4000L, "order4", ASK, TradeState.FILLED, 80.0, 1.0, 1.0)
            .add(5000L, "order5", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(6000L, "order6", ASK, TradeState.FILLED, 120.0, 1.0, 1.0)
            .build();

        params.put("ratioMin", 1.0);
        params.put("ratioMax", 100.0);
        GainLossRatioRule rule = (GainLossRatioRule) RuleType.GainLossRatio.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // 1 gain, 2 losses -> ratio = 0.5 -> below min of 1.0
        assertFalse(result[0].values()[0], "Ratio of 0.5 below min of 1.0");
    }

    /**
     * Test: Break-even position (profit = 0)
     * Should NOT count as gain or loss
     */
    @Test
    void predicate_breakEven_position() {
        // ASK 100, BID 100 = 0 profit (not counted as gain or loss)
        MyOrdersTimeSeries series = new MyOrdersTimeSeriesBuilder().setId("id")
            .add(1000L, "order1", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(2000L, "order2", ASK, TradeState.FILLED, 100.0, 1.0, 1.0)
            .build();

        params.put("ratioMin", 0.0);
        params.put("ratioMax", 100.0);
        GainLossRatioRule rule = (GainLossRatioRule) RuleType.GainLossRatio.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // 0 gains, 0 losses -> ratio = 1.0
        // 0.0 < 1.0 < 100.0 -> true
        assertTrue(result[0].values()[0]);
    }

    /**
     * Test: Empty series array
     */
    @Test
    void predicate_emptySeriesArray() {
        GainLossRatioRule rule = (GainLossRatioRule) RuleType.GainLossRatio.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // Empty input -> returns single false
        assertEquals(1, result.length, "Should have 1 result");
        assertFalse(result[0].values()[0], "Empty input returns false");
    }

    /**
     * Test: Non-MyOrdersTimeSeries in array - skipped
     */
    @Test
    void predicate_nonMyOrdersSeries() {
        DoubleTimeSeries series =
            new DoubleTimeSeriesBuilder()
                .setId("test")
                .add(1000L, 100.0)
                .build();

        GainLossRatioRule rule = (GainLossRatioRule) RuleType.GainLossRatio.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // Non-MyOrders series is skipped
        assertEquals(1, result.length, "Should have 1 result");
        assertFalse(result[0].values()[0], "Non-MyOrders series produces false");
    }

    /**
     * Test: Partially filled orders
     */
    @Test
    void predicate_partiallyFilledOrders() {
        MyOrdersTimeSeriesBuilder builder = new MyOrdersTimeSeriesBuilder().setId("test");

        // Buy 0.5 at 100, Sell 0.5 at 110
        // Profit = 0.5 * 110 - 0.5 * 100 = 5 > 0 -> gain
        builder.add(1000L, "order1", "BTC-USD", BID, "user1",
            TradeState.PARTIALLY_FILLED,
            OrderType.LIMIT,
            0.0, 100.0, 100.0, 0.0, 0.0, 0.0,
            1.0, 0.5, OrderCondition.DAY, "test");

        builder.add(2000L, "order2", "BTC-USD", ASK, "user1",
                TradeState.PARTIALLY_FILLED,
                OrderType.LIMIT,
                0.0, 110.0, 100.0, 0.0, 0.0, 0.0,
                1.0, 0.5, OrderCondition.DAY, "test");

        params.put("ratioMin", 0.0);
        params.put("ratioMax", 100.0);
        GainLossRatioRule rule = (GainLossRatioRule) RuleType.GainLossRatio.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{builder.build()};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // Profit = 5 > 0 -> gain
        // ratio = 1/0 = infinity -> exceeds max of 100
        assertFalse(result[0].values()[0], "Profit of 5 is gain, no loss, then ratio is infinity");
    }

    /**
     * Test: ratioMin = POSITIVE_INFINITY
     */
    @Test
    void predicate_ratioMinInfinity() {
        params.put("ratioMin", Double.POSITIVE_INFINITY);
        params.put("ratioMax", Double.POSITIVE_INFINITY);
        GainLossRatioRule rule = (GainLossRatioRule) RuleType.GainLossRatio.build().setParams(params);
        rule.watch(new DirectFlux<>());

        MyOrdersTimeSeries series = new MyOrdersTimeSeriesBuilder().setId("id")
            .add(1000L, "order1", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(2000L, "order2", ASK, TradeState.FILLED, 110.0, 1.0, 1.0)
            .build();

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // ratio = infinity, min = infinity -> matches
        assertTrue(result[0].values()[0], "infinity >= infinity is true");
    }

    /**
     * Test: Multiple series - each produces its own result
     */
    @Test
    void predicate_multipleSeries() {
        MyOrdersTimeSeries series1 = new MyOrdersTimeSeriesBuilder().setId("id")
            .add(1000L, "order1", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(2000L, "order2", ASK, TradeState.FILLED, 110.0, 1.0, 1.0)
            .build();

        MyOrdersTimeSeries series2 = new MyOrdersTimeSeriesBuilder().setId("id")
            .add(1500L, "order3", BID, TradeState.FILLED, 50.0, 1.0, 1.0)
            .add(2500L, "order4", ASK, TradeState.FILLED, 45.0, 1.0, 1.0)
            .build();

        params.put("ratioMin", 0.0);
        params.put("ratioMax", 100.0);
        GainLossRatioRule rule = (GainLossRatioRule) RuleType.GainLossRatio.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series1, series2};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertEquals(2, result.length, "Should have 2 results for 2 series");

        // series1: 1 gain, 0 loss -> infinity -> exceeds max of 100 -> false
        assertFalse(result[0].values()[0], "series1 (infinity) exceeds max of 100");

        // series2: 0 gain, 1 loss -> ratio = 0 -> within [0, 100] -> true
        assertTrue(result[1].values()[0], "series2 (ratio=0) within range");
    }
}
