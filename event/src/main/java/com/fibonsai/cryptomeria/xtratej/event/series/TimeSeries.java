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

package com.fibonsai.cryptomeria.xtratej.event.series;

import com.fibonsai.cryptomeria.xtratej.event.ITemporalData;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class TimeSeries implements ITemporalData {

    protected final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    protected final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    protected final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

    protected final String id;

    protected long[] timestamps = new long[0];

    protected int maxSize = 1;

    protected TimeSeries(String id) {
        this.id = id;
    }

    public static double[] minmax(double[] array) {
        if (array.length == 0) {
            return new double[0];
        }
        if (array.length == 1) {
            return new double[]{ array[0], array[0] };
        }
        if (array.length == 2) {
            return new double[]{ Math.min(array[0], array[1]), Math.max(array[0], array[1]) };
        }
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (double value : array) {
            if (Double.isNaN(value)) throw new IllegalArgumentException("array has NaN value.");
            if (value > max) max = value;
            if (value < min) min = value;
        }
        return new double[]{ min, max }; // idx: 0, 1
    }

    public static double min(double[] array) {
        double[] result = minmax(array);
        return result.length > 0 ? result[0] : Double.NaN;
    }

    public static double max(double[] array) {
        double[] result = minmax(array);
        return result.length > 0 ? result[1] : Double.NaN;
    }

    public double[] minmax() {
        throw new UnsupportedOperationException();
    }

    public String id() {
        return id;
    }

    public int size() {
        return timestamps.length;
    }

    public long[] timestamps() {
        readLock.lock();
        try {
            errorIfEmpty();
            return timestamps;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public long timestamp() {
        readLock.lock();
        try {
            errorIfEmpty();
            return timestamps[timestamps.length - 1];
        } finally {
            readLock.unlock();
        }
    }

    protected void updateTimestamps(ITemporalData temporalData, int newSize, int idx, int seriesStart) {
        long[] _timestamps = new long[newSize];
        if (idx == 0) {
            _timestamps[0] = temporalData.timestamp();
            System.arraycopy(timestamps, seriesStart, _timestamps, 1, timestamps.length - seriesStart);
        } else {
            System.arraycopy(timestamps, seriesStart, _timestamps, 0, idx - seriesStart);
            _timestamps[idx - seriesStart] = temporalData.timestamp();
            if (idx < timestamps.length) {
                System.arraycopy(timestamps, seriesStart + idx, _timestamps, idx + 1, timestamps.length - seriesStart - idx);
            }
        }
        this.timestamps = _timestamps;
    }

    protected int getIdx(ITemporalData temporalData) {
        readLock.lock();
        try {
            int idx;
            for (idx = 0; idx < timestamps.length; idx++) {
                if (timestamps[idx] > temporalData.timestamp()) {
                    break;
                }
            }
            return idx;
        } finally {
            readLock.unlock();
        }
    }

    protected static boolean[] addToArray(boolean aBoolean, boolean[] currentArray, int newSize, int idx, int seriesStart) {
        boolean[] arrayOfBooleans = new boolean[newSize];
        if (idx == 0) {
            arrayOfBooleans[0] = aBoolean;
            System.arraycopy(currentArray, seriesStart, arrayOfBooleans, 1, currentArray.length - seriesStart);
        } else {
            System.arraycopy(currentArray, seriesStart, arrayOfBooleans, 0, idx);
            arrayOfBooleans[idx] = aBoolean;
            if (idx < currentArray.length) {
                System.arraycopy(currentArray, seriesStart + idx, arrayOfBooleans, idx + 1, currentArray.length - seriesStart - idx);
            }
        }
        return arrayOfBooleans;
    }

    protected static double[] addToArray(double aDouble, double[] currentArray, int newSize, int idx, int seriesStart) {
        double[] arrayOfDoubles = new double[newSize];
        if (idx == 0) {
            arrayOfDoubles[0] = aDouble;
            System.arraycopy(currentArray, seriesStart, arrayOfDoubles, 1, currentArray.length - seriesStart);
        } else {
            System.arraycopy(currentArray, seriesStart, arrayOfDoubles, 0, idx - seriesStart);
            arrayOfDoubles[idx - seriesStart] = aDouble;
            if (idx < currentArray.length) {
                System.arraycopy(currentArray, seriesStart + idx, arrayOfDoubles, idx + 1, currentArray.length - seriesStart - idx);
            }
        }
        return arrayOfDoubles;
    }

    protected static int[] addToArray(int aInt, int[] currentArray, int newSize, int idx, int seriesStart) {
        int[] arrayOfInts = new int[newSize];
        if (idx == 0) {
            arrayOfInts[0] = aInt;
            System.arraycopy(currentArray, seriesStart, arrayOfInts, 1, currentArray.length - seriesStart);
        } else {
            System.arraycopy(currentArray, seriesStart, arrayOfInts, 0, idx - seriesStart);
            arrayOfInts[idx - seriesStart] = aInt;
            if (idx < currentArray.length) {
                System.arraycopy(currentArray, seriesStart + idx, arrayOfInts, idx + 1, currentArray.length - seriesStart - idx);
            }
        }
        return arrayOfInts;
    }

    protected static String[] addToArray(String aString, String[] currentArray, int newSize, int idx, int seriesStart) {
        String[] arrayOfStrings = new String[newSize];
        if (idx == 0) {
            arrayOfStrings[0] = aString;
            System.arraycopy(currentArray, seriesStart, arrayOfStrings, 1, currentArray.length - seriesStart);
        } else {
            System.arraycopy(currentArray, seriesStart, arrayOfStrings, 0, idx - seriesStart);
            arrayOfStrings[idx - seriesStart] = aString;
            if (idx < currentArray.length) {
                System.arraycopy(currentArray, seriesStart + idx, arrayOfStrings, idx + 1, currentArray.length - seriesStart - idx);
            }
        }
        return arrayOfStrings;
    }

    protected void errorIfEmpty() {
        if (timestamps.length == 0) throw new RuntimeException("timeseries is empty");
    }

    public abstract double[] singleDoubleValues();
    public abstract boolean[] singleBooleanValues();
}
