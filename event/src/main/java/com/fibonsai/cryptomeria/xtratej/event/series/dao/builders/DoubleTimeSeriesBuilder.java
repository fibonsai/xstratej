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

import com.fibonsai.cryptomeria.xtratej.event.series.dao.DoubleTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.ITemporalData;

public class DoubleTimeSeriesBuilder extends TimeSeriesBuilder {

    private double[] values;

    public TimeSeriesBuilder add(long timestamp, double value) {
        writeLock.lock();
        try {
            this.values = addToArray(timestamp, value, this.values);

        } finally {
            writeLock.unlock();
        }
        return this;
    }

    @Override
    public DoubleTimeSeries build() {
        return new DoubleTimeSeries(id, timestamps, values);
    }

    @Override
    public TimeSeriesBuilder from(ITemporalData temporalData) {
        if (temporalData instanceof DoubleTimeSeries doubleTimeSeries) {
            this.timestamps = doubleTimeSeries.timestamps();
            this.values = doubleTimeSeries.values();
            this.maxSize = Math.max(timestamps.length, 1);
        }
        return this;
    }
}
