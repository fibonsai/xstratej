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
 * Strategy loader V2
 */
public class LoaderV2 {

    private static final JsonNodeFactory NODE_FACTORY = JsonNodeFactory.instance;

    public static Map<String, IStrategy> fromJson(JsonNode json) {

        final Map<String, IStrategy> strategiesMap = new HashMap<>();

        if (json.isObject() && json.hasNonNull("strategies")) {
            Set<Map.Entry<String, JsonNode>> strategies = json.get("strategies").properties();
            for (var strategyEntry: strategies) {
                String strategyName = strategyEntry.getKey();
                String strategySymbol = "undef";
                StrategyType strategyType = StrategyType.UNDEF;
                JsonNode strategyJson = strategyEntry.getValue();
                if (strategyJson.hasNonNull("symbol") && strategyJson.get("symbol").isString()) {
                    strategySymbol = strategyJson.get("symbol").asString();
                }
                if (strategyJson.hasNonNull("type") && strategyJson.get("type").isString()) {
                    String typeAsString = strategyJson.get("type").asString();
                    strategyType = StrategyType.fromName(typeAsString);
                }
                IStrategy strategy = new Strategy(strategyName, strategySymbol, strategyType);

                // sources
                if (strategyJson.hasNonNull("sources")) {
                    Set<Map.Entry<String, JsonNode>> sources = strategyJson.get("sources").properties();
                    for (var sourceEntry: sources) {
                        String sourceName = sourceEntry.getKey();
                        JsonNode sourceJson = sourceEntry.getValue();
                        JsonNode sourceParams = NODE_FACTORY.nullNode();
                        SourceType sourceType = SourceType.UNDEF;
                        String publisher = "undef";
                        if (sourceJson.hasNonNull("type") && sourceJson.get("type").isString()) {
                            sourceType = SourceType.fromName(sourceJson.get("type").asString());
                        }
                        if (sourceJson.hasNonNull("publisher") && sourceJson.get("publisher").isString()) {
                            publisher = sourceJson.get("publisher").asString();
                        }
                        if (sourceJson.hasNonNull("params")) {
                            sourceParams = sourceJson.get("params");
                        }
                        Subscriber sourceInstance = sourceType.builder()
                                .setName(sourceName)
                                .setPublisher(publisher)
                                .setProperties(sourceParams).build();
                        strategy.addSource(sourceInstance);
                    }
                }

                // rule (recursive structure)
                if (strategyJson.hasNonNull("rule") && strategyJson.get("rule").isObject()) {
                    Set<Map.Entry<String, JsonNode>> ruleEntries = strategyJson.get("rule").properties();
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
        if (ruleJson.hasNonNull("type") && ruleJson.get("type").isString()) {
            ruleType = RuleType.fromName(ruleJson.get("type").asString());
        }
        if (ruleJson.hasNonNull("params")) {
            ruleParams = ruleJson.get("params");
        }
        RuleStream ruleInstance = ruleType.builder().setId(ruleName).setProperties(ruleParams).build();

        if (ruleJson.hasNonNull("inputs")) {
            JsonNode inputs = ruleJson.get("inputs");
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
