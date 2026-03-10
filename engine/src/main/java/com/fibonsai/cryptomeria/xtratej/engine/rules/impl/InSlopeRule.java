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
import com.fibonsai.cryptomeria.xtratej.event.series.dao.SingleTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.TimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.builders.BooleanTimeSeriesBuilder;
import org.hipparchus.stat.regression.SimpleRegression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;

import java.util.function.Function;

public class InSlopeRule extends RuleStream {

    private static final Logger log = LoggerFactory.getLogger(InSlopeRule.class);

    private final SimpleRegression regression = new SimpleRegression();

    private double minSlope = Double.NaN;
    private double maxSlope = Double.NaN;

    @Override
    public RuleStream setParams(JsonNode params) {
        for (var e: params.properties()) {
            if ("minSlope".equals(e.getKey()) && e.getValue().isDouble()) minSlope = e.getValue().asDouble();
            if ("maxSlope".equals(e.getKey()) && e.getValue().isDouble()) maxSlope = e.getValue().asDouble();
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

            Boolean allresult = null;
            long lastTimestamp = 0;
            for (var timeSeries: timeSeriesArray) {
                if (timeSeries instanceof SingleTimeSeries ts && timeSeries.size() > 0) {
                    double slope = getSlope(ts);

                    lastTimestamp = timeSeries.timestamp();
                    boolean result;
                    if (Double.isNaN(minSlope) && Double.isNaN(maxSlope)) {
                        result = false;
                    } else {
                        result = (Double.isNaN(minSlope) || slope > minSlope) && ((Double.isNaN(maxSlope) || slope < maxSlope));
                    }
                    allresult = allresult == null ? result : allresult && result;
                }
            }
            if (allresult == null) allresult = false;
            return new BooleanTimeSeries[] { new BooleanTimeSeriesBuilder().add(lastTimestamp, allresult).build() };
        };
    }

    public InSlopeRule setMinSlope(double minSlope) {
        this.minSlope = minSlope;
        return this;
    }

    public InSlopeRule setMaxSlope(double maxSlope) {
        this.maxSlope = maxSlope;
        return this;
    }

    private double getSlope(SingleTimeSeries series) {
        regression.clear();
        for (int x = 0; x < series.size(); x++) {
            double doubleTimestamp = series.timestamps()[x];
            double value = series.values()[x];
            regression.addData(doubleTimestamp, value);
        }
        return regression.getSlope();
    }
}
