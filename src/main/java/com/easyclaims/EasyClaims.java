package com.easyclaims;

import com.easyclaims.commands.EasyClaimsCommand;
import com.easyclaims.config.BlockGroups;
import com.easyclaims.config.PluginConfig;
import com.easyclaims.data.ClaimStorage;
import com.easyclaims.data.PlaytimeStorage;
import com.easyclaims.listeners.ClaimProtectionListener;
import com.easyclaims.managers.ClaimManager;
import com.easyclaims.managers.PlaytimeManager;
import com.easyclaims.map.ClaimMapOverlayProvider;
import com.easyclaims.map.EasyClaimsWorldMapProvider;
import com.easyclaims.systems.BlockBreakProtectionSystem;
import com.easyclaims.systems.BlockDamageProtectionSystem;
import com.easyclaims.systems.BlockPlaceProtectionSystem;
import com.easyclaims.systems.BlockUseProtectionSystem;
import com.easyclaims.systems.ClaimTitleSystem;
import com.easyclaims.systems.ItemPickupProtectionSystem;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.events.AddWorldEvent;
import com.hypixel.hytale.server.core.universe.world.events.RemoveWorldEvent;
import com.hypixel.hytale.server.core.universe.world.worldmap.provider.IWorldMapProvider;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.math.util.ChunkUtil;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import java.util.HashMap;
import java.util.Map;

/**
 * EasyClaims - A chunk-based land claiming plugin with playtime-based limits.
 *
 * Features:
 * - Claim chunks to protect your builds
 * - More playtime = more claim chunks available
 * - Trust other players in your claims
 * - Full protection for claimed areas
 */
public class EasyClaims extends JavaPlugin {

    private PluginConfig config;
    private BlockGroups blockGroups;
    private ClaimStorage claimStorage;
    private PlaytimeStorage playtimeStorage;
    private ClaimManager claimManager;
    private PlaytimeManager playtimeManager;
    private ClaimProtectionListener protectionListener;
    private ClaimMapOverlayProvider mapOverlayProvider;
    private ClaimTitleSystem claimTitleSystem;

    // Track registered worlds for map provider
    public static final Map<String, World> WORLDS = new HashMap<>();

    public EasyClaims(JavaPluginInit init) {
        super(init);
    }

    @Override
    public void setup() {
        getLogger().atSevere().log("========== EASYCLAIMS PLUGIN STARTING ==========");

        // Initialize configuration
        config = new PluginConfig(getDataDirectory());
        blockGroups = new BlockGroups(getDataDirectory());

        // Initialize storage
        claimStorage = new ClaimStorage(getDataDirectory());
        playtimeStorage = new PlaytimeStorage(getDataDirectory());

        // Initialize static accessor for map system
        EasyClaimsAccess.init(claimStorage);

        // Initialize managers
        claimManager = new ClaimManager(claimStorage, playtimeStorage, config, blockGroups);
        playtimeManager = new PlaytimeManager(playtimeStorage, config);

        // Register the main command (all functionality under /easyclaims)
        getCommandRegistry().registerCommand(new EasyClaimsCommand(this));

        // Register protection event listeners (for PlayerInteractEvent)
        protectionListener = new ClaimProtectionListener(this);
        protectionListener.register(getEventRegistry());

        // Register player connect/disconnect events for name tracking
        getEventRegistry().registerGlobal(PlayerConnectEvent.class, this::onPlayerConnect);
        getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, this::onPlayerDisconnect);

        // Register world map provider codec
        try {
            IWorldMapProvider.CODEC.register(EasyClaimsWorldMapProvider.ID,
                    EasyClaimsWorldMapProvider.class, EasyClaimsWorldMapProvider.CODEC);
            getLogger().atInfo().log("Registered EasyClaimsWorldMapProvider codec");
        } catch (Exception e) {
            getLogger().atWarning().withCause(e).log("Failed to register world map provider codec");
        }

        // Register world events for map provider setup
        getEventRegistry().registerGlobal(AddWorldEvent.class, this::onWorldAdd);
        getEventRegistry().registerGlobal(RemoveWorldEvent.class, this::onWorldRemove);

        // Initialize map overlay provider (for markers, kept for compatibility)
        mapOverlayProvider = new ClaimMapOverlayProvider(claimStorage, getLogger());

        // Register ECS block protection systems
        getLogger().atInfo().log("Registering ECS block protection systems...");
        try {
            getEntityStoreRegistry().registerSystem(new BlockDamageProtectionSystem(claimManager, getLogger()));
            getEntityStoreRegistry().registerSystem(new BlockBreakProtectionSystem(claimManager, getLogger()));
            getEntityStoreRegistry().registerSystem(new BlockPlaceProtectionSystem(claimManager, getLogger()));
            getEntityStoreRegistry().registerSystem(new BlockUseProtectionSystem(claimManager, getLogger()));
            getEntityStoreRegistry().registerSystem(new ItemPickupProtectionSystem(claimManager, getLogger()));

            // Register claim title system (shows banner when entering/leaving claims)
            claimTitleSystem = new ClaimTitleSystem(claimStorage);
            getEntityStoreRegistry().registerSystem(claimTitleSystem);

            getLogger().atInfo().log("All ECS systems registered successfully!");
        } catch (Exception e) {
            getLogger().atSevere().withCause(e).log("ERROR registering ECS systems");
        }
    }

    /**
     * Called when a world is added - set up our custom map provider.
     */
    private void onWorldAdd(AddWorldEvent event) {
        World world = event.getWorld();
        WORLDS.put(world.getName(), world);
        getLogger().atWarning().log("[Map] World added: %s (deleteOnRemove=%s)", world.getName(), world.getWorldConfig().isDeleteOnRemove());

        // Set our custom world map provider for persistent worlds
        try {
            if (!world.getWorldConfig().isDeleteOnRemove()) {
                world.getWorldConfig().setWorldMapProvider(new EasyClaimsWorldMapProvider());
                getLogger().atWarning().log("[Map] Set EasyClaimsWorldMapProvider for world: %s", world.getName());
            }
        } catch (Exception e) {
            getLogger().atSevere().withCause(e).log("[Map] Failed to set map provider for world: %s", world.getName());
        }
    }

    /**
     * Called when a world is removed.
     */
    private void onWorldRemove(RemoveWorldEvent event) {
        WORLDS.remove(event.getWorld().getName());
    }

    @Override
    public void start() {
        getLogger().atSevere().log("========== EASYCLAIMS PLUGIN STARTED ==========");
        getLogger().atWarning().log("[Map] Known worlds: %s", WORLDS.keySet());
    }

    /**
     * Refreshes the entire world map to show updated claims.
     * Clears both server and client caches to force regeneration.
     * Called after claiming/unclaiming chunks.
     */
    public void refreshWorldMap(String worldName) {
        World world = WORLDS.get(worldName);
        if (world == null) {
            getLogger().atWarning().log("[Map] Cannot refresh map - world not found: %s", worldName);
            return;
        }

        try {
            // 1. Set the generator (in case it changed)
            var worldMap = world.getWorldConfig().getWorldMapProvider().getGenerator(world);
            world.getWorldMapManager().setGenerator(worldMap);

            // 2. Clear server-side cached map images
            world.getWorldMapManager().clearImages();

            // 3. Clear each player's client-side cache to force re-request
            for (Player player : world.getPlayers()) {
                try {
                    player.getWorldMapTracker().clear();
                } catch (Exception e) {
                    getLogger().atFine().withCause(e).log("[Map] Error clearing map for player");
                }
            }

            getLogger().atInfo().log("[Map] Refreshed map for world: %s (%d players notified)",
                    worldName, world.getPlayers().size());
        } catch (Exception e) {
            getLogger().atWarning().withCause(e).log("[Map] Error refreshing map for world: %s", worldName);
        }
    }

    /**
     * Refreshes specific chunks on the world map.
     * More efficient than refreshing the entire map when only a few chunks changed.
     *
     * @param worldName The world name
     * @param chunkX The chunk X coordinate
     * @param chunkZ The chunk Z coordinate
     */
    public void refreshWorldMapChunk(String worldName, int chunkX, int chunkZ) {
        World world = WORLDS.get(worldName);
        if (world == null) {
            return;
        }

        try {
            // Create a set with this chunk and its neighbors (for border updates)
            LongSet chunksToRefresh = new LongOpenHashSet();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    chunksToRefresh.add(ChunkUtil.indexChunk(chunkX + dx, chunkZ + dz));
                }
            }

            // Clear server-side cached images for these chunks
            world.getWorldMapManager().clearImagesInChunks(chunksToRefresh);

            // Clear each player's client-side cache for these chunks
            for (Player player : world.getPlayers()) {
                try {
                    player.getWorldMapTracker().clearChunks(chunksToRefresh);
                } catch (Exception e) {
                    getLogger().atFine().withCause(e).log("[Map] Error clearing chunks for player");
                }
            }

            getLogger().atFine().log("[Map] Refreshed chunk %d,%d in world %s", chunkX, chunkZ, worldName);
        } catch (Exception e) {
            getLogger().atWarning().withCause(e).log("[Map] Error refreshing chunk %d,%d", chunkX, chunkZ);
        }
    }

    @Override
    public void shutdown() {
        // Shutdown playtime manager (saves all sessions)
        if (playtimeManager != null) {
            playtimeManager.shutdown();
        }

        // Shutdown claim storage (flushes pending saves and stops background thread)
        if (claimStorage != null) {
            claimStorage.shutdown();
        }
    }

    public PluginConfig getPluginConfig() {
        return config;
    }

    public ClaimManager getClaimManager() {
        return claimManager;
    }

    public PlaytimeManager getPlaytimeManager() {
        return playtimeManager;
    }

    /**
     * Called when a player joins - start tracking their playtime.
     * Note: This should be hooked into the server's player join event.
     */
    public void onPlayerJoin(java.util.UUID playerId) {
        playtimeManager.onPlayerJoin(playerId);
    }

    /**
     * Called when a player leaves - save their playtime.
     * Note: This should be hooked into the server's player leave event.
     */
    public void onPlayerLeave(java.util.UUID playerId) {
        playtimeManager.onPlayerLeave(playerId);
    }

    /**
     * Handles player connect event - register username and start playtime.
     */
    private void onPlayerConnect(PlayerConnectEvent event) {
        try {
            var playerRef = event.getPlayerRef();
            if (playerRef != null) {
                java.util.UUID playerId = playerRef.getUuid();
                String username = playerRef.getUsername();

                // Store player name for map display
                claimStorage.setPlayerName(playerId, username);

                // Start playtime tracking
                playtimeManager.onPlayerJoin(playerId);

                getLogger().atFine().log("Player connected: %s (%s)", username, playerId);
            }
        } catch (Exception e) {
            getLogger().atWarning().withCause(e).log("Error handling player connect");
        }
    }

    /**
     * Handles player disconnect event - save playtime and clear map cache.
     */
    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        try {
            var playerRef = event.getPlayerRef();
            if (playerRef != null) {
                java.util.UUID playerId = playerRef.getUuid();

                // Save playtime
                playtimeManager.onPlayerLeave(playerId);

                // Clear map overlay cache for this player
                if (mapOverlayProvider != null) {
                    mapOverlayProvider.clearPlayerCache(playerId);
                }

                // Clear title tracking for this player
                if (claimTitleSystem != null) {
                    claimTitleSystem.removePlayer(playerId);
                }

                getLogger().atFine().log("Player disconnected: %s", playerId);
            }
        } catch (Exception e) {
            getLogger().atWarning().withCause(e).log("Error handling player disconnect");
        }
    }

    /**
     * Refreshes all claim chunks for a specific player.
     * Called when trust is added/removed to update the trusted player names on the map.
     *
     * @param playerId The UUID of the claim owner
     */
    public void refreshPlayerClaimChunks(java.util.UUID playerId) {
        var playerClaims = claimStorage.getPlayerClaims(playerId);
        if (playerClaims == null) {
            return;
        }

        // Group claims by world for efficient refresh
        Map<String, java.util.List<int[]>> claimsByWorld = new HashMap<>();
        for (var claim : playerClaims.getClaims()) {
            claimsByWorld.computeIfAbsent(claim.getWorld(), k -> new java.util.ArrayList<>())
                    .add(new int[]{claim.getChunkX(), claim.getChunkZ()});
        }

        // Refresh each world's chunks
        for (var entry : claimsByWorld.entrySet()) {
            String worldName = entry.getKey();
            World world = WORLDS.get(worldName);
            if (world == null) {
                continue;
            }

            try {
                LongSet chunksToRefresh = new LongOpenHashSet();
                for (int[] coords : entry.getValue()) {
                    // Add the claim chunk and its neighbors for border updates
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            chunksToRefresh.add(ChunkUtil.indexChunk(coords[0] + dx, coords[1] + dz));
                        }
                    }
                }

                // Clear server-side cached images
                world.getWorldMapManager().clearImagesInChunks(chunksToRefresh);

                // Clear each player's client-side cache
                for (Player player : world.getPlayers()) {
                    try {
                        player.getWorldMapTracker().clearChunks(chunksToRefresh);
                    } catch (Exception e) {
                        getLogger().atFine().withCause(e).log("[Map] Error clearing chunks for player");
                    }
                }

                getLogger().atFine().log("[Map] Refreshed %d claim chunks for player %s in world %s",
                        entry.getValue().size(), playerId, worldName);
            } catch (Exception e) {
                getLogger().atWarning().withCause(e).log("[Map] Error refreshing claims for player %s", playerId);
            }
        }
    }

    /**
     * Gets the map overlay provider for external access.
     */
    public ClaimMapOverlayProvider getMapOverlayProvider() {
        return mapOverlayProvider;
    }

    /**
     * Gets the claim storage for direct access (e.g., for name updates).
     */
    public ClaimStorage getClaimStorage() {
        return claimStorage;
    }
}
