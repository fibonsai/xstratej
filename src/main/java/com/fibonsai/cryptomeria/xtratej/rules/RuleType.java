package com.fibonsai.cryptomeria.xtratej.rules;

import com.fibonsai.cryptomeria.xtratej.event.ITemporalData;
import com.fibonsai.cryptomeria.xtratej.event.reactive.Fifo;
import com.fibonsai.cryptomeria.xtratej.rules.impl.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public enum RuleType {
    And(AndRule.class),
    Crossed(CrossedRule.class),
    DateTime(DateTimeRule.class),
    InSlope(InSlopeRule.class),
    Limit(LimitRule.class),
    Not(NotRule.class),
    Or(OrRule.class),
    Random(RandomRule.class),
    Time(TimeRule.class),
    Trend(TrendRule.class),
    Weekday(WeekdayRule.class),
    XOr(XOrRule.class)
    ;

    private final Class<? extends RuleStream> clazz;

    RuleType(Class<? extends RuleStream> clazz) {
        this.clazz = clazz;
    }

    public Builder<? extends RuleStream> builder() {
        return new Builder<>(clazz);
    }

    public static class Builder<T> {
        private final JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
        private final Constructor<? extends RuleStream> constructor;
        private String id = "undef";
        private JsonNode properties = nodeFactory.nullNode();
        private Fifo<ITemporalData> results = new Fifo<>();

        public Builder(Class<? extends RuleStream> clazz) {
            try {
                this.constructor = clazz.getConstructor(String.class, JsonNode.class, Fifo.class);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        public Builder<T> setProperties(JsonNode properties) {
            this.properties = properties;
            return this;
        }

        public Builder<T> setId(String id) {
            this.id = id;
            return this;
        }

        public Builder<T> setResults(Fifo<ITemporalData> results) {
            this.results = results;
            return this;
        }

        @SuppressWarnings("unchecked")
        public T build() {
            try {
                return (T) constructor.newInstance(id, properties, results);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
