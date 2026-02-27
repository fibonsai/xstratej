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

import com.fibonsai.cryptomeria.xtratej.event.ITemporalData;
import com.fibonsai.cryptomeria.xtratej.event.reactive.Fifo;
import com.fibonsai.cryptomeria.xtratej.sources.Subscriber;
import tools.jackson.databind.JsonNode;

public class NatsSubscriber implements Subscriber {

    private final String name;
    private final JsonNode properties;
    private final Fifo<ITemporalData> fifo = new Fifo<>();

    public NatsSubscriber(String name, JsonNode properties) {
        this.name = name;
        this.properties = properties;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public Fifo<ITemporalData> toFifo() {
        return fifo;
    }
}
