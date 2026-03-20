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

package com.fibonsai.cryptomeria.xtratej.event.series.dao.builders;

import com.fibonsai.cryptomeria.xtratej.event.series.dao.MyOrdersTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.MyOrdersTimeSeries.BidAskSide;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.MyOrdersTimeSeries.OrderCondition;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.MyOrdersTimeSeries.OrderType;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.MyOrdersTimeSeries.TradeState;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.TimeSeries;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Comparator;

public class MyOrdersTimeSeriesBuilder extends TimeSeriesBuilder<MyOrdersTimeSeriesBuilder> {

    private record Element(long timestamp,
                           String orderId,
                           String symbol,
                           BidAskSide side,
                           String owner,
                           TradeState tradeState,
                           OrderType orderType,
                           double fee,
                           double price,
                           double limitPrice,
                           double stopPrice,
                           double takeProfitPrice,
                           double trailingPrice,
                           double initialAmount,
                           double executedAmount,
                           @Nullable OrderCondition orderCondition,
                           @Nullable String orderConditionRule) {

        private Element {
            if (orderCondition == null) orderCondition = OrderCondition.GOOD_TIL_CANCELED;
        }
    }

    private Element[] elements = new Element[0];

    // tests only
    public MyOrdersTimeSeriesBuilder add(long timestamp,
                                         String orderId,
                                         BidAskSide side,
                                         TradeState tradeState,
                                         double price,
                                         double initialAmount,
                                         double executedAmount) {

        return add(timestamp, orderId, "SYMBOL", side, "OWNER", tradeState, OrderType.MARKET, 0.0,
                price, Double.NaN, Double.NaN, Double.NaN, Double.NaN, initialAmount, executedAmount,
                OrderCondition.GOOD_TIL_CANCELED, "");
    }

    public MyOrdersTimeSeriesBuilder add(long timestamp,
                                         String orderId,
                                         String symbol,
                                         BidAskSide side,
                                         String owner,
                                         TradeState tradeState,
                                         OrderType orderType,
                                         double fee,
                                         double price,
                                         double limitPrice,
                                         double stopPrice,
                                         double takeProfitPrice,
                                         double trailingPrice,
                                         double initialAmount,
                                         double executedAmount,
                                         @Nullable OrderCondition orderCondition,
                                         @Nullable String orderConditionRule) {

        if (fee < 0.0 || price < 0.0 || limitPrice < 0.0 || stopPrice < 0.0 || takeProfitPrice < 0.0 || trailingPrice < 0.0 || initialAmount < 0.0 || executedAmount < 0.0) {
            throw new RuntimeException("negative value is not allowed.");
        }

        writeLock.lock();
        try {
            Element element = new Element(timestamp, orderId, symbol, side, owner, tradeState, orderType,
                    fee, price, limitPrice, stopPrice, takeProfitPrice, trailingPrice,
                    initialAmount, executedAmount, orderCondition, orderConditionRule);
            if (elements.length >= maxSize) {
                Arrays.sort(elements, Comparator.comparingLong(Element::timestamp));
                this.elements[0] = element;
            } else {
                Element[] newElements = new Element[elements.length + 1];
                System.arraycopy(elements, 0, newElements, 0, elements.length);
                newElements[elements.length] = element;
                this.elements = newElements;
            }
        } finally {
            writeLock.unlock();
        }
        return this;
    }

    @Override
    public MyOrdersTimeSeries build() {
        readLock.lock();
        try {
            long[] _timestamps = new long[elements.length];
            String[] _orderIds = new String[elements.length];
            String[] _symbols = new String[elements.length];
            BidAskSide[] _sides = new BidAskSide[elements.length];
            String[] _owners = new String[elements.length];
            TradeState[] _tradeStates = new TradeState[elements.length];
            OrderType[] _orderTypes = new OrderType[elements.length];
            double[] _fees = new double[elements.length];
            double[] _prices = new double[elements.length];
            double[] _limitPrices = new double[elements.length];
            double[] _stopPrices = new double[elements.length];
            double[] _takeProfitPrices = new double[elements.length];
            double[] _trailingPrices = new double[elements.length];
            double[] _initialAmounts = new double[elements.length];
            double[] _executedAmounts = new double[elements.length];
            OrderCondition[] _orderConditions = new OrderCondition[elements.length];
            String[] _orderConditionRules = new String[elements.length];
            Arrays.sort(elements, Comparator.comparingLong(Element::timestamp));
            int count = 0;
            for (var element : elements) {
                _timestamps[count] = element.timestamp();
                _orderIds[count] = element.orderId();
                _symbols[count] = element.symbol();
                _sides[count] = element.side();
                _owners[count] = element.owner();
                _tradeStates[count] = element.tradeState();
                _orderTypes[count] = element.orderType();
                _fees[count] = element.fee();
                _prices[count] = element.price();
                _limitPrices[count] = element.limitPrice();
                _stopPrices[count] = element.stopPrice();
                _takeProfitPrices[count] = element.takeProfitPrice();
                _trailingPrices[count] = element.trailingPrice();
                _initialAmounts[count] = element.initialAmount();
                _executedAmounts[count] = element.executedAmount();
                _orderConditions[count] = element.orderCondition();
                _orderConditionRules[count] = element.orderConditionRule();
                count++;
            }
            return new MyOrdersTimeSeries(id, _timestamps, _orderIds, _symbols, _sides,
                    _owners, _tradeStates, _orderTypes, _fees, _prices, _limitPrices,
                    _stopPrices, _takeProfitPrices, _trailingPrices, _initialAmounts,
                    _executedAmounts, _orderConditions, _orderConditionRules);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public MyOrdersTimeSeriesBuilder from(TimeSeries timeSeries) {
        if (timeSeries instanceof MyOrdersTimeSeries(String id1,
            long[] timestamps,
            String[] orderIds,
            String[] symbols,
            BidAskSide[] sides,
            String[] owners,
            TradeState[] tradeStates,
            OrderType[] orderTypes,
            double[] fees,
            double[] prices,
            double[] limitPrices,
            double[] stopPrices,
            double[] takeProfitPrices,
            double[] trailingPrices,
            double[] initialAmounts,
            double[] executedAmounts,
            OrderCondition[] orderConditions,
            String[] orderConditionRules)) {
            for (int x = 0; x < timestamps.length; x++) {
                add(timestamps[x], orderIds[x], symbols[x], sides[x], owners[x], tradeStates[x], orderTypes[x],
                    fees[x], prices[x], limitPrices[x], stopPrices[x], takeProfitPrices[x], trailingPrices[x],
                    initialAmounts[x], executedAmounts[x], orderConditions[x], orderConditionRules[x]);
                setId(id1);
            }
        } else {
            throw new UnsupportedOperationException("%s not supported".formatted(timeSeries.getClass().getSimpleName()));
        }
        return this;
    }

    @Override
    public MyOrdersTimeSeriesBuilder merge(TimeSeries... timeSeriesArray) {
        for (var timeSeries : timeSeriesArray) {
            from(timeSeries);
        }
        return this;
    }
}
