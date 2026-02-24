
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

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

class RandomRuleTest {

    private AutoCloseable closeable;

    @Mock
    private Fifo<ITemporalData> mockResults;

    @Mock
    private ITemporalData mockTimeSeries;

    private RandomRule randomRule;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        ObjectNode properties = JsonNodeFactory.instance.objectNode();
        properties.put("allSources", true);
        randomRule = new RandomRule("testRandomRule", properties, mockResults);
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
    void predicate_shouldReturnRandomBoolean() {
        // Arrange
        long expectedTimestamp = 123456789L;
        when(mockTimeSeries.timestamp()).thenReturn(expectedTimestamp);

        ITemporalData[] input = {mockTimeSeries};

        // Act
        Function<ITemporalData[], BooleanSingle[]> predicate = randomRule.predicate();
        BooleanSingle[] result = predicate.apply(input);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals(expectedTimestamp, result[0].timestamp());
    }

    @Test
    void predicate_whenNoSources_shouldReturnEmptyArray() {
        // Arrange
        ObjectNode properties = JsonNodeFactory.instance.objectNode();
        randomRule = new RandomRule("testRandomRule", properties, mockResults);
        randomRule.setAllSources(false);
        ITemporalData[] input = {mockTimeSeries};

        // Act
        Function<ITemporalData[], BooleanSingle[]> predicate = randomRule.predicate();
        BooleanSingle[] result = predicate.apply(input);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.length);
    }
}
