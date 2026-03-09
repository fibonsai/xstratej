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

import com.fibonsai.cryptomeria.xtratej.event.series.dao.BarTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.ITemporalData;

import java.util.TreeMap;

public class BarTimeSeriesBuilder extends TimeSeriesBuilder<BarTimeSeriesBuilder> {

    private final TreeMap<Long, Double> opens = new TreeMap<>();
    private final TreeMap<Long, Double> highs = new TreeMap<>();
    private final TreeMap<Long, Double> lows = new TreeMap<>();
    private final TreeMap<Long, Double> closes = new TreeMap<>();
    private final TreeMap<Long, Double> volumes = new TreeMap<>();

    public BarTimeSeriesBuilder add(long timestamp, double open, double high, double low, double close, double volume) {
        writeLock.lock();
        try {
            while (opens.size() >= maxSize) {
                opens.remove(opens.firstKey());
            }
            while (highs.size() >= maxSize) {
                highs.remove(highs.firstKey());
            }
            while (lows.size() >= maxSize) {
                lows.remove(lows.firstKey());
            }
            while (closes.size() >= maxSize) {
                closes.remove(closes.firstKey());
            }
            while (volumes.size() >= maxSize) {
                volumes.remove(volumes.firstKey());
            }
            opens.put(timestamp, open);
            highs.put(timestamp, high);
            lows.put(timestamp, low);
            closes.put(timestamp, close);
            volumes.put(timestamp, volume);
        } finally {
            writeLock.unlock();
        }

        return this;
    }

    @Override
    public BarTimeSeries build() {
        readLock.lock();
        try {
            long[] _timestamps = opens.sequencedKeySet().stream().mapToLong(Long::longValue).toArray();
            double[] _opens = opens.sequencedValues().stream().mapToDouble(Double::doubleValue).toArray();
            double[] _highs = highs.sequencedValues().stream().mapToDouble(Double::doubleValue).toArray();
            double[] _lows = lows.sequencedValues().stream().mapToDouble(Double::doubleValue).toArray();
            double[] _closes = closes.sequencedValues().stream().mapToDouble(Double::doubleValue).toArray();
            double[] _volumes = volumes.sequencedValues().stream().mapToDouble(Double::doubleValue).toArray();
            return new BarTimeSeries(id, _timestamps, _opens, _highs, _lows, _closes, _volumes);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public BarTimeSeriesBuilder from(ITemporalData temporalData) {
        if (temporalData instanceof BarTimeSeries(String id1, long[] timestamps, double[] o, double[] h, double[] l, double[] c, double[] v)) {
            for (int x = 0; x < timestamps.length; x++) {
                add(timestamps[x], o[x], h[x], l[x], c[x], v[x]);
                setId(id1);
            }
        }
        return this;
    }
}
