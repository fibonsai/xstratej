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

package com.fibonsai.cryptomeria.xtratej.engine.rules;

import com.fibonsai.cryptomeria.xtratej.event.reactive.DirectFlux;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.BooleanTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.EmptyTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.TimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.builders.BooleanTimeSeriesBuilder;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.builders.DoubleTimeSeriesBuilder;
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
    static class TestBooleanRuleStream extends RuleStream<BooleanTimeSeries> {
        private Function<TimeSeries[], BooleanTimeSeries[]> predicateFunction;

        protected TestBooleanRuleStream(JsonNode params) {
            this.predicateFunction = _ -> new BooleanTimeSeries[0]; // Default empty
            setParams(params);
        }

        @Override
        protected Function<TimeSeries[], BooleanTimeSeries[]> predicate() {
            return predicateFunction;
        }

        public void setPredicateFunction(Function<TimeSeries[], BooleanTimeSeries[]> predicateFunction) {
            this.predicateFunction = predicateFunction;
        }
    }

    @Test
    void execute_emitsResult() throws InterruptedException {
        JsonNode params = nodeFactory.objectNode();
        TestBooleanRuleStream ruleStream = new TestBooleanRuleStream(params);

        long timestamp = System.currentTimeMillis();
        BooleanTimeSeries[] expectedBooleanTimeSeriesArray = { new BooleanTimeSeriesBuilder().add(timestamp, true).build() };
        ruleStream.setPredicateFunction(_ -> expectedBooleanTimeSeriesArray);

        TimeSeries[] timeSeriesArray = { new DoubleTimeSeriesBuilder().add(0, 0.0).build() } ;
        var inputStream = new DirectFlux<TimeSeries[]>();
        ruleStream.watch(inputStream);
        AtomicReference<TimeSeries> result = new AtomicReference<>(EmptyTimeSeries.INSTANCE);
        CountDownLatch latch = new CountDownLatch(1);
        ruleStream.results().onSubscribe(latch::countDown).subscribe(result::set);
        //noinspection ResultOfMethodCallIgnored
        latch.await(5, TimeUnit.SECONDS);

        inputStream.emitNext(timeSeriesArray);

        TimeSeries emittedSeries = result.get();
        assertNotNull(emittedSeries);
        assertInstanceOf(BooleanTimeSeries.class, emittedSeries);
        BooleanTimeSeries booleanSeries = (BooleanTimeSeries) emittedSeries;

        assertEquals(1, booleanSeries.size());
        assertEquals(expectedBooleanTimeSeriesArray[0].timestamp(), booleanSeries.timestamps()[0]);
        assertEquals(expectedBooleanTimeSeriesArray[0].values()[0], booleanSeries.values()[0]);
    }
}