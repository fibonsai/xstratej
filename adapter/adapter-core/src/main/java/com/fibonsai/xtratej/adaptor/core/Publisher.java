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

package com.fibonsai.xtratej.adaptor.core;

import com.fibonsai.directflux.DirectFlux;
import com.fibonsai.xtratej.event.series.dao.TradingSignal;

public abstract class Publisher extends DirectFlux<TradingSignal> implements Adapter {

    private final String name;

    public Publisher(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public abstract boolean connect();
    public abstract boolean disconnect();
    public abstract boolean isConnected();
}
