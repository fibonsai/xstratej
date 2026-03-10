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

package com.fibonsai.cryptomeria.xtratej.engine.rules.impl;

import com.fibonsai.cryptomeria.xtratej.engine.rules.RuleStream;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.BooleanTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.TimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.builders.BooleanTimeSeriesBuilder;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.tools.MinMax;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;

import java.util.Objects;
import java.util.function.Function;

public class CrossedRule extends RuleStream {

    private static final Logger log = LoggerFactory.getLogger(CrossedRule.class);

    private double threshold = Double.NaN;
    private String sourceId = "";

    @Override
    public RuleStream setParams(JsonNode params) {
        for (var e : params.properties()) {
            if ("threshold".equals(e.getKey())) {
                final JsonNode value = e.getValue();
                threshold = value.isDouble() ? value.asDouble() : (value.isInt() ? value.asInt() : Double.NaN);
            }
            if ("sourceId".equals(e.getKey()) && e.getValue().isString()) {
                sourceId = e.getValue().asString();
            }
        }
        return this;
    }

    @Override
    protected Function<TimeSeries[], BooleanTimeSeries[]> predicate() {
        return timeSeriesArray -> {
            if (!isActivated()) {
                log.warn("No sources. Ignoring rule.");
                return new BooleanTimeSeries[0];
            }

            TimeSeries timeSeriesComparator = null;
            if (!sourceId.isBlank()) {
                for (var timeSeries : timeSeriesArray) {
                    if (Objects.equals(timeSeries.id(), sourceId)) {
                        timeSeriesComparator = timeSeries;
                        break;
                    }
                }
            }

            Boolean allresult = null;
            long lastTimestamp = 0;
            for (var timeSeries: timeSeriesArray) {
                if (timeSeries.size() > 0) {
                    lastTimestamp = timeSeries.timestamp();
                    boolean result;
                    if (!Double.isNaN(threshold)) {
                        result = isCrossed(timeSeries);
                    } else if (timeSeriesComparator != null && timeSeriesArray.length > 1) {
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
            return new BooleanTimeSeries[] { new BooleanTimeSeriesBuilder().add(lastTimestamp, allresult).build() };
        };
    }

    public CrossedRule setThreshold(double threshold) {
        this.threshold = threshold;
        return this;
    }

    public CrossedRule setSourceId(String sourceId) {
        this.sourceId = sourceId;
        return this;
    }

    private boolean isCrossed(TimeSeries series) {
        if (threshold > Double.NEGATIVE_INFINITY) {
            MinMax.MinMaxResult minMaxResult = MinMax.from(series);
            double min = minMaxResult.min();
            double max = minMaxResult.max();
            return min < threshold && max > threshold;
        }
        return false;
    }

    private boolean isCrossed(TimeSeries series1, TimeSeries series2) {
        MinMax.MinMaxResult minMaxResult1 = MinMax.from(series1);
        MinMax.MinMaxResult minMaxResult2 = MinMax.from(series2);
        double min1 = minMaxResult1.min();
        double max1 = minMaxResult1.max();
        double min2 = minMaxResult2.min();
        double max2 = minMaxResult2.max();
        return (min1 > min2 && max1 < max2) || (min1 < min2 && max1 > max2);
    }
}
