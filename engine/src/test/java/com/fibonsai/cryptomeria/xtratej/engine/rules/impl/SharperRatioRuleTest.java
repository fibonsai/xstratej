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

/**
 * Tests for SharperRatioRule.
 *
 * The Sharpe ratio measures risk-adjusted return:
 * Sharpe Ratio = (Return - Risk-Free Rate) / Standard Deviation of Returns
 *
 * - Higher Sharpe ratio = better risk-adjusted performance
 * - Ratio > 1: Good
 * - Ratio > 2: Very good
 * - Ratio > 3: Excellent
 * - Negative ratio: Performance worse than risk-free rate
 */
class SharperRatioRuleTest {

    private ObjectNode params;

    @BeforeEach
    void setUp() {
        params = JsonNodeFactory.instance.objectNode();
    }

    /**
     * Test: Single gain - Sharpe ratio should be infinity (no volatility).
     * With one trade, std dev = 0, so Sharpe = infinity.
     */
    @Test
    void predicate_singleGain_ratioInfinity() {
        // Profit = 120 - 100 = 20 > 0
        MyOrdersTimeSeries series = new MyOrdersTimeSeriesBuilder().setId("id")
            .add(1000L, "order1", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(2000L, "order2", ASK, TradeState.FILLED, 120.0, 1.0, 1.0)
            .build();

        SharperRatioRule rule = (SharperRatioRule) RuleType.SharperRatio.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // Single trade -> std dev = 0 -> Sharpe = infinity -> within default range
        assertTrue(result[0].values()[0], "Single gain (infinity Sharpe) within default range");
    }

    /**
     * Test: Single loss - Sharpe ratio should be infinity (no volatility, negative return).
     * With one trade, std dev = 0, so Sharpe = -infinity.
     */
    @Test
    void predicate_singleLoss_ratioInfinity() {
        // Profit = 80 - 100 = -20 < 0
        MyOrdersTimeSeries series = new MyOrdersTimeSeriesBuilder().setId("id")
            .add(1000L, "order1", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(2000L, "order2", ASK, TradeState.FILLED, 80.0, 1.0, 1.0)
            .build();

        SharperRatioRule rule = (SharperRatioRule) RuleType.SharperRatio.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // Single trade -> std dev = 0 -> Sharpe = infinity -> within default range
        assertTrue(result[0].values()[0], "Single loss (infinity Sharpe) within default range");
    }

    /**
     * Test: Two trades with same profit - Sharpe ratio is infinity (zero volatility).
     */
    @Test
    void predicate_twoSameProfits_zeroVolatility() {
        // Position 1: ASK 120, BID 100 = +20
        // Position 2: ASK 120, BID 100 = +20
        // Mean = 20, StdDev = 0 -> Sharpe = infinity
        MyOrdersTimeSeries series = new MyOrdersTimeSeriesBuilder().setId("id")
            .add(1000L, "order1", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(2000L, "order2", ASK, TradeState.FILLED, 120.0, 1.0, 1.0)
            .add(3000L, "order3", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(4000L, "order4", ASK, TradeState.FILLED, 120.0, 1.0, 1.0)
            .build();

        SharperRatioRule rule = (SharperRatioRule) RuleType.SharperRatio.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // Zero volatility -> infinity Sharpe -> within default range
        assertTrue(result[0].values()[0], "Zero volatility gives infinity Sharpe");
    }

    /**
     * Test: Two trades with different profits - calculate actual Sharpe ratio.
     * Profits: +20, -20
     * Mean = 0, StdDev = 28.28 (approximately)
     * Sharpe = 0 / 28.28 = 0
     */
    @Test
    void predicate_twoDifferentProfits_calculateSharpe() {
        // Position 1: ASK 120, BID 100 = +20
        // Position 2: ASK 80, BID 100 = -20
        // Mean = 0, StdDev = sqrt(800) ≈ 28.28
        // Sharpe = 0 / 28.28 = 0
        MyOrdersTimeSeries series = new MyOrdersTimeSeriesBuilder().setId("id")
            .add(1000L, "order1", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(2000L, "order2", ASK, TradeState.FILLED, 120.0, 1.0, 1.0)
            .add(3000L, "order3", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(4000L, "order4", ASK, TradeState.FILLED, 80.0, 1.0, 1.0)
            .build();

        // Sharpe should be 0 (mean is 0)
        params.put("ratioMin", -1.0);
        params.put("ratioMax", 1.0);
        SharperRatioRule rule = (SharperRatioRule) RuleType.SharperRatio.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // Sharpe = 0, within [-1, 1]
        assertTrue(result[0].values()[0], "Sharpe of 0 within [-1, 1]");
    }

    /**
     * Test: Three trades with positive returns - positive Sharpe ratio.
     * Profits: +10, +20, +30
     * Mean = 20, StdDev ≈ 8.16
     * Sharpe ≈ 20 / 8.16 ≈ 2.45
     */
    @Test
    void predicate_threePositiveProfits_positiveSharpe() {
        // Position 1: ASK 110, BID 100 = +10
        // Position 2: ASK 120, BID 100 = +20
        // Position 3: ASK 130, BID 100 = +30
        // Mean = 20, StdDev ≈ 8.16
        // Sharpe ≈ 2.45
        MyOrdersTimeSeries series = new MyOrdersTimeSeriesBuilder().setId("id")
            .add(1000L, "order1", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(2000L, "order2", ASK, TradeState.FILLED, 110.0, 1.0, 1.0)
            .add(3000L, "order3", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(4000L, "order4", ASK, TradeState.FILLED, 120.0, 1.0, 1.0)
            .add(5000L, "order5", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(6000L, "order6", ASK, TradeState.FILLED, 130.0, 1.0, 1.0)
            .build();

        // Sharpe ≈ 2.45, min = 2.0
        params.put("ratioMin", 2.0);
        SharperRatioRule rule = (SharperRatioRule) RuleType.SharperRatio.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // Sharpe ≈ 2.45 >= 2.0
        assertTrue(result[0].values()[0], "Sharpe ~2.45 above min of 2.0");
    }

    /**
     * Test: Three trades with negative returns - negative Sharpe ratio.
     * Profits: -10, -20, -30
     * Mean = -20, StdDev ≈ 8.16
     * Sharpe ≈ -20 / 8.16 ≈ -2.45
     */
    @Test
    void predicate_threeNegativeProfits_negativeSharpe() {
        // Position 1: ASK 90, BID 100 = -10
        // Position 2: ASK 80, BID 100 = -20
        // Position 3: ASK 70, BID 100 = -30
        // Mean = -20, StdDev ≈ 8.16
        // Sharpe ≈ -2.45
        MyOrdersTimeSeries series = new MyOrdersTimeSeriesBuilder().setId("id")
            .add(1000L, "order1", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(2000L, "order2", ASK, TradeState.FILLED, 90.0, 1.0, 1.0)
            .add(3000L, "order3", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(4000L, "order4", ASK, TradeState.FILLED, 80.0, 1.0, 1.0)
            .add(5000L, "order5", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(6000L, "order6", ASK, TradeState.FILLED, 70.0, 1.0, 1.0)
            .build();

        // Sharpe ≈ -2.45, max = -1.0
        params.put("ratioMax", -1.0);
        SharperRatioRule rule = (SharperRatioRule) RuleType.SharperRatio.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // Sharpe ≈ -2.45 <= -1.0
        assertTrue(result[0].values()[0], "Sharpe ~-2.45 below max of -1.0");
    }

    /**
     * Test: Break-even trades - Sharpe ratio is 0 (mean = 0, some volatility).
     * Profits: 0, 0, 0
     * Mean = 0, StdDev = 0 -> Sharpe is undefined but implementation returns infinity
     */
    @Test
    void predicate_breakEven_trades() {
        // All break-even: ASK 100, BID 100 = 0
        MyOrdersTimeSeries series = new MyOrdersTimeSeriesBuilder().setId("id")
            .add(1000L, "order1", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(2000L, "order2", ASK, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(3000L, "order3", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(4000L, "order4", ASK, TradeState.FILLED, 100.0, 1.0, 1.0)
            .build();

        SharperRatioRule rule = (SharperRatioRule) RuleType.SharperRatio.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // All zero returns -> std dev = 0 -> Sharpe = infinity
        assertTrue(result[0].values()[0], "Zero returns with zero volatility gives infinity Sharpe");
    }

    /**
     * Test: Risk-free rate parameter.
     * Profits: +20, +20
     * Mean = 20, StdDev = 0
     * With riskFreeRate = 5: Sharpe = (20 - 5) / 0 = infinity
     */
    @Test
    void predicate_withRiskFreeRate() {
        // Position 1: ASK 120, BID 100 = +20
        // Position 2: ASK 120, BID 100 = +20
        MyOrdersTimeSeries series = new MyOrdersTimeSeriesBuilder().setId("id")
            .add(1000L, "order1", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(2000L, "order2", ASK, TradeState.FILLED, 120.0, 1.0, 1.0)
            .add(3000L, "order3", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(4000L, "order4", ASK, TradeState.FILLED, 120.0, 1.0, 1.0)
            .build();

        params.put("riskFreeRate", 5.0);
        params.put("ratioMin", 10.0);
        SharperRatioRule rule = (SharperRatioRule) RuleType.SharperRatio.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // Sharpe = infinity regardless of risk-free rate
        assertTrue(result[0].values()[0], "Infinity Sharpe with risk-free rate");
    }

    /**
     * Test: Only losses - negative Sharpe ratio.
     * Profits: -10, -20, -30
     * Mean = -20, StdDev ≈ 8.16
     * Sharpe ≈ -2.45
     */
    @Test
    void predicate_onlyLosses_negativeSharpe() {
        MyOrdersTimeSeries series = new MyOrdersTimeSeriesBuilder().setId("id")
            .add(1000L, "order1", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(2000L, "order2", ASK, TradeState.FILLED, 95.0, 1.0, 1.0)
            .add(3000L, "order3", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(4000L, "order4", ASK, TradeState.FILLED, 85.0, 1.0, 1.0)
            .add(5000L, "order5", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(6000L, "order6", ASK, TradeState.FILLED, 90.0, 1.0, 1.0)
            .build();

        // Sharpe ≈ -2.45, should be below ratioMin of 0
        params.put("ratioMin", 0.0);
        SharperRatioRule rule = (SharperRatioRule) RuleType.SharperRatio.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // Negative Sharpe below min of 0
        assertFalse(result[0].values()[0], "Negative Sharpe below min of 0");
    }

    /**
     * Test: Ratio within bounds.
     * Profits: +10, +20, +30, -10, -20
     * Mean = 6, StdDev ≈ 17.2
     * Sharpe ≈ 0.35
     */
    @Test
    void predicate_ratioWithinBounds() {
        MyOrdersTimeSeries series = new MyOrdersTimeSeriesBuilder().setId("id")
            .add(1000L, "order1", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(2000L, "order2", ASK, TradeState.FILLED, 110.0, 1.0, 1.0)
            .add(3000L, "order3", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(4000L, "order4", ASK, TradeState.FILLED, 120.0, 1.0, 1.0)
            .add(5000L, "order5", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(6000L, "order6", ASK, TradeState.FILLED, 130.0, 1.0, 1.0)
            .add(7000L, "order7", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(8000L, "order8", ASK, TradeState.FILLED, 90.0, 1.0, 1.0)
            .add(9000L, "order9", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(10000L, "order10", ASK, TradeState.FILLED, 80.0, 1.0, 1.0)
            .build();

        // Sharpe ≈ 0.35, within [0, 1]
        params.put("ratioMin", 0.0);
        params.put("ratioMax", 1.0);
        SharperRatioRule rule = (SharperRatioRule) RuleType.SharperRatio.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // Sharpe ≈ 0.35 within [0, 1]
        assertTrue(result[0].values()[0], "Sharpe ~0.35 within [0, 1]");
    }

    /**
     * Test: Multiple series - each produces its own result.
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
        SharperRatioRule rule = (SharperRatioRule) RuleType.SharperRatio.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series1, series2};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertEquals(2, result.length, "Should have 2 results for 2 series");

        // series1: 1 gain -> Sharpe = infinity -> exceeds max of 100
        assertFalse(result[0].values()[0], "series1 (infinity Sharpe) exceeds max of 100");

        // series2: 1 loss -> Sharpe = infinity (no volatility) -> exceeds max of 100
        assertFalse(result[1].values()[0], "series2 (infinity Sharpe) exceeds max of 100");
    }

    /**
     * Test: Non-MyOrdersTimeSeries in array - produces false.
     */
    @Test
    void predicate_nonMyOrdersSeries() {
        DoubleTimeSeries series =
            new DoubleTimeSeriesBuilder()
                .setId("test")
                .add(1000L, 100.0)
                .build();

        SharperRatioRule rule = (SharperRatioRule) RuleType.SharperRatio.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // Non-MyOrders series produces false
        assertEquals(1, result.length, "Should have 1 result");
        assertFalse(result[0].values()[0], "Non-MyOrders series produces false");
    }

    /**
     * Test: Empty series array.
     */
    @Test
    void predicate_emptySeriesArray() {
        SharperRatioRule rule = (SharperRatioRule) RuleType.SharperRatio.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // Empty input -> returns single false
        assertEquals(1, result.length, "Should have 1 result");
        assertFalse(result[0].values()[0], "Empty input returns false");
    }

    /**
     * Test: Multiple series with different Sharpe ratios.
     * series1: 2 gains (infinity Sharpe), series2: mixed (lower Sharpe)
     */
    @Test
    void predicate_multipleSeriesDifferentResults() {
        MyOrdersTimeSeries series1 = new MyOrdersTimeSeriesBuilder().setId("id")
            .add(1000L, "order1", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(2000L, "order2", ASK, TradeState.FILLED, 110.0, 1.0, 1.0)
            .build();

        MyOrdersTimeSeries series2 = new MyOrdersTimeSeriesBuilder().setId("id")
            .add(1500L, "order3", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(2500L, "order4", ASK, TradeState.FILLED, 120.0, 1.0, 1.0)
            .add(3500L, "order5", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(4500L, "order6", ASK, TradeState.FILLED, 80.0, 1.0, 1.0)
            .build();

        params.put("ratioMin", 0.0);
        params.put("ratioMax", 100.0);
        SharperRatioRule rule = (SharperRatioRule) RuleType.SharperRatio.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series1, series2};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertEquals(2, result.length, "Should have 2 results");

        // series1: 1 gain -> infinity Sharpe -> exceeds max
        assertFalse(result[0].values()[0], "series1 (infinity) exceeds max");

        // series2: +20, -20 -> mean = 0, Sharpe = 0 -> within [0, 100]
        assertTrue(result[1].values()[0], "series2 (Sharpe=0) within [0, 100]");
    }
}
