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
import org.hipparchus.stat.regression.SimpleRegression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class TrendRule extends RuleStream {

    private static final Logger log = LoggerFactory.getLogger(TrendRule.class);
    private final SimpleRegression regression = new SimpleRegression();

    private String sourceId = "";
    private boolean isRising = true;

    public TrendRule(String name, JsonNode properties) {
        this(name, properties, new Fifo<>());
    }

    public TrendRule(String name, JsonNode properties, Fifo<ITemporalData> results) {
        super(name, properties, results);
        processProperties();
    }

    @Override
    protected void processProperties() {
        super.processProperties();
        for (var e : properties) {
            if ("sourceId".equals(e.getKey()) && e.getValue().isString()) {
                sourceId = e.getValue().asString();
            }
            if ("isRising".equals(e.getKey()) && e.getValue().isBoolean()) {
              isRising = e.getValue().asBoolean();
            }
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

            TimeSeries timeSeriesComparator = EmptyTimeSeries.INSTANCE;
            if (!sourceId.isBlank()) {
                for (var temporalData : temporalDatas) {
                    if (temporalData instanceof TimeSeries timeSeries && Objects.equals(timeSeries.id(), sourceId)) timeSeriesComparator = timeSeries;
                }
            }

            double slopeComparable = (sourceIndexes.size() > 1 && timeSeriesComparator != EmptyTimeSeries.INSTANCE) ? getSlope(timeSeriesComparator) : 0.0D;

            int count = 0;
            Boolean allresult = null;
            long lastTimestamp = 0;
            for (var temporalData: temporalDatas) {
                if ((allSources || sourceIndexes.contains(count++)) && temporalData instanceof TimeSeries timeSeries && timeSeries.size() > 0) {
                    if (Objects.equals(timeSeriesComparator.id(), timeSeries.id())) {
                        continue;
                    }
                    double slope = getSlope(timeSeries);
                    lastTimestamp = timeSeries.timestamp();

                    boolean result = isRising ? slope > slopeComparable : slope < slopeComparable;
                    allresult = allresult == null ? result : allresult && result;
                }
            }
            if (allresult == null) allresult = false;
            return new BooleanSingle[] { new BooleanSingle(lastTimestamp, allresult) };
        };
    }

    public TrendRule setSourceId(String sourceId) {
        this.sourceId = sourceId;
        return this;
    }

    public TrendRule setRising(boolean rising) {
        isRising = rising;
        return this;
    }

    private double getSlope(TimeSeries series) {
        regression.clear();
        for (int x = 0; x < series.size(); x++) {
            double doubleTimestamp = series.timestamps()[x];
            double value = series.singleDoubleValues()[x];
            regression.addData(doubleTimestamp, value);
        }
        return regression.getSlope();
    }
}
