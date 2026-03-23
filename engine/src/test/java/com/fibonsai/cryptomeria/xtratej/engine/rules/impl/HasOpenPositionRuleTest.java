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
import static com.fibonsai.cryptomeria.xtratej.event.series.dao.MyOrdersTimeSeries.MINIMUM_AMOUNT_ALLOWED;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for HasOpenPositionRule.
 *
 * This rule checks if there is an open position (i.e., remaining amount to be closed).
 * It tracks net position by summing BID (buy) and subtracting ASK (sell).
 * Position is "open" when remaining amount >= MINIMUM_AMOUNT_ALLOWED.
 *
 * Returns true when there IS an open position, false when position is closed.
 */
class HasOpenPositionRuleTest {

    private ObjectNode params;

    @BeforeEach
    void setUp() {
        params = JsonNodeFactory.instance.objectNode();
    }

    /**
     * Test: Closed position - equal buy and sell.
     * Buys 1.0, Sells 1.0 -> remaining = 0 -> no open position -> false
     */
    @Test
    void predicate_closedPosition_equalBuySell() {
        MyOrdersTimeSeries series = new MyOrdersTimeSeriesBuilder().setId("id")
            .add(1000L, "order1", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(2000L, "order2", ASK, TradeState.FILLED, 120.0, 1.0, 1.0)
            .build();

        HasOpenPositionRule rule = (HasOpenPositionRule) RuleType.HasOpenPosition.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // Position closed: (1.0 - 1.0) - (1.0 - 1.0) = 0 - 0 = 0
        assertFalse(result[0].values()[0], "Closed position should return false");
    }

    /**
     * Test: Open long position - bought more than sold.
     * Buys 1.5 (executed 1.0), Sells 1.0 -> remaining = 0.5 -> open position -> true
     */
    @Test
    void predicate_openLongPosition() {
        MyOrdersTimeSeries series = new MyOrdersTimeSeriesBuilder().setId("id")
            .add(1000L, "order1", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(2000L, "order2", BID, TradeState.FILLED, 100.0, 0.5, 0.0)
            .add(3000L, "order3", ASK, TradeState.FILLED, 120.0, 1.0, 1.0)
            .build();

        HasOpenPositionRule rule = (HasOpenPositionRule) RuleType.HasOpenPosition.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // remaining = (1.0 + 0.5) - (1.0 + 0.0) - ((1.0 + 0.0) - (1.0 + 0.0))
        // Wait, let me recalculate with original formula:
        // totalAmount = 1.0 + 0.5 - 1.0 = 0.5
        // totalExecutedAmount = 1.0 + 0.0 - 1.0 = 0.0
        // remainAmount = 0.5 - 0.0 = 0.5
        assertTrue(result[0].values()[0], "Open position (0.5 remaining) should return true");
    }

    /**
     * Test: Open short position - sold more than bought.
     */
    @Test
    void predicate_openShortPosition() {
        MyOrdersTimeSeries series = new MyOrdersTimeSeriesBuilder().setId("id")
            .add(1000L, "order1", ASK, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(2000L, "order2", ASK, TradeState.FILLED, 100.0, 0.5, 0.0)
            .add(3000L, "order3", BID, TradeState.FILLED, 80.0, 1.0, 1.0)
            .build();

        HasOpenPositionRule rule = (HasOpenPositionRule) RuleType.HasOpenPosition.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertFalse(result[0].values()[0], "Selling more than you have: error, than return false");
    }

    /**
     * Test: Close position completely.
     * Buys 1.0, Sells 1.0 -> remaining = 0 -> no open position -> false
     */
    @Test
    void predicate_positionCompletelyClosed() {
        MyOrdersTimeSeries series = new MyOrdersTimeSeriesBuilder().setId("id")
            .add(1000L, "order1", BID, TradeState.FILLED, 100.0, 2.0, 2.0)
            .add(2000L, "order2", ASK, TradeState.FILLED, 110.0, 2.0, 2.0)
            .build();

        HasOpenPositionRule rule = (HasOpenPositionRule) RuleType.HasOpenPosition.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // remaining = (2.0 - 2.0) - (2.0 - 2.0) = 0 - 0 = 0
        assertFalse(result[0].values()[0], "Fully closed position should return false");
    }

    /**
     * Test: Partially filled order - remaining amount to be filled.
     * Initial: 1.0, Executed: 0.5 -> remaining = 0.5 -> open position -> true
     */
    @Test
    void predicate_partiallyFilledOrder() {
        MyOrdersTimeSeries series = new MyOrdersTimeSeriesBuilder().setId("id")
            .add(1000L, "order1", BID, TradeState.FILLED, 100.0, 1.0, 0.5)
            .build();

        HasOpenPositionRule rule = (HasOpenPositionRule) RuleType.HasOpenPosition.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // Partially filled: 0.5 executed >= 1e-12
        assertTrue(result[0].values()[0], "Partially filled order has open position");
    }

    /**
     * Test: Multiple positions - net position calculation.
     * Buys: 1.0 + 0.5 = 1.5, Sells: 1.0 + 0.3 = 1.3 -> remaining = 0.2 -> true
     */
    @Test
    void predicate_multiplePositions_netPosition() {
        MyOrdersTimeSeries series = new MyOrdersTimeSeriesBuilder().setId("id")
            .add(1000L, "order1", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(2000L, "order2", BID, TradeState.FILLED, 100.0, 0.5, 0.5)
            .add(3000L, "order3", ASK, TradeState.FILLED, 110.0, 1.0, 1.0)
            .add(4000L, "order4", ASK, TradeState.FILLED, 110.0, 0.3, 0.3)
            .build();

        HasOpenPositionRule rule = (HasOpenPositionRule) RuleType.HasOpenPosition.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // Net: (1.0 + 0.5) - (1.0 + 0.3) = 0.2
        // remaining = (1.5 - 1.3) = 0.2 >= 1e-12
        assertTrue(result[0].values()[0], "Net position 0.2 is open");
    }

    /**
     * Test: Minimum amount threshold.
     * Remaining = MINIMUM_AMOUNT_ALLOWED - epsilon -> no open position -> false
     */
    @Test
    void predicate_minimumAmountThreshold() {
        double epsilon = MINIMUM_AMOUNT_ALLOWED / 2.0;
        MyOrdersTimeSeries series = new MyOrdersTimeSeriesBuilder().setId("id")
            .add(1000L, "order1", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(2000L, "order2", ASK, TradeState.FILLED, 100.0, 1.0, 1.0 - epsilon)
            .build();

        HasOpenPositionRule rule = (HasOpenPositionRule) RuleType.HasOpenPosition.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // Remaining is approximately MINIMUM_AMOUNT_ALLOWED
        // The rule checks if remaining < MINIMUM_AMOUNT_ALLOWED
        assertFalse(result[0].values()[0], "Remaining > MINIMUM_AMOUNT_ALLOWED is still open");
    }

    /**
     * Test: Remaining amount at threshold.
     * Remaining = MINIMUM_AMOUNT_ALLOWED -> still open (>= threshold) -> true
     */
    @Test
    void predicate_remainingAtThreshold() {
        MyOrdersTimeSeries series = new MyOrdersTimeSeriesBuilder().setId("id")
            .add(1000L, "order1", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(2000L, "order2", ASK, TradeState.FILLED, 100.0, MINIMUM_AMOUNT_ALLOWED, MINIMUM_AMOUNT_ALLOWED)
            .build();

        HasOpenPositionRule rule = (HasOpenPositionRule) RuleType.HasOpenPosition.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // Remaining = MINIMUM_AMOUNT_ALLOWED -> still open
        assertTrue(result[0].values()[0], "Remaining = MINIMUM_AMOUNT_ALLOWED is still open");
    }

    /**
     * Test: No trades - no open position.
     */
    @Test
    void predicate_noTrades() {
        MyOrdersTimeSeries series = new MyOrdersTimeSeriesBuilder().setId("id").build();

        HasOpenPositionRule rule = (HasOpenPositionRule) RuleType.HasOpenPosition.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // No trades -> no position
        assertFalse(result[0].values()[0], "No trades means no open position");
    }

    /**
     * Test: Multiple series - each produces its own result.
     */
    @Test
    void predicate_multipleSeries() {
        MyOrdersTimeSeries series1 = new MyOrdersTimeSeriesBuilder().setId("id")
            .add(1000L, "order1", BID, TradeState.FILLED, 100.0, 1.0, 0.5)
            .build();

        MyOrdersTimeSeries series2 = new MyOrdersTimeSeriesBuilder().setId("id")
            .add(1500L, "order2", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(2500L, "order3", ASK, TradeState.FILLED, 110.0, 1.0, 1.0)
            .build();

        HasOpenPositionRule rule = (HasOpenPositionRule) RuleType.HasOpenPosition.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series1, series2};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertEquals(2, result.length, "Should have 2 results");

        // series1: 1.0 bought (executed 0.5), 0 sold -> remaining = |1.0 - 0.5| = 0.5 >= 1e-12 -> true
        assertTrue(result[0].values()[0], "series1 has open position");

        // series2: 1.0 bought, 1.0 sold -> remaining = |1.0 - 1.0| = 0 < 1e-12 -> false
        assertFalse(result[1].values()[0], "series2 position closed");
    }

    /**
     * Test: Non-MyOrdersTimeSeries - produces false.
     */
    @Test
    void predicate_nonMyOrdersSeries() {
        DoubleTimeSeries series =
            new DoubleTimeSeriesBuilder()
                .setId("test")
                .add(1000L, 100.0)
                .build();

        HasOpenPositionRule rule = (HasOpenPositionRule) RuleType.HasOpenPosition.build().setParams(params);
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
        HasOpenPositionRule rule = (HasOpenPositionRule) RuleType.HasOpenPosition.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // Empty input -> returns single false
        assertEquals(1, result.length, "Should have 1 result");
        assertFalse(result[0].values()[0], "Empty input returns false");
    }

    /**
     * Test: Large position size.
     */
    @Test
    void predicate_largePosition() {
        MyOrdersTimeSeries series = new MyOrdersTimeSeriesBuilder().setId("id")
            .add(1000L, "order1", BID, TradeState.FILLED, 100.0, 100.0, 50.0)
            .build();

        HasOpenPositionRule rule = (HasOpenPositionRule) RuleType.HasOpenPosition.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // 100 bought, 50 sold -> 50 remaining
        assertTrue(result[0].values()[0], "Large position 50 is open");
    }

    /**
     * Test: All trades executed - position closed.
     */
    @Test
    void predicate_allTradesExecuted() {
        MyOrdersTimeSeries series = new MyOrdersTimeSeriesBuilder().setId("id")
            .add(1000L, "order1", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(2000L, "order2", BID, TradeState.FILLED, 100.0, 0.5, 0.5)
            .add(3000L, "order3", ASK, TradeState.FILLED, 110.0, 1.0, 1.0)
            .add(4000L, "order4", ASK, TradeState.FILLED, 110.0, 0.5, 0.5)
            .build();

        HasOpenPositionRule rule = (HasOpenPositionRule) RuleType.HasOpenPosition.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // All positions closed: (1.0 + 0.5) - (1.0 + 0.5) = 0
        assertFalse(result[0].values()[0], "All trades executed means no open position");
    }
}
