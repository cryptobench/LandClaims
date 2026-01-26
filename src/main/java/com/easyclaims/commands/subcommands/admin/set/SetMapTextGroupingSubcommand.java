package com.easyclaims.commands.subcommands.admin.set;

import java.awt.Color;

import javax.annotation.Nonnull;

import com.easyclaims.EasyClaims;
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

/**
 * Admin command to toggle text grouping on the world map.
 * When enabled, text spans across multiple adjacent claim tiles.
 * When disabled (default), each tile renders text independently.
 */
public class SetMapTextGroupingSubcommand extends AbstractPlayerCommand {
    private final EasyClaims plugin;
    private final RequiredArg<String> valueArg;

    public SetMapTextGroupingSubcommand(EasyClaims plugin) {
        super("maptextgrouping", "Toggle text spanning across claim tiles (experimental)");
        this.plugin = plugin;
        this.valueArg = withRequiredArg("enabled", "1/0, true/false, on/off", ArgTypes.STRING);
        addAliases("textgrouping", "grouptext");
        requirePermission("easyclaims.admin");
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
                          @Nonnull Store<EntityStore> store,
                          @Nonnull Ref<EntityStore> playerRef,
                          @Nonnull PlayerRef playerData,
                          @Nonnull World world) {
        String input = valueArg.get(ctx).toLowerCase().trim();

        // Parse various boolean representations
        Boolean value = parseBoolean(input);
        if (value == null) {
            playerData.sendMessage(Message.raw("Invalid value: " + input + ". Use: 1/0, true/false, on/off").color(Color.RED));
            return;
        }

        // Check if value is already set
        boolean currentValue = plugin.getPluginConfig().isMapTextGrouping();
        if (value == currentValue) {
            playerData.sendMessage(Message.raw("Text grouping is already " + (value ? "enabled" : "disabled") + ".").color(Color.YELLOW));
            return;
        }

        plugin.getPluginConfig().setMapTextGrouping(value);
        
        if (value) {
            playerData.sendMessage(Message.raw("Text grouping enabled (experimental). Text will span across adjacent claim tiles.").color(new Color(85, 255, 85)));
        } else {
            playerData.sendMessage(Message.raw("Text grouping disabled. Each tile renders text independently.").color(new Color(85, 255, 85)));
        }

        playerData.sendMessage(Message.raw("Refreshing map...").color(Color.GRAY));

        // Refresh all world maps to apply the change
        for (String worldName : EasyClaims.WORLDS.keySet()) {
            plugin.refreshWorldMap(worldName);
        }
    }

    /**
     * Parses various boolean representations.
     * @return true, false, or null if invalid
     */
    private Boolean parseBoolean(String input) {
        return switch (input) {
            case "1", "true", "on", "yes", "enable", "enabled" -> true;
            case "0", "false", "off", "no", "disable", "disabled" -> false;
            default -> null;
        };
    }
}
