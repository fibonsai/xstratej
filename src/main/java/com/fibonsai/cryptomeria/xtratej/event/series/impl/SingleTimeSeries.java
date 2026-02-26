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

package com.fibonsai.cryptomeria.xtratej.event.series.impl;

import com.fibonsai.cryptomeria.xtratej.event.ITemporalData;
import com.fibonsai.cryptomeria.xtratej.event.series.TimeSeries;

public class SingleTimeSeries extends TimeSeries {

    private double[] values;

    public record Single(long timestamp, double value) implements ITemporalData {}

    public SingleTimeSeries(String id, Single[] singles) {
        super(id);
        long[] _timestamps = new long[singles.length];
        double[] _values = new double[singles.length];
        for (int x = 0; x < singles.length; x++) {
            _timestamps[x] = singles[x].timestamp();
            _values[x] = singles[x].value();
        }
        this.timestamps = _timestamps;
        this.values = _values;
        this.maxSize = Math.max(timestamps.length, 1);
    }

    public SingleTimeSeries(String id, Single[] singles, int maxSize) {
        this(id, singles);
        this.maxSize = Math.max(1, maxSize);
    }

    public SingleTimeSeries(String id) {
        this(id, new Single[0]);
    }

    public SingleTimeSeries(String id, int maxSize) {
        this(id, new Single[0], maxSize);
    }

    public TimeSeries add(Single single) {
        writeLock.lock();
        try {
            int newSize;
            int seriesStart;
            if (timestamps.length == maxSize) {
                newSize = maxSize;
                seriesStart = 1;
            } else {
                newSize = timestamps.length + 1;
                seriesStart = 0;
            }

            int idx = getIdx(single);
            updateTimestamps(single, newSize, idx, seriesStart);
            this.values = addToArray(single.value(), this.values, newSize, idx, seriesStart);

        } finally {
            writeLock.unlock();
        }
        return this;
    }

    @Override
    public double[] minmax() {
        readLock.lock();
        try {
            double[] minmax = minmax(values);
            if (minmax.length == 0) {
                throw new RuntimeException("Not possible to calculate minmax: values is empty");
            }
            return minmax;
        } finally {
            readLock.unlock();
        }
    }

    public double[] values() {
        readLock.lock();
        try {
            errorIfEmpty();
            return values;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public double[] singleDoubleValues() {
        return values();
    }

    @Override
    public boolean[] singleBooleanValues() {
        return new boolean[0];
    }
}
