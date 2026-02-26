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

public class BooleanSingleTimeSeries extends TimeSeries {

    private boolean[] values;

    public record BooleanSingle(long timestamp, boolean value) implements ITemporalData {}

    public BooleanSingleTimeSeries(String id, BooleanSingle[] singles) {
        super(id);
        long[] _timestamps = new long[singles.length];
        boolean[] _values = new boolean[singles.length];
        for (int x = 0; x < singles.length; x++) {
            _timestamps[x] = singles[x].timestamp();
            _values[x] = singles[x].value();
        }
        this.timestamps = _timestamps;
        this.values = _values;
        this.maxSize = Math.max(timestamps.length, 1);
    }

    public BooleanSingleTimeSeries(String id, BooleanSingle[] singles, int maxSize) {
        this(id, singles);
        this.maxSize = Math.max(1, maxSize);
    }

    public BooleanSingleTimeSeries(String id) {
        this(id, new BooleanSingle[0]);
    }

    public BooleanSingleTimeSeries(String id, int maxSize) {
        this(id, new BooleanSingle[0], maxSize);
    }

    public TimeSeries add(BooleanSingle single) {
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

    public boolean[] values() {
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
        return new double[0];
    }

    @Override
    public boolean[] singleBooleanValues() {
        return values();
    }
}
