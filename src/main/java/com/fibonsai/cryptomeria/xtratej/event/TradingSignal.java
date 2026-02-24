package com.fibonsai.cryptomeria.xtratej.event;

import jakarta.annotation.Nonnull;

public record TradingSignal(
        long timestamp,
        @Nonnull Signal signal,
        @Nonnull String strategyName,
        @Nonnull String pair,
        @Nonnull String source) implements ITemporalData {

    public enum Signal {
        ENTER,
        EXIT
    }
}
