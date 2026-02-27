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

package com.fibonsai.cryptomeria.xtratej.sources;

import com.fibonsai.cryptomeria.xtratej.event.ITemporalData;
import com.fibonsai.cryptomeria.xtratej.event.reactive.Fifo;
import com.fibonsai.cryptomeria.xtratej.sources.impl.NatsSubscriber;
import com.fibonsai.cryptomeria.xtratej.sources.impl.SimulatedSubscriber;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public enum SourceType {
    NATS(NatsSubscriber.class),
    SIMULATED(SimulatedSubscriber.class),
    UNDEF(null)
    ;

    private final @Nullable Class<? extends Subscriber> clazz;

    SourceType(@Nullable  Class<? extends Subscriber> clazz) {
        this.clazz = clazz;
    }

    public SourceType fromName(String name) {
        for (var value: values()) {
            if (value.name().equals(name)) {
                return value;
            }
        }
        return UNDEF;
    }

    public Builder<? extends Subscriber> builder() {
        return new Builder<>(clazz);
    }

    public static class Builder<T> {
        private final JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
        private final Constructor<T> constructor;
        private String id = "undef";
        private JsonNode properties = nodeFactory.nullNode();
        private Fifo<ITemporalData> results = new Fifo<>();

        public Builder(@Nullable Class<T> clazz) {
            try {
                if (clazz == null) {
                    throw new UnsupportedOperationException();
                }
                this.constructor = clazz.getConstructor(String.class, JsonNode.class, Fifo.class);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public Builder<T> setProperties(JsonNode properties) {
            this.properties = properties;
            return this;
        }

        public Builder<T> setId(String id) {
            this.id = id;
            return this;
        }

        public Builder<T> setResults(Fifo<ITemporalData> results) {
            this.results = results;
            return this;
        }

        public T build() {
            try {
                return constructor.newInstance(id, properties, results);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
