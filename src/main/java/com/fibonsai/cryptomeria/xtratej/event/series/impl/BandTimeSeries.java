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

public final class BandTimeSeries extends TimeSeries {

    private double[] uppers;
    private double[] middles;
    private double[] lowers;

    public record Band(long timestamp, double upper, double middle, double lower) implements ITemporalData {}

    public BandTimeSeries(String id, Band[] bands) {
        super(id);
        if (bands == null) throw new NullPointerException();
        long[] _timestamps = new long[bands.length];
        double[] _uppers = new double[bands.length];
        double[] _middles = new double[bands.length];
        double[] _lowers = new double[bands.length];
        for (int x = 0; x < bands.length; x++) {
            _timestamps[x] = bands[x].timestamp();
            _uppers[x] = bands[x].upper();
            _middles[x] = bands[x].middle();
            _lowers[x] = bands[x].lower();
        }
        this.timestamps = _timestamps;
        this.uppers = _uppers;
        this.middles = _middles;
        this.lowers = _lowers;
        this.maxSize = Math.max(timestamps.length, 1);
    }

    public BandTimeSeries(String id, Band[] bands, int maxSize) {
        this(id, bands);
        this.maxSize = Math.max(maxSize, 1);
    }

    public BandTimeSeries(String id) {
        this(id, new Band[0]);
    }

    public BandTimeSeries(String id, int maxSize) {
        this(id, new Band[0], maxSize);
    }

    public TimeSeries add(@Nonnull Band band) {
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

            int idx = getIdx(band);
            updateTimestamps(band, newSize, idx, seriesStart);
            this.uppers = addToArray(band.upper(), this.uppers, newSize, idx, seriesStart);
            this.middles = addToArray(band.middle(), this.middles, newSize, idx, seriesStart);
            this.lowers = addToArray(band.lower(), this.lowers, newSize, idx, seriesStart);

        } finally {
            writeLock.unlock();
        }
        return this;
    }

    @Override
    public double[] minmax() {
        readLock.lock();
        try {
            double[] minmaxHigh = minmax(uppers);
            double maxHigh = minmaxHigh[1];
            double[] minmaxLow = minmax(lowers);
            double minLow = minmaxLow[0];
            return new double[]{ minLow, maxHigh };
        } finally {
            readLock.unlock();
        }
    }

    public double[] uppers() {
        readLock.lock();
        try {
            errorIfEmpty();
            return uppers;
        } finally {
            readLock.unlock();
        }
    }

    public double[] middles() {
        readLock.lock();
        try {
            errorIfEmpty();
            return middles;
        } finally {
            readLock.unlock();
        }
    }

    public double[] lowers() {
        readLock.lock();
        try {
            errorIfEmpty();
            return lowers;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public double[] singleDoubleValues() {
        return middles();
    }

    @Override
    public boolean[] singleBooleanValues() {
        return new boolean[0];
    }
}
