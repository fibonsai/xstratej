
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
import com.fibonsai.cryptomeria.xtratej.event.ITemporalData;
import com.fibonsai.cryptomeria.xtratej.event.reactive.Fifo;
import com.fibonsai.cryptomeria.xtratej.event.series.impl.BooleanSingleTimeSeries.BooleanSingle;
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

class WeekdayRuleTest {

    private AutoCloseable closeable;

    @Mock
    private ITemporalData mockTimeSeries;

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
        rule.watch(new Fifo<>());

        ITemporalData[] input = new ITemporalData[]{mockTimeSeries};
        BooleanSingle[] result = rule.predicate().apply(input);

        assertTrue(result[0].value());
    }

    @Test
    void predicate_todayIsNotWeekday_shouldReturnFalse() {
        String tomorrow = LocalDateTime.now().plusDays(1).getDayOfWeek().name();
        params.set("weekdays", JsonNodeFactory.instance.arrayNode().add(tomorrow.toLowerCase()));
        WeekdayRule rule = switch (RuleType.Weekday.build().setParams(params)) {
            case WeekdayRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new Fifo<>());

        ITemporalData[] input = new ITemporalData[]{mockTimeSeries};
        BooleanSingle[] result = rule.predicate().apply(input);

        assertFalse(result[0].value());
    }

    @Test
    void predicate_emptyWeekdayList_shouldReturnTrue() {
        params.set("weekdays", JsonNodeFactory.instance.arrayNode());
        WeekdayRule rule = switch (RuleType.Weekday.build().setParams(params)) {
            case WeekdayRule r -> r;
            default -> throw new RuntimeException();
        };
        rule.watch(new Fifo<>());

        ITemporalData[] input = new ITemporalData[]{mockTimeSeries};
        BooleanSingle[] result = rule.predicate().apply(input);

        assertTrue(result[0].value());
    }
    
    @Test
    void predicate_noSources_shouldReturnEmptyArray() {
        WeekdayRule rule = switch (RuleType.Weekday.build().setParams(params)) {
            case WeekdayRule r -> r;
            default -> throw new RuntimeException();
        };

        ITemporalData[] input = new ITemporalData[]{};

        BooleanSingle[] result = rule.predicate().apply(input);

        assertEquals(0, result.length);
    }
}
