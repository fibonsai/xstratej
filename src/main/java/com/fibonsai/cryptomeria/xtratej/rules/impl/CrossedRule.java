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

package com.fibonsai.cryptomeria.xtratej.rules.impl;

import com.fibonsai.cryptomeria.xtratej.event.reactive.Fifo;
import com.fibonsai.cryptomeria.xtratej.event.ITemporalData;
import com.fibonsai.cryptomeria.xtratej.event.series.TimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.impl.BooleanSingleTimeSeries.BooleanSingle;
import com.fibonsai.cryptomeria.xtratej.rules.RuleStream;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class CrossedRule extends RuleStream {

    private static final Logger log = LoggerFactory.getLogger(CrossedRule.class);
    private double threshold = Double.NaN;
    private String sourceId = "";

    public CrossedRule(@Nonnull String name, @Nonnull JsonNode properties) {
        this(name, properties, new Fifo<>());
    }

    public CrossedRule(@Nonnull String name, @Nonnull JsonNode properties, @Nonnull Fifo<ITemporalData> results) {
        super(name, properties, results);
        processProperties();
    }

    @Override
    protected void processProperties() {
        super.processProperties();
        for (var e : properties) {
            if ("threshold".equals(e.getKey())) {
                final JsonNode value = e.getValue();
                threshold = value.isDouble() ? value.asDouble() : (value.isInt() ? value.asInt() : Double.NaN);
            }
            if ("sourceId".equals(e.getKey()) && e.getValue().isString()) {
                sourceId = e.getValue().asString();
            }
        }
    }

    @Override
    protected Function<ITemporalData[], BooleanSingle[]> predicate() {
        return temporalDatas -> {
            final List<Integer> sourceIndexes = getSourceIndexes(temporalDatas);

            TimeSeries timeSeriesComparator = null;
            if (sourceId != null && !sourceId.isBlank()) {
                for (var temporalData : temporalDatas) {
                    if (temporalData instanceof TimeSeries timeSeries && Objects.equals(timeSeries.id(), sourceId)) timeSeriesComparator = timeSeries;
                }
            }

            if (sourceIndexes.isEmpty()) {
                log.warn("No sources. Ignoring rule.");
                return new BooleanSingle[0];
            }

            int count = 0;
            Boolean allresult = null;
            long lastTimestamp = 0;
            for (var temporalData: temporalDatas) {
                if ((allSources || sourceIndexes.contains(count++)) && temporalData instanceof TimeSeries timeSeries && timeSeries.size() > 0) {
                    lastTimestamp = temporalData.timestamp();
                    boolean result;
                    if (!Double.isNaN(threshold)) {
                        result = isCrossed(timeSeries);
                    } else if (sourceIndexes.size() > 1 && timeSeriesComparator != null) {
                        if (Objects.equals(timeSeriesComparator.id(), timeSeries.id())) {
                            continue;
                        }
                        result = isCrossed(timeSeriesComparator, timeSeries);
                    } else {
                        result = false;
                    }
                    allresult = allresult == null ? result : allresult && result;
                }
            }
            if (allresult == null) allresult = false;
            return new BooleanSingle[] { new BooleanSingle(lastTimestamp, allresult) };
        };
    }

    public CrossedRule setThreshold(double threshold) {
        this.threshold = threshold;
        return this;
    }

    public CrossedRule setSourceId(@Nonnull String sourceId) {
        this.sourceId = sourceId;
        return this;
    }

    private boolean isCrossed(TimeSeries series) {
        if (threshold > Double.NEGATIVE_INFINITY) {
            double[] minmax = series.minmax();
            double min = minmax[0];
            double max = minmax[1];
            return min < threshold && max > threshold;
        }
        return false;
    }

    private boolean isCrossed(TimeSeries series1, TimeSeries series2) {
        double[] minmax1 = series1.minmax();
        double min1 = minmax1[0];
        double max1 = minmax1[1];
        double[] minmax2 = series2.minmax();
        double min2 = minmax2[0];
        double max2 = minmax2[1];
        return (min1 > min2 && max1 < max2) || (min1 < min2 && max1 > max2);
    }
}
