package com.easyclaims.commands.subcommands.admin;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.easyclaims.EasyClaims;
import com.easyclaims.config.PluginConfig;

import javax.annotation.Nonnull;
import java.awt.Color;

public class AdminConfigSubcommand extends AbstractPlayerCommand {
    private final EasyClaims plugin;

    private static final Color GOLD = new Color(255, 170, 0);
    private static final Color YELLOW = new Color(255, 255, 85);
    private static final Color GRAY = new Color(170, 170, 170);
    private static final Color AQUA = new Color(85, 255, 255);

    public AdminConfigSubcommand(EasyClaims plugin) {
        super("config", "View current server claim settings");
        this.plugin = plugin;
        requirePermission("easyclaims.admin");
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
                          @Nonnull Store<EntityStore> store,
                          @Nonnull Ref<EntityStore> playerRef,
                          @Nonnull PlayerRef playerData,
                          @Nonnull World world) {
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
}
