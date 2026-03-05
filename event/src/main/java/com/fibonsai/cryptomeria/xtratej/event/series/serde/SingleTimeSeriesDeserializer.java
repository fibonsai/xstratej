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

package com.fibonsai.cryptomeria.xtratej.event.series.serde;

import com.fibonsai.cryptomeria.xtratej.event.series.impl.SingleTimeSeries;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;

import java.util.Iterator;

public class SingleTimeSeriesDeserializer extends StdDeserializer<SingleTimeSeries> {

    protected SingleTimeSeriesDeserializer() {
        super(SingleTimeSeries.class);
    }

    @Override
    public SingleTimeSeries deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        JsonNode node = p.objectReadContext().readTree(p);
        if (node.hasNonNull("timestamps") && node.get("timestamps").isArray()) {
            int timestampsSize = node.get("timestamps").size();
            SingleTimeSeries.Single[] singles = new SingleTimeSeries.Single[timestampsSize];
            int count = 0;
            if (node.get("values").isArray()) {
                Iterator<JsonNode> valuesIter = node.get("values").iterator();
                for (var timestampJson : node.get("timestamps")) {
                    JsonNode valueJson;
                    if ((valueJson = valuesIter.next()).isDouble() && timestampJson.isLong()) {
                        long timestamp = timestampJson.asLong();
                        double value = valueJson.asDouble();
                        singles[count++] = new SingleTimeSeries.Single(timestamp, value);
                    }
                }
                String id = node.hasNonNull("id") ? node.get("id").asString() : null;
                if (id !=null && !id.isBlank() && singles.length > 0) {
                    return new SingleTimeSeries(id, singles);
                }
            }
        }
        throw new RuntimeException("deserialization problem");
    }
}
