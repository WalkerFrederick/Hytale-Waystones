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
            return (BuilderCodec<SettingsEventData>) ((BuilderCodec.Builder) ((BuilderCodec.Builder) ((BuilderCodec.Builder) ((BuilderCodec.Builder) ((BuilderCodec.Builder) ((BuilderCodec.Builder) ((BuilderCodec.Builder) ((BuilderCodec.Builder) ((BuilderCodec.Builder) BuilderCodec.builder(SettingsEventData.class, SettingsEventData::new)
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
                    .append(new KeyedCodec("@OrientationInput", (Codec) Codec.STRING),
                            (e, s) -> ((SettingsEventData)e).orientation = (String)s, e -> ((SettingsEventData)e).orientation)
                    .add())
                    .append(new KeyedCodec("@ServerOwnedInput", (Codec) Codec.STRING),
                            (e, s) -> ((SettingsEventData)e).serverOwned = (String)s, e -> ((SettingsEventData)e).serverOwned)
                    .add())
                    .append(new KeyedCodec("@DefaultDiscoveredInput", (Codec) Codec.STRING),
                            (e, s) -> ((SettingsEventData)e).defaultDiscovered = (String)s, e -> ((SettingsEventData)e).defaultDiscovered)
                    .add())
                    .append(new KeyedCodec("@ColorInput", (Codec) Codec.STRING),
                            (e, s) -> ((SettingsEventData)e).color = (String)s, e -> ((SettingsEventData)e).color)
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
        private String orientation;
        private String serverOwned;
        private String defaultDiscovered;
        private String color;

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

        public String getOrientation() {
            return orientation;
        }

        public String getServerOwned() {
            return serverOwned;
        }

        public String getDefaultDiscovered() {
            return defaultDiscovered;
        }

        public String getColor() {
            return color;
        }
    }

    private final String waystoneId;
    private final Runnable onBack;
    /** True if user has full edit access (allowEditAll permission or is OP). Enables admin-only settings. */
    private final boolean hasFullEditPerm;
    private final boolean canMakePublic;

    // Track the pending values from text fields
    private String pendingName;
    private int pendingPriority;
    private String pendingDirection;
    private String pendingOrientation;
    private boolean pendingServerOwned;
    private boolean pendingDefaultDiscovered;
    private String pendingColor;
    // Error message to display
    private String errorMessage = null;

    public WaystoneSettingsPage(@Nonnull PlayerRef playerRef,
                                @Nonnull String playerUuid,
                                @Nonnull String waystoneId,
                                @Nonnull Runnable onBack) {
        super(playerRef, CustomPageLifetime.CanDismiss, SettingsEventData.CODEC);
        this.waystoneId = waystoneId;
        this.onBack = onBack;

        UUID uuid = UUID.fromString(playerUuid);
        
        // Check if player has full edit permission (or is OP) - enables admin-only settings
        this.hasFullEditPerm = PermissionUtils.hasPermissionOrOp(uuid, WaystonePermissions.ALLOW_EDIT_ALL);
        
        // Check if player can make waystones public (not blocked by permission, or is OP)
        boolean isOp = PermissionUtils.isOp(uuid);
        this.canMakePublic = isOp || !PermissionUtils.hasPermission(uuid, WaystonePermissions.BLOCK_PUBLIC_WAYSTONE_CREATION);

        // Initialize pending values from current waystone
        Waystone waystone = WaystoneRegistry.get().get(waystoneId);
        this.pendingName = waystone != null ? waystone.getName() : "";
        this.pendingPriority = waystone != null ? waystone.getPriority() : 0;
        this.pendingDirection = waystone != null ? waystone.getTeleportDirection() : "north";
        this.pendingOrientation = waystone != null ? waystone.getPlayerOrientation() : "away";
        this.pendingServerOwned = waystone != null && waystone.isServerOwned();
        this.pendingDefaultDiscovered = waystone != null && waystone.isDefaultDiscovered();
        this.pendingColor = waystone != null ? waystone.getColor() : "default";
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder,
                      @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/WaystoneSettingsPage.ui");

        // Set up all dropdown entries first (before setting values)
        commandBuilder.set("#Visibility #Input.Entries", new DropdownEntryInfo[] {
                new DropdownEntryInfo(LocalizableString.fromString("Public"), "public"),
                new DropdownEntryInfo(LocalizableString.fromString("Private"), "private")
        });
        commandBuilder.set("#TeleportDirection #DirectionInput.Entries", new DropdownEntryInfo[] {
                new DropdownEntryInfo(LocalizableString.fromString("North"), "north"),
                new DropdownEntryInfo(LocalizableString.fromString("South"), "south"),
                new DropdownEntryInfo(LocalizableString.fromString("East"), "east"),
                new DropdownEntryInfo(LocalizableString.fromString("West"), "west")
        });
        commandBuilder.set("#PlayerOrientation #OrientationInput.Entries", new DropdownEntryInfo[] {
                new DropdownEntryInfo(LocalizableString.fromString("Away from Statue"), "away"),
                new DropdownEntryInfo(LocalizableString.fromString("Towards Statue"), "towards")
        });
        commandBuilder.set("#WaystoneColor #ColorInput.Entries", new DropdownEntryInfo[] {
                new DropdownEntryInfo(LocalizableString.fromString("Default (Blue)"), "default"),
                new DropdownEntryInfo(LocalizableString.fromString("Red"), "red"),
                new DropdownEntryInfo(LocalizableString.fromString("Green"), "green")
        });

        // Set current waystone values in the text fields
        Waystone waystone = WaystoneRegistry.get().get(waystoneId);
        if (waystone != null) {
            commandBuilder.set("#NameInput.Value", waystone.getName());
            commandBuilder.set("#Visibility #Input.Value", waystone.isPublic() ? "public" : "private");
            commandBuilder.set("#TeleportDirection #DirectionInput.Value", waystone.getTeleportDirection());
            commandBuilder.set("#PlayerOrientation #OrientationInput.Value", waystone.getPlayerOrientation());
            commandBuilder.set("#WaystoneColor #ColorInput.Value", waystone.getColor());
        }
        
        // Hide visibility section if user cannot make waystones public
        if (!canMakePublic) {
            commandBuilder.set("#Visibility.Visible", false);
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

        // Bind player orientation dropdown value changes
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#PlayerOrientation #OrientationInput",
                EventData.of("@OrientationInput", "#PlayerOrientation #OrientationInput.Value"),
                false
        );

        // Bind color dropdown value changes
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#WaystoneColor #ColorInput",
                EventData.of("@ColorInput", "#WaystoneColor #ColorInput.Value"),
                false
        );

        // Priority section - only visible to ops
        if (hasFullEditPerm) {
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

            // Server Owned section - also only visible to ops
            commandBuilder.set("#ServerOwnedSection.Visible", true);
            commandBuilder.set("#ServerOwnedSection #ServerOwnedInput.Entries", new DropdownEntryInfo[] {
                    new DropdownEntryInfo(LocalizableString.fromString("No"), "no"),
                    new DropdownEntryInfo(LocalizableString.fromString("Yes"), "yes")
            });
            if (waystone != null) {
                commandBuilder.set("#ServerOwnedSection #ServerOwnedInput.Value", waystone.isServerOwned() ? "yes" : "no");
            }

            // Bind server owned dropdown value changes
            eventBuilder.addEventBinding(
                    CustomUIEventBindingType.ValueChanged,
                    "#ServerOwnedSection #ServerOwnedInput",
                    EventData.of("@ServerOwnedInput", "#ServerOwnedSection #ServerOwnedInput.Value"),
                    false
            );

            // Default Discovered section - only visible if requireDiscover is enabled
            if (WaystoneRegistry.isRequireDiscoverEnabled()) {
                commandBuilder.set("#DefaultDiscoveredSection.Visible", true);
                commandBuilder.set("#DefaultDiscoveredSection #DefaultDiscoveredInput.Entries", new DropdownEntryInfo[] {
                        new DropdownEntryInfo(LocalizableString.fromString("No"), "no"),
                        new DropdownEntryInfo(LocalizableString.fromString("Yes"), "yes")
                });
                if (waystone != null) {
                    commandBuilder.set("#DefaultDiscoveredSection #DefaultDiscoveredInput.Value", waystone.isDefaultDiscovered() ? "yes" : "no");
                }

                // Bind default discovered dropdown value changes
                eventBuilder.addEventBinding(
                        CustomUIEventBindingType.ValueChanged,
                        "#DefaultDiscoveredSection #DefaultDiscoveredInput",
                        EventData.of("@DefaultDiscoveredInput", "#DefaultDiscoveredSection #DefaultDiscoveredInput.Value"),
                        false
                );
            }

            // Reset section - also only visible to ops
            commandBuilder.set("#ResetSection.Visible", true);

            // Bind reset button
            eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#ResetButton",
                    EventData.of("Action", "reset")
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
        if (event.getPriority() != null && hasFullEditPerm) {
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
            // Block changing to public if user doesn't have permission
            if (isPublic && !canMakePublic) {
                sendUpdate(null, false);
                return;
            }
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

        // Handle orientation dropdown value changes
        if (event.getOrientation() != null) {
            pendingOrientation = event.getOrientation();
            WaystoneRegistry.get().updatePlayerOrientation(waystoneId, pendingOrientation);
            sendUpdate(null, false);
            return;
        }

        // Handle server owned dropdown value changes
        if (event.getServerOwned() != null) {
            pendingServerOwned = "yes".equals(event.getServerOwned());
            WaystoneRegistry.get().updateServerOwned(waystoneId, pendingServerOwned);
            sendUpdate(null, false);
            return;
        }

        // Handle default discovered dropdown value changes
        if (event.getDefaultDiscovered() != null) {
            pendingDefaultDiscovered = "yes".equals(event.getDefaultDiscovered());
            WaystoneRegistry.get().updateDefaultDiscovered(waystoneId, pendingDefaultDiscovered);
            sendUpdate(null, false);
            return;
        }

        // Handle color dropdown value changes
        if (event.getColor() != null) {
            pendingColor = event.getColor();
            WaystoneRegistry.get().updateColor(waystoneId, pendingColor);
            // Swap the block in-world
            WaystoneColorSwapper.swapBlock(waystoneId);
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
                if (hasFullEditPerm) {
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
            case "reset" -> {
                // Only ops can reset waystones
                if (hasFullEditPerm) {
                    WaystoneRegistry.get().unregister(waystoneId);
                    close();
                }
            }
            default -> sendUpdate(null, false);
        }
    }
}
