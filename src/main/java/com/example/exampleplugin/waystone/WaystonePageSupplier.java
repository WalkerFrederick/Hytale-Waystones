package com.example.exampleplugin.waystone;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Page supplier for the Waystone list UI.
 * Opens the waystone menu when a player uses a waystone block.
 */
public class WaystonePageSupplier implements OpenCustomUIInteraction.CustomPageSupplier {

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

        // Get player UUID
        Player playerComponent = (Player) componentAccessor.getComponent(ref, Player.getComponentType());
        if (playerComponent == null) {
            return null;
        }

        String playerUuid = playerRef.getUuid().toString();
        Store<EntityStore> store = ref.getStore();
        World world = ((EntityStore) store.getExternalData()).getWorld();

        // Find the waystone at this position
        Waystone currentWaystone = WaystoneRegistry.get().getByPosition(
                world.getName(),
                targetBlock.x,
                targetBlock.y,
                targetBlock.z
        );

        String currentWaystoneId = currentWaystone != null ? currentWaystone.getId() : null;

        // Create the waystone list page
        return new WaystoneListPage(
                playerRef,
                playerUuid,
                currentWaystoneId,
                // On teleport callback
                waystone -> {
                    Teleport teleport = waystone.toTeleport();
                    if (teleport != null) {
                        store.addComponent(ref, Teleport.getComponentType(), teleport);
                    }
                },
                // On rename callback - opens the naming page
                () -> {
                    if (currentWaystoneId != null && currentWaystone != null) {
                        // Only allow owner to rename
                        if (currentWaystone.isOwnedBy(playerUuid)) {
                            openNamingPage(playerRef, currentWaystoneId, currentWaystone.getName());
                        }
                    }
                }
        );
    }

    /**
     * Opens the naming page for renaming a waystone.
     */
    private void openNamingPage(@Nonnull PlayerRef playerRef,
                                @Nonnull String waystoneId,
                                @Nonnull String currentName) {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null) return;

        Store<EntityStore> store = ref.getStore();
        Player playerComponent = (Player) store.getComponent(ref, Player.getComponentType());
        if (playerComponent == null) return;

        WaystoneNamingPage namingPage = new WaystoneNamingPage(
                playerRef,
                waystoneId,
                currentName,
                false, // Not a new waystone
                newName -> {
                    WaystoneRegistry.get().updateName(waystoneId, newName);
                },
                () -> {
                    // On cancel - do nothing
                }
        );

        playerComponent.getPageManager().openCustomPage(ref, store, namingPage);
    }
}
