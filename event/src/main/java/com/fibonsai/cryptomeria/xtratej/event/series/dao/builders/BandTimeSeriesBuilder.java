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

import com.fibonsai.cryptomeria.xtratej.event.series.dao.BandTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.TimeSeries;

import java.util.Arrays;
import java.util.Comparator;

public class BandTimeSeriesBuilder extends TimeSeriesBuilder<BandTimeSeriesBuilder> {

    private record Element(long timestamp, double upper, double middle, double lower) {}

    private Element[] elements = new Element[0];

    public BandTimeSeriesBuilder add(long timestamp, double upper, double middle, double lower) {
        writeLock.lock();
        try {
            if (elements.length >= maxSize) {
                Arrays.sort(elements, Comparator.comparingLong(Element::timestamp));
                this.elements[0] = new Element(timestamp, upper, middle, lower);
            } else {
                Element[] newElements = new Element[elements.length + 1];
                System.arraycopy(elements, 0, newElements, 0, elements.length);
                newElements[elements.length] = new Element(timestamp, upper, middle, lower);
                this.elements = newElements;
            }
        } finally {
            writeLock.unlock();
        }
        return this;
    }

    @Override
    public BandTimeSeries build() {
        readLock.lock();
        try {
            long[] _timestamps = new long[elements.length];
            double[] _uppers = new double[elements.length];
            double[] _middles = new double[elements.length];
            double[] _lowers = new double[elements.length];
            Arrays.sort(elements, Comparator.comparingLong(Element::timestamp));
            int count = 0;
            for (var element: elements) {
                _timestamps[count] = element.timestamp();
                _uppers[count] = element.upper();
                _middles[count] = element.middle();
                _lowers[count] = element.lower();
                count++;
            }
            return new BandTimeSeries(id, _timestamps, _uppers, _middles, _lowers);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public BandTimeSeriesBuilder from(TimeSeries timeSeries) {
        if (timeSeries instanceof BandTimeSeries(String id1, long[] timestamps, double[] u, double[] m, double[] l)) {
            for (int x = 0; x < timestamps.length; x++) {
                add(timestamps[x], u[x], m[x], l[x]);
                setId(id1);
            }
        }
        return this;
    }

    @Override
    public BandTimeSeriesBuilder merge(TimeSeries... timeSeriesArray) {
        for (var timeSeries: timeSeriesArray) {
            from(timeSeries);
        }
        return this;
    }
}
