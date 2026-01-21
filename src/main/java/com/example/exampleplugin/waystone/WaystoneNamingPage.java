package com.example.exampleplugin.waystone;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * UI Page for naming or renaming a waystone.
 * Shows a text input field for the player to enter a name.
 * Extends InteractiveCustomUIPage for proper form handling.
 */
public class WaystoneNamingPage extends InteractiveCustomUIPage<WaystoneNamingPage.NamingEventData> {

    /**
     * Event data received from the UI when user submits the name.
     */
    public static class NamingEventData {
        public static final BuilderCodec<NamingEventData> CODEC;

        @SuppressWarnings({"unchecked", "rawtypes"})
        private static BuilderCodec<NamingEventData> createCodec() {
            // Key should be @ElementId where ElementId is the TextField's ID without #
            return (BuilderCodec<NamingEventData>) ((BuilderCodec.Builder) ((BuilderCodec.Builder) BuilderCodec.builder(NamingEventData.class, NamingEventData::new)
                    .append(new KeyedCodec("Action", (Codec) Codec.STRING),
                            (e, s) -> ((NamingEventData)e).action = (String)s, e -> ((NamingEventData)e).action)
                    .add())
                    .append(new KeyedCodec("@NameInput", (Codec) Codec.STRING),
                            (e, s) -> ((NamingEventData)e).name = (String)s, e -> ((NamingEventData)e).name)
                    .add())
                    .build();
        }

        static {
            CODEC = createCodec();
        }

        private String name;
        private String action;

        public String getName() {
            return name;
        }

        public String getAction() {
            return action;
        }
    }

    @Nullable
    private final String currentName;
    private final Consumer<String> onNameSubmit;
    private final Runnable onCancel;

    /**
     * Creates a new WaystoneNamingPage.
     *
     * @param playerRef The player viewing the page
     * @param waystoneId The ID of the waystone being named
     * @param currentName The current name (null for new waystones)
     * @param isNewWaystone True if this is a newly placed waystone
     * @param onNameSubmit Callback when player submits a name
     * @param onCancel Callback when player cancels
     */
    public WaystoneNamingPage(@Nonnull PlayerRef playerRef,
                              @Nonnull String waystoneId,
                              @Nullable String currentName,
                              boolean isNewWaystone,
                              @Nonnull Consumer<String> onNameSubmit,
                              @Nonnull Runnable onCancel) {
        super(playerRef, CustomPageLifetime.CanDismiss, NamingEventData.CODEC);
        this.currentName = currentName;
        this.onNameSubmit = onNameSubmit;
        this.onCancel = onCancel;
    }

    // Store the latest text field value
    private String pendingName = "";

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder,
                      @Nonnull Store<EntityStore> store) {
        // Load our custom UI template
        commandBuilder.append("Pages/WaystoneNamingPage.ui");

        // Set placeholder text if there's a current name
        if (currentName != null && !currentName.isEmpty()) {
            commandBuilder.set("#NameInput.Text", currentName);
            pendingName = currentName;
        }

        // Bind TextField with ValueChanged to capture text as user types
        // Must specify EventData to map the TextField value to a key
        // Format: EventData.of("@KeyName", "#ElementId.Value")
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#NameInput",
                EventData.of("@NameInput", "#NameInput.Value"),
                false  // Don't lock interface during value change
        );

        // Bind submit button with Activating
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#SubmitButton",
                EventData.of("Action", "submit")
        );

        // Bind cancel button
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#CancelButton",
                EventData.of("Action", "cancel")
        );
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull NamingEventData event) {
        // If we received a name value (from ValueChanged on TextField), store it
        if (event.getName() != null) {
            pendingName = event.getName();
            // Must call sendUpdate to acknowledge the event
            sendUpdate(null, false);
            return;
        }

        String action = event.getAction();
        if (action == null) {
            // No action means this was a ValueChange event, already handled above
            sendUpdate(null, false);
            return;
        }

        switch (action) {
            case "submit" -> {
                // Use the stored pending name from ValueChange events
                if (pendingName != null && !pendingName.trim().isEmpty()) {
                    onNameSubmit.accept(pendingName.trim());
                } else {
                    // Use default name if empty
                    onNameSubmit.accept("Waystone");
                }
                close();
            }
            case "cancel" -> {
                onCancel.run();
                close();
            }
        }
    }

    @Override
    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        // If dismissed without submitting, treat as cancel
    }
}
