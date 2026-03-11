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

import com.fibonsai.cryptomeria.xtratej.event.series.dao.Double2TimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.TimeSeries;

import java.util.Arrays;
import java.util.Comparator;

public class Double2TimeSeriesBuilder extends TimeSeriesBuilder<Double2TimeSeriesBuilder> {

    private record Element(long timestamp, double value, double value2) {}

    private Element[] elements = new Element[0];

    public Double2TimeSeriesBuilder add(long timestamp, double value, double value2) {
        writeLock.lock();
        try {
            if (elements.length >= maxSize) {
                Arrays.sort(elements, Comparator.comparingLong(Element::timestamp));
                this.elements[0] = new Element(timestamp, value, value2);
            } else {
                Element[] _elements = new Element[elements.length + 1];
                System.arraycopy(elements, 0, _elements, 0, elements.length);
                _elements[elements.length] = new Element(timestamp, value, value2);
                this.elements = _elements;
            }
        } finally {
            writeLock.unlock();
        }
        return this;
    }

    @Override
    public Double2TimeSeries build() {
        readLock.lock();
        try {
            long[] _timestamps = new long[elements.length];
            double[] _values = new double[elements.length];
            double[] _values2 = new double[elements.length];
            Arrays.sort(elements, Comparator.comparingLong(Element::timestamp));
            int count = 0;
            for (var element: elements) {
                _timestamps[count] = element.timestamp();
                _values[count] = element.value();
                _values2[count] = element.value2();
                count++;
            }
            return new Double2TimeSeries(id, _timestamps, _values, _values2);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Double2TimeSeriesBuilder from(TimeSeries timeSeries) {
        if (timeSeries instanceof Double2TimeSeries(String id1, long[] timestamps, double[] values, double[] values2)) {
            for (int x = 0; x < timestamps.length; x++) {
                add(timestamps[x], values[x], values2[x]);
                setId(id1);
            }
        }
        return this;
    }

    @Override
    public Double2TimeSeriesBuilder merge(TimeSeries... timeSeriesArray) {
        for (var timeSeries: timeSeriesArray) {
            from(timeSeries);
        }
        return this;
    }
}
