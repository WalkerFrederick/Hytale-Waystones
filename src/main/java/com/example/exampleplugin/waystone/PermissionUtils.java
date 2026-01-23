package com.example.exampleplugin.waystone;

import com.hypixel.hytale.server.core.permissions.PermissionsModule;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.UUID;

/**
 * Utility class for permission checking.
 * Provides consistent permission logic across all waystone components.
 */
public final class PermissionUtils {
    
    private PermissionUtils() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Checks if a user is in the OP group.
     * 
     * @param uuid The player's UUID
     * @return true if the player is an OP
     */
    public static boolean isOp(@Nonnull UUID uuid) {
        for (var provider : PermissionsModule.get().getProviders()) {
            if (provider.getGroupsForUser(uuid).contains(WaystonePermissions.OP_GROUP)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Checks if a user has a specific permission.
     * This checks direct user permissions and group permissions, but does NOT
     * automatically grant permissions to OPs. Use {@link #hasPermissionOrOp} for that.
     * 
     * @param uuid The player's UUID
     * @param permission The permission string to check
     * @return true if the player has the permission
     */
    public static boolean hasPermission(@Nonnull UUID uuid, @Nonnull String permission) {
        for (var provider : PermissionsModule.get().getProviders()) {
            // Check direct user permissions
            if (provider.getUserPermissions(uuid).contains(permission)) {
                return true;
            }
            
            // Check group permissions
            for (String group : provider.getGroupsForUser(uuid)) {
                if (provider.getGroupPermissions(group).contains(permission)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Checks if a user has a specific permission OR is an OP.
     * OPs are automatically granted all "allow" permissions.
     * 
     * @param uuid The player's UUID
     * @param permission The permission string to check
     * @return true if the player has the permission or is an OP
     */
    public static boolean hasPermissionOrOp(@Nonnull UUID uuid, @Nonnull String permission) {
        if (isOp(uuid)) {
            return true;
        }
        return hasPermission(uuid, permission);
    }
    
    /**
     * Gets the maximum waystones limit for a player based on their permissions.
     * Checks for permissions like maxWaystones.1, maxWaystones.2, etc. and returns the highest value.
     * 
     * @param uuid The player's UUID
     * @return The max limit, or -1 if no limit is set (unlimited)
     */
    public static int getMaxWaystonesLimit(@Nonnull UUID uuid) {
        int maxLimit = -1; // -1 means no limit set
        
        for (var provider : PermissionsModule.get().getProviders()) {
            // Check direct user permissions
            maxLimit = Math.max(maxLimit, findMaxWaystonesPerm(provider.getUserPermissions(uuid)));
            
            // Check group permissions
            for (String group : provider.getGroupsForUser(uuid)) {
                maxLimit = Math.max(maxLimit, findMaxWaystonesPerm(provider.getGroupPermissions(group)));
            }
        }
        
        return maxLimit;
    }
    
    /**
     * Finds the highest maxWaystones.X permission value from a set of permissions.
     */
    private static int findMaxWaystonesPerm(@Nonnull Set<String> permissions) {
        int max = -1;
        for (String perm : permissions) {
            if (perm.startsWith(WaystonePermissions.MAX_WAYSTONES_PREFIX)) {
                try {
                    int value = Integer.parseInt(perm.substring(WaystonePermissions.MAX_WAYSTONES_PREFIX.length()));
                    max = Math.max(max, value);
                } catch (NumberFormatException ignored) {
                    // Not a valid number, skip
                }
            }
        }
        return max;
    }
}
