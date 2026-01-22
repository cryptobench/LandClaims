package com.easyclaims.commands.subcommands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.easyclaims.EasyClaims;
import com.easyclaims.gui.ClaimSettingsGui;

import javax.annotation.Nonnull;

public class SettingsSubcommand extends AbstractPlayerCommand {
    private final EasyClaims plugin;

    public SettingsSubcommand(EasyClaims plugin) {
        super("settings", "Manage your trusted players");
        this.plugin = plugin;
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
                          @Nonnull Store<EntityStore> store,
                          @Nonnull Ref<EntityStore> playerRef,
                          @Nonnull PlayerRef playerData,
                          @Nonnull World world) {
        Player player = store.getComponent(playerRef, Player.getComponentType());
        if (player == null) return;

        world.execute(() -> {
            player.getPageManager().openCustomPage(playerRef, store,
                    new ClaimSettingsGui(
                            playerData,
                            plugin.getClaimManager(),
                            plugin.getPlaytimeManager(),
                            (playerId) -> plugin.refreshPlayerClaimChunks(playerId)
                    )
            );
        });
    }
}
