
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
import com.fibonsai.cryptomeria.xtratej.event.reactive.Fifo;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.BooleanTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.TimeSeries;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class DateTimeRuleTest {

    private AutoCloseable closeable;

    @Mock
    private TimeSeries mockTimeSeries;

    private ObjectNode params;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        params = JsonNodeFactory.instance.objectNode();
        when(mockTimeSeries.timestamp()).thenReturn(123L);
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
        LocalDateTime now = LocalDateTime.now();
        params.put("begin", now.minusHours(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        params.put("end", now.plusHours(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        DateTimeRule rule = switch (RuleType.DateTime.build().setParams(params)) {
            case DateTimeRule dateTimeRule -> dateTimeRule;
            default -> throw new RuntimeException();
        };
        rule.watch(new Fifo<>());

        TimeSeries[] input = new TimeSeries[]{mockTimeSeries};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_nowIsOutsideRange_shouldReturnFalse() {
        LocalDateTime now = LocalDateTime.now();
        params.put("begin", now.plusHours(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        params.put("end", now.plusHours(2).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        DateTimeRule rule = switch (RuleType.DateTime.build().setParams(params)) {
            case DateTimeRule dateTimeRule -> dateTimeRule;
            default -> throw new RuntimeException();
        };
        rule.watch(new Fifo<>());

        TimeSeries[] input = new TimeSeries[]{mockTimeSeries};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertFalse(result[0].values()[0]);
    }

    @Test
    void predicate_withCustomFormat_shouldEvaluateCorrectly() {
        LocalDateTime now = LocalDateTime.now();
        String format = "yyyy-MM-dd HH:mm";
        params.put("datetimeFormat", format);
        params.put("begin", now.minusMinutes(1).format(DateTimeFormatter.ofPattern(format)));
        params.put("end", now.plusMinutes(1).format(DateTimeFormatter.ofPattern(format)));
        DateTimeRule rule = switch (RuleType.DateTime.build().setParams(params)) {
            case DateTimeRule dateTimeRule -> dateTimeRule;
            default -> throw new RuntimeException();
        };
        rule.watch(new Fifo<>());

        TimeSeries[] input = new TimeSeries[]{mockTimeSeries};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[0]);
    }
    
    @Test
    void predicate_invertedRange_nowIsOutside_shouldReturnTrue() {
        LocalDateTime now = LocalDateTime.now();
        // Inverted range: true if now is NOT between end and begin
        params.put("begin", now.plusHours(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        params.put("end", now.minusHours(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        DateTimeRule rule = switch (RuleType.DateTime.build().setParams(params)) {
            case DateTimeRule dateTimeRule -> dateTimeRule;
            default -> throw new RuntimeException();
        };
        rule.watch(new Fifo<>());

        TimeSeries[] input = new TimeSeries[]{mockTimeSeries};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_noSources_shouldReturnEmptyArray() {
        DateTimeRule rule = switch (RuleType.DateTime.build().setParams(params)) {
            case DateTimeRule dateTimeRule -> dateTimeRule;
            default -> throw new RuntimeException();
        };

        TimeSeries[] input = new TimeSeries[]{};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertEquals(0, result.length);
    }
}
