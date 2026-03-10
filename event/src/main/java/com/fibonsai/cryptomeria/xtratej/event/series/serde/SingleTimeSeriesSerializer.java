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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

@Deprecated(forRemoval = true)
public class SingleTimeSeriesSerializer extends StdSerializer<SingleTimeSeries> {

    private static final Logger log = LoggerFactory.getLogger(SingleTimeSeriesSerializer.class);

    protected SingleTimeSeriesSerializer() {
        super(SingleTimeSeries.class);
    }

    @Override
    public void serialize(SingleTimeSeries value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
        String id = value.id();
        long[] timestamps = value.timestamps();
        double[] values = value.values();

        gen.writeStartObject();
        gen.writeStringProperty("id", id);
        gen.writeArrayPropertyStart("timestamps");
        for (var timestamp: timestamps) {
            gen.writeNumber(timestamp);
        }
        gen.writeEndArray();
        gen.writeArrayPropertyStart("values");
        for (var aDouble: values) {
            gen.writeNumber(aDouble);
        }
        gen.writeEndArray();
        gen.writeEndObject();

        gen.flush();
    }
}
