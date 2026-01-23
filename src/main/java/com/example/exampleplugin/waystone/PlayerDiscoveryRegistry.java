package com.example.exampleplugin.waystone;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.util.BsonUtil;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.BsonValue;

import javax.annotation.Nonnull;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Registry for managing player waystone discovery data.
 * Tracks which waystones each player has discovered.
 */
public class PlayerDiscoveryRegistry {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String PLAYERS_FILE = "waystones-players.json";

    private static PlayerDiscoveryRegistry instance;

    // Map of player UUID -> Set of discovered waystone IDs
    private final Map<String, Set<String>> playerDiscoveries = new ConcurrentHashMap<>();
    private final AtomicBoolean loaded = new AtomicBoolean(false);
    private final ReentrantLock saveLock = new ReentrantLock();
    private final AtomicBoolean postSaveRedo = new AtomicBoolean(false);

    private PlayerDiscoveryRegistry() {
    }

    /**
     * Gets the singleton instance of the registry.
     */
    @Nonnull
    public static PlayerDiscoveryRegistry get() {
        if (instance == null) {
            instance = new PlayerDiscoveryRegistry();
        }
        return instance;
    }

    /**
     * Checks if the registry has been loaded from disk.
     */
    public boolean isLoaded() {
        return loaded.get();
    }

    /**
     * Loads player discovery data from disk.
     */
    public void load() {
        Path universePath = Universe.get().getPath();
        Path path = universePath.resolve(PLAYERS_FILE);

        if (Files.exists(path)) {
            try {
                BsonDocument document = BsonUtil.readDocument(path).join();

                if (document != null && document.containsKey("Players")) {
                    BsonDocument playersDoc = document.getDocument("Players");
                    playerDiscoveries.clear();

                    for (String playerUuid : playersDoc.keySet()) {
                        BsonDocument playerData = playersDoc.getDocument(playerUuid);
                        if (playerData.containsKey("discovered")) {
                            BsonArray discoveredArray = playerData.getArray("discovered");
                            Set<String> discoveredSet = new HashSet<>();
                            for (BsonValue value : discoveredArray) {
                                discoveredSet.add(value.asString().getValue());
                            }
                            playerDiscoveries.put(playerUuid, discoveredSet);
                        }
                    }

                    if (WaystoneRegistry.isDebugEnabled()) {
                        LOGGER.atInfo().log("Loaded discovery data for %d players", playerDiscoveries.size());
                    }
                }
            } catch (Exception e) {
                LOGGER.atWarning().log("Failed to load player discoveries: %s", e.getMessage());
            }
        } else {
            if (WaystoneRegistry.isDebugEnabled()) {
                LOGGER.atInfo().log("No player discovery file found, starting fresh");
            }
        }

        loaded.set(true);
    }

    /**
     * Saves player discovery data to disk.
     */
    public void save() {
        if (saveLock.tryLock()) {
            try {
                saveInternal();
            } catch (Exception e) {
                LOGGER.atWarning().log("Failed to save player discoveries: %s", e.getMessage());
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
        BsonDocument playersDoc = new BsonDocument();

        for (Map.Entry<String, Set<String>> entry : playerDiscoveries.entrySet()) {
            BsonArray discoveredArray = new BsonArray();
            for (String waystoneId : entry.getValue()) {
                discoveredArray.add(new BsonString(waystoneId));
            }

            BsonDocument playerData = new BsonDocument();
            playerData.put("discovered", discoveredArray);
            playersDoc.put(entry.getKey(), playerData);
        }

        BsonDocument document = new BsonDocument();
        document.put("Players", playersDoc);

        Path path = Universe.get().getPath().resolve(PLAYERS_FILE);
        BsonUtil.writeDocument(path, document).join();

        if (WaystoneRegistry.isDebugEnabled()) {
            LOGGER.atInfo().log("Saved discovery data for %d players to %s", playerDiscoveries.size(), PLAYERS_FILE);
        }
    }

    /**
     * Ensures a player entry exists in the registry.
     * Creates an empty discovery set if the player is not already tracked.
     */
    public void ensurePlayerExists(@Nonnull String playerUuid) {
        playerDiscoveries.computeIfAbsent(playerUuid, k -> new HashSet<>());
    }

    /**
     * Checks if a player has discovered a specific waystone.
     */
    public boolean hasDiscovered(@Nonnull String playerUuid, @Nonnull String waystoneId) {
        Set<String> discovered = playerDiscoveries.get(playerUuid);
        return discovered != null && discovered.contains(waystoneId);
    }

    /**
     * Marks a waystone as discovered by a player.
     * Discovery is always tracked regardless of the requireDiscover config.
     */
    public void discoverWaystone(@Nonnull String playerUuid, @Nonnull String waystoneId) {
        Set<String> discovered = playerDiscoveries.computeIfAbsent(playerUuid, k -> new HashSet<>());
        if (discovered.add(waystoneId)) {
            save();
            if (WaystoneRegistry.isDebugEnabled()) {
                LOGGER.atInfo().log("Player %s discovered waystone %s", playerUuid, waystoneId);
            }
        }
    }

    /**
     * Gets all discovered waystone IDs for a player.
     * Lazily filters out waystone IDs that no longer exist in WaystoneRegistry.
     * 
     * @return Set of valid discovered waystone IDs
     */
    @Nonnull
    public Set<String> getDiscoveredWaystones(@Nonnull String playerUuid) {
        Set<String> discovered = playerDiscoveries.get(playerUuid);
        if (discovered == null) {
            return Collections.emptySet();
        }

        // Lazily filter out non-existent waystones
        Set<String> validDiscoveries = discovered.stream()
                .filter(waystoneId -> WaystoneRegistry.get().exists(waystoneId))
                .collect(Collectors.toSet());

        // If we filtered out any stale entries, update and save
        if (validDiscoveries.size() < discovered.size()) {
            playerDiscoveries.put(playerUuid, validDiscoveries);
            save();
            if (WaystoneRegistry.isDebugEnabled()) {
                LOGGER.atInfo().log("Cleaned up %d stale discovery entries for player %s",
                        discovered.size() - validDiscoveries.size(), playerUuid);
            }
        }

        return Collections.unmodifiableSet(validDiscoveries);
    }

    /**
     * Gets the count of players with discovery data.
     */
    public int getPlayerCount() {
        return playerDiscoveries.size();
    }

    /**
     * Clears all discovery data (for testing).
     */
    public void clear() {
        playerDiscoveries.clear();
        save();
    }
}
