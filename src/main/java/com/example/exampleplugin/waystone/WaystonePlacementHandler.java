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
        // Create the waystone with a temporary name
        Waystone newWaystone = Waystone.create(
                "New Waystone",
                world,
                position,
                yaw,
                UUID.fromString(playerUuid),
                playerName,
                true // Default to public
        );

        // Register it immediately
        WaystoneRegistry.get().register(newWaystone);

        // Open naming page
        return new WaystoneNamingPage(
                playerRef,
                newWaystone.getId(),
                null, // No current name (new waystone)
                true, // Is new waystone
                newName -> {
                    // Update the name when submitted
                    WaystoneRegistry.get().updateName(newWaystone.getId(), newName);
                },
                () -> {
                    // On cancel - keep the default name "New Waystone"
                    // The waystone is already registered
                }
        );
    }

    private void openRenamePage(@Nonnull PlayerRef playerRef,
                                 @Nonnull String waystoneId,
                                 @Nonnull String currentName,
                                 @Nonnull Store<EntityStore> store,
                                 @Nonnull Ref<EntityStore> ref) {
        Player playerComponent = (Player) store.getComponent(ref, Player.getComponentType());
        if (playerComponent == null) return;

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
}
