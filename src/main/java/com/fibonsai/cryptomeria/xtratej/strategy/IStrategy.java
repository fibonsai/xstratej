/*
 *  Copyright (c) 2025 fibonsai.com
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

import com.fibonsai.cryptomeria.xtratej.event.reactive.Fifo;
import com.fibonsai.cryptomeria.xtratej.event.ITemporalData;
import com.fibonsai.cryptomeria.xtratej.rules.RuleStream;

import java.util.function.Consumer;

public interface IStrategy {

    default boolean isActivated() { return false; }

    enum StrategyType {
        ENTER,
        EXIT
    }

    IStrategy addIndicator(Fifo<ITemporalData> indicatorTimeseries);

    IStrategy setAggregatorRule(String ruleName);

    IStrategy addIndicatorRule(RuleStream rule);

    IStrategy addLogicRule(RuleStream rule);

    IStrategy activeRules();

    String name();

    String symbol();

    String source();

    StrategyType strategyType();

    IStrategy subscribe(Consumer<ITemporalData> consumer, Runnable onSubscribe);

    default IStrategy subscribe(Consumer<ITemporalData> consumer) {
        return subscribe(consumer, () -> {});
    }
}
