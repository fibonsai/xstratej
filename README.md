# xtratej

**xtratej** is a robust, reactive strategy/rule engine for Java. It provides a flexible framework for defining, testing, and executing trading strategies based on time-series data and event streams.

## Features

*   **Reactive Architecture**: Built on a custom `Fifo` reactive stream implementation for efficient event processing.
*   **Extensible Rules Engine**: define complex logic using a variety of composable rules (`CrossedRule`, `AndRule`, `OrRule`, etc.).
*   **Subcriber connectors** (WIP): Support to connect and receive external datas.
*   **Time-Series Handling**: Specialized support for temporal data manipulation and analysis.
*   **Strategy Management**: Organize indicators and logic into cohesive `Strategy` units.

## Usage

### 1. Programmatic Definition

You can define strategies and rules directly in Java. Use `StrategyManager` to coordinate multiple strategies.

```java
import com.fibonsai.cryptomeria.xtratej.strategy.Strategy;
import com.fibonsai.cryptomeria.xtratej.strategy.IStrategy;
import com.fibonsai.cryptomeria.xtratej.strategy.StrategyManager;
import com.fibonsai.cryptomeria.xtratej.rules.RuleType;
import com.fibonsai.cryptomeria.xtratej.rules.impl.LimitRule;
import com.fibonsai.cryptomeria.xtratej.sources.SourceType;
import com.fibonsai.cryptomeria.xtratej.sources.Subscriber;
import com.fibonsai.cryptomeria.xtratej.event.reactive.Fifo;

// 1. Create Sources
Subscriber source1 = SourceType.SIMULATED.builder()
    .setName("flux1")
    .setPublisher("test")
    .build();

// 2. Configure Rules
LimitRule limitRule = (LimitRule) RuleType.Limit.build();
limitRule.setMin(2.0).setMax(80.0);
limitRule.watch(Fifo.zip(source1.toFifo()));

// 3. Define Strategy
IStrategy myStrategy = new Strategy(
    "MyStrategy",
    "BTC-USD",
    IStrategy.StrategyType.ENTER
);

myStrategy.addSource(source1)
          .setAggregatorRule(limitRule);

// 4. Manage and Run
StrategyManager manager = new StrategyManager(new Fifo<>())
    .registerStrategy(myStrategy);

manager.run();
```

### 2. JSON Configuration

Strategies can be loaded from a JSON file for better flexibility.

```java
import com.fibonsai.cryptomeria.xtratej.strategy.Loader;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

ObjectMapper mapper = new ObjectMapper();
JsonNode jsonNode = mapper.readTree(new File("strategies.json"));
Map<String, IStrategy> strategies = Loader.fromJson(jsonNode);
```

#### JSON Schema

The `Loader` class expects a JSON structure as follows:

- `strategies`: (Object) Top-level container.
  - `[strategyName]`: (Object) Key is the strategy name.
    - `symbol`: (String) Trading symbol (e.g., "BTC/USD").
    - `type`: (String) "ENTER" or "EXIT".
    - `sources`: (Object) Map of data sources.
      - `[sourceName]`: (Object) Key is the source identifier.
        - `type`: (String) Source type (e.g., "SIMULATED", "NATS").
        - `publisher`: (String) Publisher identifier.
        - `params`: (Object) Source-specific properties.
    - `rule`: (Object) The root rule definition.
      - `type`: (String) Rule type (e.g., "And", "Or", "Limit", "Crossed").
      - `description`: (String) Optional description.
      - `params`: (Object) Rule properties (e.g., `{"min": 10.0, "max": 20.0}`).
      - `inputs`: (Array) data sources name list (string list), declared in `sources` attribute, OR nested rule objects.

##### Example Json
```json
{
  "strategies": {
    "enter": {
      "symbol": "BTC/USD",
      "type": "ENTER",
      "sources": {
        "flux1": {
          "type": "SIMULATED",
          "publisher": "local",
          "params": {}
        },
        "flux2": {
          "type": "SIMULATED",
          "publisher": "local",
          "params": {}
        },
        "flux3": {
          "type": "SIMULATED",
          "publisher": "local",
          "params": {}
        }
      },
      "rule": {
        "description": "aggregator",
        "type": "And",
        "inputs": [
          {
            "type": "Or",
            "inputs": [
              {
                "type": "Limit",
                "description": "limit1",
                "params": {
                  "min": 2.0,
                  "max": 80.0
                },
                "inputs": [
                  "flux1",
                  "flux2",
                  "flux3"
                ]
              },
              {
                "type": "Limit",
                "description": "limit2",
                "params": {
                  "min": 0.0,
                  "max": 50.0
                },
                "inputs": [
                  "flux1",
                  "flux2",
                  "flux3"
                ]
              }
            ]
          },
          {
            "type": "Not",
            "inputs": [
              {
                "type": "Limit",
                "description": "limit3",
                "params": {
                  "upperSourceId": "flux1",
                  "lowerSourceId": "flux2"
                },
                "inputs": [
                  "flux1",
                  "flux2",
                  "flux3"
                ]
              }
            ]
          }
        ]
      }
    }
  }
}
```

## Architecture

*   **Strategy**: The central coordinator that manages lifecycle and data flow.
*   **RuleStream**: Abstract base for all logic components. Rules process inputs and emit `BooleanSingleTimeSeries` results.
*   **TimeSeries**: Optimized storage for temporal data points (prices, signals).
*   **Fifo**: The underlying reactive pipe connecting components.
*   **Subscriber**: Entity to subscribe external data sources.

## Requirements

*   Java 25 or higher
*   Maven 3.8+

## TODO

- [ ] Publishers to external consumers
- [ ] NATS Subscriber support.
- [ ] Kafka Subscriber support.
- [ ] Candles rules support.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
