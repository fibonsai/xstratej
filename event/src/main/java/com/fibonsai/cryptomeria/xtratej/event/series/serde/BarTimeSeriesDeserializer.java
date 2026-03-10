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

import com.fibonsai.cryptomeria.xtratej.event.series.impl.BarTimeSeries;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;

import java.util.Iterator;

@Deprecated(forRemoval = true)
public class BarTimeSeriesDeserializer extends StdDeserializer<BarTimeSeries> {

    protected BarTimeSeriesDeserializer() {
        super(BarTimeSeries.class);
    }

    @Override
    public BarTimeSeries deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        JsonNode node = p.objectReadContext().readTree(p);
        if (node.hasNonNull("timestamps") && node.get("timestamps").isArray()) {
            int timestampsSize = node.get("timestamps").size();
            BarTimeSeries.Bar[] bars = new BarTimeSeries.Bar[timestampsSize];
            int count = 0;
            if (node.get("opens").isArray() && node.get("highs").isArray() && node.get("lows").isArray() && node.get("closes").isArray() && node.get("volumes").isArray()) {
                Iterator<JsonNode> opensIter = node.get("opens").iterator();
                Iterator<JsonNode> highsIter = node.get("highs").iterator();
                Iterator<JsonNode> lowsIter = node.get("lows").iterator();
                Iterator<JsonNode> closesIter = node.get("closes").iterator();
                Iterator<JsonNode> volumesIter = node.get("volumes").iterator();
                for (var timestampJson : node.get("timestamps")) {
                    JsonNode openJson;
                    JsonNode highJson;
                    JsonNode lowJson;
                    JsonNode closeJson;
                    JsonNode volumeJson;
                    if ((openJson = opensIter.next()).isDouble() &&
                        (highJson = highsIter.next()).isDouble() &&
                        (lowJson = lowsIter.next()).isDouble() &&
                        (closeJson = closesIter.next()).isDouble() &&
                        (volumeJson = volumesIter.next()).isDouble() &&
                        timestampJson.isLong()) {

                        long timestamp = timestampJson.asLong();
                        double open = openJson.asDouble();
                        double high = highJson.asDouble();
                        double low = lowJson.asDouble();
                        double close = closeJson.asDouble();
                        double volume = volumeJson.asDouble();
                        bars[count++] = new BarTimeSeries.Bar(timestamp, open, high, low, close, volume);
                    }
                }
                String id = node.hasNonNull("id") ? node.get("id").asString() : null;
                if (id !=null && !id.isBlank() && bars.length > 0) {
                    return new BarTimeSeries(id, bars);
                }
            }
        }
        throw new RuntimeException("deserialization problem");
    }
}
