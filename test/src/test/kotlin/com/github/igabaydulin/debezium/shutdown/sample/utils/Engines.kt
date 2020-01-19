package com.github.igabaydulin.debezium.shutdown.sample.utils

import io.debezium.config.Configuration
import io.debezium.connector.postgresql.PostgresConnector
import io.debezium.connector.postgresql.PostgresConnectorConfig
import io.debezium.embedded.EmbeddedEngine
import io.debezium.heartbeat.Heartbeat
import org.apache.kafka.connect.source.SourceRecord
import org.apache.kafka.connect.storage.MemoryOffsetBackingStore
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeUnit

class Engines {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(Engines::class.java)

        private val coreEngineConfiguration = mapOf(
                EmbeddedEngine.ENGINE_NAME.name() to "embedded-debezium-connector",
                EmbeddedEngine.CONNECTOR_CLASS.name() to PostgresConnector::class.java.canonicalName,
                EmbeddedEngine.OFFSET_STORAGE.name() to MemoryOffsetBackingStore::class.java.canonicalName,
                EmbeddedEngine.OFFSET_FLUSH_INTERVAL_MS.name() to 1000L,
                PostgresConnectorConfig.USER.name() to "alice",
                PostgresConnectorConfig.PASSWORD.name() to "123456",
                PostgresConnectorConfig.DATABASE_NAME.name() to "foo",
                PostgresConnectorConfig.SERVER_NAME.name() to "test",
                PostgresConnectorConfig.SLOT_NAME.name() to "embedded_debezium_slot",
                PostgresConnectorConfig.TABLE_WHITELIST.name() to "public.bar"
        )

        private fun defaultPostgresConfiguration(postgresContainer: PostgresContainer): HashMap<String, Any> {
            val configurationMap = HashMap(coreEngineConfiguration)
            configurationMap[PostgresConnectorConfig.HOSTNAME.name()] = postgresContainer.containerIpAddress
            configurationMap[PostgresConnectorConfig.PORT.name()] = postgresContainer.firstMappedPort

            return configurationMap
        }

        private fun defaultConfiguration(postgresContainer: PostgresContainer): Configuration {
            return Configuration.from(defaultPostgresConfiguration(postgresContainer))
        }

        private fun heartbeatConfiguration(postgresContainer: PostgresContainer): Configuration {
            val configurationMap = defaultPostgresConfiguration(postgresContainer)
            configurationMap[Heartbeat.HEARTBEAT_INTERVAL.name()] = 1

            return Configuration.from(configurationMap)
        }

        private fun buildEngine(callback: EmbeddedEngine.ConnectorCallback, configuration: Configuration): EmbeddedEngine {
            return EmbeddedEngine.create()
                    .using(configuration)
                    .using(callback)
                    .notifying { record: SourceRecord ->
                        run {
                            log.debug("Received record: {}", record)
                        }
                    }
                    .build()
        }

        fun defaultEngine(postgresContainer: PostgresContainer, callback: EmbeddedEngine.ConnectorCallback): EmbeddedEngine {
            return buildEngine(callback, defaultConfiguration(postgresContainer))
        }

        fun heartbeatEngine(postgresContainer: PostgresContainer, callback: EmbeddedEngine.ConnectorCallback): EmbeddedEngine {
            return buildEngine(callback, heartbeatConfiguration(postgresContainer))
        }

        fun startEngine(engine: EmbeddedEngine, callback: ConnectorCallbackHandler) {
            ForkJoinPool.commonPool().execute { engine.run() }
            log.debug("Waiting for connector to start")
            callback.isConnectorStarted.await()
            log.debug("Waiting connector to be ready to read WAL log")
            TimeUnit.SECONDS.sleep(10)
            log.debug("Connector is started")
        }
    }
}