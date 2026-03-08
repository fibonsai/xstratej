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
import org.jspecify.annotations.Nullable;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class TimeSeriesBuilder<T> {

    protected final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    protected final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    protected final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

    protected @Nullable String id = null;
    protected long[] timestamps = new long[0];

    protected int maxSize = 1;

    public TimeSeriesBuilder<T> setMaxSize(int maxSize) {
        this.maxSize = maxSize;
        return this;
    }

    public TimeSeriesBuilder<T> setId(String id) {
        this.id = id;
        return this;
    }

    private record UpdateTimestampsResult(int idx, int newSize, int seriesStart) {};

    private UpdateTimestampsResult updateTimestamps(long timestamp) {
        int newSize;
        int seriesStart;
        if (timestamps.length == maxSize) {
            newSize = maxSize;
            seriesStart = 1;
        } else {
            newSize = timestamps.length + 1;
            seriesStart = 0;
        }
        int idx = getIdx(timestamp);
        long[] _timestamps = new long[newSize];
        if (idx == 0) {
            _timestamps[0] = timestamp;
            System.arraycopy(timestamps, seriesStart, _timestamps, 1, timestamps.length - seriesStart);
        } else {
            System.arraycopy(timestamps, seriesStart, _timestamps, 0, idx - seriesStart);
            _timestamps[idx - seriesStart] = timestamp;
            if (idx < timestamps.length) {
                System.arraycopy(timestamps, seriesStart + idx, _timestamps, idx + 1, timestamps.length - seriesStart - idx);
            }
        }
        this.timestamps = _timestamps;
        return new UpdateTimestampsResult(idx, newSize, seriesStart);
    }

    private int getIdx(long timestamp) {
        readLock.lock();
        try {
            int idx;
            for (idx = 0; idx < timestamps.length; idx++) {
                if (timestamps[idx] > timestamp) {
                    break;
                }
            }
            return idx;
        } finally {
            readLock.unlock();
        }
    }

    protected boolean[] addToArray(long timestamp, boolean aBoolean, boolean[] currentArray) {
        //noinspection ConstantValue
        if (updateTimestamps(timestamp) instanceof UpdateTimestampsResult(int idx, int newSize, int seriesStart)) {
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
        return currentArray;
    }

    protected double[] addToArray(long timestamp, double aDouble, double[] currentArray) {
        //noinspection ConstantValue
        if (updateTimestamps(timestamp) instanceof UpdateTimestampsResult(int idx, int newSize, int seriesStart)) {
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
        return currentArray;
    }

    protected int[] addToArray(long timestamp, int aInt, int[] currentArray) {
        //noinspection ConstantValue
        if (updateTimestamps(timestamp) instanceof UpdateTimestampsResult(int idx, int newSize, int seriesStart)) {
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
        return currentArray;
    }

    protected String[] addToArray(long timestamp, String aString, String[] currentArray) {
        //noinspection ConstantValue
        if (updateTimestamps(timestamp) instanceof UpdateTimestampsResult(int idx, int newSize, int seriesStart)) {
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
        return currentArray;
    }

    public abstract ITemporalData build();
    public abstract TimeSeriesBuilder<T> from(ITemporalData temporalData);
}
