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

package com.fibonsai.xtratej.engine.adapters;

import com.fibonsai.xtratej.adaptor.core.Publisher;
import com.fibonsai.xtratej.adaptor.simulated.SimulatedPublisher;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public enum TargetType {
    SIMULATED(SimulatedPublisher.class),
    UNDEF(null)
    ;

    private final @Nullable Class<? extends Publisher> clazz;

    TargetType(@Nullable  Class<? extends Publisher> clazz) {
        this.clazz = clazz;
    }

    public static TargetType fromName(String name) {
        for (var value: values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return UNDEF;
    }

    public Builder<? extends Publisher> builder() {
        return new Builder<>(clazz);
    }

    public static class Builder<T> {
        private final Constructor<T> constructor;
        private String name = "undef";

        public Builder(@Nullable Class<T> clazz) {
            try {
                if (clazz == null) {
                    throw new UnsupportedOperationException();
                }
                this.constructor = clazz.getConstructor(String.class);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public Builder<T> setName(String name) {
            this.name = name;
            return this;
        }

        public T build() {
            try {
                return constructor.newInstance(name);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
