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

import javax.annotation.Nonnull;
import java.awt.Color;

public class UnclaimAllSubcommand extends AbstractPlayerCommand {
    private final EasyClaims plugin;

    private static final Color GREEN = new Color(85, 255, 85);
    private static final Color YELLOW = new Color(255, 255, 85);

    public UnclaimAllSubcommand(EasyClaims plugin) {
        super("unclaimall", "Remove all of your claims");
        this.plugin = plugin;
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
                          @Nonnull Store<EntityStore> store,
                          @Nonnull Ref<EntityStore> playerRef,
                          @Nonnull PlayerRef playerData,
                          @Nonnull World world) {
        int count = plugin.getClaimManager().unclaimAll(playerData.getUuid());

        if (count > 0) {
            playerData.sendMessage(Message.raw("Removed " + count + " claim(s)").color(GREEN));
            for (String worldName : EasyClaims.WORLDS.keySet()) {
                plugin.refreshWorldMap(worldName);
            }
        } else {
            playerData.sendMessage(Message.raw("You don't have any claims to remove.").color(YELLOW));
        }
    }
}
