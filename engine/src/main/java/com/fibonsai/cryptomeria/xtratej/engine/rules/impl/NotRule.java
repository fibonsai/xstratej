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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

public class NotRule extends RuleStream {

    private static final Logger log = LoggerFactory.getLogger(NotRule.class);

    @Override
    protected Function<TimeSeries[], BooleanTimeSeries[]> predicate() {
        return timeSeriesArray -> {
            if (!isActivated()) {
                log.warn("No sources. Ignoring rule.");
                return new BooleanTimeSeries[0];
            }

            if (timeSeriesArray.length > 1) {
                log.error("Multi-timeseries are not supported as a source. Only one.");
                return new BooleanTimeSeries[0];
            }

            var ts = timeSeriesArray[0];
            if (ts instanceof BooleanTimeSeries series) {
                int endIndex = series.size() - 1;
                boolean lastValue = series.values()[endIndex];
                long lastTimestamp = series.timestamp();
                return new BooleanTimeSeries[] { new BooleanTimeSeriesBuilder().add(lastTimestamp, !lastValue).build() };
            }

            return new BooleanTimeSeries[0];
        };
    }
}
