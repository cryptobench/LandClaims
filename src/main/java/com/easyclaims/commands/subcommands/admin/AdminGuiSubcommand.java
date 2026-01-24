package com.easyclaims.commands.subcommands.admin;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.easyclaims.EasyClaims;
import com.easyclaims.gui.ChunkVisualizerGui;

import javax.annotation.Nonnull;

public class AdminGuiSubcommand extends AbstractPlayerCommand {
    private final EasyClaims plugin;

    public AdminGuiSubcommand(EasyClaims plugin) {
        super("gui", "Open the admin claim manager");
        this.plugin = plugin;
        addAliases("map");
        requirePermission("easyclaims.admin");
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
                          @Nonnull Store<EntityStore> store,
                          @Nonnull Ref<EntityStore> playerRef,
                          @Nonnull PlayerRef playerData,
                          @Nonnull World world) {
        Player player = store.getComponent(playerRef, Player.getComponentType());
        if (player == null) return;

        TransformComponent transform = store.getComponent(playerRef, TransformComponent.getComponentType());
        Vector3d position = transform.getPosition();

        int chunkX = ChunkUtil.chunkCoordinate((int) position.getX());
        int chunkZ = ChunkUtil.chunkCoordinate((int) position.getZ());

        world.execute(() -> {
            player.getPageManager().openCustomPage(playerRef, store,
                    new ChunkVisualizerGui(
                            playerData,
                            world.getName(),
                            chunkX,
                            chunkZ,
                            plugin.getClaimManager(),
                            plugin.getClaimStorage(),
                            true,   // isAdmin - bypass limits
                            true,   // adminClaimMode - create admin claims instead of player claims
                            (worldName) -> plugin.refreshWorldMap(worldName)
                    )
            );
        });
    }
}
