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

public record EmptyTimeSeries() implements ITemporalData {

    public static final ITemporalData INSTANCE = new EmptyTimeSeries();

    @Override
    public String id() {
        return EmptyTimeSeries.class.getSimpleName();
    }

    @Override
    public long[] timestamps() {
        return new long[0];
    }

    @Override
    public long timestamp() {
        return 0;
    }

    @SuppressWarnings("ComparatorMethodParameterNotUsed")
    @Override
    public int compareTo(ITemporalData o) {
        return -1;
    }
}
