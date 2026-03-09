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

import com.fibonsai.cryptomeria.xtratej.event.series.dao.ITemporalData;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.SingleTimeSeries;

import java.util.TreeMap;

public class SingleTimeSeriesBuilder extends TimeSeriesBuilder<SingleTimeSeriesBuilder> {

    private final TreeMap<Long, Double> doubles = new TreeMap<>();

    public SingleTimeSeriesBuilder add(long timestamp, double value) {
        writeLock.lock();
        try {
            while (doubles.size() >= maxSize) {
                doubles.remove(doubles.firstKey());
            }
            doubles.put(timestamp, value);
        } finally {
            writeLock.unlock();
        }

        return this;
    }

    @Override
    public SingleTimeSeries build() {
        readLock.lock();
        try {
            long[] _timestamps = doubles.sequencedKeySet().stream().mapToLong(Long::longValue).toArray();
            double[] _doubles = doubles.sequencedValues().stream().mapToDouble(Double::doubleValue).toArray();
            return new SingleTimeSeries(id, _timestamps, _doubles);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public SingleTimeSeriesBuilder from(ITemporalData temporalData) {
        if (temporalData instanceof SingleTimeSeries(String id1, long[] timestamps, double[] values)) {
            for (int x = 0; x < timestamps.length; x++) {
                add(timestamps[x], values[x]);
                setId(id1);
            }
        }
        return this;
    }
}
