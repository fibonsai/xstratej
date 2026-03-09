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
import com.fibonsai.cryptomeria.xtratej.event.series.dao.ITemporalData;

import java.util.TreeMap;

public class BandTimeSeriesBuilder extends TimeSeriesBuilder<BandTimeSeriesBuilder> {

    private final TreeMap<Long, Double> uppers = new TreeMap<>();
    private final TreeMap<Long, Double> middles = new TreeMap<>();
    private final TreeMap<Long, Double> lowers = new TreeMap<>();

    public BandTimeSeriesBuilder add(long timestamp, double upper, double middle, double lower) {
        writeLock.lock();
        try {
            while (uppers.size() >= maxSize) {
                uppers.remove(uppers.firstKey());
            }
            while (middles.size() >= maxSize) {
                middles.remove(middles.firstKey());
            }
            while (lowers.size() >= maxSize) {
                lowers.remove(lowers.firstKey());
            }
            uppers.put(timestamp, upper);
            middles.put(timestamp, middle);
            lowers.put(timestamp, lower);
        } finally {
            writeLock.unlock();
        }

        return this;
    }

    @Override
    public BandTimeSeries build() {
        readLock.lock();
        try {
            long[] _timestamps = uppers.sequencedKeySet().stream().mapToLong(Long::longValue).toArray();
            double[] _uppers = uppers.sequencedValues().stream().mapToDouble(Double::doubleValue).toArray();
            double[] _middles = middles.sequencedValues().stream().mapToDouble(Double::doubleValue).toArray();
            double[] _lowers = lowers.sequencedValues().stream().mapToDouble(Double::doubleValue).toArray();
            return new BandTimeSeries(id, _timestamps, _uppers, _middles, _lowers);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public BandTimeSeriesBuilder from(ITemporalData temporalData) {
        if (temporalData instanceof BandTimeSeries(String id1, long[] timestamps, double[] u, double[] m, double[] l)) {
            for (int x = 0; x < timestamps.length; x++) {
                add(timestamps[x], u[x], m[x], l[x]);
                setId(id1);
            }
        }
        return this;
    }
}
