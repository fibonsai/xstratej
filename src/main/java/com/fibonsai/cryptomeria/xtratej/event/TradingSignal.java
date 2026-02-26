package com.fibonsai.cryptomeria.xtratej.event;

public record TradingSignal(
        long timestamp,
        Signal signal,
        String strategyName,
        String pair,
        String source) implements ITemporalData {

    public enum Signal {
        ENTER,
        EXIT
    }
}
