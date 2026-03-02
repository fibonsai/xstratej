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

package com.fibonsai.cryptomeria.xtratej.rules;

import com.fibonsai.cryptomeria.xtratej.event.ITemporalData;
import com.fibonsai.cryptomeria.xtratej.event.reactive.Fifo;
import com.fibonsai.cryptomeria.xtratej.event.series.impl.BooleanSingleTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.impl.BooleanSingleTimeSeries.BooleanSingle;
import com.fibonsai.cryptomeria.xtratej.event.series.impl.EmptyTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.impl.SingleTimeSeries;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class RuleStreamTest {

    private final JsonNodeFactory nodeFactory = JsonNodeFactory.instance;

    // Concrete implementation for testing abstract RuleStream
    static class TestRuleStream extends RuleStream {
        private Function<ITemporalData[], BooleanSingle[]> predicateFunction;

        protected TestRuleStream(JsonNode properties) {
            super(properties);
            this.predicateFunction = _ -> new BooleanSingle[0]; // Default empty
        }

        @Override
        protected void processProperties() {}

        @Override
        protected Function<ITemporalData[], BooleanSingle[]> predicate() {
            return predicateFunction;
        }

        public void setPredicateFunction(Function<ITemporalData[], BooleanSingle[]> predicateFunction) {
            this.predicateFunction = predicateFunction;
        }
    }

    @Test
    void constructorAndNameMethod() {
        JsonNode properties = nodeFactory.objectNode();
        TestRuleStream ruleStream = new TestRuleStream(properties);

        assertNotNull(ruleStream.getProperties());
        assertTrue(ruleStream.getProperties().isEmpty());
    }

    @Test
    void execute_emitsResult() throws InterruptedException {
        JsonNode properties = nodeFactory.objectNode();
        TestRuleStream ruleStream = new TestRuleStream(properties);

        long timestamp = System.currentTimeMillis();
        BooleanSingle[] expectedBooleanSingles = {new BooleanSingle(timestamp, true)};
        ruleStream.setPredicateFunction(_ -> expectedBooleanSingles);

        ITemporalData[] temporalDatas = { new SingleTimeSeries.Single(0, 0.0) } ;
        var inputStream = new Fifo<ITemporalData[]>();
        ruleStream.subscribe(inputStream);
        AtomicReference<ITemporalData> result = new AtomicReference<>(EmptyTimeSeries.INSTANCE);
        CountDownLatch latch = new CountDownLatch(1);
        ruleStream.results().onSubscribe(latch::countDown).subscribe(result::set);
        //noinspection ResultOfMethodCallIgnored
        latch.await(5, TimeUnit.SECONDS);

        inputStream.emitNext(temporalDatas);

        ITemporalData emittedSeries = result.get();
        assertNotNull(emittedSeries);
        assertInstanceOf(BooleanSingleTimeSeries.class, emittedSeries);
        BooleanSingleTimeSeries booleanSeries = (BooleanSingleTimeSeries) emittedSeries;

        assertEquals(1, booleanSeries.size());
        assertEquals(expectedBooleanSingles[0].timestamp(), booleanSeries.timestamps()[0]);
        assertEquals(expectedBooleanSingles[0].value(), booleanSeries.values()[0]);
    }
}