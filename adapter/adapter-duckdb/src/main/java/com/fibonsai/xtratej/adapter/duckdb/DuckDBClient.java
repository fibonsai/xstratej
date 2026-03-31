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

import com.fibonsai.directflux.DirectFlux;
import com.fibonsai.xtratej.adapter.core.Adapter;
import com.fibonsai.xtratej.adapter.core.WithParams;
import com.fibonsai.xtratej.event.series.dao.BarTimeSeries;
import com.fibonsai.xtratej.event.series.dao.BooleanTimeSeries;
import com.fibonsai.xtratej.event.series.dao.DoubleTimeSeries;
import com.fibonsai.xtratej.event.series.dao.TimeSeries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public class DuckDBClient implements Adapter, WithParams {

    private final DirectFlux<TimeSeries> caller;

    public enum DuckDBKey {
        NATS_CREDS("nats-creds"),
        SERVERS("servers"),
        MAX_RECONNECTS("max-reconnects"),
        MAX_MESSAGES_INOUTGOING_QUEUE("max-messages-outgoing-queue"),
        TOPICS("topics"),
        ;

        private final String key;

        DuckDBKey(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    public static final List<Class<? extends TimeSeries>> CLASSES_SUPPORTED = List.of(DoubleTimeSeries.class, BarTimeSeries.class, BooleanTimeSeries.class);

    private static final Logger log = LoggerFactory.getLogger(DuckDBClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private boolean connected = false;

    private final List<String> topics = new ArrayList<>();

    private Runnable onConnect = () -> {};
    private Runnable onDisconnect = () -> {};

    public DuckDBClient(DirectFlux<TimeSeries> caller) {
        this.caller = caller;
    }

    @Override
    public DuckDBClient setParams(JsonNode params) {
        return this;
    }

    public DuckDBClient onConnect(Runnable runnable) {
        this.onConnect = runnable;
        return this;
    }

    public DuckDBClient onDisconnect(Runnable runnable) {
        this.onDisconnect = runnable;
        return this;
    }

    @Override
    public boolean connect() {

        try {
            onConnect.run();
        } catch (Exception e) {
            log.error("onConnect error", e);
        }
        return connected;
    }

    @Override
    public boolean disconnect() {
        if (connected) {
            try {
                onDisconnect.run();
                connected = false;
            } catch (Exception e) {
                log.error("onDisconnect error", e);
            }
        }
        return !connected;
    }

    public void publish(TimeSeries timeSeries) {
        caller.emitNext(timeSeries);
    }

    @Override
    public boolean isConnected() {
        return connected;
    }
}
