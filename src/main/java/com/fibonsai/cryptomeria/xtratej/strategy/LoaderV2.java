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

package com.fibonsai.cryptomeria.xtratej.strategy;

import com.fibonsai.cryptomeria.xtratej.rules.RuleStream;
import com.fibonsai.cryptomeria.xtratej.rules.RuleType;
import com.fibonsai.cryptomeria.xtratej.sources.SourceType;
import com.fibonsai.cryptomeria.xtratej.sources.Subscriber;
import com.fibonsai.cryptomeria.xtratej.strategy.IStrategy.StrategyType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.fibonsai.cryptomeria.xtratej.strategy.LoaderV2.SchemaKey.*;

/**
 * Strategy loader V2
 */
public class LoaderV2 {

    public enum SchemaKey {
        STRATEGIES("strategies"),
        SYMBOL("symbol"),
        TYPE("type"),
        SOURCES("sources"),
        PUBLISHER("publisher"),
        PARAMS("params"),
        RULE("rule"),
        INPUTS("inputs"),
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
                        JsonNode sourceParams = NODE_FACTORY.nullNode();
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
                        Subscriber sourceInstance = sourceType.builder()
                                .setName(sourceName)
                                .setPublisher(publisher)
                                .setProperties(sourceParams).build();
                        strategy.addSource(sourceInstance);
                    }
                }

                // rule (recursive structure)
                if (strategyJson.hasNonNull(RULE.key()) && strategyJson.get(RULE.key()).isObject()) {
                    Set<Map.Entry<String, JsonNode>> ruleEntries = strategyJson.get(RULE.key()).properties();
                    for (var ruleEntry : ruleEntries) {
                        String rootRuleName = ruleEntry.getKey();
                        parseRule(rootRuleName, ruleEntry.getValue(), strategy);
                        strategy.setAggregatorRule(rootRuleName);
                    }
                }

                strategiesMap.put(strategyName, strategy);
            }
        }
        return strategiesMap;
    }

    private static void parseRule(String ruleName, JsonNode ruleJson, IStrategy strategy) {
        RuleType ruleType = RuleType.False;
        JsonNode ruleParams = NODE_FACTORY.nullNode();
        if (ruleJson.hasNonNull(TYPE.key()) && ruleJson.get(TYPE.key()).isString()) {
            ruleType = RuleType.fromName(ruleJson.get(TYPE.key()).asString());
        }
        if (ruleJson.hasNonNull(PARAMS.key())) {
            ruleParams = ruleJson.get(PARAMS.key());
        }
        RuleStream ruleInstance = ruleType.builder().setId(ruleName).setProperties(ruleParams).build();

        if (ruleJson.hasNonNull(INPUTS.key())) {
            JsonNode inputs = ruleJson.get(INPUTS.key());
            if (inputs.isArray()) {
                // Leaf rule taking sources as inputs
                for (JsonNode input : inputs) {
                    if (input.isString()) {
                        ruleInstance.addSourceId(input.asString());
                    }
                }
            } else if (inputs.isObject()) {
                // Composite rule taking other rules as inputs
                Set<Map.Entry<String, JsonNode>> subRules = inputs.properties();
                for (var entry : subRules) {
                    String subRuleName = entry.getKey();
                    parseRule(subRuleName, entry.getValue(), strategy);
                    ruleInstance.addSourceId(subRuleName);
                }
            }
        }
        strategy.addRule(ruleInstance);
    }
}
