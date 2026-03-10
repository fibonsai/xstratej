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

import java.util.TreeMap;

public class BooleanTimeSeriesBuilder extends TimeSeriesBuilder<BooleanTimeSeriesBuilder> {

    private final TreeMap<Long, Boolean> booleans = new TreeMap<>();

    public BooleanTimeSeriesBuilder add(long timestamp, boolean value) {
        writeLock.lock();
        try {
            while (booleans.size() >= maxSize) {
                booleans.remove(booleans.firstKey());
            }
            booleans.put(timestamp, value);
        } finally {
            writeLock.unlock();
        }

        return this;
    }

    @Override
    public BooleanTimeSeries build() {
        readLock.lock();
        try {
            long[] _timestamps = booleans.sequencedKeySet().stream().mapToLong(Long::longValue).toArray();
            boolean[] _booleans = new boolean[_timestamps.length];
            var iter = booleans.sequencedValues().iterator();
            for (int x = 0; x < _timestamps.length; x++) {
                _booleans[x] = iter.next();
            }
            return new BooleanTimeSeries(id, _timestamps, _booleans);
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
