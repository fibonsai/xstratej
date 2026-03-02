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

package com.fibonsai.cryptomeria.xtratej.rules;

import com.fibonsai.cryptomeria.xtratej.event.ITemporalData;
import com.fibonsai.cryptomeria.xtratej.event.reactive.Fifo;
import com.fibonsai.cryptomeria.xtratej.event.series.impl.BooleanSingleTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.impl.BooleanSingleTimeSeries.BooleanSingle;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public abstract class RuleStream {

    private static final JsonNode NULL_NODE = JsonNodeFactory.instance.nullNode();

    private Set<Map.Entry<String, JsonNode>> properties;

    private final Fifo<ITemporalData> results = new Fifo<>();
    private final AtomicBoolean activated = new AtomicBoolean(false);

    private String description = "";

    public RuleStream() {
        this(NULL_NODE);
    }

    protected RuleStream(JsonNode properties) {
        this.properties = properties.properties();
        processProperties();
    }

    public Set<Map.Entry<String, JsonNode>> getProperties() {
        return properties;
    }

    public RuleStream setProperties(JsonNode properties) {
        this.properties = properties.properties();
        processProperties();
        return this;
    }

    public void subscribe(Fifo<ITemporalData[]> inputs) {
        inputs.onSubscribe(() -> activated.set(true)).subscribe(temporalDatas -> {
            BooleanSingle[] booleanSingles = predicate().apply(temporalDatas);
            BooleanSingleTimeSeries resultSeries = new BooleanSingleTimeSeries(toString(), booleanSingles);
            results.emitNext(resultSeries);
        });
    }

    public boolean isActivated() {
        return activated.get();
    }

    protected abstract void processProperties();

    protected abstract Function<ITemporalData[], BooleanSingle[]> predicate();

    public Fifo<ITemporalData> results() {
        return results;
    }

    public String getDescription() {
        return description;
    }

    public RuleStream setDescription(String description) {
        this.description = description;
        return this;
    }
}
