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

package com.fibonsai.cryptomeria.xtratej.engine.targets;

import com.fibonsai.cryptomeria.xtratej.event.reactive.Fifo;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.TradingSignal;

public abstract class Publisher {

    public static final Publisher NULL = new Publisher("NULL") {

        @Override
        public Fifo<TradingSignal> toFifo() {
            return Fifo.empty();
        }

        @Override
        public boolean connect() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean disconnect() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isConnected() {
            throw new UnsupportedOperationException();
        }
    };

    private final String name;

    private final Fifo<TradingSignal> fifo = new Fifo<>();

    public Publisher(String name) {
        this.name = name;
    }

    public Fifo<TradingSignal> toFifo() {
        return this.fifo;
    }

    public String name() {
        return name;
    }

    public abstract boolean connect();
    public abstract boolean disconnect();
    public abstract boolean isConnected();
}
