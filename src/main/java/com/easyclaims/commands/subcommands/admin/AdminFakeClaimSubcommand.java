package com.easyclaims.commands.subcommands.admin;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.easyclaims.EasyClaims;
import com.easyclaims.data.Claim;
import com.easyclaims.data.TrustLevel;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.UUID;

public class AdminFakeClaimSubcommand extends AbstractPlayerCommand {
    private final EasyClaims plugin;
    private final OptionalArg<String> actionArg;
    private final OptionalArg<String> levelArg;

    private static final Color GOLD = new Color(255, 170, 0);
    private static final Color GREEN = new Color(85, 255, 85);
    private static final Color RED = new Color(255, 85, 85);
    private static final Color YELLOW = new Color(255, 255, 85);
    private static final Color GRAY = new Color(170, 170, 170);
    private static final Color AQUA = new Color(85, 255, 255);

    // Static fake player UUID for testing (consistent across sessions)
    private static final UUID FAKE_PLAYER_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String FAKE_PLAYER_NAME = "TestPlayer";

    public AdminFakeClaimSubcommand(EasyClaims plugin) {
        super("fakeclaim", "Create a test claim as a fake player");
        this.plugin = plugin;
        this.actionArg = withOptionalArg("action", "Action: trust/untrust/remove", ArgTypes.STRING);
        this.levelArg = withOptionalArg("level", "Trust level for trust action", ArgTypes.STRING);
        requirePermission("easyclaims.admin");
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
                          @Nonnull Store<EntityStore> store,
                          @Nonnull Ref<EntityStore> playerRef,
                          @Nonnull PlayerRef playerData,
                          @Nonnull World world) {
        String subCmd = actionArg.get(ctx);
        String arg1 = levelArg.get(ctx);

        if (subCmd == null) {
            // Claim current chunk as fake player
            TransformComponent transform = store.getComponent(playerRef, TransformComponent.getComponentType());
            Vector3d position = transform.getPosition();
            String worldName = world.getName();

            int chunkX = ChunkUtil.chunkCoordinate((int) position.getX());
            int chunkZ = ChunkUtil.chunkCoordinate((int) position.getZ());

            // Check if already claimed
            UUID existingOwner = plugin.getClaimStorage().getClaimOwner(worldName, chunkX, chunkZ);
            if (existingOwner != null) {
                String ownerName = plugin.getClaimStorage().getPlayerName(existingOwner);
                playerData.sendMessage(Message.raw("Chunk already claimed by " + ownerName).color(RED));
                return;
            }

            // Register fake player name
            plugin.getClaimStorage().setPlayerName(FAKE_PLAYER_UUID, FAKE_PLAYER_NAME);

            // Add claim for fake player
            plugin.getClaimStorage().addClaim(FAKE_PLAYER_UUID, new Claim(worldName, chunkX, chunkZ));
            plugin.refreshWorldMapChunk(worldName, chunkX, chunkZ);

            playerData.sendMessage(Message.raw("=== Fake Claim Created ===").color(GOLD));
            playerData.sendMessage(Message.raw("Chunk [" + chunkX + ", " + chunkZ + "] claimed as " + FAKE_PLAYER_NAME).color(GREEN));
            playerData.sendMessage(Message.raw("You are NOT trusted - try to break/pickup items to test protection!").color(YELLOW));
            playerData.sendMessage(Message.raw("").color(GRAY));
            playerData.sendMessage(Message.raw("Commands:").color(AQUA));
            playerData.sendMessage(Message.raw("  /easyclaims admin fakeclaim trust <level> - Trust yourself").color(GRAY));
            playerData.sendMessage(Message.raw("  /easyclaims admin fakeclaim untrust - Remove your trust").color(GRAY));
            playerData.sendMessage(Message.raw("  /easyclaims admin fakeclaim remove - Remove all fake claims").color(GRAY));

        } else if (subCmd.equalsIgnoreCase("trust")) {
            // Trust the player to fake claims
            TrustLevel level = TrustLevel.BUILD;
            if (arg1 != null && !arg1.isEmpty()) {
                level = TrustLevel.fromString(arg1);
                if (level == null || level == TrustLevel.NONE) {
                    playerData.sendMessage(Message.raw("Invalid level. Use: use, container, workstation, damage, build").color(RED));
                    return;
                }
            }

            plugin.getClaimManager().addTrust(FAKE_PLAYER_UUID, playerData.getUuid(), playerData.getUsername(), level);
            playerData.sendMessage(Message.raw("You now have " + level.getDescription() + " trust in fake claims").color(GREEN));
            plugin.refreshPlayerClaimChunks(FAKE_PLAYER_UUID);

        } else if (subCmd.equalsIgnoreCase("untrust")) {
            // Remove trust
            plugin.getClaimManager().removeTrust(FAKE_PLAYER_UUID, playerData.getUuid());
            playerData.sendMessage(Message.raw("Removed your trust from fake claims - you should be blocked now").color(GREEN));
            plugin.refreshPlayerClaimChunks(FAKE_PLAYER_UUID);

        } else if (subCmd.equalsIgnoreCase("remove")) {
            // Remove all fake claims
            int count = plugin.getClaimManager().unclaimAll(FAKE_PLAYER_UUID);
            if (count > 0) {
                playerData.sendMessage(Message.raw("Removed " + count + " fake claim(s)").color(GREEN));
                for (String worldName : EasyClaims.WORLDS.keySet()) {
                    plugin.refreshWorldMap(worldName);
                }
            } else {
                playerData.sendMessage(Message.raw("No fake claims to remove").color(YELLOW));
            }

        } else {
            playerData.sendMessage(Message.raw("Unknown fakeclaim command: " + subCmd).color(RED));
            playerData.sendMessage(Message.raw("Use: trust <level>, untrust, or remove").color(GRAY));
        }
    }
}
