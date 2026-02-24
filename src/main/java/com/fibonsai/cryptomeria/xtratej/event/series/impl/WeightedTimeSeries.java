/*
 *  Copyright (c) 2025 fibonsai.com
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
import jakarta.annotation.Nonnull;

public class WeightedTimeSeries extends TimeSeries {

    private double[] values;
    private double[] weights;

    public record Weighted(long timestamp, double value, double weight) implements ITemporalData {}

    public WeightedTimeSeries(String id, Weighted[] weighteds) {
        super(id);
        if (weighteds == null) throw new NullPointerException();
        long[] _timestamps = new long[weighteds.length];
        double[] _values = new double[weighteds.length];
        double[] _weights = new double[weighteds.length];
        for (int x = 0; x < weighteds.length; x++) {
            _timestamps[x] = weighteds[x].timestamp();
            _values[x] = weighteds[x].value();
            _weights[x] = weighteds[x].weight();
        }
        this.timestamps = _timestamps;
        this.values = _values;
        this.weights = _weights;
        this.maxSize = Math.max(timestamps.length, 1);
    }

    public WeightedTimeSeries(String id, Weighted[] weighteds, int maxSize) {
        this(id, weighteds);
        this.maxSize = Math.max(1, maxSize);
    }

    public WeightedTimeSeries(String id) {
        this(id, new Weighted[0]);
    }

    public WeightedTimeSeries(String id, int maxSize) {
        this(id, new Weighted[0], maxSize);
    }

    public TimeSeries add(@Nonnull Weighted weighted) {
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

            int idx = getIdx(weighted);
            updateTimestamps(weighted, newSize, idx, seriesStart);
            this.values = addToArray(weighted.value(), this.values, newSize, idx, seriesStart);
            this.weights = addToArray(weighted.weight(), this.weights, newSize, idx, seriesStart);

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

    public double[] weights() {
        readLock.lock();
        try {
            errorIfEmpty();
            return weights;
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
