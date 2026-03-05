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

public class CorrelatedTimeSeries extends TimeSeries {

    private double[] values;
    private double[] values2;

    public record Correlated(long timestamp, double value, double value2) implements ITemporalData {}

    public CorrelatedTimeSeries(String id, Correlated[] correlateds) {
        super(id);
        long[] _timestamps = new long[correlateds.length];
        double[] _leftValues = new double[correlateds.length];
        double[] _rightValues = new double[correlateds.length];
        for (int x = 0; x < correlateds.length; x++) {
            _timestamps[x] = correlateds[x].timestamp();
            _leftValues[x] = correlateds[x].value();
            _rightValues[x] = correlateds[x].value2();
        }
        this.timestamps = _timestamps;
        this.values = _leftValues;
        this.values2 = _rightValues;
        this.maxSize = Math.max(timestamps.length, 1);
    }

    public CorrelatedTimeSeries(String id, Correlated[] correlateds, int maxSize) {
        this(id, correlateds);
        this.maxSize = Math.max(1, maxSize);
    }

    public CorrelatedTimeSeries(String id) {
        this(id, new Correlated[0]);
    }

    public CorrelatedTimeSeries(String id, int maxSize) {
        this(id, new Correlated[0], maxSize);
    }

    public TimeSeries add(Correlated correlated) {
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

            int idx = getIdx(correlated);
            updateTimestamps(correlated, newSize, idx, seriesStart);
            this.values = addToArray(correlated.value(), this.values, newSize, idx, seriesStart);
            this.values2 = addToArray(correlated.value2(), this.values2, newSize, idx, seriesStart);

        } finally {
            writeLock.unlock();
        }
        return this;
    }

    @Override
    public double[] minmax() {
        readLock.lock();
        try {
            final double[] minmaxValues = minmax(values);
            double minValue = minmaxValues[0];
            double maxValue = minmaxValues[1];
            double[] minmaxValues2 = minmax(values2);
            double minValue2 = minmaxValues2[0];
            double maxValue2 = minmaxValues2[1];
            return new double[]{ Math.min(minValue, minValue2), Math.max(maxValue, maxValue2) };
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

    public double[] values2() {
        readLock.lock();
        try {
            errorIfEmpty();
            return values2;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public double[] singleDoubleValues() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean[] singleBooleanValues() {
        return new boolean[0];
    }
}
