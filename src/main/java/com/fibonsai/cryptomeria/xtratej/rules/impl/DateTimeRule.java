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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

public class DateTimeRule extends RuleStream {

    private static final Logger log = LoggerFactory.getLogger(DateTimeRule.class);

    private String begin = "";
    private String end = "";
    private String datetimeFormat = "";

    public DateTimeRule(String name, JsonNode properties) {
        this(name, properties, new Fifo<>());
    }

    public DateTimeRule(String name, JsonNode properties, Fifo<ITemporalData> results) {
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
            if ("datetimeFormat".equals(e.getKey()) && e.getValue().isString()) {
                datetimeFormat = e.getValue().asString();
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
            if (datetimeFormat.isBlank()) {
                formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            } else {
                formatter = DateTimeFormatter.ofPattern(datetimeFormat);
            }
            LocalDateTime dateTimeBegin = begin.isBlank() ? LocalDateTime.MIN : LocalDateTime.parse(begin, formatter);
            LocalDateTime dateTimeEnd = end.isBlank() ? LocalDateTime.MAX : LocalDateTime.parse(end, formatter);
            LocalDateTime now = LocalDateTime.now();

            boolean result = dateTimeEnd.isAfter(dateTimeBegin) ?
                    now.isAfter(dateTimeBegin) && now.isBefore(dateTimeEnd) :
                    now.isBefore(dateTimeBegin) || now.isAfter(dateTimeEnd);

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

    public DateTimeRule setBegin(String begin) {
        this.begin = begin;
        return this;
    }

    public DateTimeRule setEnd(String end) {
        this.end = end;
        return this;
    }

    public DateTimeRule setDatetimeFormat(String datetimeFormat) {
        this.datetimeFormat = datetimeFormat;
        return this;
    }
}
