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

import com.fibonsai.cryptomeria.xtratej.engine.targets.Publisher;
import com.fibonsai.cryptomeria.xtratej.engine.targets.WithParams;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.TradingSignal;
import io.nats.client.AuthHandler;
import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.impl.Headers;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.fibonsai.cryptomeria.xtratej.engine.targets.impl.NatsPublisher.NatsKey.*;

public class NatsPublisher extends Publisher implements WithParams {

    public enum NatsKey {
        NATS_CREDS("nats-creds"),
        SERVERS("servers"),
        MAX_RECONNECTS("max-reconnects"),
        MAX_MESSAGES_INOUTGOING_QUEUE("max-messages-outgoing-queue"),
        ;

        private final String key;

        NatsKey(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    private static final Logger log = LoggerFactory.getLogger(NatsPublisher.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Options natsOptions = Options.builder().build();
    private @Nullable Connection connection;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final Headers headers = new Headers().add("class", TradingSignal.class.getSimpleName());

    public NatsPublisher(String name) {
        super(name);
        subscribe(publishToNats());
    }

    @Override
    public Publisher setParams(JsonNode params) {
        String natsCreds = null;
        Options.Builder natsOptionsBuilder = Options.builder();
        for (var param: params) {
            if (param.hasNonNull(NATS_CREDS.key()) && param.get(NATS_CREDS.key()).isString()) {
                natsCreds = param.get(NATS_CREDS.key()).asString();
            }
            if (param.hasNonNull(SERVERS.key()) && param.get(SERVERS.key()).isArray()) {
                for (var server: param.get(SERVERS.key())) {
                    natsOptionsBuilder.server(server.asString());
                }
            }
            if (param.hasNonNull(MAX_RECONNECTS.key()) && param.get(MAX_RECONNECTS.key()).isInt()) {
                natsOptionsBuilder.maxReconnects(param.get(MAX_RECONNECTS.key()).asInt());
            }
            if (param.hasNonNull(MAX_MESSAGES_INOUTGOING_QUEUE.key()) && param.get(MAX_MESSAGES_INOUTGOING_QUEUE.key()).isInt()) {
                natsOptionsBuilder.maxMessagesInOutgoingQueue(param.get(MAX_MESSAGES_INOUTGOING_QUEUE.key()).asInt());
            }
        }
        if (natsCreds == null) natsCreds = System.getenv("NATS_CREDS");
        if (natsCreds != null) {
            AuthHandler authHandler = Nats.credentials(natsCreds);
            natsOptionsBuilder.authHandler(authHandler);
        }

        natsOptions = natsOptionsBuilder.build();
        return this;
    }

    private java.util.function.Consumer<TradingSignal> publishToNats() {
        return signal -> {
            if (connection != null && isConnected()) {
                byte[] bytes = MAPPER.writeValueAsBytes(signal);
                connection.publish(name(), headers, bytes);
            } else {
                log.error("connections is NULL or Not Connected. Ignoring {}", signal);
            }
        };
    }

    @Override
    public boolean connect() {
        try {
            connection = Nats.connectReconnectOnConnect(natsOptions);
            connected.set(connection.getStatus() == Connection.Status.CONNECTED);
        } catch (InterruptedException | IOException e) {
            log.error(e.getMessage(), e);
        }
        return connected.get();
    }

    @Override
    public boolean disconnect() {
        if (connection != null) {
            try {
                connection.close();
                connected.set(false);
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
            }
        }
        return !connected.get();
    }

    @Override
    public boolean isConnected() {
        return connected.get();
    }
}
