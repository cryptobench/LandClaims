package com.easyclaims.map;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.hypixel.hytale.protocol.packets.worldmap.MapMarker;
import com.hypixel.hytale.protocol.packets.worldmap.UpdateWorldMapSettings;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.map.WorldMap;
import com.hypixel.hytale.server.core.universe.world.worldmap.IWorldMap;
import com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapSettings;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;

/**
 * Custom world map implementation that renders claims as colored overlays.
 * Based on SimpleClaims' SimpleClaimsChunkWorldMap.
 * Uses settings compatible with BetterMap's defaults.
 */
public class EasyClaimsChunkWorldMap implements IWorldMap {

    public static final EasyClaimsChunkWorldMap INSTANCE = new EasyClaimsChunkWorldMap();

    @Override
    public WorldMapSettings getWorldMapSettings() {
        // Default settings - BetterMap will override these via reflection if installed
        UpdateWorldMapSettings settingsPacket = new UpdateWorldMapSettings();
        settingsPacket.defaultScale = 128.0F;
        settingsPacket.minScale = 64.0F;
        settingsPacket.maxScale = 128.0F;
        return new WorldMapSettings(null, 3.0F, 2.0F, 3, 32, settingsPacket);
    }

    @Override
    public CompletableFuture<WorldMap> generate(World world, int imageWidth, int imageHeight, LongSet chunksToGenerate) {
        @SuppressWarnings("unchecked")
        CompletableFuture<ClaimImageBuilder>[] futures = new CompletableFuture[chunksToGenerate.size()];
        int futureIndex = 0;

        for (LongIterator iterator = chunksToGenerate.iterator(); iterator.hasNext(); ) {
            long chunkIndex = iterator.nextLong();
            futures[futureIndex++] = ClaimImageBuilder.build(chunkIndex, imageWidth, imageHeight, world);
        }

        return CompletableFuture.allOf(futures).thenApply((unused) -> {
            WorldMap worldMap = new WorldMap(futures.length);

            for (int i = 0; i < futures.length; ++i) {
                ClaimImageBuilder builder = futures[i].getNow(null);
                if (builder != null) {
                    worldMap.getChunks().put(builder.getIndex(), builder.getImage());
                }
            }

            return worldMap;
        });
    }

    @Override
    public CompletableFuture<Map<String, MapMarker>> generatePointsOfInterest(World world) {
        return CompletableFuture.completedFuture(Collections.emptyMap());
    }
}
