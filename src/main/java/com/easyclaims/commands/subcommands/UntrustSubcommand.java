package com.easyclaims.commands.subcommands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.easyclaims.EasyClaims;
import com.easyclaims.data.PlayerClaims;
import com.easyclaims.data.TrustLevel;
import com.easyclaims.data.TrustedPlayer;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.Map;
import java.util.UUID;

public class UntrustSubcommand extends AbstractPlayerCommand {
    private final EasyClaims plugin;
    private final OptionalArg<String> playerArg;

    private static final Color GREEN = new Color(85, 255, 85);
    private static final Color RED = new Color(255, 85, 85);
    private static final Color YELLOW = new Color(255, 255, 85);

    public UntrustSubcommand(EasyClaims plugin) {
        super("untrust", "Remove trust from a player");
        this.plugin = plugin;
        this.playerArg = withOptionalArg("player", "Player name to untrust", ArgTypes.STRING);
        requirePermission("easyclaims.use");
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
                          @Nonnull Store<EntityStore> store,
                          @Nonnull Ref<EntityStore> playerRef,
                          @Nonnull PlayerRef playerData,
                          @Nonnull World world) {
        String playerInput = playerArg.get(ctx);

        if (playerInput == null || playerInput.isEmpty()) {
            playerData.sendMessage(Message.raw("Usage: /easyclaims untrust <player>").color(RED));
            return;
        }

        PlayerClaims claims = plugin.getClaimManager().getPlayerClaims(playerData.getUuid());
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
        plugin.refreshPlayerClaimChunks(playerData.getUuid());
    }
}
