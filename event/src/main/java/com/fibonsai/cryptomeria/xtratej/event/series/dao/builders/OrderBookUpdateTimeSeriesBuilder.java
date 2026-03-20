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

import com.fibonsai.cryptomeria.xtratej.event.series.dao.OrderBookUpdateTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.TimeSeries;

import java.util.Arrays;
import java.util.Comparator;

public class OrderBookUpdateTimeSeriesBuilder extends TimeSeriesBuilder<OrderBookUpdateTimeSeriesBuilder> {

    private record Element(long timestamp,
                           String bidOrderId,
                           String askOrderId,
                           double bidPrice,
                           double askPrice,
                           double bidAmount,
                           double askAmount,
                           double spread) {}

    private Element[] elements = new Element[0];

    public OrderBookUpdateTimeSeriesBuilder add(long timestamp,
                                                String bidOrderId,
                                                String askOrderId,
                                                double bidPrice,
                                                double askPrice,
                                                double bidAmount,
                                                double askAmount,
                                                double spread) {

        if (bidPrice < 0.0 || askPrice < 0.0 || bidAmount < 0.0 || askAmount < 0.0 || spread < 0.0) {
            throw new RuntimeException("negative value is not allowed.");
        }

        writeLock.lock();
        try {
            Element element = new Element(timestamp, bidOrderId, askOrderId, bidPrice, askPrice, bidAmount, askAmount, spread);
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
    public OrderBookUpdateTimeSeries build() {
        readLock.lock();
        try {
            long[] _timestamps = new long[elements.length];
            String[] _bidOrderIds = new String[elements.length];
            String[] _askOrderIds = new String[elements.length];
            double[] _bidPrices = new double[elements.length];
            double[] _askPrices = new double[elements.length];
            double[] _bidAmounts = new double[elements.length];
            double[] _askAmounts = new double[elements.length];
            double[] _spreads = new double[elements.length];
            Arrays.sort(elements, Comparator.comparingLong(Element::timestamp));
            int count = 0;
            for (var element : elements) {
                _timestamps[count] = element.timestamp();
                _bidOrderIds[count] = element.bidOrderId();
                _askOrderIds[count] = element.askOrderId();
                _bidPrices[count] = element.bidPrice();
                _askPrices[count] = element.askPrice();
                _bidAmounts[count] = element.bidAmount();
                _askAmounts[count] = element.askAmount();
                _spreads[count] = element.spread();
                count++;
            }
            return new OrderBookUpdateTimeSeries(id, _timestamps, _bidOrderIds, _askOrderIds,
                _bidPrices, _askPrices, _bidAmounts, _askAmounts, _spreads);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public OrderBookUpdateTimeSeriesBuilder from(TimeSeries timeSeries) {
        if (timeSeries instanceof OrderBookUpdateTimeSeries(String id1,
            long[] timestamps,
            String[] bidOrderIds,
            String[] askOrderIds,
            double[] bidPrices,
            double[] askPrices,
            double[] bidAmounts,
            double[] askAmounts,
            double[] spreads)) {
            for (int x = 0; x < timestamps.length; x++) {
                add(timestamps[x], bidOrderIds[x], askOrderIds[x], bidPrices[x], askPrices[x], bidAmounts[x], askAmounts[x], spreads[x]);
                setId(id1);
            }
        } else {
            throw new UnsupportedOperationException("%s not supported".formatted(timeSeries.getClass().getSimpleName()));
        }
        return this;
    }

    @Override
    public OrderBookUpdateTimeSeriesBuilder merge(TimeSeries... timeSeriesArray) {
        for (var timeSeries : timeSeriesArray) {
            from(timeSeries);
        }
        return this;
    }
}
