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

package com.fibonsai.cryptomeria.xtratej.event.series.dao;

import org.jspecify.annotations.Nullable;

public record Double2TimeSeries(@Nullable String id, long[] timestamps, double[] values, double[] values2) implements ITemporalData {

    public Double2TimeSeries {
        if (timestamps.length > 1 && id == null) throw new RuntimeException("ID is mandatory if there is more than one value");
    }

    public Double2TimeSeries(long[] timestamps, double[] values, double[] values2) {
        this(null, timestamps, values, values2);
    }

    @Override
    public long timestamp() {
        return timestamps.length > 0 ? timestamps[timestamps.length - 1] : 0L;
    }
}
