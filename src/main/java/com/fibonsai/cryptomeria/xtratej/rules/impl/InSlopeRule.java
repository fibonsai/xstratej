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
import com.fibonsai.cryptomeria.xtratej.rules.RuleStream;
import org.hipparchus.stat.regression.SimpleRegression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.function.Function;

public class InSlopeRule extends RuleStream {

    private static final Logger log = LoggerFactory.getLogger(InSlopeRule.class);
    private final SimpleRegression regression = new SimpleRegression();

    private double minSlope = Double.NaN;
    private double maxSlope = Double.NaN;

    public InSlopeRule(String name, JsonNode properties) {
        this(name, properties, new Fifo<>());
    }

    public InSlopeRule(String name, JsonNode properties, Fifo<ITemporalData> results) {
        super(name, properties, results);
        processProperties();
    }

    @Override
    protected void processProperties() {
        super.processProperties();
        for (var e: properties) {
            if ("minSlope".equals(e.getKey()) && e.getValue().isDouble()) minSlope = e.getValue().asDouble();
            if ("maxSlope".equals(e.getKey()) && e.getValue().isDouble()) maxSlope = e.getValue().asDouble();
        }
    }

    @Override
    protected Function<ITemporalData[], BooleanSingle[]> predicate() {
        return temporalDatas -> {
            List<Integer> sourceIndexes = getSourceIndexes(temporalDatas);
            if (sourceIndexes.isEmpty()) {
                log.warn("No sources. Ignoring rule.");
                return new BooleanSingle[0];
            }

            int count = 0;
            Boolean allresult = null;
            long lastTimestamp = 0;
            for (var temporalData: temporalDatas) {
                if ((allSources || sourceIndexes.contains(count++)) && temporalData instanceof TimeSeries timeSeries && timeSeries.size() > 0) {
                    double slope = getSlope(timeSeries);

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
            return new BooleanSingle[] { new BooleanSingle(lastTimestamp, allresult) };
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
