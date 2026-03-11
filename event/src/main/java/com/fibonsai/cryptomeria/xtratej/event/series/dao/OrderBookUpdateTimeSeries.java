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

public record OrderBookUpdateTimeSeries(
        @Nullable String id,
        long[] timestamps,
        String[] bidOrderIds,
        String[] askOrderIds,
        double[] bidPrices,
        double[] askPrices,
        double[] bidAmounts,
        double[] askAmounts,
        double[] spreads
) implements TimeSeries {

    public OrderBookUpdateTimeSeries {
        if (timestamps.length > 1 && id == null) throw new RuntimeException("ID is mandatory if there is more than one value");
    }
}