package com.example.exampleplugin.waystone;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
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
    private static final String BLOCK_PLACEMENT_PERMISSION = "hytale.command.waystones.blockWaystonePlacement";

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
        if (!isOp(playerUuid) && hasPermission(playerUuid, BLOCK_PLACEMENT_PERMISSION)) {
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
    
    /**
     * Checks if a user is an OP.
     */
    private static boolean isOp(UUID uuid) {
        for (var provider : PermissionsModule.get().getProviders()) {
            if (provider.getGroupsForUser(uuid).contains("OP")) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Checks if a user has a specific permission (either directly or via their groups).
     */
    private static boolean hasPermission(UUID uuid, String permission) {
        for (var provider : PermissionsModule.get().getProviders()) {
            // Check direct user permissions
            if (provider.getUserPermissions(uuid).contains(permission)) {
                return true;
            }
            
            // Check group permissions
            for (String group : provider.getGroupsForUser(uuid)) {
                if (provider.getGroupPermissions(group).contains(permission)) {
                    return true;
                }
            }
        }
        return false;
    }
}
