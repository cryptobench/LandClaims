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
import com.easyclaims.util.Messages;

import javax.annotation.Nonnull;
import java.awt.Color;

/**
 * Admin command to toggle whether claims are shown on the world map.
 * When disabled, the map shows normal terrain without claim overlays.
 * Accepts: true/false, 1/0, on/off, yes/no
 */
public class SetShowClaimsOnMapSubcommand extends AbstractPlayerCommand {
    private final EasyClaims plugin;
    private final RequiredArg<String> valueArg;

    public SetShowClaimsOnMapSubcommand(EasyClaims plugin) {
        super("showclaimsonmap", "Toggle claim overlays on the world map");
        this.plugin = plugin;
        this.valueArg = withRequiredArg("visible", "1/0, true/false, on/off", ArgTypes.STRING);
        addAliases("mapclaims", "claimmap");
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
        boolean currentValue = plugin.getPluginConfig().isShowClaimsOnMap();
        if (value == currentValue) {
            playerData.sendMessage(Message.raw("Claim overlays are already " + (value ? "visible" : "hidden") + ".").color(Color.YELLOW));
            return;
        }

        playerData.sendMessage(Messages.claimMapRefreshing());

        // Toggle claim visibility - this handles provider switching and refresh
        plugin.setClaimMapVisibility(value);

        playerData.sendMessage(Messages.claimMapVisibilityChanged(value));
    }

    /**
     * Parses various boolean representations.
     * @return true, false, or null if invalid
     */
    private Boolean parseBoolean(String input) {
        return switch (input) {
            case "1", "true", "on", "yes", "enable", "enabled", "show" -> true;
            case "0", "false", "off", "no", "disable", "disabled", "hide" -> false;
            default -> null;
        };
    }
}
