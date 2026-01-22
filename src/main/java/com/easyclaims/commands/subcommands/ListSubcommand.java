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
import com.easyclaims.data.Claim;
import com.easyclaims.data.PlayerClaims;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.List;

public class ListSubcommand extends AbstractPlayerCommand {
    private final EasyClaims plugin;

    private static final Color GOLD = new Color(255, 170, 0);
    private static final Color YELLOW = new Color(255, 255, 85);
    private static final Color GRAY = new Color(170, 170, 170);
    private static final Color AQUA = new Color(85, 255, 255);

    public ListSubcommand(EasyClaims plugin) {
        super("list", "List all your claimed chunks");
        this.plugin = plugin;
        addAliases("claims");
        requirePermission("easyclaims.use");
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
                          @Nonnull Store<EntityStore> store,
                          @Nonnull Ref<EntityStore> playerRef,
                          @Nonnull PlayerRef playerData,
                          @Nonnull World world) {
        PlayerClaims playerClaims = plugin.getClaimManager().getPlayerClaims(playerData.getUuid());
        List<Claim> claims = playerClaims.getClaims();

        if (claims.isEmpty()) {
            playerData.sendMessage(Message.raw("You don't have any claims.").color(YELLOW));
            playerData.sendMessage(Message.raw("Use /easyclaims claim to claim land!").color(GRAY));
            return;
        }

        int maxClaims = plugin.getClaimManager().getMaxClaims(playerData.getUuid());
        playerData.sendMessage(Message.raw("Your Claims (" + claims.size() + "/" + maxClaims + "):").color(GOLD));

        for (Claim claim : claims) {
            String marker = claim.getWorld().equals(world.getName()) ? " (current world)" : "";
            playerData.sendMessage(Message.raw("  " + claim.getWorld() + " [" + claim.getChunkX() + ", " + claim.getChunkZ() + "]" + marker).color(AQUA));
        }
    }
}
