# JMH Benchmarks for xtratej

This directory contains JMH (Java Microbenchmark Harness) benchmarks for critical paths in the xtratej codebase.

## Benchmarks Included

1. **RuleEvaluationBenchmark** - Tests performance of rule evaluations (CrossedRule, LimitRule, TrendRule)
2. **FifoZipBenchmark** - Tests performance of Fifo.zip operations
3. **TimeSeriesBenchmark** - Tests performance of TimeSeries operations
4. **StrategyExecutionBenchmark** - Tests performance of strategy execution

## Running Benchmarks

### Option 1: Run all benchmarks using the runner
```bash
cd engine
mvn exec:java -Dexec.mainClass="com.fibonsai.cryptomeria.xtratej.benchmarks.BenchmarkRunner"
```

### Option 2: Run specific benchmark
```bash
cd engine
mvn clean compile
java -cp target/classes:target/test-classes org.openjdk.jmh.Main RuleEvaluationBenchmark
```

### Option 3: Package and run
```bash
cd engine
mvn clean package
java -jar target/benchmarks.jar
```

## Benchmark Configuration

- Fork: 1 JVM fork per benchmark
- Warmup: 3 iterations of 2 seconds each
- Measurement: 5 iterations of 2 seconds each
- Memory: 2GB heap allocated (-Xms2G -Xmx2G)
- Mode: Average time for most benchmarks, Throughput for StrategyExecutionBenchmark
- Output: Nanoseconds for time-based benchmarks, operations per second for throughput

## Dependencies

The benchmarks require JMH to be in the test scope, which is already included in the engine/pom.xml:

```xml
<dependency>
    <groupId>org.openjdk.jmh</groupId>
    <artifactId>jmh-core</artifactId>
    <version>1.37</version>
    <scope>test</scope>
</dependency>
```

## Adding New Benchmarks

To add new benchmarks:

1. Create a new benchmark class in this package
2. Annotate with appropriate JMH annotations (@Benchmark, @State, etc.)
3. Add to the BenchmarkRunner if you want it included in the full run
4. Follow the existing patterns for configuration