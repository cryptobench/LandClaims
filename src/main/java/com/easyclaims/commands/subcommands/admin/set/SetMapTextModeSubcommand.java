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
import com.easyclaims.config.PluginConfig.MapTextMode;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Admin command to set the map text rendering mode for claim overlays.
 * Controls how owner names are rendered on claim tiles.
 */
public class SetMapTextModeSubcommand extends AbstractPlayerCommand {
    private final EasyClaims plugin;
    private final RequiredArg<String> valueArg;

    public SetMapTextModeSubcommand(EasyClaims plugin) {
        super("maptextmode", "Set text rendering mode for claim overlays");
        this.plugin = plugin;
        this.valueArg = withRequiredArg("mode", "off/overflow/fit/auto", ArgTypes.STRING);
        addAliases("textmode");
        requirePermission("easyclaims.admin");
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
                          @Nonnull Store<EntityStore> store,
                          @Nonnull Ref<EntityStore> playerRef,
                          @Nonnull PlayerRef playerData,
                          @Nonnull World world) {
        String input = valueArg.get(ctx).toLowerCase().trim();

        MapTextMode mode = MapTextMode.fromString(input);

        // Check if it's a valid input
        if (mode == null) {
            String options = Arrays.stream(MapTextMode.values())
                    .map(m -> m.name().toLowerCase())
                    .collect(Collectors.joining(", "));
            playerData.sendMessage(Message.raw("Invalid mode: " + input + ". Options: " + options).color(Color.RED));
            return;
        }

        // Check if already set
        MapTextMode current = plugin.getPluginConfig().getMapTextMode();
        if (mode == current) {
            playerData.sendMessage(Message.raw("Map text mode is already set to " + mode.name().toLowerCase() + ".").color(Color.YELLOW));
            return;
        }

        plugin.getPluginConfig().setMapTextMode(mode);
        playerData.sendMessage(Message.raw("Map text mode set to: " + mode.name().toLowerCase()).color(Color.GREEN));
        playerData.sendMessage(Message.raw("  " + mode.description).color(Color.GRAY));
        playerData.sendMessage(Message.raw("Refreshing map...").color(Color.GRAY));

        // Refresh all world maps to apply the change
        for (String worldName : EasyClaims.WORLDS.keySet()) {
            plugin.refreshWorldMap(worldName);
        }
    }
}
