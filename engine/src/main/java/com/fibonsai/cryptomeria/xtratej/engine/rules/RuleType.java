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

import com.fibonsai.cryptomeria.xtratej.engine.rules.impl.*;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.TimeSeries;

import java.util.function.Supplier;

public enum RuleType {
    And(AndRule::new),
    Crossed(CrossedRule::new),
    DateTime(DateTimeRule::new),
    False(FalseRule::new),
    InSlope(InSlopeRule::new),
    Limit(LimitRule::new),
    Not(NotRule::new),
    Or(OrRule::new),
    Random(RandomRule::new),
    Time(TimeRule::new),
    Trend(TrendRule::new),
    Weekday(WeekdayRule::new),
    XOr(XOrRule::new)
    ;

    private final Supplier<? extends RuleStream<? extends TimeSeries>> supplier;

    RuleType(Supplier<? extends RuleStream<? extends TimeSeries>> supplier) {
        this.supplier = supplier;
    }

    public static RuleType fromName(String name) {
        for (var value: values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        throw new RuntimeException("%s not exist".formatted(name));
    }

    public RuleStream<? extends TimeSeries> build() {
        return supplier.get();
    }
}
