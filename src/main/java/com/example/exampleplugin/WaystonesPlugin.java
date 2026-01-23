package com.example.exampleplugin;

import com.example.exampleplugin.waystone.WaystoneComponent;
import com.example.exampleplugin.waystone.WaystonePlacementHandler;
import com.example.exampleplugin.waystone.WaystoneRegistry;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.events.AllWorldsLoadedEvent;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

/**
 * Main plugin class for the Waystone teleportation system.
 * Provides a standalone waystone network separate from vanilla warps.
 */
public class WaystonesPlugin extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static WaystonesPlugin instance;
    private ComponentType<ChunkStore, WaystoneComponent> waystoneComponentType;

    public WaystonesPlugin(JavaPluginInit init) {
        super(init);
        instance = this;
    }

    /**
     * Gets the plugin instance.
     */
    public static WaystonesPlugin get() {
        return instance;
    }

    @Override
    protected void setup() {
        // Register commands
        this.getCommandRegistry().registerCommand(new WaystonesCommand(this.getName(), this.getManifest().getVersion().toString()));

        // Register the WaystoneComponent type for block entities
        waystoneComponentType = ChunkStore.REGISTRY.registerComponent(
                WaystoneComponent.class,
                WaystoneComponent::new
        );
        WaystoneComponent.setComponentType(waystoneComponentType);

        // Register the Waystone page supplier for block interactions
        OpenCustomUIInteraction.registerCustomPageSupplier(
                this,
                WaystonePlacementHandler.class,
                "Waystone",
                new WaystonePlacementHandler()
        );

        // Register ECS systems for waystone block events
        getEntityStoreRegistry().registerSystem(new com.example.exampleplugin.waystone.WaystoneBreakHandler());
        getEntityStoreRegistry().registerSystem(new com.example.exampleplugin.waystone.WaystonePlaceHandler());

        // Register event to load waystones when worlds are ready
        EventRegistry eventRegistry = getEventRegistry();
        eventRegistry.registerGlobal(AllWorldsLoadedEvent.class, event -> {
            WaystoneRegistry.get().load();
            if (WaystoneRegistry.isDebugEnabled()) {
                LOGGER.atInfo().log("Waystone system initialized with %d waystones", WaystoneRegistry.get().count());
            }
        });

        if (WaystoneRegistry.isDebugEnabled()) {
            LOGGER.atInfo().log("Waystone plugin setup complete");
        }
    }

    @Override
    protected void shutdown() {
        // Save waystones on shutdown
        if (WaystoneRegistry.get().isLoaded()) {
            WaystoneRegistry.get().save();
            if (WaystoneRegistry.isDebugEnabled()) {
                LOGGER.atInfo().log("Saved waystones on shutdown");
            }
        }
    }

    /**
     * Gets the waystone component type.
     */
    public ComponentType<ChunkStore, WaystoneComponent> getWaystoneComponentType() {
        return waystoneComponentType;
    }
}
