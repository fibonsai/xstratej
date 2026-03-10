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

package com.fibonsai.cryptomeria.xtratej.event.series.dao.tools;

import com.fibonsai.cryptomeria.xtratej.event.series.dao.*;

public class MinMax {

    public record MinMaxResult(double min, double max) {}

    private MinMax() {}

    public static MinMaxResult from(TimeSeries timeSeries) {
        if (timeSeries.size() == 0) {
            return new MinMaxResult(Double.NaN, Double.NaN);
        }

        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;

        switch (timeSeries) {
            case SingleTimeSeries ts -> {
                for (var value : ts.values()) {
                    if (Double.isNaN(value)) continue;
                    if (value < min) {
                        min = value;
                    }
                    if (value > max) {
                        max = value;
                    }
                }
            }
            case CorrelatedTimeSeries ts -> {
                for (int x = 0; x < ts.size(); x++) {
                    double value = ts.values()[x];
                    double value2 = ts.values2()[x];
                    if (Double.isNaN(value) || Double.isNaN(value2)) {
                        continue;
                    }
                    double _max = Math.max(value, value2);
                    double _min = Math.min(value, value2);
                    if (_min < min) {
                        min = _min;
                    }
                    if (_max > max) {
                        max = _max;
                    }
                }
            }
            case BandTimeSeries ts -> {
                for (int x = 0; x < ts.size(); x++) {
                    double _upper = ts.uppers()[x];
                    double _lower = ts.lowers()[x];
                    if (Double.isNaN(_upper) || Double.isNaN(_lower)) {
                        continue;
                    }
                    if (_lower < _upper && _lower < min) {
                        min = _lower;
                    }
                    if (_upper > _lower && _upper > max) {
                        max = _upper;
                    }
                }
            }
            case BarTimeSeries ts -> {
                for (var close : ts.closes()) {
                    if (Double.isNaN(close)) continue;
                    if (close < min) {
                        min = close;
                    }
                    if (close > max) {
                        max = close;
                    }
                }
            }
            default -> {
                min = Double.NaN;
                max = Double.NaN;
            }
        }
        if (min == Double.POSITIVE_INFINITY || min == Double.NEGATIVE_INFINITY) {
            min = Double.NaN;
        }
        if (max == Double.NEGATIVE_INFINITY || max == Double.POSITIVE_INFINITY) {
            max = Double.NaN;
        }
        return new MinMaxResult(min, max);
    }
}
