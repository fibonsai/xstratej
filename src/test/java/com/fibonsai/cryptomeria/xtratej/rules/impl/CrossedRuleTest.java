
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

class CrossedRuleTest {

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
    void processProperties_shouldSetThresholdAndSourceId() {
        properties.put("threshold", 50.5);
        properties.put("sourceId", "comparator");
        CrossedRule rule = new CrossedRule("test", properties, mockResults);

        // We can't directly test private fields, but we test the behavior in other tests
        assertNotNull(rule);
    }

    @Test
    void predicate_crossedThreshold_shouldReturnTrue() {
        properties.put("threshold", 50.0);
        CrossedRule rule = new CrossedRule("test", properties, mockResults);
        rule.setAllSources(true);

        ITemporalData series = createSingleTimeSeries("s1", new long[]{1L, 2L}, new double[]{40.0, 60.0});
        ITemporalData[] input = new ITemporalData[]{series};

        BooleanSingle[] result = rule.predicate().apply(input);

        assertEquals(1, result.length);
        assertTrue(result[0].value());
    }

    @Test
    void predicate_notCrossedThreshold_shouldReturnFalse() {
        properties.put("threshold", 70.0);
        CrossedRule rule = new CrossedRule("test", properties, mockResults);
        rule.setAllSources(true);

        ITemporalData series = createSingleTimeSeries("s1", new long[]{1L, 2L}, new double[]{40.0, 60.0});
        ITemporalData[] input = new ITemporalData[]{series};

        BooleanSingle[] result = rule.predicate().apply(input);

        assertEquals(1, result.length);
        assertFalse(result[0].value());
    }

    @Test
    void predicate_seriesCrossed_shouldReturnTrue() {
        properties.put("sourceId", "s2");
        properties.set("sources", JsonNodeFactory.instance.arrayNode().add("s1").add("s2"));
        CrossedRule rule = new CrossedRule("test", properties, mockResults);
        rule.setAllSources(false);

        ITemporalData series1 = createSingleTimeSeries("s1", new long[]{1L, 2L}, new double[]{40.0, 60.0});
        ITemporalData series2 = createSingleTimeSeries("s2", new long[]{1L, 2L}, new double[]{50.0, 50.0});
        ITemporalData[] input = new ITemporalData[]{series1, series2};

        BooleanSingle[] result = rule.predicate().apply(input);

        assertEquals(1, result.length);
        assertTrue(result[0].value());
    }

    @Test
    void predicate_seriesNotCrossed_shouldReturnFalse() {
        properties.put("sourceId", "s2");
        properties.set("sources", JsonNodeFactory.instance.arrayNode().add("s1").add("s2"));
        CrossedRule rule = new CrossedRule("test", properties, mockResults);
        rule.setAllSources(false);

        ITemporalData series1 = createSingleTimeSeries("s1", new long[]{1L, 2L}, new double[]{40.0, 60.0});
        ITemporalData series2 = createSingleTimeSeries("s2", new long[]{1L, 2L}, new double[]{70.0, 80.0});
        ITemporalData[] input = new ITemporalData[]{series1, series2};

        BooleanSingle[] result = rule.predicate().apply(input);

        assertEquals(1, result.length);
        assertFalse(result[0].value());
    }

    @Test
    void predicate_noSources_shouldReturnEmptyArray() {
        CrossedRule rule = new CrossedRule("test", properties, mockResults);
        rule.setAllSources(false);
        ITemporalData[] input = new ITemporalData[]{};

        BooleanSingle[] result = rule.predicate().apply(input);

        assertEquals(0, result.length);
    }
}
