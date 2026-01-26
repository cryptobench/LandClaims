package com.easyclaims.commands.subcommands.admin.set;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.easyclaims.EasyClaims;
import com.easyclaims.config.PluginConfig.MapTextScale;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Admin command to set the map text scale for claim overlays.
 * Controls the size of owner/trusted player names on the map.
 */
public class SetMapTextScaleSubcommand extends AbstractPlayerCommand {
    private final EasyClaims plugin;
    private final RequiredArg<String> valueArg;

    public SetMapTextScaleSubcommand(EasyClaims plugin) {
        super("maptextscale", "Set text size for claim owner names on map");
        this.plugin = plugin;
        this.valueArg = withRequiredArg("scale", "off/small/medium/large/auto", ArgTypes.STRING);
        addAliases("textscale", "maptext");
        requirePermission("easyclaims.admin");
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
                          @Nonnull Store<EntityStore> store,
                          @Nonnull Ref<EntityStore> playerRef,
                          @Nonnull PlayerRef playerData,
                          @Nonnull World world) {
        String input = valueArg.get(ctx).toLowerCase().trim();

        MapTextScale scale = MapTextScale.fromString(input);

        // Check if it's a valid input
        if (scale == null) {
            String options = Arrays.stream(MapTextScale.values())
                    .map(s -> s.name().toLowerCase())
                    .collect(Collectors.joining(", "));
            playerData.sendMessage(Message.raw("Invalid scale: " + input + ". Options: " + options).color(Color.RED));
            return;
        }

        // Check if already set
        MapTextScale current = plugin.getPluginConfig().getMapTextScale();
        if (scale == current) {
            playerData.sendMessage(Message.raw("Map text scale is already set to " + scale.name().toLowerCase() + ".").color(Color.YELLOW));
            return;
        }

        plugin.getPluginConfig().setMapTextScale(scale);
        playerData.sendMessage(Message.raw("Map text scale set to: " + scale.name().toLowerCase() + " (" + scale.description + ")").color(Color.GREEN));
        playerData.sendMessage(Message.raw("Refreshing map...").color(Color.GRAY));

        // Refresh all world maps to apply the change
        for (String worldName : EasyClaims.WORLDS.keySet()) {
            plugin.refreshWorldMap(worldName);
        }
    }
}
