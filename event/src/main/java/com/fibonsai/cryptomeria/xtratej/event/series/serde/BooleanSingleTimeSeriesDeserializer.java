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

import com.fibonsai.cryptomeria.xtratej.event.series.impl.BooleanSingleTimeSeries;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;

import java.util.Iterator;

public class BooleanSingleTimeSeriesDeserializer extends StdDeserializer<BooleanSingleTimeSeries> {

    protected BooleanSingleTimeSeriesDeserializer() {
        super(BooleanSingleTimeSeries.class);
    }

    @Override
    public BooleanSingleTimeSeries deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        JsonNode node = p.objectReadContext().readTree(p);
        if (node.hasNonNull("timestamps") && node.get("timestamps").isArray()) {
            int timestampsSize = node.get("timestamps").size();
            BooleanSingleTimeSeries.BooleanSingle[] booleanSingles = new BooleanSingleTimeSeries.BooleanSingle[timestampsSize];
            int count = 0;
            if (node.get("values").isArray()) {
                Iterator<JsonNode> valuesIter = node.get("values").iterator();
                for (var timestampJson : node.get("timestamps")) {
                    JsonNode valueJson;
                    if ((valueJson = valuesIter.next()).isBoolean() && timestampJson.isLong()) {
                        long timestamp = timestampJson.asLong();
                        boolean value = valueJson.asBoolean();
                        booleanSingles[count++] = new BooleanSingleTimeSeries.BooleanSingle(timestamp, value);
                    }
                }
                String id = node.hasNonNull("id") ? node.get("id").asString() : null;
                if (id !=null && !id.isBlank() && booleanSingles.length > 0) {
                    return new BooleanSingleTimeSeries(id, booleanSingles);
                }
            }
        }
        throw new RuntimeException("deserialization problem");
    }
}
