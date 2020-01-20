package com.github.igabaydulin.debezium.shutdown.sample

import com.github.igabaydulin.debezium.shutdown.sample.utils.ConnectorCallbackHandler
import com.github.igabaydulin.debezium.shutdown.sample.utils.Engines
import com.github.igabaydulin.debezium.shutdown.sample.utils.PostgresContainer
import io.debezium.embedded.EmbeddedEngine
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

class DebeziumPostgresHeartbeatShutdownTest {

    companion object {
        val postgres: PostgresContainer = PostgresContainer.newInstance()
        val callback: ConnectorCallbackHandler = ConnectorCallbackHandler.newInstance()
    }

    private lateinit var engine: EmbeddedEngine

    @BeforeTest
    fun setUp() {
        postgres.start()
        engine = Engines.heartbeatEngine(postgres, callback)
    }

    @AfterTest
    fun finish() {
        engine.stop()
    }

    @Test
    fun shutdown_connector_when_postgres_tries_to_shutdown_and_heartbeat_is_enabled() {
        Engines.startEngine(engine, callback)
        postgres.insertQuery()
        postgres.shutdown()

        val isCallbackApplied = callback.isConnectorStopped.await(30, TimeUnit.SECONDS)
        Assert.assertTrue("Timeout on waiting callback to be applied", isCallbackApplied)
        Assert.assertFalse("Connector is still running", engine.isRunning)
    }
}
