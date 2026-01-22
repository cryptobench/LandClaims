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
import com.easyclaims.managers.ClaimManager;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.UUID;

public class PlaytimeSubcommand extends AbstractPlayerCommand {
    private final EasyClaims plugin;

    private static final Color GOLD = new Color(255, 170, 0);
    private static final Color GREEN = new Color(85, 255, 85);
    private static final Color YELLOW = new Color(255, 255, 85);
    private static final Color GRAY = new Color(170, 170, 170);
    private static final Color AQUA = new Color(85, 255, 255);

    public PlaytimeSubcommand(EasyClaims plugin) {
        super("playtime", "Show your playtime and claim slots");
        this.plugin = plugin;
        addAliases("stats");
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
                          @Nonnull Store<EntityStore> store,
                          @Nonnull Ref<EntityStore> playerRef,
                          @Nonnull PlayerRef playerData,
                          @Nonnull World world) {
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
}
