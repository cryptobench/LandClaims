package com.easyclaims.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.easyclaims.util.ChunkUtil;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages persistent storage of claims using JSON files.
 */
public class ClaimStorage {
    private final Path claimsDirectory;
    private final Path indexFile;
    private final Path namesFile;
    private final Gson gson;
    private final Map<UUID, PlayerClaims> cache;
    private final Map<String, Map<String, UUID>> claimIndex; // world -> (chunkKey -> ownerUUID)
    private final Map<UUID, String> playerNames; // playerId -> username (for map display)

    // Async save infrastructure
    private final ScheduledExecutorService saveExecutor;
    private final AtomicBoolean indexDirty = new AtomicBoolean(false);
    private final AtomicBoolean namesDirty = new AtomicBoolean(false);
    private final Set<UUID> dirtyPlayers = ConcurrentHashMap.newKeySet();

    public ClaimStorage(Path dataDirectory) {
        this.claimsDirectory = dataDirectory.resolve("claims");
        this.indexFile = claimsDirectory.resolve("index.json");
        this.namesFile = claimsDirectory.resolve("names.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.cache = new ConcurrentHashMap<>();
        this.claimIndex = new ConcurrentHashMap<>();
        this.playerNames = new ConcurrentHashMap<>();

        // Initialize async save executor
        this.saveExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "EasyClaims-Storage");
            t.setDaemon(true);
            return t;
        });
        saveExecutor.scheduleAtFixedRate(this::flushDirtyData, 30, 30, TimeUnit.SECONDS);

        try {
            Files.createDirectories(claimsDirectory);
        } catch (IOException e) {
            e.printStackTrace();
        }

        loadIndex();
        loadNames();
    }

    private void loadIndex() {
        if (Files.exists(indexFile)) {
            try {
                String json = Files.readString(indexFile);
                Type type = new TypeToken<Map<String, Map<String, String>>>() {}.getType();
                Map<String, Map<String, String>> loaded = gson.fromJson(json, type);
                if (loaded != null) {
                    for (Map.Entry<String, Map<String, String>> worldEntry : loaded.entrySet()) {
                        String world = worldEntry.getKey();
                        Map<String, UUID> worldClaims = new ConcurrentHashMap<>();
                        for (Map.Entry<String, String> claimEntry : worldEntry.getValue().entrySet()) {
                            try {
                                worldClaims.put(claimEntry.getKey(), UUID.fromString(claimEntry.getValue()));
                            } catch (IllegalArgumentException ignored) {}
                        }
                        claimIndex.put(world, worldClaims);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveIndex() {
        indexDirty.set(true);
    }

    private void saveIndexNow() {
        Map<String, Map<String, String>> toSave = new HashMap<>();
        for (Map.Entry<String, Map<String, UUID>> worldEntry : claimIndex.entrySet()) {
            Map<String, String> worldClaims = new HashMap<>();
            for (Map.Entry<String, UUID> claimEntry : worldEntry.getValue().entrySet()) {
                worldClaims.put(claimEntry.getKey(), claimEntry.getValue().toString());
            }
            toSave.put(worldEntry.getKey(), worldClaims);
        }

        try {
            Files.writeString(indexFile, gson.toJson(toSave));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadNames() {
        if (Files.exists(namesFile)) {
            try {
                String json = Files.readString(namesFile);
                Type type = new TypeToken<Map<String, String>>() {}.getType();
                Map<String, String> loaded = gson.fromJson(json, type);
                if (loaded != null) {
                    for (Map.Entry<String, String> entry : loaded.entrySet()) {
                        try {
                            playerNames.put(UUID.fromString(entry.getKey()), entry.getValue());
                        } catch (IllegalArgumentException ignored) {}
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveNames() {
        namesDirty.set(true);
    }

    private void saveNamesNow() {
        Map<String, String> toSave = new HashMap<>();
        for (Map.Entry<UUID, String> entry : playerNames.entrySet()) {
            toSave.put(entry.getKey().toString(), entry.getValue());
        }
        try {
            Files.writeString(namesFile, gson.toJson(toSave));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets or updates a player's username for map display.
     */
    public void setPlayerName(UUID playerId, String username) {
        if (playerId != null && username != null) {
            playerNames.put(playerId, username);
            saveNames();
        }
    }

    /**
     * Gets a player's stored username for map display.
     * @return the username, or the UUID string if not found
     */
    public String getPlayerName(UUID playerId) {
        if (playerId == null) return "Unknown";
        return playerNames.getOrDefault(playerId, playerId.toString().substring(0, 8));
    }

    /**
     * Gets all claimed chunks in a specific world.
     * @return Map of chunk key ("x,z") to owner UUID
     */
    public Map<String, UUID> getClaimedChunksInWorld(String world) {
        Map<String, UUID> worldClaims = claimIndex.get(world);
        if (worldClaims == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(worldClaims);
    }

    /**
     * Gets all claimed chunks in a specific area.
     * @param world The world name
     * @param minChunkX Minimum chunk X (inclusive)
     * @param maxChunkX Maximum chunk X (inclusive)
     * @param minChunkZ Minimum chunk Z (inclusive)
     * @param maxChunkZ Maximum chunk Z (inclusive)
     * @return Map of chunk coordinates to owner info (UUID, name)
     */
    public Map<long[], ClaimInfo> getClaimsInArea(String world, int minChunkX, int maxChunkX, int minChunkZ, int maxChunkZ) {
        Map<long[], ClaimInfo> result = new HashMap<>();
        Map<String, UUID> worldClaims = claimIndex.get(world);
        if (worldClaims == null) return result;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                String key = ChunkUtil.chunkKey(cx, cz);
                UUID owner = worldClaims.get(key);
                if (owner != null) {
                    String ownerName = getPlayerName(owner);
                    result.put(new long[]{cx, cz}, new ClaimInfo(owner, ownerName));
                }
            }
        }
        return result;
    }

    /**
     * Simple claim info holder for map display.
     */
    public static class ClaimInfo {
        public final UUID ownerId;
        public final String ownerName;

        public ClaimInfo(UUID ownerId, String ownerName) {
            this.ownerId = ownerId;
            this.ownerName = ownerName;
        }
    }

    public PlayerClaims getPlayerClaims(UUID playerId) {
        return cache.computeIfAbsent(playerId, this::loadPlayerClaims);
    }

    private PlayerClaims loadPlayerClaims(UUID playerId) {
        Path file = claimsDirectory.resolve(playerId.toString() + ".json");

        if (Files.exists(file)) {
            try {
                String json = Files.readString(file);
                PlayerClaimsJson data = gson.fromJson(json, PlayerClaimsJson.class);

                PlayerClaims claims = new PlayerClaims(playerId);
                if (data != null) {
                    if (data.claims != null) {
                        for (ClaimJson c : data.claims) {
                            claims.addClaim(new Claim(c.world, c.chunkX, c.chunkZ, c.claimedAt));
                        }
                    }
                    // Newest format: trustedPlayersData (Map<UUID, TrustedPlayerJson>)
                    if (data.trustedPlayersData != null) {
                        for (Map.Entry<String, TrustedPlayerJson> entry : data.trustedPlayersData.entrySet()) {
                            try {
                                UUID trustedId = UUID.fromString(entry.getKey());
                                TrustedPlayerJson tp = entry.getValue();
                                String name = tp.name != null ? tp.name : trustedId.toString();
                                TrustLevel level = TrustLevel.fromString(tp.level);
                                if (level == null) level = TrustLevel.BUILD; // Default to BUILD
                                claims.addTrustedPlayer(trustedId, name, level);
                            } catch (IllegalArgumentException ignored) {}
                        }
                    }
                    // Previous format: trustedPlayersWithNames (Map<UUID, name>)
                    else if (data.trustedPlayersWithNames != null) {
                        for (Map.Entry<String, String> entry : data.trustedPlayersWithNames.entrySet()) {
                            try {
                                UUID trustedId = UUID.fromString(entry.getKey());
                                String name = entry.getValue() != null ? entry.getValue() : trustedId.toString();
                                claims.addTrustedPlayer(trustedId, name, TrustLevel.BUILD);
                            } catch (IllegalArgumentException ignored) {}
                        }
                    }
                    // Oldest format: trustedPlayers (List<UUID>)
                    else if (data.trustedPlayers != null) {
                        for (String trusted : data.trustedPlayers) {
                            try {
                                UUID trustedId = UUID.fromString(trusted);
                                claims.addTrustedPlayer(trustedId, trusted, TrustLevel.BUILD);
                            } catch (IllegalArgumentException ignored) {}
                        }
                    }
                }
                return claims;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return new PlayerClaims(playerId);
    }

    public void savePlayerClaims(UUID playerId) {
        dirtyPlayers.add(playerId);
    }

    private void savePlayerClaimsNow(UUID playerId) {
        PlayerClaims claims = cache.get(playerId);
        if (claims == null) return;

        Path file = claimsDirectory.resolve(playerId.toString() + ".json");

        PlayerClaimsJson data = new PlayerClaimsJson();
        data.claims = new ArrayList<>();
        data.trustedPlayersData = new HashMap<>();

        for (Claim claim : claims.getClaims()) {
            ClaimJson c = new ClaimJson();
            c.world = claim.getWorld();
            c.chunkX = claim.getChunkX();
            c.chunkZ = claim.getChunkZ();
            c.claimedAt = claim.getClaimedAt();
            data.claims.add(c);
        }

        // Save trusted players with their names and trust levels
        for (Map.Entry<UUID, TrustedPlayer> entry : claims.getTrustedPlayersMap().entrySet()) {
            TrustedPlayer tp = entry.getValue();
            TrustedPlayerJson tpj = new TrustedPlayerJson();
            tpj.name = tp.getName();
            tpj.level = tp.getLevel().getKey();
            data.trustedPlayersData.put(entry.getKey().toString(), tpj);
        }

        try {
            Files.writeString(file, gson.toJson(data));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addClaim(UUID playerId, Claim claim) {
        PlayerClaims claims = getPlayerClaims(playerId);
        claims.addClaim(claim);

        // Update index
        String chunkKey = ChunkUtil.chunkKey(claim.getChunkX(), claim.getChunkZ());
        claimIndex.computeIfAbsent(claim.getWorld(), k -> new ConcurrentHashMap<>()).put(chunkKey, playerId);

        savePlayerClaims(playerId);
        saveIndex();
    }

    public void removeClaim(UUID playerId, String world, int chunkX, int chunkZ) {
        PlayerClaims claims = getPlayerClaims(playerId);
        claims.removeClaim(world, chunkX, chunkZ);

        // Update index
        String chunkKey = ChunkUtil.chunkKey(chunkX, chunkZ);
        Map<String, UUID> worldClaims = claimIndex.get(world);
        if (worldClaims != null) {
            worldClaims.remove(chunkKey);
        }

        savePlayerClaims(playerId);
        saveIndex();
    }

    /**
     * Removes all claims for a player.
     */
    public void removeAllClaims(UUID playerId) {
        PlayerClaims claims = getPlayerClaims(playerId);

        // Remove from index
        for (Claim claim : claims.getClaims()) {
            String chunkKey = ChunkUtil.chunkKey(claim.getChunkX(), claim.getChunkZ());
            Map<String, UUID> worldClaims = claimIndex.get(claim.getWorld());
            if (worldClaims != null) {
                worldClaims.remove(chunkKey);
            }
        }

        // Clear claims
        claims.clearAllClaims();

        savePlayerClaims(playerId);
        saveIndex();
    }

    /**
     * Gets the owner of a chunk, or null if unclaimed.
     */
    public UUID getClaimOwner(String world, int chunkX, int chunkZ) {
        Map<String, UUID> worldClaims = claimIndex.get(world);
        if (worldClaims == null) return null;

        String chunkKey = ChunkUtil.chunkKey(chunkX, chunkZ);
        return worldClaims.get(chunkKey);
    }

    /**
     * Checks if a chunk is claimed.
     */
    public boolean isClaimed(String world, int chunkX, int chunkZ) {
        return getClaimOwner(world, chunkX, chunkZ) != null;
    }

    /**
     * Finds claims by OTHER players within a radius of a target chunk.
     * Returns the first found claim owner that isn't the excluded player, or null if none found.
     *
     * @param world The world name
     * @param centerChunkX Center chunk X coordinate
     * @param centerChunkZ Center chunk Z coordinate
     * @param radius Radius in chunks to search (e.g., 2 = check 5x5 area)
     * @param excludePlayerId Player whose claims should be ignored (the one trying to claim)
     * @return UUID of first other player with a claim in range, or null if none
     */
    public UUID findNearbyClaimByOtherPlayer(String world, int centerChunkX, int centerChunkZ,
                                              int radius, UUID excludePlayerId) {
        Map<String, UUID> worldClaims = claimIndex.get(world);
        if (worldClaims == null || radius <= 0) {
            return null;
        }

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int checkX = centerChunkX + dx;
                int checkZ = centerChunkZ + dz;
                String key = ChunkUtil.chunkKey(checkX, checkZ);
                UUID owner = worldClaims.get(key);

                if (owner != null && !owner.equals(excludePlayerId)) {
                    return owner;  // Found a claim by another player
                }
            }
        }
        return null;  // No other player claims in range
    }

    public void saveAll() {
        // Mark everything dirty
        for (UUID playerId : cache.keySet()) {
            dirtyPlayers.add(playerId);
        }
        indexDirty.set(true);
        namesDirty.set(true);
        // Synchronously flush
        flushDirtyData();
    }

    /**
     * Flushes dirty data to disk. Called periodically by background thread
     * and synchronously during shutdown.
     */
    private void flushDirtyData() {
        if (indexDirty.compareAndSet(true, false)) {
            saveIndexNow();
        }
        if (namesDirty.compareAndSet(true, false)) {
            saveNamesNow();
        }
        // Copy and clear to avoid concurrent modification
        for (UUID playerId : dirtyPlayers.toArray(new UUID[0])) {
            dirtyPlayers.remove(playerId);
            savePlayerClaimsNow(playerId);
        }
    }

    /**
     * Shuts down the async save executor and performs final flush.
     */
    public void shutdown() {
        saveExecutor.shutdown();
        try {
            saveExecutor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // Final synchronous flush to ensure all data is saved
        flushDirtyData();
    }

    // JSON data classes
    private static class PlayerClaimsJson {
        List<ClaimJson> claims;
        List<String> trustedPlayers; // Oldest format (v1)
        Map<String, String> trustedPlayersWithNames; // Previous format (v2): UUID -> name
        Map<String, TrustedPlayerJson> trustedPlayersData; // Current format (v3): UUID -> {name, level}
    }

    private static class ClaimJson {
        String world;
        int chunkX;
        int chunkZ;
        long claimedAt;
    }

    private static class TrustedPlayerJson {
        String name;
        String level;
    }
}
