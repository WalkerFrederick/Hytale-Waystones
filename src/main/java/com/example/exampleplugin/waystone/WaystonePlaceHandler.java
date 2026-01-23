package com.example.exampleplugin.waystone;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * ECS system that listens for block place events for waystones.
 * Blocks placement if user has the blockPlacement permission (deny list).
 */
public class WaystonePlaceHandler extends EntityEventSystem<EntityStore, PlaceBlockEvent> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String WAYSTONE_BLOCK_ID = "Warp_Block";

    public WaystonePlaceHandler() {
        super(PlaceBlockEvent.class);
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();  // Match any entity
    }

    @Override
    public void handle(int index,
                       @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull PlaceBlockEvent event) {
        var itemInHand = event.getItemInHand();
        if (itemInHand == null) {
            return;
        }
        
        String itemId = itemInHand.getItemId();
        
        // Only process waystone blocks
        if (!itemId.equals(WAYSTONE_BLOCK_ID) && !itemId.endsWith(":" + WAYSTONE_BLOCK_ID)) {
            return;
        }
        
        var position = event.getTargetBlock();
        String worldName = store.getExternalData().getWorld().getName();
        
        // Get the player's UUID from the entity
        UUIDComponent uuidComponent = archetypeChunk.getComponent(index, UUIDComponent.getComponentType());
        if (uuidComponent == null) {
            return;
        }
        UUID playerUuid = uuidComponent.getUuid();
        
        // Check if user has the blockWaystonePlacement permission (deny list)
        // OPs bypass the deny permission
        if (!PermissionUtils.isOp(playerUuid) && PermissionUtils.hasPermission(playerUuid, WaystonePermissions.BLOCK_WAYSTONE_PLACEMENT)) {
            // Cancel the placement
            event.setCancelled(true);
            if (WaystoneRegistry.isDebugEnabled()) {
                LOGGER.atInfo().log("Blocked waystone placement for user %s at %s (%d, %d, %d) - has blockWaystonePlacement permission",
                        playerUuid, worldName, position.x, position.y, position.z);
            }
            return;
        }
        
        if (WaystoneRegistry.isDebugEnabled()) {
            LOGGER.atInfo().log("User %s placing waystone at %s (%d, %d, %d)",
                    playerUuid, worldName, position.x, position.y, position.z);
        }
    }
}
