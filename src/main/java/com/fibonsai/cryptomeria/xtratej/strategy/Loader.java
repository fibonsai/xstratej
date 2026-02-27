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

/**
 * Strategy loader
 */
public class Loader {

    private static final JsonNodeFactory NODE_FACTORY = JsonNodeFactory.instance;

    public static Map<String, IStrategy> fromJson(JsonNode json) {

        final Map<String, IStrategy> strategiesMap = new HashMap<>();

        if (json.isObject() && json.hasNonNull("strategies")) {
            Set<Map.Entry<String, JsonNode>> strategies = json.get("strategies").properties();
            for (var strategyEntry: strategies) {
                String strategyName = strategyEntry.getKey();
                String strategySymbol = "undef";
                String strategySource = "undef";
                StrategyType strategyType = StrategyType.UNDEF;
                JsonNode strategyJson = strategyEntry.getValue();
                if (strategyJson.hasNonNull("symbol") && strategyJson.get("symbol").isString()) {
                    strategySymbol = strategyJson.get("symbol").asString();
                }
                if (strategyJson.hasNonNull("source") && strategyJson.get("source").isString()) {
                    strategySource = strategyJson.get("source").asString();
                }
                if (strategyJson.hasNonNull("type") && strategyJson.get("type").isString()) {
                    String typeAsString = strategyJson.get("type").asString();
                    strategyType = StrategyType.fromName(typeAsString);
                }
                IStrategy strategy = new Strategy(strategyName, strategySymbol, strategySource, strategyType);

                // sources
                if (strategyJson.hasNonNull("sources")) {
                    Set<Map.Entry<String, JsonNode>> sources = strategyJson.get("sources").properties();
                    for (var sourceEntry: sources) {
                        String sourceName = sourceEntry.getKey();
                        JsonNode sourceJson = sourceEntry.getValue();
                        JsonNode sourceParams = NODE_FACTORY.nullNode();
                        SourceType sourceType = SourceType.UNDEF;
                        if (sourceJson.hasNonNull("type") && sourceJson.get("type").isString()) {
                            sourceType = SourceType.fromName(sourceJson.get("type").asString());
                        }
                        if (sourceJson.hasNonNull("params")) {
                            sourceParams = sourceJson.get("params");
                        }
                        Subscriber sourceInstance = sourceType.builder().setId(sourceName).setProperties(sourceParams).build();
                        strategy.addSource(sourceInstance);
                    }
                }

                // rules
                if (strategyJson.hasNonNull("rules")) {
                    Set<Map.Entry<String, JsonNode>> rulesJson = strategyJson.get("rules").properties();
                    for (var ruleEntry: rulesJson) {
                        String ruleName = ruleEntry.getKey();
                        JsonNode ruleJson = ruleEntry.getValue();
                        RuleType ruleType = RuleType.False;
                        JsonNode ruleParams = NODE_FACTORY.nullNode();
                        if (ruleJson.hasNonNull("type") && ruleJson.get("type").isString()) {
                            ruleType = RuleType.fromName(ruleJson.get("type").asString());
                        }
                        if (ruleJson.hasNonNull("params")) {
                            ruleParams = ruleJson.get("params");
                        }
                        RuleStream ruleInstance = ruleType.builder().setId(ruleName).setProperties(ruleParams).build();
                        if (ruleJson.hasNonNull("sources") && ruleJson.get("sources").isArray()) {
                            for (var source: ruleJson.get("sources")) {
                                if (source.isString()) {
                                    ruleInstance.addSourceId(source.asString());
                                }
                            }
                        }
                        strategy.addRule(ruleInstance);
                    }
                }

                // aggregator
                if (strategyJson.hasNonNull("aggregator") && strategyJson.get("aggregator").isString()) {
                    strategy.setAggregatorRule(strategyJson.get("aggregator").asString());
                }

                strategiesMap.put(strategyName, strategy);
            }
        }
        return strategiesMap;
    }
}
