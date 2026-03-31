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

import com.fibonsai.xtratej.adapter.core.Publisher;
import com.fibonsai.xtratej.adapter.core.WithParams;
import com.fibonsai.xtratej.event.series.dao.TimeSeries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;

import java.util.function.Consumer;

public class DuckDBPublisher extends Publisher implements WithParams {

    private static final Logger log = LoggerFactory.getLogger(DuckDBPublisher.class);

    private final DuckDBClient client = new DuckDBClient(this);

    public DuckDBPublisher(String name) {
        super(name);
        subscribe(publish());
    }

    @Override
    public DuckDBPublisher setParams(JsonNode params) {
        client.setParams(params);
        return this;
    }

    private Consumer<TimeSeries> publish() {
        return timeSeries -> {
            if (isConnected()) {
                client.publish(timeSeries);
            } else {
                log.error("not connected. Ignoring {}", timeSeries);
            }
        };
    }

    @Override
    public boolean connect() {
        return client.connect();
    }

    @Override
    public boolean disconnect() {
        return client.disconnect();
    }

    @Override
    public boolean isConnected() {
        return client.isConnected();
    }
}
