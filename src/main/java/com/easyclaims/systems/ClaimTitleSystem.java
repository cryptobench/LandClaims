package com.easyclaims.systems;

import com.easyclaims.data.ClaimStorage;
import com.easyclaims.util.ChunkUtil;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;

import javax.annotation.Nullable;
import java.awt.Color;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ticking system that shows a title banner when players enter or leave claimed zones.
 * Runs every tick for all players and displays a title when the claim status changes.
 */
public class ClaimTitleSystem extends EntityTickingSystem<EntityStore> {

    private static final Message WILDERNESS_MESSAGE = Message.raw("Wilderness").color(new Color(85, 255, 85));
    private static final Message EASY_CLAIMS_MESSAGE = Message.raw("EasyClaims");
    private static final String WILDERNESS_TEXT = "Wilderness";

    private final ClaimStorage claimStorage;
    private final Map<UUID, String> playerLastTitle;

    public ClaimTitleSystem(ClaimStorage claimStorage) {
        this.claimStorage = claimStorage;
        this.playerLastTitle = new ConcurrentHashMap<>();
    }

    @Override
    public void tick(float deltaTime, int index, ArchetypeChunk<EntityStore> archetypeChunk,
                     Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        if (ref == null) return;

        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        Player player = store.getComponent(ref, Player.getComponentType());
        if (playerRef == null || player == null) return;

        // Get player's current position and convert to chunk coordinates
        double posX = playerRef.getTransform().getPosition().getX();
        double posZ = playerRef.getTransform().getPosition().getZ();
        int chunkX = ChunkUtil.toChunkX(posX);
        int chunkZ = ChunkUtil.toChunkZ(posZ);
        String worldName = player.getWorld().getName();

        // Check if this chunk is claimed
        Message titleMessage = WILDERNESS_MESSAGE;
        String titleText = WILDERNESS_TEXT;

        UUID claimOwner = claimStorage.getClaimOwner(worldName, chunkX, chunkZ);
        if (claimOwner != null) {
            String ownerName = claimStorage.getPlayerName(claimOwner);
            titleText = ownerName + "'s Claim";

            // Use different colors for own claims vs others
            if (claimOwner.equals(playerRef.getUuid())) {
                titleMessage = Message.raw("Your Claim").color(new Color(85, 255, 255)); // Cyan for own claim
            } else {
                titleMessage = Message.raw(ownerName + "'s Claim").color(Color.WHITE);
            }
        }

        // Only show title if the claim has changed
        String previousTitle = playerLastTitle.get(playerRef.getUuid());
        if (!titleText.equals(previousTitle)) {
            playerLastTitle.put(playerRef.getUuid(), titleText);
            EventTitleUtil.showEventTitleToPlayer(playerRef, titleMessage, EASY_CLAIMS_MESSAGE,
                    false, null, 2, 0.5f, 0.5f);
        }
    }

    /**
     * Remove player from tracking when they disconnect.
     */
    public void removePlayer(UUID playerId) {
        playerLastTitle.remove(playerId);
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }
}
