
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class DateTimeRuleTest {

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
        LocalDateTime now = LocalDateTime.now();
        properties.put("begin", now.minusHours(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        properties.put("end", now.plusHours(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        DateTimeRule rule = new DateTimeRule("test", properties, mockResults);
        rule.setAllSources(true);

        ITemporalData[] input = new ITemporalData[]{mockTimeSeries};
        BooleanSingle[] result = rule.predicate().apply(input);

        assertTrue(result[0].value());
    }

    @Test
    void predicate_nowIsOutsideRange_shouldReturnFalse() {
        LocalDateTime now = LocalDateTime.now();
        properties.put("begin", now.plusHours(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        properties.put("end", now.plusHours(2).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        DateTimeRule rule = new DateTimeRule("test", properties, mockResults);
        rule.setAllSources(true);

        ITemporalData[] input = new ITemporalData[]{mockTimeSeries};
        BooleanSingle[] result = rule.predicate().apply(input);

        assertFalse(result[0].value());
    }

    @Test
    void predicate_withCustomFormat_shouldEvaluateCorrectly() {
        LocalDateTime now = LocalDateTime.now();
        String format = "yyyy-MM-dd HH:mm";
        properties.put("datetimeFormat", format);
        properties.put("begin", now.minusMinutes(1).format(DateTimeFormatter.ofPattern(format)));
        properties.put("end", now.plusMinutes(1).format(DateTimeFormatter.ofPattern(format)));
        DateTimeRule rule = new DateTimeRule("test", properties, mockResults);
        rule.setAllSources(true);

        ITemporalData[] input = new ITemporalData[]{mockTimeSeries};
        BooleanSingle[] result = rule.predicate().apply(input);

        assertTrue(result[0].value());
    }
    
    @Test
    void predicate_invertedRange_nowIsOutside_shouldReturnTrue() {
        LocalDateTime now = LocalDateTime.now();
        // Inverted range: true if now is NOT between end and begin
        properties.put("begin", now.plusHours(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        properties.put("end", now.minusHours(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        DateTimeRule rule = new DateTimeRule("test", properties, mockResults);
        rule.setAllSources(true);

        ITemporalData[] input = new ITemporalData[]{mockTimeSeries};
        BooleanSingle[] result = rule.predicate().apply(input);

        assertTrue(result[0].value());
    }

    @Test
    void predicate_noSources_shouldReturnEmptyArray() {
        DateTimeRule rule = new DateTimeRule("test", properties, mockResults);
        rule.setAllSources(false);
        ITemporalData[] input = new ITemporalData[]{};

        BooleanSingle[] result = rule.predicate().apply(input);

        assertEquals(0, result.length);
    }
}
