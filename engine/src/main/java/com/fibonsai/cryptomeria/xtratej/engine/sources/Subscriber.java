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

package com.fibonsai.cryptomeria.xtratej.engine.sources;

import com.fibonsai.cryptomeria.xtratej.event.reactive.Fifo;
import com.fibonsai.cryptomeria.xtratej.event.series.dao.TimeSeries;

public abstract class Subscriber {
    private final String name;
    private final String publisher;
    private final Fifo<TimeSeries> fifo = new Fifo<>();

    public Subscriber(String name, String publisher) {
        this.name = name;
        this.publisher = publisher;
    }

    public String name() {
        return name;
    }

    public String publisher() {
        return publisher;
    }

    public Fifo<TimeSeries> toFifo() {
        return this.fifo;
    }

    public abstract boolean connect();
    public abstract boolean disconnect();
    public abstract boolean isConnected();
}
