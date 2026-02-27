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

import com.fibonsai.cryptomeria.xtratej.event.ITemporalData;
import com.fibonsai.cryptomeria.xtratej.rules.RuleStream;
import com.fibonsai.cryptomeria.xtratej.sources.Subscriber;

import java.util.Collection;
import java.util.function.Consumer;

public interface IStrategy {

    default boolean isActivated() { return false; }

    Collection<Subscriber> getSources();

    enum StrategyType {
        ENTER,
        EXIT,
        UNDEF
        ;

        public static StrategyType fromName(String name) {
            for (var value: values()) {
                if (value.name().equals(name)) {
                    return value;
                }
            }
            return UNDEF;
        }
    }

    IStrategy addSource(Subscriber source);

    IStrategy addRule(RuleStream rule);

    IStrategy setAggregatorRule(RuleStream aggregator);

    IStrategy setAggregatorRule(String ruleName);

    IStrategy activeRules();

    String name();

    String symbol();

    String source();

    StrategyType strategyType();

    IStrategy onSubscribe(Runnable onSubscribe);

    IStrategy subscribe(Consumer<ITemporalData> consumer);
}
