
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

class LimitRuleTest {

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
    void predicate_withinMinMax_shouldReturnTrue() {
        properties.put("min", 10.0);
        properties.put("max", 20.0);
        LimitRule rule = new LimitRule("test", properties, mockResults);
        rule.setAllSources(true);

        ITemporalData series = createSingleTimeSeries("s1", new long[]{1L}, new double[]{15.0});
        ITemporalData[] input = new ITemporalData[]{series};

        BooleanSingle[] result = rule.predicate().apply(input);

        assertTrue(result[0].value());
    }

    @Test
    void predicate_belowMin_shouldReturnFalse() {
        properties.put("min", 10.0);
        properties.put("max", 20.0);
        LimitRule rule = new LimitRule("test", properties, mockResults);
        rule.setAllSources(true);

        ITemporalData series = createSingleTimeSeries("s1", new long[]{1L}, new double[]{5.0});
        ITemporalData[] input = new ITemporalData[]{series};

        BooleanSingle[] result = rule.predicate().apply(input);

        assertFalse(result[0].value());
    }

    @Test
    void predicate_withinSourceBounds_shouldReturnTrue() {
        properties.put("lowerSourceId", "lower");
        properties.put("topSourceId", "top");
        properties.set("sources", JsonNodeFactory.instance.arrayNode().add("s1"));
        LimitRule rule = new LimitRule("test", properties, mockResults);

        ITemporalData series = createSingleTimeSeries("s1", new long[]{1L}, new double[]{15.0});
        ITemporalData lower = createSingleTimeSeries("lower", new long[]{1L}, new double[]{10.0});
        ITemporalData top = createSingleTimeSeries("top", new long[]{1L}, new double[]{20.0});
        ITemporalData[] input = new ITemporalData[]{series, lower, top};

        BooleanSingle[] result = rule.predicate().apply(input);

        assertTrue(result[0].value());
    }

    @Test
    void predicate_aboveTopSource_shouldReturnFalse() {
        properties.put("lowerSourceId", "lower");
        properties.put("topSourceId", "top");
        properties.set("sources", JsonNodeFactory.instance.arrayNode().add("s1"));
        LimitRule rule = new LimitRule("test", properties, mockResults);

        ITemporalData series = createSingleTimeSeries("s1", new long[]{1L}, new double[]{25.0});
        ITemporalData lower = createSingleTimeSeries("lower", new long[]{1L}, new double[]{10.0});
        ITemporalData top = createSingleTimeSeries("top", new long[]{1L}, new double[]{20.0});
        ITemporalData[] input = new ITemporalData[]{series, lower, top};

        BooleanSingle[] result = rule.predicate().apply(input);

        assertFalse(result[0].value());
    }
    
    @Test
    void predicate_noSources_shouldReturnEmptyArray() {
        LimitRule rule = new LimitRule("test", properties, mockResults);
        rule.setAllSources(false);
        ITemporalData[] input = new ITemporalData[]{};

        BooleanSingle[] result = rule.predicate().apply(input);

        assertEquals(0, result.length);
    }
}
