package com.github.igabaydulin.debezium.shutdown.sample

import io.debezium.config.Configuration
import io.debezium.connector.postgresql.PostgresConnector
import io.debezium.connector.postgresql.PostgresConnectorConfig
import io.debezium.embedded.EmbeddedEngine
import org.apache.kafka.connect.runtime.WorkerConfig
import org.apache.kafka.connect.source.SourceRecord
import org.apache.kafka.connect.storage.MemoryOffsetBackingStore
import org.apache.kafka.connect.storage.OffsetBackingStore
import org.apache.kafka.connect.util.Callback
import org.junit.Assert
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testcontainers.containers.Container
import org.testcontainers.containers.GenericContainer
import org.testcontainers.images.builder.ImageFromDockerfile
import java.nio.ByteBuffer
import java.sql.DriverManager
import java.sql.Statement
import java.util.concurrent.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class PostgresContainer(imageName: Future<String>) : GenericContainer<PostgresContainer>(imageName)

class ConnectorStartedCallback : EmbeddedEngine.ConnectorCallback {

    val isConnectorStarted = CountDownLatch(1)
    val isConnectorStopped = CountDownLatch(1)

    override fun connectorStopped() {
        EngineTest.log.info("Connector is stopped")
        isConnectorStopped.countDown()
    }

    override fun connectorStarted() {
        EngineTest.log.info("Connector is running")
        isConnectorStarted.countDown()
    }
}

class EngineTest {

    companion object {
        val log: Logger = LoggerFactory.getLogger(EngineTest::class.java)
        val postgres: PostgresContainer = PostgresContainer(
                ImageFromDockerfile()
                        .withFileFromClasspath("Dockerfile", "docker/postgresql/Dockerfile")
                        .withFileFromClasspath("init.sql", "docker/postgresql/init.sql")
        ).withExposedPorts(5432)
        val callback: ConnectorStartedCallback = ConnectorStartedCallback()
    }

    private lateinit var engine: EmbeddedEngine

    @BeforeTest
    fun setUp() {
        postgres.start()
        engine = EmbeddedEngine.create()
                .using(Configuration.from(mutableMapOf(
                        EmbeddedEngine.ENGINE_NAME.name() to "embedded-debezium-connector",
                        EmbeddedEngine.CONNECTOR_CLASS.name() to PostgresConnector::class.java.canonicalName,
                        EmbeddedEngine.OFFSET_STORAGE.name() to MemoryOffsetBackingStore::class.java.canonicalName,
                        EmbeddedEngine.OFFSET_FLUSH_INTERVAL_MS.name() to 1000L,
                        PostgresConnectorConfig.HOSTNAME.name() to postgres.containerIpAddress,
                        PostgresConnectorConfig.PORT.name() to postgres.firstMappedPort,
                        PostgresConnectorConfig.USER.name() to "alice",
                        PostgresConnectorConfig.PASSWORD.name() to "123456",
                        PostgresConnectorConfig.DATABASE_NAME.name() to "foo",
                        PostgresConnectorConfig.SERVER_NAME.name() to "test",
                        PostgresConnectorConfig.SLOT_NAME.name() to "embedded_debezium_slot",
                        PostgresConnectorConfig.TABLE_WHITELIST.name() to "public.apples"
                )))
                .using(callback)
                .notifying { record: SourceRecord ->
                    run {
                        log.info("Received record: {}", record)
                    }
                }
                .build()
    }

    @AfterTest
    fun finish() {
        //engine.stop()
    }

    fun executeQuery(statement: Statement, query: String): Boolean {
        TimeUnit.MILLISECONDS.sleep(1)
        log.info("Executing query: {}", query)
        return statement.execute(query)
    }

    @Suppress("SqlNoDataSourceInspection", "SqlResolve")
    @Test
    fun test() {
        ForkJoinPool.commonPool().execute { engine.run() }
        log.info("Waiting for connector to start")
        callback.isConnectorStarted.await()
        log.info("Connector is started")

        val runStatements = Thread {
            DriverManager.getConnection("jdbc:postgresql://${postgres.containerIpAddress}:${postgres.firstMappedPort}/foo", "alice", "123456").use {
                log.info("Connection established")
                val statement: Statement = it.createStatement()
                repeat(1) it@{
                    if (executeQuery(statement, "INSERT INTO apples VALUES (1, 'Golden Apple', 4.45, '{\"description\": \"Best apple ever\"}');")) {
                        return@it
                    }
                }
            }
        }
        TimeUnit.SECONDS.sleep(2)
        runStatements.start()
        log.info("Sleep a bit in main thread")
        TimeUnit.SECONDS.sleep(2)
        log.info("Shutting down postgres")
        val result: Container.ExecResult = postgres.execInContainer(
                "/usr/lib/postgresql/12/bin/pg_ctl",
                "stop",
                "-D",
                "/var/lib/postgresql/data")
        log.info("Result $result")
        callback.isConnectorStopped.await(5, TimeUnit.SECONDS)
        Assert.assertFalse("Connector is still running", engine.isRunning)
        log.info("Connector isRunning: ${engine.isRunning}")
    }
}
