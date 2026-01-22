package com.example.exampleplugin.waystone;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Settings page for waystone configuration.
 * Allows renaming the waystone directly via a text field.
 */
public class WaystoneSettingsPage extends InteractiveCustomUIPage<WaystoneSettingsPage.SettingsEventData> {

    public static class SettingsEventData {
        public static final BuilderCodec<SettingsEventData> CODEC;

        @SuppressWarnings({"unchecked", "rawtypes"})
        private static BuilderCodec<SettingsEventData> createCodec() {
            return (BuilderCodec<SettingsEventData>) ((BuilderCodec.Builder) ((BuilderCodec.Builder) ((BuilderCodec.Builder) ((BuilderCodec.Builder) ((BuilderCodec.Builder) BuilderCodec.builder(SettingsEventData.class, SettingsEventData::new)
                    .append(new KeyedCodec("Action", (Codec) Codec.STRING),
                            (e, s) -> ((SettingsEventData)e).action = (String)s, e -> ((SettingsEventData)e).action)
                    .add())
                    .append(new KeyedCodec("@NameInput", (Codec) Codec.STRING),
                            (e, s) -> ((SettingsEventData)e).name = (String)s, e -> ((SettingsEventData)e).name)
                    .add())
                    .append(new KeyedCodec("@PriorityInput", (Codec) Codec.STRING),
                            (e, s) -> ((SettingsEventData)e).priority = (String)s, e -> ((SettingsEventData)e).priority)
                    .add())
                    .append(new KeyedCodec("@VisibilityInput", (Codec) Codec.STRING),
                            (e, s) -> ((SettingsEventData)e).visibility = (String)s, e -> ((SettingsEventData)e).visibility)
                    .add())
                    .append(new KeyedCodec("@DirectionInput", (Codec) Codec.STRING),
                            (e, s) -> ((SettingsEventData)e).direction = (String)s, e -> ((SettingsEventData)e).direction)
                    .add())
                    .build();
        }

        static {
            CODEC = createCodec();
        }

        private String action;
        private String name;
        private String priority;
        private String visibility;
        private String direction;

        public String getAction() {
            return action;
        }

        public String getName() {
            return name;
        }

        public String getPriority() {
            return priority;
        }

        public String getVisibility() {
            return visibility;
        }

        public String getDirection() {
            return direction;
        }
    }

    private final PlayerRef playerRef;
    private final String playerUuid;
    private final String waystoneId;
    private final Runnable onBack;
    private final boolean isOp;

    // Track the pending values from text fields
    private String pendingName;
    private int pendingPriority;
    private String pendingDirection;
    // Error message to display
    private String errorMessage = null;

    public WaystoneSettingsPage(@Nonnull PlayerRef playerRef,
                                @Nonnull String playerUuid,
                                @Nonnull String waystoneId,
                                @Nonnull Runnable onBack) {
        super(playerRef, CustomPageLifetime.CanDismiss, SettingsEventData.CODEC);
        this.playerRef = playerRef;
        this.playerUuid = playerUuid;
        this.waystoneId = waystoneId;
        this.onBack = onBack;

        // Check if player is an operator
        this.isOp = PermissionsModule.get().getGroupsForUser(UUID.fromString(playerUuid)).contains("OP");

        // Initialize pending values from current waystone
        Waystone waystone = WaystoneRegistry.get().get(waystoneId);
        this.pendingName = waystone != null ? waystone.getName() : "";
        this.pendingPriority = waystone != null ? waystone.getPriority() : 0;
        this.pendingDirection = waystone != null ? waystone.getTeleportDirection() : "north";
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder,
                      @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/WaystoneSettingsPage.ui");

        // Set current waystone values in the text fields
        Waystone waystone = WaystoneRegistry.get().get(waystoneId);
        if (waystone != null) {
            commandBuilder.set("#NameInput.Value", waystone.getName());

            // Set up visibility dropdown
            commandBuilder.set("#Visibility #Input.Entries", new DropdownEntryInfo[] {
                    new DropdownEntryInfo(LocalizableString.fromString("Public"), "public"),
                    new DropdownEntryInfo(LocalizableString.fromString("Private"), "private")
            });
            commandBuilder.set("#Visibility #Input.Value", waystone.isPublic() ? "public" : "private");

            // Set up teleport direction dropdown
            commandBuilder.set("#TeleportDirection #DirectionInput.Entries", new DropdownEntryInfo[] {
                    new DropdownEntryInfo(LocalizableString.fromString("North"), "north"),
                    new DropdownEntryInfo(LocalizableString.fromString("South"), "south"),
                    new DropdownEntryInfo(LocalizableString.fromString("East"), "east"),
                    new DropdownEntryInfo(LocalizableString.fromString("West"), "west")
            });
            commandBuilder.set("#TeleportDirection #DirectionInput.Value", waystone.getTeleportDirection());
        }

        // Show error message if there is one
        if (errorMessage != null) {
            commandBuilder.set("#Error.Text", errorMessage);
            commandBuilder.set("#Error.Visible", true);
        }

        // Bind name text field value changes
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#NameInput",
                EventData.of("@NameInput", "#NameInput.Value"),
                false
        );

        // Bind visibility dropdown value changes
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#Visibility #Input",
                EventData.of("@VisibilityInput", "#Visibility #Input.Value"),
                false
        );

        // Bind direction dropdown value changes
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#TeleportDirection #DirectionInput",
                EventData.of("@DirectionInput", "#TeleportDirection #DirectionInput.Value"),
                false
        );

        // Priority section - only visible to ops
        if (isOp) {
            commandBuilder.set("#PrioritySection.Visible", true);
            if (waystone != null) {
                commandBuilder.set("#PriorityInput.Value", String.valueOf(waystone.getPriority()));
            }

            // Bind priority text field value changes
            eventBuilder.addEventBinding(
                    CustomUIEventBindingType.ValueChanged,
                    "#PriorityInput",
                    EventData.of("@PriorityInput", "#PriorityInput.Value"),
                    false
            );
        }

        // Bind save button
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#SaveButton",
                EventData.of("Action", "save")
        );

        // Bind exit button
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#CloseButton",
                EventData.of("Action", "exit")
        );
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull SettingsEventData event) {
        // Handle name text field value changes
        if (event.getName() != null) {
            pendingName = event.getName();
            sendUpdate(null, false);
            return;
        }

        // Handle priority text field value changes (ops only)
        if (event.getPriority() != null && isOp) {
            try {
                pendingPriority = Integer.parseInt(event.getPriority().trim());
            } catch (NumberFormatException e) {
                // Keep existing value if invalid
            }
            sendUpdate(null, false);
            return;
        }

        // Handle visibility dropdown value changes
        if (event.getVisibility() != null) {
            boolean isPublic = "public".equals(event.getVisibility());
            Waystone waystone = WaystoneRegistry.get().get(waystoneId);
            if (waystone != null && waystone.isPublic() != isPublic) {
                WaystoneRegistry.get().toggleVisibility(waystoneId);
                rebuild();
            }
            return;
        }

        // Handle direction dropdown value changes
        if (event.getDirection() != null) {
            pendingDirection = event.getDirection();
            WaystoneRegistry.get().updateTeleportDirection(waystoneId, pendingDirection);
            sendUpdate(null, false);
            return;
        }

        String action = event.getAction();
        if (action == null) {
            sendUpdate(null, false);
            return;
        }

        switch (action) {
            case "save" -> {
                String nameToSave = (pendingName != null && !pendingName.trim().isEmpty())
                        ? pendingName.trim()
                        : "Waystone";

                // Check name length (max 100 characters)
                if (nameToSave.length() > 100) {
                    errorMessage = "Name must be 100 characters or less";
                    rebuild();
                    return;
                }

                // Check if name is already taken (exclude current waystone)
                if (WaystoneRegistry.get().isNameTaken(nameToSave, waystoneId)) {
                    errorMessage = "A waystone with this name already exists";
                    rebuild();
                    return;
                }

                // Update the waystone name
                WaystoneRegistry.get().updateName(waystoneId, nameToSave);

                // Update priority only if op
                if (isOp) {
                    WaystoneRegistry.get().updatePriority(waystoneId, pendingPriority);
                }
                errorMessage = null;

                // Close the settings page
                close();
            }
            case "back" -> {
                close();
                onBack.run();
            }
            case "exit" -> {
                close();
            }
            default -> sendUpdate(null, false);
        }
    }
}
