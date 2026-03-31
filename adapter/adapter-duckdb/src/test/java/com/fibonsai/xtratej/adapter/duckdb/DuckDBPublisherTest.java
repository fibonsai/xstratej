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

package com.fibonsai.xtratej.adapter.duckdb;

import com.fibonsai.xtratej.event.series.dao.TradingSignal;
import com.fibonsai.xtratej.event.series.dao.TradingSignal.Signal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
public class DuckDBPublisherTest {

    private JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;

    private DuckDBPublisher duckDBPublisher;

    @Container
    private static final MinIOContainer container = new MinIOContainer("minio/minio:RELEASE.2023-09-04T19-57-37Z");

    private JsonNode params = jsonNodeFactory.nullNode();

    @AfterEach
    void tearDown() throws Exception {
        if (duckDBPublisher != null && duckDBPublisher.isConnected()) {
            duckDBPublisher.disconnect();
        }
    }

    @Test
    void testConnect() throws Exception {
        duckDBPublisher.setParams(params);
        boolean connected = duckDBPublisher.connect();

        assertTrue(connected, "Should connect successfully");
    }

    @Test
    void testReceiveTradingSignalMessage() throws Exception {
        duckDBPublisher.setParams(params);

        boolean connected = duckDBPublisher.connect();
        assertTrue(connected, "Should connect successfully");

        TradingSignal tradingSignal = new TradingSignal("test", Instant.now().toEpochMilli(), Signal.ENTER, "strategy-test", "BTC/USD", duckDBPublisher.name());

        // ...

        duckDBPublisher.emitNext(tradingSignal);


        //assertTrue(received, "Should receive message within timeout");
    }

    @Test
    void testDisconnect() throws Exception {
        duckDBPublisher.setParams(params);

        boolean connected = duckDBPublisher.connect();
        assertTrue(connected, "Should connect successfully");

        boolean disconnected = duckDBPublisher.disconnect();
        assertTrue(disconnected, "Should disconnect successfully");
    }
}
