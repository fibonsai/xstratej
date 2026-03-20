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

package com.fibonsai.cryptomeria.xtratej.event.series.dao.builders;

import com.fibonsai.cryptomeria.xtratej.event.series.dao.BalanceUpdateTimeSeries;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.BalanceUpdateTimeSeries.UpdateCause;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.TimeSeries;

import java.util.Arrays;
import java.util.Comparator;

public class BalanceUpdateTimeSeriesBuilder extends TimeSeriesBuilder<BalanceUpdateTimeSeriesBuilder> {

    private record Element(long timestamp,
                           String symbol,
                           String owner,
                           UpdateCause updateCause,
                           double total,
                           double available,
                           double frozen,
                           double borrowed,
                           double loaned,
                           double withdrawing,
                           double depositing,
                           int scale) {}

    private Element[] elements = new Element[0];

    public BalanceUpdateTimeSeriesBuilder add(long timestamp,
              String symbol,
              String owner,
              UpdateCause updateCause,
              double total,
              double available,
              double frozen,
              double borrowed,
              double loaned,
              double withdrawing,
              double depositing,
              int scale) {

        if (total < 0.0 || available < 0.0 || frozen < 0.0 || borrowed < 0.0 || loaned < 0.0 || withdrawing < 0.0 || depositing < 0.0) {
            throw new RuntimeException("negative value is not allowed.");
        }

        writeLock.lock();
        try {
            Element element = new Element(timestamp, symbol, owner, updateCause, total, available, frozen, borrowed, loaned, withdrawing, depositing, scale);
            if (elements.length >= maxSize) {
                Arrays.sort(elements, Comparator.comparingLong(Element::timestamp));
                this.elements[0] = element;
            } else {
                Element[] newElements = new Element[elements.length + 1];
                System.arraycopy(elements, 0, newElements, 0, elements.length);
                newElements[elements.length] = element;
                this.elements = newElements;
            }
        } finally {
            writeLock.unlock();

        }
        return this;
    }

    @Override
    public BalanceUpdateTimeSeries build() {
        readLock.lock();
        try {
            long[] _timestamps = new long[elements.length];
            String[] _symbols = new String[elements.length];
            String[] _owners = new String[elements.length];
            UpdateCause[] _updateCauses = new UpdateCause[elements.length];
            double[] _totals = new double[elements.length];
            double[] _availables = new double[elements.length];
            double[] _frozens = new double[elements.length];
            double[] _borroweds = new double[elements.length];
            double[] _loaneds = new double[elements.length];
            double[] _withdrawings = new double[elements.length];
            double[] _depositings = new double[elements.length];
            int[] _scales = new int[elements.length];
            Arrays.sort(elements, Comparator.comparingLong(Element::timestamp));
            int count = 0;
            for (var element: elements) {
                _timestamps[count] = element.timestamp();
                //timestamp, symbol, owner, updateCause, total, available, frozen, borrowed, loaned, withdrawing, depositing, scale
                _symbols[count] = element.symbol();
                _owners[count] = element.owner();
                _updateCauses[count] = element.updateCause();
                _totals[count] = element.total();
                _availables[count] = element.available();
                _frozens[count] = element.frozen();
                _borroweds[count] = element.borrowed();
                _loaneds[count] = element.loaned();
                _withdrawings[count] = element.withdrawing();
                _depositings[count] = element.depositing();
                _scales[count] = element.scale();
                count++;
            }
            return new BalanceUpdateTimeSeries(id, _timestamps, _symbols, _owners, _updateCauses, _totals, _availables, _frozens, _borroweds, _loaneds, _withdrawings, _depositings, _scales);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public BalanceUpdateTimeSeriesBuilder from(TimeSeries timeSeries) {
        if (timeSeries instanceof BalanceUpdateTimeSeries(String id1,
                                                          long[] timestamps,
                                                          String[] symbols,
                                                          String[] owners,
                                                          UpdateCause[] updateCauses,
                                                          double[] totals,
                                                          double[] availables,
                                                          double[] frozens,
                                                          double[] borroweds,
                                                          double[] loaneds,
                                                          double[] withdrawings,
                                                          double[] depositings,
                                                          int[] scales)) {
            for (int x = 0; x < timestamps.length; x++) {
                add(timestamps[x], symbols[x], owners[x], updateCauses[x], totals[x], availables[x], frozens[x], borroweds[x], loaneds[x], withdrawings[x], depositings[x], scales[x]);
                setId(id1);
            }
        } else {
            throw new UnsupportedOperationException("%s not supported".formatted(timeSeries.getClass().getSimpleName()));
        }
        return this;
    }

    @Override
    public BalanceUpdateTimeSeriesBuilder merge(TimeSeries... timeSeriesArray) {
        for (var timeSeries: timeSeriesArray) {
            from(timeSeries);
        }
        return this;
    }
}
