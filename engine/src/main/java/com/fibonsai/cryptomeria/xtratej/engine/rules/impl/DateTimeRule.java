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
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAccessor;
import java.util.function.Function;

public class DateTimeRule extends RuleStream<BooleanTimeSeries> {

    private static final Logger log = LoggerFactory.getLogger(DateTimeRule.class);

    private boolean between = true;
    private @Nullable TemporalAccessor begin = null;
    private @Nullable TemporalAccessor end = null;
    private long tolerance = 10; // default 10.0 seconds

    private final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
            .appendOptional(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            .appendOptional(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendOptional(DateTimeFormatter.ISO_LOCAL_TIME)
            .toFormatter();;

    @Override
    public RuleStream<BooleanTimeSeries> setParams(JsonNode params) {
        for (var e: params.properties()) {
            if ("begin".equals(e.getKey()) && e.getValue().isString()) {
                String beginStr = e.getValue().asString();
                if (!beginStr.isBlank()) {
                    this.begin = formatter.parseBest(beginStr, LocalDateTime::from, LocalDate::from, LocalTime::from);
                }
            }
            if ("end".equals(e.getKey()) && e.getValue().isString()) {
                String endStr = e.getValue().asString();
                if (!endStr.isBlank()) {
                    this.end = formatter.parseBest(endStr, LocalDateTime::from, LocalDate::from, LocalTime::from);
                }
            }
            if ("between".equals(e.getKey()) && e.getValue().isBoolean()) {
                between = e.getValue().asBoolean();
            }
            if ("tolerance".equals(e.getKey()) && e.getValue().isLong()) {
                tolerance = e.getValue().asLong();
            }
        }
        return this;
    }

    public LocalDateTime now() {
        // useful when mock in tests
        return LocalDateTime.now();
    }

    @Override
    protected Function<TimeSeries[], BooleanTimeSeries[]> predicate() {
        return timeSeriesArray -> {
            if (!isActivated() || timeSeriesArray.length == 0) {
                log.warn("No sources. Ignoring rule.");
                return new BooleanTimeSeries[0];
            }

            Boolean result = null;
            boolean resultBegin = true;
            if (begin != null) {
                resultBegin = switch (begin) {
                    case LocalDateTime dateTimeBegin -> {
                        if (now().isEqual(dateTimeBegin)) {
                            yield true;
                        } else {
                            yield between ? now().isAfter(dateTimeBegin) : now().isBefore(dateTimeBegin);
                        }
                    }
                    case LocalDate dateBegin -> {
                        LocalDate nowDate = now().toLocalDate();
                        if (nowDate.isEqual(dateBegin)) {
                            yield true;
                        } else {
                            yield between ? nowDate.isAfter(dateBegin) : nowDate.isBefore(dateBegin);
                        }
                    }
                    case LocalTime timeBegin -> {
                        LocalTime nowTime = now().toLocalTime();
                        if (end == null) {
                            end = LocalTime.MAX;
                        }
                        LocalTime timeEnd = LocalTime.from(end);

                        // check time "equal" with tolerance
                        if ((timeBegin.minusSeconds(tolerance).isBefore(nowTime) && timeBegin.plusSeconds(tolerance).isAfter(nowTime)) ||
                                timeEnd.minusSeconds(tolerance).isBefore(nowTime) && timeEnd.plusSeconds(tolerance).isAfter(nowTime)
                        ) {
                            result = true;
                        } else {
                            if (between) {
                                // check between interval
                                result = timeBegin.isBefore(timeEnd) ? nowTime.isAfter(timeBegin) && nowTime.isBefore(timeEnd) :
                                        nowTime.isAfter(timeBegin) || nowTime.isBefore(timeEnd);
                            } else {
                                // check outside interval
                                result = nowTime.isBefore(timeBegin) || nowTime.isAfter(timeEnd);
                            }
                        }
                        yield result;
                    }
                    default -> false;
                };
            }

            boolean resultEnd = true;
            if (result == null && end != null) {
                resultEnd = switch (end) {
                    case LocalDate dateEnd -> {
                        LocalDate nowDate = now().toLocalDate();
                        if (nowDate.isEqual(dateEnd)) {
                            yield true;
                        } else {
                            yield between ? nowDate.isBefore(dateEnd) : nowDate.isAfter(dateEnd);
                        }
                    }
                    case LocalDateTime dateTimeEnd -> {
                        if (now().isEqual(dateTimeEnd)) {
                            yield true;
                        } else {
                            yield between ? now().isBefore(dateTimeEnd) : now().isAfter(dateTimeEnd);
                        }
                    }
                    case LocalTime timeEnd -> {
                        LocalTime nowTime = now().toLocalTime();
                        if (timeEnd.minusSeconds(tolerance).isBefore(nowTime) && timeEnd.plusSeconds(tolerance).isAfter(nowTime)) {
                            yield true;
                        } else {
                            yield between ? timeEnd.isAfter(nowTime) : timeEnd.isBefore(nowTime);
                        }
                    }
                    default -> false;
                };
            }

            result = resultBegin && resultEnd;

            long timestamp = 0L;
            for (var ts: timeSeriesArray) {
                if (ts.timestamp() > timestamp) {
                    timestamp = ts.timestamp();
                }
            }

            return new BooleanTimeSeries[] { new BooleanTimeSeriesBuilder().add(timestamp, result).build() };
        };
    }

    public DateTimeRule setBetween(boolean between) {
        this.between = between;
        return this;
    }

    public DateTimeRule setBegin(String beginStr) {
        if (!beginStr.isBlank()) {
            TemporalAccessor beginObj = formatter.parseBest(beginStr, LocalDateTime::from, LocalDate::from, LocalTime::from);
            this.begin = beginObj instanceof LocalDateTime dt ? dt : (beginObj instanceof LocalDate d ? d.atStartOfDay() : (beginObj instanceof LocalTime t ? t.atDate(LocalDate.now()) : LocalDateTime.MIN));
        }
        return this;
    }

    public DateTimeRule setEnd(String endStr) {
        if (!endStr.isBlank()) {
            TemporalAccessor endObj = formatter.parseBest(endStr, LocalDateTime::from, LocalDate::from, LocalTime::from);
            this.end = endObj instanceof LocalDateTime dt ? dt : (endObj instanceof LocalDate d ? d.atStartOfDay() : (endObj instanceof LocalTime t ? t.atDate(LocalDate.now()) : LocalDateTime.MAX));
        }
        return this;
    }

    public DateTimeRule setTolerance(long tolerance) {
        this.tolerance = tolerance;
        return this;
    }
}
