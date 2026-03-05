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

package com.fibonsai.cryptomeria.xtratej.engine.strategy;

import com.fibonsai.cryptomeria.xtratej.engine.rules.RuleStream;
import com.fibonsai.cryptomeria.xtratej.engine.rules.RuleType;
import com.fibonsai.cryptomeria.xtratej.engine.sources.SourceType;
import com.fibonsai.cryptomeria.xtratej.engine.sources.Subscriber;
import com.fibonsai.cryptomeria.xtratej.engine.strategy.IStrategy.StrategyType;
import com.fibonsai.cryptomeria.xtratej.event.ITemporalData;
import com.fibonsai.cryptomeria.xtratej.event.reactive.Fifo;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.fibonsai.cryptomeria.xtratej.engine.strategy.Loader.SchemaKey.*;

/**
 * Strategy loader V2
 */
public class Loader {

    public enum SchemaKey {
        STRATEGIES("strategies"),
        SYMBOL("symbol"),
        TYPE("type"),
        SOURCES("sources"),
        PUBLISHER("publisher"),
        PARAMS("params"),
        RULE("rule"),
        INPUTS("inputs"),
        DESCRIPTION("description"),
        ;

        private final String keyName;

        SchemaKey(String keyName) {
            this.keyName = keyName;
        }

        public String key() {
            return keyName;
        }
    }

    private static final String UNDEF = "undef";
    private static final JsonNodeFactory NODE_FACTORY = JsonNodeFactory.instance;
    public static final JsonNode EMPTY_PARAMS = new ObjectNode(NODE_FACTORY, Map.of());
    public static final JsonNode EMPTY_ARRAY = new ArrayNode(NODE_FACTORY, List.of());

    public static Map<String, IStrategy> fromJson(JsonNode json) {

        final Map<String, IStrategy> strategiesMap = new HashMap<>();

        if (json.isObject() && json.hasNonNull(STRATEGIES.key())) {
            Set<Map.Entry<String, JsonNode>> strategies = json.get(STRATEGIES.key()).properties();
            for (var strategyEntry: strategies) {
                String strategyName = strategyEntry.getKey();
                String strategySymbol = UNDEF;
                StrategyType strategyType = StrategyType.UNDEF;
                JsonNode strategyJson = strategyEntry.getValue();
                if (strategyJson.hasNonNull(SYMBOL.key()) && strategyJson.get(SYMBOL.key()).isString()) {
                    strategySymbol = strategyJson.get(SYMBOL.key()).asString();
                }
                if (strategyJson.hasNonNull(TYPE.key()) && strategyJson.get(TYPE.key()).isString()) {
                    String typeAsString = strategyJson.get(TYPE.key()).asString();
                    strategyType = StrategyType.fromName(typeAsString);
                }
                IStrategy strategy = new Strategy(strategyName, strategySymbol, strategyType);

                // sources
                if (strategyJson.hasNonNull(SOURCES.key())) {
                    Set<Map.Entry<String, JsonNode>> sources = strategyJson.get(SOURCES.key()).properties();
                    for (var sourceEntry: sources) {
                        String sourceName = sourceEntry.getKey();
                        JsonNode sourceJson = sourceEntry.getValue();
                        JsonNode sourceParams = EMPTY_PARAMS;
                        SourceType sourceType = SourceType.UNDEF;
                        String publisher = UNDEF;
                        if (sourceJson.hasNonNull(TYPE.key()) && sourceJson.get(TYPE.key()).isString()) {
                            sourceType = SourceType.fromName(sourceJson.get(TYPE.key()).asString());
                        }
                        if (sourceJson.hasNonNull(PUBLISHER.key()) && sourceJson.get(PUBLISHER.key()).isString()) {
                            publisher = sourceJson.get(PUBLISHER.key()).asString();
                        }
                        if (sourceJson.hasNonNull(PARAMS.key())) {
                            sourceParams = sourceJson.get(PARAMS.key());
                        }
                        strategy.addSource(sourceType, sourceName, publisher, sourceParams);
                    }
                }

                // rule (recursive structure)
                if (strategyJson.hasNonNull(RULE.key())) {
                    JsonNode ruleAggregatorJson = strategyJson.get(RULE.key());
                    RuleStream ruleAggregator = parseRule(ruleAggregatorJson, strategy);
                    strategy.setAggregatorRule(ruleAggregator);
                }

                strategiesMap.put(strategyName, strategy);
            }
        }
        return strategiesMap;
    }

    private static RuleStream parseRule(JsonNode ruleJson, IStrategy strategy) {
        RuleType ruleType = RuleType.False;
        JsonNode ruleParams = EMPTY_PARAMS;
        String description = "";
        JsonNode inputs = EMPTY_ARRAY;

        if (ruleJson.hasNonNull(TYPE.key()) && ruleJson.get(TYPE.key()).isString()) {
            ruleType = RuleType.fromName(ruleJson.get(TYPE.key()).asString());
        }
        if (ruleJson.hasNonNull(PARAMS.key())) {
            ruleParams = ruleJson.get(PARAMS.key());
        }
        if (ruleJson.hasNonNull(DESCRIPTION.key()) && ruleJson.get(DESCRIPTION.key()).isString()) {
            description = ruleJson.get(DESCRIPTION.key()).asString();
        }
        if (ruleJson.hasNonNull(INPUTS.key()) && ruleJson.get(INPUTS.key()).isArray()) {
            inputs = ruleJson.get(INPUTS.key()).asArray();
        }

        RuleStream ruleInstance = ruleType.build()
                .setParams(ruleParams)
                .setDescription(description);

        if (!inputs.isEmpty()) {
            JsonNode firstInput = inputs.get(0);
            Fifo<ITemporalData>[] fifos = Fifo.createArray(inputs.size());
            int counter = 0;
            if (firstInput != null && firstInput.isString()) {
                for (var input : inputs) {
                    Subscriber subscriber = strategy.getSources().get(input.asString());
                    fifos[counter++] = subscriber != null ? subscriber.toFifo() : Fifo.empty();
                }
            } else {
                for (var input : inputs) {
                    RuleStream subRule = parseRule(input, strategy);
                    fifos[counter++] = subRule.results();
                }
            }
            ruleInstance.watch(Fifo.zip(fifos));
        }
        return ruleInstance;
    }

}
