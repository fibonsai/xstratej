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

import com.fibonsai.cryptomeria.xtratej.event.ITemporalData;
import com.fibonsai.cryptomeria.xtratej.event.reactive.Fifo;
import com.fibonsai.cryptomeria.xtratej.event.series.TimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.impl.BooleanSingleTimeSeries.BooleanSingle;
import com.fibonsai.cryptomeria.xtratej.event.series.impl.EmptyTimeSeries;
import com.fibonsai.cryptomeria.xtratej.rules.RuleStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class LimitRule extends RuleStream {

    private static final Logger log = LoggerFactory.getLogger(LimitRule.class);

    private double min = Double.NEGATIVE_INFINITY;
    private double max = Double.POSITIVE_INFINITY;
    private String upperSourceId = "";
    private String lowerSourceId = "";

    public LimitRule(String name, JsonNode properties) {
        this(name, properties, new Fifo<>());
    }

    public LimitRule(String name, JsonNode properties, Fifo<ITemporalData> results) {
        super(name, properties, results);
        processProperties();
    }

    @Override
    protected void processProperties() {
        super.processProperties();
        for (var e: properties) {
            if ("min".equals(e.getKey()) && e.getValue().isDouble()) min = e.getValue().asDouble();
            if ("max".equals(e.getKey()) && e.getValue().isDouble()) max = e.getValue().asDouble();
            if ("upperSourceId".equals(e.getKey()) && e.getValue().isString()) upperSourceId = e.getValue().asString();
            if ("lowerSourceId".equals(e.getKey()) && e.getValue().isString()) lowerSourceId = e.getValue().asString();
        }
    }

    @Override
    protected Function<ITemporalData[], BooleanSingle[]> predicate() {
        return temporalDatas -> {
            final List<Integer> sourceIndexes = getSourceIndexes(temporalDatas);

            if (sourceIndexes.isEmpty()) {
                log.warn("No sources. Ignoring rule.");
                return new BooleanSingle[0];
            }

            int count = 0;
            boolean result = false;
            long lastTimestamp = 0;

            TimeSeries tsUpper = EmptyTimeSeries.INSTANCE;
            TimeSeries tsLower = EmptyTimeSeries.INSTANCE;
            for (var temporalData : temporalDatas) {
                if (temporalData instanceof TimeSeries timeSeries) {
                    if (Objects.equals(timeSeries.id(), upperSourceId)) tsUpper = timeSeries;
                    if (Objects.equals(timeSeries.id(), lowerSourceId)) tsLower = timeSeries;
                }
            }

            loop1:
            for (var temporalData: temporalDatas) {
                if (temporalData instanceof TimeSeries timeSeries) {
                    if (Objects.equals(timeSeries.id(), tsUpper.id()) || Objects.equals(timeSeries.id(), tsLower.id())) {
                        continue;
                    }
                    if ((allSources || sourceIndexes.contains(count++)) && timeSeries.size() > 0) {
                        lastTimestamp = timeSeries.timestamp();
                        for (int x = timeSeries.size() - 1; x >= 0; x--) {
                            double value = timeSeries.singleDoubleValues()[x];
                            if (tsUpper.size() > 0) {
                                int topIndex = tsUpper.size() - 1 - x;
                                if (topIndex < 0) {
                                    break;
                                }
                                if (tsUpper.singleDoubleValues()[topIndex] < value) {
                                    result = false;
                                    break loop1;
                                }
                            }
                            if (tsLower.size() > 0) {
                                int loweIndex = tsLower.size() - 1 - x;
                                if (loweIndex < 0) {
                                    break;
                                }
                                if (tsLower.singleDoubleValues()[loweIndex] > value) {
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
            return new BooleanSingle[] { new BooleanSingle(lastTimestamp, result) };
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
