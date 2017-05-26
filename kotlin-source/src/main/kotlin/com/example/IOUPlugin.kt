package com.example

class IOUPlugin : net.corda.core.node.CordaPluginRegistry() {
    /**
     * A list of long lived services to be hosted within the node. Typically you would use these to register flow
     * factories that would be used when an initiating party attempts to communicate with our node using a particular
     * flow.
     */
    override val servicePlugins = listOf(java.util.function.Function(IOUService::Service))

    object IOUService {
        class Service(services: net.corda.core.node.PluginServiceHub) {
            init {
                services.registerServiceFlow(com.example.IOUFlow.Initiator::class.java) { com.example.IOUFlow.Acceptor(it) }
            }
        }
    }
}