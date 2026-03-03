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

package com.fibonsai.cryptomeria.xtratej.sources.impl;

import com.fibonsai.cryptomeria.xtratej.sources.Subscriber;
import com.fibonsai.cryptomeria.xtratej.sources.WithProperties;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

public class NatsSubscriber extends Subscriber implements WithProperties {

    private JsonNode properties = JsonNodeFactory.instance.objectNode();

    public NatsSubscriber(String name, String publisher) {
        super(name, publisher);
    }

    @Override
    public Subscriber setProperties(JsonNode properties) {
        this.properties = properties;
        return this;
    }

    @Override
    public boolean connect() {
        // WIP. Ref: https://github.com/nats-io/nats.java

        JsonNode natsProperties = JsonNodeFactory.instance.objectNode();
        if (properties.hasNonNull("nats")) {
            natsProperties = properties.get("nats");
        }

        /* Add dependency:
         *
         * <dependency>
         *     <groupId>io.nats</groupId>
         *     <artifactId>jnats</artifactId>
         *     <version>2.25.1</version>
         * </dependency>
         *
         * And...
         *
         * AuthHandler authHandler = Nats.credentials(System.getenv("NATS_CREDS"));
         * Options o = new Options.Builder()
         *                          .server("nats://serverone:4222")
         *                          .server("nats://servertwo:4222")
         *                          .maxReconnects(-1)
         *                          .maxMessagesInOutgoingQueue(6000)
         *                          .maxMessagesInOutgoingQueue(8000)
         *                          .authHandler(authHandler)
         *                          .build();
         * Connection nc = Nats.connectReconnectOnConnect(o);
         *
         * // Create a dispatcher and inline message handler
         * Dispatcher d = nc.createDispatcher((msg) -> { ... });
         *
         * // Subscribe
         * d.subscribe("updates");
         */
        return false;
    }

    @Override
    public boolean disconnect() {
        // WIP. Ref: https://docs.nats.io/using-nats/developer/receiving/drain#java

        /*
         * // Messages that have arrived will be processed
         * CompletableFuture<Boolean> drained = d.drain(Duration.ofSeconds(10));
         *
         * // Wait for the drain to complete
         * drained.get();
         *
         * // Close the connection
         * nc.close();
         */
        return false;
    }
}
