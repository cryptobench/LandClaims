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
import com.easyclaims.data.TrustLevel;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.UUID;

public class TrustSubcommand extends AbstractPlayerCommand {
    private final EasyClaims plugin;
    private final OptionalArg<String> playerArg;
    private final OptionalArg<String> levelArg;

    private static final Color GREEN = new Color(85, 255, 85);
    private static final Color RED = new Color(255, 85, 85);
    private static final Color GRAY = new Color(170, 170, 170);

    public TrustSubcommand(EasyClaims plugin) {
        super("trust", "Trust a player to interact with your claims");
        this.plugin = plugin;
        this.playerArg = withOptionalArg("player", "Player name to trust", ArgTypes.STRING);
        this.levelArg = withOptionalArg("level", "Trust level (use/container/workstation/build)", ArgTypes.STRING);
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
                          @Nonnull Store<EntityStore> store,
                          @Nonnull Ref<EntityStore> playerRef,
                          @Nonnull PlayerRef playerData,
                          @Nonnull World world) {
        String playerInput = playerArg.get(ctx);
        String levelInput = levelArg.get(ctx);

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
        plugin.refreshPlayerClaimChunks(playerData.getUuid());
    }
}
