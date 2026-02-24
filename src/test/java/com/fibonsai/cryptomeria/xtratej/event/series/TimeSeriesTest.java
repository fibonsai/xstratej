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

package com.fibonsai.cryptomeria.xtratej.event.series;

import com.fibonsai.cryptomeria.xtratej.event.series.impl.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("DataFlowIssue")
public class TimeSeriesTest {

    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    @Test
    public void testAddSingle() {
        SingleTimeSeries timeSeries = new SingleTimeSeries("");
        SingleTimeSeries.Single single = new SingleTimeSeries.Single(1000L, 1.0D);

        timeSeries.add(single);

        long[] expectedTimestamps = {1000L};
        double[] expectedValues = {1.0D};
        Assertions.assertArrayEquals(timeSeries.timestamps(), expectedTimestamps);
        Assertions.assertArrayEquals(timeSeries.values(), expectedValues);
    }

    @Test
    public void testAddDuo() {
        CorrelatedTimeSeries timeSeries = new CorrelatedTimeSeries("");
        CorrelatedTimeSeries.Correlated correlated = new CorrelatedTimeSeries.Correlated(1000L, 1.0D, 2.0D);

        timeSeries.add(correlated);

        long[] expectedTimestamps = {1000L};
        double[] expectedValues = {1.0D};
        double[] expectedcomparableValues = {2.0D};

        Assertions.assertArrayEquals(timeSeries.timestamps(), expectedTimestamps);
        Assertions.assertArrayEquals(timeSeries.values(), expectedValues);
        Assertions.assertArrayEquals(timeSeries.values2(), expectedcomparableValues);
    }

    @Test
    public void testAddWeightedByPeriod() {
        WeightedTimeSeries timeSeries = new WeightedTimeSeries("");
        WeightedTimeSeries.Weighted weighted = new WeightedTimeSeries.Weighted(1000L, 1.0D, 2.0D);

        timeSeries.add(weighted);

        long[] expectedTimestamps = {1000L};
        double[] expectedValues = {1.0D};
        double[] expectedperiods = {2.0D};

        Assertions.assertArrayEquals(timeSeries.timestamps(), expectedTimestamps);
        Assertions.assertArrayEquals(timeSeries.values(), expectedValues);
        Assertions.assertArrayEquals(timeSeries.weights(), expectedperiods);
    }

    @Test
    public void testAddBar() {
        BarTimeSeries timeSeries = new BarTimeSeries("");
        BarTimeSeries.Bar bar = new BarTimeSeries.Bar(1000L, 1.0D, 2.0D, 3.0D, 4.0D, 5.0D);

        timeSeries.add(bar);

        long[] expectedTimestamps = {1000L};
        double[] expectedOpens = {1.0D};
        double[] expectedHighs = {2.0D};
        double[] expectedLows = {3.0D};
        double[] expectedCloses = {4.0D};
        double[] expectedVolumes = {5.0D};

        Assertions.assertArrayEquals(timeSeries.timestamps(), expectedTimestamps);
        Assertions.assertArrayEquals(timeSeries.opens(), expectedOpens);
        Assertions.assertArrayEquals(timeSeries.highs(), expectedHighs);
        Assertions.assertArrayEquals(timeSeries.lows(), expectedLows);
        Assertions.assertArrayEquals(timeSeries.closes(), expectedCloses);
        Assertions.assertArrayEquals(timeSeries.volumes(), expectedVolumes);
    }

    @Test
    public void testAddBand() {
        BandTimeSeries timeSeries = new BandTimeSeries("");
        BandTimeSeries.Band band = new BandTimeSeries.Band(1000L, 3.0D, 2.0D, 1.0D);

        timeSeries.add(band);

        long[] expectedTimestamps = {1000L};
        double[] expectedUpper = {3.0D};
        double[] expectedMiddle = {2.0D};
        double[] expectedLower = {1.0D};

        Assertions.assertArrayEquals(timeSeries.timestamps(), expectedTimestamps);
        Assertions.assertArrayEquals(timeSeries.uppers(), expectedUpper);
        Assertions.assertArrayEquals(timeSeries.middles(), expectedMiddle);
        Assertions.assertArrayEquals(timeSeries.lowers(), expectedLower);
    }

    @Test
    public void testDefaultMaxSize() {
        SingleTimeSeries timeSeries = new SingleTimeSeries("");
        SingleTimeSeries.Single[] singles = {
                new SingleTimeSeries.Single(2000L, 1.0D),
                new SingleTimeSeries.Single(1000L, 1.0D)
        };

        timeSeries.add(singles[0]);
        timeSeries.add(singles[1]);

        Assertions.assertNotEquals(singles.length, timeSeries.timestamps().length);
    }

    @Test
    public void testSetMaxSize() {
        SingleTimeSeries timeSeries = new SingleTimeSeries("", 2);
        SingleTimeSeries.Single single1 = new SingleTimeSeries.Single(2000L, 1.0D);
        SingleTimeSeries.Single single2 = new SingleTimeSeries.Single(1000L, 1.0D);

        timeSeries.add(single1);
        timeSeries.add(single2);

        long[] expectedTimestamps = {1000L, 2000L};
        Assertions.assertArrayEquals(timeSeries.timestamps(), expectedTimestamps);
    }

    @Test
    public void testTimestampSorted() {
        SingleTimeSeries timeSeries = new SingleTimeSeries("", 10);
        SingleTimeSeries.Single[] singles = new SingleTimeSeries.Single[10];

        for (int x = 0; x < singles.length; x++) {
            singles[x] = new SingleTimeSeries.Single(random.nextLong(1, Long.MAX_VALUE - 1), 1.0D);
            timeSeries.add(singles[x]);
        }

        long[] expected = Stream.of(singles).map(SingleTimeSeries.Single::timestamp).mapToLong(l -> l).sorted().toArray();
        Assertions.assertArrayEquals(timeSeries.timestamps(), expected);
    }

    @Test
    public void testValuesLinkedWithSortedTimestamp() {
        SingleTimeSeries timeSeries = new SingleTimeSeries("", 10);
        SingleTimeSeries.Single[] singles = new SingleTimeSeries.Single[10];
        for (int x = 0; x < singles.length; x++) {
            long timestamp = random.nextLong(1, Long.MAX_VALUE - 1);
            double value = timestamp / 2.0D;
            singles[x] = new SingleTimeSeries.Single(timestamp, value);
            timeSeries.add(singles[x]);
        }

        double[] values = timeSeries.values();
        double[] expected = Stream.of(singles).map(SingleTimeSeries.Single::value).mapToDouble(d -> d).sorted().toArray();
        Assertions.assertArrayEquals(values, expected);
    }

    @Test
    public void testBooleanSingleTimeSeries() {
        BooleanSingleTimeSeries timeSeries = new BooleanSingleTimeSeries("");
        BooleanSingleTimeSeries.BooleanSingle single = new BooleanSingleTimeSeries.BooleanSingle(1000L, true);

        timeSeries.add(single);

        long[] expectedTimestamps = {1000L};
        boolean[] expectedValues = {true};

        Assertions.assertArrayEquals(timeSeries.timestamps(), expectedTimestamps);
        Assertions.assertArrayEquals(timeSeries.values(), expectedValues);
    }

    @Test
    public void testCorrelatedTimeSeriesMinMax() {
        CorrelatedTimeSeries timeSeries = new CorrelatedTimeSeries("", 5);
        timeSeries.add(new CorrelatedTimeSeries.Correlated(1000L, 5.0D, 10.0D));
        timeSeries.add(new CorrelatedTimeSeries.Correlated(2000L, 3.0D, 12.0D));
        timeSeries.add(new CorrelatedTimeSeries.Correlated(3000L, 7.0D, 8.0D));

        double[] minmax = timeSeries.minmax();
        Assertions.assertEquals(3.0D, minmax[0]);
        Assertions.assertEquals(12.0D, minmax[1]);
    }

    @Test
    public void testWeightedTimeSeriesMinMax() {
        WeightedTimeSeries timeSeries = new WeightedTimeSeries("", 5);
        timeSeries.add(new WeightedTimeSeries.Weighted(1000L, 5.0D, 1.0D));
        timeSeries.add(new WeightedTimeSeries.Weighted(2000L, 3.0D, 2.0D));
        timeSeries.add(new WeightedTimeSeries.Weighted(3000L, 7.0D, 1.5D));

        double[] minmax = timeSeries.minmax();
        Assertions.assertEquals(3.0D, minmax[0]);
        Assertions.assertEquals(7.0D, minmax[1]);
    }

    @Test
    public void testBandTimeSeriesMinMax() {
        BandTimeSeries timeSeries = new BandTimeSeries("", 5);
        timeSeries.add(new BandTimeSeries.Band(1000L, 10.0D, 7.0D, 4.0D));
        timeSeries.add(new BandTimeSeries.Band(2000L, 12.0D, 8.0D, 5.0D));
        timeSeries.add(new BandTimeSeries.Band(3000L, 9.0D, 6.0D, 3.0D));

        double[] minmax = timeSeries.minmax();
        Assertions.assertEquals(3.0D, minmax[0]);
        Assertions.assertEquals(12.0D, minmax[1]);
    }

    @Test
    public void testBarTimeSeriesMinMax() {
        BarTimeSeries timeSeries = new BarTimeSeries("", 5);
        timeSeries.add(new BarTimeSeries.Bar(1000L, 5.0D, 10.0D, 3.0D, 8.0D, 100.0D));
        timeSeries.add(new BarTimeSeries.Bar(2000L, 6.0D, 11.0D, 4.0D, 9.0D, 200.0D));
        timeSeries.add(new BarTimeSeries.Bar(3000L, 7.0D, 9.0D, 5.0D, 8.5D, 150.0D));

        double[] minmax = timeSeries.minmax();
        Assertions.assertEquals(3.0D, minmax[0]);
        Assertions.assertEquals(11.0D, minmax[1]);
    }

    @Test
    public void testEmptySingleTimeSeries() {
        SingleTimeSeries timeSeries = new SingleTimeSeries("");

        Assertions.assertEquals(0, timeSeries.size());
        assertThrows(RuntimeException.class, timeSeries::timestamps);
        assertThrows(RuntimeException.class, timeSeries::timestamp);
        assertThrows(RuntimeException.class, timeSeries::minmax);
        assertThrows(RuntimeException.class, timeSeries::values);
    }

    @Test
    public void testEmptyCorrelatedTimeSeries() {
        CorrelatedTimeSeries timeSeries = new CorrelatedTimeSeries("");

        Assertions.assertEquals(0, timeSeries.size());
        assertThrows(RuntimeException.class, timeSeries::timestamps);
        assertThrows(RuntimeException.class, timeSeries::timestamp);
        assertThrows(RuntimeException.class, timeSeries::minmax);
        assertThrows(RuntimeException.class, timeSeries::values);
        assertThrows(RuntimeException.class, timeSeries::values2);
    }

    @Test
    public void testEmptyWeightedTimeSeries() {
        WeightedTimeSeries timeSeries = new WeightedTimeSeries("");

        Assertions.assertEquals(0, timeSeries.size());
        assertThrows(RuntimeException.class, timeSeries::timestamps);
        assertThrows(RuntimeException.class, timeSeries::timestamp);
        assertThrows(RuntimeException.class, timeSeries::minmax);
        assertThrows(RuntimeException.class, timeSeries::values);
        assertThrows(RuntimeException.class, timeSeries::weights);
    }

    @Test
    public void testEmptyBandTimeSeries() {
        BandTimeSeries timeSeries = new BandTimeSeries("");

        Assertions.assertEquals(0, timeSeries.size());
        assertThrows(RuntimeException.class, timeSeries::timestamps);
        assertThrows(RuntimeException.class, timeSeries::timestamp);
        assertThrows(RuntimeException.class, timeSeries::minmax);
        assertThrows(RuntimeException.class, timeSeries::uppers);
        assertThrows(RuntimeException.class, timeSeries::middles);
        assertThrows(RuntimeException.class, timeSeries::lowers);
    }

    @Test
    public void testEmptyBarTimeSeries() {
        BarTimeSeries timeSeries = new BarTimeSeries("");

        Assertions.assertEquals(0, timeSeries.size());
        assertThrows(RuntimeException.class, timeSeries::timestamps);
        assertThrows(RuntimeException.class, timeSeries::timestamp);
        assertThrows(RuntimeException.class, timeSeries::minmax);
        assertThrows(RuntimeException.class, timeSeries::opens);
        assertThrows(RuntimeException.class, timeSeries::highs);
        assertThrows(RuntimeException.class, timeSeries::lows);
        assertThrows(RuntimeException.class, timeSeries::closes);
        assertThrows(RuntimeException.class, timeSeries::volumes);
    }

    @Test
    public void testTwoValuesMinmax() {
        double[] twoValues = {3.0D, 7.0D};
        double[] result = TimeSeries.minmax(twoValues);
        Assertions.assertEquals(3.0D, result[0]);
        Assertions.assertEquals(7.0D, result[1]);
    }

    @Test
    public void testNegativeValuesMinmax() {
        double[] values = {-5.0D, -3.0D, -10.0D};
        double[] result = TimeSeries.minmax(values);
        Assertions.assertEquals(-10.0D, result[0]);
        Assertions.assertEquals(-3.0D, result[1]);
    }

    @Test
    public void testMixedValuesMinmax() {
        double[] values = {-5.0D, 3.0D, -10.0D, 15.0D};
        double[] result = TimeSeries.minmax(values);
        Assertions.assertEquals(-10.0D, result[0]);
        Assertions.assertEquals(15.0D, result[1]);
    }

    @Test
    public void testSingleValueMinmax() {
        double[] singleValue = {5.0D};
        double[] result = TimeSeries.minmax(singleValue);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(5.0D, result[0]);
        Assertions.assertEquals(5.0D, result[1]);
    }

    @Test
    public void testEmptyArrayMinmax() {
        double[] empty = {};
        double[] result = TimeSeries.minmax(empty);
        Assertions.assertEquals(0, result.length);
    }

    @Test
    public void testNullArrayMinmax() {
        double[] result = TimeSeries.minmax(null);
        Assertions.assertEquals(0, result.length);
    }

    @Test
    public void testMinWithEmpty() {
        double result = TimeSeries.min(new double[0]);
        Assertions.assertTrue(Double.isNaN(result));
    }

    @Test
    public void testMaxWithEmpty() {
        double result = TimeSeries.max(new double[0]);
        Assertions.assertTrue(Double.isNaN(result));
    }

    @Test
    public void testMinWithValues() {
        double result = TimeSeries.min(new double[]{5.0D, 3.0D, 7.0D});
        Assertions.assertEquals(3.0D, result);
    }

    @Test
    public void testMaxWithValues() {
        double result = TimeSeries.max(new double[]{5.0D, 3.0D, 7.0D});
        Assertions.assertEquals(7.0D, result);
    }

    @Test
    public void testTimeSeriesId() {
        SingleTimeSeries timeSeries = new SingleTimeSeries("test-id");
        Assertions.assertEquals("test-id", timeSeries.id());
    }

    @Test
    public void testAddWithNullSingle() {
        SingleTimeSeries timeSeries = new SingleTimeSeries("");
        assertThrows(NullPointerException.class, () -> timeSeries.add(null));
    }

    @Test
    public void testAddWithNullCorrelated() {
        CorrelatedTimeSeries timeSeries = new CorrelatedTimeSeries("");
        assertThrows(NullPointerException.class, () -> timeSeries.add(null));
    }

    @Test
    public void testAddWithNullWeighted() {
        WeightedTimeSeries timeSeries = new WeightedTimeSeries("");
        assertThrows(NullPointerException.class, () -> timeSeries.add(null));
    }

    @Test
    public void testAddWithNullBand() {
        BandTimeSeries timeSeries = new BandTimeSeries("");
        assertThrows(NullPointerException.class, () -> timeSeries.add(null));
    }

    @Test
    public void testAddWithNullBar() {
        BarTimeSeries timeSeries = new BarTimeSeries("");
        assertThrows(NullPointerException.class, () -> timeSeries.add(null));
    }

    @Test
    public void testAddWithNullBooleanSingle() {
        BooleanSingleTimeSeries timeSeries = new BooleanSingleTimeSeries("");
        assertThrows(NullPointerException.class, () -> timeSeries.add(null));
    }

    @Test
    public void testTimeSeriesConcurrency() {
        SingleTimeSeries timeSeries = new SingleTimeSeries("", 100);
        
        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            final int threadNum = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    long timestamp = Thread.currentThread().threadId();
                    timeSeries.add(new SingleTimeSeries.Single(timestamp, threadNum * 10 + j));
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        Assertions.assertTrue(timeSeries.size() > 0);
        long[] ts = timeSeries.timestamps();
        for (int i = 1; i < ts.length; i++) {
            Assertions.assertTrue(ts[i - 1] <= ts[i]);
        }
    }

    @Test
    public void testLargeTimestamps() {
        SingleTimeSeries timeSeries = new SingleTimeSeries("", 5);
        timeSeries.add(new SingleTimeSeries.Single(Long.MAX_VALUE - 1000, 1.0D));
        timeSeries.add(new SingleTimeSeries.Single(Long.MAX_VALUE - 2000, 2.0D));
        timeSeries.add(new SingleTimeSeries.Single(Long.MAX_VALUE - 3000, 3.0D));

        Assertions.assertEquals(3, timeSeries.size());
        long[] timestamps = timeSeries.timestamps();
        Assertions.assertEquals(Long.MAX_VALUE - 3000, timestamps[0]);
        Assertions.assertEquals(Long.MAX_VALUE - 2000, timestamps[1]);
        Assertions.assertEquals(Long.MAX_VALUE - 1000, timestamps[2]);
    }

    @Test
    public void testSameTimestampsSorted() {
        SingleTimeSeries timeSeries = new SingleTimeSeries("", 5);
        timeSeries.add(new SingleTimeSeries.Single(1000L, 3.0D));
        timeSeries.add(new SingleTimeSeries.Single(1000L, 1.0D));
        timeSeries.add(new SingleTimeSeries.Single(1000L, 2.0D));

        Assertions.assertEquals(3, timeSeries.size());
        double[] values = timeSeries.values();
        Assertions.assertEquals(3.0D, values[0]);
        Assertions.assertEquals(1.0D, values[1]);
        Assertions.assertEquals(2.0D, values[2]);
    }

    @Test
    public void testOverwriteMaxSize() {
        SingleTimeSeries timeSeries = new SingleTimeSeries("", 3);
        timeSeries.add(new SingleTimeSeries.Single(1000L, 1.0D));
        timeSeries.add(new SingleTimeSeries.Single(2000L, 2.0D));
        timeSeries.add(new SingleTimeSeries.Single(3000L, 3.0D));
        timeSeries.add(new SingleTimeSeries.Single(4000L, 4.0D));

        Assertions.assertEquals(3, timeSeries.size());
        long[] timestamps = timeSeries.timestamps();
        Assertions.assertEquals(2000L, timestamps[0], "First timestamp should be 2000 (skipped 1000)");
        Assertions.assertEquals(3000L, timestamps[1], "Second timestamp should be 3000");
        Assertions.assertEquals(4000L, timestamps[2], "Third timestamp should be 4000 (appended)");
    }

    @Test
    public void testBooleanSingleTimeSeriesAdd() {
        BooleanSingleTimeSeries timeSeries = new BooleanSingleTimeSeries("", 5);
        timeSeries.add(new BooleanSingleTimeSeries.BooleanSingle(1000L, true));
        timeSeries.add(new BooleanSingleTimeSeries.BooleanSingle(2000L, false));
        timeSeries.add(new BooleanSingleTimeSeries.BooleanSingle(3000L, true));

        Assertions.assertEquals(3, timeSeries.size());
        boolean[] values = timeSeries.values();
        Assertions.assertTrue(values[0]);
        Assertions.assertFalse(values[1]);
        Assertions.assertTrue(values[2]);
    }

    @Test
    public void testBooleanSingleTimeSeriesEmpty() {
        BooleanSingleTimeSeries timeSeries = new BooleanSingleTimeSeries("");

        Assertions.assertEquals(0, timeSeries.size());
        assertThrows(RuntimeException.class, timeSeries::timestamps);
        assertThrows(RuntimeException.class, timeSeries::timestamp);
        assertThrows(RuntimeException.class, timeSeries::values);
    }
}
