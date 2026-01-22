package com.easyclaims.commands.subcommands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.easyclaims.EasyClaims;

import javax.annotation.Nonnull;
import java.awt.Color;

public class UnclaimSubcommand extends AbstractPlayerCommand {
    private final EasyClaims plugin;

    private static final Color GREEN = new Color(85, 255, 85);
    private static final Color RED = new Color(255, 85, 85);

    public UnclaimSubcommand(EasyClaims plugin) {
        super("unclaim", "Unclaim the chunk you're standing in");
        this.plugin = plugin;
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
                          @Nonnull Store<EntityStore> store,
                          @Nonnull Ref<EntityStore> playerRef,
                          @Nonnull PlayerRef playerData,
                          @Nonnull World world) {
        TransformComponent transform = store.getComponent(playerRef, TransformComponent.getComponentType());
        Vector3d position = transform.getPosition();
        String worldName = world.getName();

        int chunkX = ChunkUtil.chunkCoordinate((int) position.getX());
        int chunkZ = ChunkUtil.chunkCoordinate((int) position.getZ());

        boolean success = plugin.getClaimManager().unclaimChunk(
                playerData.getUuid(), worldName, position.getX(), position.getZ());

        if (success) {
            playerData.sendMessage(Message.raw("Unclaimed chunk [" + chunkX + ", " + chunkZ + "]").color(GREEN));
            plugin.refreshWorldMapChunk(worldName, chunkX, chunkZ);
        } else {
            playerData.sendMessage(Message.raw("This chunk is not your claim!").color(RED));
        }
    }
}
