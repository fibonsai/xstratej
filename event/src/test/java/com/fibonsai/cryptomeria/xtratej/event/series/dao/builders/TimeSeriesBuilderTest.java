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

import com.fibonsai.cryptomeria.xtratej.event.series.dao.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

public class TimeSeriesBuilderTest {

    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    @Test
    public void testAddSingle() {
        SingleTimeSeriesBuilder builder = new SingleTimeSeriesBuilder();
        SingleTimeSeries timeSeries = builder.add(1000L, 1.0D).setId("x").build();

        long[] expectedTimestamps = {1000L};
        double[] expectedValues = {1.0D};
        String expectedId = "x";

        assertArrayEquals(timeSeries.timestamps(), expectedTimestamps);
        assertArrayEquals(timeSeries.values(), expectedValues);
        assertEquals(expectedId, timeSeries.id());
    }

    @Test
    public void testAddCorrelated() {
        CorrelatedTimeSeriesBuilder builder = new CorrelatedTimeSeriesBuilder();
        CorrelatedTimeSeries timeSeries = builder.add(1000L, 1.0D, 2.0D).setId("x").build();

        long[] expectedTimestamps = {1000L};
        double[] expectedValues = {1.0D};
        double[] expectedcomparableValues = {2.0D};
        String expectedId = "x";

        assertArrayEquals(timeSeries.timestamps(), expectedTimestamps);
        assertArrayEquals(timeSeries.values(), expectedValues);
        assertArrayEquals(timeSeries.values2(), expectedcomparableValues);
        assertEquals(expectedId, timeSeries.id());
    }

    @Test
    public void testAddBar() {
        BarTimeSeriesBuilder builder = new BarTimeSeriesBuilder();
        BarTimeSeries timeSeries = builder.add(1000L, 1.0D, 2.0D, 3.0D, 4.0D, 5.0D).setId("x").build();

        long[] expectedTimestamps = {1000L};
        double[] expectedOpens = {1.0D};
        double[] expectedHighs = {2.0D};
        double[] expectedLows = {3.0D};
        double[] expectedCloses = {4.0D};
        double[] expectedVolumes = {5.0D};
        String expectedId = "x";

        assertArrayEquals(timeSeries.timestamps(), expectedTimestamps);
        assertArrayEquals(timeSeries.opens(), expectedOpens);
        assertArrayEquals(timeSeries.highs(), expectedHighs);
        assertArrayEquals(timeSeries.lows(), expectedLows);
        assertArrayEquals(timeSeries.closes(), expectedCloses);
        assertArrayEquals(timeSeries.volumes(), expectedVolumes);
        assertEquals(expectedId, timeSeries.id());
    }

    @Test
    public void testAddBand() {
        BandTimeSeriesBuilder builder = new BandTimeSeriesBuilder();
        BandTimeSeries timeSeries = builder.add(1000L, 1.0D, 2.0D, 3.0D).setId("x").build();

        long[] expectedTimestamps = {1000L};
        double[] expectedUpper = {1.0D};
        double[] expectedMiddle = {2.0D};
        double[] expectedLower = {3.0D};
        String expectedId = "x";

        assertArrayEquals(timeSeries.timestamps(), expectedTimestamps);
        assertArrayEquals(timeSeries.uppers(), expectedUpper);
        assertArrayEquals(timeSeries.middles(), expectedMiddle);
        assertArrayEquals(timeSeries.lowers(), expectedLower);
        assertEquals(expectedId, timeSeries.id());
    }

    @Test
    public void testAddBooleanTimeSeries() {
        BooleanTimeSeriesBuilder builder = new BooleanTimeSeriesBuilder();
        BooleanTimeSeries timeSeries = builder.add(1000L, true).setId("x").build();

        long[] expectedTimestamps = {1000L};
        boolean[] expectedValues = {true};
        String expectedId = "x";

        assertArrayEquals(timeSeries.timestamps(), expectedTimestamps);
        assertArrayEquals(timeSeries.values(), expectedValues);
        assertEquals(expectedId, timeSeries.id());
    }

    @Test
    public void testAddCorrelatedTimeSeries() {
        CorrelatedTimeSeriesBuilder builder = new CorrelatedTimeSeriesBuilder();
        CorrelatedTimeSeries timeSeries = builder.add(1000L, 1.0D, 2.0D).setId("x").build();

        long[] expectedTimestamps = {1000L};
        double[] expectedValues = {1.0D};
        double[] expectedValues2 = {2.0D};
        String expectedId = "x";

        assertArrayEquals(timeSeries.timestamps(), expectedTimestamps);
        assertArrayEquals(timeSeries.values(), expectedValues);
        assertArrayEquals(timeSeries.values2(), expectedValues2);
        assertEquals(expectedId, timeSeries.id());
    }

    @Test
    public void testMaxSizeOne() {
        SingleTimeSeriesBuilder builder = new SingleTimeSeriesBuilder().setId("x").setMaxSize(1);
        SingleTimeSeries timeSeries = builder
                .add(1000L, 1.0D)
                .add(2000L, 1.0D)
                .build();

        assertNotEquals(2, timeSeries.size());
    }

    @Test
    public void testSetMaxSize() {
        SingleTimeSeriesBuilder builder = new SingleTimeSeriesBuilder().setId("x");
        SingleTimeSeries timeSeries = builder
                .setMaxSize(2)
                .add(1000L, 1.0D)
                .add(2000L, 1.0D)
                .build();

        assertEquals(2, timeSeries.size());

        long[] expectedTimestamps = {1000L, 2000L};
        assertArrayEquals(timeSeries.timestamps(), expectedTimestamps);
    }

    @Test
    public void testTimestampSorted() {
        SingleTimeSeriesBuilder builder = new SingleTimeSeriesBuilder().setMaxSize(10).setId("x");

        ArrayList<Long> timestamps = new ArrayList<>();
        for (int x = 0; x < 10; x++) {
            long timestamp = random.nextLong(1, Long.MAX_VALUE - 1);
            builder.add(timestamp, 1.0D);
            timestamps.add(timestamp);
        }
        SingleTimeSeries timeSeries = builder.build();

        long[] expected = timestamps.stream().sorted().mapToLong(Long::longValue).toArray();
        assertArrayEquals(timeSeries.timestamps(), expected);
    }

    @Test
    public void testValuesLinkedWithSortedTimestamp() {
        SingleTimeSeriesBuilder builder = new SingleTimeSeriesBuilder().setMaxSize(10).setId("x");

        ArrayList<Long> timestamps = new ArrayList<>();
        for (int x = 0; x < 10; x++) {
            long timestamp = random.nextLong(1, Long.MAX_VALUE - 1);
            builder.add(timestamp, timestamp / 2.0);
            timestamps.add(timestamp);
        }
        SingleTimeSeries timeSeries = builder.build();

        double[] expected = timestamps.stream().sorted().mapToDouble(l -> l / 2.0D).toArray();
        double[] values = timeSeries.values();
        assertArrayEquals(values, expected);
    }

    @Test
    public void testEmptySingleTimeSeries() {
        SingleTimeSeries timeSeries = new SingleTimeSeriesBuilder().build();

        assertEquals(0, timeSeries.size());
        assertEquals(0, timeSeries.values().length);
    }


    @Test
    public void testTimeSeriesConcurrency() {
        SingleTimeSeriesBuilder builder = new SingleTimeSeriesBuilder().setMaxSize(100).setId("x");

        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            final int threadNum = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    long timestamp = Thread.currentThread().threadId();
                    builder.add(timestamp, threadNum * 10 + j);
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

        SingleTimeSeries timeSeries = builder.build();

        Assertions.assertTrue(timeSeries.size() > 0);
        long[] ts = timeSeries.timestamps();
        for (int i = 1; i < ts.length; i++) {
            Assertions.assertTrue(ts[i - 1] <= ts[i]);
        }
    }

    @Test
    public void testLargeTimestamps() {
        SingleTimeSeriesBuilder builder = new SingleTimeSeriesBuilder().setMaxSize(5).setId("x");
        builder.add(Long.MAX_VALUE - 1000, 1.0D);
        builder.add(Long.MAX_VALUE - 2000, 2.0D);
        builder.add(Long.MAX_VALUE - 3000, 3.0D);
        SingleTimeSeries timeSeries = builder.build();

        assertEquals(3, timeSeries.size());
        long[] timestamps = timeSeries.timestamps();
        assertEquals(Long.MAX_VALUE - 3000, timestamps[0]);
        assertEquals(Long.MAX_VALUE - 2000, timestamps[1]);
        assertEquals(Long.MAX_VALUE - 1000, timestamps[2]);
    }

    @Test
    public void testSameTimestampsSorted() {
        SingleTimeSeriesBuilder builder = new SingleTimeSeriesBuilder().setMaxSize(5).setId("x");
        builder.add(1000, 1.0D);
        builder.add(1000, 2.0D);
        builder.add(1000, 3.0D);
        SingleTimeSeries timeSeries = builder.build();

        double[] values = timeSeries.values();
        double[] expected = { 3.0D };
        assertArrayEquals(expected, values);
    }

    @Test
    public void testOverwriteMaxSize() {
        SingleTimeSeriesBuilder builder = new SingleTimeSeriesBuilder().setMaxSize(3).setId("x");
        SingleTimeSeries timeSeries = builder
                .add(1000L, 1.0D)
                .add(2000L, 1.0D)
                .add(3000L, 1.0D)
                .add(4000L, 1.0D)
                .build();

        assertEquals(3, timeSeries.size());

        long[] timestamps = timeSeries.timestamps();
        assertEquals(2000L, timestamps[0], "First timestamp should be 2000 (skipped 1000)");
        assertEquals(3000L, timestamps[1], "Second timestamp should be 3000");
        assertEquals(4000L, timestamps[2], "Third timestamp should be 4000 (appended)");
    }
}
