package com.easyclaims.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds all claims and trusted players for a single player.
 */
public class PlayerClaims {
    private final UUID owner;
    private final List<Claim> claims;
    // Maps trusted player UUID to their TrustedPlayer data
    private final Map<UUID, TrustedPlayer> trustedPlayers;

    public PlayerClaims(UUID owner) {
        this.owner = owner;
        this.claims = Collections.synchronizedList(new ArrayList<>());
        this.trustedPlayers = new ConcurrentHashMap<>();
    }

    public UUID getOwner() {
        return owner;
    }

    public List<Claim> getClaims() {
        synchronized (claims) {
            return new ArrayList<>(claims);  // Safe copy under lock
        }
    }

    public int getClaimCount() {
        return claims.size();
    }

    public void addClaim(Claim claim) {
        synchronized (claims) {
            if (!claims.stream().anyMatch(c -> c.getWorld().equals(claim.getWorld()) &&
                    c.getChunkX() == claim.getChunkX() && c.getChunkZ() == claim.getChunkZ())) {
                claims.add(claim);
            }
        }
    }

    public boolean removeClaim(String world, int chunkX, int chunkZ) {
        synchronized (claims) {
            return claims.removeIf(c -> c.getWorld().equals(world) && c.getChunkX() == chunkX && c.getChunkZ() == chunkZ);
        }
    }

    public boolean hasClaim(String world, int chunkX, int chunkZ) {
        synchronized (claims) {
            return claims.stream().anyMatch(c -> c.getWorld().equals(world) && c.getChunkX() == chunkX && c.getChunkZ() == chunkZ);
        }
    }

    public void clearAllClaims() {
        synchronized (claims) {
            claims.clear();
        }
    }

    public Set<UUID> getTrustedPlayers() {
        return trustedPlayers.keySet();
    }

    /**
     * Gets all trusted players with their data.
     */
    public Map<UUID, TrustedPlayer> getTrustedPlayersMap() {
        return Collections.unmodifiableMap(trustedPlayers);
    }

    /**
     * Gets trusted player names with their trust levels for display.
     * @return Map of UUID -> "name (level)"
     */
    public Map<UUID, String> getTrustedPlayersWithNames() {
        Map<UUID, String> result = new HashMap<>();
        for (Map.Entry<UUID, TrustedPlayer> entry : trustedPlayers.entrySet()) {
            TrustedPlayer tp = entry.getValue();
            result.put(entry.getKey(), tp.getName());
        }
        return result;
    }

    /**
     * Add or update a trusted player with a specific trust level.
     */
    public void addTrustedPlayer(UUID playerId, String playerName, TrustLevel level) {
        // Use compute() for atomic check-and-update to avoid race conditions
        trustedPlayers.compute(playerId, (id, existing) -> {
            if (existing != null) {
                existing.setName(playerName);
                existing.setLevel(level);
                return existing;
            } else {
                return new TrustedPlayer(playerId, playerName, level);
            }
        });
    }

    /**
     * Legacy method for backward compatibility - defaults to BUILD level.
     */
    public void addTrustedPlayer(UUID playerId, String playerName) {
        addTrustedPlayer(playerId, playerName, TrustLevel.BUILD);
    }

    /**
     * Removes a trusted player.
     * @return the removed player's name, or null if not found
     */
    public String removeTrustedPlayer(UUID playerId) {
        TrustedPlayer removed = trustedPlayers.remove(playerId);
        return removed != null ? removed.getName() : null;
    }

    /**
     * Check if a player has any trust level (not NONE).
     */
    public boolean isTrusted(UUID playerId) {
        TrustedPlayer tp = trustedPlayers.get(playerId);
        return tp != null && tp.getLevel() != TrustLevel.NONE;
    }

    /**
     * Gets the trust level for a player.
     * @return the trust level, or NONE if not trusted
     */
    public TrustLevel getTrustLevel(UUID playerId) {
        TrustedPlayer tp = trustedPlayers.get(playerId);
        return tp != null ? tp.getLevel() : TrustLevel.NONE;
    }

    /**
     * Check if a player has at least the given trust level.
     */
    public boolean hasPermission(UUID playerId, TrustLevel required) {
        TrustLevel actual = getTrustLevel(playerId);
        return actual.hasPermission(required);
    }

    /**
     * Gets the TrustedPlayer data for a player.
     */
    public TrustedPlayer getTrustedPlayer(UUID playerId) {
        return trustedPlayers.get(playerId);
    }

    /**
     * Gets the stored username for a trusted player.
     */
    public String getTrustedPlayerName(UUID playerId) {
        TrustedPlayer tp = trustedPlayers.get(playerId);
        return tp != null ? tp.getName() : null;
    }

    /**
     * Finds a trusted player UUID by their stored username (case-insensitive).
     * @return the UUID if found, null otherwise
     */
    public UUID getTrustedPlayerByName(String name) {
        for (Map.Entry<UUID, TrustedPlayer> entry : trustedPlayers.entrySet()) {
            if (entry.getValue().getName() != null && entry.getValue().getName().equalsIgnoreCase(name)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
