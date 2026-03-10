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

import com.fibonsai.cryptomeria.xtratej.event.reactive.Fifo;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.BooleanTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.TimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.builders.BooleanTimeSeriesBuilder;
import tools.jackson.databind.JsonNode;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public abstract class RuleStream {

    private final Fifo<TimeSeries> results = new Fifo<>();
    private final AtomicBoolean activated = new AtomicBoolean(false);

    private String description = "";

    public RuleStream setParams(JsonNode params) {
        return this;
    }

    public void watch(Fifo<TimeSeries[]> inputs) {
        inputs.onSubscribe(() -> activated.set(true)).subscribe(timeSeriesArray -> {
            BooleanTimeSeries[] booleanTimeSeries = predicate().apply(timeSeriesArray);
            BooleanTimeSeriesBuilder builder = new BooleanTimeSeriesBuilder().setId(toString());
            BooleanTimeSeries resultSeries = builder.merge(booleanTimeSeries).build();
            results.emitNext(resultSeries);
        });
    }

    public boolean isActivated() {
        return activated.get();
    }

    protected abstract Function<TimeSeries[], BooleanTimeSeries[]> predicate();

    public Fifo<TimeSeries> results() {
        return results;
    }

    public String getDescription() {
        return description;
    }

    public RuleStream setDescription(String description) {
        this.description = description;
        return this;
    }
}
