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

import com.fibonsai.cryptomeria.xtratej.event.reactive.Fifo;
import com.fibonsai.cryptomeria.xtratej.event.ITemporalData;
import com.fibonsai.cryptomeria.xtratej.event.series.impl.BooleanSingleTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.impl.BooleanSingleTimeSeries.BooleanSingle;
import com.fibonsai.cryptomeria.xtratej.rules.RuleStream;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.function.Function;

public class NotRule extends RuleStream {

    private static final Logger log = LoggerFactory.getLogger(NotRule.class);

    public NotRule(@Nonnull String name, @Nonnull JsonNode properties) {
        this(name, properties, new Fifo<>());
    }

    public NotRule(@Nonnull String name, @Nonnull JsonNode properties, @Nonnull Fifo<ITemporalData> results) {
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
            if (sourceIndexes.size() > 1) {
                log.error("Multi-timeseries are not supported as a source. Only one.");
                return new BooleanSingle[0];
            }

            var ts = temporalDatas[0];
            if (ts instanceof BooleanSingleTimeSeries series) {
                int endIndex = series.size() - 1;
                boolean lastValue = series.values()[endIndex];
                long lastTimestamp = series.timestamp();
                return new BooleanSingle[] { new BooleanSingle(lastTimestamp, !lastValue) };
            }

            return new BooleanSingle[0];
        };
    }
}
