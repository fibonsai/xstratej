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

package com.fibonsai.cryptomeria.xtratej.engine.rules;

import com.fibonsai.cryptomeria.xtratej.engine.sources.Subscriber;
import com.fibonsai.cryptomeria.xtratej.event.reactive.DirectFlux;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.*;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.builders.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public abstract class RuleStream<T extends TimeSeries> {

    private static final Logger log = LoggerFactory.getLogger(RuleStream.class);

    private final DirectFlux<TimeSeries> results = new DirectFlux<>();
    private final AtomicBoolean activated = new AtomicBoolean(false);

    private String description = "";

    public RuleStream<T> setParams(JsonNode params) {
        return this;
    }

    public void watch(DirectFlux<TimeSeries[]> inputs) {
        inputs.onSubscribe(() -> activated.set(true)).subscribe(inputTimeSeriesArray -> {
            try {
                T[] resultTimeSeriesArray = predicate().apply(inputTimeSeriesArray);
                if (resultTimeSeriesArray.length == 0) {
                    results.emitNext(EmptyTimeSeries.INSTANCE);
                } else if (resultTimeSeriesArray.length == 1) {
                    results.emitNext(resultTimeSeriesArray[0]);
                } else {
                    var ids = new LinkedHashSet<String>();
                    List.of(resultTimeSeriesArray).forEach(ts -> ids.add(ts.id()));
                    String newId = String.join("#", ids);
                    var builder = switch (resultTimeSeriesArray[0]) {
                        case BooleanTimeSeries _ -> new BooleanTimeSeriesBuilder();
                        case DoubleTimeSeries _ -> new DoubleTimeSeriesBuilder();
                        case Double2TimeSeries _ -> new Double2TimeSeriesBuilder();
                        case BarTimeSeries _ -> new BarTimeSeriesBuilder();
                        case BandTimeSeries _ -> new BandTimeSeriesBuilder();
                        default -> throw new UnsupportedOperationException("not supported");
                    };
                    TimeSeries merged = builder.setId(newId).merge(resultTimeSeriesArray).build();
                    results.emitNext(merged);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                results.emitError(e);
            }
        });
    }

    public void watch(Subscriber... subscribers) {
        DirectFlux<TimeSeries>[] arrayOfFluxes = DirectFlux.createArray(subscribers.length);
        int count = 0;
        for (var subscribe: subscribers) {
            arrayOfFluxes[count++] = subscribe.toDirectFlux();
        }
        DirectFlux<TimeSeries[]> inputs = DirectFlux.zip(arrayOfFluxes);
        watch(inputs);
    }

    @SafeVarargs
    public final void watch(RuleStream<T>... rules) {
        DirectFlux<TimeSeries>[] arrayOfFluxes = DirectFlux.createArray(rules.length);
        int count = 0;
        for (var rule: rules) {
            arrayOfFluxes[count++] = rule.results();
        }
        DirectFlux<TimeSeries[]> inputs = DirectFlux.zip(arrayOfFluxes);
        watch(inputs);
    }

    public boolean isActivated() {
        return activated.get();
    }

    protected abstract Function<TimeSeries[], T[]> predicate();

    public DirectFlux<TimeSeries> results() {
        return results;
    }

    public String getDescription() {
        return description;
    }

    public RuleStream<T> setDescription(String description) {
        this.description = description;
        return this;
    }
}
