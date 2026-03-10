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

import com.fibonsai.cryptomeria.xtratej.event.series.dao.TimeSeries;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class TimeSeriesBuilder<T extends TimeSeriesBuilder<?>> {

    protected final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    protected final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    protected final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

    protected @Nullable String id = null;

    protected int maxSize = Integer.MAX_VALUE;

    @SuppressWarnings("unchecked")
    public T setMaxSize(int maxSize) {
        writeLock.lock();
        try {
            this.maxSize = maxSize;
        } finally {
            writeLock.unlock();
        }
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setId(@Nullable String id) {
        writeLock.lock();
        try {
            this.id = id;
        } finally {
            writeLock.unlock();
        }
        return (T) this;
    }

    public abstract TimeSeries build();
    public abstract TimeSeriesBuilder<T> from(TimeSeries timeSeries);
    public abstract TimeSeriesBuilder<T> merge(TimeSeries... timeSeriesArray);
}
