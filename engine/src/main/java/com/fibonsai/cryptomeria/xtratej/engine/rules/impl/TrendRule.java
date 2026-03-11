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
import com.fibonsai.cryptomeria.xtratej.event.series.dao.DoubleTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.EmptyTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.TimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.builders.BooleanTimeSeriesBuilder;
import org.hipparchus.stat.regression.SimpleRegression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;

import java.util.Objects;
import java.util.function.Function;

public class TrendRule extends RuleStream<BooleanTimeSeries> {

    private static final Logger log = LoggerFactory.getLogger(TrendRule.class);
    private final SimpleRegression regression = new SimpleRegression();

    private String sourceId = "";
    private boolean isRising = true;

    @Override
    public RuleStream<BooleanTimeSeries> setParams(JsonNode params) {
        for (var e : params.properties()) {
            if ("sourceId".equals(e.getKey()) && e.getValue().isString()) {
                sourceId = e.getValue().asString();
            }
            if ("isRising".equals(e.getKey()) && e.getValue().isBoolean()) {
              isRising = e.getValue().asBoolean();
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

            TimeSeries timeSeriesComparator = EmptyTimeSeries.INSTANCE;
            if (!sourceId.isBlank()) {
                for (var timeSeries : timeSeriesArray) {
                    if (Objects.equals(timeSeries.id(), sourceId)) timeSeriesComparator = timeSeries;
                }
            }

            double slopeComparable = (timeSeriesComparator != EmptyTimeSeries.INSTANCE) && timeSeriesComparator instanceof DoubleTimeSeries ts ? getSlope(ts) : 0.0D;

            Boolean allresult = null;
            long lastTimestamp = 0;
            for (var timeSeries: timeSeriesArray) {
                if (timeSeries instanceof DoubleTimeSeries doubleTimeSeries && doubleTimeSeries.size() > 0) {
                    if (Objects.equals(timeSeriesComparator.id(), doubleTimeSeries.id())) {
                        continue;
                    }
                    double slope = getSlope(doubleTimeSeries);
                    lastTimestamp = doubleTimeSeries.timestamp();

                    boolean result = isRising ? slope > slopeComparable : slope < slopeComparable;
                    allresult = allresult == null ? result : allresult && result;
                }
            }
            if (allresult == null) allresult = false;
            return new BooleanTimeSeries[] { new BooleanTimeSeriesBuilder().add(lastTimestamp, allresult).build() };
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

    private double getSlope(DoubleTimeSeries series) {
        regression.clear();
        for (int x = 0; x < series.size(); x++) {
            double doubleTimestamp = series.timestamps()[x];
            double value = series.values()[x];
            regression.addData(doubleTimestamp, value);
        }
        return regression.getSlope();
    }
}
