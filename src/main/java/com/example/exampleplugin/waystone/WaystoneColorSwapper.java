package com.example.exampleplugin.waystone;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;

import javax.annotation.Nonnull;

/**
 * Utility class for swapping waystone blocks when color changes.
 */
public class WaystoneColorSwapper {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * Swaps the waystone block in-world to match the waystone's current color.
     * 
     * @param waystoneId The ID of the waystone to update
     * @return true if the block was swapped successfully
     */
    public static boolean swapBlock(@Nonnull String waystoneId) {
        Waystone waystone = WaystoneRegistry.get().get(waystoneId);
        if (waystone == null) {
            if (WaystoneRegistry.isDebugEnabled()) {
                LOGGER.atWarning().log("Cannot swap block: waystone %s not found", waystoneId);
            }
            return false;
        }

        return swapBlock(waystone);
    }

    /**
     * Swaps the waystone block in-world to match the waystone's current color.
     * 
     * @param waystone The waystone to update
     * @return true if the block was swapped successfully
     */
    public static boolean swapBlock(@Nonnull Waystone waystone) {
        LOGGER.atInfo().log("[ColorSwapper] Starting swap for waystone '%s' to color '%s'", 
                waystone.getName(), waystone.getColor());
        
        World world = Universe.get().getWorld(waystone.getWorldName());
        if (world == null) {
            LOGGER.atWarning().log("[ColorSwapper] Cannot swap block: world '%s' not found", waystone.getWorldName());
            return false;
        }

        // World coordinates
        int worldX = (int) Math.floor(waystone.getX());
        int worldY = (int) waystone.getY();
        int worldZ = (int) Math.floor(waystone.getZ());

        // Chunks in Hytale are 32x32, so we use >> 5 (divide by 32)
        long chunkIndex = ChunkUtil.indexChunk(worldX >> 5, worldZ >> 5);
        WorldChunk chunk = world.getChunkIfInMemory(chunkIndex);

        if (chunk == null) {
            LOGGER.atWarning().log("[ColorSwapper] Cannot swap block: chunk not loaded for waystone '%s' at (%d, %d, %d)",
                    waystone.getName(), worldX, worldY, worldZ);
            return false;
        }

        String newBlockId = waystone.getBlockId();
        LOGGER.atInfo().log("[ColorSwapper] Looking up block type: '%s'", newBlockId);
        
        BlockType newBlockType = BlockType.getAssetMap().getAsset(newBlockId);
        
        if (newBlockType == null) {
            // Try with namespace prefix - plugin group is "Waystones"
            String namespacedId = "Waystones:" + newBlockId;
            LOGGER.atInfo().log("[ColorSwapper] Block type '%s' not found, trying '%s'", newBlockId, namespacedId);
            newBlockType = BlockType.getAssetMap().getAsset(namespacedId);
            if (newBlockType != null) {
                newBlockId = namespacedId;
            } else {
                // Try lowercase
                namespacedId = "waystones:" + newBlockId;
                LOGGER.atInfo().log("[ColorSwapper] Block type not found, trying '%s'", namespacedId);
                newBlockType = BlockType.getAssetMap().getAsset(namespacedId);
                if (newBlockType != null) {
                    newBlockId = namespacedId;
                }
            }
        }
        
        if (newBlockType == null) {
            LOGGER.atWarning().log("[ColorSwapper] Cannot swap block: block type '%s' not found in asset map", newBlockId);
            return false;
        }

        // Convert to local chunk coordinates (chunks are 32x32)
        int localX = worldX & 0x1F;  // worldX % 32
        int localZ = worldZ & 0x1F;  // worldZ % 32

        // Set the new block using local coordinates
        int blockIdInt = BlockType.getAssetMap().getIndex(newBlockId);
        LOGGER.atInfo().log("[ColorSwapper] Setting block at world (%d, %d, %d) local (%d, %d, %d) to %s (id: %d)", 
                worldX, worldY, worldZ, localX, worldY, localZ, newBlockId, blockIdInt);
        
        boolean success = chunk.setBlock(localX, worldY, localZ, blockIdInt, newBlockType, 0, 0, 0);

        LOGGER.atInfo().log("[ColorSwapper] Swapped waystone '%s' block to %s at world (%d, %d, %d): %s",
                waystone.getName(), newBlockId, worldX, worldY, worldZ, success ? "success" : "failed");

        return success;
    }
}
