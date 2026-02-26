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

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

public class TimeRule extends RuleStream {

    private static final Logger log = LoggerFactory.getLogger(TimeRule.class);

    private String begin = "";
    private String end = "";
    private String timeFormat = "";
    private boolean invert = false;

    public TimeRule(String name, JsonNode properties) {
        this(name, properties, new Fifo<>());
    }

    public TimeRule(String name, JsonNode properties, Fifo<ITemporalData> results) {
        super(name, properties, results);
        processProperties();
    }

    @Override
    protected void processProperties() {
        super.processProperties();
        for (var e: properties) {
            if ("begin".equals(e.getKey()) && e.getValue().isString()) {
                begin = e.getValue().asString();
            }
            if ("end".equals(e.getKey()) && e.getValue().isString()) {
                end = e.getValue().asString();
            }
            if ("timeFormat".equals(e.getKey()) && e.getValue().isString()) {
                timeFormat = e.getValue().asString();
            }
            if ("invert".equals(e.getKey()) && e.getValue().isBoolean()) {
                invert = e.getValue().asBoolean();
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

            DateTimeFormatter formatter;
            if (timeFormat.isBlank()) {
                formatter = DateTimeFormatter.ISO_LOCAL_TIME;
            } else {
                formatter = DateTimeFormatter.ofPattern(timeFormat);
            }
            boolean result = isOnTime(formatter);

            long timestamp = 0L;
            int count = 0;
            for (var ts: temporalDatas) {
                if ((allSources || sourceIndexes.contains(count++)) && ts.timestamp() > timestamp) {
                    timestamp = ts.timestamp();
                }
            }

            return new BooleanSingle[] { new BooleanSingle(timestamp, result) };
        };
    }

    private boolean isOnTime(DateTimeFormatter formatter) {
        LocalTime tempTimeBegin = begin.isBlank() ? LocalTime.MIN : LocalTime.parse(begin, formatter);
        LocalTime tempTimeEnd = end.isBlank() ? LocalTime.MAX : LocalTime.parse(end, formatter);
        LocalTime now = LocalTime.now();

        LocalTime timeBegin = tempTimeBegin.isAfter(tempTimeEnd) && !invert ? LocalTime.MIN : tempTimeBegin;
        LocalTime timeEnd = tempTimeEnd.isBefore(timeBegin) && !invert ? LocalTime.MAX : tempTimeEnd;

        boolean result;
        if (timeBegin.isBefore(timeEnd)) {
            result = now.isAfter(timeBegin) && now.isBefore(timeEnd);
        } else {
            result = now.isBefore(timeBegin) || now.isAfter(timeEnd);
        }
        return result;
    }

    public TimeRule setBegin(String begin) {
        this.begin = begin;
        return this;
    }

    public TimeRule setEnd(String end) {
        this.end = end;
        return this;
    }

    public TimeRule setTimeFormat(String timeFormat) {
        this.timeFormat = timeFormat;
        return this;
    }

    public TimeRule setInvert(boolean invert) {
        this.invert = invert;
        return this;
    }
}
