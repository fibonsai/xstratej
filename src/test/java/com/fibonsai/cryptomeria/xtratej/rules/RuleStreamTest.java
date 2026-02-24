package com.fibonsai.cryptomeria.xtratej.rules;

import com.fibonsai.cryptomeria.xtratej.event.reactive.Fifo;
import com.fibonsai.cryptomeria.xtratej.event.ITemporalData;
import com.fibonsai.cryptomeria.xtratej.event.series.impl.BooleanSingleTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.impl.BooleanSingleTimeSeries.BooleanSingle;
import com.fibonsai.cryptomeria.xtratej.event.series.impl.SingleTimeSeries;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.BooleanNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.*;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RuleStreamTest {

    private Fifo<ITemporalData> mockResults;
    private TestRuleStream ruleStream;
    private final JsonNodeFactory nodeFactory = JsonNodeFactory.instance;

    // Concrete implementation for testing abstract RuleStream
    static class TestRuleStream extends RuleStream {
        private Function<ITemporalData[], BooleanSingle[]> predicateFunction;

        protected TestRuleStream(String name, JsonNode properties, Fifo<ITemporalData> results) {
            super(name, properties, results);
            this.predicateFunction = _ -> new BooleanSingle[0]; // Default empty
            processProperties();
        }

        @Override
        protected Function<ITemporalData[], BooleanSingle[]> predicate() {
            return predicateFunction;
        }

        public void setPredicateFunction(Function<ITemporalData[], BooleanSingle[]> predicateFunction) {
            this.predicateFunction = predicateFunction;
        }

        // Expose for testing
        public Set<Map.Entry<String, JsonNode>> getProperties() {
            return properties;
        }

        public boolean getAllSources() {
            return allSources;
        }

        public List<String> getSourceIds() {
            return sourceIds;
        }
    }

    @BeforeEach
    void setUp() {
        //noinspection unchecked
        mockResults = mock(Fifo.class);
    }

    @Test
    void constructorAndNameMethod() {
        String ruleName = "TestRule";
        JsonNode properties = nodeFactory.objectNode();
        ruleStream = new TestRuleStream(ruleName, properties, mockResults);

        assertEquals(ruleName, ruleStream.name());
        assertNotNull(ruleStream.getProperties());
        assertTrue(ruleStream.getProperties().isEmpty());
    }

    @Test
    void processProperties_allSourcesTrue() {
        ObjectNode properties = nodeFactory.objectNode();
        properties.set("allSources", BooleanNode.TRUE);
        properties.set("sources", nodeFactory.arrayNode().add("source1")); // Should be ignored

        ruleStream = new TestRuleStream("TestRule", properties, mockResults);
        assertTrue(ruleStream.getAllSources());
        assertTrue(ruleStream.getSourceIds().isEmpty());
    }

    @Test
    void processProperties_allSourcesFalseAndSourcesDefined() {
        ObjectNode properties = nodeFactory.objectNode();
        properties.set("allSources", BooleanNode.FALSE);
        ArrayNode sourcesArray = nodeFactory.arrayNode();
        sourcesArray.add("sourceA");
        sourcesArray.add("sourceB");
        properties.set("sources", sourcesArray);

        ruleStream = new TestRuleStream("TestRule", properties, mockResults);
        assertFalse(ruleStream.getAllSources());
        assertEquals(Arrays.asList("sourceA", "sourceB"), ruleStream.getSourceIds());
    }

    @Test
    void processProperties_noAllSourcesAndNoSources() {
        ObjectNode properties = nodeFactory.objectNode(); // Empty properties

        ruleStream = new TestRuleStream("TestRule", properties, mockResults);
        assertTrue(ruleStream.getAllSources()); // Default is true
        assertTrue(ruleStream.getSourceIds().isEmpty());
    }

    @Test
    void getSourceIndexes_allSourcesTrue() {
        ObjectNode properties = nodeFactory.objectNode();
        properties.set("allSources", BooleanNode.TRUE);
        ruleStream = new TestRuleStream("TestRule", properties, mockResults);

        ITemporalData source1 = new SingleTimeSeries("source1");
        ITemporalData source2 = new SingleTimeSeries("source2");
        ITemporalData[] arraySeries = new ITemporalData[]{source1, source2};

        List<Integer> expectedIndexes = List.of(0, 1);
        List<Integer> actualIndexes = ruleStream.getSourceIndexes(arraySeries);
        Collections.sort(actualIndexes);

        assertEquals(expectedIndexes, actualIndexes);
    }

    @Test
    void getSourceIndexes_allSourcesFalseAndSpecificSources() {
        ObjectNode properties = nodeFactory.objectNode();
        properties.set("allSources", BooleanNode.FALSE);
        ArrayNode sourcesArray = nodeFactory.arrayNode();
        sourcesArray.add("sourceA");
        sourcesArray.add("sourceC");
        properties.set("sources", sourcesArray);
        ruleStream = new TestRuleStream("TestRule", properties, mockResults);

        ITemporalData sourceA = new SingleTimeSeries("sourceA");
        ITemporalData sourceB = new SingleTimeSeries("sourceB");
        ITemporalData sourceC = new SingleTimeSeries("sourceC");
        ITemporalData[] arraySeries = new ITemporalData[]{sourceA, sourceB, sourceC};

        List<Integer> expectedIndexes = Arrays.asList(0, 2);
        List<Integer> actualIndexes = ruleStream.getSourceIndexes(arraySeries);
        
        // Sort both lists for comparison since order may vary
        Collections.sort(expectedIndexes);
        Collections.sort(actualIndexes);

        assertEquals(expectedIndexes, actualIndexes);
    }

    @Test
    void execute_emitsResult() {
        String ruleName = "TestRule";
        JsonNode properties = nodeFactory.objectNode();
        ruleStream = new TestRuleStream(ruleName, properties, mockResults);

        long timestamp = System.currentTimeMillis();
        BooleanSingle[] expectedBooleanSingles = {new BooleanSingle(timestamp, true)};
        ruleStream.setPredicateFunction(_ -> expectedBooleanSingles);

        ITemporalData[] temporalDatas = { new SingleTimeSeries.Single(0, 0.0) } ;
        var inputStream = new Fifo<ITemporalData[]>();
        ruleStream.subscribe(inputStream);
        inputStream.emitNext(temporalDatas);

        ArgumentCaptor<ITemporalData> captor = ArgumentCaptor.forClass(ITemporalData.class);
        verify(mockResults, times(1)).emitNext(captor.capture());

        ITemporalData emittedSeries = captor.getValue();
        assertNotNull(emittedSeries);
        assertInstanceOf(BooleanSingleTimeSeries.class, emittedSeries);
        BooleanSingleTimeSeries booleanSeries = (BooleanSingleTimeSeries) emittedSeries;

        assertEquals(ruleName, booleanSeries.id());
        assertEquals(1, booleanSeries.size());
        assertEquals(expectedBooleanSingles[0].timestamp(), booleanSeries.timestamps()[0]);
        assertEquals(expectedBooleanSingles[0].value(), booleanSeries.values()[0]);
    }

    @Test
    void setAllSources() {
        ruleStream = new TestRuleStream("TestRule", nodeFactory.objectNode(), mockResults);
        assertTrue(ruleStream.getAllSources()); // Default

        ruleStream.setAllSources(false);
        assertFalse(ruleStream.getAllSources());

        ruleStream.setAllSources(true);
        assertTrue(ruleStream.getAllSources());
    }

    @Test
    void addSourceId() {
        ruleStream = new TestRuleStream("TestRule", nodeFactory.objectNode(), mockResults);
        assertTrue(ruleStream.getSourceIds().isEmpty());

        ruleStream.addSourceId("newSource");
        assertEquals(Collections.singletonList("newSource"), ruleStream.getSourceIds());

        ruleStream.addSourceId("anotherSource");
        assertEquals(Arrays.asList("newSource", "anotherSource"), ruleStream.getSourceIds());
    }

    @Test
    void equalsAndHashCode() {
        JsonNode properties = nodeFactory.objectNode();
        RuleStream rule1 = new TestRuleStream("RuleA", properties, mockResults);
        RuleStream rule2 = new TestRuleStream("RuleA", properties, mockResults);
        RuleStream rule3 = new TestRuleStream("RuleB", properties, mockResults);

        assertEquals(rule1, rule2);
        assertNotEquals(rule1, rule3);
        assertEquals(rule1.hashCode(), rule2.hashCode());
        assertNotEquals(rule1.hashCode(), rule3.hashCode());
    }
}