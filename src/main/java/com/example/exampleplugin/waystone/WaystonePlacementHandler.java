package com.example.exampleplugin.waystone;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Handles waystone placement and opens the naming dialog for new waystones.
 * This is used as a page supplier that checks if a waystone exists at the location.
 * If not, it creates a new one and opens the naming dialog.
 * If it exists, it opens the waystone list.
 */
public class WaystonePlacementHandler implements OpenCustomUIInteraction.CustomPageSupplier {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Nullable
    @Override
    public CustomUIPage tryCreate(@Nonnull Ref<EntityStore> ref,
                                   @Nonnull ComponentAccessor<EntityStore> componentAccessor,
                                   @Nonnull PlayerRef playerRef,
                                   @Nonnull InteractionContext context) {
        BlockPosition targetBlock = context.getTargetBlock();
        if (targetBlock == null) {
            return null;
        }

        // Get player info
        Player playerComponent = (Player) componentAccessor.getComponent(ref, Player.getComponentType());
        if (playerComponent == null) {
            return null;
        }

        String playerUuid = playerRef.getUuid().toString();
        String playerName = playerRef.getUsername();
        Store<EntityStore> store = ref.getStore();
        World world = ((EntityStore) store.getExternalData()).getWorld();

        // Check if a waystone already exists at this position
        Waystone existingWaystone = WaystoneRegistry.get().getByPosition(
                world.getName(),
                targetBlock.x,
                targetBlock.y,
                targetBlock.z
        );

        if (existingWaystone != null) {
            // Existing waystone - open the list page
            return createListPage(playerRef, playerUuid, existingWaystone.getId(), store, ref);
        } else {
            // New waystone - check max waystones limit first
            UUID playerUuidObj = UUID.fromString(playerUuid);
            
            // OPs bypass the limit
            if (!PermissionUtils.isOp(playerUuidObj)) {
                int maxWaystones = PermissionUtils.getMaxWaystonesLimit(playerUuidObj);
                if (maxWaystones >= 0) { // -1 means no limit
                    int currentCount = WaystoneRegistry.get().countByOwner(playerUuid);
                    if (currentCount >= maxWaystones) {
                        // Over limit - send message and don't open naming page
                        playerComponent.sendMessage(Message.raw("You have reached your maximum waystone limit (" + maxWaystones + ")"));
                        return null;
                    }
                }
            }
            
            // Create new waystone and open naming page
            return createNewWaystoneAndNamingPage(
                    playerRef,
                    playerUuid,
                    playerName,
                    world,
                    new Vector3d(targetBlock.x, targetBlock.y, targetBlock.z),
                    0f, // Yaw not used for spawn position (always spawn south of block)
                    store,
                    ref
            );
        }
    }

    private CustomUIPage createListPage(@Nonnull PlayerRef playerRef,
                                         @Nonnull String playerUuid,
                                         @Nonnull String currentWaystoneId,
                                         @Nonnull Store<EntityStore> store,
                                         @Nonnull Ref<EntityStore> ref) {
        Waystone currentWaystone = WaystoneRegistry.get().get(currentWaystoneId);

        // Get current world from store for logging
        final World currentWorld = ((EntityStore) store.getExternalData()).getWorld();
        
        return new WaystoneListPage(
                playerRef,
                playerUuid,
                currentWaystoneId,
                waystone -> {
                    // Teleport to selected waystone
                    var teleport = waystone.toTeleport();
                    if (teleport != null) {
                        if (WaystoneRegistry.isDebugEnabled()) {
                            boolean isCrossWorld = !waystone.getWorldName().equals(currentWorld.getName());
                            LOGGER.atInfo().log("Teleport request: '%s' (ID: %s) to world '%s' (cross-world: %s) at (%.1f, %.1f, %.1f)",
                                    waystone.getName(), waystone.getId(), waystone.getWorldName(), isCrossWorld,
                                    teleport.getPosition().x, teleport.getPosition().y, teleport.getPosition().z);
                        }
                        store.addComponent(ref, com.hypixel.hytale.server.core.modules.entity.teleport.Teleport.getComponentType(), teleport);
                        
                        // Show arrival banner
                        EventTitleUtil.showEventTitleToPlayer(
                                playerRef,
                                Message.raw(waystone.getName()),           // Primary title: waystone name
                                Message.raw("Waystone"),                   // Secondary title: placeholder
                                true,                                      // isMajor: large banner style
                                null,                                      // icon: none for now
                                3.0f,                                      // duration
                                1.0f,                                      // fadeInDuration
                                1.0f                                       // fadeOutDuration
                        );
                    } else {
                        LOGGER.atWarning().log("toTeleport returned null for waystone '%s'", waystone.getName());
                    }
                },
                () -> {
                    // Rename callback
                    if (currentWaystone != null && currentWaystone.isOwnedBy(playerUuid)) {
                        openRenamePage(playerRef, currentWaystoneId, currentWaystone.getName(), store, ref);
                    }
                },
                () -> {
                    // Settings callback
                    openSettingsPage(playerRef, playerUuid, currentWaystoneId, store, ref);
                },
                // On edit waystone callback (for ops gear icon)
                waystoneId -> {
                    openSettingsPage(playerRef, playerUuid, waystoneId, store, ref);
                }
        );
    }

    private CustomUIPage createNewWaystoneAndNamingPage(@Nonnull PlayerRef playerRef,
                                                         @Nonnull String playerUuid,
                                                         @Nonnull String playerName,
                                                         @Nonnull World world,
                                                         @Nonnull Vector3d position,
                                                         float yaw,
                                                         @Nonnull Store<EntityStore> store,
                                                         @Nonnull Ref<EntityStore> ref) {
        // Prepare waystone data but don't register yet - only register when user confirms name
        final String finalPlayerUuid = playerUuid;
        final String finalPlayerName = playerName;
        final String worldName = world.getName();
        final double x = position.x;
        final double y = position.y;
        final double z = position.z;
        
        // Check if user has blockPublicWaystoneCreation permission - if so, default to private
        // OPs always default to public
        UUID playerUuidObj = UUID.fromString(playerUuid);
        final boolean defaultToPublic = PermissionUtils.isOp(playerUuidObj) || 
                !PermissionUtils.hasPermission(playerUuidObj, WaystonePermissions.BLOCK_PUBLIC_WAYSTONE_CREATION);

        // Open naming page - waystone will be created and registered only on submit
        return new WaystoneNamingPage(
                playerRef,
                null, // No waystone ID yet - it will be created on submit
                null, // No current name (new waystone)
                true, // Is new waystone
                newName -> {
                    // Create and register the waystone (name already validated in naming page)
                    Waystone newWaystone = Waystone.create(
                            newName,
                            world,
                            position,
                            yaw,
                            UUID.fromString(finalPlayerUuid),
                            finalPlayerName,
                            defaultToPublic
                    );
                    WaystoneRegistry.get().register(newWaystone);
                    if (WaystoneRegistry.isDebugEnabled()) {
                        LOGGER.atInfo().log("Created new waystone: '%s' at (%.0f, %.0f, %.0f) in world '%s' (public: %s)",
                                newName, x, y, z, worldName, defaultToPublic);
                    }
                },
                () -> {
                    // On cancel - don't register the waystone
                    // Next time user clicks the block, they'll see the naming dialog again
                    if (WaystoneRegistry.isDebugEnabled()) {
                        LOGGER.atInfo().log("Waystone creation cancelled");
                    }
                }
        );
    }

    private void openRenamePage(@Nonnull PlayerRef playerRef,
                                 @Nonnull String waystoneId,
                                 @Nonnull String currentName,
                                 @Nonnull Store<EntityStore> store,
                                 @Nonnull Ref<EntityStore> ref) {
        if (WaystoneRegistry.isDebugEnabled()) {
            LOGGER.atInfo().log("openRenamePage called for waystone: %s (current name: %s)", waystoneId, currentName);
        }
        
        Player playerComponent = (Player) store.getComponent(ref, Player.getComponentType());
        if (playerComponent == null) {
            LOGGER.atWarning().log("playerComponent is null in openRenamePage");
            return;
        }

        WaystoneNamingPage namingPage = new WaystoneNamingPage(
                playerRef,
                waystoneId,
                currentName,
                false,
                newName -> WaystoneRegistry.get().updateName(waystoneId, newName),
                () -> { /* cancelled */ }
        );

        playerComponent.getPageManager().openCustomPage(ref, store, namingPage);
    }

    private void openSettingsPage(@Nonnull PlayerRef playerRef,
                                   @Nonnull String playerUuid,
                                   @Nonnull String waystoneId,
                                   @Nonnull Store<EntityStore> store,
                                   @Nonnull Ref<EntityStore> ref) {
        Player playerComponent = (Player) store.getComponent(ref, Player.getComponentType());
        if (playerComponent == null) return;

        Waystone waystone = WaystoneRegistry.get().get(waystoneId);
        if (waystone == null) return;

        WaystoneSettingsPage settingsPage = new WaystoneSettingsPage(
                playerRef,
                playerUuid,
                waystoneId,
                () -> {
                    // Back callback - reopen list page
                    Player player = (Player) store.getComponent(ref, Player.getComponentType());
                    if (player != null) {
                        CustomUIPage listPage = createListPage(playerRef, playerUuid, waystoneId, store, ref);
                        player.getPageManager().openCustomPage(ref, store, listPage);
                    }
                }
        );

        playerComponent.getPageManager().openCustomPage(ref, store, settingsPage);
    }
}
