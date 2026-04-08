package com.astryxion.backtools;

import com.astryxion.backtools.client.core.ClientModBusEvents;
import com.astryxion.backtools.client.core.EventHandler;
import com.astryxion.backtools.common.BackTools;
import com.astryxion.backtools.common.core.BackToolsConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

public final class BackToolsClient implements ClientModInitializer
{
	@Override
	public void onInitializeClient()
	{
		BackToolsConfig.bootstrap();
		ClientModBusEvents.registerPlayerLayer();
		EventHandler.registerFabricEvents();
		BackToolsConfig.startConfigFileWatcher();
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
			BackTools.processImcQueue();
			EventHandler.setupConfig();
		});
		EventHandler.setupConfig();
	}
}
