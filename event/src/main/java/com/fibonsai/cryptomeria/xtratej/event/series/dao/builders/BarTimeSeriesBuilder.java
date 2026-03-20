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

import com.fibonsai.cryptomeria.xtratej.event.series.dao.BarTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.Double2TimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.DoubleTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.TimeSeries;

import java.util.Arrays;
import java.util.Comparator;

public class BarTimeSeriesBuilder extends TimeSeriesBuilder<BarTimeSeriesBuilder> {

    private record Element(long timestamp, double open, double high, double low, double close, double volume) {}

    private Element[] elements = new Element[0];

    public BarTimeSeriesBuilder add(long timestamp, double open, double high, double low, double close, double volume) {

        if (open < 0.0 || high < 0.0 || low < 0.0 || close < 0.0 || volume < 0.0) {
            throw new RuntimeException("negative value is not allowed.");
        }

        writeLock.lock();
        try {
            Element element = new Element(timestamp, open, high, low, close, volume);
            if (elements.length >= maxSize) {
                Arrays.sort(elements, Comparator.comparingLong(Element::timestamp));
                this.elements[0] = element;
            } else {
                Element[] _elements = new Element[elements.length + 1];
                System.arraycopy(elements, 0, _elements, 0, elements.length);
                _elements[elements.length] = element;
                this.elements = _elements;
            }
        } finally {
            writeLock.unlock();
        }
        return this;
    }

    @Override
    public BarTimeSeries build() {
        readLock.lock();
        try {
            long[] _timestamps = new long[elements.length];
            double[] _opens = new double[elements.length];
            double[] _highs = new double[elements.length];
            double[] _lows = new double[elements.length];
            double[] _closes = new double[elements.length];
            double[] _volumes = new double[elements.length];
            Arrays.sort(elements, Comparator.comparingLong(Element::timestamp));
            int count = 0;
            for (var element: elements) {
                _timestamps[count] = element.timestamp();
                _opens[count] = element.open();
                _highs[count] = element.high();
                _lows[count] = element.low();
                _closes[count] = element.close();
                _volumes[count] = element.volume();
                count++;
            }
            return new BarTimeSeries(id, _timestamps, _opens, _highs, _lows, _closes, _volumes);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public BarTimeSeriesBuilder from(TimeSeries timeSeries) {
        switch (timeSeries) {
            case BarTimeSeries(String id1, long[] timestamps, double[] o, double[] h, double[] l, double[] c, double[] v) -> {
                for (int x = 0; x < timestamps.length; x++) {
                    add(timestamps[x], o[x], h[x], l[x], c[x], v[x]);
                    setId(id1);
                }
            }
            case DoubleTimeSeries(String id1, long[]timestamps, double[] values) -> {
                for (int x = 0; x < timestamps.length; x++) {
                    add(timestamps[x], values[x], values[x], values[x], values[x], 1.0);
                    setId(id1);
                }
            }
            case Double2TimeSeries(String id1, long[]timestamps, double[] values, double[] values2) -> {
                for (int x = 0; x < timestamps.length; x++) {
                    add(timestamps[x], values[x], values[x], values[x], values[x], values2[x]);
                    setId(id1);
                }
            }
            default -> throw new UnsupportedOperationException("%s not supported".formatted(timeSeries.getClass().getSimpleName()));
        }
        return this;
    }

    @Override
    public BarTimeSeriesBuilder merge(TimeSeries... timeSeriesArray) {
        for (var timeSeries: timeSeriesArray) {
            from(timeSeries);
        }
        return this;
    }
}
