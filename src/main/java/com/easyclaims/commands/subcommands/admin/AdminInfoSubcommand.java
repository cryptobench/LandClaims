package com.easyclaims.commands.subcommands.admin;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.easyclaims.EasyClaims;
import com.easyclaims.data.PlayerClaims;
import com.easyclaims.data.PlaytimeData;

import java.awt.Color;
import java.util.UUID;

/**
 * Shows detailed claim information for a player.
 * Works for online and offline players.
 * Can be run from console or by a player.
 *
 * Usage: /easyclaims admin info <player>
 *
 * Displays:
 * - Current claims / max claims
 * - Playtime hours
 * - Bonus claim slots (admin-granted)
 * - Bonus max claims (admin-granted)
 * - Unlimited claims status
 */
public class AdminInfoSubcommand extends CommandBase {
    private final EasyClaims plugin;
    private final RequiredArg<String> playerArg;

    private static final Color GREEN = new Color(85, 255, 85);
    private static final Color RED = new Color(255, 85, 85);
    private static final Color YELLOW = new Color(255, 255, 85);
    private static final Color GOLD = new Color(255, 170, 0);
    private static final Color GRAY = new Color(170, 170, 170);
    private static final Color AQUA = new Color(85, 255, 255);

    public AdminInfoSubcommand(EasyClaims plugin) {
        super("info", "View a player's claim statistics and bonuses");
        this.plugin = plugin;
        this.playerArg = withRequiredArg("player", "Player name or UUID", ArgTypes.STRING);
        requirePermission("easyclaims.admin");
    }

    @Override
    protected void executeSync(CommandContext ctx) {
        String playerInput = playerArg.get(ctx);

        // Resolve player (online or offline)
        UUID targetId = null;
        String targetName = playerInput;
        boolean isOnline = false;

        // Try 1: Online player lookup by username
        PlayerRef targetPlayer = Universe.get().getPlayerByUsername(playerInput, NameMatching.EXACT_IGNORE_CASE);
        if (targetPlayer != null) {
            targetId = targetPlayer.getUuid();
            targetName = targetPlayer.getUsername();
            isOnline = true;
        } else {
            // Try 2: Parse as UUID (works for offline players)
            try {
                targetId = UUID.fromString(playerInput);
                String storedName = plugin.getClaimStorage().getPlayerName(targetId);
                if (storedName != null && !storedName.equals(targetId.toString().substring(0, 8))) {
                    targetName = storedName;
                }
            } catch (IllegalArgumentException e) {
                // Try 3: Lookup by username in stored names
                targetId = plugin.getClaimStorage().getPlayerUUID(playerInput);
                if (targetId == null) {
                    ctx.sendMessage(Message.raw("Player not found: " + playerInput).color(RED));
                    ctx.sendMessage(Message.raw("Use a UUID for players who have never claimed.").color(YELLOW));
                    return;
                }
            }
        }

        // Get player data
        PlayerClaims claims = plugin.getClaimStorage().getPlayerClaims(targetId);
        PlaytimeData playtime = plugin.getPlaytimeStorage().getPlaytime(targetId);

        // Calculate stats
        int currentClaims = claims.getClaimCount();
        int maxClaims = plugin.getClaimManager().getMaxClaims(targetId);
        double playtimeHours = playtime.getTotalHoursWithCurrentSession();
        int bonusSlots = claims.getBonusClaimSlots();
        int bonusMax = claims.getBonusMaxClaims();
        boolean unlimited = claims.hasUnlimitedClaims();

        // Server config values
        int serverStarting = plugin.getPluginConfig().getStartingClaims();
        int serverMax = plugin.getPluginConfig().getMaxClaims();
        double claimsPerHour = plugin.getPluginConfig().getClaimsPerHour();

        // Calculate breakdown
        int fromPlaytime = (int) (playtimeHours * claimsPerHour);
        int baseMax = serverStarting + fromPlaytime;
        int cappedMax = Math.min(baseMax, serverMax + bonusMax);

        // Display header
        ctx.sendMessage(Message.raw("=== Claim Info: " + targetName + " ===").color(GOLD));
        String status = isOnline ? " (online)" : " (offline)";
        ctx.sendMessage(Message.raw("UUID: " + targetId.toString() + status).color(GRAY));

        // Display claims
        String claimStatus = currentClaims + " / " + (unlimited ? "unlimited" : String.valueOf(maxClaims));
        ctx.sendMessage(Message.raw("Claims: " + claimStatus).color(currentClaims >= maxClaims && !unlimited ? YELLOW : GREEN));

        // Display playtime
        String playtimeStr = String.format("%.1f hours", playtimeHours);
        ctx.sendMessage(Message.raw("Playtime: " + playtimeStr).color(AQUA));

        // Display max claims calculation breakdown
        ctx.sendMessage(Message.raw("--- Max Claims Breakdown ---").color(GRAY));
        ctx.sendMessage(Message.raw("  Starting claims: " + serverStarting).color(GRAY));
        ctx.sendMessage(Message.raw("  From playtime: +" + fromPlaytime + " (" + String.format("%.1f", playtimeHours) + "h x " + claimsPerHour + "/h)").color(GRAY));
        ctx.sendMessage(Message.raw("  Base total: " + baseMax).color(GRAY));

        if (!unlimited) {
            int effectiveCap = serverMax + bonusMax;
            ctx.sendMessage(Message.raw("  Cap: " + serverMax + " (server)" + (bonusMax > 0 ? " + " + bonusMax + " (bonus)" : "") + " = " + effectiveCap).color(GRAY));
            ctx.sendMessage(Message.raw("  After cap: " + cappedMax).color(GRAY));
        }

        // Display admin-granted bonuses
        ctx.sendMessage(Message.raw("--- Admin Bonuses ---").color(GOLD));

        if (bonusSlots > 0) {
            ctx.sendMessage(Message.raw("  Bonus claim slots: +" + bonusSlots).color(GREEN));
        } else {
            ctx.sendMessage(Message.raw("  Bonus claim slots: 0").color(GRAY));
        }

        if (bonusMax > 0) {
            ctx.sendMessage(Message.raw("  Bonus max claims: +" + bonusMax).color(GREEN));
        } else {
            ctx.sendMessage(Message.raw("  Bonus max claims: 0").color(GRAY));
        }

        if (unlimited) {
            ctx.sendMessage(Message.raw("  Unlimited claims: YES").color(GOLD));
        } else {
            ctx.sendMessage(Message.raw("  Unlimited claims: no").color(GRAY));
        }

        // Final effective max
        if (!unlimited) {
            int effective = cappedMax + bonusSlots;
            ctx.sendMessage(Message.raw("Effective max: " + effective + " (" + cappedMax + " + " + bonusSlots + " bonus slots)").color(AQUA));
        }
    }
}
