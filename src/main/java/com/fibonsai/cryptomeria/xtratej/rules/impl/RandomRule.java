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

package com.fibonsai.cryptomeria.xtratej.rules.impl;

import com.fibonsai.cryptomeria.xtratej.event.ITemporalData;
import com.fibonsai.cryptomeria.xtratej.event.reactive.Fifo;
import com.fibonsai.cryptomeria.xtratej.event.series.impl.BooleanSingleTimeSeries.BooleanSingle;
import com.fibonsai.cryptomeria.xtratej.rules.RuleStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

public class RandomRule extends RuleStream {

    private static final Logger log = LoggerFactory.getLogger(RandomRule.class);

    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    public RandomRule(String name, JsonNode properties) {
        this(name, properties, new Fifo<>());
    }

    public RandomRule(String name, JsonNode properties, Fifo<ITemporalData> results) {
        super(name, properties, results);
        processProperties();
    }

    @Override
    protected Function<ITemporalData[], BooleanSingle[]> predicate() {
        return temporalDatas -> {
            List<Integer> sourceIndexes = getSourceIndexes(temporalDatas);
            if (sourceIndexes.isEmpty()) {
                log.warn("No sources. Ignoring rule.");
                return new BooleanSingle[0];
            }

            boolean result = random.nextBoolean();
            long timestamp = 0L;
            int count = 0;
            for (var ts: temporalDatas) {
                if ((allSources || sourceIndexes.contains(count++)) && ts.timestamp() > timestamp) {
                    timestamp = ts.timestamp();
                }
            }

            return new BooleanSingle[] { new BooleanSingle(timestamp, result) };
        };
    }
}
