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

StrategyManager manager = new StrategyManager();
manager.registerStrategy(myStrategy);

manager.run();

manager.tradingSignalPublisher().subscribe(signal -> {
   // buy/sell logic 
});
```

### 2. JSON Configuration

Strategies can be loaded from a JSON file for better flexibility.

```java
ObjectMapper mapper = new ObjectMapper();
JsonNode jsonNode = mapper.readTree(new File("strategies.json"));
Map<String, IStrategy> strategies = Loader.fromJson(jsonNode);

StrategyManager manager = new StrategyManager(tradingSignalConsumer);
for (var strategy: strategies.values()) {
    manager.registerStrategy(strategy);
}

manager.run();

manager.tradingSignalPublisher().subscribe(signal -> {
    // buy/sell logic 
});
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
        - `params`: (Object) Source-specific params.
    - `rule`: (Object) The root rule definition.
      - `type`: (String) Rule type (e.g., "And", "Or", "Limit", "Crossed").
      - `description`: (String) Optional description.
      - `params`: (Object) Rule params (e.g., `{"min": 10.0, "max": 20.0}`).
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

We have two modules/subprojects:
* **event**: data flow containers implementations supported by a simple, but "real-time" reactive approach.
  *   **TimeSeries**: Optimized storage for temporal data points (prices, signals).
  *   **Fifo**: The underlying reactive pipe connecting components.
  
* **engine**: Rule/Strategy engine with external sources connectors.
  *   **Strategy**: The central coordinator that manages lifecycle and data flow.
  *   **RuleStream**: Abstract base for all logic components. Rules process inputs and emit `BooleanSingleTimeSeries` results.
  *   **Subscriber**: Entity to subscribe external data sources.

## Requirements

*   Java 25 or higher
*   Maven 3.8+

## TODO

- [x] NATS Subscriber support.
- [ ] Publishers to external consumers
- [ ] Kafka Subscriber support.
- [ ] Candles rules support.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
