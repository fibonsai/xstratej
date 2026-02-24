
package com.fibonsai.cryptomeria.xtratej.rules.impl;

import com.fibonsai.cryptomeria.xtratej.event.reactive.Fifo;
import com.fibonsai.cryptomeria.xtratej.event.ITemporalData;
import com.fibonsai.cryptomeria.xtratej.event.series.impl.BooleanSingleTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.impl.BooleanSingleTimeSeries.BooleanSingle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.*;

class XOrRuleTest {

    private AutoCloseable closeable;

    @Mock
    private Fifo<ITemporalData> mockResults;

    private XOrRule xorRule;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        ObjectNode properties = JsonNodeFactory.instance.objectNode();
        properties.put("allSources", true);
        xorRule = new XOrRule("testXOrRule", properties, mockResults);
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
    void predicate_withTwoTrue_shouldReturnFalse() {
        ITemporalData series1 = createBooleanSeries("s1", 100L, true);
        ITemporalData series2 = createBooleanSeries("s2", 101L, true);
        ITemporalData[] input = new ITemporalData[]{series1, series2};

        BooleanSingle[] result = xorRule.predicate().apply(input);

        assertEquals(1, result.length);
        assertFalse(result[0].value());
    }

    @Test
    void predicate_withOneTrueOneFalse_shouldReturnTrue() {
        ITemporalData series1 = createBooleanSeries("s1", 100L, true);
        ITemporalData series2 = createBooleanSeries("s2", 101L, false);
        ITemporalData[] input = new ITemporalData[]{series1, series2};

        BooleanSingle[] result = xorRule.predicate().apply(input);

        assertEquals(1, result.length);
        assertTrue(result[0].value());
    }

    @Test
    void predicate_withTwoFalse_shouldReturnFalse() {
        ITemporalData series1 = createBooleanSeries("s1", 100L, false);
        ITemporalData series2 = createBooleanSeries("s2", 101L, false);
        ITemporalData[] input = new ITemporalData[]{series1, series2};

        BooleanSingle[] result = xorRule.predicate().apply(input);

        assertEquals(1, result.length);
        assertFalse(result[0].value());
    }

    @Test
    void predicate_withThreeTrue_shouldReturnTrue() {
        ITemporalData series1 = createBooleanSeries("s1", 100L, true);
        ITemporalData series2 = createBooleanSeries("s2", 101L, true);
        ITemporalData series3 = createBooleanSeries("s3", 102L, true);
        ITemporalData[] input = new ITemporalData[]{series1, series2, series3};

        BooleanSingle[] result = xorRule.predicate().apply(input);

        assertEquals(1, result.length);
        assertTrue(result[0].value());
    }

    @Test
    void predicate_withTwoTrueOneFalse_shouldReturnFalse() {
        ITemporalData series1 = createBooleanSeries("s1", 100L, true);
        ITemporalData series2 = createBooleanSeries("s2", 101L, true);
        ITemporalData series3 = createBooleanSeries("s3", 102L, false);
        ITemporalData[] input = new ITemporalData[]{series1, series2, series3};

        BooleanSingle[] result = xorRule.predicate().apply(input);

        assertEquals(1, result.length);
        assertFalse(result[0].value());
    }

    @Test
    void predicate_withNoSources_shouldReturnEmptyArray() {
        xorRule.setAllSources(false);
        ITemporalData[] input = new ITemporalData[]{};

        BooleanSingle[] result = xorRule.predicate().apply(input);

        assertEquals(0, result.length);
    }
}
