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
import com.fibonsai.cryptomeria.xtratej.engine.rules.impl.FalseRule;
import com.fibonsai.cryptomeria.xtratej.engine.sources.SourceType;
import com.fibonsai.cryptomeria.xtratej.engine.sources.Subscriber;
import com.fibonsai.cryptomeria.xtratej.engine.sources.WithParams;
import com.fibonsai.cryptomeria.xtratej.event.ITemporalData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Strategy implements IStrategy {

    private static final Logger log = LoggerFactory.getLogger(Strategy.class);

    private final String name;
    private final String symbol;
    private final StrategyType strategyType;

    private RuleStream aggregator = RuleType.False.build();
    private Runnable onSubscribe = () -> {};

    private final Map<String, Subscriber> sources = new HashMap<>();

    public Strategy(String name, String symbol, StrategyType strategyType) {
        this.name = name;
        this.symbol = symbol;
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
    public Set<String> publishers() {
        return sources.values().stream().map(Subscriber::publisher).collect(Collectors.toSet());
    }

    @Override
    public StrategyType strategyType() {
        return strategyType;
    }

    @Override
    public boolean isActivated() {
        return ! (aggregator instanceof FalseRule);
    }

    @Override
    public IStrategy addSource(Subscriber source) {
        if (!isActivated()) {
            sources.put(source.name(), source);
        }
        return this;
    }

    @Override
    public IStrategy addSource(SourceType sourceType, String name, String publisher) {
        return addSource(sourceType, name, publisher, Loader.EMPTY_PARAMS);
    }

    @Override
    public IStrategy addSource(SourceType sourceType, String name, String publisher, JsonNode params) {
        if (!isActivated()) {
            Subscriber subscriber = sourceType.builder().setName(name).setPublisher(publisher).build();
            if (subscriber instanceof WithParams subscribeWithParams) {
                subscribeWithParams.setParams(params);
            }
            addSource(subscriber);
        }
        return this;
    }

    @Override
    public IStrategy setAggregatorRule(RuleStream aggregator) {
        if (!isActivated()) {
            this.aggregator = aggregator;
            log.info("{} strategy: Aggregator rule {} registered", name(), aggregator.getDescription());
        }
        return this;
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

    @Override
    public Map<String, Subscriber> getSources() {
        return sources;
    }
}
