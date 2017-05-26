package com.example.plugin

import com.example.flow.IOUFlow
import net.corda.core.node.CordaPluginRegistry
import net.corda.core.node.PluginServiceHub
import java.util.function.Function

class IOUPlugin : CordaPluginRegistry() {
    /**
     * A list of long lived services to be hosted within the node. Typically you would use these to register flow
     * factories that would be used when an initiating party attempts to communicate with our node using a particular
     * flow.
     */
    override val servicePlugins = listOf(Function(IOUService::Service))

    object IOUService {
        class Service(services: PluginServiceHub) {
            init {
                services.registerServiceFlow(IOUFlow.Initiator::class.java) { IOUFlow.Acceptor(it) }
            }
        }
    }
}