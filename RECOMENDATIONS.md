# Recommendations for xtratej

This document provides detailed recommendations for improving and extending the xtratej codebase, based on comprehensive analysis of the source code, patterns, and architecture.

---

## Table of Contents

1. [Rule Implementation Recommendations](#rule-implementation-recommendations)
2. [Event Stream Architecture](#event-stream-architecture)
3. [Configuration and Validation](#configuration-and-validation)
4. [Testing Best Practices](#testing-best-practices)
5. [Performance Considerations](#performance-considerations)
6. [Error Handling and Resilience](#error-handling-and-resilience)
7. [Architecture Patterns](#architecture-patterns)

---

## Rule Implementation Recommendations

### 1. Always Implement `setParams()` with Validation

All rule implementations should validate their configuration parameters. The current implementation in `LimitRule` lacks validation, which could lead to runtime errors or unexpected behavior.

**Current Implementation (LimitRule.java:40-48):**
```java
@Override
public RuleStream<BooleanTimeSeries> setParams(JsonNode params) {
    for (var e: params.properties()) {
        if ("min".equals(e.getKey()) && e.getValue().isDouble()) min = e.getValue().asDouble();
        if ("max".equals(e.getKey()) && e.getValue().isDouble()) max = e.getValue().asDouble();
        if ("upperSourceId".equals(e.getKey()) && e.getValue().isString()) upperSourceId = e.getValue().asString();
        if ("lowerSourceId".equals(e.getKey()) && e.getValue().isString()) lowerSourceId = e.getValue().asString();
    }
    return this;
}
```

**Recommended Implementation:**
```java
@Override
public RuleStream<BooleanTimeSeries> setParams(JsonNode params) {
    for (var e : params.properties()) {
        String key = e.getKey();
        JsonNode value = e.getValue();

        switch (key) {
            case "min" -> {
                if (!value.isNumber()) {
                    throw new IllegalArgumentException("LimitRule.min must be a number");
                }
                min = value.asDouble();
            }
            case "max" -> {
                if (!value.isNumber()) {
                    throw new IllegalArgumentException("LimitRule.max must be a number");
                }
                max = value.asDouble();
            }
            case "upperSourceId" -> {
                if (!value.isTextual()) {
                    throw new IllegalArgumentException("LimitRule.upperSourceId must be a string");
                }
                upperSourceId = value.asText();
            }
            case "lowerSourceId" -> {
                if (!value.isTextual()) {
                    throw new IllegalArgumentException("LimitRule.lowerSourceId must be a string");
                }
                lowerSourceId = value.asText();
            }
            default -> {
                log.warn("Unknown parameter '{}' for LimitRule, ignoring", key);
            }
        }
    }

    // Validate cross-parameter constraints
    if (min > max) {
        log.warn("LimitRule: min ({}) > max ({}), swapping values", min, max);
        double temp = min;
        min = max;
        max = temp;
    }

    return this;
}
```

### 2. Prefer Immutable Rules for Predictable Behavior

Current rules are mutable after creation. This can cause issues in complex strategies where rules might be reused or reconfigured. Consider making rules immutable:

```java
public class LimitRule extends RuleStream<BooleanTimeSeries> {

    // Final fields for immutability
    private final double min;
    private final double max;
    private final String upperSourceId;
    private final String lowerSourceId;

    // Private constructor - use factory method
    private LimitRule(double min, double max, String upperSourceId, String lowerSourceId) {
        this.min = min;
        this.max = max;
        this.upperSourceId = upperSourceId;
        this.lowerSourceId = lowerSourceId;
    }

    // Factory method for new instances
    public static LimitRule of(double min, double max) {
        return new LimitRule(min, max, "", "");
    }

    // Factory method with fluent configuration
    public static LimitRuleBuilder builder() {
        return new LimitRuleBuilder();
    }

    @Override
    protected Function<TimeSeries[], BooleanTimeSeries[]> predicate() {
        return timeSeriesArray -> {
            // Rule logic using final fields
            // ... implementation
        };
    }

    // Builder for fluent configuration
    public static class LimitRuleBuilder {
        private double min = Double.NEGATIVE_INFINITY;
        private double max = Double.POSITIVE_INFINITY;
        private String upperSourceId = "";
        private String lowerSourceId = "";

        public LimitRuleBuilder setMin(double min) {
            this.min = min;
            return this;
        }

        public LimitRuleBuilder setMax(double max) {
            this.max = max;
            return this;
        }

        public LimitRuleBuilder setUpperSourceId(String upperSourceId) {
            this.upperSourceId = upperSourceId;
            return this;
        }

        public LimitRuleBuilder setLowerSourceId(String lowerSourceId) {
            this.lowerSourceId = lowerSourceId;
            return this;
        }

        public LimitRule build() {
            return new LimitRule(min, max, upperSourceId, lowerSourceId);
        }
    }
}
```

### 3. Handle Null Safety in Predicate Methods

All predicate methods should handle edge cases gracefully. The `NotRule` implementation already shows good practice with its single-input check.

**Good example from NotRule.java:38-41:**
```java
if (timeSeriesArray.length > 1) {
    log.error("Multi-timeseries are not supported as a source. Only one.");
    return new BooleanTimeSeries[0];
}
```

Apply similar defensive checks to all rules.

### 4. Use Helper Classes for Complex Logic

Complex calculations (like regression in `TrendRule`) should be extracted to helper classes:

```java
public final class SlopeCalculator {

    private SlopeCalculator() {
        throw new IllegalStateException("Utility class");
    }

    public static double calculate(DoubleTimeSeries series) {
        if (series.size() < 2) {
            return 0.0;
        }

        SimpleRegression regression = new SimpleRegression();
        for (int i = 0; i < series.size(); i++) {
            regression.addData(series.timestamps()[i], series.values()[i]);
        }
        return regression.getSlope();
    }

    public static boolean isRising(DoubleTimeSeries series) {
        return calculate(series) > 0;
    }

    public static boolean isFalling(DoubleTimeSeries series) {
        return calculate(series) < 0;
    }
}
```

This enables better testing and reuse.

### 5. Document Rule Behavior in Class-Level Javadoc

Each rule should document its behavior clearly:

```java
/**
 * Logical AND rule that evaluates to {@code true} only when ALL input rules
 * evaluate to {@code true}.
 *
 * <p>Behavior:
 * <ul>
 *   <li>If no inputs are provided, returns {@code false}</li>
 *   <li>If any input is {@code false}, returns {@code false}</li>
 *   <li>If all inputs are {@code true}, returns {@code true}</li>
 *   <li>Null values from inputs are treated as {@code false}</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * AndRule andRule = (AndRule) RuleType.And.build();
 * andRule.watch(rule1, rule2, rule3);
 * }</pre>
 *
 * @see OrRule
 * @see NotRule
 */
public class AndRule extends RuleStream<BooleanTimeSeries> {
    // ...
}
```

---

## Event Stream Architecture

### 1. Add Backpressure Mechanism to Fifo

The current `Fifo.emitNext()` uses a 10-second latch timeout but has no backpressure mechanism. If subscribers are slow, events accumulate without any control.

**Recommended Addition to Fifo.java:**

```java
public class Fifo<T> {

    private final int maxBufferSize;
    private final BlockingQueue<T> buffer;

    public Fifo() {
        this(Integer.MAX_VALUE);  // Default no limit
    }

    public Fifo(int maxBufferSize) {
        this.maxBufferSize = maxBufferSize;
        this.buffer = new ArrayBlockingQueue<>(maxBufferSize);
    }

    /**
     * Emits an event to all subscribers with optional backpressure handling.
     *
     * @param event the event to emit
     * @param timeout how long to wait if buffer is full (-1 for infinite, 0 for non-blocking)
     * @return true if event was emitted, false if dropped due to backpressure
     */
    public boolean emitNext(T event, long timeout, TimeUnit unit) throws InterruptedException {
        if (maxBufferSize < Integer.MAX_VALUE) {
            if (timeout == 0) {
                if (!buffer.offer(event)) {
                    log.warn("Buffer full, dropping event");
                    return false;
                }
            } else if (timeout > 0) {
                if (!buffer.offer(event, timeout, unit)) {
                    log.warn("Buffer full after timeout, dropping event");
                    return false;
                }
            } else {
                buffer.put(event);  // Blocking
            }
        }

        readLock.lock();
        try {
            CountDownLatch latch = new CountDownLatch(consumers.size());
            for (var consumer : consumers) {
                Thread.startVirtualThread(() -> {
                    try {
                        consumer.accept(event);
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
            return false;
        } finally {
            readLock.unlock();
        }
        return true;
    }
}
```

### 2. Consider Adding a Filter Operator

Add a filter method to `RuleStream` for filtering events before processing:

```java
public class RuleStream<T extends TimeSeries> {

    private Fifo<TimeSeries> filteredResults;
    private Predicate<TimeSeries> filterPredicate = ts -> true;

    /**
     * Creates a filtered view of this rule's results.
     */
    public RuleStream<T> filter(Predicate<TimeSeries> predicate) {
        RuleStream<T> filtered = new FilteredRuleStream<>(this, predicate);
        return filtered;
    }

    private static class FilteredRuleStream<S extends TimeSeries> extends RuleStream<S> {
        private final RuleStream<S> upstream;
        private final Predicate<TimeSeries> predicate;

        FilteredRuleStream(RuleStream<S> upstream, Predicate<TimeSeries> predicate) {
            this.upstream = upstream;
            this.predicate = predicate;
            upstream.results().subscribe(ts -> {
                if (predicate.test(ts)) {
                    results().emitNext(ts);
                }
            });
        }

        @Override
        protected Function<TimeSeries[], S[]> predicate() {
            return upstream.predicate();
        }
    }
}
```

### 3. Implement a Zip with Custom Tolerance

The current `Fifo.zip()` has a fixed 10-second tolerance. Allow configuration:

```java
public class Fifo<T> {

    /**
     * Zip with custom tolerance and optional "sticky" behavior.
     *
     * @param delayToleration the maximum time difference between aligned events
     * @param sticky if true, use last known value for missing sources
     * @param directFluxes the source directFlux to zip
     */
    @SafeVarargs
    public static <T> Fifo<T[]> zip(
            Duration delayToleration,
            boolean sticky,
            Fifo<T>... directFluxes) {
        // Implementation with sticky behavior
        // ...
    }
}
```

---

## Configuration and Validation

### 1. Add Schema Validation for JSON Configuration

Create a JSON schema validator for strategy configurations:

```java
public class StrategyValidator {

    private static final JsonSchemaFactory SCHEMA_FACTORY =
        JsonSchemaFactory.byDefault();

    private static final Map<String, JsonNode> RULE_SCHEMAS = Map.of(
        "Limit", loadSchema("/schemas/rule-limit.json"),
        "And", loadSchema("/schemas/rule-boolean.json"),
        "Or", loadSchema("/schemas/rule-boolean.json"),
        "Not", loadSchema("/schemas/rule-not.json"),
        "Crossed", loadSchema("/schemas/rule-crossed.json"),
        "Trend", loadSchema("/schemas/rule-trend.json")
    );

    private static JsonNode loadSchema(String path) {
        try (InputStream is = StrategyValidator.class.getResourceAsStream(path)) {
            return new ObjectMapper().readTree(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load schema: " + path, e);
        }
    }

    public static void validateStrategy(JsonNode strategyNode) throws ValidationException {
        if (!strategyNode.has("rule") || !strategyNode.get("rule").has("type")) {
            throw new ValidationException("Strategy must have a rule with type");
        }

        String ruleType = strategyNode.get("rule").get("type").asText();
        JsonNode ruleSchema = RULE_SCHEMAS.get(ruleType);

        if (ruleSchema == null) {
            throw new ValidationException("Unknown rule type: " + ruleType);
        }

        JsonSchema schema = SCHEMA_FACTORY.getSchema(ruleSchema);
        Set<ValidationMessage> errors = schema.validate(strategyNode.get("rule"));

        if (!errors.isEmpty()) {
            throw new ValidationException(
                "Rule validation failed: " + errors.stream()
                    .map(ValidationMessage::getMessage)
                    .collect(Collectors.joining("; "))
            );
        }
    }
}
```

### 2. Add Builder Validation

Validate builder configurations before building:

```java
public class DoubleTimeSeriesBuilder extends TimeSeriesBuilder<DoubleTimeSeriesBuilder> {

    public DoubleTimeSeries build() {
        readLock.lock();
        try {
            // Validate minimum requirements
            if (doubles.isEmpty()) {
                log.warn("Building EmptyTimeSeries - no values added");
                return EmptyTimeSeries.INSTANCE;
            }

            // Validate timestamps are in ascending order
            if (!isTimestampsOrdered()) {
                throw new IllegalStateException(
                    "Timestamps must be in ascending order. " +
                    "Last timestamp was " + doubles.lastKey());
            }

            long[] _timestamps = doubles.sequencedKeySet().stream()
                .mapToLong(Long::longValue).toArray();
            double[] _doubles = doubles.sequencedValues().stream()
                .mapToDouble(Double::doubleValue).toArray();
            return new DoubleTimeSeries(id, _timestamps, _doubles);
        } finally {
            readLock.unlock();
        }
    }

    private boolean isTimestampsOrdered() {
        long prev = Long.MIN_VALUE;
        for (long ts : doubles.keySet()) {
            if (ts < prev) {
                return false;
            }
            prev = ts;
        }
        return true;
    }
}
```

### 3. Add Parameter Mapping Utilities

Create utilities for common parameter conversions:

```java
public final class Params {

    private Params() {
        throw new IllegalStateException("Utility class");
    }

    public static double getDouble(JsonNode params, String key, double defaultValue) {
        if (!params.has(key)) {
            return defaultValue;
        }
        JsonNode value = params.get(key);
        if (!value.isNumber()) {
            throw new IllegalArgumentException(key + " must be a number, got " + value.getNodeType());
        }
        return value.asDouble();
    }

    public static int getInt(JsonNode params, String key, int defaultValue) {
        if (!params.has(key)) {
            return defaultValue;
        }
        JsonNode value = params.get(key);
        if (!value.isIntegralNumber()) {
            throw new IllegalArgumentException(key + " must be an integer, got " + value.getNodeType());
        }
        return value.asInt();
    }

    public static String getString(JsonNode params, String key, String defaultValue) {
        if (!params.has(key)) {
            return defaultValue;
        }
        JsonNode value = params.get(key);
        if (!value.isTextual()) {
            throw new IllegalArgumentException(key + " must be a string, got " + value.getNodeType());
        }
        return value.asText();
    }

    public static List<String> getStringList(JsonNode params, String key, List<String> defaultValue) {
        if (!params.has(key)) {
            return defaultValue;
        }
        JsonNode value = params.get(key);
        if (!value.isArray()) {
            throw new IllegalArgumentException(key + " must be an array, got " + value.getNodeType());
        }
        return IntStream.range(0, value.size())
            .mapToObj(value::get)
            .map(JsonNode::asText)
            .collect(Collectors.toList());
    }
}
```

---

## Testing Best Practices

### 1. Add Parameterized Tests for Rules

Use JUnit 5's parameterized tests to cover multiple scenarios:

```java
@ParameterizedTest
@CsvSource({
    "15.0, 10.0, 20.0, true",
    "5.0, 10.0, 20.0, false",
    "25.0, 10.0, 20.0, false",
    "10.0, 10.0, 20.0, true",
    "20.0, 10.0, 20.0, true"
})
void predicate_valueInRange_returnsExpected(
        double value, double min, double max, boolean expected) {

    ObjectNode params = JsonNodeFactory.instance.objectNode();
    params.put("min", min);
    params.put("max", max);

    LimitRule rule = (LimitRule) RuleType.Limit.build().setParams(params);

    DoubleTimeSeries series = new DoubleTimeSeriesBuilder()
        .setId("test")
        .add(1L, value)
        .build();

    BooleanTimeSeries[] result = rule.predicate().apply(new TimeSeries[]{series});
    assertEquals(expected, result[0].values()[0]);
}

@ParameterizedTest
@ValueSource(booleans = {true, false})
void predicate_withMultipleValues_considersAll(boolean expectTrue) {
    // Test with multiple values in series
}
```

### 2. Add Integration Tests with Real NATS

The existing `StrategyTest` shows good integration testing. Add more comprehensive NATS tests:

```java
@Testcontainers
class StrategyNatsIntegrationTest {

    @Container
    static final NatsContainer NATS = new NatsContainer();

    private Publisher publisher;
    private StrategyManager manager;

    @BeforeEach
    void setUp() {
        // Configure with test container
        String natsUrl = NATS.getNatsUrl();

        Subscriber source = SourceType.NATS.builder()
            .setName("test-source")
            .setPublisher("local")
            .build();

        // Set NATS params
        ObjectNode params = JsonNodeFactory.instance.objectNode();
        ArrayNode servers = params.putArray("servers");
        servers.add(natsUrl);

        ((NatsSubscriber) source).setParams(params);

        publisher = TargetType.SIMULATED.builder().setName("test-publisher").build();
        manager = new StrategyManager().setPublisher(publisher);
    }

    @Test
    void strategy_receivesNatsEvents() throws InterruptedException {
        // Test receiving events from real NATS
    }
}
```

### 3. Add Fuzz Testing for Edge Cases

Use tools like j-u-i-t-e-r-f to test edge cases:

```java
@FuzzTest
void predicate_handleNullTimestamps(FuzzData data) {
    LimitRule rule = (LimitRule) RuleType.Limit.build();
    rule.setMin(0).setMax(100);

    // Test with null/invalid timestamps
    DoubleTimeSeries series = new DoubleTimeSeriesBuilder()
        .setId("test")
        .add(Long.MIN_VALUE, 50.0)
        .add(Long.MAX_VALUE, 50.0)
        .build();

    BooleanTimeSeries[] result = rule.predicate().apply(new TimeSeries[]{series});
    assertNotNull(result);
}
```

### 4. Add Performance Benchmarks

Create JMH benchmarks for critical paths:

```java
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class RuleBenchmark {

    private LimitRule rule;
    private TimeSeries[] inputs;

    @Setup
    public void setup() {
        rule = (LimitRule) RuleType.Limit.build();
        rule.setMin(10).setMax(90);

        // Create test data
        inputs = new TimeSeries[3];
        for (int i = 0; i < inputs.length; i++) {
            DoubleTimeSeriesBuilder builder = new DoubleTimeSeriesBuilder().setId("s" + i);
            for (int j = 0; j < 1000; j++) {
                builder.add(j, j * 0.5);
            }
            inputs[i] = builder.build();
        }
    }

    @Benchmark
    public void benchmarkLimitRule() {
        rule.predicate().apply(inputs);
    }

    @Benchmark
    public void benchmarkAndRule() {
        AndRule rule = new AndRule();
        rule.watch(inputs);
        // Trigger evaluation
    }
}
```

---

## Performance Considerations

### 1. Cache Timestamp lookups in Rules

Avoid repeated timestamp lookups in tight loops:

```java
// Before (TrendRule.java:99-106):
private double getSlope(DoubleTimeSeries series) {
    regression.clear();
    for (int x = 0; x < series.size(); x++) {
        double doubleTimestamp = series.timestamps()[x];
        double value = series.values()[x];
        regression.addData(doubleTimestamp, value);
    }
    return regression.getSlope();
}

// After - cache the arrays:
private double getSlope(DoubleTimeSeries series) {
    regression.clear();
    long[] timestamps = series.timestamps();  // Cache
    double[] values = series.values();        // Cache
    for (int x = 0; x < series.size(); x++) {
        regression.addData(timestamps[x], values[x]);
    }
    return regression.getSlope();
}
```

### 2. Use Parallel Processing for Independent Rules

For independent rule evaluation, use parallel streams:

```java
public class Strategy {

    public List<BooleanTimeSeries> evaluateRulesParallel(TimeSeries[] inputs) {
        return rules.parallelStream()
            .map(rule -> {
                if (rule.isActivated()) {
                    return rule.predicate().apply(inputs);
                }
                return new BooleanTimeSeries[0];
            })
            .filter(arr -> arr.length > 0)
            .collect(Collectors.toList());
    }
}
```

### 3. Add Monitoring Hooks

Add hooks for performance monitoring:

```java
public class Fifo<T> {

    private static final AtomicLong emitCount = new AtomicLong(0);
    private static final LongAdder totalLatencyNanos = new LongAdder();

    public void emitNext(T event) {
        long start = System.nanoTime();

        readLock.lock();
        try {
            CountDownLatch latch = new CountDownLatch(consumers.size());
            for (var consumer : consumers) {
                Thread.startVirtualThread(() -> {
                    try {
                        consumer.accept(event);
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        } finally {
            readLock.unlock();
        }

        long latency = System.nanoTime() - start;
        emitCount.increment();
        totalLatencyNanos.add(latency);
    }

    public static MonitoringStats getStats() {
        return new MonitoringStats(
            emitCount.get(),
            totalLatencyNanos.sum() / Math.max(1, emitCount.get()),
            consumers.size()
        );
    }

    public record MonitoringStats(
        long totalEvents,
        double avgLatencyNanos,
        int subscriberCount
    ) {}
}
```

### 4. Optimize TimeSeries Merging

The `merge()` operation can be optimized:

```java
public BooleanTimeSeriesBuilder merge(TimeSeries... timeSeriesArray) {
    if (timeSeriesArray.length == 0) {
        return this;
    }

    // Find the max size to pre-allocate
    int totalSize = 0;
    for (var ts : timeSeriesArray) {
        totalSize += ts.size();
    }

    // Pre-allocate if using ArrayList-based internal storage
    // For TreeMap, this is less critical but still helps

    for (var timeSeries : timeSeriesArray) {
        from(timeSeries);
    }
    return this;
}
```

---

## Error Handling and Resilience

### 1. Add Retry Policy for External Connections

Implement retry with exponential backoff for NATS connections:

```java
public class NatsSubscriber extends Subscriber implements WithParams {

    private static final int MAX_RETRIES = 5;
    private static final Duration INITIAL_BACKOFF = Duration.ofMillis(100);
    private static final Duration MAX_BACKOFF = Duration.ofSeconds(10);

    @Override
    public boolean connect() {
        int attempt = 0;
        Duration backoff = INITIAL_BACKOFF;

        while (attempt < MAX_RETRIES) {
            try {
                connection = Nats.connectReconnectOnConnect(natsOptions);
                dispatcher = connection.createDispatcher(raw -> {
                    // ... existing callback
                });

                boolean subscribed = topics.stream()
                    .map(dispatcher::subscribe)
                    .allMatch(Consumer::isActive);

                connected.set(connection.getStatus() == Connection.Status.CONNECTED
                    && dispatcher.isActive()
                    && subscribed);

                if (connected.get()) {
                    return true;
                }

            } catch (InterruptedException | IOException e) {
                log.warn("Connect attempt {} failed: {}", attempt + 1, e.getMessage());
            }

            attempt++;
            if (attempt < MAX_RETRIES) {
                try {
                    Thread.sleep(backoff.toMillis());
                    backoff = Duration.ofMillis(Math.min(
                        backoff.multipliedBy(2).toMillis(),
                        MAX_BACKOFF.toMillis()
                    ));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }

        log.error("Failed to connect after {} attempts", MAX_RETRIES);
        return false;
    }
}
```

### 2. Implement Circuit Breaker Pattern

For rules that depend on external sources:

```java
public class CircuitBreakerRule<T extends TimeSeries> extends RuleStream<T> {

    private final RuleStream<T> upstream;
    private final CircuitBreaker circuitBreaker;

    public CircuitBreakerRule(RuleStream<T> upstream, CircuitBreakerConfig config) {
        this.upstream = upstream;
        this.circuitBreaker = CircuitBreaker.of("rule-" + upstream.hashCode(), config);

        upstream.results().subscribe(ts -> {
            circuitBreaker.executeRunnable(() -> {
                results().emitNext(ts);
            });
        });
    }

    public static class CircuitBreakerConfig {
        private final double failureRateThreshold;
        private final int minimumNumberOfCalls;
        private final Duration waitDurationInOpenState;

        private CircuitBreakerConfig(double failureRateThreshold,
                                    int minimumNumberOfCalls,
                                    Duration waitDurationInOpenState) {
            this.failureRateThreshold = failureRateThreshold;
            this.minimumNumberOfCalls = minimumNumberOfCalls;
            this.waitDurationInOpenState = waitDurationInOpenState;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private double failureRateThreshold = 50.0;
            private int minimumNumberOfCalls = 10;
            private Duration waitDurationInOpenState = Duration.ofSeconds(30);

            public Builder failureRateThreshold(double threshold) {
                this.failureRateThreshold = threshold;
                return this;
            }

            public Builder minimumNumberOfCalls(int calls) {
                this.minimumNumberOfCalls = calls;
                return this;
            }

            public Builder waitDurationInOpenState(Duration duration) {
                this.waitDurationInOpenState = duration;
                return this;
            }

            public CircuitBreakerConfig build() {
                return new CircuitBreakerConfig(
                    failureRateThreshold,
                    minimumNumberOfCalls,
                    waitDurationInOpenState
                );
            }
        }
    }
}
```

### 3. Add Graceful Degradation

When sources timeout or fail, provide fallback behavior:

```java
public class FallbackRule extends RuleStream<BooleanTimeSeries> {

    private final RuleStream<BooleanTimeSeries> primary;
    private final RuleStream<BooleanTimeSeries> fallback;
    private final Duration timeout;

    public FallbackRule(RuleStream<BooleanTimeSeries> primary,
                       RuleStream<BooleanTimeSeries> fallback,
                       Duration timeout) {
        this.primary = primary;
        this.fallback = fallback;
        this.timeout = timeout;
    }

    @Override
    protected Function<TimeSeries[], BooleanTimeSeries[]> predicate() {
        return timeSeriesArray -> {
            // Try primary with timeout
            try {
                Future<BooleanTimeSeries[]> primaryResult =
                    ForkJoinPool.commonPool().submit(() -> primary.predicate().apply(timeSeriesArray));

                BooleanTimeSeries[] result = primaryResult.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
                if (result.length > 0 && !result[0].equals(EmptyTimeSeries.INSTANCE)) {
                    return result;
                }
            } catch (TimeoutException e) {
                log.warn("Primary rule timed out, using fallback");
            } catch (Exception e) {
                log.warn("Primary rule failed, using fallback", e);
            }

            // Use fallback
            return fallback.predicate().apply(timeSeriesArray);
        };
    }
}
```

### 4. Add Alerting on Rule Failures

Log and alert when rules fail:

```java
public class AlertingRule extends RuleStream<BooleanTimeSeries> {

    private final RuleStream<BooleanTimeSeries> upstream;
    private final AlertPublisher alertPublisher;
    private int failureCount = 0;
    private static final int FAILURE_THRESHOLD = 10;

    public AlertingRule(RuleStream<BooleanTimeSeries> upstream, AlertPublisher alertPublisher) {
        this.upstream = upstream;
        this.alertPublisher = alertPublisher;
    }

    @Override
    protected Function<TimeSeries[], BooleanTimeSeries[]> predicate() {
        return timeSeriesArray -> {
            try {
                return upstream.predicate().apply(timeSeriesArray);
            } catch (Exception e) {
                failureCount++;
                log.error("Rule evaluation failed", e);

                if (failureCount >= FAILURE_THRESHOLD) {
                    alertPublisher.publish(Alert.builder()
                        .type(Alert.AlertType.RULE_FAILURE)
                        .rule(upstream.getDescription())
                        .message("Rule failed " + failureCount + " times")
                        .timestamp(System.currentTimeMillis())
                        .build());
                    failureCount = 0;
                }

                return new BooleanTimeSeries[] {
                    new BooleanTimeSeriesBuilder().add(0, false).build()
                };
            }
        };
    }
}
```

---

## Architecture Patterns

### 1. Strategy for Rule Compilation

Implement different compilation strategies for rules:

```java
public interface RuleCompiler {
    RuleStream<BooleanTimeSeries> compile(JsonNode ruleJson, Strategy strategy);
}

public class AstRuleCompiler implements RuleCompiler {
    @Override
    public RuleStream<BooleanTimeSeries> compile(JsonNode ruleJson, Strategy strategy) {
        // Parse and compile rule AST
        // Optimize the rule structure
        // Return compiled rule
    }
}

public class InterpretedRuleCompiler implements RuleCompiler {
    @Override
    public RuleStream<BooleanTimeSeries> compile(JsonNode ruleJson, Strategy strategy) {
        // Interpret rule at runtime
        // No optimization
    }
}

public class RuleCompilationFactory {
    private static final RuleCompiler AST_COMPILER = new AstRuleCompiler();
    private static final RuleCompiler INTERPRETED_COMPILER = new InterpretedRuleCompiler();

    public static RuleCompiler getCompiler(String mode) {
        return switch (mode) {
            case "interpreted" -> INTERPRETED_COMPILER;
            case "optimized" -> AST_COMPILER;
            default -> AST_COMPILER;
        };
    }
}
```

### 2. Observer Pattern for Strategy Events

Add event notifications for strategy lifecycle:

```java
public interface StrategyEventListener {
    void onStrategyRegistered(IStrategy strategy);
    void onStrategyActivated(IStrategy strategy);
    void onStrategyDeactivated(IStrategy strategy);
    void onSignalPublished(TradingSignal signal);
}

public class StrategyManager {

    private final List<StrategyEventListener> listeners = new CopyOnWriteArrayList<>();

    public StrategyManager addListener(StrategyEventListener listener) {
        listeners.add(listener);
        return this;
    }

    public StrategyManager removeListener(StrategyEventListener listener) {
        listeners.remove(listener);
        return this;
    }

    private void notifyStrategyRegistered(IStrategy strategy) {
        listeners.forEach(l -> l.onStrategyRegistered(strategy));
    }

    // Use in registerStrategy, run, etc.
}
```

### 3. Factory Pattern for TimeSeries

Create a factory for consistent TimeSeries creation:

```java
public final class TimeSeriesFactory {

    private TimeSeriesFactory() {
        throw new IllegalStateException("Utility class");
    }

    public static DoubleTimeSeries create(String id, long timestamp, double value) {
        return new DoubleTimeSeriesBuilder()
            .setId(id)
            .add(timestamp, value)
            .build();
    }

    public static DoubleTimeSeries create(String id, long[] timestamps, double[] values) {
        if (timestamps.length != values.length) {
            throw new IllegalArgumentException("Timestamps and values must have same length");
        }
        if (timestamps.length == 0) {
            return EmptyTimeSeries.INSTANCE;
        }
        return new DoubleTimeSeriesBuilder()
            .setId(id)
            .add(timestamps[0], values[0])
            .build();
    }

    public static BooleanTimeSeries createBoolean(String id, long timestamp, boolean value) {
        return new BooleanTimeSeriesBuilder()
            .setId(id)
            .add(timestamp, value)
            .build();
    }

    public static BarTimeSeries createBar(String id, long timestamp,
                                         double open, double high, double low, double close, long volume) {
        // Implementation
    }
}
```

### 4. Visitor Pattern for Rule Analysis

Implement a visitor for analyzing rule structures:

```java
public interface RuleVisitor {
    void visit(LimitRule rule);
    void visit(AndRule rule);
    void visit(OrRule rule);
    void visit(NotRule rule);
    void visit(CrossedRule rule);
    void visit(TrendRule rule);
}

public class RuleAnalyzer {

    public void analyze(IStrategy strategy) {
        RuleVisitor visitor = createAnalysisVisitor();
        analyzeRule(strategy.getAggregatorRule(), visitor);
    }

    private void analyzeRule(RuleStream<?> rule, RuleVisitor visitor) {
        if (rule instanceof LimitRule l) visitor.visit(l);
        else if (rule instanceof AndRule a) {
            visitor.visit(a);
            // Recursively analyze inputs
        }
        // Handle other rule types
    }

    private RuleVisitor createAnalysisVisitor() {
        return new RuleVisitor() {
            @Override
            public void visit(LimitRule rule) {
                // Analyze limit rule
            }

            @Override
            public void visit(AndRule rule) {
                // Analyze and rule
            }
            // ...
        };
    }
}
```

---

## JSON Configuration Examples

### Complete Strategy Example with Validation

```json
{
  "strategies": {
    "btc-enter": {
      "symbol": "BTC/USD",
      "type": "ENTER",
      "sources": {
        "price": {
          "type": "NATS",
          "publisher": "market-data",
          "params": {
            "servers": ["nats://localhost:4222"],
            "topics": ["btc.price"]
          }
        },
        "volume": {
          "type": "NATS",
          "publisher": "market-data",
          "params": {
            "servers": ["nats://localhost:4222"],
            "topics": ["btc.volume"]
          }
        }
      },
      "rule": {
        "type": "And",
        "inputs": [
          {
            "type": "Limit",
            "description": "Price within normal range",
            "params": {
              "min": 10000.0,
              "max": 100000.0
            },
            "inputs": ["price"]
          },
          {
            "type": "Or",
            "description": "High volume OR price surge",
            "inputs": [
              {
                "type": "Limit",
                "params": {
                  "min": 100.0,
                  "max": 10000.0
                },
                "inputs": ["volume"]
              },
              {
                "type": "Trend",
                "params": {
                  "isRising": true,
                  "sourceId": "price"
                },
                "inputs": ["price"]
              }
            ]
          }
        ]
      }
    }
  }
}
```

### Dynamic Rule with Source References

```json
{
  "rule": {
    "type": "Crossed",
    "description": "MACD signal cross",
    "params": {
      "sourceId": "macd_line",
      "threshold": 0.0
    },
    "inputs": ["macd_line", "signal_line"]
  }
}
```

---

## Migration Guide

### From Version 1.x to 2.x

1. **Rule Registration**: Rules must now be explicitly activated by calling `watch()` with valid sources before use.

2. **TimeSeries Builders**: Use builder pattern consistently:
   ```java
   // Old (direct construction):
   new DoubleTimeSeries(id, timestamps, values);

   // New (builder pattern):
   new DoubleTimeSeriesBuilder()
       .setId(id)
       .add(timestamp, value)
       .build();
   ```

3. **Parameter Handling**: The `setParams(JsonNode)` method is now required for all rules.

4. **Fifo API**: `emitNext()` now returns void (no longer returns latch).

---

## Summary

This document provides comprehensive recommendations for improving xtratej across multiple dimensions:

| Category | Key Recommendations |
|----------|---------------------|
| **Rules** | Add validation, consider immutability, document behavior |
| **Event Stream** | Add backpressure, filter operator, configurable zip tolerance |
| **Configuration** | Add schema validation, builder validation, parameter utilities |
| **Testing** | Use parameterized tests, NATS integration tests, JMH benchmarks |
| **Performance** | Cache lookups, parallel processing, add monitoring hooks |
| **Error Handling** | Implement retry with backoff, circuit breaker, graceful degradation |
| **Architecture** | Strategy compilation, observer pattern, factory pattern, visitor pattern |
