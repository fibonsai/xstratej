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
import com.fibonsai.cryptomeria.xtratej.event.series.impl.BooleanSingleTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.impl.BooleanSingleTimeSeries.BooleanSingle;
import com.fibonsai.cryptomeria.xtratej.rules.RuleStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

public class OrRule extends RuleStream {

    private static final Logger log = LoggerFactory.getLogger(OrRule.class);

    @Override
    protected Function<ITemporalData[], BooleanSingle[]> predicate() {
        return temporalDatas -> {
            if (!isActivated()) {
                log.warn("No sources. Ignoring rule.");
                return new BooleanSingle[0];
            }

            Boolean result = null;
            long timestamp = 0L;
            for (var ts : temporalDatas) {
                if (ts instanceof BooleanSingleTimeSeries series && series.size() > 0) {
                    if (series.timestamp() > timestamp) timestamp = series.timestamp();
                    for (boolean bool : series.values()) {
                        result = (result == null) ? bool : result || bool;
                    }
                }
            }

            return new BooleanSingle[]{new BooleanSingle(timestamp, result != null && result)};
        };
    }

    @Override
    protected void processProperties() {}
}
