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
import com.fibonsai.cryptomeria.xtratej.event.series.impl.BooleanSingleTimeSeries.BooleanSingle;
import com.fibonsai.cryptomeria.xtratej.rules.RuleStream;
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

    public WeekdayRule(String name, JsonNode properties) {
        this(name, properties, new Fifo<>());
    }

    public WeekdayRule(String name, JsonNode properties, Fifo<ITemporalData> results) {
        super(name, properties, results);
        processProperties();
    }

    @Override
    protected void processProperties() {
        super.processProperties();
        for (var e: properties) {
            if ("weekdays".equals(e.getKey()) && e.getValue().isArray()) {
                for (var element: e.getValue()) {
                    if (element.isString()) {
                        weekdays.add(element.asString().toLowerCase());
                    }
                }
            }
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

            String dayOfWeek = LocalDateTime.now().getDayOfWeek().name().toLowerCase();
            boolean result = weekdays.isEmpty() || weekdays.contains(dayOfWeek);

            long timestamp = 0L;
            int count = 0;
            for (var temporalData: temporalDatas) {
                if ((allSources || sourceIndexes.contains(count++)) && temporalData.timestamp() > timestamp) {
                    timestamp = temporalData.timestamp();
                }
            }

            return new BooleanSingle[] { new BooleanSingle(timestamp, result) };
        };
    }

    public WeekdayRule addWeekday(String weekday) {
        weekdays.add(weekday);
        return this;
    }
}
