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

import com.fibonsai.cryptomeria.xtratej.event.series.TimeSeries;

public class EmptyTimeSeries extends TimeSeries {
    public static final TimeSeries INSTANCE = new EmptyTimeSeries();

    private EmptyTimeSeries() {
        super(EmptyTimeSeries.class.getSimpleName());
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public long[] timestamps() {
        return new long[0];
    }

    @Override
    public long timestamp() {
        throw new UnsupportedOperationException();
    }

    @Override
    public double[] minmax() {
        return new double[0];
    }

    @Override
    public double[] singleDoubleValues() {
        return new double[0];
    }

    @Override
    public boolean[] singleBooleanValues() {
        return new boolean[0];
    }
}
