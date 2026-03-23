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

import com.fibonsai.cryptomeria.xtratej.engine.sources.Subscriber;
import com.fibonsai.cryptomeria.xtratej.engine.sources.WithParams;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.BarTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.BooleanTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.DoubleTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.TimeSeries;
import io.nats.client.*;
import io.nats.client.impl.Headers;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.fibonsai.cryptomeria.xtratej.engine.sources.impl.NatsSubscriber.NatsKey.*;

public class NatsSubscriber extends Subscriber implements WithParams {

    public static final List<Class<? extends TimeSeries>> CLASSES_SUPPORTED = List.of(DoubleTimeSeries.class, BarTimeSeries.class, BooleanTimeSeries.class);

    public enum NatsKey {
        NATS_CREDS("nats-creds"),
        SERVERS("servers"),
        MAX_RECONNECTS("max-reconnects"),
        MAX_MESSAGES_INOUTGOING_QUEUE("max-messages-outgoing-queue"),
        TOPICS("topics"),
        ;

        private final String key;

        NatsKey(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    private static final Logger log = LoggerFactory.getLogger(NatsSubscriber.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Options natsOptions = Options.builder().build();
    private @Nullable Dispatcher dispatcher;
    private @Nullable Connection connection;
    private final List<String> topics = new ArrayList<>();
    private final AtomicBoolean connected = new AtomicBoolean(false);

    public NatsSubscriber(String name, String publisher) {
        super(name, publisher);
    }

    @Override
    public Subscriber setParams(JsonNode params) {
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
            if (param.hasNonNull(TOPICS.key()) && param.get(TOPICS.key()).isArray()) {
                for (var topic: param.get(TOPICS.key())) {
                    topics.add(topic.asString());
                }
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

    @Override
    public boolean connect() {
        try {
            connection = Nats.connectReconnectOnConnect(natsOptions);
            dispatcher = connection.createDispatcher(raw -> {
                String msg = new String(raw.getData(), StandardCharsets.UTF_8);
                Headers headers = raw.getHeaders();
                String className = headers != null ? headers.getFirst("class") : Object.class.getSimpleName();
                TimeSeries timeSeries = null;

                if (className != null) {
                    for (var clazz: CLASSES_SUPPORTED) {
                        if (clazz.getSimpleName().equals(className)) {
                            timeSeries = MAPPER.readValue(msg, clazz);
                            break;
                        }
                    }
                    if (timeSeries != null) {
                        if (log.isDebugEnabled()) {
                            log.debug(">>>>>>> [{}] SEND {}", timeSeries.timestamp(), timeSeries);
                        }
                        toDirectFlux().emitNext(timeSeries);
                    } else {
                        log.warn("header `class` NOT defined or its value IS NOT supported");
                    }
                }
            });
            boolean subscribed = topics.stream().map(dispatcher::subscribe).allMatch(Consumer::isActive);
            connected.set(connection.getStatus() == Connection.Status.CONNECTED && dispatcher.isActive() && subscribed);
        } catch (InterruptedException | IOException e) {
            log.error(e.getMessage(), e);
        }
        return connected.get();
    }

    @Override
    public boolean disconnect() {
        if (dispatcher != null && connection != null) {
            try {
                CompletableFuture<Boolean> drained = dispatcher.drain(Duration.ofSeconds(10));
                drained.get();
                connection.close();
                connected.set(false);
            } catch (InterruptedException|ExecutionException e) {
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
