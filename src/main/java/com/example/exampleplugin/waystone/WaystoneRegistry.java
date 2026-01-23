package com.example.exampleplugin.waystone;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.Universe;
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

    // Config settings
    private boolean debugLogs = false;
    private boolean requireDiscover = false;

    private WaystoneRegistry() {
    }

    /**
     * Checks if debug logging is enabled.
     */
    public static boolean isDebugEnabled() {
        return instance != null && instance.debugLogs;
    }

    /**
     * Sets whether debug logging is enabled.
     */
    public void setDebugLogs(boolean enabled) {
        this.debugLogs = enabled;
        save();
    }

    /**
     * Checks if waystone discovery is required.
     * When enabled, players can only see waystones they have discovered.
     */
    public static boolean isRequireDiscoverEnabled() {
        return instance != null && instance.requireDiscover;
    }

    /**
     * Sets whether waystone discovery is required.
     */
    public void setRequireDiscover(boolean enabled) {
        this.requireDiscover = enabled;
        save();
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
                
                // Load config section
                if (document != null && document.containsKey("Config")) {
                    BsonDocument config = document.getDocument("Config");
                    if (config.containsKey("debugLogs")) {
                        debugLogs = config.getBoolean("debugLogs").getValue();
                    }
                    if (config.containsKey("requireDiscover")) {
                        requireDiscover = config.getBoolean("requireDiscover").getValue();
                    }
                }
                
                // Load waystones
                if (document != null && document.containsKey("Waystones")) {
                    BsonArray bsonWarps = document.getArray("Waystones");
                    waystones.clear();
                    Waystone[] loaded = Waystone.ARRAY_CODEC.decode((BsonValue) bsonWarps);
                    for (Waystone waystone : loaded) {
                        waystones.put(waystone.getId(), waystone);
                    }
                    if (debugLogs) {
                        LOGGER.atInfo().log("Loaded %d waystones", waystones.size());
                    }
                }
            } catch (Exception e) {
                LOGGER.atWarning().log("Failed to load waystones: %s", e.getMessage());
            }
        } else {
            if (debugLogs) {
                LOGGER.atInfo().log("No waystones file found, starting fresh");
            }
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
        
        // Build config section
        BsonDocument config = new BsonDocument();
        config.put("debugLogs", new org.bson.BsonBoolean(debugLogs));
        config.put("requireDiscover", new org.bson.BsonBoolean(requireDiscover));
        
        // Build main document
        BsonDocument document = new BsonDocument();
        document.put("Config", config);
        document.put("Waystones", Waystone.ARRAY_CODEC.encode(array));

        Path path = Universe.get().getPath().resolve(WAYSTONES_FILE);
        BsonUtil.writeDocument(path, document).join();
        if (debugLogs) {
            LOGGER.atInfo().log("Saved %d waystones to %s", array.length, WAYSTONES_FILE);
        }
    }

    /**
     * Registers a new waystone.
     */
    public void register(@Nonnull Waystone waystone) {
        waystones.put(waystone.getId(), waystone);
        save();
        if (debugLogs) {
            LOGGER.atInfo().log("Registered waystone: %s", waystone.getName());
        }
    }

    /**
     * Unregisters a waystone by ID.
     */
    public boolean unregister(@Nonnull String waystoneId) {
        Waystone removed = waystones.remove(waystoneId);
        if (removed != null) {
            save();
            if (debugLogs) {
                LOGGER.atInfo().log("Unregistered waystone: %s", removed.getName());
            }
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
                .sorted(Comparator.comparingInt(Waystone::getPriority).reversed()
                        .thenComparing(Waystone::getName, String.CASE_INSENSITIVE_ORDER))
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
     * Checks if a waystone name is already taken.
     * 
     * @param name The name to check
     * @return true if the name is already in use
     */
    public boolean isNameTaken(@Nonnull String name) {
        return isNameTaken(name, null);
    }

    /**
     * Checks if a waystone name is already taken, excluding a specific waystone.
     * Used for renaming to allow keeping the same name.
     * 
     * @param name The name to check
     * @param excludeId The waystone ID to exclude from the check (can be null)
     * @return true if the name is already in use by another waystone
     */
    public boolean isNameTaken(@Nonnull String name, @Nullable String excludeId) {
        String normalizedName = name.trim().toLowerCase();
        for (Waystone waystone : waystones.values()) {
            if (excludeId != null && waystone.getId().equals(excludeId)) {
                continue;
            }
            if (waystone.getName().trim().toLowerCase().equals(normalizedName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Generates a unique name by appending a number if needed.
     * 
     * @param baseName The desired name
     * @param excludeId The waystone ID to exclude (for renaming)
     * @return A unique name (either the original or with a number suffix)
     */
    @Nonnull
    public String generateUniqueName(@Nonnull String baseName, @Nullable String excludeId) {
        if (!isNameTaken(baseName, excludeId)) {
            return baseName;
        }
        
        int counter = 2;
        String newName;
        do {
            newName = baseName + " " + counter;
            counter++;
        } while (isNameTaken(newName, excludeId));
        
        return newName;
    }

    /**
     * Updates a waystone's name.
     * Note: Name uniqueness should be validated before calling this method.
     */
    public void updateName(@Nonnull String waystoneId, @Nonnull String newName) {
        Waystone waystone = waystones.get(waystoneId);
        if (waystone != null) {
            waystone.setName(newName);
            save();
            if (debugLogs) {
                LOGGER.atInfo().log("Updated waystone name to: %s", newName);
            }
        }
    }

    /**
     * Updates a waystone's priority.
     * Lower numbers appear first in the list.
     */
    public void updatePriority(@Nonnull String waystoneId, int priority) {
        Waystone waystone = waystones.get(waystoneId);
        if (waystone != null) {
            waystone.setPriority(priority);
            save();
            if (debugLogs) {
                LOGGER.atInfo().log("Updated waystone priority to: %d", priority);
            }
        }
    }

    /**
     * Updates a waystone's text color.
     */
    public void updateTextColor(@Nonnull String waystoneId, @Nonnull String textColor) {
        Waystone waystone = waystones.get(waystoneId);
        if (waystone != null) {
            waystone.setTextColor(textColor);
            save();
            if (debugLogs) {
                LOGGER.atInfo().log("Updated waystone text color to: %s", textColor);
            }
        }
    }

    /**
     * Updates a waystone's teleport direction.
     */
    public void updateTeleportDirection(@Nonnull String waystoneId, @Nonnull String direction) {
        Waystone waystone = waystones.get(waystoneId);
        if (waystone != null) {
            waystone.setTeleportDirection(direction);
            save();
            if (debugLogs) {
                LOGGER.atInfo().log("Updated waystone teleport direction to: %s", direction);
            }
        }
    }

    /**
     * Updates a waystone's player orientation.
     */
    public void updatePlayerOrientation(@Nonnull String waystoneId, @Nonnull String orientation) {
        Waystone waystone = waystones.get(waystoneId);
        if (waystone != null) {
            waystone.setPlayerOrientation(orientation);
            save();
            if (debugLogs) {
                LOGGER.atInfo().log("Updated waystone player orientation to: %s", orientation);
            }
        }
    }

    /**
     * Updates a waystone's server owned status.
     */
    public void updateServerOwned(@Nonnull String waystoneId, boolean serverOwned) {
        Waystone waystone = waystones.get(waystoneId);
        if (waystone != null) {
            waystone.setServerOwned(serverOwned);
            save();
            if (debugLogs) {
                LOGGER.atInfo().log("Updated waystone server owned to: %s", serverOwned);
            }
        }
    }

    /**
     * Updates a waystone's default discovered status.
     */
    public void updateDefaultDiscovered(@Nonnull String waystoneId, boolean defaultDiscovered) {
        Waystone waystone = waystones.get(waystoneId);
        if (waystone != null) {
            waystone.setDefaultDiscovered(defaultDiscovered);
            save();
            if (debugLogs) {
                LOGGER.atInfo().log("Updated waystone default discovered to: %s", defaultDiscovered);
            }
        }
    }

    /**
     * Updates a waystone's color and swaps the block in-world.
     * @param waystoneId The waystone ID
     * @param color The new color ("default", "red", "green")
     * @return true if the color was updated and block swapped successfully
     */
    public boolean updateColor(@Nonnull String waystoneId, @Nonnull String color) {
        Waystone waystone = waystones.get(waystoneId);
        if (waystone == null) {
            return false;
        }
        
        String oldColor = waystone.getColor();
        if (oldColor.equals(color)) {
            return true; // No change needed
        }
        
        // Update the waystone color in data
        waystone.setColor(color);
        save();
        
        if (debugLogs) {
            LOGGER.atInfo().log("Updated waystone '%s' color from %s to %s", waystone.getName(), oldColor, color);
        }
        
        // Block swapping will be handled by WaystoneColorSwapper utility
        return true;
    }

    /**
     * Toggles a waystone's visibility.
     */
    public void toggleVisibility(@Nonnull String waystoneId) {
        Waystone waystone = waystones.get(waystoneId);
        if (waystone != null) {
            waystone.setPublic(!waystone.isPublic());
            save();
            if (debugLogs) {
                LOGGER.atInfo().log("Toggled waystone visibility: %s is now %s",
                        waystone.getName(), waystone.isPublic() ? "public" : "private");
            }
        }
    }

    /**
     * Adds an editor to a waystone.
     */
    public void addEditor(@Nonnull String waystoneId, @Nonnull String playerUuid) {
        Waystone waystone = waystones.get(waystoneId);
        if (waystone != null) {
            waystone.addEditor(playerUuid);
            save();
            if (debugLogs) {
                LOGGER.atInfo().log("Added editor %s to waystone %s", playerUuid, waystone.getName());
            }
        }
    }

    /**
     * Removes an editor from a waystone.
     */
    public void removeEditor(@Nonnull String waystoneId, @Nonnull String playerUuid) {
        Waystone waystone = waystones.get(waystoneId);
        if (waystone != null) {
            waystone.removeEditor(playerUuid);
            save();
            if (debugLogs) {
                LOGGER.atInfo().log("Removed editor %s from waystone %s", playerUuid, waystone.getName());
            }
        }
    }

    /**
     * Adds a viewer to a waystone.
     */
    public void addViewer(@Nonnull String waystoneId, @Nonnull String playerUuid) {
        Waystone waystone = waystones.get(waystoneId);
        if (waystone != null) {
            waystone.addViewer(playerUuid);
            save();
            if (debugLogs) {
                LOGGER.atInfo().log("Added viewer %s to waystone %s", playerUuid, waystone.getName());
            }
        }
    }

    /**
     * Removes a viewer from a waystone.
     */
    public void removeViewer(@Nonnull String waystoneId, @Nonnull String playerUuid) {
        Waystone waystone = waystones.get(waystoneId);
        if (waystone != null) {
            waystone.removeViewer(playerUuid);
            save();
            if (debugLogs) {
                LOGGER.atInfo().log("Removed viewer %s from waystone %s", playerUuid, waystone.getName());
            }
        }
    }

    /**
     * Gets the count of waystones.
     */
    public int count() {
        return waystones.size();
    }

    /**
     * Checks if a waystone with the given ID exists.
     */
    public boolean exists(@Nonnull String waystoneId) {
        return waystones.containsKey(waystoneId);
    }
    
    /**
     * Counts the number of waystones owned by a specific player.
     * @param ownerUuid The UUID of the player (as string)
     * @return The number of waystones owned by this player
     */
    public int countByOwner(@Nonnull String ownerUuid) {
        return (int) waystones.values().stream()
                .filter(w -> ownerUuid.equals(w.getOwnerUuid()))
                .count();
    }

    /**
     * Clears all waystones (for testing).
     */
    public void clear() {
        waystones.clear();
        save();
    }

}
