package com.example.exampleplugin.waystone;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * ECS system that listens for block break events and removes waystones from registry when broken.
 * Blocks removal if user has the blockWaystoneRemoval permission (deny list).
 */
public class WaystoneBreakHandler extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    
    // All waystone block variants (base + colors)
    private static final String WAYSTONE_BLOCK_ID = "Warp_Block";
    private static final String WAYSTONE_BLOCK_ID_RED = "Warp_Block_Red";
    private static final String WAYSTONE_BLOCK_ID_GREEN = "Warp_Block_Green";

    public WaystoneBreakHandler() {
        super(BreakBlockEvent.class);
    }

    /**
     * Checks if a block type ID is any waystone variant.
     */
    private boolean isWaystoneBlock(String blockTypeId) {
        // Check for exact match or namespace-prefixed match (e.g., "exampleplugin:Warp_Block")
        return blockTypeId.equals(WAYSTONE_BLOCK_ID) || blockTypeId.endsWith(":" + WAYSTONE_BLOCK_ID)
                || blockTypeId.equals(WAYSTONE_BLOCK_ID_RED) || blockTypeId.endsWith(":" + WAYSTONE_BLOCK_ID_RED)
                || blockTypeId.equals(WAYSTONE_BLOCK_ID_GREEN) || blockTypeId.endsWith(":" + WAYSTONE_BLOCK_ID_GREEN);
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
                       @Nonnull BreakBlockEvent event) {
        String blockTypeId = event.getBlockType().getId();
        
        // Only process waystone blocks (all color variants)
        if (!isWaystoneBlock(blockTypeId)) {
            return;
        }
        
        String worldName = store.getExternalData().getWorld().getName();
        var position = event.getTargetBlock();
        
        // Get the player's UUID from the entity
        UUIDComponent uuidComponent = archetypeChunk.getComponent(index, UUIDComponent.getComponentType());
        if (uuidComponent == null) {
            return;
        }
        UUID playerUuid = uuidComponent.getUuid();
        
        boolean playerIsOp = PermissionUtils.isOp(playerUuid);
        
        // Get the waystone at this position (if any)
        Waystone waystone = WaystoneRegistry.get().getByPosition(worldName, position.x, position.y, position.z);
        
        // Check if waystone is server-owned - only OPs can break server-owned waystones
        if (waystone != null && waystone.isServerOwned() && !playerIsOp) {
            event.setCancelled(true);
            // Send message to player
            Player playerComponent = archetypeChunk.getComponent(index, Player.getComponentType());
            if (playerComponent != null) {
                playerComponent.sendMessage(Message.raw("You do not have permission to break this Waystone"));
            }
            if (WaystoneRegistry.isDebugEnabled()) {
                LOGGER.atInfo().log("Blocked server-owned waystone removal for user %s at %s (%d, %d, %d)",
                        playerUuid, worldName, position.x, position.y, position.z);
            }
            return;
        }
        
        // Check if waystone is private and player is not the owner
        // OPs and players with allowPrivateWaystoneRemoval permission can break private waystones
        if (waystone != null && !waystone.isPublic() && !waystone.isOwnedBy(playerUuid.toString())) {
            if (!playerIsOp && !PermissionUtils.hasPermission(playerUuid, WaystonePermissions.ALLOW_PRIVATE_WAYSTONE_REMOVAL)) {
                event.setCancelled(true);
                Player playerComponent = archetypeChunk.getComponent(index, Player.getComponentType());
                if (playerComponent != null) {
                    playerComponent.sendMessage(Message.raw("You do not have permission to break this Waystone"));
                }
                if (WaystoneRegistry.isDebugEnabled()) {
                    LOGGER.atInfo().log("Blocked private waystone removal for user %s at %s (%d, %d, %d) - not owner",
                            playerUuid, worldName, position.x, position.y, position.z);
                }
                return;
            }
        }
        
        // Check if user has the blockWaystoneRemoval permission (deny list)
        // OPs bypass the deny permission
        if (!playerIsOp && PermissionUtils.hasPermission(playerUuid, WaystonePermissions.BLOCK_WAYSTONE_REMOVAL)) {
            // Cancel the break
            event.setCancelled(true);
            if (WaystoneRegistry.isDebugEnabled()) {
                LOGGER.atInfo().log("Blocked waystone removal for user %s at %s (%d, %d, %d) - has blockWaystoneRemoval permission",
                        playerUuid, worldName, position.x, position.y, position.z);
            }
            return;
        }
        
        if (waystone != null) {
            if (WaystoneRegistry.isDebugEnabled()) {
                LOGGER.atInfo().log("Waystone '%s' broken at %s (%d, %d, %d), removing from registry",
                        waystone.getName(), worldName, position.x, position.y, position.z);
            }
            WaystoneRegistry.get().unregister(waystone.getId());
        } else if (WaystoneRegistry.isDebugEnabled()) {
            LOGGER.atWarning().log("Waystone block broken at %s (%d, %d, %d) but no waystone found in registry",
                    worldName, position.x, position.y, position.z);
        }
    }
}
