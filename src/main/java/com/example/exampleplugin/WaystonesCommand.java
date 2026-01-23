package com.example.exampleplugin;

import com.example.exampleplugin.waystone.Waystone;
import com.example.exampleplugin.waystone.WaystoneListPage;
import com.example.exampleplugin.waystone.WaystoneRegistry;
import com.example.exampleplugin.waystone.WaystoneSettingsPage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;

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
        addSubCommand((AbstractCommand) new EditCommand());
    }

    @Override
    protected boolean canGeneratePermission() {
        // Don't auto-generate permission for parent command
        // Subcommands define their own permissions
        return false;
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        ctx.sendMessage(Message.raw(pluginName + " v" + pluginVersion));
    }

    /**
     * Subcommand: /waystones list
     * Opens the waystone menu for the player.
     * Requires permission: hytale.command.waystones.allowListMenu
     */
    private static class ListCommand extends AbstractPlayerCommand {
        
        public ListCommand() {
            super("list", "Opens the waystone menu.");
            requirePermission(HytalePermissions.fromCommand("waystones.allowListMenu"));
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

    /**
     * Strips surrounding quotes from a string if present.
     */
    private static String stripQuotes(String s) {
        if (s == null) return null;
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    /**
     * Subcommand: /waystones edit <name> <property> <value>
     * Edits a waystone property by name.
     * Requires permission: hytale.command.waystones.allowEditAll
     */
    private static class EditCommand extends CommandBase {

        @Nonnull
        private final RequiredArg<String> waystoneNameArg = withRequiredArg("name", "The name of the waystone to edit", (ArgumentType<String>) ArgTypes.STRING);

        @Nonnull
        private final RequiredArg<String> propertyArg = withRequiredArg("property", "The property to edit", (ArgumentType<String>) ArgTypes.STRING);

        @Nonnull
        private final RequiredArg<String> valueArg = withRequiredArg("value", "The new value", (ArgumentType<String>) ArgTypes.STRING);

        public EditCommand() {
            super("edit", "Edits a waystone property.");
            requirePermission(HytalePermissions.fromCommand("waystones.allowEditAll"));
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            String waystoneName = stripQuotes(waystoneNameArg.get(context));
            String property = propertyArg.get(context).toLowerCase();
            String value = stripQuotes(valueArg.get(context));

            // Find waystone by name (exact match only)
            Waystone waystone = null;
            for (Waystone w : WaystoneRegistry.get().getAll()) {
                if (w.getName().equalsIgnoreCase(waystoneName)) {
                    waystone = w;
                    break;
                }
            }

            if (waystone == null) {
                context.sendMessage(Message.raw("Waystone '" + waystoneName + "' not found."));
                context.sendMessage(Message.raw("Note: Spaces in names may not work. Try using a name without spaces."));
                return;
            }

            // Check for array/object properties
            if (property.equals("editors") || property.equals("viewers")) {
                context.sendMessage(Message.raw("Editing arrays and objects is not supported by the /edit command."));
                return;
            }

            try {
                switch (property) {
                    case "name" -> {
                        waystone.setName(value);
                        WaystoneRegistry.get().save();
                        context.sendMessage(Message.raw("Updated name to: " + value));
                    }
                    case "ispublic", "public" -> {
                        boolean isPublic = Boolean.parseBoolean(value);
                        waystone.setPublic(isPublic);
                        WaystoneRegistry.get().save();
                        context.sendMessage(Message.raw("Updated isPublic to: " + isPublic));
                    }
                    case "priority" -> {
                        int priority = Integer.parseInt(value);
                        waystone.setPriority(priority);
                        WaystoneRegistry.get().save();
                        context.sendMessage(Message.raw("Updated priority to: " + priority));
                    }
                    case "textcolor" -> {
                        waystone.setTextColor(value);
                        WaystoneRegistry.get().save();
                        context.sendMessage(Message.raw("Updated textColor to: " + value));
                    }
                    case "teleportdirection", "direction" -> {
                        waystone.setTeleportDirection(value);
                        WaystoneRegistry.get().save();
                        context.sendMessage(Message.raw("Updated teleportDirection to: " + value));
                    }
                    case "playerorientation", "orientation" -> {
                        waystone.setPlayerOrientation(value);
                        WaystoneRegistry.get().save();
                        context.sendMessage(Message.raw("Updated playerOrientation to: " + value));
                    }
                    case "serverowned" -> {
                        boolean serverOwned = Boolean.parseBoolean(value);
                        waystone.setServerOwned(serverOwned);
                        WaystoneRegistry.get().save();
                        context.sendMessage(Message.raw("Updated serverOwned to: " + serverOwned));
                    }
                    case "ownername" -> {
                        waystone.setOwnerName(value);
                        WaystoneRegistry.get().save();
                        context.sendMessage(Message.raw("Warning: Changing owner name may cause display issues if the name doesn't match a real player."));
                        context.sendMessage(Message.raw("Updated ownerName to: " + value));
                    }
                    case "owneruuid" -> {
                        waystone.setOwnerUuid(value);
                        WaystoneRegistry.get().save();
                        context.sendMessage(Message.raw("Warning: Changing owner UUID may break ownership permissions if the UUID doesn't exist."));
                        context.sendMessage(Message.raw("Updated ownerUuid to: " + value));
                    }
                    case "world", "worldname" -> {
                        context.sendMessage(Message.raw("Editing world is not supported. Destroy and recreate the waystone."));
                    }
                    case "x", "y", "z", "yaw" -> {
                        context.sendMessage(Message.raw("Editing position is not supported. Destroy and recreate the waystone."));
                    }
                    case "id", "createdat" -> {
                        context.sendMessage(Message.raw("Editing " + property + " is not supported."));
                    }
                    default -> {
                        context.sendMessage(Message.raw("Unknown property: " + property));
                        context.sendMessage(Message.raw("Available properties: name, isPublic, priority, textColor, teleportDirection, playerOrientation, serverOwned, ownerName, ownerUuid"));
                    }
                }
            } catch (NumberFormatException e) {
                context.sendMessage(Message.raw("Invalid value for " + property + ": " + value));
            }
        }
    }
}
