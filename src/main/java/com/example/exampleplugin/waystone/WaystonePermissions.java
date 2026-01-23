package com.example.exampleplugin.waystone;

/**
 * Central location for all waystone permission strings.
 * This ensures consistency and makes it easy to find/change permissions.
 */
public final class WaystonePermissions {
    
    private WaystonePermissions() {
        // Utility class - prevent instantiation
    }
    
    // ============================================
    // ALLOW PERMISSIONS (grant additional access)
    // ============================================
    
    /**
     * Grants access to the /waystones list command.
     */
    public static final String ALLOW_LIST_MENU = "hytale.command.waystones.allowListMenu";
    
    /**
     * Allows editing any waystone's settings, including admin-only options
     * like Priority, ServerOwned, and Reset.
     */
    public static final String ALLOW_EDIT_ALL = "hytale.command.waystones.allowEditAll";
    
    /**
     * Allows viewing and teleporting to all private waystones.
     */
    public static final String ALLOW_SEE_ALL_PRIVATE = "hytale.command.waystones.allowSeeAllPrivate";
    
    /**
     * Allows breaking private waystones owned by other players.
     */
    public static final String ALLOW_PRIVATE_WAYSTONE_REMOVAL = "hytale.command.waystones.allowPrivateWaystoneRemoval";
    
    /**
     * Allows viewing all waystones regardless of discovery status.
     * Bypasses the requireDiscover config setting.
     */
    public static final String ALLOW_SHOW_UNDISCOVERED = "hytale.command.waystones.allowShowUndiscovered";
    
    // ============================================
    // BLOCK PERMISSIONS (deny/restrict access)
    // OPs bypass all block permissions
    // ============================================
    
    /**
     * Prevents placing waystone blocks.
     */
    public static final String BLOCK_WAYSTONE_PLACEMENT = "hytale.command.waystones.blockWaystonePlacement";
    
    /**
     * Prevents breaking waystone blocks.
     */
    public static final String BLOCK_WAYSTONE_REMOVAL = "hytale.command.waystones.blockWaystoneRemoval";
    
    /**
     * Forces new waystones to be private and hides visibility dropdown.
     */
    public static final String BLOCK_PUBLIC_WAYSTONE_CREATION = "hytale.command.waystones.blockPublicWaystoneCreation";
    
    // ============================================
    // LIMIT PERMISSIONS
    // ============================================
    
    /**
     * Prefix for max waystones limit permissions.
     * Append a number, e.g., "hytale.command.waystones.maxWaystones.5"
     */
    public static final String MAX_WAYSTONES_PREFIX = "hytale.command.waystones.maxWaystones.";
    
    // ============================================
    // GROUP NAMES
    // ============================================
    
    /**
     * The OP group name that bypasses all restrictions.
     */
    public static final String OP_GROUP = "OP";
}
