package com.easyclaims;

import com.easyclaims.config.PluginConfig;
import com.easyclaims.data.AdminClaims;
import com.easyclaims.data.Claim;
import com.easyclaims.data.ClaimStorage;
import com.easyclaims.data.PlayerClaims;
import com.easyclaims.data.TrustedPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Static accessor for claim data used by the map system.
 * This is needed because the map image builder runs asynchronously
 * and needs access to claim information.
 */
public class EasyClaimsAccess {
    private static ClaimStorage claimStorage;
    private static PluginConfig pluginConfig;

    /**
     * Initializes the accessor with the claim storage and config instances.
     * Called during plugin startup.
     */
    public static void init(ClaimStorage storage, PluginConfig config) {
        claimStorage = storage;
        pluginConfig = config;
        System.out.println("[EasyClaimsAccess] Initialized with claimStorage: " + (storage != null ? "OK" : "NULL"));
    }

    /**
     * Gets the owner of a chunk, or null if unclaimed.
     * Used by ClaimImageBuilder to determine claim colors.
     */
    public static UUID getClaimOwner(String worldName, int chunkX, int chunkZ) {
        if (claimStorage == null) {
            System.out.println("[EasyClaimsAccess] ERROR: claimStorage is null!");
            return null;
        }
        return claimStorage.getClaimOwner(worldName, chunkX, chunkZ);
    }

    /**
     * Gets the name of a player by their UUID.
     */
    public static String getPlayerName(UUID playerId) {
        if (claimStorage == null || playerId == null) {
            return "Unknown";
        }
        return claimStorage.getPlayerName(playerId);
    }

    /**
     * Gets the owner name for a claimed chunk.
     */
    public static String getOwnerName(String worldName, int chunkX, int chunkZ) {
        UUID owner = getClaimOwner(worldName, chunkX, chunkZ);
        if (owner == null) {
            return null;
        }
        return getPlayerName(owner);
    }

    /**
     * Gets the list of trusted player names for a claimed chunk.
     * Returns an empty list if unclaimed or no trusted players.
     */
    public static List<String> getTrustedPlayerNames(String worldName, int chunkX, int chunkZ) {
        List<String> names = new ArrayList<>();
        if (claimStorage == null) {
            return names;
        }

        UUID owner = getClaimOwner(worldName, chunkX, chunkZ);
        if (owner == null) {
            return names;
        }

        PlayerClaims playerClaims = claimStorage.getPlayerClaims(owner);
        if (playerClaims == null) {
            return names;
        }

        Map<UUID, TrustedPlayer> trustedMap = playerClaims.getTrustedPlayersMap();
        for (TrustedPlayer trusted : trustedMap.values()) {
            names.add(trusted.getName());
        }

        return names;
    }

    /**
     * Checks if PvP is disabled at a location (for map rendering).
     * - Unclaimed: PvP enabled (returns false)
     * - Admin claims: Use claim's pvpEnabled setting
     * - Player claims: Use server config (pvpInPlayerClaims)
     */
    public static boolean isPvPDisabled(String worldName, int chunkX, int chunkZ) {
        if (claimStorage == null) return false;
        Claim claim = claimStorage.getClaimAt(worldName, chunkX, chunkZ);
        if (claim == null) {
            return false; // Unclaimed = PvP enabled
        }

        // Admin claims use their own per-claim setting
        if (claim.isAdminClaim()) {
            return !claim.isPvpEnabled();
        }

        // Player claims use the global server setting
        if (pluginConfig == null) return false;
        return !pluginConfig.isPvpInPlayerClaims();
    }

    /**
     * Checks if a chunk is an admin claim.
     */
    public static boolean isAdminClaim(String worldName, int chunkX, int chunkZ) {
        if (claimStorage == null) return false;
        UUID owner = claimStorage.getClaimOwner(worldName, chunkX, chunkZ);
        return AdminClaims.isAdminClaim(owner);
    }

    /**
     * Gets the display name for a claim (for admin claims with custom names).
     * Returns the custom display name if set, otherwise returns the owner name.
     */
    public static String getClaimDisplayName(String worldName, int chunkX, int chunkZ) {
        if (claimStorage == null) return null;

        Claim claim = claimStorage.getClaimAt(worldName, chunkX, chunkZ);
        if (claim != null && claim.getDisplayName() != null && !claim.getDisplayName().isEmpty()) {
            return claim.getDisplayName();
        }

        // Fall back to owner name
        return getOwnerName(worldName, chunkX, chunkZ);
    }

    /**
     * Checks if claim overlays should be shown on the map.
     * This is a global setting that affects all players.
     */
    public static boolean shouldShowClaimsOnMap() {
        if (pluginConfig == null) return true; // Default to showing claims
        return pluginConfig.isShowClaimsOnMap();
    }

    /**
     * Gets the effective text scale for map rendering.
     * @param imageWidth The width of the map image
     * @param imageHeight The height of the map image
     * @return The scale factor (0 = no text, 1-3 = text size)
     */
    public static int getMapTextScale(int imageWidth, int imageHeight) {
        if (pluginConfig == null) {
            // Default AUTO behavior - match PluginConfig.MapTextScale.AUTO
            // Keep text small - scale 1 is 7px tall, good for most tile sizes
            int minDimension = Math.min(imageWidth, imageHeight);
            if (minDimension < 12) return 0;  // Too small for any text
            if (minDimension < 128) return 1; // Scale 1 (7px) for 12-127px tiles
            if (minDimension < 384) return 2; // Scale 2 (14px) for 128-383px tiles
            return 3;                         // Scale 3 (21px) for 384px+ tiles
        }
        return pluginConfig.getMapTextScale().getEffectiveScale(imageWidth, imageHeight);
    }

    /**
     * Gets the effective text mode for map rendering.
     * @param imageWidth The width of the map image
     * @param imageHeight The height of the map image
     * @return The text mode (never AUTO - resolved to actual mode)
     */
    public static PluginConfig.MapTextMode getMapTextMode(int imageWidth, int imageHeight) {
        if (pluginConfig == null) {
            // Default AUTO behavior
            int minDimension = Math.min(imageWidth, imageHeight);
            return minDimension >= 64 ? PluginConfig.MapTextMode.OVERFLOW : PluginConfig.MapTextMode.FIT;
        }
        return pluginConfig.getMapTextMode().getEffectiveMode(imageWidth, imageHeight);
    }

    /**
     * Whether text grouping (spanning across tiles) is enabled.
     * When disabled, each tile renders text independently.
     */
    public static boolean isMapTextGroupingEnabled() {
        if (pluginConfig == null) return false; // Default off
        return pluginConfig.isMapTextGrouping();
    }

    /**
     * Gets the claim group info for text spanning.
     * Finds the maximal RECTANGLE of same-owner claims that contains this chunk.
     * 
     * Algorithm: Iteratively expand outward from current chunk until stable.
     * Then verify all chunks in the rectangle compute the SAME rectangle.
     * This prevents issues with L-shaped or irregular claims where different
     * chunks would compute overlapping but different rectangles.
     * 
     * @param worldName The world name
     * @param chunkX The chunk X coordinate
     * @param chunkZ The chunk Z coordinate
     * @return int[4] = {groupWidth, groupHeight, posX, posZ} or null if not claimed
     *         posX/posZ are 0-indexed position of this chunk within the rectangle
     */
    public static int[] getClaimGroupInfo(String worldName, int chunkX, int chunkZ) {
        UUID owner = getClaimOwner(worldName, chunkX, chunkZ);
        if (owner == null) return null;

        int maxExpand = 8; // Allow larger groups
        
        // Compute the rectangle from this chunk's perspective
        int[] rect = computeRectangle(worldName, chunkX, chunkZ, owner, maxExpand);
        int minX = rect[0], maxX = rect[1], minZ = rect[2], maxZ = rect[3];
        
        int groupWidth = maxX - minX + 1;
        int groupHeight = maxZ - minZ + 1;
        
        // For single chunks, no verification needed
        if (groupWidth == 1 && groupHeight == 1) {
            return new int[]{1, 1, 0, 0};
        }
        
        // Verify all chunks in the rectangle compute the SAME rectangle
        // This prevents issues with L-shaped or irregular claims
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                int[] otherRect = computeRectangle(worldName, x, z, owner, maxExpand);
                if (otherRect[0] != minX || otherRect[1] != maxX || 
                    otherRect[2] != minZ || otherRect[3] != maxZ) {
                    // Inconsistent rectangles - this chunk is at a junction
                    // Fall back to single-chunk rendering
                    return new int[]{1, 1, 0, 0};
                }
            }
        }

        int posX = chunkX - minX;
        int posZ = chunkZ - minZ;

        return new int[]{groupWidth, groupHeight, posX, posZ};
    }
    
    /**
     * Computes the maximal rectangle containing (chunkX, chunkZ) where all chunks
     * are owned by the given owner.
     * 
     * @return int[4] = {minX, maxX, minZ, maxZ}
     */
    private static int[] computeRectangle(String worldName, int chunkX, int chunkZ, UUID owner, int maxExpand) {
        int minX = chunkX, maxX = chunkX;
        int minZ = chunkZ, maxZ = chunkZ;
        
        // Iteratively expand until no changes
        boolean changed = true;
        int iterations = 0;
        int maxIterations = maxExpand * 4; // Safety limit
        
        while (changed && iterations < maxIterations) {
            changed = false;
            iterations++;
            
            // Try expand left - check if full column is owned
            if (minX > chunkX - maxExpand) {
                if (isColumnOwnedBy(worldName, minX - 1, minZ, maxZ, owner)) {
                    minX--;
                    changed = true;
                }
            }
            
            // Try expand right
            if (maxX < chunkX + maxExpand) {
                if (isColumnOwnedBy(worldName, maxX + 1, minZ, maxZ, owner)) {
                    maxX++;
                    changed = true;
                }
            }
            
            // Try expand up
            if (minZ > chunkZ - maxExpand) {
                if (isRowOwnedBy(worldName, minX, maxX, minZ - 1, owner)) {
                    minZ--;
                    changed = true;
                }
            }
            
            // Try expand down
            if (maxZ < chunkZ + maxExpand) {
                if (isRowOwnedBy(worldName, minX, maxX, maxZ + 1, owner)) {
                    maxZ++;
                    changed = true;
                }
            }
        }
        
        return new int[]{minX, maxX, minZ, maxZ};
    }
    
    /**
     * Checks if an entire column (fixed X, range of Z) is owned by the given owner.
     */
    private static boolean isColumnOwnedBy(String worldName, int x, int minZ, int maxZ, UUID owner) {
        for (int z = minZ; z <= maxZ; z++) {
            if (!owner.equals(getClaimOwner(worldName, x, z))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if an entire row (range of X, fixed Z) is owned by the given owner.
     */
    private static boolean isRowOwnedBy(String worldName, int minX, int maxX, int z, UUID owner) {
        for (int x = minX; x <= maxX; x++) {
            if (!owner.equals(getClaimOwner(worldName, x, z))) {
                return false;
            }
        }
        return true;
    }
}
