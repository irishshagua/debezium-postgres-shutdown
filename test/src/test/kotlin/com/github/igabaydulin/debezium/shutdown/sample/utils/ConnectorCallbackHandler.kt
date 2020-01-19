package com.github.igabaydulin.debezium.shutdown.sample.utils

import io.debezium.embedded.EmbeddedEngine
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.CountDownLatch

class ConnectorCallbackHandler : EmbeddedEngine.ConnectorCallback {

    val isConnectorStarted = CountDownLatch(1)
    val isConnectorStopped = CountDownLatch(1)

    companion object {
        val log: Logger = LoggerFactory.getLogger(ConnectorCallbackHandler::class.java)

        fun newInstance(): ConnectorCallbackHandler {
            return ConnectorCallbackHandler()
        }
    }

    override fun connectorStopped() {
        log.debug("Connector is stopped")
        isConnectorStopped.countDown()
    }

    override fun connectorStarted() {
        log.debug("Connector is running")
        isConnectorStarted.countDown()
    }
}