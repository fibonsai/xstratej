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

import com.fibonsai.cryptomeria.xtratej.event.series.dao.BooleanTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.TimeSeries;

import java.util.Arrays;
import java.util.Comparator;

public class BooleanTimeSeriesBuilder extends TimeSeriesBuilder<BooleanTimeSeriesBuilder> {

    private record Element(long timestamp, boolean value) {}

    private Element[] elements = new Element[0];

    public BooleanTimeSeriesBuilder add(long timestamp, boolean value) {
        writeLock.lock();
        try {
            if (elements.length >= maxSize) {
                Arrays.sort(elements, Comparator.comparingLong(Element::timestamp));
                this.elements[0] = new Element(timestamp, value);
            } else {
                Element[] _elements = new Element[elements.length + 1];
                System.arraycopy(elements, 0, _elements, 0, elements.length);
                _elements[elements.length] = new Element(timestamp, value);
                this.elements = _elements;
            }
        } finally {
            writeLock.unlock();
        }
        return this;
    }

    @Override
    public BooleanTimeSeries build() {
        readLock.lock();
        try {
            long[] _timestamps = new long[elements.length];
            boolean[] _values = new boolean[elements.length];
            Arrays.sort(elements, Comparator.comparingLong(Element::timestamp));
            int count = 0;
            for (var element: elements) {
                _timestamps[count] = element.timestamp();
                _values[count] = element.value();
                count++;
            }
            return new BooleanTimeSeries(id, _timestamps, _values);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public BooleanTimeSeriesBuilder from(TimeSeries timeSeries) {
        if (timeSeries instanceof BooleanTimeSeries(String id1, long[] timestamps, boolean[] values)) {
            for (int x = 0; x < timestamps.length; x++) {
                add(timestamps[x], values[x]);
                if (id1 != null) setId(id1);
            }
        }
        return this;
    }

    @Override
    public BooleanTimeSeriesBuilder merge(TimeSeries... timeSeriesArray) {
        for (var timeSeries: timeSeriesArray) {
            from(timeSeries);
        }
        return this;
    }
}
