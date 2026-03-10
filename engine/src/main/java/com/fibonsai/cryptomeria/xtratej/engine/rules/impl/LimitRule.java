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
import com.fibonsai.cryptomeria.xtratej.event.series.dao.EmptyTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.SingleTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.TimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.builders.BooleanTimeSeriesBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;

import java.util.Objects;
import java.util.function.Function;

public class LimitRule extends RuleStream {

    private static final Logger log = LoggerFactory.getLogger(LimitRule.class);

    private double min = Double.NEGATIVE_INFINITY;
    private double max = Double.POSITIVE_INFINITY;
    private String upperSourceId = "";
    private String lowerSourceId = "";

    @Override
    public RuleStream setParams(JsonNode params) {
        for (var e: params.properties()) {
            if ("min".equals(e.getKey()) && e.getValue().isDouble()) min = e.getValue().asDouble();
            if ("max".equals(e.getKey()) && e.getValue().isDouble()) max = e.getValue().asDouble();
            if ("upperSourceId".equals(e.getKey()) && e.getValue().isString()) upperSourceId = e.getValue().asString();
            if ("lowerSourceId".equals(e.getKey()) && e.getValue().isString()) lowerSourceId = e.getValue().asString();
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

            boolean result = false;
            long lastTimestamp = 0;

            TimeSeries tsUpper = EmptyTimeSeries.INSTANCE;
            TimeSeries tsLower = EmptyTimeSeries.INSTANCE;
            for (var timeSeries : timeSeriesArray) {
                if (timeSeries instanceof SingleTimeSeries && Objects.equals(timeSeries.id(), upperSourceId)) tsUpper = timeSeries;
                if (timeSeries instanceof SingleTimeSeries && Objects.equals(timeSeries.id(), lowerSourceId)) tsLower = timeSeries;
            }

            loop1:
            for (var timeSeries: timeSeriesArray) {
                if (timeSeries instanceof SingleTimeSeries singleTimeSeries) {
                    if (Objects.equals(singleTimeSeries.id(), tsUpper.id()) || Objects.equals(singleTimeSeries.id(), tsLower.id())) {
                        continue;
                    }
                    if (singleTimeSeries.size() > 0) {
                        lastTimestamp = singleTimeSeries.timestamp();
                        for (int x = singleTimeSeries.size() - 1; x >= 0; x--) {
                            double value = singleTimeSeries.values()[x];
                            if (tsUpper.size() > 0) {
                                int topIndex = tsUpper.size() - 1 - x;
                                if (topIndex < 0) {
                                    break;
                                }
                                if (((SingleTimeSeries)tsUpper).values()[topIndex] < value) {
                                    result = false;
                                    break loop1;
                                }
                            }
                            if (tsLower.size() > 0) {
                                int loweIndex = tsLower.size() - 1 - x;
                                if (loweIndex < 0) {
                                    break;
                                }
                                if (((SingleTimeSeries)tsLower).values()[loweIndex] > value) {
                                    result = false;
                                    break loop1;
                                }
                            }
                            if (value < min || value > max) {
                                result = false;
                                break loop1;
                            }
                        }
                        result = true;
                    }
                }
            }
            return new BooleanTimeSeries[] { new BooleanTimeSeriesBuilder().add(lastTimestamp, result).build() };
        };
    }

    public LimitRule setMin(double min) {
        this.min = min;
        return this;
    }

    public LimitRule setMax(double max) {
        this.max = max;
        return this;
    }

    public LimitRule setUpperSourceId(String upperSourceId) {
        this.upperSourceId = upperSourceId;
        return this;
    }

    public LimitRule setLowerSourceId(String lowerSourceId) {
        this.lowerSourceId = lowerSourceId;
        return this;
    }
}
