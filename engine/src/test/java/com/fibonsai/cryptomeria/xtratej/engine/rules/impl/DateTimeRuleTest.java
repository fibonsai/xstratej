
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

import com.fibonsai.cryptomeria.xtratej.engine.rules.RuleType;
import com.fibonsai.cryptomeria.xtratej.event.reactive.DirectFlux;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.BooleanTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.TimeSeries;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DateTimeRuleTest {

    private AutoCloseable closeable;

    @Mock
    private TimeSeries mockTimeSeries;

    private DateTimeRule rule;
    private ObjectNode params;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        params = JsonNodeFactory.instance.objectNode();
        now = LocalDateTime.of(LocalDate.EPOCH, LocalTime.MIN).plusMonths(6).plusHours(12);
        when(mockTimeSeries.timestamp()).thenReturn(123L);
        //noinspection SwitchStatementWithTooFewBranches
        DateTimeRule realDateTimeRule = switch (RuleType.DateTime.build()) {
            case DateTimeRule r -> r;
            default -> throw new RuntimeException();
        };
        rule = spy(realDateTimeRule);
        doReturn(now).when(rule).now();
    }

    @AfterEach
    void finish() {
        try {
            closeable.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void predicate_nowIsWithinRange_shouldReturnTrue() {
        params.put("begin", now.minusHours(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        params.put("end", now.plusHours(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        rule.setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{mockTimeSeries};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_nowIsWithinRange2_shouldReturnTrue() {
        params.put("begin", now.minusDays(15).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        params.put("end", now.plusDays(15).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        rule.setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{mockTimeSeries};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_nowTimeIsWithinRange_shouldReturnTrue() {
        LocalTime nowTime = now.toLocalTime();
        params.put("begin", nowTime.minusHours(1).format(DateTimeFormatter.ISO_LOCAL_TIME));
        params.put("end", nowTime.plusHours(1).format(DateTimeFormatter.ISO_LOCAL_TIME));
        rule.setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{mockTimeSeries};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_nowIsOutsideRange_shouldReturnFalse() {
        params.put("begin", now.plusHours(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        params.put("end", now.plusHours(2).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        rule.setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{mockTimeSeries};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertFalse(result[0].values()[0]);
    }

    @Test
    void predicate_noSources_shouldReturnEmptyArray() {
        rule.setParams(params);
        TimeSeries[] input = new TimeSeries[]{};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertEquals(0, result.length);
    }

    @Test
    void predicate_beginExactlyAtNow_shouldReturnTrue() {
        params.put("begin", now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        params.put("end", now.plusHours(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        rule.setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{mockTimeSeries};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_endExactlyAtNow_shouldReturnTrue() {
        params.put("begin", now.minusHours(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        params.put("end", now.plusSeconds(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        rule.setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{mockTimeSeries};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_emptyBegin_shouldEvaluateOnlyEnd() {
        params.put("end", now.plusHours(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        rule.setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{mockTimeSeries};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_emptyEnd_shouldEvaluateOnlyBegin() {
        params.put("begin", now.minusHours(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        rule.setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{mockTimeSeries};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_bothEmpty_shouldReturnTrue() {
        rule.setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{mockTimeSeries};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_withFutureTimestampInPastRange_shouldReturnFalse() {
        // Range is in the past
        params.put("begin", now.minusHours(2).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        params.put("end", now.minusHours(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        rule.setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{mockTimeSeries};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // Mock timestamp 123L is way in the past compared to now
        assertFalse(result[0].values()[0]);
    }

    @Test
    void predicate_withVeryFarFutureRange_shouldReturnFalse() {
        // Range is way in the future
        params.put("begin", now.plusDays(365).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        params.put("end", now.plusDays(366).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        rule.setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{mockTimeSeries};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertFalse(result[0].values()[0]);
    }

    @Test
    void predicate_notActivated_returnsEmpty() {
        rule.setParams(params);
        TimeSeries[] input = new TimeSeries[]{mockTimeSeries};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertEquals(0, result.length);
    }

    @Test
    void predicate_nowTimeIsOutsideRange_shouldReturnFalse() {
        LocalTime nowLocalTime = now.toLocalTime();
        params.put("begin", nowLocalTime.plusHours(1).format(DateTimeFormatter.ISO_LOCAL_TIME));
        params.put("end", nowLocalTime.plusHours(2).format(DateTimeFormatter.ISO_LOCAL_TIME));
        rule.setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{mockTimeSeries};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertFalse(result[0].values()[0]);
    }

    @Test
    void predicate_invertedRange_nowIsInside_shouldReturnFalse() {
        // Inverted range means end = MAX if `invert` param is default (false)
        LocalTime nowLocalTime = now.toLocalTime();
        params.put("begin", nowLocalTime.plusHours(1).format(DateTimeFormatter.ISO_LOCAL_TIME));
        params.put("end", nowLocalTime.minusHours(1).format(DateTimeFormatter.ISO_LOCAL_TIME));
        rule.setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{mockTimeSeries};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertFalse(result[0].values()[0]);
    }

    @Test
    void predicate_invertedRange_with_invertFlag_nowIsInside_shouldReturnTrue() {
        // Inverted range with invert = true means we check if now is OUTSIDE the range from end to begin
        LocalTime nowLocalTime = now.toLocalTime();
        params.put("begin", nowLocalTime.plusHours(1).format(DateTimeFormatter.ISO_LOCAL_TIME));
        params.put("end", nowLocalTime.minusHours(1).format(DateTimeFormatter.ISO_LOCAL_TIME));
        rule.setParams(params);
        rule.watch(new DirectFlux<>());

        rule.setBetween(false);

        TimeSeries[] input = new TimeSeries[]{mockTimeSeries};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_beginTimeExactlyAtNow_shouldReturnTrue() {
        LocalTime nowLocalTime = now.toLocalTime();
        long tolerance = 10; // seconds
        params.put("begin", nowLocalTime.format(DateTimeFormatter.ISO_LOCAL_TIME));
        params.put("end", nowLocalTime.format(DateTimeFormatter.ISO_LOCAL_TIME));
        params.put("tolerance", tolerance);
        rule.setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{mockTimeSeries};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_endTimeExactlyAtNow_shouldReturnTrue() {
        LocalTime nowLocalTime = now.toLocalTime();
        params.put("begin", nowLocalTime.minusHours(1).format(DateTimeFormatter.ISO_LOCAL_TIME));
        long tolerance = 10; // seconds
        params.put("end", nowLocalTime.format(DateTimeFormatter.ISO_LOCAL_TIME));
        params.put("tolerance", tolerance);
        rule.setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{mockTimeSeries};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_midnightBoundary_begin() {
        now = LocalDateTime.of(LocalDate.EPOCH, LocalTime.MIN);
        doReturn(now).when(rule).now();
        LocalTime midnightBoundaryBegin = LocalTime.of(0, 0, 1); // Just after midnight
        params.put("begin", midnightBoundaryBegin.minusSeconds(30).format(DateTimeFormatter.ISO_LOCAL_TIME));
        params.put("end", midnightBoundaryBegin.plusHours(1).format(DateTimeFormatter.ISO_LOCAL_TIME));
        rule.setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{mockTimeSeries};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_midnightBoundary_end() {
        now = LocalDateTime.of(LocalDate.EPOCH, LocalTime.MIN);
        doReturn(now).when(rule).now();
        LocalTime midnightBoundaryEnd = LocalTime.of(23, 59, 59); // Just before midnight
        params.put("begin", midnightBoundaryEnd.minusHours(1).format(DateTimeFormatter.ISO_LOCAL_TIME));
        params.put("end", midnightBoundaryEnd.plusSeconds(30).format(DateTimeFormatter.ISO_LOCAL_TIME));
        rule.setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{mockTimeSeries};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_emptyBeginTime_shouldEvaluateOnlyEnd() {
        LocalTime nowTime = now.toLocalTime();
        params.put("end", nowTime.plusHours(1).format(DateTimeFormatter.ISO_LOCAL_TIME));
        rule.setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{mockTimeSeries};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_emptyEndTime_shouldEvaluateOnlyBegin() {
        LocalTime nowTime = now.toLocalTime();
        params.put("begin", nowTime.minusHours(1).format(DateTimeFormatter.ISO_LOCAL_TIME));
        rule.setParams(params);
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{mockTimeSeries};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[0]);
    }
}
