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

package com.fibonsai.cryptomeria.xtratej.event.series.dao;

import org.jspecify.annotations.Nullable;

public record MyOrdersTimeSeries(
    @Nullable String id,
    long[] timestamps,
    String[] orderIds,
    String[] symbols,
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
    String[] orderConditionsRule
) implements TimeSeries {

    public enum TradeState {
        PENDING_NEW,
        NEW,
        PARTIALLY_FILLED,
        FILLED,
        PENDING_CANCEL,
        PARTIALLY_CANCELED,
        CANCELED,
        PENDING_REPLACE,
        REPLACED,
        STOPPED,
        REJECTED,
        EXPIRED,
        OPEN,
        CLOSED,
        UNKNOWN,
        NULL,
    }

    public enum BidAskSide {
        BID,
        ASK,
        UNDEF,
    }

    public enum OrderType {
        CANCEL_ALL,
        CLOSE_ALL,
        MARKET,
        LIMIT,
        STOP,
        TRAILING_STOP,
        UNDEF,
    }

    public enum OrderCondition {
        DAY,
        GOOD_TIL_DATE,
        GOOD_TIL_CANCELED,
        IMMEDIATE_OR_CANCEL,
        FILL_OR_KILL,
        ONE_CANCELS_OTHER,
        ORDER_TRIGGER_OTHER,
    }

    public MyOrdersTimeSeries {
        if (timestamps.length > 1 && id == null) throw new RuntimeException("ID is mandatory if there is more than one value");
    }
}
