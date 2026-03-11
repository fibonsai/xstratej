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

import com.fibonsai.cryptomeria.xtratej.event.series.dao.DoubleTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.TimeSeries;

import java.util.Arrays;
import java.util.Comparator;

public class DoubleTimeSeriesBuilder extends TimeSeriesBuilder<DoubleTimeSeriesBuilder> {

    private record Element(long timestamp, double value) {}

    private Element[] elements = new Element[0];

    public DoubleTimeSeriesBuilder add(long timestamp, double value) {
        writeLock.lock();
        try {
            if (elements.length >= maxSize) {
                Arrays.sort(elements, Comparator.comparingLong(Element::timestamp));
                this.elements[0] = new Element(timestamp, value);
            } else {
                Element[] newElements = new Element[elements.length + 1];
                System.arraycopy(elements, 0, newElements, 0, elements.length);
                newElements[elements.length] = new Element(timestamp, value);
                this.elements = newElements;
            }
        } finally {
            writeLock.unlock();
        }
        return this;
    }

    @Override
    public DoubleTimeSeries build() {
        readLock.lock();
        try {
            long[] _timestamps = new long[elements.length];
            double[] _values = new double[elements.length];
            Arrays.sort(elements, Comparator.comparingLong(Element::timestamp));
            int count = 0;
            for (var element: elements) {
                _timestamps[count] = element.timestamp();
                _values[count] = element.value();
                count++;
            }
            return new DoubleTimeSeries(id, _timestamps, _values);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public DoubleTimeSeriesBuilder from(TimeSeries timeSeries) {
        if (timeSeries instanceof DoubleTimeSeries(String id1, long[] timestamps, double[] values)) {
            for (int x = 0; x < timestamps.length; x++) {
                add(timestamps[x], values[x]);
                setId(id1);
            }
        }
        return this;
    }

    @Override
    public DoubleTimeSeriesBuilder merge(TimeSeries... timeSeriesArray) {
        for (var timeSeries: timeSeriesArray) {
            from(timeSeries);
        }
        return this;
    }
}
