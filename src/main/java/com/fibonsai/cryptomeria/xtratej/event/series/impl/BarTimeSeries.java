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

public class BarTimeSeries extends TimeSeries {

    private double[] opens;
    private double[] highs;
    private double[] lows;
    private double[] closes;
    private double[] volumes;

    public record Bar(long timestamp, double open, double high, double low, double close, double volume) implements ITemporalData {}

    public BarTimeSeries(String id, Bar[] bars) {
        super(id);
        if (bars == null) throw new NullPointerException();
        long[] _timestamps = new long[bars.length];
        double[] _opens = new double[bars.length];
        double[] _highs = new double[bars.length];
        double[] _lows = new double[bars.length];
        double[] _closes = new double[bars.length];
        double[] _volumes = new double[bars.length];
        for (int x = 0; x < bars.length; x++) {
            _timestamps[x] = bars[x].timestamp();
            _opens[x] = bars[x].open();
            _highs[x] = bars[x].high();
            _lows[x] = bars[x].low();
            _closes[x] = bars[x].close();
            _volumes[x] = bars[x].volume();
        }
        this.timestamps = _timestamps;
        this.opens = _opens;
        this.highs = _highs;
        this.lows = _lows;
        this.closes = _closes;
        this.volumes = _volumes;
        this.maxSize = Math.max(timestamps.length, 1);
    }

    public BarTimeSeries(String id, Bar[] bars, int maxSize) {
        this(id, bars);
        this.maxSize = Math.max(1, maxSize);
    }

    public BarTimeSeries(String id) {
        this(id, new Bar[0]);
    }

    public BarTimeSeries(String id, int maxSize) {
        this(id, new Bar[0], maxSize);
    }

    public TimeSeries add(@Nonnull Bar bar) {
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

            int idx = getIdx(bar);
            updateTimestamps(bar, newSize, idx, seriesStart);
            this.opens = addToArray(bar.open(), this.opens, newSize, idx, seriesStart);
            this.highs = addToArray(bar.high(), this.highs, newSize, idx, seriesStart);
            this.lows = addToArray(bar.low(), this.lows, newSize, idx, seriesStart);
            this.closes = addToArray(bar.close(), this.closes, newSize, idx, seriesStart);
            this.volumes = addToArray(bar.volume(), this.volumes, newSize, idx, seriesStart);

        } finally {
            writeLock.unlock();
        }
        return this;
    }

    @Override
    public double[] minmax() {
        readLock.lock();
        try {
            double[] minmaxHigh = minmax(highs());
            double maxHigh = minmaxHigh[1];
            double[] minmaxLow = minmax(lows());
            double minLow = minmaxLow[0];
            return new double[]{ minLow, maxHigh };
        } finally {
            readLock.unlock();
        }
    }

    public double[] opens() {
        readLock.lock();
        try {
            errorIfEmpty();
            return opens;
        } finally {
            readLock.unlock();
        }
    }

    public double[] highs() {
        readLock.lock();
        try {
            errorIfEmpty();
            return highs;
        } finally {
            readLock.unlock();
        }
    }

    public double[] lows() {
        readLock.lock();
        try {
            errorIfEmpty();
            return lows;
        } finally {
            readLock.unlock();
        }
    }

    public double[] closes() {
        readLock.lock();
        try {
            errorIfEmpty();
            return closes;
        } finally {
            readLock.unlock();
        }
    }

    public double[] volumes() {
        readLock.lock();
        try {
            errorIfEmpty();
            return volumes;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public double[] singleDoubleValues() {
        return closes();
    }

    @Override
    public boolean[] singleBooleanValues() {
        return new boolean[0];
    }
}
