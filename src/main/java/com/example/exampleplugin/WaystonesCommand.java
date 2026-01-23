package com.example.exampleplugin;

import com.example.exampleplugin.waystone.WaystoneListPage;
import com.example.exampleplugin.waystone.WaystoneSettingsPage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * Command for waystone management.
 * /waystones - Prints plugin info
 * /waystones list - Opens the waystone menu (ops only)
 */
public class WaystonesCommand extends CommandBase {
    private final String pluginName;
    private final String pluginVersion;

    public WaystonesCommand(String pluginName, String pluginVersion) {
        super("waystones", "Waystone plugin commands.");
        this.pluginName = pluginName;
        this.pluginVersion = pluginVersion;
        
        // Add subcommands
        addSubCommand((AbstractCommand) new ListCommand());
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        ctx.sendMessage(Message.raw(pluginName + " v" + pluginVersion));
    }

    /**
     * Subcommand: /waystones list
     * Opens the waystone menu for the player. Ops only.
     */
    private static class ListCommand extends AbstractPlayerCommand {
        
        public ListCommand() {
            super("list", "Opens the waystone menu.");
            // null permission group = OP only
            setPermissionGroup(null);
        }

        @Override
        protected void execute(@Nonnull CommandContext context,
                               @Nonnull Store<EntityStore> store,
                               @Nonnull Ref<EntityStore> ref,
                               @Nonnull PlayerRef playerRef,
                               @Nonnull World world) {
            
            Player playerComponent = (Player) store.getComponent(ref, Player.getComponentType());
            if (playerComponent == null) {
                context.sendMessage(Message.raw("Error: Could not get player component."));
                return;
            }

            String playerUuid = playerRef.getUuid().toString();

            // Create the waystone list page (no current waystone since opened via command)
            WaystoneListPage listPage = new WaystoneListPage(
                    playerRef,
                    playerUuid,
                    null, // No current waystone
                    waystone -> {
                        // On teleport callback
                        if (waystone != null) {
                            Teleport teleport = waystone.toTeleport();
                            if (teleport != null) {
                                store.addComponent(ref, Teleport.getComponentType(), teleport);
                            }
                        }
                    },
                    () -> { /* on rename - not applicable from command */ },
                    () -> { /* on settings - not applicable from command */ },
                    // On edit waystone callback (for ops gear icon)
                    waystoneId -> {
                        WaystoneSettingsPage settingsPage = new WaystoneSettingsPage(
                                playerRef,
                                playerUuid,
                                waystoneId,
                                () -> { /* on back - just close */ }
                        );
                        playerComponent.getPageManager().openCustomPage(ref, store, settingsPage);
                    }
            );

            // Open the page
            playerComponent.getPageManager().openCustomPage(ref, store, listPage);
        }
    }
}
