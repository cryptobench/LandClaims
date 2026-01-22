package com.easyclaims;

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

    /**
     * Initializes the accessor with the claim storage instance.
     * Called during plugin startup.
     */
    public static void init(ClaimStorage storage) {
        claimStorage = storage;
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
}
