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

package com.fibonsai.cryptomeria.xtratej.rules;

import com.fibonsai.cryptomeria.xtratej.event.reactive.Fifo;
import com.fibonsai.cryptomeria.xtratej.event.ITemporalData;
import com.fibonsai.cryptomeria.xtratej.event.series.TimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.impl.BooleanSingleTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.impl.BooleanSingleTimeSeries.BooleanSingle;
import jakarta.annotation.Nonnull;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public abstract class RuleStream {

    private final String name;

    protected final Set<Map.Entry<String, JsonNode>> properties;
    private final Fifo<ITemporalData> results;

    protected boolean allSources = true;
    protected final List<String> sourceIds = new ArrayList<>();

    protected RuleStream(@Nonnull String name, @Nonnull JsonNode properties, @Nonnull Fifo<ITemporalData> results) {
        this.name = name;
        this.properties = properties.properties();
        this.results = results;
    }

    public String name() {
        return name;
    }

    public void subscribe(@Nonnull Fifo<ITemporalData[]> inputs) {
        inputs.subscribe(temporalDatas -> {
            BooleanSingle[] booleanSingles = predicate().apply(temporalDatas);
            BooleanSingleTimeSeries resultSeries = new BooleanSingleTimeSeries(name(), booleanSingles);
            results.emitNext(resultSeries);
        });
    }

    protected void processProperties() {
        for (var e: this.properties) {
            if ("allSources".equals(e.getKey()) && e.getValue().isBoolean()) {
                allSources = e.getValue().asBoolean();
            }
            if (!allSources && "sources".equals(e.getKey()) && e.getValue().isArray()) {
                for (var element: e.getValue()) {
                    if (element.isString()) {
                        sourceIds.add(element.asString());
                    }
                }
            }
        }
    }

    protected abstract Function<ITemporalData[], BooleanSingle[]> predicate();

    protected @Nonnull List<Integer> getSourceIndexes(ITemporalData[] temporalData) {
        List<Integer> sourceIndexes = new ArrayList<>();
        for (int x = 0; x< temporalData.length; x++) {
            if (allSources || (temporalData[x] instanceof TimeSeries series && sourceIds.contains(series.id()))) sourceIndexes.add(x);
        }
        return sourceIndexes;
    }

    public Fifo<ITemporalData> results() {
        return results;
    }

    public List<String> sourceIds() {
        return sourceIds;
    }

    public RuleStream setAllSources(boolean allSources) {
        this.allSources = allSources;
        for (var e: this.properties) {
            if (!allSources && "sources".equals(e.getKey()) && e.getValue().isArray()) {
                for (var element: e.getValue()) {
                    if (element.isString()) {
                        sourceIds.add(element.asString());
                    }
                }
            }
        }
        return this;
    }

    public RuleStream addSourceId(@Nonnull String id) {
        sourceIds.add(id);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RuleStream rule)) return false;
        return name.equals(rule.name());
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
