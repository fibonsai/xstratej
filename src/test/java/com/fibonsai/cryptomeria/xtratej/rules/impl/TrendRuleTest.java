
package com.fibonsai.cryptomeria.xtratej.rules.impl;

import com.fibonsai.cryptomeria.xtratej.event.reactive.Fifo;
import com.fibonsai.cryptomeria.xtratej.event.ITemporalData;
import com.fibonsai.cryptomeria.xtratej.event.series.impl.BooleanSingleTimeSeries.BooleanSingle;
import com.fibonsai.cryptomeria.xtratej.event.series.impl.SingleTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.impl.SingleTimeSeries.Single;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.*;

class TrendRuleTest {

    private AutoCloseable closeable;

    @Mock
    private Fifo<ITemporalData> mockResults;

    private ObjectNode properties;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        properties = JsonNodeFactory.instance.objectNode();
    }

    @AfterEach
    void finish() {
        try {
            closeable.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private SingleTimeSeries createSingleTimeSeries(String name, long[] timestamps, double[] values) {
        Single[] singles = new Single[values.length];
        for (int i = 0; i < values.length; i++) {
            singles[i] = new Single(timestamps[i], values[i]);
        }
        return new SingleTimeSeries(name, singles);
    }

    @Test
    void processProperties_shouldSetSourceIdAndIsRising() {
        properties.put("sourceId", "s2");
        properties.put("isRising", true);
        TrendRule rule = new TrendRule("test", properties, mockResults);
        assertNotNull(rule);
    }

    @Test
    void predicate_isRisingTrue_withRisingTrend_shouldReturnTrue() {
        properties.put("isRising", true);
        TrendRule rule = new TrendRule("test", properties, mockResults);
        rule.setAllSources(true);

        ITemporalData series = createSingleTimeSeries("s1", new long[]{1, 2, 3}, new double[]{1, 2, 3}); // Rising
        ITemporalData[] input = new ITemporalData[]{series};

        BooleanSingle[] result = rule.predicate().apply(input);

        assertTrue(result[0].value());
    }

    @Test
    void predicate_isRisingTrue_withFallingTrend_shouldReturnFalse() {
        properties.put("isRising", true);
        TrendRule rule = new TrendRule("test", properties, mockResults);
        rule.setAllSources(true);

        ITemporalData series = createSingleTimeSeries("s1", new long[]{1, 2, 3}, new double[]{3, 2, 1}); // Falling
        ITemporalData[] input = new ITemporalData[]{series};

        BooleanSingle[] result = rule.predicate().apply(input);

        assertFalse(result[0].value());
    }

    @Test
    void predicate_isRisingFalse_withFallingTrend_shouldReturnTrue() {
        properties.put("isRising", false);
        TrendRule rule = new TrendRule("test", properties, mockResults);
        rule.setAllSources(true);

        ITemporalData series = createSingleTimeSeries("s1", new long[]{1, 2, 3}, new double[]{3, 2, 1}); // Falling
        ITemporalData[] input = new ITemporalData[]{series};

        BooleanSingle[] result = rule.predicate().apply(input);

        assertTrue(result[0].value());
    }

    @Test
    void predicate_compareWithSource_shouldReturnCorrectTrend() {
        properties.put("sourceId", "s2");
        properties.put("isRising", true); // s1 slope > s2 slope
        properties.set("sources", JsonNodeFactory.instance.arrayNode().add("s1"));
        TrendRule rule = new TrendRule("test", properties, mockResults);

        ITemporalData s1 = createSingleTimeSeries("s1", new long[]{1, 2, 3}, new double[]{1, 2, 3}); // Slope 1.0
        ITemporalData s2 = createSingleTimeSeries("s2", new long[]{1, 2, 3}, new double[]{1, 1.5, 2}); // Slope 0.5
        ITemporalData[] input = new ITemporalData[]{s1, s2};

        BooleanSingle[] result = rule.predicate().apply(input);

        assertTrue(result[0].value());
    }
    
    @Test
    void predicate_noSources_shouldReturnEmptyArray() {
        TrendRule rule = new TrendRule("test", properties, mockResults);
        rule.setAllSources(false);
        ITemporalData[] input = new ITemporalData[]{};

        BooleanSingle[] result = rule.predicate().apply(input);

        assertEquals(0, result.length);
    }
}
