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
 * Grants bonus claim slots to a player (additive).
 * Works for online and offline players.
 * Can be run from console or by a player.
 *
 * Usage: /easyclaims admin grant claims <player> <amount>
 *
 * The bonus slots are added on top of the playtime-based calculation,
 * so they always apply regardless of the max claims cap.
 */
public class GrantClaimsSubcommand extends CommandBase {
    private final EasyClaims plugin;
    private final RequiredArg<String> playerArg;
    private final RequiredArg<Integer> amountArg;

    private static final Color GREEN = new Color(85, 255, 85);
    private static final Color RED = new Color(255, 85, 85);
    private static final Color YELLOW = new Color(255, 255, 85);

    public GrantClaimsSubcommand(EasyClaims plugin) {
        super("claims", "Grant bonus claim slots to a player (additive)");
        this.plugin = plugin;
        this.playerArg = withRequiredArg("player", "Player name or UUID", ArgTypes.STRING);
        this.amountArg = withRequiredArg("amount", "Number of bonus slots to add", ArgTypes.INTEGER);
        requirePermission("easyclaims.admin");
    }

    @Override
    protected void executeSync(CommandContext ctx) {
        String playerInput = playerArg.get(ctx);
        int amount = amountArg.get(ctx);

        if (amount <= 0) {
            ctx.sendMessage(Message.raw("Amount must be positive.").color(RED));
            return;
        }

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

        // Get player claims and add bonus slots
        PlayerClaims claims = plugin.getClaimStorage().getPlayerClaims(targetId);
        int previousBonus = claims.getBonusClaimSlots();
        claims.addBonusClaimSlots(amount);
        int newBonus = claims.getBonusClaimSlots();

        // Save changes
        plugin.getClaimStorage().savePlayerClaims(targetId);

        // Report result
        ctx.sendMessage(Message.raw("Granted " + amount + " bonus claim slots to " + targetName).color(GREEN));
        ctx.sendMessage(Message.raw("Bonus slots: " + previousBonus + " -> " + newBonus).color(YELLOW));
    }
}
