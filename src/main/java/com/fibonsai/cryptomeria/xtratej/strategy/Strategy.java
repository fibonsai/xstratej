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

import com.fibonsai.cryptomeria.xtratej.event.ITemporalData;
import com.fibonsai.cryptomeria.xtratej.event.reactive.Fifo;
import com.fibonsai.cryptomeria.xtratej.rules.RuleStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class Strategy implements IStrategy {

    private static final Logger log = LoggerFactory.getLogger(Strategy.class);
    private final String name;
    private final String symbol;
    private final String source;
    private final StrategyType strategyType;

    private final List<Fifo<ITemporalData>> indicators = new ArrayList<>();
    private final AtomicInteger indicatorsZippedsCounter = new AtomicInteger(0);
    private final AtomicInteger logicsZippedsCounter = new AtomicInteger(0);
    private final Map<String, RuleStream> indicatorRules = new HashMap<>();
    private final Map<String, RuleStream> logicRules = new HashMap<>();
    private Fifo<ITemporalData> aggregatedResults = new Fifo<>();
    private Runnable onSubscribe = () -> {};

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
        return indicatorRules.size() == indicatorsZippedsCounter.get() && logicRules.size() == logicsZippedsCounter.get();
    }

    @Override
    public IStrategy addIndicator(Fifo<ITemporalData> indicatorTimeseries) {
        indicators.add(indicatorTimeseries);
        return this;
    }

    @Override
    public IStrategy setAggregatorRule(String ruleName) {
        try {
            aggregatedResults = Optional.ofNullable(logicRules.get(ruleName))
                    .map(RuleStream::results)
                    .orElseThrow(() -> new RuntimeException("%s logic rule NOT REGISTERED".formatted(ruleName)));

            log.info("{} strategy: Aggregator rule {} registered", name(), ruleName);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return this;
    }

    @Override
    public IStrategy addIndicatorRule(RuleStream rule) {
        indicatorRules.put(rule.name(), rule);
        return this;
    }

    @Override
    public IStrategy addLogicRule(RuleStream rule) {
        logicRules.put(rule.name(), rule);
        return this;
    }

    @Override
    public IStrategy activeRules() {
        if (!isActivated()) {
            subscribeIndicators();
            subscribeLogicRules();
        }
        return this;
    }

    private void subscribeLogicRules() {
        for (var entry : logicRules.entrySet()) {
            RuleStream rule = entry.getValue();
            List<String> sourceIds = rule.sourceIds();
            List<Fifo<ITemporalData>> inputs = new ArrayList<>();
            for (var indicatorRuleEntry : indicatorRules.entrySet()) {
                RuleStream level0Rule = indicatorRuleEntry.getValue();
                if (sourceIds.contains(level0Rule.name())) {
                    inputs.add(level0Rule.results());
                }
            }
            for (var logicRuleEntry : logicRules.entrySet()) {
                RuleStream level1Rule = logicRuleEntry.getValue();
                if (sourceIds.contains(level1Rule.name())) {
                    inputs.add(level1Rule.results());
                }
            }
            var inputsArray = inputs.<Fifo<ITemporalData>>toArray(Fifo[]::new);
            var zipped = Fifo.zip(inputsArray);
            rule.subscribe(zipped);
            logicsZippedsCounter.getAndIncrement();
            log.info("Logic Rule {} activated", rule.name());
        }
    }

    private void subscribeIndicators() {
        for (var entry : indicatorRules.entrySet()) {
            RuleStream rule = entry.getValue();
            var indicatorsArray = indicators.<Fifo<ITemporalData>>toArray(Fifo[]::new);
            var zipped = Fifo.zip(indicatorsArray);
            rule.subscribe(zipped);
            indicatorsZippedsCounter.getAndIncrement();
            log.info("Indicator Rule {} activated", rule.name());
        }
    }

    @Override
    public IStrategy onSubscribe(Runnable onSubscribe) {
        this.onSubscribe = onSubscribe;
        return this;
    }

    @Override
    public IStrategy subscribe(Consumer<ITemporalData> consumer) {
        aggregatedResults.onSubscribe(onSubscribe).subscribe(consumer);
        return this;
    }
}
