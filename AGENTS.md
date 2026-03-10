# Agent Guidelines for xtratej

This document provides instructions and context for AI agents working on the `xtratej` codebase.

## Project Structure

It's a maven project with two modules/subprojects:

* **event**: data flow containers implementations supported by a simple, but "real-time" reactive approach.
* **engine**: Rule/Strategy engine with external sources connectors.

IMPORTANT: **engine** module/subproject depends on **event** module/subproject.

### `event` structure

*   **Base Source Code**: `event/src/main/java/com/fibonsai/cryptomeria/xtratej/event`
*   **Tests**: `event/src/test/java/com/fibonsai/cryptomeria/xtratej/event`

### `engine` structure

*   **Base Source Code**: `engine/src/main/java/com/fibonsai/cryptomeria/xtratej/engine`
*   **Tests**: `engine/src/test/java/com/fibonsai/cryptomeria/xtratej/engine`
*   **Rules**: `engine/src/main/java/com/fibonsai/cryptomeria/xtratej/engine/rules/`
*   **Strategy**: `engine/src/main/java/com/fibonsai/cryptomeria/xtratej/engine/strategy/`
*   **External Sources**: `engine/src/main/java/com/fibonsai/cryptomeria/xtratej/engine/sources/`


## Coding Standards

1.  **Java Version**: The project uses Java 25. Ensure features compatible with this version are used correctly.
2.  **Package Naming**: Follow the `com.fibonsai.cryptomeria.xtratej.*` convention.
3.  **Copyright Headers**: All new files must include the standard file header found in existing files (e.g., `Strategy.java`).
4.  **Logging**: Use SLF4J for logging.
    ```java
    private static final Logger log = LoggerFactory.getLogger(MyClass.class);
    ```

## Architectural Patterns

*   **Reactive First**: This is a reactive application. Use `Fifo` class for data streams. Avoid blocking operations in the event loop.
*   **Immutability**: Prefer immutable data structures for event payloads (`TimeSeries` implementations).
*   **Rule Composition**: Complex logic should be composed of smaller, reusable `RuleStream` implementations rather than monolithic blocks.
*   **Injected data from external datasources**: Implements support to connect and subscribe external market data providers (`Subscriber` implementations)
*   **Enum Builder**: Prefer create new instances using Enum Builders, as RuleType, and SourceType.

## Architectural Decisions

* Wiring Mechanism: The `Loader` class is responsible for wiring. It parses a JSON definition where sources are defined first. Rules then specify their inputs which can be names of these sources or nested rule definitions.
* Reactive Data Flow: Connection is achieved using `Fifo<TimeSeries>`. `Loader.parseRule` collects the FIFOs from the named sources (via `strategy.getSources().get(inputName).toFifo()`), zips them using `Fifo.zip()`, and passes the resulting zipped FIFO to `RuleStream.watch()`.
* LimitRule Logic: `LimitRule` specifically looks for `upperSourceId` and `lowerSourceId` in its params. In its predicate function, it iterates through the provided `TimeSeries[]` array (produced by the zipped FIFO) and matches TimeSeries IDs against these params to determine dynamic boundaries. If no dynamic boundaries are found, it falls back to fixed min/max values.
* Strategy Integration: `Strategy` acts as a container. `StrategyManager` runs strategies by subscribing to the root rule's (aggregator) result FIFO. When the rule evaluates to true, a `TradingSignal` is emitted.

## Exploring the code

* "Read `engine/src/main/java/com/fibonsai/cryptomeria/xtratej/engine/rules/impl/LimitRule.java` to understand its logic and params (min, max, upperSourceId, lowerSourceId).
* "Read `engine/src/main/java/com/fibonsai/cryptomeria/xtratej/engine/rules/RuleStream.java` to understand the base rule class and the watch mechanism using Fifo.
* "Read `engine/src/main/java/com/fibonsai/cryptomeria/xtratej/engine/strategy/Strategy.java` and @src/main/java/com/fibonsai/cryptomeria/xtratej/strategy/IStrategy.java to see how strategies manage sources and the aggregator rule.
* "Read `engine/src/main/java/com/fibonsai/cryptomeria/xtratej/engine/strategy/StrategyManager.java` to see how strategies are executed and how results are handled.
* "Read `engine/src/main/java/com/fibonsai/cryptomeria/xtratej/engine/strategy/Loader.java` to understand how the JSON configuration is parsed and how rules are wired to sources using Fifo.zip.
* "Read `engine/src/test/java/com/fibonsai/cryptomeria/xtratej/engine/rules/impl/LimitRule.java` to see example usage and testing patterns for rules.

## Testing

*   **Framework**: JUnit 5 + Mockito.
*   **Requirement**: Every new feature or bug fix must include a corresponding test case.
*   **Location**: Mirror the package structure in `[event|engine]/src/test/java`.

## Common Tasks

### Update README.md
1. Always update README.md file, if necessary.

### Adding a New Rule
1.  Extend `RuleStream`.
2.  Implement the `predicate()` method.
3.  Add unit tests in `engine/src/test/java/.../rules/impl/`.
4.  Update `RuleType` enum.
5.  If the rule requires configuration, use `JsonNode` params and call method `RuleStream.setParams(JsonNode params)` or add fluent setter methods.

### Debugging
*   Check `pom.xml` for dependency versions if you encounter build issues.
*   Ensure that the `Fifo` streams are properly subscribed to; otherwise, events will not flow.

### Testing
* Always check the code quality running all tests

### Not commit
* Never commit changes

### Never downgrade dependencies
* If necessary change dependency version, always check new versions, but never downgrade.