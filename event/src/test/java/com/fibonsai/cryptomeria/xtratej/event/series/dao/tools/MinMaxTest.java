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

package com.fibonsai.cryptomeria.xtratej.event.series.dao.tools;

import com.fibonsai.cryptomeria.xtratej.event.series.dao.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MinMaxTest {

    @Test
    public void emptyTimeSeriesTest() {
        var minMaxResult = MinMax.from(EmptyTimeSeries.INSTANCE);

        Assertions.assertTrue(Double.isNaN(minMaxResult.min()));
        Assertions.assertTrue(Double.isNaN(minMaxResult.max()));
    }

    @Test
    public void booleanTimeSeriesTest() {
        var minMaxResult = MinMax.from(new BooleanTimeSeries(new long[]{1000L}, new boolean[]{true}));

        Assertions.assertTrue(Double.isNaN(minMaxResult.min()));
        Assertions.assertTrue(Double.isNaN(minMaxResult.max()));
    }

    @Test
    public void singleTimeSeriesTest() {
        var minMaxResult = MinMax.from(new SingleTimeSeries("x", new long[]{1000L, 2000L, 3000L}, new double[]{1.0D, 2.0D, 3.0D}));

        Assertions.assertEquals(1.0D, minMaxResult.min());
        Assertions.assertEquals(3.0D, minMaxResult.max());
    }

    @Test
    public void singleTimeSeriesAllPositiveInfTest() {
        var minMaxResult = MinMax.from(new SingleTimeSeries("x", new long[]{1000L, 2000L, 3000L}, new double[]{Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY}));

        Assertions.assertTrue(Double.isNaN(minMaxResult.min()));
        Assertions.assertTrue(Double.isNaN(minMaxResult.max()));
    }

    @Test
    public void singleTimeSeriesAllNegativeInfTest() {
        var minMaxResult = MinMax.from(new SingleTimeSeries("x", new long[]{1000L, 2000L, 3000L}, new double[]{Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY}));

        Assertions.assertTrue(Double.isNaN(minMaxResult.min()));
        Assertions.assertTrue(Double.isNaN(minMaxResult.max()));
    }

    @Test
    public void singleTimeSeriesAllNaNTest() {
        var minMaxResult = MinMax.from(new SingleTimeSeries("x", new long[]{1000L, 2000L, 3000L}, new double[]{Double.NaN, Double.NaN, Double.NaN}));

        Assertions.assertTrue(Double.isNaN(minMaxResult.min()));
        Assertions.assertTrue(Double.isNaN(minMaxResult.max()));
    }

    @Test
    public void correlatedTimeSeriesTest() {
        var minMaxResult = MinMax.from(new CorrelatedTimeSeries("x", new long[]{1000L, 2000L, 3000L}, new double[]{1.0D, 2.0D, 3.0D}, new double[]{4.0D, 5.0D, 6.0D}));

        Assertions.assertEquals(1.0D, minMaxResult.min());
        Assertions.assertEquals(6.0D, minMaxResult.max());
    }

    @Test
    public void barTimeSeriesTest() {
        long[] timestamps = {1000L, 2000L, 3000L};
        double[] opens = {1.0D, 2.0D, 3.0D};
        double[] highs = {4.0D, 5.0D, 6.0D};
        double[] lows = {7.0D, 8.0D, 9.0D};
        double[] closes = {10.0D, 11.0D, 12.0D};
        double[] volumes = {13.0D, 14.0D, 15.0D};
        var minMaxResult = MinMax.from(new BarTimeSeries("x", timestamps, opens, highs, lows, closes, volumes));

        Assertions.assertEquals(10.0D, minMaxResult.min());
        Assertions.assertEquals(12.0D, minMaxResult.max());
    }

    @Test
    public void bandTimeSeriesTest() {
        long[] timestamps = {1000L, 2000L, 3000L};
        double[] uppers = {7.0D, 8.0D, 9.0D};
        double[] middles = {4.0D, 5.0D, 6.0D};
        double[] lowers = {1.0D, 2.0D, 3.0D};
        var minMaxResult = MinMax.from(new BandTimeSeries("x", timestamps, uppers, middles, lowers));

        Assertions.assertEquals(1.0D, minMaxResult.min());
        Assertions.assertEquals(9.0D, minMaxResult.max());
    }
}
