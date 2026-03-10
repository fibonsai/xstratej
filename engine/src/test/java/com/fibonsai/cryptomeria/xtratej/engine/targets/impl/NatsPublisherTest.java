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

package com.fibonsai.cryptomeria.xtratej.engine.targets.impl;

import com.fibonsai.cryptomeria.xtratej.event.series.dao.TradingSignal;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.TradingSignal.Signal;
import io.github.amadeusitgroup.testcontainers.nats.NatsContainer;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;
import io.nats.client.Options;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
public class NatsPublisherTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static String NATS_URL = System.getenv("NATS_URL");

    private NatsPublisher natsPublisher;

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
        natsPublisher = new NatsPublisher("test-nats");
    }

    @AfterEach
    void tearDown() throws Exception {
        if (natsPublisher != null && natsPublisher.isConnected()) {
            natsPublisher.disconnect();
        }
    }

    @Test
    void testConnect() throws Exception {
        if (NATS_URL == null) {
            fail("NATS_URL environment variable must be set for this test");
        }

        natsPublisher.setParams(MAPPER.readTree(String.format("""
            [{
                "servers": ["%s"]
            }]
            """, NATS_URL)));

        boolean connected = natsPublisher.connect();

        assertTrue(connected, "Should connect successfully");
    }

    @Test
    void testReceiveTradingSignalMessage() throws Exception {
        if (NATS_URL == null) {
            fail("NATS_URL environment variable must be set for this test");
        }

        natsPublisher.setParams(MAPPER.readTree(String.format("""
            [{
                "servers": ["%s"],
                "topics": ["test.topic"]
            }]
            """, NATS_URL)));

        boolean connected = natsPublisher.connect();
        assertTrue(connected, "Should connect successfully");

        TradingSignal tradingSignal = new TradingSignal("test", Instant.now().toEpochMilli(), Signal.ENTER, "strategy-test", "BTC/USD", natsPublisher.name());

        CountDownLatch latch = new CountDownLatch(1);
        Options options = Options.builder().server(NATS_URL).build();
        Connection conn = Nats.connect(options);
        Dispatcher dispatcher = conn.createDispatcher(raw -> {
            String msg = new String(raw.getData(), StandardCharsets.UTF_8);
            TradingSignal tradingSignalReceived = MAPPER.readValue(msg, TradingSignal.class);
            assertEquals(tradingSignal.id(), tradingSignalReceived.id());
            assertEquals(tradingSignal.publishers(), tradingSignalReceived.publishers());
            assertEquals(tradingSignal.timestamp(), tradingSignalReceived.timestamp());
            latch.countDown();
        });

        dispatcher.subscribe(natsPublisher.name());

        natsPublisher.emitNext(tradingSignal);

        boolean received = latch.await(5, TimeUnit.SECONDS);

        dispatcher.drain(Duration.ofSeconds(2));
        conn.close();

        assertTrue(received, "Should receive message within timeout");
    }

    @Test
    void testDisconnect() throws Exception {
        if (NATS_URL == null) {
            fail("NATS_URL environment variable must be set for this test");
        }

        natsPublisher.setParams(MAPPER.readTree(String.format("""
            [{
                "servers": ["%s"],
                "topics": ["test.topic"]
            }]
            """, NATS_URL)));

        boolean connected = natsPublisher.connect();
        assertTrue(connected, "Should connect successfully");

        boolean disconnected = natsPublisher.disconnect();
        assertTrue(disconnected, "Should disconnect successfully");
    }
}
