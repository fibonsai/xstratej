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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

import java.util.Iterator;
import java.util.List;

public class BarTimeSeriesSerializer extends StdSerializer<BarTimeSeries> {

    private static final Logger log = LoggerFactory.getLogger(BarTimeSeriesSerializer.class);

    protected BarTimeSeriesSerializer() {
        super(BarTimeSeries.class);
    }

    @Override
    public void serialize(BarTimeSeries value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
        String id = value.id();
        long[] timestamps = value.timestamps();
        double[] opens = value.opens();
        double[] highs = value.highs();
        double[] lows = value.lows();
        double[] closes = value.closes();
        double[] volumes = value.volumes();

        gen.writeStartObject();
        gen.writeStringProperty("id", id);
        gen.writeArrayPropertyStart("timestamps");
        for (var timestamp: timestamps) {
            gen.writeNumber(timestamp);
        }
        gen.writeEndArray();

        Iterator<String> iteratorNames = List.of("opens", "highs", "lows", "closes", "volumes").iterator();
        for (var array: List.of(opens, highs, lows, closes, volumes)) {
            String key = iteratorNames.next();
            gen.writeArrayPropertyStart(key);
            for (var aDouble : array) {
                gen.writeNumber(aDouble);
            }
            gen.writeEndArray();
        }
        gen.writeEndObject();

        gen.flush();
    }
}
