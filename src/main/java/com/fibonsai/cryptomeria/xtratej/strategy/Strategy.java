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
import com.fibonsai.cryptomeria.xtratej.event.reactive.Fifo;
import com.fibonsai.cryptomeria.xtratej.rules.RuleStream;
import com.fibonsai.cryptomeria.xtratej.rules.RuleType;
import com.fibonsai.cryptomeria.xtratej.sources.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class Strategy implements IStrategy {

    private static final Logger log = LoggerFactory.getLogger(Strategy.class);
    private final String name;
    private final String symbol;
    private final String source;
    private final StrategyType strategyType;

    private RuleStream aggregator = RuleType.False.builder().build();
    private Runnable onSubscribe = () -> {};

    private final AtomicInteger rulesSubscribed = new AtomicInteger(0);

    private final Map<String, Subscriber> sources = new HashMap<>();
    private final Map<String, RuleStream> rules = new HashMap<>();

    public Strategy(String name, String symbol, String source, StrategyType strategyType) {
        this.name = name;
        this.symbol = symbol;
        this.source = source;
        this.strategyType = strategyType;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String symbol() {
        return symbol;
    }

    @Override
    public String source() {
        return source;
    }

    @Override
    public StrategyType strategyType() {
        return strategyType;
    }

    @Override
    public boolean isActivated() {
        return rules.size() == rulesSubscribed.get();
    }

    @Override
    public IStrategy addSource(Subscriber source) {
        if (!isActivated()) {
            sources.put(source.name(), source);
        }
        return this;
    }

    @Override
    public IStrategy addRule(RuleStream rule) {
        if (!isActivated()) {
            rules.put(rule.name(), rule);
        }
        return this;
    }

    @Override
    public IStrategy setAggregatorRule(RuleStream aggregator) {
        if (!isActivated()) {
            this.aggregator = aggregator;
            log.info("{} strategy: Aggregator rule {} registered", name(), aggregator.name());
        }
        return this;
    }

    @Override
    public IStrategy setAggregatorRule(String ruleName) {
        RuleStream rule = rules.get(ruleName);
        if (rule != null) {
            setAggregatorRule(rule);
        } else {
            log.error("{} rule NOT REGISTERED",ruleName);
            aggregator = RuleType.False.builder().build();
        }
        return this;
    }

    @Override
    public IStrategy activeRules() {
        if (!isActivated()) {
            subscribeRules();
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    private void subscribeRules() {
        for (var entry: rules.entrySet()) {
            RuleStream rule = entry.getValue();
            var inputs = new LinkedList<Fifo<ITemporalData>>();
            for (var sourceId: rule.sourceIds()) {
                var source = sources.get(sourceId);
                if (source != null) {
                    inputs.add(source.toFifo());
                }
                var otherRule = rules.get(sourceId);
                if (otherRule != null) {
                    inputs.add(otherRule.results());
                }
            }
            var inputsArray = inputs.<Fifo<ITemporalData>>toArray(Fifo[]::new); // unckecked, but ok
            var inputsZipped = Fifo.zip(inputsArray);
            rule.subscribe(inputsZipped);
            rulesSubscribed.getAndIncrement();
        }
    }

    @Override
    public IStrategy onSubscribe(Runnable onSubscribe) {
        this.onSubscribe = onSubscribe;
        return this;
    }

    @Override
    public IStrategy subscribe(Consumer<ITemporalData> consumer) {
        aggregator.results().onSubscribe(onSubscribe).subscribe(consumer);
        return this;
    }
}
