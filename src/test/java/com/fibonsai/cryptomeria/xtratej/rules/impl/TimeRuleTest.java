
package com.fibonsai.cryptomeria.xtratej.rules.impl;

import com.fibonsai.cryptomeria.xtratej.event.reactive.Fifo;
import com.fibonsai.cryptomeria.xtratej.event.ITemporalData;
import com.fibonsai.cryptomeria.xtratej.event.series.impl.BooleanSingleTimeSeries.BooleanSingle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class TimeRuleTest {

    private AutoCloseable closeable;

    @Mock
    private Fifo<ITemporalData> mockResults;

    @Mock
    private ITemporalData mockTimeSeries;

    private ObjectNode properties;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        properties = JsonNodeFactory.instance.objectNode();
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
        LocalTime now = LocalTime.now();
        properties.put("begin", now.minusHours(1).format(DateTimeFormatter.ISO_LOCAL_TIME));
        properties.put("end", now.plusHours(1).format(DateTimeFormatter.ISO_LOCAL_TIME));
        TimeRule rule = new TimeRule("test", properties, mockResults);
        rule.setAllSources(true);

        ITemporalData[] input = new ITemporalData[]{mockTimeSeries};
        BooleanSingle[] result = rule.predicate().apply(input);

        assertTrue(result[0].value());
    }

    @Test
    void predicate_nowIsOutsideRange_shouldReturnFalse() {
        LocalTime now = LocalTime.now();
        properties.put("begin", now.plusHours(1).format(DateTimeFormatter.ISO_LOCAL_TIME));
        properties.put("end", now.plusHours(2).format(DateTimeFormatter.ISO_LOCAL_TIME));
        TimeRule rule = new TimeRule("test", properties, mockResults);
        rule.setAllSources(true);

        ITemporalData[] input = new ITemporalData[]{mockTimeSeries};
        BooleanSingle[] result = rule.predicate().apply(input);

        assertFalse(result[0].value());
    }

    @Test
    void predicate_invertedRange_nowIsInside_shouldReturnFalse() {
        LocalTime now = LocalTime.now();
        // Inverted range means end = MAX if invert property is default (false)
        properties.put("begin", now.plusHours(1).format(DateTimeFormatter.ISO_LOCAL_TIME));
        properties.put("end", now.minusHours(1).format(DateTimeFormatter.ISO_LOCAL_TIME));
        TimeRule rule = new TimeRule("test", properties, mockResults);
        rule.setAllSources(true);

        ITemporalData[] input = new ITemporalData[]{mockTimeSeries};
        BooleanSingle[] result = rule.predicate().apply(input);

        assertFalse(result[0].value());
    }

    @Test
    void predicate_invertedRange_with_invertFlag_nowIsInside_shouldReturnTrue() {
        LocalTime now = LocalTime.now();
        // Inverted range with invert = true means we check if now is OUTSIDE the range from end to begin
        properties.put("begin", now.plusHours(1).format(DateTimeFormatter.ISO_LOCAL_TIME));
        properties.put("end", now.minusHours(1).format(DateTimeFormatter.ISO_LOCAL_TIME));
        TimeRule rule = new TimeRule("test", properties, mockResults);
        rule.setAllSources(true);
        rule.setInvert(true);

        ITemporalData[] input = new ITemporalData[]{mockTimeSeries};
        BooleanSingle[] result = rule.predicate().apply(input);

        assertTrue(result[0].value());
    }

    @Test
    void predicate_noSources_shouldReturnEmptyArray() {
        TimeRule rule = new TimeRule("test", properties, mockResults);
        rule.setAllSources(false);
        ITemporalData[] input = new ITemporalData[]{};

        BooleanSingle[] result = rule.predicate().apply(input);

        assertEquals(0, result.length);
    }
}
