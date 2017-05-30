package com.iou;

import com.google.common.collect.ImmutableList;
import net.corda.core.node.CordaPluginRegistry;
import net.corda.core.node.PluginServiceHub;

import java.util.List;
import java.util.function.Function;

public class IOUPlugin extends CordaPluginRegistry {
    /**
     * A list of long lived services to be hosted within the node. Typically you would use these to register flow
     * factories that would be used when an initiating party attempts to communicate with our node using a particular
     * flow. See the [ExampleService.Service] class for an implementation which sets up a
     */
    private final List<Function<PluginServiceHub, ?>> servicePlugins = ImmutableList.of(IOUService::new);

    @Override public List<Function<PluginServiceHub, ?>> getServicePlugins() { return servicePlugins; }
}