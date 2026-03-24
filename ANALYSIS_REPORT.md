# xtratej Codebase Analysis Report

This report provides a comprehensive analysis of the xtratej codebase, examining its architecture, patterns, strengths, and areas for improvement.

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Project Structure](#project-structure)
3. [Architecture Overview](#architecture-overview)
4. [Event Module Analysis](#event-module-analysis)
5. [Engine Module Analysis](#engine-module-analysis)
6. [Code Quality Assessment](#code-quality-assessment)
7. [Pattern Analysis](#pattern-analysis)
8. [Recommendations Summary](#recommendations-summary)

---

## Executive Summary

**xtratej** is a reactive strategy/rule engine for Java 25+ designed for trading strategy execution. The codebase demonstrates solid engineering practices with a focus on:

- **Reactive Architecture**: Custom `Fifo` implementation using virtual threads
- **Modular Design**: Clean separation between `event` (data models) and `engine` (business logic)
- **Type Safety**: Enum-based factory patterns for rules, sources, and targets
- **Modern Java**: Extensive use of records, pattern matching, and virtual threads

**Key Strengths:**
- Well-organized module structure with clear responsibilities
- Consistent use of builder patterns for configuration
- Good test coverage with both unit and integration tests
- Modern Java 25 features properly utilized

**Areas for Improvement:**
- Missing parameter validation in rule configurations
- No backpressure mechanism in the reactive stream
- Limited error handling for external connections
- Mutable rule instances could cause issues in complex scenarios

---

## Project Structure

```
xtratej/
├── event/                          # Data flow containers
│   └── src/main/java/com/fibonsai/cryptomeria/xtratej/event/
│       ├── reactive/               # Fifo reactive stream
│       │   └── Fifo.java
│       └── series/dao/             # TimeSeries implementations
│           ├── TimeSeries.java     # Base interface
│           ├── BooleanTimeSeries.java
│           ├── DoubleTimeSeries.java
│           ├── Double2TimeSeries.java
│           ├── BarTimeSeries.java
│           ├── BandTimeSeries.java
│           ├── EmptyTimeSeries.java
│           ├── TradingSignal.java
│           ├── tools/              # Utility classes (MinMax, etc.)
│           └── builders/           # Builder classes
│               ├── TimeSeriesBuilder.java
│               ├── BooleanTimeSeriesBuilder.java
│               └── DoubleTimeSeriesBuilder.java
└── engine/                         # Rule/Strategy engine
    └── src/main/java/com/fibonsai/cryptomeria/xtratej/engine/
        ├── strategy/               # Strategy management
        │   ├── Strategy.java
        │   ├── StrategyManager.java
        │   └── Loader.java         # JSON configuration parser
        ├── rules/                  # Rule implementations
        │   ├── RuleStream.java     # Base rule class
        │   ├── RuleType.java       # Rule enum factory
        │   └── impl/               # Individual rule implementations
        │       ├── AndRule.java
        │       ├── OrRule.java
        │       ├── NotRule.java
        │       ├── XOrRule.java
        │       ├── LimitRule.java
        │       ├── CrossedRule.java
        │       ├── TrendRule.java
        │       ├── InSlopeRule.java
        │       ├── DateTimeRule.java
        │       ├── TimeRule.java
        │       ├── WeekdayRule.java
        │       ├── RandomRule.java
        │       └── FalseRule.java
        ├── sources/                # External data sources
        │   ├── Subscriber.java
        │   ├── WithParams.java
        │   ├── SourceType.java
        │   └── impl/
        │       ├── NatsSubscriber.java
        │       └── SimulatedSubscriber.java
        └── targets/                # Signal outputs
            ├── Publisher.java
            ├── TargetType.java
            └── impl/
                └── SimulatedPublisher.java
```

---

## Architecture Overview

### Two-Module Architecture

**1. `event` Module (Data Layer)**
- Purpose: Data flow containers and time-series handling
- Key Components:
  - `Fifo<T>`: Reactive stream implementation with publish-subscribe pattern
  - `TimeSeries` and its implementations: Immutable data containers
  - Builders: Mutable configuration objects for creating TimeSeries

**2. `engine` Module (Business Logic Layer)**
- Purpose: Strategy orchestration and rule evaluation
- Key Components:
  - `RuleStream`: Base class for all rules with `predicate()` method
  - `Strategy`: Container for sources and aggregation rules
  - `StrategyManager`: Orchestrates multiple strategies
  - `Loader`: JSON configuration parser

### Data Flow

```
External Sources (NATS, Simulated)
    ↓
Subscriber → Fifo<TimeSeries>
    ↓
RuleStream (with watch() combining inputs via Fifo.zip())
    ↓
Rule Stream Result (BooleanTimeSeries)
    ↓
Strategy Aggregator
    ↓
StrategyManager → TradingSignal → Publisher
```

---

## Event Module Analysis

### `Fifo<T>` - Reactive Stream Implementation

**Design Pattern**: Publish-Subscribe with virtual threads

**Key Features:**
- Thread-safe with `ReentrantReadWriteLock`
- Virtual thread per subscriber (`Thread.startVirtualThread()`)
- `Fifo.zip()` combines multiple streams with configurable tolerance

**Implementation Details:**
```java
// Subscribe pattern
public void subscribe(Consumer<T> consumer) {
    writeLock.lock();
    try {
        consumers.add(consumer);
        onSubscribe.run();  // Notify subscription
    } finally {
        writeLock.unlock();
    }
}

// Emit pattern with latch synchronization
public void emitNext(T event) {
    readLock.lock();
    try {
        CountDownLatch latch = new CountDownLatch(consumers.size());
        for (var consumer : consumers) {
            Thread.startVirtualThread(() -> {
                consumer.accept(event);
                latch.countDown();
            });
        }
        latch.await(10, TimeUnit.SECONDS);  // Hardcoded timeout
    } catch (InterruptedException e) {
        log.error(e.getMessage(), e);
    } finally {
        readLock.unlock();
    }
}
```

**Strengths:**
- Clean separation of subscribe/emit lifecycle
- Proper lock usage with try-finally blocks
- Virtual threads for high-concurrency support

**Issues:**
1. **No Backpressure**: Consumers can be overwhelmed if they process slowly
2. **Hardcoded Timeout**: 10-second timeout in `emitNext` is not configurable
3. **No Error Propagation**: Exceptions in consumers are only logged

**`Fifo.zip()` Implementation:**
- Combines multiple FIFOs into one emitting arrays
- Uses `ZipCoordinator` with per-source queues
- Implements tolerance window for event alignment
- Discards partial slots when window expires

### TimeSeries Implementations

All TimeSeries implementations use Java 16+ **records**:

| Type | Fields | Purpose |
|------|--------|---------|
| `BooleanTimeSeries` | `id, timestamps[], values[]` | Boolean signal streams |
| `DoubleTimeSeries` | `id, timestamps[], values[]` | Single value streams |
| `Double2TimeSeries` | `id, timestamps[], values[], values2[]` | Two parallel value streams |
| `BarTimeSeries` | `id, timestamps[], opens[], highs[], lows[], closes[], volumes[]` | OHLCV candle data |
| `BandTimeSeries` | `id, timestamps[], uppers[], middles[], lowers[]` | Upper/Middle/Lower band data |
| `TradingSignal` | `id, timestamp, signal, strategyName, pair, publishers` | Trading signals (ENTER/EXIT) |

**Design Pattern**: Immutable records with builder pattern

**Builder Example:**
```java
public class DoubleTimeSeriesBuilder extends TimeSeriesBuilder<DoubleTimeSeriesBuilder> {
    private final TreeMap<Long, Double> doubles = new TreeMap<>();

    public DoubleTimeSeriesBuilder add(long timestamp, double value) {
        writeLock.lock();
        try {
            // Respects maxSize limit
            while (doubles.size() >= maxSize) {
                doubles.remove(doubles.firstKey());
            }
            doubles.put(timestamp, value);
        } finally {
            writeLock.unlock();
        }
        return this;
    }

    public DoubleTimeSeries build() {
        readLock.lock();
        try {
            long[] _timestamps = doubles.sequencedKeySet().stream()
                .mapToLong(Long::longValue).toArray();
            double[] _doubles = doubles.sequencedValues().stream()
                .mapToDouble(Double::doubleValue).toArray();
            return new DoubleTimeSeries(id, _timestamps, _doubles);
        } finally {
            readLock.unlock();
        }
    }
}
```

**Strengths:**
- Thread-safe with R/W locks
- Automatic ordering via `TreeMap`
- Immutable output records
- Maximum size enforcement

**Issues:**
- No validation for duplicate timestamps (last one wins)
- No validation for timestamp ordering

### `EmptyTimeSeries` Sentinel

A singleton sentinel pattern for "no data" scenarios:

```java
public record EmptyTimeSeries() implements TimeSeries {
    public static final TimeSeries INSTANCE = new EmptyTimeSeries();

    @Override
    public long[] timestamps() {
        return new long[0];
    }

    @Override
    public int compareTo(TimeSeries o) {
        return -1;  // Always "before" other time series
    }
}
```

**Usage**: Returned by rules when no sources are activated, or for empty FIFOs.

---

## Engine Module Analysis

### `RuleStream` - Base Rule Class

**Design Pattern**: Template Method

```java
public abstract class RuleStream<T extends TimeSeries> {
    private final Fifo<TimeSeries> results = new Fifo<>();
    private final AtomicBoolean activated = new AtomicBoolean(false);
    private String description = "";

    protected abstract Function<TimeSeries[], T[]> predicate();

    public void watch(Fifo<TimeSeries[]> inputs) { /* ... */ }
    public void watch(Subscriber... subscribers) { /* ... */ }
    @SafeVarargs public final void watch(RuleStream<T>... rules) { /* ... */ }
}
```

**Watch Method Overloads:**
1. `watch(Fifo<TimeSeries[]> inputs)` - Direct FIFO connection
2. `watch(Subscriber... subscribers)` - Auto-wire from subscribers via `Fifo.zip()`
3. `watch(RuleStream<T>... rules)` - Compose rules via their results

**Implementation Pattern:**
```java
// Rule streams inputs through zip, then applies predicate
public void watch(Fifo<TimeSeries[]> inputs) {
    inputs.onSubscribe(() -> activated.set(true)).subscribe(inputTimeSeriesArray -> {
        T[] resultTimeSeriesArray = predicate().apply(inputTimeSeriesArray);
        if (resultTimeSeriesArray.length == 0) {
            results.emitNext(EmptyTimeSeries.INSTANCE);
        } else if (resultTimeSeriesArray.length == 1) {
            results.emitNext(resultTimeSeriesArray[0]);
        } else {
            // Merge multiple results
            TimeSeries merged = builder.setId(newId).merge(resultTimeSeriesArray).build();
            results.emitNext(merged);
        }
    });
}
```

### Rule Implementations

| Rule | Type | Parameters | Purpose |
|------|------|------------|---------|
| `AndRule` | Composite | None | Logical AND across inputs |
| `OrRule` | Composite | None | Logical OR across inputs |
| `NotRule` | Composite | None | Invert single boolean |
| `XOrRule` | Composite | None | Exclusive OR across inputs |
| `LimitRule` | Comparison | `min`, `max`, `upperSourceId`, `lowerSourceId` | Value within range |
| `CrossedRule` | Comparison | `threshold`, `sourceId` | Detect cross events |
| `TrendRule` | Analysis | `sourceId`, `isRising` | Slope comparison |
| `InSlopeRule` | Analysis | `minSlope`, `maxSlope` | Slope within range |
| `DateTimeRule` | Temporal | `begin`, `end`, `datetimeFormat` | Date/time window check |
| `TimeRule` | Temporal | `begin`, `end`, `timeFormat`, `invert` | Time-of-day check |
| `WeekdayRule` | Temporal | `weekdays` (array) | Day-of-week filter |
| `RandomRule` | Generator | None | Random boolean |
| `FalseRule` | Sentinel | None | Always returns false |

**Example: LimitRule**
```java
public class LimitRule extends RuleStream<BooleanTimeSeries> {
    private double min = Double.NEGATIVE_INFINITY;
    private double max = Double.POSITIVE_INFINITY;
    private String upperSourceId = "";
    private String lowerSourceId = "";

    @Override
    public RuleStream<BooleanTimeSeries> setParams(JsonNode params) {
        // Parameter parsing - currently no validation
        for (var e: params.properties()) {
            if ("min".equals(e.getKey()) && e.getValue().isDouble())
                min = e.getValue().asDouble();
            // ... other params
        }
        return this;
    }

    @Override
    protected Function<TimeSeries[], BooleanTimeSeries[]> predicate() {
        return timeSeriesArray -> {
            // Find upper/lower source values
            // Check if main value is within bounds
            // Return BooleanTimeSeries
        };
    }
}
```

**Observations on Rules:**
1. All return `BooleanTimeSeries[]` results
2. Stateful - parameters stored as instance fields
3. Thread-safe due to immutability of input arrays and local processing
4. `setParams()` provides JSON configuration capability

### Strategy Pattern

**`IStrategy` Interface:**
```java
public interface IStrategy {
    enum StrategyType { ENTER, EXIT, UNDEF }

    IStrategy addSource(Subscriber source);
    IStrategy addSource(SourceType, String name, String publisher, JsonNode params);
    IStrategy setAggregatorRule(RuleStream<? extends TimeSeries> aggregator);
    IStrategy onSubscribe(Runnable onSubscribe);
    IStrategy subscribe(Consumer<TimeSeries> consumer);

    String name();
    String symbol();
    Set<String> publishers();
    StrategyType strategyType();
    Map<String, Subscriber> getSources();
}
```

**`Strategy` Implementation:**
- Immutable after activation (check via `isActivated()`)
- Sources stored in `HashMap<String, Subscriber>`
- Aggregator rule produces `TimeSeries` results

**`StrategyManager`:**
- Thread-safe strategy registration with R/W lock
- Runs all strategies concurrently using virtual threads
- Emits `TradingSignal` when aggregator rule evaluates to true
- Uses `CountDownLatch` for activation synchronization (10s timeout)

### Loader - JSON Configuration Parser

**Schema Structure:**
```json
{
  "strategies": {
    "strategyName": {
      "symbol": "BTC/USD",
      "type": "ENTER",
      "sources": { "sourceName": { "type": "SIMULATED", ... } },
      "rule": {
        "type": "And",
        "params": {},
        "inputs": ["sourceName", { "type": "Or", ... }]
      }
    }
  }
}
```

**Recursive Parsing:**
```java
public static Map<String, IStrategy> fromJson(JsonNode json) {
    // Parse strategies object
    // For each strategy:
    //   1. Create Strategy instance
    //   2. Parse sources and add to strategy
    //   3. Parse rule (recursive parseRule)
    //   4. Set aggregator rule
}

private static RuleStream<?> parseRule(JsonNode ruleJson, IStrategy strategy) {
    // Parse rule type, params, description
    RuleStream<?> ruleInstance = ruleType.build().setParams(ruleParams);

    if (inputs not empty) {
        if (first input is string) {
            // Parse as source names - get Fifos from strategy sources
        } else {
            // Parse as nested rules - recursive parseRule calls
        }
        ruleInstance.watch(Fifo.zip(directFluxes));
    }
    return ruleInstance;
}
```

**Strengths:**
- Flexible JSON schema supports nested rules
- Recursive parsing handles complex rule trees
- Sources defined first, rules reference by name

**Issues:**
- No schema validation
- No error messages for missing sources
- Silent failures when source not found (returns empty FIFO)

### Source and Target Types

**SourceType Enum:**
```java
public enum SourceType {
    NATS(NatsSubscriber.class),
    SIMULATED(SimulatedSubscriber.class);

    public static class Builder<T> {
        private String name = "undef";
        private String publisher = "undef";

        public T build() {
            return constructor.newInstance(name, publisher);
        }
    }
}
```

**NatsSubscriber:**
- Supports `DoubleTimeSeries`, `BarTimeSeries`, `BooleanTimeSeries`
- Configurable via `WithParams` interface
- NATS connection with reconnect capability
- Header-based type routing for messages

**Subscriber Base Class:**
```java
public abstract class Subscriber {
    private final String name;
    private final String publisher;
    private final Fifo<TimeSeries> fifo = new Fifo<>();

    public abstract boolean connect();
    public abstract boolean disconnect();
    public abstract boolean isConnected();
}
```

**Publisher Base Class:**
- Extends `Fifo<TradingSignal>`
- Abstract connection methods
- `TargetType` enum with `SIMULATED` implementation

---

## Code Quality Assessment

### Positive Patterns

| Pattern | Location | Description |
|---------|----------|-------------|
| **Immutable Records** | `TimeSeries` implementations | All TimeSeries are immutable records |
| **Builder Pattern** | `DoubleTimeSeriesBuilder` | Fluent, chainable configuration |
| **Template Method** | `RuleStream` | Abstract `predicate()` implemented by subclasses |
| **Enum Factories** | `RuleType`, `SourceType`, `TargetType` | Type-safe instance creation |
| **Virtual Threads** | `Fifo`, `StrategyManager` | High-concurrency with virtual threads |
| **Javax Null Safety** | throughout | `@NullMarked` and `@Nullable` annotations |
| **SLF4J Logging** | All major classes | Consistent logging with proper loggers |
| **Fluent Interface** | `IStrategy` | Method chaining with `return this` |

### Code Smells

1. **Magic Strings in RuleType.fromName()**:
   ```java
   public static RuleType fromName(String name) {
       for (var value: values()) {
           if (value.name().equalsIgnoreCase(name)) {  // Case insensitive
               return value;
           }
       }
       return False;  // Silent fallback
   }
   ```
   *Issue*: Returns `False` for unknown types instead of throwing exception.

2. **Hardcoded Timeout in Fifo**:
   ```java
   latch.await(10, TimeUnit.SECONDS);  // Not configurable
   ```
   *Issue*: May be too short or too long depending on use case.

3. **Stateful Rules**:
   ```java
   public class LimitRule extends RuleStream<BooleanTimeSeries> {
       private double min;  // Mutable state
       private double max;
   }
   ```
   *Issue*: Rules could be reused with different parameters causing race conditions.

4. **No Input Validation**:
   ```java
   // LimitRule.setParams() - no validation
   if ("min".equals(e.getKey()) && e.getValue().isDouble())
       min = e.getValue().asDouble();
   ```
   *Issue*: Invalid configurations silently accepted.

5. **Inconsistent Timestamp Handling**:
   - Some rules use `timestamp()` (last value)
   - Some iterate and find max timestamp
   - Some set to 0 when no data

### Test Coverage

**Existing Tests:**
- `LimitRuleTest.java` - 6 tests covering range checks
- `AndRuleTest.java` - 4 tests covering logical operations
- `StrategyTest.java` - Integration tests with virtual threads

**Missing Tests:**
- `OrRule`, `NotRule`, `XOrRule` - No unit tests found
- `CrossedRule`, `TrendRule` - No unit tests found
- Temporal rules (`DateTimeRule`, `TimeRule`, `WeekdayRule`) - No tests
- `StrategyManager` - No dedicated tests
- `Loader` - No dedicated tests
- `Fifo.zip()` - No unit tests

---

## Pattern Analysis

### 1. Reactive Stream Pattern

**Fifo Implementation:**
```
Publishers → Fifo → Subscribers (virtual threads)
```

**Strengths:**
- Decoupled producer/consumer
- High throughput with virtual threads
- Thread-safe without excessive synchronization

**Weaknesses:**
- No backpressure
- No message durability
- No flow control

### 2. Builder Pattern

**Consistent Usage:**
```java
// Rule configuration
LimitRule rule = (LimitRule) RuleType.Limit.build();
rule.setMin(10).setMax(100).watch(subscribers);

// Source creation
Subscriber source = SourceType.SIMULATED.builder()
    .setName("flux1")
    .setPublisher("test")
    .build();

// TimeSeries creation
DoubleTimeSeries ts = new DoubleTimeSeriesBuilder()
    .setId("price")
    .add(timestamp, value)
    .build();
```

### 3. Composite Pattern

**Rule Composition:**
```
      AndRule
     /       \
  OrRule    NotRule
   /   \       \
Limit  Limit  Limit
```

**JSON Representation:**
```json
{
  "type": "And",
  "inputs": [
    { "type": "Or", "inputs": [ ... ] },
    { "type": "Not", "inputs": [ ... ] }
  ]
}
```

### 4. Strategy Pattern

**IStrategy implementations:**
- Strategy encapsulates sources and rules
- StrategyManager handles orchestration
- Supports multiple concurrent strategies

### 5. Factory Method Pattern

**Enum-based Factories:**
```java
// RuleType - creates rule instances
RuleStream<?> rule = RuleType.Limit.build();

// SourceType - creates subscriber instances
Subscriber subscriber = SourceType.NATS.builder().build();

// TargetType - creates publisher instances
Publisher publisher = TargetType.SIMULATED.builder().build();
```

---

## Recommendations Summary

| Priority | Issue | Recommendation |
|----------|-------|----------------|
| **High** | Missing parameter validation | Add `setParams()` validation with proper error messages |
| **High** | No backpressure in Fifo | Implement configurable buffer with backpressure |
| **High** | Rules are mutable | Consider immutable rules with builder pattern |
| **Medium** | No JSON schema validation | Add validator for strategy JSON configs |
| **Medium** | Hardcoded 10s timeout | Make timeout configurable |
| **Medium** | Limited test coverage | Add tests for all rules and edge cases |
| **Low** | Silent failures in Loader | Log warnings for missing sources |
| **Low** | Inconsistent timestamp handling | Standardize timestamp selection logic |
| **Low** | No error handling for connections | Add retry policy with circuit breaker |

### Priority Implementations

1. **Add Parameter Validation** - Prevent invalid rule configurations at runtime
2. **Add Backpressure** - Prevent consumer overload in high-volume scenarios
3. **Immutable Rules** - Prevent state corruption in concurrent scenarios
4. **Schema Validation** - Catch configuration errors early
5. **Comprehensive Tests** - Ensure all rules work correctly under various inputs

---

*Report generated from analysis of xtratej codebase version 1.0+*
