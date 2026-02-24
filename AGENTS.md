# Agent Guidelines for xtratej

This document provides instructions and context for AI agents working on the `xtratej` codebase.

## Project Structure

*   **Root**: `/home/tuxmonteiro/dev/github.com/fibonsai/xtratej`
*   **Source Code**: `src/main/java/com/fibonsai/cryptomeria/xtratej/`
*   **Tests**: `src/test/java/com/fibonsai/cryptomeria/xtratej/`
*   **Event Handling**: `src/main/java/com/fibonsai/cryptomeria/xtratej/event/`
*   **Rules**: `src/main/java/com/fibonsai/cryptomeria/xtratej/rules/`
*   **Strategy**: `src/main/java/com/fibonsai/cryptomeria/xtratej/strategy/`

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
*   **Immutability**: Prefer immutable data structures for event payloads (`ITemporalData` implementations).
*   **Rule Composition**: Complex logic should be composed of smaller, reusable `RuleStream` implementations rather than monolithic blocks.

## Testing

*   **Framework**: JUnit 5 + Mockito.
*   **Requirement**: Every new feature or bug fix must include a corresponding test case.
*   **Location**: Mirror the package structure in `src/test/java`.

## Common Tasks

### Adding a New Rule
1.  Extend `RuleStream`.
2.  Implement the `predicate()` method.
3.  Add unit tests in `src/test/java/.../rules/impl/`.
4.  If the rule requires configuration, use `JsonNode` properties in the constructor or add fluent setter methods.

### Debugging
*   Check `pom.xml` for dependency versions if you encounter build issues.
*   Ensure that the `Fifo` streams are properly subscribed to; otherwise, events will not flow.
