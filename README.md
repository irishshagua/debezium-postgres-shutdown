# Testing Debezium Behaviour when PostgreSQL Process Tries to Shutdown
Relates to [DBZ-1727](https://issues.redhat.com/browse/DBZ-1727) problem

## Preparation
Before running test we should build debezium from sources
```
mvn clean install -Passembly -DskipITs -DskipTests
```

Done! Now we can use debezium from maven's local repository. We can move to
the next step to execute tests. But if you have questions about building
debezium from sources you can read more [here](https://github.com/debezium/debezium#building-the-code)
and [here](https://github.com/debezium/debezium/blob/master/CONTRIBUTE.md#building-locally)

## Tests Execution
```
./gradlew clean test
```

## About Tests
There are two tests:
1. Checks how Debezium's connector will behave by default
2. Checks how Debezium's connector will behave when heartbeat is enabled

That's it!

## Results
Current Debezium PostgreSQL connector's implementation
will keep running in both cases, while DBZ-1727 will
stop a connector when heartbeat is enabled

## GitHub Actions
If you don't want to run tests manually, but interested in results,
you can check out GitHub Actions [page](https://github.com/igabaydulin/debezium-postgres-shutdown/actions) for this repository

