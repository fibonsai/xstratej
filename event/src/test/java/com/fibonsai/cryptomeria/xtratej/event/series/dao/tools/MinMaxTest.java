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
import com.fibonsai.cryptomeria.xtratej.event.series.dao.builders.SingleTimeSeriesBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;

public class MinMaxTest {

    private final ThreadLocalRandom random = ThreadLocalRandom.current();

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
    
    @Test
    public void testTwoValuesMinmax() {
        SingleTimeSeriesBuilder builder = new SingleTimeSeriesBuilder().setId("x");
        double[] values = {3.0D, 7.0D};
        for (var value: values) {
            builder.add(random.nextLong(), value);
        }
        SingleTimeSeries timeSeries = builder.build();
        var minMaxResult = MinMax.from(timeSeries);

        Assertions.assertEquals(3.0D, minMaxResult.min());
        Assertions.assertEquals(7.0D, minMaxResult.max());
    }

    @Test
    public void testNegativeValuesMinmax() {
        SingleTimeSeriesBuilder builder = new SingleTimeSeriesBuilder().setId("x");
        double[] values = {-3.0D, -7.0D};
        for (var value: values) {
            builder.add(random.nextLong(), value);
        }
        SingleTimeSeries timeSeries = builder.build();
        var minMaxResult = MinMax.from(timeSeries);

        Assertions.assertEquals(-7.0D, minMaxResult.min());
        Assertions.assertEquals(-3.0D, minMaxResult.max());
    }

    @Test
    public void testMixedValuesMinmax() {
        SingleTimeSeriesBuilder builder = new SingleTimeSeriesBuilder().setId("x");
        double[] values = {-5.0D, 3.0D, -10.0D, 15.0D};
        for (var value: values) {
            builder.add(random.nextLong(), value);
        }
        SingleTimeSeries timeSeries = builder.build();
        var minMaxResult = MinMax.from(timeSeries);

        Assertions.assertEquals(-10.0D, minMaxResult.min());
        Assertions.assertEquals(15.0D, minMaxResult.max());
    }

    @Test
    public void testSingleValueMinmax() {
        SingleTimeSeriesBuilder builder = new SingleTimeSeriesBuilder().setId("x");
        double[] values = {5.0D};
        for (var value: values) {
            builder.add(random.nextLong(), value);
        }
        SingleTimeSeries timeSeries = builder.build();
        var minMaxResult = MinMax.from(timeSeries);

        Assertions.assertEquals(5.0D, minMaxResult.min());
        Assertions.assertEquals(5.0D, minMaxResult.max());
    }

    @Test
    public void testEmptyArrayMinmax() {
        SingleTimeSeriesBuilder builder = new SingleTimeSeriesBuilder().setId("x");
        SingleTimeSeries timeSeries = builder.build();
        var minMaxResult = MinMax.from(timeSeries);

        Assertions.assertEquals(0, timeSeries.size());
        Assertions.assertTrue(Double.isNaN(minMaxResult.min()));
        Assertions.assertTrue(Double.isNaN(minMaxResult.max()));
    }
}
