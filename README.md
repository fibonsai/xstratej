# xtratej

**xtratej** is a robust, reactive strategy engine for Java, designed by Fibonsai. It provides a flexible framework for defining, testing, and executing trading strategies based on time-series data and event streams.

## Features

*   **Reactive Architecture**: Built on a custom `Fifo` reactive stream implementation for efficient event processing.
*   **Extensible Rules Engine**: define complex logic using a variety of composable rules (`CrossedRule`, `AndRule`, `OrRule`, etc.).
*   **Time-Series Handling**: Specialized support for temporal data manipulation and analysis.
*   **Strategy Management**: Organize indicators and logic into cohesive `Strategy` units.

## Installation

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.fibonsai.cryptomeria</groupId>
    <artifactId>xtratej</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

## Usage

### 1. Define a Strategy

```java
import com.fibonsai.cryptomeria.xtratej.strategy.Strategy;
import com.fibonsai.cryptomeria.xtratej.strategy.IStrategy;

IStrategy myStrategy = new Strategy(
    "MyMovingAverageStrategy",
    "BTC-USD",
    "Binance",
    IStrategy.StrategyType.ENTER
);
```

### 2. Configure Rules

Rules can be configured using `JsonNode` properties or fluent setters.

```java
import com.fibonsai.cryptomeria.xtratej.rules.RuleType;
import com.fibonsai.cryptomeria.xtratej.rules.impl.CrossedRule;
import tools.jackson.databind.ObjectMapper;

// Example: Create a rule that triggers when a value crosses a threshold
CrossedRule crossRule = (CrossedRule) RuleType.Crossed.builder()
        .setId("CrossAbove100")
        .build()
        .setThreshold(100.0)
        .setSourceId("price_series");

// Add the rule to the strategy
myStrategy.addIndicatorRule(crossRule);
```

### 3. Subscribe and Execute

```java
myStrategy.activeRules(); // Activate the strategy logic

myStrategy.subscribe(result -> {
    System.out.println("Strategy Event: " + result);
});
```

## Architecture

*   **Strategy**: The central coordinator that manages lifecycle and data flow.
*   **RuleStream**: Abstract base for all logic components. Rules process inputs and emit `BooleanSingleTimeSeries` results.
*   **TimeSeries**: Optimized storage for temporal data points (prices, signals).
*   **Fifo**: The underlying reactive pipe connecting components.

## Requirements

*   Java 25 or higher
*   Maven 3.8+

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
