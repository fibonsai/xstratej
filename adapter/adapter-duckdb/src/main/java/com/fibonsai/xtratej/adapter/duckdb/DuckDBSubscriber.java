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

package com.fibonsai.xtratej.adapter.duckdb;

import com.fibonsai.xtratej.adapter.core.Subscriber;
import com.fibonsai.xtratej.adapter.core.WithParams;
import tools.jackson.databind.JsonNode;

public class DuckDBSubscriber extends Subscriber implements WithParams {

    private final DuckDBClient client = new DuckDBClient(this);

    public DuckDBSubscriber(String name, String publisher) {
        super(name, publisher);
    }

    @Override
    public Subscriber setParams(JsonNode params) {
        client.setParams(params);
        return this;
    }

    @Override
    public boolean connect() {
        if (client.connect()) {
            client.subscribe();
        }
        return client.isSubscribed();
    }

    @Override
    public boolean disconnect() {
        return client.disconnect();
    }

    @Override
    public boolean isConnected() {
        return client.isConnected();
    }

    public void onConnect(Runnable runnable) {
        client.onConnect(runnable);
    }
}
