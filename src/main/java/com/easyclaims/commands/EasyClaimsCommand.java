package com.easyclaims.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.easyclaims.EasyClaims;
import com.easyclaims.gui.ChunkVisualizerGui;
import com.easyclaims.gui.ClaimSettingsGui;
import com.easyclaims.config.PluginConfig;
import com.easyclaims.data.Claim;
import com.easyclaims.data.PlayerClaims;
import com.easyclaims.data.TrustLevel;
import com.easyclaims.data.TrustedPlayer;
import com.easyclaims.managers.ClaimManager;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Main command for EasyClaims plugin.
 * All functionality is accessed through /easyclaims <subcommand>
 *
 * Subcommands:
 *   gui                      - Open chunk visualizer GUI
 *   settings                 - Manage trusted players GUI
 *   claim                    - Claim current chunk
 *   unclaim                  - Unclaim current chunk
 *   unclaimall               - Unclaim all your chunks
 *   list                     - List all your claims
 *   trust <player> [level]   - Trust a player
 *   untrust <player>         - Remove trust from a player
 *   trustlist                - List trusted players
 *   playtime                 - Show your playtime and claim slots
 *   help                     - Show help
 *   admin config             - Show config (admin)
 *   admin set <key> <value>  - Change config (admin)
 *   admin reload             - Reload config (admin)
 *   admin gui                - Open admin chunk visualizer
 */
public class EasyClaimsCommand extends AbstractPlayerCommand {
    private final EasyClaims plugin;

    private static final Color GOLD = new Color(255, 170, 0);
    private static final Color GREEN = new Color(85, 255, 85);
    private static final Color RED = new Color(255, 85, 85);
    private static final Color GRAY = new Color(170, 170, 170);
    private static final Color AQUA = new Color(85, 255, 255);
    private static final Color YELLOW = new Color(255, 255, 85);

    public EasyClaimsCommand(EasyClaims plugin) {
        super("easyclaims", "Land claiming commands");
        this.plugin = plugin;
        setAllowsExtraArguments(true);  // Allow any number of arguments
        requirePermission("easyclaims.use");
    }

    // Parse arguments from raw input string, skipping the command name itself
    private String[] parseArgs(CommandContext ctx) {
        String input = ctx.getInputString().trim();
        if (input.isEmpty()) {
            return new String[0];
        }
        String[] allArgs = input.split("\\s+");
        // Skip the first argument if it's the command name
        if (allArgs.length > 0 && allArgs[0].equalsIgnoreCase("easyclaims")) {
            String[] args = new String[allArgs.length - 1];
            System.arraycopy(allArgs, 1, args, 0, allArgs.length - 1);
            return args;
        }
        return allArgs;
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
                          @Nonnull Store<EntityStore> store,
                          @Nonnull Ref<EntityStore> playerRef,
                          @Nonnull PlayerRef playerData,
                          @Nonnull World world) {

        String[] args = parseArgs(ctx);

        if (args.length == 0) {
            showHelp(playerData);
            return;
        }

        String subcommand = args[0];
        String arg1 = args.length > 1 ? args[1] : null;
        String arg2 = args.length > 2 ? args[2] : null;

        switch (subcommand.toLowerCase()) {
            case "gui":
            case "map":
                handleGui(playerData, store, playerRef, world, false);
                break;
            case "settings":
                handleSettings(playerData, store, playerRef, world);
                break;
            case "claim":
                handleClaim(playerData, store, playerRef, world);
                break;
            case "unclaim":
                handleUnclaim(playerData, store, playerRef, world);
                break;
            case "unclaimall":
                handleUnclaimAll(playerData);
                break;
            case "list":
            case "claims":
                handleList(playerData, world);
                break;
            case "trust":
                handleTrust(playerData, arg1, arg2);
                break;
            case "untrust":
                handleUntrust(playerData, arg1);
                break;
            case "trustlist":
                handleTrustList(playerData);
                break;
            case "playtime":
                handlePlaytime(playerData);
                break;
            case "help":
                showHelp(playerData);
                break;
            case "admin":
                handleAdmin(playerData, args, store, playerRef, world);
                break;
            default:
                playerData.sendMessage(Message.raw("Unknown subcommand: " + subcommand).color(RED));
                showHelp(playerData);
        }
    }

    // ===== ADMIN =====
    private void handleAdmin(PlayerRef playerData, String[] args, Store<EntityStore> store, Ref<EntityStore> playerRef, World world) {
        if (!hasAdminPermission(playerData)) {
            return;
        }

        // args[0] is "admin", so admin subcommand is args[1]
        if (args.length < 2) {
            showAdminHelp(playerData);
            return;
        }

        String adminSubcmd = args[1];
        String arg1 = args.length > 2 ? args[2] : null;
        String arg2 = args.length > 3 ? args[3] : null;

        switch (adminSubcmd.toLowerCase()) {
            case "config":
                showConfig(playerData);
                break;
            case "set":
                handleSet(playerData, arg1, arg2);
                break;
            case "reload":
                handleReload(playerData);
                break;
            case "unclaim":
                handleAdminUnclaim(playerData, arg1, store, playerRef, world);
                break;
            case "gui":
            case "map":
                handleGui(playerData, store, playerRef, world, true);
                break;
            default:
                playerData.sendMessage(Message.raw("Unknown admin command: " + adminSubcmd).color(RED));
                showAdminHelp(playerData);
        }
    }

    private void showAdminHelp(PlayerRef playerData) {
        playerData.sendMessage(Message.raw("=== EasyClaims Admin Commands ===").color(GOLD));
        playerData.sendMessage(Message.raw("/easyclaims admin gui - Open claim manager (admin mode)").color(GRAY));
        playerData.sendMessage(Message.raw("/easyclaims admin config - Show current settings").color(GRAY));
        playerData.sendMessage(Message.raw("/easyclaims admin set <key> <value> - Change a setting").color(GRAY));
        playerData.sendMessage(Message.raw("/easyclaims admin reload - Reload config from file").color(GRAY));
        playerData.sendMessage(Message.raw("/easyclaims admin unclaim - Remove claim at your location").color(GRAY));
        playerData.sendMessage(Message.raw("/easyclaims admin unclaim <player> - Remove all claims from player").color(GRAY));
        playerData.sendMessage(Message.raw("").color(GRAY));
        playerData.sendMessage(Message.raw("Settings: starting, perhour, max, buffer").color(AQUA));
    }

    // ===== GUI =====
    private void handleGui(PlayerRef playerData, Store<EntityStore> store, Ref<EntityStore> playerRef, World world, boolean isAdmin) {
        Player player = store.getComponent(playerRef, Player.getComponentType());
        if (player == null) return;

        TransformComponent transform = store.getComponent(playerRef, TransformComponent.getComponentType());
        Vector3d position = transform.getPosition();

        int chunkX = ChunkUtil.chunkCoordinate((int) position.getX());
        int chunkZ = ChunkUtil.chunkCoordinate((int) position.getZ());

        // Open the GUI on the world thread
        world.execute(() -> {
            player.getPageManager().openCustomPage(playerRef, store,
                    new ChunkVisualizerGui(
                            playerData,
                            world.getName(),
                            chunkX,
                            chunkZ,
                            plugin.getClaimManager(),
                            plugin.getClaimStorage(),
                            isAdmin,
                            (worldName) -> plugin.refreshWorldMap(worldName)
                    )
            );
        });
    }

    // ===== SETTINGS =====
    private void handleSettings(PlayerRef playerData, Store<EntityStore> store, Ref<EntityStore> playerRef, World world) {
        Player player = store.getComponent(playerRef, Player.getComponentType());
        if (player == null) return;

        // Open the settings GUI on the world thread
        world.execute(() -> {
            player.getPageManager().openCustomPage(playerRef, store,
                    new ClaimSettingsGui(
                            playerData,
                            plugin.getClaimManager(),
                            plugin.getPlaytimeManager()
                    )
            );
        });
    }

    // ===== CLAIM =====
    private void handleClaim(PlayerRef playerData, Store<EntityStore> store, Ref<EntityStore> playerRef, World world) {
        UUID playerId = playerData.getUuid();
        String worldName = world.getName();

        TransformComponent transform = store.getComponent(playerRef, TransformComponent.getComponentType());
        Vector3d position = transform.getPosition();

        int chunkX = ChunkUtil.chunkCoordinate((int) position.getX());
        int chunkZ = ChunkUtil.chunkCoordinate((int) position.getZ());

        ClaimManager claimManager = plugin.getClaimManager();
        int currentClaims = claimManager.getPlayerClaims(playerId).getClaimCount();
        int maxClaims = claimManager.getMaxClaims(playerId);

        ClaimManager.ClaimResult result = claimManager.claimChunk(playerId, worldName, position.getX(), position.getZ());

        switch (result) {
            case SUCCESS:
                playerData.sendMessage(Message.raw("Claimed chunk [" + chunkX + ", " + chunkZ + "]").color(GREEN));
                playerData.sendMessage(Message.raw("Claims: " + (currentClaims + 1) + "/" + maxClaims).color(GRAY));
                plugin.refreshWorldMapChunk(worldName, chunkX, chunkZ);
                break;
            case ALREADY_OWN:
                playerData.sendMessage(Message.raw("You already own this chunk!").color(YELLOW));
                break;
            case CLAIMED_BY_OTHER:
                UUID owner = claimManager.getOwnerAt(worldName, position.getX(), position.getZ());
                String ownerName = owner != null ? plugin.getClaimStorage().getPlayerName(owner) : "Unknown";
                playerData.sendMessage(Message.raw("This chunk is claimed by " + ownerName).color(RED));
                break;
            case LIMIT_REACHED:
                playerData.sendMessage(Message.raw("Claim limit reached! (" + currentClaims + "/" + maxClaims + ")").color(RED));
                playerData.sendMessage(Message.raw("Play more to earn additional claims.").color(GRAY));
                break;
            case TOO_CLOSE_TO_OTHER_CLAIM:
                playerData.sendMessage(Message.raw("Cannot claim here - too close to another player's claim!").color(RED));
                int buffer = plugin.getPluginConfig().getClaimBufferSize();
                playerData.sendMessage(Message.raw("Claims must be at least " + buffer + " chunks away from other players.").color(GRAY));
                break;
        }
    }

    // ===== UNCLAIM =====
    private void handleUnclaim(PlayerRef playerData, Store<EntityStore> store, Ref<EntityStore> playerRef, World world) {
        TransformComponent transform = store.getComponent(playerRef, TransformComponent.getComponentType());
        Vector3d position = transform.getPosition();
        String worldName = world.getName();

        int chunkX = ChunkUtil.chunkCoordinate((int) position.getX());
        int chunkZ = ChunkUtil.chunkCoordinate((int) position.getZ());

        boolean success = plugin.getClaimManager().unclaimChunk(
                playerData.getUuid(), worldName, position.getX(), position.getZ());

        if (success) {
            playerData.sendMessage(Message.raw("Unclaimed chunk [" + chunkX + ", " + chunkZ + "]").color(GREEN));
            plugin.refreshWorldMapChunk(worldName, chunkX, chunkZ);
        } else {
            playerData.sendMessage(Message.raw("This chunk is not your claim!").color(RED));
        }
    }

    // ===== UNCLAIM ALL =====
    private void handleUnclaimAll(PlayerRef playerData) {
        int count = plugin.getClaimManager().unclaimAll(playerData.getUuid());

        if (count > 0) {
            playerData.sendMessage(Message.raw("Removed " + count + " claim(s)").color(GREEN));
            for (String worldName : EasyClaims.WORLDS.keySet()) {
                plugin.refreshWorldMap(worldName);
            }
        } else {
            playerData.sendMessage(Message.raw("You don't have any claims to remove.").color(YELLOW));
        }
    }

    // ===== LIST =====
    private void handleList(PlayerRef playerData, World world) {
        PlayerClaims playerClaims = plugin.getClaimManager().getPlayerClaims(playerData.getUuid());
        List<Claim> claims = playerClaims.getClaims();

        if (claims.isEmpty()) {
            playerData.sendMessage(Message.raw("You don't have any claims.").color(YELLOW));
            playerData.sendMessage(Message.raw("Use /easyclaims claim to claim land!").color(GRAY));
            return;
        }

        int maxClaims = plugin.getClaimManager().getMaxClaims(playerData.getUuid());
        playerData.sendMessage(Message.raw("Your Claims (" + claims.size() + "/" + maxClaims + "):").color(GOLD));

        for (Claim claim : claims) {
            String marker = claim.getWorld().equals(world.getName()) ? " (current world)" : "";
            playerData.sendMessage(Message.raw("  " + claim.getWorld() + " [" + claim.getChunkX() + ", " + claim.getChunkZ() + "]" + marker).color(AQUA));
        }
    }

    // ===== TRUST =====
    private void handleTrust(PlayerRef playerData, String playerInput, String levelInput) {
        if (playerInput == null || playerInput.isEmpty()) {
            playerData.sendMessage(Message.raw("Usage: /easyclaims trust <player> [level]").color(RED));
            playerData.sendMessage(Message.raw("Levels: use, container, workstation, build").color(GRAY));
            return;
        }

        TrustLevel level = TrustLevel.BUILD;
        if (levelInput != null && !levelInput.isEmpty()) {
            level = TrustLevel.fromString(levelInput);
            if (level == null || level == TrustLevel.NONE) {
                playerData.sendMessage(Message.raw("Invalid trust level: " + levelInput).color(RED));
                playerData.sendMessage(Message.raw("Valid levels: use, container, workstation, build").color(GRAY));
                return;
            }
        }

        UUID targetId = null;
        String targetName = playerInput;

        PlayerRef targetPlayer = Universe.get().getPlayerByUsername(playerInput, NameMatching.EXACT_IGNORE_CASE);
        if (targetPlayer != null) {
            targetId = targetPlayer.getUuid();
            targetName = targetPlayer.getUsername();
        } else {
            try {
                targetId = UUID.fromString(playerInput);
            } catch (IllegalArgumentException e) {
                playerData.sendMessage(Message.raw("Player not found: " + playerInput).color(RED));
                return;
            }
        }

        if (targetId.equals(playerData.getUuid())) {
            playerData.sendMessage(Message.raw("You can't trust yourself!").color(RED));
            return;
        }

        plugin.getClaimManager().addTrust(playerData.getUuid(), targetId, targetName, level);
        playerData.sendMessage(Message.raw("Trusted " + targetName + " with " + level.getDescription()).color(GREEN));
    }

    // ===== UNTRUST =====
    private void handleUntrust(PlayerRef playerData, String playerInput) {
        if (playerInput == null || playerInput.isEmpty()) {
            playerData.sendMessage(Message.raw("Usage: /easyclaims untrust <player>").color(RED));
            return;
        }

        PlayerClaims claims = plugin.getClaimManager().getPlayerClaims(playerData.getUuid());
        UUID targetId = null;
        String targetName = playerInput;

        // Try online player first
        PlayerRef targetPlayer = Universe.get().getPlayerByUsername(playerInput, NameMatching.EXACT_IGNORE_CASE);
        if (targetPlayer != null) {
            targetId = targetPlayer.getUuid();
            targetName = targetPlayer.getUsername();
        } else {
            // Try UUID
            try {
                targetId = UUID.fromString(playerInput);
            } catch (IllegalArgumentException e) {
                // Try finding by name in trusted list
                for (Map.Entry<UUID, TrustedPlayer> entry : claims.getTrustedPlayersMap().entrySet()) {
                    if (entry.getValue().getName().equalsIgnoreCase(playerInput)) {
                        targetId = entry.getKey();
                        targetName = entry.getValue().getName();
                        break;
                    }
                }
            }
        }

        if (targetId == null) {
            playerData.sendMessage(Message.raw("Player not found: " + playerInput).color(RED));
            return;
        }

        if (claims.getTrustLevel(targetId) == TrustLevel.NONE) {
            playerData.sendMessage(Message.raw(targetName + " is not trusted.").color(YELLOW));
            return;
        }

        plugin.getClaimManager().removeTrust(playerData.getUuid(), targetId);
        playerData.sendMessage(Message.raw("Removed trust from " + targetName).color(GREEN));
    }

    // ===== TRUST LIST =====
    private void handleTrustList(PlayerRef playerData) {
        PlayerClaims claims = plugin.getClaimManager().getPlayerClaims(playerData.getUuid());
        Map<UUID, TrustedPlayer> trustedPlayers = claims.getTrustedPlayersMap();

        if (trustedPlayers.isEmpty()) {
            playerData.sendMessage(Message.raw("You haven't trusted anyone.").color(YELLOW));
            playerData.sendMessage(Message.raw("Use /easyclaims trust <player> [level]").color(GRAY));
            return;
        }

        playerData.sendMessage(Message.raw("Trusted Players (" + trustedPlayers.size() + "):").color(GOLD));
        for (TrustedPlayer tp : trustedPlayers.values()) {
            playerData.sendMessage(Message.raw("  " + tp.getName() + " [" + tp.getLevel().getDescription() + "]").color(GRAY));
        }
    }

    // ===== PLAYTIME =====
    private void handlePlaytime(PlayerRef playerData) {
        UUID playerId = playerData.getUuid();
        ClaimManager claimManager = plugin.getClaimManager();

        double hours = plugin.getPlaytimeManager().getTotalHours(playerId);
        int currentClaims = claimManager.getPlayerClaims(playerId).getClaimCount();
        int maxClaims = claimManager.getMaxClaims(playerId);

        playerData.sendMessage(Message.raw("=== Your Stats ===").color(GOLD));
        playerData.sendMessage(Message.raw("Playtime: " + String.format("%.1f", hours) + " hours").color(AQUA));
        playerData.sendMessage(Message.raw("Claims: " + currentClaims + "/" + maxClaims + " used").color(AQUA));

        if (currentClaims < maxClaims) {
            playerData.sendMessage(Message.raw("You can claim " + (maxClaims - currentClaims) + " more chunk(s)!").color(GREEN));
        } else {
            double hoursUntilNext = claimManager.getHoursUntilNextClaim(playerId);
            if (hoursUntilNext > 0) {
                playerData.sendMessage(Message.raw("Next claim slot in " + String.format("%.1f", hoursUntilNext) + " hours").color(GRAY));
            } else {
                playerData.sendMessage(Message.raw("Maximum claims reached!").color(YELLOW));
            }
        }
    }

    // ===== HELP =====
    private void showHelp(PlayerRef playerData) {
        playerData.sendMessage(Message.raw("=== EasyClaims Commands ===").color(GOLD));
        playerData.sendMessage(Message.raw("/easyclaims gui - Open claim manager").color(GRAY));
        playerData.sendMessage(Message.raw("/easyclaims settings - Manage trusted players").color(GRAY));
        playerData.sendMessage(Message.raw("/easyclaims claim - Claim current chunk").color(GRAY));
        playerData.sendMessage(Message.raw("/easyclaims unclaim - Unclaim current chunk").color(GRAY));
        playerData.sendMessage(Message.raw("/easyclaims unclaimall - Remove all claims").color(GRAY));
        playerData.sendMessage(Message.raw("/easyclaims list - List your claims").color(GRAY));
        playerData.sendMessage(Message.raw("/easyclaims trust <player> [level] - Trust player").color(GRAY));
        playerData.sendMessage(Message.raw("/easyclaims untrust <player> - Remove trust").color(GRAY));
        playerData.sendMessage(Message.raw("/easyclaims trustlist - List trusted players").color(GRAY));
        playerData.sendMessage(Message.raw("/easyclaims playtime - Show your stats").color(GRAY));
        playerData.sendMessage(Message.raw("").color(GRAY));
        playerData.sendMessage(Message.raw("Trust levels: use, container, workstation, build").color(AQUA));
    }

    // ===== ADMIN: CONFIG =====
    private boolean hasAdminPermission(PlayerRef playerData) {
        if (!PermissionsModule.get().hasPermission(playerData.getUuid(), "easyclaims.admin")) {
            playerData.sendMessage(Message.raw("You don't have permission for this command.").color(RED));
            return false;
        }
        return true;
    }

    private void showConfig(PlayerRef playerData) {
        PluginConfig config = plugin.getPluginConfig();
        playerData.sendMessage(Message.raw("=== Server Claim Settings ===").color(GOLD));
        playerData.sendMessage(Message.raw("").color(GRAY));
        playerData.sendMessage(Message.raw("New players start with: " + config.getStartingClaims() + " claims").color(AQUA));
        playerData.sendMessage(Message.raw("Players earn: " + config.getClaimsPerHour() + " extra claims per hour played").color(AQUA));
        playerData.sendMessage(Message.raw("Maximum claims allowed: " + config.getMaxClaims()).color(AQUA));
        int buffer = config.getClaimBufferSize();
        String bufferText = buffer > 0 ? buffer + " chunks" : "disabled";
        playerData.sendMessage(Message.raw("Claim buffer zone: " + bufferText).color(AQUA));
        playerData.sendMessage(Message.raw("").color(GRAY));
        playerData.sendMessage(Message.raw("To change these settings:").color(GRAY));
        playerData.sendMessage(Message.raw("  /easyclaims admin set starting <number>").color(YELLOW));
        playerData.sendMessage(Message.raw("  /easyclaims admin set perhour <number>").color(YELLOW));
        playerData.sendMessage(Message.raw("  /easyclaims admin set max <number>").color(YELLOW));
        playerData.sendMessage(Message.raw("  /easyclaims admin set buffer <number>").color(YELLOW));
    }

    private void handleSet(PlayerRef playerData, String key, String valueStr) {
        if (key == null || valueStr == null) {
            playerData.sendMessage(Message.raw("How to change settings:").color(GOLD));
            playerData.sendMessage(Message.raw("  /easyclaims admin set starting <number>").color(YELLOW));
            playerData.sendMessage(Message.raw("    How many claims new players start with").color(GRAY));
            playerData.sendMessage(Message.raw("  /easyclaims admin set perhour <number>").color(YELLOW));
            playerData.sendMessage(Message.raw("    Extra claims earned per hour played").color(GRAY));
            playerData.sendMessage(Message.raw("  /easyclaims admin set max <number>").color(YELLOW));
            playerData.sendMessage(Message.raw("    Maximum claims any player can have").color(GRAY));
            playerData.sendMessage(Message.raw("  /easyclaims admin set buffer <number>").color(YELLOW));
            playerData.sendMessage(Message.raw("    Buffer zone in chunks around claims (0 = disabled)").color(GRAY));
            return;
        }

        int value;
        try {
            value = Integer.parseInt(valueStr);
        } catch (NumberFormatException e) {
            playerData.sendMessage(Message.raw("Please enter a number! Example: /easyclaims admin set max 100").color(RED));
            return;
        }

        PluginConfig config = plugin.getPluginConfig();
        switch (key.toLowerCase()) {
            case "starting":
            case "startingclaims":
                config.setStartingClaims(value);
                playerData.sendMessage(Message.raw("New players will now start with " + value + " claims!").color(GREEN));
                break;
            case "perhour":
            case "claimsperhour":
                config.setClaimsPerHour(value);
                playerData.sendMessage(Message.raw("Players will now earn " + value + " claims per hour!").color(GREEN));
                break;
            case "max":
            case "maxclaims":
                config.setMaxClaims(value);
                playerData.sendMessage(Message.raw("Maximum claims is now " + value + "!").color(GREEN));
                break;
            case "buffer":
            case "buffersize":
            case "claimbuffer":
                config.setClaimBufferSize(value);
                if (value == 0) {
                    playerData.sendMessage(Message.raw("Claim buffer zone disabled!").color(GREEN));
                } else {
                    playerData.sendMessage(Message.raw("Claim buffer zone set to " + value + " chunks!").color(GREEN));
                }
                break;
            default:
                playerData.sendMessage(Message.raw("Unknown setting! Try: starting, perhour, max, or buffer").color(RED));
        }
    }

    private void handleReload(PlayerRef playerData) {
        plugin.getPluginConfig().reload();
        playerData.sendMessage(Message.raw("Configuration reloaded!").color(GREEN));
        showConfig(playerData);
    }

    // ===== ADMIN: UNCLAIM =====
    private void handleAdminUnclaim(PlayerRef playerData, String playerInput, Store<EntityStore> store, Ref<EntityStore> playerRef, World world) {
        if (playerInput == null || playerInput.isEmpty()) {
            // Unclaim chunk at current location
            TransformComponent transform = store.getComponent(playerRef, TransformComponent.getComponentType());
            Vector3d position = transform.getPosition();
            String worldName = world.getName();

            int chunkX = ChunkUtil.chunkCoordinate((int) position.getX());
            int chunkZ = ChunkUtil.chunkCoordinate((int) position.getZ());

            UUID owner = plugin.getClaimStorage().getClaimOwner(worldName, chunkX, chunkZ);
            if (owner == null) {
                playerData.sendMessage(Message.raw("This chunk is not claimed.").color(YELLOW));
                return;
            }

            String ownerName = plugin.getClaimStorage().getPlayerName(owner);
            plugin.getClaimStorage().removeClaim(owner, worldName, chunkX, chunkZ);
            playerData.sendMessage(Message.raw("Removed claim [" + chunkX + ", " + chunkZ + "] from " + ownerName).color(GREEN));
            plugin.refreshWorldMapChunk(worldName, chunkX, chunkZ);
        } else {
            // Unclaim all chunks from a specific player
            UUID targetId = null;
            String targetName = playerInput;

            // Try online player first
            PlayerRef targetPlayer = Universe.get().getPlayerByUsername(playerInput, NameMatching.EXACT_IGNORE_CASE);
            if (targetPlayer != null) {
                targetId = targetPlayer.getUuid();
                targetName = targetPlayer.getUsername();
            } else {
                // Try UUID
                try {
                    targetId = UUID.fromString(playerInput);
                    targetName = plugin.getClaimStorage().getPlayerName(targetId);
                } catch (IllegalArgumentException e) {
                    playerData.sendMessage(Message.raw("Player not found: " + playerInput).color(RED));
                    return;
                }
            }

            int count = plugin.getClaimManager().unclaimAll(targetId);
            if (count > 0) {
                playerData.sendMessage(Message.raw("Removed " + count + " claim(s) from " + targetName).color(GREEN));
                for (String worldName : EasyClaims.WORLDS.keySet()) {
                    plugin.refreshWorldMap(worldName);
                }
            } else {
                playerData.sendMessage(Message.raw(targetName + " doesn't have any claims.").color(YELLOW));
            }
        }
    }
}
