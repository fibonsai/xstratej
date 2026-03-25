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

package com.fibonsai.xtratej.engine.strategy;

import com.fibonsai.xtratej.adaptor.core.Subscriber;
import com.fibonsai.xtratej.engine.adapters.SourceType;
import com.fibonsai.xtratej.engine.rules.RuleStream;
import com.fibonsai.xtratej.event.series.dao.TimeSeries;
import tools.jackson.databind.JsonNode;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public interface IStrategy {

    default boolean isActivated() { return false; }

    enum StrategyType {
        ENTER,
        EXIT,
        UNDEF
        ;

        public static StrategyType fromName(String name) {
            for (var value: values()) {
                if (value.name().equalsIgnoreCase(name)) {
                    return value;
                }
            }
            return UNDEF;
        }
    }

    IStrategy addSource(Subscriber source);

    IStrategy addSource(SourceType sourceType, String name, String publisher);

    IStrategy addSource(SourceType sourceType, String name, String published, JsonNode params);

    IStrategy setAggregatorRule(RuleStream<? extends TimeSeries> aggregator);

    String name();

    String symbol();

    Set<String> publishers();

    StrategyType strategyType();

    IStrategy onSubscribe(Runnable onSubscribe);

    IStrategy subscribe(Consumer<TimeSeries> consumer);

    Map<String, Subscriber> getSources();
}
