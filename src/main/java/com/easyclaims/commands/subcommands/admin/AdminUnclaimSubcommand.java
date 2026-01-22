package com.easyclaims.commands.subcommands.admin;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.easyclaims.EasyClaims;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.UUID;

public class AdminUnclaimSubcommand extends AbstractPlayerCommand {
    private final EasyClaims plugin;
    private final OptionalArg<String> playerArg;

    private static final Color GREEN = new Color(85, 255, 85);
    private static final Color RED = new Color(255, 85, 85);
    private static final Color YELLOW = new Color(255, 255, 85);

    public AdminUnclaimSubcommand(EasyClaims plugin) {
        super("unclaim", "Remove a claim (current location or all from player)");
        this.plugin = plugin;
        this.playerArg = withOptionalArg("player", "Player name to remove all claims from", ArgTypes.STRING);
        requirePermission("easyclaims.admin");
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
                          @Nonnull Store<EntityStore> store,
                          @Nonnull Ref<EntityStore> playerRef,
                          @Nonnull PlayerRef playerData,
                          @Nonnull World world) {
        String playerInput = playerArg.get(ctx);

        if (playerInput == null || playerInput.isEmpty()) {
            // Unclaim chunk at current location
            TransformComponent transform = store.getComponent(playerRef, TransformComponent.getComponentType());
            Vector3d position = transform.getPosition();
            String worldName = world.getName();

            int chunkX = ChunkUtil.chunkCoordinate((int) position.getX());
            int chunkZ = ChunkUtil.chunkCoordinate((int) position.getZ());

            UUID owner = plugin.getClaimStorage().getClaimOwner(worldName, chunkX, chunkZ);
            if (owner == null) {
                playerData.sendMessage(Message.raw("This chunk is not claimed.").color(YELLOW));
                return;
            }

            String ownerName = plugin.getClaimStorage().getPlayerName(owner);
            plugin.getClaimStorage().removeClaim(owner, worldName, chunkX, chunkZ);
            playerData.sendMessage(Message.raw("Removed claim [" + chunkX + ", " + chunkZ + "] from " + ownerName).color(GREEN));
            plugin.refreshWorldMapChunk(worldName, chunkX, chunkZ);
        } else {
            // Unclaim all chunks from a specific player
            UUID targetId = null;
            String targetName = playerInput;

            PlayerRef targetPlayer = Universe.get().getPlayerByUsername(playerInput, NameMatching.EXACT_IGNORE_CASE);
            if (targetPlayer != null) {
                targetId = targetPlayer.getUuid();
                targetName = targetPlayer.getUsername();
            } else {
                try {
                    targetId = UUID.fromString(playerInput);
                    targetName = plugin.getClaimStorage().getPlayerName(targetId);
                } catch (IllegalArgumentException e) {
                    playerData.sendMessage(Message.raw("Player not found: " + playerInput).color(RED));
                    return;
                }
            }

            int count = plugin.getClaimManager().unclaimAll(targetId);
            if (count > 0) {
                playerData.sendMessage(Message.raw("Removed " + count + " claim(s) from " + targetName).color(GREEN));
                for (String worldName : EasyClaims.WORLDS.keySet()) {
                    plugin.refreshWorldMap(worldName);
                }
            } else {
                playerData.sendMessage(Message.raw(targetName + " doesn't have any claims.").color(YELLOW));
            }
        }
    }
}
