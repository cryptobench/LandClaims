package com.easyclaims.gui;

import com.easyclaims.data.ClaimStorage;
import com.easyclaims.managers.ClaimManager;
import static com.easyclaims.util.ChunkUtil.CHUNK_SIZE;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.util.ColorParseUtil;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Interactive GUI for visualizing and managing chunk claims.
 * Displays a 17x17 grid of chunks centered on the player's current position.
 * Left-click to claim, right-click to unclaim.
 */
public class ChunkVisualizerGui extends InteractiveCustomUIPage<ChunkVisualizerGui.GuiData> {

    private static final Color WILDERNESS_COLOR = new Color(0, 170, 0, 34);
    private static final Color OWN_CLAIM_COLOR = new Color(85, 255, 255, 128);
    private static final Color OTHER_CLAIM_COLOR = new Color(255, 85, 85, 128);
    private static final String GOLD_COLOR = "#93844c";

    private final int centerChunkX;
    private final int centerChunkZ;
    private final String worldName;
    private final ClaimManager claimManager;
    private final ClaimStorage claimStorage;
    private final boolean isAdmin;
    private final Consumer<String> mapRefresher;

    public ChunkVisualizerGui(@Nonnull PlayerRef playerRef, String worldName, int centerChunkX, int centerChunkZ,
                               ClaimManager claimManager, ClaimStorage claimStorage, boolean isAdmin,
                               Consumer<String> mapRefresher) {
        super(playerRef, CustomPageLifetime.CanDismiss, GuiData.CODEC);
        this.worldName = worldName;
        this.centerChunkX = centerChunkX;
        this.centerChunkZ = centerChunkZ;
        this.claimManager = claimManager;
        this.claimStorage = claimStorage;
        this.isAdmin = isAdmin;
        this.mapRefresher = mapRefresher;
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull GuiData data) {
        super.handleDataEvent(ref, store, data);

        if (data.action == null) {
            this.sendUpdate();
            return;
        }

        var playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        var player = store.getComponent(ref, Player.getComponentType());
        if (playerRef == null || player == null) {
            this.sendUpdate();
            return;
        }

        UUID playerId = playerRef.getUuid();
        String[] actions = data.action.split(":");
        String button = actions[0];
        int chunkX = Integer.parseInt(actions[1]);
        int chunkZ = Integer.parseInt(actions[2]);

        if (button.equals("LeftClick")) {
            // Try to claim the chunk
            UUID existingOwner = claimStorage.getClaimOwner(worldName, chunkX, chunkZ);
            if (existingOwner == null) {
                // Convert chunk coords to block coords (center of chunk)
                double blockX = chunkX * CHUNK_SIZE + CHUNK_SIZE / 2.0;
                double blockZ = chunkZ * CHUNK_SIZE + CHUNK_SIZE / 2.0;

                ClaimManager.ClaimResult result = claimManager.claimChunk(playerId, worldName, blockX, blockZ);

                switch (result) {
                    case SUCCESS:
                        player.sendMessage(Message.raw("Chunk claimed!").color(new Color(85, 255, 85)));
                        if (mapRefresher != null) {
                            mapRefresher.accept(worldName);
                        }
                        break;
                    case LIMIT_REACHED:
                        player.sendMessage(Message.raw("You've reached your claim limit! Play more to unlock more claims.").color(new Color(255, 85, 85)));
                        break;
                    case TOO_CLOSE_TO_OTHER_CLAIM:
                        player.sendMessage(Message.raw("Too close to another player's claim!").color(new Color(255, 85, 85)));
                        break;
                    default:
                        break;
                }
            }
        } else if (button.equals("RightClick")) {
            // Try to unclaim the chunk
            UUID owner = claimStorage.getClaimOwner(worldName, chunkX, chunkZ);
            if (owner != null && (owner.equals(playerId) || isAdmin)) {
                // Convert chunk coords to block coords (center of chunk)
                double blockX = chunkX * CHUNK_SIZE + CHUNK_SIZE / 2.0;
                double blockZ = chunkZ * CHUNK_SIZE + CHUNK_SIZE / 2.0;

                if (isAdmin) {
                    // Admin can unclaim any chunk
                    claimStorage.removeClaim(owner, worldName, chunkX, chunkZ);
                    player.sendMessage(Message.raw("Chunk unclaimed (admin)!").color(new Color(255, 255, 85)));
                } else {
                    boolean success = claimManager.unclaimChunk(playerId, worldName, blockX, blockZ);
                    if (success) {
                        player.sendMessage(Message.raw("Chunk unclaimed!").color(new Color(255, 255, 85)));
                    }
                }

                if (mapRefresher != null) {
                    mapRefresher.accept(worldName);
                }
            }
        }

        // Rebuild and send updated UI
        UICommandBuilder commandBuilder = new UICommandBuilder();
        UIEventBuilder eventBuilder = new UIEventBuilder();
        this.build(ref, commandBuilder, eventBuilder, store);
        this.sendUpdate(commandBuilder, eventBuilder, true);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder,
                      @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        uiCommandBuilder.append("Pages/cryptobench_EasyClaims_ChunkVisualizer.ui");

        var playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) return;

        UUID playerId = playerRef.getUuid();

        // Set title for admin mode
        if (isAdmin) {
            uiCommandBuilder.set("#TitleText.Text", "Claim Manager - Admin Mode");
        }

        // Set claim count info
        int currentClaims = claimManager.getPlayerClaims(playerId).getClaimCount();
        int maxClaims = claimManager.getMaxClaims(playerId);
        uiCommandBuilder.set("#ClaimedChunksInfo #ClaimedChunksCount.Text", String.valueOf(currentClaims));
        uiCommandBuilder.set("#ClaimedChunksInfo #MaxChunksCount.Text", String.valueOf(maxClaims));

        // Set next claim time with friendly format
        double hoursUntilNext = claimManager.getHoursUntilNextClaim(playerId);
        String nextClaimText;
        if (currentClaims >= maxClaims && hoursUntilNext > 0) {
            nextClaimText = formatTimeRemaining(hoursUntilNext);
        } else if (currentClaims < maxClaims) {
            nextClaimText = "Available!";
        } else {
            nextClaimText = "Max reached";
        }
        uiCommandBuilder.set("#PlaytimeInfo #NextClaimTime.Text", nextClaimText);

        // Build the 17x17 chunk grid (8 chunks in each direction from center)
        int gridSize = 8;
        for (int z = 0; z <= gridSize * 2; z++) {
            uiCommandBuilder.appendInline("#ChunkCards", "Group { LayoutMode: Left; Anchor: (Bottom: 0); }");

            for (int x = 0; x <= gridSize * 2; x++) {
                uiCommandBuilder.append("#ChunkCards[" + z + "]", "Pages/cryptobench_EasyClaims_ChunkEntry.ui");

                int chunkX = centerChunkX + x - gridSize;
                int chunkZ = centerChunkZ + z - gridSize;

                // Mark center chunk with a "+"
                if (x == gridSize && z == gridSize) {
                    uiCommandBuilder.set("#ChunkCards[" + z + "][" + x + "].Text", "+");
                }

                UUID owner = claimStorage.getClaimOwner(worldName, chunkX, chunkZ);

                if (owner != null) {
                    // Chunk is claimed
                    String ownerName = claimStorage.getPlayerName(owner);
                    Color chunkColor;

                    if (owner.equals(playerId)) {
                        // Player's own claim - cyan
                        chunkColor = OWN_CLAIM_COLOR;
                    } else {
                        // Someone else's claim - red
                        chunkColor = OTHER_CLAIM_COLOR;
                    }

                    uiCommandBuilder.set("#ChunkCards[" + z + "][" + x + "].Background.Color", ColorParseUtil.colorToHexAlpha(chunkColor));
                    uiCommandBuilder.set("#ChunkCards[" + z + "][" + x + "].OutlineColor", ColorParseUtil.colorToHexAlpha(chunkColor));
                    uiCommandBuilder.set("#ChunkCards[" + z + "][" + x + "].OutlineSize", 1);

                    // Build tooltip - use simple text since Message doesn't support append
                    String tooltipText;
                    if (owner.equals(playerId)) {
                        tooltipText = "Your Claim\n\nRight Click to Unclaim";
                    } else {
                        tooltipText = "Owner: " + ownerName;
                        if (isAdmin) {
                            tooltipText += "\n\nRight Click to Unclaim (Admin)";
                        }
                    }

                    uiCommandBuilder.set("#ChunkCards[" + z + "][" + x + "].TooltipTextSpans",
                            Message.raw(tooltipText).color(owner.equals(playerId) ? new Color(85, 255, 255) : Color.WHITE));

                    // Bind right-click for unclaim (only for own chunks or admin)
                    if (owner.equals(playerId) || isAdmin) {
                        uiEventBuilder.addEventBinding(
                                CustomUIEventBindingType.RightClicking,
                                "#ChunkCards[" + z + "][" + x + "]",
                                EventData.of("Action", "RightClick:" + chunkX + ":" + chunkZ)
                        );
                    }
                } else {
                    // Unclaimed wilderness
                    uiCommandBuilder.set("#ChunkCards[" + z + "][" + x + "].TooltipTextSpans",
                            Message.raw("Wilderness\n\nLeft Click to Claim").color(new Color(0, 170, 0)));

                    // Bind left-click for claiming
                    uiEventBuilder.addEventBinding(
                            CustomUIEventBindingType.Activating,
                            "#ChunkCards[" + z + "][" + x + "]",
                            EventData.of("Action", "LeftClick:" + chunkX + ":" + chunkZ)
                    );
                }
            }
        }
    }

    /**
     * Formats hours remaining into a user-friendly string.
     * Examples: "2h 30m", "45m 12s", "30s"
     */
    private String formatTimeRemaining(double hours) {
        long totalSeconds = (long) (hours * 3600);

        if (totalSeconds <= 0) {
            return "Ready!";
        }

        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();

        if (h > 0) {
            sb.append(h).append("h ");
            if (m > 0) {
                sb.append(m).append("m");
            }
        } else if (m > 0) {
            sb.append(m).append("m ");
            if (s > 0) {
                sb.append(s).append("s");
            }
        } else {
            sb.append(s).append("s");
        }

        return sb.toString().trim();
    }

    /**
     * Data class for handling GUI events.
     */
    public static class GuiData {
        private static final String KEY_ACTION = "Action";

        public static final BuilderCodec<GuiData> CODEC = BuilderCodec.<GuiData>builder(GuiData.class, GuiData::new)
                .addField(new KeyedCodec<>(KEY_ACTION, Codec.STRING),
                        (data, s) -> data.action = s,
                        data -> data.action)
                .build();

        private String action;
    }
}
