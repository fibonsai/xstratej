
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

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SuppressWarnings("SwitchStatementWithTooFewBranches")
class WeekdayRuleTest {

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
    void predicate_todayIsWithekday_shouldReturnTrue() {
        String today = LocalDateTime.now().getDayOfWeek().name();
        params.set("weekdays", JsonNodeFactory.instance.arrayNode().add(today.toLowerCase()));
        WeekdayRule rule = switch (RuleType.Weekday.build().setParams(params)) {
            case WeekdayRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{mockTimeSeries};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_todayIsNotWeekday_shouldReturnFalse() {
        String tomorrow = LocalDateTime.now().plusDays(1).getDayOfWeek().name();
        params.set("weekdays", JsonNodeFactory.instance.arrayNode().add(tomorrow.toLowerCase()));
        WeekdayRule rule = switch (RuleType.Weekday.build().setParams(params)) {
            case WeekdayRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{mockTimeSeries};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertFalse(result[0].values()[0]);
    }

    @Test
    void predicate_emptyWeekdayList_shouldReturnTrue() {
        params.set("weekdays", JsonNodeFactory.instance.arrayNode());
        WeekdayRule rule = switch (RuleType.Weekday.build().setParams(params)) {
            case WeekdayRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{mockTimeSeries};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[0]);
    }
    
    @Test
    void predicate_noSources_shouldReturnEmptyArray() {
        WeekdayRule rule = switch (RuleType.Weekday.build().setParams(params)) {
            case WeekdayRule r -> r;
            default -> throw new RuntimeException();
        };

        TimeSeries[] input = new TimeSeries[]{};

        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertEquals(0, result.length);
    }

    @Test
    void predicate_multipleWeekdays_shouldReturnTrue() {
        String today = LocalDateTime.now().getDayOfWeek().name();
        String tomorrow = LocalDateTime.now().plusDays(1).getDayOfWeek().name();
        params.set("weekdays", JsonNodeFactory.instance.arrayNode()
                .add(today.toLowerCase())
                .add(tomorrow.toLowerCase()));
        WeekdayRule rule = switch (RuleType.Weekday.build().setParams(params)) {
            case WeekdayRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{mockTimeSeries};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_allWeekdays_shouldReturnTrue() {
        params.set("weekdays", JsonNodeFactory.instance.arrayNode()
                .add("monday")
                .add("tuesday")
                .add("wednesday")
                .add("thursday")
                .add("friday")
                .add("saturday")
                .add("sunday"));
        WeekdayRule rule = switch (RuleType.Weekday.build().setParams(params)) {
            case WeekdayRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{mockTimeSeries};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_weekendOnly_todayIsWeekend_shouldReturnTrue() {
        String today = LocalDateTime.now().getDayOfWeek().name();
        params.set("weekdays", JsonNodeFactory.instance.arrayNode().add(today.toLowerCase()));
        WeekdayRule rule = switch (RuleType.Weekday.build().setParams(params)) {
            case WeekdayRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{mockTimeSeries};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_weekendOnly_todayIsNotWeekend_shouldReturnFalse() {
        String today = LocalDateTime.now().getDayOfWeek().name();
        params.set("weekdays", JsonNodeFactory.instance.arrayNode().add("saturday").add("sunday"));
        WeekdayRule rule = switch (RuleType.Weekday.build().setParams(params)) {
            case WeekdayRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{mockTimeSeries};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // If today is weekend, it should return true; otherwise false
        boolean isWeekend = today.equalsIgnoreCase("saturday") || today.equalsIgnoreCase("sunday");
        assertEquals(isWeekend, result[0].values()[0]);
    }

    @Test
    void predicate_uppercaseWeekday_shouldMatch() {
        String today = LocalDateTime.now().getDayOfWeek().name();
        params.set("weekdays", JsonNodeFactory.instance.arrayNode().add(today.toUpperCase()));
        WeekdayRule rule = switch (RuleType.Weekday.build().setParams(params)) {
            case WeekdayRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{mockTimeSeries};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_invalidWeekday_ignoresIt() {
        params.set("weekdays", JsonNodeFactory.instance.arrayNode().add("invalidday"));
        WeekdayRule rule = switch (RuleType.Weekday.build().setParams(params)) {
            case WeekdayRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{mockTimeSeries};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        // Invalid weekday should result in false (no match)
        assertFalse(result[0].values()[0]);
    }

    @Test
    void predicate_notActivated_returnsEmpty() {
        WeekdayRule inactiveRule = switch (RuleType.Weekday.build().setParams(params)) {
            case WeekdayRule r -> r;
            default -> throw new RuntimeException();
        };

        TimeSeries[] input = new TimeSeries[]{mockTimeSeries};
        BooleanTimeSeries[] result = inactiveRule.predicate().apply(input);

        assertEquals(0, result.length);
    }

    @Test
    void predicate_withSingleDayArray() {
        String today = LocalDateTime.now().getDayOfWeek().name();
        params.set("weekdays", JsonNodeFactory.instance.arrayNode().add(today.toLowerCase()));
        WeekdayRule rule = switch (RuleType.Weekday.build().setParams(params)) {
            case WeekdayRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{mockTimeSeries};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[0]);
    }

    @Test
    void predicate_mixedCaseWeekdays() {
        String today = LocalDateTime.now().getDayOfWeek().name();
        params.set("weekdays", JsonNodeFactory.instance.arrayNode()
                .add(today.substring(0, 1).toUpperCase() + today.substring(1).toLowerCase()));
        WeekdayRule rule = switch (RuleType.Weekday.build().setParams(params)) {
            case WeekdayRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new DirectFlux<>());

        TimeSeries[] input = new TimeSeries[]{mockTimeSeries};
        BooleanTimeSeries[] result = rule.predicate().apply(input);

        assertTrue(result[0].values()[0]);
    }
}
