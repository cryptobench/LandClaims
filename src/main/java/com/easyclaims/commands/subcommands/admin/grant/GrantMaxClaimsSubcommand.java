package com.easyclaims.commands.subcommands.admin.grant;

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

import java.awt.Color;
import java.util.UUID;

/**
 * Increases a player's maximum claims cap (additive) or sets unlimited.
 * Works for online and offline players.
 * Can be run from console or by a player.
 *
 * Usage:
 *   /easyclaims admin grant maxclaims <player> <amount>   - Add to max claims cap (additive)
 *   /easyclaims admin grant maxclaims <player> unlimited  - Remove the cap entirely
 *
 * The bonus is added to the server's default max claims cap.
 */
public class GrantMaxClaimsSubcommand extends CommandBase {
    private final EasyClaims plugin;
    private final RequiredArg<String> playerArg;
    private final RequiredArg<String> amountArg;

    private static final Color GREEN = new Color(85, 255, 85);
    private static final Color RED = new Color(255, 85, 85);
    private static final Color YELLOW = new Color(255, 255, 85);
    private static final Color GOLD = new Color(255, 170, 0);

    public GrantMaxClaimsSubcommand(EasyClaims plugin) {
        super("maxclaims", "Increase player's max claims cap (additive) or set unlimited");
        this.plugin = plugin;
        this.playerArg = withRequiredArg("player", "Player name or UUID", ArgTypes.STRING);
        this.amountArg = withRequiredArg("amount", "Number to add or 'unlimited'", ArgTypes.STRING);
        requirePermission("easyclaims.admin");
    }

    @Override
    protected void executeSync(CommandContext ctx) {
        String playerInput = playerArg.get(ctx);
        String amountInput = amountArg.get(ctx);

        // Resolve player (online or offline)
        UUID targetId = null;
        String targetName = playerInput;

        // Try 1: Online player lookup by username
        PlayerRef targetPlayer = Universe.get().getPlayerByUsername(playerInput, NameMatching.EXACT_IGNORE_CASE);
        if (targetPlayer != null) {
            targetId = targetPlayer.getUuid();
            targetName = targetPlayer.getUsername();
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

        // Get player claims
        PlayerClaims claims = plugin.getClaimStorage().getPlayerClaims(targetId);

        // Handle "unlimited" case
        if (amountInput.equalsIgnoreCase("unlimited")) {
            claims.setUnlimitedClaims(true);
            plugin.getClaimStorage().savePlayerClaims(targetId);

            ctx.sendMessage(Message.raw("Set unlimited claims for " + targetName).color(GOLD));
            ctx.sendMessage(Message.raw("They can now claim without any limit!").color(YELLOW));
            return;
        }

        // Parse amount
        int amount;
        try {
            amount = Integer.parseInt(amountInput);
        } catch (NumberFormatException e) {
            ctx.sendMessage(Message.raw("Invalid amount: " + amountInput).color(RED));
            ctx.sendMessage(Message.raw("Use a number or 'unlimited'.").color(YELLOW));
            return;
        }

        if (amount <= 0) {
            ctx.sendMessage(Message.raw("Amount must be positive.").color(RED));
            return;
        }

        // If they had unlimited, disable it since we're setting a specific bonus
        if (claims.hasUnlimitedClaims()) {
            claims.setUnlimitedClaims(false);
            ctx.sendMessage(Message.raw("Note: Unlimited flag was disabled.").color(YELLOW));
        }

        // Add bonus max claims (additive)
        int previousBonus = claims.getBonusMaxClaims();
        claims.addBonusMaxClaims(amount);
        int newBonus = claims.getBonusMaxClaims();

        // Save changes
        plugin.getClaimStorage().savePlayerClaims(targetId);

        // Calculate effective max for display
        int serverMax = plugin.getPluginConfig().getMaxClaims();
        int effectiveMax = serverMax + newBonus;

        // Report result
        ctx.sendMessage(Message.raw("Increased max claims for " + targetName + " by " + amount).color(GREEN));
        ctx.sendMessage(Message.raw("Bonus max claims: " + previousBonus + " -> " + newBonus).color(YELLOW));
        ctx.sendMessage(Message.raw("Effective max: " + serverMax + " (server) + " + newBonus + " (bonus) = " + effectiveMax).color(YELLOW));
    }
}
