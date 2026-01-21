package com.example.exampleplugin.waystone;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a Waystone teleportation point in the world.
 * Each placed waystone block creates one of these entries.
 */
public class Waystone {

    public static final Codec<Waystone> CODEC;
    public static final ArrayCodec<Waystone> ARRAY_CODEC;

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Codec<Waystone> createCodec() {
        return (Codec<Waystone>) ((BuilderCodec.Builder) ((BuilderCodec.Builder) ((BuilderCodec.Builder) ((BuilderCodec.Builder) ((BuilderCodec.Builder) ((BuilderCodec.Builder) ((BuilderCodec.Builder) ((BuilderCodec.Builder) ((BuilderCodec.Builder) ((BuilderCodec.Builder) BuilderCodec.builder(Waystone.class, Waystone::new)
                .addField(new KeyedCodec("Id", (Codec) Codec.STRING), (w, s) -> ((Waystone)w).id = (String)s, w -> ((Waystone)w).id))
                .addField(new KeyedCodec("Name", (Codec) Codec.STRING), (w, s) -> ((Waystone)w).name = (String)s, w -> ((Waystone)w).name))
                .addField(new KeyedCodec("World", (Codec) Codec.STRING), (w, s) -> ((Waystone)w).worldName = (String)s, w -> ((Waystone)w).worldName))
                .addField(new KeyedCodec("X", (Codec) Codec.DOUBLE), (w, v) -> ((Waystone)w).x = (Double)v, w -> ((Waystone)w).x))
                .addField(new KeyedCodec("Y", (Codec) Codec.DOUBLE), (w, v) -> ((Waystone)w).y = (Double)v, w -> ((Waystone)w).y))
                .addField(new KeyedCodec("Z", (Codec) Codec.DOUBLE), (w, v) -> ((Waystone)w).z = (Double)v, w -> ((Waystone)w).z))
                .addField(new KeyedCodec("Yaw", (Codec) Codec.FLOAT), (w, v) -> ((Waystone)w).yaw = (Float)v, w -> ((Waystone)w).yaw))
                .addField(new KeyedCodec("OwnerUuid", (Codec) Codec.STRING), (w, s) -> ((Waystone)w).ownerUuid = (String)s, w -> ((Waystone)w).ownerUuid))
                .addField(new KeyedCodec("OwnerName", (Codec) Codec.STRING), (w, s) -> ((Waystone)w).ownerName = (String)s, w -> ((Waystone)w).ownerName))
                .addField(new KeyedCodec("IsPublic", (Codec) Codec.BOOLEAN), (w, v) -> ((Waystone)w).isPublic = (Boolean)v, w -> ((Waystone)w).isPublic))
                .append(new KeyedCodec("CreatedAt", (Codec) Codec.INSTANT), (w, i) -> ((Waystone)w).createdAt = (Instant)i, w -> ((Waystone)w).createdAt)
                .add()
                .build();
    }

    static {
        CODEC = createCodec();
        ARRAY_CODEC = new ArrayCodec<>(CODEC, Waystone[]::new);
    }

    private String id;
    private String name;
    private String worldName;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private String ownerUuid;
    private String ownerName;
    private boolean isPublic;
    private Instant createdAt;

    /**
     * Default constructor for codec deserialization.
     */
    public Waystone() {
    }

    /**
     * Creates a new Waystone.
     */
    public Waystone(@Nonnull String id, @Nonnull String name, @Nonnull String worldName,
                    double x, double y, double z, float yaw,
                    @Nonnull String ownerUuid, @Nonnull String ownerName,
                    boolean isPublic, @Nonnull Instant createdAt) {
        this.id = id;
        this.name = name;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.isPublic = isPublic;
        this.createdAt = createdAt;
    }

    /**
     * Creates a new Waystone with a generated UUID.
     */
    public static Waystone create(@Nonnull String name, @Nonnull World world,
                                   @Nonnull Vector3d position, float yaw,
                                   @Nonnull UUID ownerUuid, @Nonnull String ownerName,
                                   boolean isPublic) {
        return new Waystone(
                UUID.randomUUID().toString(),
                name,
                world.getName(),
                position.x,
                position.y,
                position.z,
                yaw,
                ownerUuid.toString(),
                ownerName,
                isPublic,
                Instant.now()
        );
    }

    // Getters
    @Nonnull
    public String getId() {
        return id;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    public void setName(@Nonnull String name) {
        this.name = name;
    }

    @Nonnull
    public String getWorldName() {
        return worldName;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    @Nonnull
    public Vector3d getPosition() {
        return new Vector3d(x, y, z);
    }

    public float getYaw() {
        return yaw;
    }

    @Nonnull
    public String getOwnerUuid() {
        return ownerUuid;
    }

    @Nonnull
    public String getOwnerName() {
        return ownerName;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    @Nonnull
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Checks if this waystone is visible to the given player.
     */
    public boolean isVisibleTo(@Nonnull String playerUuid) {
        return isPublic || ownerUuid.equals(playerUuid);
    }

    /**
     * Checks if the given player is the owner of this waystone.
     */
    public boolean isOwnedBy(@Nonnull String playerUuid) {
        return ownerUuid.equals(playerUuid);
    }

    /**
     * Checks if the destination is safe for teleportation.
     * A destination is safe if there are 2 blocks of non-solid space above the waystone block.
     * 
     * @return true if safe to teleport, false if blocked
     */
    public boolean isSafeDestination() {
        World world = Universe.get().getWorld(worldName);
        if (world == null) {
            return false; // Can't check, assume unsafe
        }
        
        // Player spawns at y+1, so we need to check y+1 and y+2 for air
        int checkX = (int) Math.floor(x);
        int checkY1 = (int) y + 1; // Feet position
        int checkY2 = (int) y + 2; // Head position
        int checkZ = (int) Math.floor(z);
        
        boolean feetClear = isBlockPassable(world, checkX, checkY1, checkZ);
        boolean headClear = isBlockPassable(world, checkX, checkY2, checkZ);
        
        System.out.println("[Waystone] Safety check for '" + name + "': feet(" + checkX + "," + checkY1 + "," + checkZ + ")=" + 
                (feetClear ? "clear" : "blocked") + ", head(" + checkX + "," + checkY2 + "," + checkZ + ")=" + 
                (headClear ? "clear" : "blocked"));
        
        return feetClear && headClear;
    }
    
    /**
     * Checks if a block at the given position is passable (air or non-solid).
     */
    private boolean isBlockPassable(@Nonnull World world, int x, int y, int z) {
        long chunkIndex = ChunkUtil.indexChunk(x >> 4, z >> 4);
        WorldChunk chunk = world.getChunkIfInMemory(chunkIndex);
        
        if (chunk == null) {
            // Chunk not loaded - we can't check, assume it's safe
            // (the teleport system will load it)
            return true;
        }
        
        BlockType blockType = chunk.getBlockType(x, y, z);
        if (blockType == null) {
            return true; // No block = air = safe
        }
        
        // Check if block is air or passable
        String blockId = blockType.getId();
        // Air blocks typically have "Air" in their ID or are null
        // For safety, we'll check for common passable blocks
        if (blockId == null || blockId.isEmpty()) {
            return true;
        }
        
        String lowerBlockId = blockId.toLowerCase();
        // Consider these as passable
        if (lowerBlockId.contains("air") || 
            lowerBlockId.contains("water") ||
            lowerBlockId.contains("grass_plant") ||
            lowerBlockId.contains("flower") ||
            lowerBlockId.contains("mushroom") ||
            lowerBlockId.contains("torch") ||
            lowerBlockId.contains("sign")) {
            return true;
        }
        
        // Check if the block has collision - if it doesn't, it's passable
        // Unfortunately we may not have direct access to collision info,
        // so we'll assume solid blocks are not passable
        System.out.println("[Waystone] Block at (" + x + "," + y + "," + z + "): " + blockId);
        return false;
    }

    /**
     * Converts this waystone to a Teleport component for teleporting players.
     * The player will spawn south of the waystone (+Z), facing north toward it.
     */
    @Nullable
    public Teleport toTeleport() {
        World world = Universe.get().getWorld(worldName);
        if (world == null) {
            System.out.println("[Waystone] toTeleport: World '" + worldName + "' not found!");
            return null;
        }
        
        // Always spawn south of the block (+Z direction), facing north toward it
        // This is predictable and consistent regardless of block rotation
        double teleportX = x + 0.5;           // Center X
        double teleportY = y + 1.0;           // One block above waystone
        double teleportZ = z + 0.5 + 1.5;     // 1.5 blocks south (+Z)
        Vector3d position = new Vector3d(teleportX, teleportY, teleportZ);
        
        // Player faces north (toward the waystone) - yaw = 0 in Hytale means facing -Z (north)
        float playerYaw = 0f;
        Vector3f rotation = new Vector3f(0, playerYaw, 0);
        
        System.out.println("[Waystone] toTeleport: name='" + name + "' world='" + worldName + 
                "' block pos=(" + x + ", " + y + ", " + z + ")" +
                " teleport pos=(" + teleportX + ", " + teleportY + ", " + teleportZ + ")");
        
        return new Teleport(world, position, rotation);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Waystone waystone = (Waystone) o;
        return Objects.equals(id, waystone.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Waystone{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", worldName='" + worldName + '\'' +
                ", position=(" + x + ", " + y + ", " + z + ")" +
                ", owner='" + ownerName + '\'' +
                ", isPublic=" + isPublic +
                '}';
    }
}
