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

import static com.fibonsai.cryptomeria.xtratej.event.series.dao.MyOrdersTimeSeries.BidAskSide.BID;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ActiveOrderRule.
 *
 * This rule counts active orders and checks if the count is within specified bounds.
 * Active states include:
 * - NEW, OPEN, PARTIALLY_FILLED, PARTIALLY_CANCELED
 * - PENDING_REPLACE, PENDING_NEW, REPLACED, STOPPED
 *
 * Non-active states (filled, canceled, rejected, expired, closed) are NOT counted.
 */
class ActiveOrderRuleTest {

    private ObjectNode params;

    @BeforeEach
    void setUp() {
        params = JsonNodeFactory.instance.objectNode();
    }

    /**
     * Test: All active orders within range.
     * 3 active orders (NEW, OPEN, PARTIALLY_FILLED) -> true
     */
    @Test
    void predicate_allActiveOrders() {
        MyOrdersTimeSeries series = new MyOrdersTimeSeriesBuilder().setId("id")
            .add(1000L, "order1", BID, TradeState.NEW, 100.0, 1.0, 0.0)
            .add(2000L, "order2", BID, TradeState.OPEN, 100.0, 1.0, 0.0)
            .add(3000L, "order3", BID, TradeState.PARTIALLY_FILLED, 100.0, 1.0, 0.3)
            .build();

        params.put("min", 1);
        params.put("max", 5);
        ActiveOrderRule rule = (ActiveOrderRule) RuleType.ActiveOrder.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // 3 active orders within [1, 5]
        assertTrue(result[0].values()[0], "3 active orders within [1, 5]");
    }

    /**
     * Test: No active orders - all filled/canceled.
     * 0 active orders -> false
     */
    @Test
    void predicate_noActiveOrders() {
        MyOrdersTimeSeries series = new MyOrdersTimeSeriesBuilder().setId("id")
            .add(1000L, "order1", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(2000L, "order2", BID, TradeState.CANCELED, 100.0, 1.0, 0.0)
            .build();

        params.put("min", 1);
        ActiveOrderRule rule = (ActiveOrderRule) RuleType.ActiveOrder.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // 0 active orders, min = 1 -> false
        assertFalse(result[0].values()[0], "0 active orders below min of 1");
    }

    /**
     * Test: Below minimum threshold.
     * 2 active orders, min = 3 -> false
     */
    @Test
    void predicate_belowMinimum() {
        MyOrdersTimeSeries series = new MyOrdersTimeSeriesBuilder().setId("id")
            .add(1000L, "order1", BID, TradeState.NEW, 100.0, 1.0, 0.0)
            .add(2000L, "order2", BID, TradeState.OPEN, 100.0, 1.0, 0.0)
            .build();

        params.put("min", 3);
        params.put("max", 5);
        ActiveOrderRule rule = (ActiveOrderRule) RuleType.ActiveOrder.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // 2 active orders < min of 3 -> false
        assertFalse(result[0].values()[0], "2 active orders below min of 3");
    }

    /**
     * Test: Above maximum threshold.
     * 5 active orders, max = 3 -> false
     */
    @Test
    void predicate_aboveMaximum() {
        MyOrdersTimeSeries series = new MyOrdersTimeSeriesBuilder().setId("id")
            .add(1000L, "order1", BID, TradeState.NEW, 100.0, 1.0, 0.0)
            .add(2000L, "order2", BID, TradeState.OPEN, 100.0, 1.0, 0.0)
            .add(3000L, "order3", BID, TradeState.PARTIALLY_FILLED, 100.0, 1.0, 0.3)
            .add(4000L, "order4", BID, TradeState.PENDING_NEW, 100.0, 1.0, 0.0)
            .add(5000L, "order5", BID, TradeState.REPLACED, 100.0, 1.0, 0.0)
            .build();

        params.put("min", 1);
        params.put("max", 3);
        ActiveOrderRule rule = (ActiveOrderRule) RuleType.ActiveOrder.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // 5 active orders > max of 3 -> false
        assertFalse(result[0].values()[0], "5 active orders above max of 3");
    }

    /**
     * Test: Exactly at minimum.
     * 3 active orders, min = 3 -> true
     */
    @Test
    void predicate_atMinimum() {
        MyOrdersTimeSeries series = new MyOrdersTimeSeriesBuilder().setId("id")
            .add(1000L, "order1", BID, TradeState.NEW, 100.0, 1.0, 0.0)
            .add(2000L, "order2", BID, TradeState.OPEN, 100.0, 1.0, 0.0)
            .add(3000L, "order3", BID, TradeState.PARTIALLY_FILLED, 100.0, 1.0, 0.3)
            .build();

        params.put("min", 3);
        ActiveOrderRule rule = (ActiveOrderRule) RuleType.ActiveOrder.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // 3 active orders = min of 3 -> true
        assertTrue(result[0].values()[0], "3 active orders equals min of 3");
    }

    /**
     * Test: Exactly at maximum.
     * 5 active orders, max = 5 -> true
     */
    @Test
    void predicate_atMaximum() {
        MyOrdersTimeSeries series = new MyOrdersTimeSeriesBuilder().setId("id")
            .add(1000L, "order1", BID, TradeState.NEW, 100.0, 1.0, 0.0)
            .add(2000L, "order2", BID, TradeState.OPEN, 100.0, 1.0, 0.0)
            .add(3000L, "order3", BID, TradeState.PARTIALLY_FILLED, 100.0, 1.0, 0.3)
            .add(4000L, "order4", BID, TradeState.PENDING_NEW, 100.0, 1.0, 0.0)
            .add(5000L, "order5", BID, TradeState.REPLACED, 100.0, 1.0, 0.0)
            .build();

        params.put("max", 5);
        ActiveOrderRule rule = (ActiveOrderRule) RuleType.ActiveOrder.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // 5 active orders = max of 5 -> true
        assertTrue(result[0].values()[0], "5 active orders equals max of 5");
    }

    /**
     * Test: Mixed active and inactive states.
     * 3 active (NEW, OPEN, PENDING_REPLACE) + 3 inactive (FILLED, CANCELED, REJECTED) -> true
     */
    @Test
    void predicate_mixedStates() {
        MyOrdersTimeSeries series = new MyOrdersTimeSeriesBuilder().setId("id")
            .add(1000L, "order1", BID, TradeState.NEW, 100.0, 1.0, 0.0)
            .add(2000L, "order2", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(3000L, "order3", BID, TradeState.OPEN, 100.0, 1.0, 0.0)
            .add(4000L, "order4", BID, TradeState.CANCELED, 100.0, 1.0, 0.0)
            .add(5000L, "order5", BID, TradeState.PENDING_REPLACE, 100.0, 1.0, 0.0)
            .add(6000L, "order6", BID, TradeState.REJECTED, 100.0, 1.0, 0.0)
            .build();

        params.put("min", 1);
        params.put("max", 5);
        ActiveOrderRule rule = (ActiveOrderRule) RuleType.ActiveOrder.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // 3 active orders within [1, 5]
        assertTrue(result[0].values()[0], "3 active (NEW, OPEN, PENDING_REPLACE) within [1, 5]");
    }

    /**
     * Test: All active states from the ACTIVE_STATES list.
     */
    @Test
    void predicate_allActiveStates() {
        MyOrdersTimeSeries series = new MyOrdersTimeSeriesBuilder().setId("id")
            .add(1000L, "order1", BID, TradeState.NEW, 100.0, 1.0, 0.0)
            .add(2000L, "order2", BID, TradeState.OPEN, 100.0, 1.0, 0.0)
            .add(3000L, "order3", BID, TradeState.PARTIALLY_FILLED, 100.0, 1.0, 0.3)
            .add(4000L, "order4", BID, TradeState.PARTIALLY_CANCELED, 100.0, 1.0, 0.7)
            .add(5000L, "order5", BID, TradeState.PENDING_REPLACE, 100.0, 1.0, 0.0)
            .add(6000L, "order6", BID, TradeState.PENDING_NEW, 100.0, 1.0, 0.0)
            .add(7000L, "order7", BID, TradeState.REPLACED, 100.0, 1.0, 0.0)
            .add(8000L, "order8", BID, TradeState.STOPPED, 100.0, 1.0, 0.0)
            .build();

        params.put("min", 5);
        params.put("max", 10);
        ActiveOrderRule rule = (ActiveOrderRule) RuleType.ActiveOrder.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // 8 active orders within [5, 10]
        assertTrue(result[0].values()[0], "8 active orders within [5, 10]");
    }

    /**
     * Test: Inactive states - FILLED, CANCELED, REJECTED, EXPIRED, CLOSED.
     * These should NOT be counted as active.
     */
    @Test
    void predicate_inactiveStates() {
        MyOrdersTimeSeries series = new MyOrdersTimeSeriesBuilder().setId("id")
            .add(1000L, "order1", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .add(2000L, "order2", BID, TradeState.CANCELED, 100.0, 1.0, 0.0)
            .add(3000L, "order3", BID, TradeState.REJECTED, 100.0, 1.0, 0.0)
            .add(4000L, "order4", BID, TradeState.EXPIRED, 100.0, 1.0, 0.0)
            .add(5000L, "order5", BID, TradeState.CLOSED, 100.0, 1.0, 0.0)
            .build();

        params.put("min", 1);
        ActiveOrderRule rule = (ActiveOrderRule) RuleType.ActiveOrder.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // 0 active orders (all are inactive) -> false
        assertFalse(result[0].values()[0], "0 active orders, all are inactive states");
    }

    /**
     * Test: Empty series - no orders.
     */
    @Test
    void predicate_noOrders() {
        MyOrdersTimeSeries series = new MyOrdersTimeSeriesBuilder().setId("id").build();

        ActiveOrderRule rule = (ActiveOrderRule) RuleType.ActiveOrder.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // 0 orders -> 0 active -> false (default min is 0)
        // With default min=0, max=infinity, 0 >= 0 is true
        assertTrue(result[0].values()[0], "0 active orders within [0, infinity]");
    }

    /**
     * Test: Default params (min=0, max=infinity) - any count is valid.
     */
    @Test
    void predicate_defaultParams() {
        MyOrdersTimeSeries series = new MyOrdersTimeSeriesBuilder().setId("id")
            .add(1000L, "order1", BID, TradeState.NEW, 100.0, 1.0, 0.0)
        .build();

        ActiveOrderRule rule = (ActiveOrderRule) RuleType.ActiveOrder.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // With default params, 1 active order is valid
        assertTrue(result[0].values()[0], "1 active order within [0, infinity]");
    }

    /**
     * Test: Multiple series - each produces its own result.
     */
    @Test
    void predicate_multipleSeries() {
        MyOrdersTimeSeries series1 = new MyOrdersTimeSeriesBuilder().setId("series1")
            .add(1000L, "order1", BID, TradeState.NEW, 100.0, 1.0, 0.0)
            .add(2000L, "order2", BID, TradeState.OPEN, 100.0, 1.0, 0.0)
            .build();

        MyOrdersTimeSeries series2 = new MyOrdersTimeSeriesBuilder().setId("series2")
            .add(1500L, "order3", BID, TradeState.FILLED, 100.0, 1.0, 1.0)
            .build();

        params.put("min", 1);
        params.put("max", 5);
        ActiveOrderRule rule = (ActiveOrderRule) RuleType.ActiveOrder.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{series1, series2};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertEquals(2, result.length, "Should have 2 results");

        // series1: 2 active orders within [1, 5] -> true
        assertTrue(result[0].values()[0], "series1 has 2 active orders");

        // series2: 0 active orders -> false
        assertFalse(result[1].values()[0], "series2 has 0 active orders");
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

        ActiveOrderRule rule = (ActiveOrderRule) RuleType.ActiveOrder.build().setParams(params);
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
        ActiveOrderRule rule = (ActiveOrderRule) RuleType.ActiveOrder.build().setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // Empty input -> returns single false
        assertEquals(1, result.length, "Should have 1 result");
        assertFalse(result[0].values()[0], "Empty input returns false");
    }
}
