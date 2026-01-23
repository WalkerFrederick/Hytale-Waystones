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

        // Clean up any orphaned waystones in this world before proceeding
        WaystoneRegistry.get().cleanupOrphanedWaystones(world);

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
            // New waystone - create it and open naming page
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
                        boolean isCrossWorld = !waystone.getWorldName().equals(currentWorld.getName());
                        System.out.println("[Waystone] === TELEPORT REQUEST ===");
                        System.out.println("[Waystone] Waystone: '" + waystone.getName() + "' (ID: " + waystone.getId() + ")");
                        System.out.println("[Waystone] Target world: '" + waystone.getWorldName() + "' (cross-world: " + isCrossWorld + ")");
                        System.out.println("[Waystone] Teleport pos: (" + 
                                teleport.getPosition().x + ", " + teleport.getPosition().y + ", " + teleport.getPosition().z + ")");
                        System.out.println("[Waystone] =========================");
                        
                        store.addComponent(ref, com.hypixel.hytale.server.core.modules.entity.teleport.Teleport.getComponentType(), teleport);
                    } else {
                        System.out.println("[Waystone] toTeleport returned null!");
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
                            true // Default to public
                    );
                    WaystoneRegistry.get().register(newWaystone);
                    System.out.println("[Waystone] Created new waystone: '" + newName + "' at (" + x + ", " + y + ", " + z + ") in world '" + worldName + "'");
                },
                () -> {
                    // On cancel - don't register the waystone
                    // Next time user clicks the block, they'll see the naming dialog again
                    System.out.println("[Waystone] Waystone creation cancelled");
                }
        );
    }

    private void openRenamePage(@Nonnull PlayerRef playerRef,
                                 @Nonnull String waystoneId,
                                 @Nonnull String currentName,
                                 @Nonnull Store<EntityStore> store,
                                 @Nonnull Ref<EntityStore> ref) {
        System.out.println("[Waystone] openRenamePage called for waystone: " + waystoneId);
        System.out.println("[Waystone] Current name: " + currentName);
        
        Player playerComponent = (Player) store.getComponent(ref, Player.getComponentType());
        if (playerComponent == null) {
            System.out.println("[Waystone] ERROR: playerComponent is null!");
            return;
        }
        System.out.println("[Waystone] Got player component, opening naming page...");

        WaystoneNamingPage namingPage = new WaystoneNamingPage(
                playerRef,
                waystoneId,
                currentName,
                false,
                newName -> WaystoneRegistry.get().updateName(waystoneId, newName),
                () -> { /* cancelled */ }
        );

        System.out.println("[Waystone] Calling openCustomPage for naming page...");
        playerComponent.getPageManager().openCustomPage(ref, store, namingPage);
        System.out.println("[Waystone] openCustomPage called successfully");
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
