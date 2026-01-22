package com.easyclaims.commands.subcommands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.easyclaims.EasyClaims;
import com.easyclaims.data.PlayerClaims;
import com.easyclaims.data.TrustedPlayer;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.Map;
import java.util.UUID;

public class TrustListSubcommand extends AbstractPlayerCommand {
    private final EasyClaims plugin;

    private static final Color GOLD = new Color(255, 170, 0);
    private static final Color YELLOW = new Color(255, 255, 85);
    private static final Color GRAY = new Color(170, 170, 170);

    public TrustListSubcommand(EasyClaims plugin) {
        super("trustlist", "List all players you've trusted");
        this.plugin = plugin;
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
                          @Nonnull Store<EntityStore> store,
                          @Nonnull Ref<EntityStore> playerRef,
                          @Nonnull PlayerRef playerData,
                          @Nonnull World world) {
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
}
