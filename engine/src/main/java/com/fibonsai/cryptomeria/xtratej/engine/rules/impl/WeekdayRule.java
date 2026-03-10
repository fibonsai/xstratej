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
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class WeekdayRule extends RuleStream {

    private static final Logger log = LoggerFactory.getLogger(WeekdayRule.class);

    private final List<String> weekdays = new ArrayList<>();

    @Override
    public RuleStream setParams(JsonNode params) {
        for (var e: params.properties()) {
            if ("weekdays".equals(e.getKey()) && e.getValue().isArray()) {
                for (var element: e.getValue()) {
                    if (element.isString()) {
                        weekdays.add(element.asString().toLowerCase());
                    }
                }
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

            String dayOfWeek = LocalDateTime.now().getDayOfWeek().name().toLowerCase();
            boolean result = weekdays.isEmpty() || weekdays.contains(dayOfWeek);

            long timestamp = 0L;
            for (var timeSeries: timeSeriesArray) {
                if (timeSeries.timestamp() > timestamp) {
                    timestamp = timeSeries.timestamp();
                }
            }

            return new BooleanTimeSeries[] { new BooleanTimeSeriesBuilder().add(timestamp, result).build() };
        };
    }

    public WeekdayRule addWeekday(String weekday) {
        weekdays.add(weekday);
        return this;
    }
}
