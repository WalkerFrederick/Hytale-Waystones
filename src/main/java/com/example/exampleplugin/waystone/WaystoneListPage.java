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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
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
            return (BuilderCodec<WaystoneEventData>) ((BuilderCodec.Builder) ((BuilderCodec.Builder) ((BuilderCodec.Builder) BuilderCodec.builder(WaystoneEventData.class, WaystoneEventData::new)
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
        private String action;
        private String searchQuery;

        public int getIndex() {
            return index;
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
    private String searchQuery = "";
    // Cache the current list of waystones for index-based lookup
    private List<Waystone> currentWaystones = List.of();

    /**
     * Creates a new WaystoneListPage.
     *
     * @param playerRef The player viewing the page
     * @param playerUuid The player's UUID for visibility filtering
     * @param currentWaystoneId The waystone the player is currently at (can be null)
     * @param onTeleport Callback when player selects a waystone to teleport to
     * @param onRename Callback when player wants to rename the current waystone
     */
    public WaystoneListPage(@Nonnull PlayerRef playerRef,
                            @Nonnull String playerUuid,
                            @Nullable String currentWaystoneId,
                            @Nonnull Consumer<Waystone> onTeleport,
                            @Nonnull Runnable onRename) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction);
        this.playerUuid = playerUuid;
        this.currentWaystoneId = currentWaystoneId;
        this.onTeleport = onTeleport;
        this.onRename = onRename;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder,
                      @Nonnull Store<EntityStore> store) {
        // Load the UI template
        commandBuilder.append("Pages/WaystoneListPage.ui");

        // Set the title to the current waystone's name
        if (currentWaystoneId != null) {
            Waystone currentWaystone = WaystoneRegistry.get().get(currentWaystoneId);
            if (currentWaystone != null) {
                commandBuilder.set("#TitleText.Text", currentWaystone.getName());
            }
        }

        // Get waystones visible to this player
        List<Waystone> waystones = WaystoneRegistry.get().getVisibleTo(playerUuid);

        // Filter by search query if present
        if (searchQuery != null && !searchQuery.isEmpty()) {
            String query = searchQuery.toLowerCase();
            waystones = waystones.stream()
                    .filter(w -> w.getName().toLowerCase().contains(query) ||
                                 w.getOwnerName().toLowerCase().contains(query))
                    .toList();
        }

        // Store the list for index-based lookup in event handler
        this.currentWaystones = waystones;

        // Clear the list before adding entries
        commandBuilder.clear("#WaystoneList");

        // Add waystone entries
        for (int i = 0; i < waystones.size(); i++) {
            Waystone waystone = waystones.get(i);
            String selector = "#WaystoneList[" + i + "]";

            // Append an entry button for each waystone
            commandBuilder.append("#WaystoneList", "Pages/WaystoneEntryButton.ui");
            commandBuilder.set(selector + " #Name.Text", waystone.getName());
            commandBuilder.set(selector + " #Owner.Text", waystone.getOwnerName());
            commandBuilder.set(selector + " #World.Text", waystone.getWorldName());

            // Bind click event for this button - sends Index back to server
            // The appended element IS the button, so use the list index selector directly
            eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    selector,
                    EventData.of("Index", String.valueOf(i))
            );
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
                case "close" -> close();
            }
        } catch (Exception e) {
            // Log error but don't crash
            close();
        }
    }
}
