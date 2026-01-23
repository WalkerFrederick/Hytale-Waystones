package com.example.exampleplugin.waystone;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.util.RawJsonReader;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * UI Page that displays the list of waystones the player can teleport to.
 * Also provides options to rename the current waystone and toggle visibility.
 */
public class WaystoneListPage extends CustomUIPage {

    /**
     * Event data received from the UI when user interacts with the page.
     */
    public static class WaystoneEventData {
        public static final BuilderCodec<WaystoneEventData> CODEC;

        @SuppressWarnings({"unchecked", "rawtypes"})
        private static BuilderCodec<WaystoneEventData> createCodec() {
            return (BuilderCodec<WaystoneEventData>) ((BuilderCodec.Builder) ((BuilderCodec.Builder) ((BuilderCodec.Builder) ((BuilderCodec.Builder) BuilderCodec.builder(WaystoneEventData.class, WaystoneEventData::new)
                    .append(new KeyedCodec("Index", (Codec) Codec.STRING),
                            (e, s) -> {
                                ((WaystoneEventData)e).indexStr = (String)s;
                                try {
                                    ((WaystoneEventData)e).index = Integer.parseInt((String)s);
                                } catch (NumberFormatException ex) {
                                    ((WaystoneEventData)e).index = -1;
                                }
                            }, e -> ((WaystoneEventData)e).indexStr)
                    .add())
                    .append(new KeyedCodec("EditIndex", (Codec) Codec.STRING),
                            (e, s) -> {
                                ((WaystoneEventData)e).editIndexStr = (String)s;
                                try {
                                    ((WaystoneEventData)e).editIndex = Integer.parseInt((String)s);
                                } catch (NumberFormatException ex) {
                                    ((WaystoneEventData)e).editIndex = -1;
                                }
                            }, e -> ((WaystoneEventData)e).editIndexStr)
                    .add())
                    .append(new KeyedCodec("Action", (Codec) Codec.STRING),
                            (e, s) -> ((WaystoneEventData)e).action = (String)s, e -> ((WaystoneEventData)e).action)
                    .add())
                    .append(new KeyedCodec("@SearchQuery", (Codec) Codec.STRING),
                            (e, s) -> ((WaystoneEventData)e).searchQuery = (String)s, e -> ((WaystoneEventData)e).searchQuery)
                    .add())
                    .build();
        }

        static {
            CODEC = createCodec();
        }

        private String indexStr;
        private int index = -1;
        private String editIndexStr;
        private int editIndex = -1;
        private String action;
        private String searchQuery;

        public int getIndex() {
            return index;
        }

        public int getEditIndex() {
            return editIndex;
        }

        public String getAction() {
            return action;
        }

        public String getSearchQuery() {
            return searchQuery;
        }
    }

    private final String playerUuid;
    @Nullable
    private final String currentWaystoneId;
    private final Consumer<Waystone> onTeleport;
    private final Runnable onRename;
    private final Runnable onSettings;
    private final Consumer<String> onEditWaystone; // Callback for ops to edit any waystone by ID
    private final boolean isOp;
    private String searchQuery = "";
    // Cache the current list of waystones for index-based lookup
    private List<Waystone> currentWaystones = List.of();
    // Track which tab is currently selected: "public" or "private"
    private String currentTab = "public";

    /**
     * Creates a new WaystoneListPage.
     *
     * @param playerRef The player viewing the page
     * @param playerUuid The player's UUID for visibility filtering
     * @param currentWaystoneId The waystone the player is currently at (can be null)
     * @param onTeleport Callback when player selects a waystone to teleport to
     * @param onRename Callback when player wants to rename the current waystone
     * @param onSettings Callback when player wants to open settings
     * @param onEditWaystone Callback for ops to edit any waystone by ID (can be null)
     */
    public WaystoneListPage(@Nonnull PlayerRef playerRef,
                            @Nonnull String playerUuid,
                            @Nullable String currentWaystoneId,
                            @Nonnull Consumer<Waystone> onTeleport,
                            @Nonnull Runnable onRename,
                            @Nonnull Runnable onSettings,
                            @Nullable Consumer<String> onEditWaystone) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction);
        this.playerUuid = playerUuid;
        this.currentWaystoneId = currentWaystoneId;
        this.onTeleport = onTeleport;
        this.onRename = onRename;
        this.onSettings = onSettings;
        this.onEditWaystone = onEditWaystone;
        this.isOp = PermissionsModule.get().getGroupsForUser(UUID.fromString(playerUuid)).contains("OP");
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder,
                      @Nonnull Store<EntityStore> store) {
        // Load the UI template
        commandBuilder.append("Pages/WaystoneListPage.ui");

        // Set the title to the current waystone's name and show/hide edit button
        if (currentWaystoneId != null) {
            Waystone currentWaystone = WaystoneRegistry.get().get(currentWaystoneId);
            if (currentWaystone != null) {
                commandBuilder.set("#TitleText.Text", currentWaystone.getName());
            }
            // Show edit button and bind it
            commandBuilder.set("#EditButtonGroup.Visible", true);
            eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#SettingsButton",
                    EventData.of("Action", "settings")
            );
        } else {
            // Hide edit button group when opened via command (no current waystone)
            commandBuilder.set("#EditButtonGroup.Visible", false);
        }

        // Bind exit button
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#CloseButton",
                EventData.of("Action", "close")
        );

        // Bind tab buttons
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#TabPublic",
                EventData.of("Action", "tab_public")
        );
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#TabPrivate",
                EventData.of("Action", "tab_private")
        );

        // Set tab visibility based on current tab
        commandBuilder.set("#PublicContent.Visible", "public".equals(currentTab));
        commandBuilder.set("#PrivateContent.Visible", "private".equals(currentTab));

        // Get waystones visible to this player and split into public/private
        List<Waystone> allWaystones = WaystoneRegistry.get().getVisibleTo(playerUuid);

        // Filter by search query if present
        if (searchQuery != null && !searchQuery.isEmpty()) {
            String query = searchQuery.toLowerCase();
            allWaystones = allWaystones.stream()
                    .filter(w -> w.getName().toLowerCase().contains(query) ||
                                 w.getOwnerName().toLowerCase().contains(query))
                    .toList();
        }

        // Split into public and private lists
        List<Waystone> publicWaystones = allWaystones.stream()
                .filter(Waystone::isPublic)
                .toList();
        List<Waystone> privateWaystones = allWaystones.stream()
                .filter(w -> !w.isPublic())
                .toList();

        // Store combined list for index-based lookup (public first, then private)
        List<Waystone> combinedList = new java.util.ArrayList<>();
        combinedList.addAll(publicWaystones);
        combinedList.addAll(privateWaystones);
        this.currentWaystones = combinedList;

        // Clear the lists
        commandBuilder.clear("#PublicContent #PublicList");
        commandBuilder.clear("#PrivateContent #PrivateList");

        // Populate public waystones
        if (publicWaystones.isEmpty()) {
            commandBuilder.set("#PublicContent #NoPublic.Visible", true);
        } else {
            commandBuilder.set("#PublicContent #NoPublic.Visible", false);
            for (int i = 0; i < publicWaystones.size(); i++) {
                Waystone waystone = publicWaystones.get(i);
                String selector = "#PublicContent #PublicList[" + i + "]";
                int globalIndex = i; // Index in combined list

                commandBuilder.append("#PublicContent #PublicList", "Pages/WaystoneEntryButton.ui");
                commandBuilder.set(selector + " #Name.Text", waystone.getName());
                // Hide owner name if server owned
                commandBuilder.set(selector + " #Owner.Text", waystone.isServerOwned() ? "" : waystone.getOwnerName());
                String worldDisplay = waystone.getWorldName().equals("default") ? "" : waystone.getWorldName();
                commandBuilder.set(selector + " #World.Text", worldDisplay);

                // Show gear button for ops
                if (isOp && onEditWaystone != null) {
                    commandBuilder.set(selector + " #GearButton.Visible", true);
                    eventBuilder.addEventBinding(
                            CustomUIEventBindingType.Activating,
                            selector + " #GearButton",
                            EventData.of("EditIndex", String.valueOf(globalIndex))
                    );
                }

                eventBuilder.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        selector + " #Button",
                        EventData.of("Index", String.valueOf(globalIndex))
                );
            }
        }

        // Populate private waystones
        if (privateWaystones.isEmpty()) {
            commandBuilder.set("#PrivateContent #NoPrivate.Visible", true);
        } else {
            commandBuilder.set("#PrivateContent #NoPrivate.Visible", false);
            for (int i = 0; i < privateWaystones.size(); i++) {
                Waystone waystone = privateWaystones.get(i);
                String selector = "#PrivateContent #PrivateList[" + i + "]";
                int globalIndex = publicWaystones.size() + i; // Index in combined list

                commandBuilder.append("#PrivateContent #PrivateList", "Pages/WaystoneEntryButton.ui");
                commandBuilder.set(selector + " #Name.Text", waystone.getName());
                // Hide owner name if server owned
                commandBuilder.set(selector + " #Owner.Text", waystone.isServerOwned() ? "" : waystone.getOwnerName());
                String worldDisplay = waystone.getWorldName().equals("default") ? "" : waystone.getWorldName();
                commandBuilder.set(selector + " #World.Text", worldDisplay);

                // Show gear button for ops
                if (isOp && onEditWaystone != null) {
                    commandBuilder.set(selector + " #GearButton.Visible", true);
                    eventBuilder.addEventBinding(
                            CustomUIEventBindingType.Activating,
                            selector + " #GearButton",
                            EventData.of("EditIndex", String.valueOf(globalIndex))
                    );
                }

                eventBuilder.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        selector + " #Button",
                        EventData.of("Index", String.valueOf(globalIndex))
                );
            }
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                String rawData) {
        try {
            ExtraInfo extraInfo = ExtraInfo.THREAD_LOCAL.get();
            WaystoneEventData event = WaystoneEventData.CODEC.decodeJson(new RawJsonReader(rawData.toCharArray()), extraInfo);

            // Handle search query update
            if (event.getSearchQuery() != null) {
                this.searchQuery = event.getSearchQuery();
                rebuild();
                return;
            }

            // Handle gear button click (edit waystone) for ops
            int editIndex = event.getEditIndex();
            if (editIndex >= 0 && editIndex < currentWaystones.size() && isOp && onEditWaystone != null) {
                Waystone waystone = currentWaystones.get(editIndex);
                if (waystone != null) {
                    // Don't close - let the new page replace this one (same as settings flow)
                    onEditWaystone.accept(waystone.getId());
                    return;
                }
            }

            String action = event.getAction();
            if (action == null) {
                action = "teleport"; // Default action
            }

            switch (action) {
                case "teleport" -> {
                    int index = event.getIndex();
                    if (index >= 0 && index < currentWaystones.size()) {
                        Waystone waystone = currentWaystones.get(index);
                        if (waystone != null && waystone.isVisibleTo(playerUuid)) {
                            // Close BEFORE teleport - important for cross-world teleports
                            // where the player ref becomes invalid after world change
                            close();
                            onTeleport.accept(waystone);
                        }
                    }
                }
                case "rename" -> {
                    // Close before callback to ensure clean state
                    close();
                    onRename.run();
                }
                case "toggleVisibility" -> {
                    if (currentWaystoneId != null) {
                        Waystone current = WaystoneRegistry.get().get(currentWaystoneId);
                        if (current != null && current.isOwnedBy(playerUuid)) {
                            WaystoneRegistry.get().toggleVisibility(currentWaystoneId);
                            rebuild();
                        }
                    }
                }
                case "settings" -> {
                    System.out.println("[Waystone List] Settings button clicked, opening settings page...");
                    // Don't close - let the new page replace this one
                    onSettings.run();
                    System.out.println("[Waystone List] onSettings callback completed");
                }
                case "close" -> close();
                case "tab_public" -> {
                    currentTab = "public";
                    rebuild();
                }
                case "tab_private" -> {
                    currentTab = "private";
                    rebuild();
                }
            }
        } catch (Exception e) {
            // Log error but don't crash
            close();
        }
    }
}
