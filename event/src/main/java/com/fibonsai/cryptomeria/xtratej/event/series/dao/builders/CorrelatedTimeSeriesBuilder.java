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

import com.fibonsai.cryptomeria.xtratej.event.series.dao.CorrelatedTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.TimeSeries;

import java.util.TreeMap;

public class CorrelatedTimeSeriesBuilder extends TimeSeriesBuilder<CorrelatedTimeSeriesBuilder> {

    private final TreeMap<Long, Double> doubles = new TreeMap<>();
    private final TreeMap<Long, Double> doubles2 = new TreeMap<>();

    public CorrelatedTimeSeriesBuilder add(long timestamp, double value, double value2) {
        writeLock.lock();
        try {
            while (doubles.size() >= maxSize) {
                doubles.remove(doubles.firstKey());
            }
            while (doubles2.size() >= maxSize) {
                doubles2.remove(doubles2.firstKey());
            }
            doubles.put(timestamp, value);
            doubles2.put(timestamp, value2);
        } finally {
            writeLock.unlock();
        }

        return this;
    }


    @Override
    public CorrelatedTimeSeries build() {
        readLock.lock();
        try {
            long[] _timestamps = doubles.sequencedKeySet().stream().mapToLong(Long::longValue).toArray();
            double[] _doubles = doubles.sequencedValues().stream().mapToDouble(Double::doubleValue).toArray();
            double[] _doubles2 = doubles2.sequencedValues().stream().mapToDouble(Double::doubleValue).toArray();
            return new CorrelatedTimeSeries(id, _timestamps, _doubles, _doubles2);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public CorrelatedTimeSeriesBuilder from(TimeSeries timeSeries) {
        if (timeSeries instanceof CorrelatedTimeSeries(String id1, long[] timestamps, double[] values, double[] values2)) {
            for (int x = 0; x < timestamps.length; x++) {
                add(timestamps[x], values[x], values2[x]);
                setId(id1);
            }
        }
        return this;
    }

    @Override
    public CorrelatedTimeSeriesBuilder merge(TimeSeries... timeSeriesArray) {
        for (var timeSeries: timeSeriesArray) {
            from(timeSeries);
        }
        return this;
    }
}
