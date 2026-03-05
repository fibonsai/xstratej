/*
 *  Copyright (c) 2025 fibonsai.com
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

package com.fibonsai.cryptomeria.xtratej.event;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public interface ITemporalData extends Comparable<ITemporalData>, Serializable {
    long timestamp();

    @Override
    default int compareTo(ITemporalData o) {
        return Long.compare(timestamp(), o.timestamp());
    }

    default long convertTimestamp(LocalDateTime dateTime, ZoneOffset zoneOffset) {
        return convertTimestamp(dateTime.toInstant(zoneOffset));
    }

    default long convertTimestamp(Instant instant) {
        return instant.toEpochMilli();
    }
}
