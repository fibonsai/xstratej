
package com.fibonsai.cryptomeria.xtratej.rules.impl;

import com.fibonsai.cryptomeria.xtratej.event.reactive.Fifo;
import com.fibonsai.cryptomeria.xtratej.event.ITemporalData;
import com.fibonsai.cryptomeria.xtratej.event.series.impl.BooleanSingleTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.impl.BooleanSingleTimeSeries.BooleanSingle;
import com.fibonsai.cryptomeria.xtratej.event.series.impl.SingleTimeSeries;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.*;

class NotRuleTest {

    private AutoCloseable closeable;

    @Mock
    private Fifo<ITemporalData> mockResults;

    private NotRule notRule;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        ObjectNode properties = JsonNodeFactory.instance.objectNode();
        properties.set("sources", JsonNodeFactory.instance.arrayNode().add("s1"));
        notRule = new NotRule("testNotRule", properties, mockResults);
    }

    @AfterEach
    void finish() {
        try {
            closeable.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private BooleanSingleTimeSeries createBooleanSeries(String name, long timestamp, boolean... values) {
        BooleanSingle[] singles = new BooleanSingle[values.length];
        for (int i = 0; i < values.length; i++) {
            singles[i] = new BooleanSingle(timestamp + i, values[i]);
        }
        return new BooleanSingleTimeSeries(name, singles);
    }

    @Test
    void predicate_withTrue_shouldReturnFalse() {
        ITemporalData series = createBooleanSeries("s1", 100L, true);
        ITemporalData[] input = new ITemporalData[]{series};

        BooleanSingle[] result = notRule.predicate().apply(input);

        assertEquals(1, result.length);
        assertFalse(result[0].value());
        assertEquals(100L, result[0].timestamp());
    }

    @Test
    void predicate_withFalse_shouldReturnTrue() {
        ITemporalData series = createBooleanSeries("s1", 100L, false);
        ITemporalData[] input = new ITemporalData[]{series};

        BooleanSingle[] result = notRule.predicate().apply(input);

        assertEquals(1, result.length);
        assertTrue(result[0].value());
        assertEquals(100L, result[0].timestamp());
    }

    @Test
    void predicate_withMultipleTimeSeries_shouldReturnEmptyArray() {
        ObjectNode properties = JsonNodeFactory.instance.objectNode();
        properties.put("allSources", true);
        notRule = new NotRule("testNotRule", properties, mockResults);
        
        ITemporalData series1 = createBooleanSeries("s1", 100L, true);
        ITemporalData series2 = createBooleanSeries("s2", 101L, false);
        ITemporalData[] input = new ITemporalData[]{series1, series2};

        BooleanSingle[] result = notRule.predicate().apply(input);

        assertEquals(0, result.length);
    }

    @Test
    void predicate_withNonBooleanTimeSeries_shouldReturnEmptyArray() {
        ITemporalData series = new SingleTimeSeries("s1", new SingleTimeSeries.Single[]{new SingleTimeSeries.Single(100L, 1.0)});
        ITemporalData[] input = new ITemporalData[]{series};

        BooleanSingle[] result = notRule.predicate().apply(input);

        assertEquals(0, result.length);
    }

    @Test
    void predicate_withNoSources_shouldReturnEmptyArray() {
        notRule.setAllSources(false);
        ITemporalData[] input = new ITemporalData[]{};

        BooleanSingle[] result = notRule.predicate().apply(input);

        assertEquals(0, result.length);
    }
}
