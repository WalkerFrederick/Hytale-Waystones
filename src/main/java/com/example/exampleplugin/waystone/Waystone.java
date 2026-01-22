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
import java.util.*;

/**
 * Represents a Waystone teleportation point in the world.
 * Each placed waystone block creates one of these entries.
 */
public class Waystone {

    public static final Codec<Waystone> CODEC;
    public static final ArrayCodec<Waystone> ARRAY_CODEC;

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Codec<Waystone> createCodec() {
        return (Codec<Waystone>) ((BuilderCodec.Builder) ((BuilderCodec.Builder) ((BuilderCodec.Builder) ((BuilderCodec.Builder) ((BuilderCodec.Builder) ((BuilderCodec.Builder) ((BuilderCodec.Builder) ((BuilderCodec.Builder) ((BuilderCodec.Builder) ((BuilderCodec.Builder) ((BuilderCodec.Builder) ((BuilderCodec.Builder) ((BuilderCodec.Builder) ((BuilderCodec.Builder) ((BuilderCodec.Builder) ((BuilderCodec.Builder) BuilderCodec.builder(Waystone.class, Waystone::new)
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
                .append(new KeyedCodec("Priority", (Codec) Codec.INTEGER, false, true), (w, v) -> ((Waystone)w).priority = v != null ? (Integer)v : 0, w -> ((Waystone)w).priority)
                .add())
                .append(new KeyedCodec("Editors", (Codec) Codec.STRING_ARRAY, false, true), (w, v) -> ((Waystone)w).editors = v != null ? (String[])v : new String[0], w -> ((Waystone)w).editors)
                .add())
                .append(new KeyedCodec("Viewers", (Codec) Codec.STRING_ARRAY, false, true), (w, v) -> ((Waystone)w).viewers = v != null ? (String[])v : new String[0], w -> ((Waystone)w).viewers)
                .add())
                .append(new KeyedCodec("CreatedAt", (Codec) Codec.INSTANT), (w, i) -> ((Waystone)w).createdAt = (Instant)i, w -> ((Waystone)w).createdAt)
                .add())
                .append(new KeyedCodec("TextColor", (Codec) Codec.STRING, false, true), (w, v) -> ((Waystone)w).textColor = v != null ? (String)v : "#ffffff", w -> ((Waystone)w).textColor)
                .add())
                .append(new KeyedCodec("TeleportDirection", (Codec) Codec.STRING, false, true), (w, v) -> ((Waystone)w).teleportDirection = v != null ? (String)v : "north", w -> ((Waystone)w).teleportDirection)
                .add())
                .append(new KeyedCodec("PlayerOrientation", (Codec) Codec.STRING, false, true), (w, v) -> ((Waystone)w).playerOrientation = v != null ? (String)v : "away", w -> ((Waystone)w).playerOrientation)
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
    private int priority = 0;
    private String[] editors = new String[0];
    private String[] viewers = new String[0];
    private Instant createdAt;
    private String textColor = "#ffffff";
    private String teleportDirection = "north"; // north, south, east, west
    private String playerOrientation = "away"; // away, towards

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

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Nonnull
    public Instant getCreatedAt() {
        return createdAt;
    }

    @Nonnull
    public String getTextColor() {
        return textColor != null ? textColor : "#ffffff";
    }

    public void setTextColor(@Nonnull String textColor) {
        this.textColor = textColor;
    }

    @Nonnull
    public String getTeleportDirection() {
        return teleportDirection != null ? teleportDirection : "north";
    }

    public void setTeleportDirection(@Nonnull String teleportDirection) {
        this.teleportDirection = teleportDirection;
    }

    @Nonnull
    public String getPlayerOrientation() {
        return playerOrientation != null ? playerOrientation : "away";
    }

    public void setPlayerOrientation(@Nonnull String playerOrientation) {
        this.playerOrientation = playerOrientation;
    }

    /**
     * Checks if this waystone is visible to the given player.
     * A waystone is visible if:
     * - It's public, OR
     * - The player is the creator/owner (by UUID)
     */
    public boolean isVisibleTo(@Nonnull String playerUuid) {
        if (isPublic) return true;
        return ownerUuid.equals(playerUuid);
    }

    /**
     * Checks if the given player can edit this waystone.
     * A player can edit if they are the creator/owner.
     */
    public boolean canEdit(@Nonnull String playerUuid) {
        return ownerUuid.equals(playerUuid);
    }

    /**
     * Checks if the given player is the owner/creator of this waystone.
     */
    public boolean isOwnedBy(@Nonnull String playerUuid) {
        return ownerUuid.equals(playerUuid);
    }

    /**
     * Checks if the given username is an editor.
     */
    public boolean isEditor(@Nonnull String username) {
        for (String editor : editors) {
            if (editor.equalsIgnoreCase(username)) return true;
        }
        return false;
    }

    /**
     * Checks if the given username is a viewer.
     */
    public boolean isViewer(@Nonnull String username) {
        for (String viewer : viewers) {
            if (viewer.equalsIgnoreCase(username)) return true;
        }
        return false;
    }

    /**
     * Gets the list of editor UUIDs.
     */
    @Nonnull
    public String[] getEditors() {
        return editors;
    }

    /**
     * Sets the list of editor UUIDs.
     */
    public void setEditors(@Nonnull String[] editors) {
        this.editors = editors;
    }

    /**
     * Adds an editor by UUID.
     */
    public void addEditor(@Nonnull String playerUuid) {
        if (!isEditor(playerUuid)) {
            String[] newEditors = Arrays.copyOf(editors, editors.length + 1);
            newEditors[editors.length] = playerUuid;
            this.editors = newEditors;
        }
    }

    /**
     * Removes an editor by UUID.
     */
    public void removeEditor(@Nonnull String playerUuid) {
        List<String> list = new ArrayList<>(Arrays.asList(editors));
        list.remove(playerUuid);
        this.editors = list.toArray(new String[0]);
    }

    /**
     * Gets the list of viewer UUIDs.
     */
    @Nonnull
    public String[] getViewers() {
        return viewers;
    }

    /**
     * Sets the list of viewer UUIDs.
     */
    public void setViewers(@Nonnull String[] viewers) {
        this.viewers = viewers;
    }

    /**
     * Adds a viewer by UUID.
     */
    public void addViewer(@Nonnull String playerUuid) {
        if (!isViewer(playerUuid)) {
            String[] newViewers = Arrays.copyOf(viewers, viewers.length + 1);
            newViewers[viewers.length] = playerUuid;
            this.viewers = newViewers;
        }
    }

    /**
     * Removes a viewer by UUID.
     */
    public void removeViewer(@Nonnull String playerUuid) {
        List<String> list = new ArrayList<>(Arrays.asList(viewers));
        list.remove(playerUuid);
        this.viewers = list.toArray(new String[0]);
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
     * The spawn position and facing direction are based on the configured teleportDirection.
     */
    @Nullable
    public Teleport toTeleport() {
        World world = Universe.get().getWorld(worldName);
        if (world == null) {
            System.out.println("[Waystone] toTeleport: World '" + worldName + "' not found!");
            return null;
        }
        
        // Calculate spawn position based on configured side
        double teleportX = x + 0.5;  // Center X
        double teleportY = y + 0.5;  // One block above waystone
        double teleportZ = z + 0.5;  // Center Z
        
        String side = getTeleportDirection();
        String orientation = getPlayerOrientation();
        
        // Determine spawn position based on side
        switch (side) {
            case "north" -> teleportZ -= 1.1;// Spawn north of statue (-Z)
            case "south" -> teleportZ += 1.1;  // Spawn south of statue (+Z)
            case "east" -> teleportX += 1.1;   // Spawn east of statue (+X)
            case "west" -> teleportX -= 1.1;   // Spawn west of statue (-X)
            default -> teleportZ -= 1.1;       // Default to north
        }
        
        // Determine facing direction based on orientation
        // "away" = face away from statue, "towards" = face towards statue
        float playerYaw = 0f;
        boolean facingTowards = "towards".equals(orientation);
        
        switch (side) {
            case "north" -> {
                // Spawned north of statue
                // Away = face north (-Z), Towards = face south (+Z)
                playerYaw = facingTowards ? (float) Math.PI : 0f;
            }
            case "south" -> {
                // Spawned south of statue
                // Away = face south (+Z), Towards = face north (-Z)
                playerYaw = facingTowards ? 0f : (float) Math.PI;
            }
            case "east" -> {
                // Spawned east of statue
                // Away = face east (+X), Towards = face west (-X)
                playerYaw = facingTowards ? (float) (Math.PI / 2) : (float) (-Math.PI / 2);
            }
            case "west" -> {
                // Spawned west of statue
                // Away = face west (-X), Towards = face east (+X)
                playerYaw = facingTowards ? (float) (-Math.PI / 2) : (float) (Math.PI / 2);
            }
            default -> playerYaw = facingTowards ? (float) Math.PI : 0f;
        }
        
        Vector3d position = new Vector3d(teleportX, teleportY, teleportZ);
        Vector3f rotation = new Vector3f(0, playerYaw, 0);
        
        System.out.println("[Waystone] toTeleport: name='" + name + "' world='" + worldName + 
                "' side='" + side + "' orientation='" + orientation + "' block pos=(" + x + ", " + y + ", " + z + ")" +
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
