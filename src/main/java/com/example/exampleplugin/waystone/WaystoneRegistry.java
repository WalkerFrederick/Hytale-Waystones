package com.example.exampleplugin.waystone;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.util.BsonUtil;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Registry for managing all waystones in the game.
 * Handles CRUD operations and persistence to disk.
 */
public class WaystoneRegistry {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String WAYSTONES_FILE = "waystones.json";

    private static WaystoneRegistry instance;

    private final Map<String, Waystone> waystones = new ConcurrentHashMap<>();
    private final AtomicBoolean loaded = new AtomicBoolean(false);
    private final ReentrantLock saveLock = new ReentrantLock();
    private final AtomicBoolean postSaveRedo = new AtomicBoolean(false);

    private WaystoneRegistry() {
    }

    /**
     * Gets the singleton instance of the registry.
     */
    @Nonnull
    public static WaystoneRegistry get() {
        if (instance == null) {
            instance = new WaystoneRegistry();
        }
        return instance;
    }

    /**
     * Checks if waystones have been loaded from disk.
     */
    public boolean isLoaded() {
        return loaded.get();
    }

    /**
     * Loads waystones from disk.
     */
    public void load() {
        Path universePath = Universe.get().getPath();
        Path path = universePath.resolve(WAYSTONES_FILE);

        if (Files.exists(path)) {
            try {
                BsonDocument document = BsonUtil.readDocument(path).join();
                if (document != null && document.containsKey("Waystones")) {
                    BsonArray bsonWarps = document.getArray("Waystones");
                    waystones.clear();
                    Waystone[] loaded = Waystone.ARRAY_CODEC.decode((BsonValue) bsonWarps);
                    for (Waystone waystone : loaded) {
                        waystones.put(waystone.getId(), waystone);
                    }
                    LOGGER.atInfo().log("Loaded %d waystones", waystones.size());
                }
            } catch (Exception e) {
                LOGGER.atWarning().log("Failed to load waystones: %s", e.getMessage());
            }
        } else {
            LOGGER.atInfo().log("No waystones file found, starting fresh");
        }

        loaded.set(true);
    }

    /**
     * Saves waystones to disk.
     */
    public void save() {
        if (saveLock.tryLock()) {
            try {
                saveInternal();
            } catch (Exception e) {
                LOGGER.atWarning().log("Failed to save waystones: %s", e.getMessage());
            } finally {
                saveLock.unlock();
            }
            if (postSaveRedo.getAndSet(false)) {
                save();
            }
        } else {
            postSaveRedo.set(true);
        }
    }

    private void saveInternal() {
        Waystone[] array = waystones.values().toArray(new Waystone[0]);
        BsonDocument document = new BsonDocument("Waystones", Waystone.ARRAY_CODEC.encode(array));

        Path path = Universe.get().getPath().resolve(WAYSTONES_FILE);
        BsonUtil.writeDocument(path, document).join();
        LOGGER.atInfo().log("Saved %d waystones to %s", array.length, WAYSTONES_FILE);
    }

    /**
     * Registers a new waystone.
     */
    public void register(@Nonnull Waystone waystone) {
        waystones.put(waystone.getId(), waystone);
        save();
        LOGGER.atInfo().log("Registered waystone: %s", waystone.getName());
    }

    /**
     * Unregisters a waystone by ID.
     */
    public boolean unregister(@Nonnull String waystoneId) {
        Waystone removed = waystones.remove(waystoneId);
        if (removed != null) {
            save();
            LOGGER.atInfo().log("Unregistered waystone: %s", removed.getName());
            return true;
        }
        return false;
    }

    /**
     * Gets a waystone by ID.
     */
    @Nullable
    public Waystone get(@Nonnull String waystoneId) {
        return waystones.get(waystoneId);
    }

    /**
     * Gets a waystone by block position.
     */
    @Nullable
    public Waystone getByPosition(@Nonnull String worldName, double x, double y, double z) {
        for (Waystone waystone : waystones.values()) {
            if (waystone.getWorldName().equals(worldName) &&
                Math.floor(waystone.getX()) == Math.floor(x) &&
                Math.floor(waystone.getY()) == Math.floor(y) &&
                Math.floor(waystone.getZ()) == Math.floor(z)) {
                return waystone;
            }
        }
        return null;
    }

    /**
     * Gets all waystones.
     */
    @Nonnull
    public Collection<Waystone> getAll() {
        return Collections.unmodifiableCollection(waystones.values());
    }

    /**
     * Gets all waystones owned by a player.
     */
    @Nonnull
    public List<Waystone> getByOwner(@Nonnull String ownerUuid) {
        return waystones.values().stream()
                .filter(w -> w.getOwnerUuid().equals(ownerUuid))
                .collect(Collectors.toList());
    }

    /**
     * Gets all waystones visible to a player (public + owned).
     */
    @Nonnull
    public List<Waystone> getVisibleTo(@Nonnull String playerUuid) {
        return waystones.values().stream()
                .filter(w -> w.isVisibleTo(playerUuid))
                .sorted(Comparator.comparing(Waystone::getName))
                .collect(Collectors.toList());
    }

    /**
     * Gets all public waystones.
     */
    @Nonnull
    public List<Waystone> getPublic() {
        return waystones.values().stream()
                .filter(Waystone::isPublic)
                .collect(Collectors.toList());
    }

    /**
     * Updates a waystone's name.
     */
    public void updateName(@Nonnull String waystoneId, @Nonnull String newName) {
        Waystone waystone = waystones.get(waystoneId);
        if (waystone != null) {
            waystone.setName(newName);
            save();
            LOGGER.atInfo().log("Updated waystone name to: %s", newName);
        }
    }

    /**
     * Toggles a waystone's visibility.
     */
    public void toggleVisibility(@Nonnull String waystoneId) {
        Waystone waystone = waystones.get(waystoneId);
        if (waystone != null) {
            waystone.setPublic(!waystone.isPublic());
            save();
            LOGGER.atInfo().log("Toggled waystone visibility: %s is now %s",
                    waystone.getName(), waystone.isPublic() ? "public" : "private");
        }
    }

    /**
     * Gets the count of waystones.
     */
    public int count() {
        return waystones.size();
    }

    /**
     * Clears all waystones (for testing).
     */
    public void clear() {
        waystones.clear();
        save();
    }

    /**
     * Cleans up orphaned waystones in a specific world.
     * A waystone is orphaned if its block no longer exists at its position.
     * 
     * @param world The world to check waystones in
     * @return The number of waystones removed
     */
    public int cleanupOrphanedWaystones(@Nonnull World world) {
        String worldName = world.getName();
        List<String> toRemove = new ArrayList<>();

        for (Waystone waystone : waystones.values()) {
            // Only check waystones in this world
            if (!waystone.getWorldName().equals(worldName)) {
                continue;
            }

            // Check if the block at this position is still a waystone block
            int x = (int) Math.floor(waystone.getX());
            int y = (int) Math.floor(waystone.getY());
            int z = (int) Math.floor(waystone.getZ());

            if (!isWaystoneBlockAt(world, x, y, z)) {
                toRemove.add(waystone.getId());
                LOGGER.atInfo().log("Found orphaned waystone '%s' at %d, %d, %d - block no longer exists",
                        waystone.getName(), x, y, z);
            }
        }

        // Remove orphaned waystones
        for (String id : toRemove) {
            waystones.remove(id);
        }

        if (!toRemove.isEmpty()) {
            save();
            LOGGER.atInfo().log("Cleaned up %d orphaned waystones", toRemove.size());
        }

        return toRemove.size();
    }

    /**
     * Checks if a waystone block exists at the given position.
     */
    private boolean isWaystoneBlockAt(@Nonnull World world, int x, int y, int z) {
        try {
            long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
            WorldChunk chunk = world.getChunkIfInMemory(chunkIndex);
            
            if (chunk == null) {
                // Chunk not loaded - assume waystone still exists to avoid false positives
                return true;
            }

            BlockType blockType = chunk.getBlockType(x, y, z);
            if (blockType == null) {
                return false;
            }

            String blockId = blockType.getId();
            // Check if it's our waystone block (handles namespacing)
            return blockId != null && (blockId.equals("Warp_Block") || blockId.endsWith(":Warp_Block"));
        } catch (Exception e) {
            // If we can't check, assume it exists to be safe
            LOGGER.atWarning().log("Failed to check block at %d, %d, %d: %s", x, y, z, e.getMessage());
            return true;
        }
    }
}
