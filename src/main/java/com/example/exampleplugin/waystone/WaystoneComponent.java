package com.example.exampleplugin.waystone;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * ECS Component that links a placed waystone block to its Waystone data in the registry.
 * Attached to the block entity when a waystone is placed.
 */
public class WaystoneComponent implements Component<ChunkStore> {

    public static final BuilderCodec<WaystoneComponent> CODEC;

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BuilderCodec<WaystoneComponent> createCodec() {
        return (BuilderCodec<WaystoneComponent>) ((BuilderCodec.Builder) BuilderCodec.builder(WaystoneComponent.class, WaystoneComponent::new)
                .addField(new KeyedCodec("WaystoneId", (Codec) Codec.STRING),
                        (c, s) -> ((WaystoneComponent)c).waystoneId = (String)s,
                        c -> ((WaystoneComponent)c).waystoneId))
                .build();
    }

    static {
        CODEC = createCodec();
    }

    private static ComponentType<ChunkStore, WaystoneComponent> componentType;

    /**
     * Sets the component type (called during plugin initialization).
     */
    public static void setComponentType(ComponentType<ChunkStore, WaystoneComponent> type) {
        componentType = type;
    }

    /**
     * Gets the component type.
     */
    @Nonnull
    public static ComponentType<ChunkStore, WaystoneComponent> getComponentType() {
        if (componentType == null) {
            throw new IllegalStateException("WaystoneComponent type not initialized! Did you forget to call setComponentType?");
        }
        return componentType;
    }

    private String waystoneId;

    /**
     * Default constructor for codec.
     */
    public WaystoneComponent() {
    }

    /**
     * Creates a component with the given waystone ID.
     */
    public WaystoneComponent(@Nonnull String waystoneId) {
        this.waystoneId = waystoneId;
    }

    /**
     * Gets the ID of the waystone this block is linked to.
     */
    @Nonnull
    public String getWaystoneId() {
        return waystoneId;
    }

    /**
     * Gets the Waystone data from the registry.
     */
    @Nullable
    public Waystone getWaystone() {
        return WaystoneRegistry.get().get(waystoneId);
    }

    @Nonnull
    @Override
    public Component<ChunkStore> clone() {
        return new WaystoneComponent(waystoneId);
    }

    @Override
    public String toString() {
        return "WaystoneComponent{waystoneId='" + waystoneId + "'}";
    }
}
