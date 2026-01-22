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
import com.easyclaims.managers.ClaimManager;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.UUID;

public class ClaimSubcommand extends AbstractPlayerCommand {
    private final EasyClaims plugin;

    private static final Color GREEN = new Color(85, 255, 85);
    private static final Color RED = new Color(255, 85, 85);
    private static final Color YELLOW = new Color(255, 255, 85);
    private static final Color GRAY = new Color(170, 170, 170);

    public ClaimSubcommand(EasyClaims plugin) {
        super("claim", "Claim the chunk you're standing in");
        this.plugin = plugin;
        requirePermission("easyclaims.use");
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
                          @Nonnull Store<EntityStore> store,
                          @Nonnull Ref<EntityStore> playerRef,
                          @Nonnull PlayerRef playerData,
                          @Nonnull World world) {
        UUID playerId = playerData.getUuid();
        String worldName = world.getName();

        TransformComponent transform = store.getComponent(playerRef, TransformComponent.getComponentType());
        Vector3d position = transform.getPosition();

        int chunkX = ChunkUtil.chunkCoordinate((int) position.getX());
        int chunkZ = ChunkUtil.chunkCoordinate((int) position.getZ());

        ClaimManager claimManager = plugin.getClaimManager();
        int currentClaims = claimManager.getPlayerClaims(playerId).getClaimCount();
        int maxClaims = claimManager.getMaxClaims(playerId);

        ClaimManager.ClaimResult result = claimManager.claimChunk(playerId, worldName, position.getX(), position.getZ());

        switch (result) {
            case SUCCESS:
                playerData.sendMessage(Message.raw("Claimed chunk [" + chunkX + ", " + chunkZ + "]").color(GREEN));
                playerData.sendMessage(Message.raw("Claims: " + (currentClaims + 1) + "/" + maxClaims).color(GRAY));
                plugin.refreshWorldMapChunk(worldName, chunkX, chunkZ);
                break;
            case ALREADY_OWN:
                playerData.sendMessage(Message.raw("You already own this chunk!").color(YELLOW));
                break;
            case CLAIMED_BY_OTHER:
                UUID owner = claimManager.getOwnerAt(worldName, position.getX(), position.getZ());
                String ownerName = owner != null ? plugin.getClaimStorage().getPlayerName(owner) : "Unknown";
                playerData.sendMessage(Message.raw("This chunk is claimed by " + ownerName).color(RED));
                break;
            case LIMIT_REACHED:
                playerData.sendMessage(Message.raw("Claim limit reached! (" + currentClaims + "/" + maxClaims + ")").color(RED));
                playerData.sendMessage(Message.raw("Play more to earn additional claims.").color(GRAY));
                break;
            case TOO_CLOSE_TO_OTHER_CLAIM:
                playerData.sendMessage(Message.raw("Cannot claim here - too close to another player's claim!").color(RED));
                int buffer = plugin.getPluginConfig().getClaimBufferSize();
                playerData.sendMessage(Message.raw("Claims must be at least " + buffer + " chunks away from other players.").color(GRAY));
                break;
        }
    }
}
