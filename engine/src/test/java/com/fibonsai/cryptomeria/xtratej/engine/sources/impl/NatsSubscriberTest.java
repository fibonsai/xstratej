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

package com.fibonsai.cryptomeria.xtratej.engine.sources.impl;

import com.fibonsai.cryptomeria.xtratej.event.series.dao.BarTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.BooleanTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.DoubleTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.TimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.builders.BarTimeSeriesBuilder;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.builders.BooleanTimeSeriesBuilder;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.builders.DoubleTimeSeriesBuilder;
import io.github.amadeusitgroup.testcontainers.nats.NatsContainer;
import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.impl.Headers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
public class NatsSubscriberTest {

    private static final Logger log = LoggerFactory.getLogger(NatsSubscriberTest.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static String NATS_URL = System.getenv("NATS_URL");

    private NatsSubscriber natsSubscriber;

    @Container
    private static final NatsContainer natsServer = new NatsContainer("nats:2.9");

    @BeforeAll
    static void init() {
        if (NATS_URL == null) {
            NATS_URL = natsServer.getConnectionUrl();
        }
    }

    @BeforeEach
    void setUp() {
        natsSubscriber = new NatsSubscriber("test-nats", "test-publisher");
    }

    @AfterEach
    void tearDown() throws Exception {
        if (natsSubscriber != null && natsSubscriber.isConnected()) {
            natsSubscriber.disconnect();
        }
    }

    @Test
    void testConnectAndSubscribe() throws Exception {
        if (NATS_URL == null) {
            fail("NATS_URL environment variable must be set for this test");
        }

        natsSubscriber.setParams(MAPPER.readTree(String.format("""
            [{
                "servers": ["%s"]
            }]
            """, NATS_URL)));

        boolean connected = natsSubscriber.connect();

        assertTrue(connected, "Should connect successfully");
    }

    @Test
    void testReceiveDoubleTimeSeriesMessage() throws Exception {
        if (NATS_URL == null) {
            fail("NATS_URL environment variable must be set for this test");
        }

        natsSubscriber.setParams(MAPPER.readTree(String.format("""
            [{
                "servers": ["%s"],
                "topics": ["test.topic"]
            }]
            """, NATS_URL)));

        boolean connected = natsSubscriber.connect();
        assertTrue(connected, "Should connect successfully");

        DoubleTimeSeries testSeries = new DoubleTimeSeriesBuilder().setId("test-series").add(Instant.now().toEpochMilli(), 123.45).build();

        boolean received = sendAndSubscribe(testSeries).await(5, TimeUnit.SECONDS);
        assertTrue(received, "Should receive message within timeout");
    }

    @Test
    void testReceiveBooleanTimeSeriesMessage() throws Exception {
        if (NATS_URL == null) {
            fail("NATS_URL environment variable must be set for this test");
        }

        natsSubscriber.setParams(MAPPER.readTree(String.format("""
            [{
                "servers": ["%s"],
                "topics": ["test.topic"]
            }]
            """, NATS_URL)));

        boolean connected = natsSubscriber.connect();
        assertTrue(connected, "Should connect successfully");

        BooleanTimeSeries testSeries = new BooleanTimeSeriesBuilder().setId("test-series").add(Instant.now().toEpochMilli(), true).build();

        boolean received = sendAndSubscribe(testSeries).await(5, TimeUnit.SECONDS);
        assertTrue(received, "Should receive message within timeout");
    }

    @Test
    void testReceiveBarTimeSeriesMessage() throws Exception {
        if (NATS_URL == null) {
            fail("NATS_URL environment variable must be set for this test");
        }

        natsSubscriber.setParams(MAPPER.readTree(String.format("""
            [{
                "servers": ["%s"],
                "topics": ["test.topic"]
            }]
            """, NATS_URL)));

        boolean connected = natsSubscriber.connect();
        assertTrue(connected, "Should connect successfully");

        BarTimeSeries testSeries = new BarTimeSeriesBuilder().setId("test-series").add(Instant.now().toEpochMilli(), 1.0D, 2.0D, 3.0D, 4.0D, 5.0D).build();

        boolean received = sendAndSubscribe(testSeries).await(5, TimeUnit.SECONDS);
        assertTrue(received, "Should receive message within timeout");
    }

    @Test
    void testDisconnect() throws Exception {
        if (NATS_URL == null) {
            fail("NATS_URL environment variable must be set for this test");
        }

        natsSubscriber.setParams(MAPPER.readTree(String.format("""
            [{
                "servers": ["%s"],
                "topics": ["test.topic"]
            }]
            """, NATS_URL)));

        boolean connected = natsSubscriber.connect();
        assertTrue(connected, "Should connect successfully");

        boolean disconnected = natsSubscriber.disconnect();
        assertTrue(disconnected, "Should disconnect successfully");
    }

    @Test
    void testMultipleTopics() throws Exception {
        if (NATS_URL == null) {
            fail("NATS_URL environment variable must be set for this test");
        }

        natsSubscriber.setParams(MAPPER.readTree(String.format("""
            [{
                "servers": ["%s"],
                "topics": ["topic1", "topic2"]
            }]
            """, NATS_URL)));

        boolean connected = natsSubscriber.connect();
        assertTrue(connected, "Should connect successfully");

        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);
        
        natsSubscriber.toDirectFlux().subscribe(timeSeries -> {
            assertInstanceOf(DoubleTimeSeries.class, timeSeries);
            DoubleTimeSeries doubleTimeSeries = (DoubleTimeSeries) timeSeries;
            if ("topic1-series".equals(doubleTimeSeries.id())) {
                latch1.countDown();
            } else if ("topic2-series".equals(doubleTimeSeries.id())) {
                latch2.countDown();
            }
        });

        Options options = Options.builder().server(NATS_URL).build();
        Connection conn = Nats.connect(options);
        Headers headers = new Headers();
        headers.add("class", DoubleTimeSeries.class.getSimpleName());

        DoubleTimeSeries series1 = new DoubleTimeSeriesBuilder().setId("topic1-series").add(Instant.now().toEpochMilli(), 100.0).build();
        String series1Str = MAPPER.writeValueAsString(series1);
        log.error(series1Str);

        conn.publish("topic1", headers, series1Str.getBytes());

        DoubleTimeSeries series2 = new DoubleTimeSeriesBuilder().setId("topic2-series").add(Instant.now().toEpochMilli(), 200.0).build();

        conn.publish("topic2", headers, MAPPER.writeValueAsString(series2).getBytes());

        conn.close();

        boolean received1 = latch1.await(5, TimeUnit.SECONDS);
        boolean received2 = latch2.await(5, TimeUnit.SECONDS);
        
        assertTrue(received1, "Should receive message from topic1");
        assertTrue(received2, "Should receive message from topic2");
    }

    private CountDownLatch sendAndSubscribe(TimeSeries testSeries) throws IOException, InterruptedException {
        String message = MAPPER.writeValueAsString(testSeries);
        Headers headers = new Headers();
        headers.add("class", testSeries.getClass().getSimpleName());

        CountDownLatch latch = new CountDownLatch(1);
        natsSubscriber.toDirectFlux().subscribe(data -> {
            assertInstanceOf(testSeries.getClass(), data);
            TimeSeries timeSeries = testSeries.getClass().cast(data);
            assertEquals("test-series", timeSeries.id());
            assertTrue(timeSeries.timestamp() > 0);
            latch.countDown();
        });

        Options options = Options.builder().server(NATS_URL).build();
        Connection conn = Nats.connect(options);
        conn.publish("test.topic", headers, message.getBytes());
        conn.close();
        return latch;
    }
}
