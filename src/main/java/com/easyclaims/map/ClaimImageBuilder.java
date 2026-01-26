package com.easyclaims.map;

import java.awt.Color;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.easyclaims.EasyClaimsAccess;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.packets.worldmap.MapImage;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.environment.config.Environment;
import com.hypixel.hytale.server.core.asset.type.fluid.Fluid;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.ChunkColumn;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.FluidSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

/**
 * Builds map images with claim overlays rendered directly into the terrain.
 * Based on SimpleClaims' CustomImageBuilder.
 */
public class ClaimImageBuilder {
    private final long index;
    private final World world;
    @Nonnull
    private final MapImage image;
    private final int sampleWidth;
    private final int sampleHeight;
    private final int blockStepX;
    private final int blockStepZ;
    @Nonnull
    private final short[] heightSamples;
    @Nonnull
    private final int[] tintSamples;
    @Nonnull
    private final int[] blockSamples;
    @Nonnull
    private final short[] neighborHeightSamples;
    @Nonnull
    private final short[] fluidDepthSamples;
    @Nonnull
    private final int[] environmentSamples;
    @Nonnull
    private final int[] fluidSamples;
    private final MapColor outColor = new MapColor();
    private final UUID[] nearbyOwners = new UUID[4];  // Reusable array for neighboring claim lookups
    @Nullable
    private WorldChunk worldChunk;
    private FluidSection[] fluidSections;

    public ClaimImageBuilder(long index, int imageWidth, int imageHeight, World world) {
        this.index = index;
        this.world = world;
        this.image = new MapImage(imageWidth, imageHeight, new int[imageWidth * imageHeight]);
        this.sampleWidth = Math.min(32, this.image.width);
        this.sampleHeight = Math.min(32, this.image.height);
        this.blockStepX = Math.max(1, 32 / this.image.width);
        this.blockStepZ = Math.max(1, 32 / this.image.height);
        this.heightSamples = new short[this.sampleWidth * this.sampleHeight];
        this.tintSamples = new int[this.sampleWidth * this.sampleHeight];
        this.blockSamples = new int[this.sampleWidth * this.sampleHeight];
        this.neighborHeightSamples = new short[(this.sampleWidth + 2) * (this.sampleHeight + 2)];
        this.fluidDepthSamples = new short[this.sampleWidth * this.sampleHeight];
        this.environmentSamples = new int[this.sampleWidth * this.sampleHeight];
        this.fluidSamples = new int[this.sampleWidth * this.sampleHeight];
    }

    public long getIndex() {
        return this.index;
    }

    @Nonnull
    public MapImage getImage() {
        return this.image;
    }

    @Nonnull
    private CompletableFuture<ClaimImageBuilder> fetchChunk() {
        return this.world.getChunkStore().getChunkReferenceAsync(this.index).thenApplyAsync((ref) -> {
            if (ref != null && ref.isValid()) {
                this.worldChunk = ref.getStore().getComponent(ref, WorldChunk.getComponentType());
                ChunkColumn chunkColumn = ref.getStore().getComponent(ref, ChunkColumn.getComponentType());
                this.fluidSections = new FluidSection[10];

                for (int y = 0; y < 10; ++y) {
                    Ref<ChunkStore> sectionRef = chunkColumn.getSection(y);
                    this.fluidSections[y] = this.world.getChunkStore().getStore().getComponent(sectionRef, FluidSection.getComponentType());
                }

                return this;
            } else {
                return null;
            }
        }, this.world);
    }

    @Nonnull
    private CompletableFuture<ClaimImageBuilder> sampleNeighborsSync() {
        CompletableFuture<Void> north = this.world.getChunkStore().getChunkReferenceAsync(
                ChunkUtil.indexChunk(this.worldChunk.getX(), this.worldChunk.getZ() - 1)).thenAcceptAsync((ref) -> {
            if (ref != null && ref.isValid()) {
                WorldChunk wc = ref.getStore().getComponent(ref, WorldChunk.getComponentType());
                int z = (this.sampleHeight - 1) * this.blockStepZ;
                for (int ix = 0; ix < this.sampleWidth; ++ix) {
                    int x = ix * this.blockStepX;
                    this.neighborHeightSamples[1 + ix] = wc.getHeight(x, z);
                }
            }
        }, this.world);

        CompletableFuture<Void> south = this.world.getChunkStore().getChunkReferenceAsync(
                ChunkUtil.indexChunk(this.worldChunk.getX(), this.worldChunk.getZ() + 1)).thenAcceptAsync((ref) -> {
            if (ref != null && ref.isValid()) {
                WorldChunk wc = ref.getStore().getComponent(ref, WorldChunk.getComponentType());
                int z = 0;
                int neighbourStartIndex = (this.sampleHeight + 1) * (this.sampleWidth + 2) + 1;
                for (int ix = 0; ix < this.sampleWidth; ++ix) {
                    int x = ix * this.blockStepX;
                    this.neighborHeightSamples[neighbourStartIndex + ix] = wc.getHeight(x, z);
                }
            }
        }, this.world);

        CompletableFuture<Void> west = this.world.getChunkStore().getChunkReferenceAsync(
                ChunkUtil.indexChunk(this.worldChunk.getX() - 1, this.worldChunk.getZ())).thenAcceptAsync((ref) -> {
            if (ref != null && ref.isValid()) {
                WorldChunk wc = ref.getStore().getComponent(ref, WorldChunk.getComponentType());
                int x = (this.sampleWidth - 1) * this.blockStepX;
                for (int iz = 0; iz < this.sampleHeight; ++iz) {
                    int z = iz * this.blockStepZ;
                    this.neighborHeightSamples[(iz + 1) * (this.sampleWidth + 2)] = wc.getHeight(x, z);
                }
            }
        }, this.world);

        CompletableFuture<Void> east = this.world.getChunkStore().getChunkReferenceAsync(
                ChunkUtil.indexChunk(this.worldChunk.getX() + 1, this.worldChunk.getZ())).thenAcceptAsync((ref) -> {
            if (ref != null && ref.isValid()) {
                WorldChunk wc = ref.getStore().getComponent(ref, WorldChunk.getComponentType());
                int x = 0;
                for (int iz = 0; iz < this.sampleHeight; ++iz) {
                    int z = iz * this.blockStepZ;
                    this.neighborHeightSamples[(iz + 1) * (this.sampleWidth + 2) + this.sampleWidth + 1] = wc.getHeight(x, z);
                }
            }
        }, this.world);

        CompletableFuture<Void> northeast = this.world.getChunkStore().getChunkReferenceAsync(
                ChunkUtil.indexChunk(this.worldChunk.getX() + 1, this.worldChunk.getZ() - 1)).thenAcceptAsync((ref) -> {
            if (ref != null && ref.isValid()) {
                WorldChunk wc = ref.getStore().getComponent(ref, WorldChunk.getComponentType());
                int x = 0;
                int z = (this.sampleHeight - 1) * this.blockStepZ;
                this.neighborHeightSamples[0] = wc.getHeight(x, z);
            }
        }, this.world);

        CompletableFuture<Void> northwest = this.world.getChunkStore().getChunkReferenceAsync(
                ChunkUtil.indexChunk(this.worldChunk.getX() - 1, this.worldChunk.getZ() - 1)).thenAcceptAsync((ref) -> {
            if (ref != null && ref.isValid()) {
                WorldChunk wc = ref.getStore().getComponent(ref, WorldChunk.getComponentType());
                int x = (this.sampleWidth - 1) * this.blockStepX;
                int z = (this.sampleHeight - 1) * this.blockStepZ;
                this.neighborHeightSamples[this.sampleWidth + 1] = wc.getHeight(x, z);
            }
        }, this.world);

        CompletableFuture<Void> southeast = this.world.getChunkStore().getChunkReferenceAsync(
                ChunkUtil.indexChunk(this.worldChunk.getX() + 1, this.worldChunk.getZ() + 1)).thenAcceptAsync((ref) -> {
            if (ref != null && ref.isValid()) {
                WorldChunk wc = ref.getStore().getComponent(ref, WorldChunk.getComponentType());
                int x = 0;
                int z = 0;
                this.neighborHeightSamples[(this.sampleHeight + 1) * (this.sampleWidth + 2) + this.sampleWidth + 1] = wc.getHeight(x, z);
            }
        }, this.world);

        CompletableFuture<Void> southwest = this.world.getChunkStore().getChunkReferenceAsync(
                ChunkUtil.indexChunk(this.worldChunk.getX() - 1, this.worldChunk.getZ() + 1)).thenAcceptAsync((ref) -> {
            if (ref != null && ref.isValid()) {
                WorldChunk wc = ref.getStore().getComponent(ref, WorldChunk.getComponentType());
                int x = (this.sampleWidth - 1) * this.blockStepX;
                int z = 0;
                this.neighborHeightSamples[(this.sampleHeight + 1) * (this.sampleWidth + 2)] = wc.getHeight(x, z);
            }
        }, this.world);

        return CompletableFuture.allOf(north, south, west, east, northeast, northwest, southeast, southwest)
                .thenApply((v) -> this);
    }

    private ClaimImageBuilder generateImageAsync() {
        // Sample block data
        for (int ix = 0; ix < this.sampleWidth; ++ix) {
            for (int iz = 0; iz < this.sampleHeight; ++iz) {
                int sampleIndex = iz * this.sampleWidth + ix;
                int x = ix * this.blockStepX;
                int z = iz * this.blockStepZ;
                short height = this.worldChunk.getHeight(x, z);
                int tint = this.worldChunk.getTint(x, z);
                this.heightSamples[sampleIndex] = height;
                this.tintSamples[sampleIndex] = tint;
                int blockId = this.worldChunk.getBlock(x, height, z);
                this.blockSamples[sampleIndex] = blockId;

                // Sample fluid data
                int fluidId = 0;
                int fluidTop = 320;
                Fluid fluid = null;
                int chunkYGround = ChunkUtil.chunkCoordinate(height);
                int chunkY = 9;

                label97:
                while (chunkY >= 0 && chunkY >= chunkYGround) {
                    FluidSection fluidSection = this.fluidSections[chunkY];
                    if (fluidSection != null && !fluidSection.isEmpty()) {
                        int minBlockY = Math.max(ChunkUtil.minBlock(chunkY), height);
                        int maxBlockY = ChunkUtil.maxBlock(chunkY);

                        for (int blockY = maxBlockY; blockY >= minBlockY; --blockY) {
                            fluidId = fluidSection.getFluidId(x, blockY, z);
                            if (fluidId != 0) {
                                fluid = Fluid.getAssetMap().getAsset(fluidId);
                                fluidTop = blockY;
                                break label97;
                            }
                        }
                        --chunkY;
                    } else {
                        --chunkY;
                    }
                }

                int fluidBottom;
                label119:
                for (fluidBottom = height; chunkY >= 0 && chunkY >= chunkYGround; --chunkY) {
                    FluidSection fluidSection = this.fluidSections[chunkY];
                    if (fluidSection == null || fluidSection.isEmpty()) {
                        fluidBottom = Math.min(ChunkUtil.maxBlock(chunkY) + 1, fluidTop);
                        break;
                    }

                    int minBlockY = Math.max(ChunkUtil.minBlock(chunkY), height);
                    int maxBlockY = Math.min(ChunkUtil.maxBlock(chunkY), fluidTop - 1);

                    for (int blockY = maxBlockY; blockY >= minBlockY; --blockY) {
                        int nextFluidId = fluidSection.getFluidId(x, blockY, z);
                        if (nextFluidId != fluidId) {
                            Fluid nextFluid = Fluid.getAssetMap().getAsset(nextFluidId);
                            if (!Objects.equals(fluid.getParticleColor(), nextFluid.getParticleColor())) {
                                fluidBottom = blockY + 1;
                                break label119;
                            }
                        }
                    }
                }

                short fluidDepth = fluidId != 0 ? (short) (fluidTop - fluidBottom + 1) : 0;
                int environmentId = this.worldChunk.getBlockChunk().getEnvironment(x, fluidTop, z);
                this.fluidDepthSamples[sampleIndex] = fluidDepth;
                this.environmentSamples[sampleIndex] = environmentId;
                this.fluidSamples[sampleIndex] = fluidId;
            }
        }

        float imageToSampleRatioWidth = (float) this.sampleWidth / (float) this.image.width;
        float imageToSampleRatioHeight = (float) this.sampleHeight / (float) this.image.height;
        int blockPixelWidth = Math.max(1, this.image.width / this.sampleWidth);
        int blockPixelHeight = Math.max(1, this.image.height / this.sampleHeight);

        for (int iz = 0; iz < this.sampleHeight; ++iz) {
            System.arraycopy(this.heightSamples, iz * this.sampleWidth,
                    this.neighborHeightSamples, (iz + 1) * (this.sampleWidth + 2) + 1, this.sampleWidth);
        }

        int chunkX = ChunkUtil.xOfChunkIndex(this.index);
        int chunkZ = ChunkUtil.zOfChunkIndex(this.index);

        // Get claim info for this chunk using the accessor
        String worldName = this.worldChunk.getWorld().getName();

        // Check if claim overlays should be shown
        boolean showClaims = EasyClaimsAccess.shouldShowClaimsOnMap();

        UUID claimOwner = showClaims ? EasyClaimsAccess.getClaimOwner(worldName, chunkX, chunkZ) : null;
        Color claimColor = claimOwner != null ? ClaimColorGenerator.getPlayerColor(claimOwner) : null;

        // Check for admin claims and PvP status
        boolean isAdminClaim = showClaims && EasyClaimsAccess.isAdminClaim(worldName, chunkX, chunkZ);
        boolean pvpDisabled = showClaims && EasyClaimsAccess.isPvPDisabled(worldName, chunkX, chunkZ);

        // Admin claims get a distinct light blue color
        if (isAdminClaim && claimOwner != null) {
            claimColor = new Color(100, 200, 255); // Light blue for admin claims
        }

        // Get neighboring claim owners to determine borders (reuse array to reduce allocations)
        if (showClaims) {
            nearbyOwners[0] = EasyClaimsAccess.getClaimOwner(worldName, chunkX, chunkZ + 1); // SOUTH
            nearbyOwners[1] = EasyClaimsAccess.getClaimOwner(worldName, chunkX, chunkZ - 1); // NORTH
            nearbyOwners[2] = EasyClaimsAccess.getClaimOwner(worldName, chunkX + 1, chunkZ); // EAST
            nearbyOwners[3] = EasyClaimsAccess.getClaimOwner(worldName, chunkX - 1, chunkZ); // WEST
        }

        // Generate the image
        for (int ix = 0; ix < this.image.width; ++ix) {
            for (int iz = 0; iz < this.image.height; ++iz) {
                int sampleX = Math.min((int) ((float) ix * imageToSampleRatioWidth), this.sampleWidth - 1);
                int sampleZ = Math.min((int) ((float) iz * imageToSampleRatioHeight), this.sampleHeight - 1);
                int sampleIndex = sampleZ * this.sampleWidth + sampleX;
                int blockPixelX = ix % blockPixelWidth;
                int blockPixelZ = iz % blockPixelHeight;
                short height = this.heightSamples[sampleIndex];
                int tint = this.tintSamples[sampleIndex];
                int blockId = this.blockSamples[sampleIndex];

                getBlockColor(blockId, tint, this.outColor);

                // Apply lighting/shading
                short north = this.neighborHeightSamples[sampleZ * (this.sampleWidth + 2) + sampleX + 1];
                short south = this.neighborHeightSamples[(sampleZ + 2) * (this.sampleWidth + 2) + sampleX + 1];
                short west = this.neighborHeightSamples[(sampleZ + 1) * (this.sampleWidth + 2) + sampleX];
                short east = this.neighborHeightSamples[(sampleZ + 1) * (this.sampleWidth + 2) + sampleX + 2];
                short northWest = this.neighborHeightSamples[sampleZ * (this.sampleWidth + 2) + sampleX];
                short northEast = this.neighborHeightSamples[sampleZ * (this.sampleWidth + 2) + sampleX + 2];
                short southWest = this.neighborHeightSamples[(sampleZ + 2) * (this.sampleWidth + 2) + sampleX];
                short southEast = this.neighborHeightSamples[(sampleZ + 2) * (this.sampleWidth + 2) + sampleX + 2];

                float shade = shadeFromHeights(blockPixelX, blockPixelZ, blockPixelWidth, blockPixelHeight,
                        height, north, south, west, east, northWest, northEast, southWest, southEast);
                this.outColor.multiply(shade);

                // Apply fluid tinting (water/lava color)
                if (height < 320) {
                    int fluidId = this.fluidSamples[sampleIndex];
                    if (fluidId != 0) {
                        short fluidDepth = this.fluidDepthSamples[sampleIndex];
                        int environmentId = this.environmentSamples[sampleIndex];
                        getFluidColor(fluidId, environmentId, fluidDepth, this.outColor);
                    }
                }

                // Apply claim overlay AFTER fluid tinting so it shows on water/ocean tiles
                if (claimColor != null) {
                    boolean isBorder = false;
                    int borderSize = 2;

                    // Check if this pixel is on a border where the adjacent chunk has a different owner
                    if ((ix <= borderSize && !Objects.equals(claimOwner, nearbyOwners[3])) // WEST border
                            || (ix >= this.image.width - borderSize - 1 && !Objects.equals(claimOwner, nearbyOwners[2])) // EAST border
                            || (iz <= borderSize && !Objects.equals(claimOwner, nearbyOwners[1])) // NORTH border
                            || (iz >= this.image.height - borderSize - 1 && !Objects.equals(claimOwner, nearbyOwners[0]))) { // SOUTH border
                        isBorder = true;
                    }

                    applyClaimColor(claimColor, this.outColor, isBorder);

                    // Apply green tint for PvP-disabled zones (safe areas)
                    if (pvpDisabled) {
                        applyPvPSafeOverlay(this.outColor, isBorder);
                    }
                }

                this.image.data[iz * this.image.width + ix] = this.outColor.pack();
            }
        }

        // Draw owner name and trusted players text on claimed chunks
        if (claimOwner != null) {
            drawClaimText(worldName, chunkX, chunkZ, pvpDisabled);
        }

        return this;
    }

    /**
     * Draws owner name on the map tile.
     * For multi-chunk claims, renders text spanning the full claim area,
     * with each tile rendering its portion of the total text.
     * 
     * Supports multiple rendering modes:
     * - OVERFLOW: Text may extend beyond tile (vanilla-style, for large tiles)
     * - FIT: Text uses full claim space across multiple tiles
     * - AUTO: Automatically choose based on tile size
     *
     * @param pvpDisabled If true, shows "[Safe]" indicator
     */
    private void drawClaimText(String worldName, int chunkX, int chunkZ, boolean pvpDisabled) {
        // Get text mode based on tile size
        com.easyclaims.config.PluginConfig.MapTextMode textMode = 
            EasyClaimsAccess.getMapTextMode(this.image.width, this.image.height);
        
        if (textMode == com.easyclaims.config.PluginConfig.MapTextMode.OFF) {
            return;
        }

        // Use display name for admin claims, otherwise owner name
        String displayName = EasyClaimsAccess.getClaimDisplayName(worldName, chunkX, chunkZ);
        List<String> trustedNames = EasyClaimsAccess.getTrustedPlayerNames(worldName, chunkX, chunkZ);

        if (displayName == null) {
            return;
        }

        // Add "[Safe]" indicator for PvP-disabled zones
        if (pvpDisabled) {
            displayName = displayName + " [Safe]";
        }

        if (textMode == com.easyclaims.config.PluginConfig.MapTextMode.OVERFLOW) {
            // Original behavior: text may extend beyond tile
            drawTextOverflowMode(displayName, trustedNames);
        } else {
            // FIT mode: use full claim space across multiple tiles
            drawTextSpanningMode(worldName, chunkX, chunkZ, displayName);
        }
    }

    /**
     * Draw text in OVERFLOW mode - may extend beyond tile boundaries.
     * Used for large tiles (vanilla map style).
     */
    private void drawTextOverflowMode(String displayName, List<String> trustedNames) {
        int lineHeight = BitmapFont.CHAR_HEIGHT + 2;
        int totalLines = 1 + Math.min(trustedNames.size(), 2);
        int startY = (this.image.height - (totalLines * lineHeight)) / 2;

        // Draw owner/display name
        BitmapFont.drawTextCenteredWithOutline(
            this.image.data, this.image.width, this.image.height,
            displayName, startY,
            BitmapFont.WHITE, BitmapFont.BLACK
        );

        // Draw trusted players (yellow)
        int trustedY = startY + lineHeight;
        int trustedCount = 0;
        for (String trustedName : trustedNames) {
            if (trustedCount >= 2) break;

            BitmapFont.drawTextCenteredWithOutline(
                this.image.data, this.image.width, this.image.height,
                trustedName, trustedY,
                BitmapFont.YELLOW, BitmapFont.BLACK
            );

            trustedY += lineHeight;
            trustedCount++;
        }
    }

    /**
     * Draw text in FIT mode.
     * 
     * When grouping is disabled (default): Simple per-tile rendering.
     * When grouping is enabled: Uses merged group rendering with clipping.
     */
    private void drawTextSpanningMode(String worldName, int chunkX, int chunkZ, String displayName) {
        int tileWidth = this.image.width;
        int tileHeight = this.image.height;
        
        // Skip tiles too small for any text
        if (tileWidth < 12 || tileHeight < 8) {
            return;
        }
        
        int margin = 2;
        int availWidth = tileWidth - margin * 2;
        int availHeight = tileHeight - margin * 2;
        
        // Check if grouping is enabled
        if (!EasyClaimsAccess.isMapTextGroupingEnabled()) {
            // Simple per-tile rendering (default, stable)
            TextFitResult fit = calculateBestFit(displayName, availWidth, availHeight);
            renderTextWithFit(displayName, fit, tileWidth, tileHeight, 0, 0);
            return;
        }
        
        // Grouping enabled - use merged group rendering
        int[] groupInfo = EasyClaimsAccess.getClaimGroupInfo(worldName, chunkX, chunkZ);
        
        if (groupInfo == null) {
            // No group found, render locally
            TextFitResult singleTileFit = calculateBestFit(displayName, availWidth, availHeight);
            renderTextWithFit(displayName, singleTileFit, tileWidth, tileHeight, 0, 0);
            return;
        }
        
        int groupWidth = groupInfo[0];   // Number of tiles horizontally
        int groupHeight = groupInfo[1];  // Number of tiles vertically
        int posX = groupInfo[2];         // This tile's X position in group (0-indexed)
        int posZ = groupInfo[3];         // This tile's Z position in group (0-indexed)
        
        // For 1x1 groups, just render locally
        if (groupWidth == 1 && groupHeight == 1) {
            TextFitResult singleTileFit = calculateBestFit(displayName, availWidth, availHeight);
            renderTextWithFit(displayName, singleTileFit, tileWidth, tileHeight, 0, 0);
            return;
        }
        
        // Multi-tile group - ALL tiles render with clipping
        // Calculate merged area dimensions
        int mergedWidth = groupWidth * tileWidth;
        int mergedHeight = groupHeight * tileHeight;
        int mergedAvailWidth = mergedWidth - margin * 2;
        int mergedAvailHeight = mergedHeight - margin * 2;
        
        // Calculate best fit for the merged area
        TextFitResult mergedFit = calculateBestFit(displayName, mergedAvailWidth, mergedAvailHeight);
        
        // Calculate this tile's offset within the merged area
        int offsetX = posX * tileWidth;
        int offsetZ = posZ * tileHeight;
        renderMergedText(displayName, mergedFit, mergedWidth, mergedHeight, 
                         offsetX, offsetZ, tileWidth, tileHeight);
    }

    /**
     * Result of calculating how well text fits in a given area.
     */
    private static class TextFitResult {
        boolean fitsWell;       // True if text fits without truncation
        boolean useMicro;       // True to use micro font
        boolean vertical;       // True to use vertical orientation
        int lines;              // Number of lines needed
        int score;              // Fit quality score (higher = better)
        
        TextFitResult(boolean fitsWell, boolean useMicro, boolean vertical, int lines, int score) {
            this.fitsWell = fitsWell;
            this.useMicro = useMicro;
            this.vertical = vertical;
            this.lines = lines;
            this.score = score;
        }
    }

    /**
     * Calculate the best text fit for given dimensions.
     * Tries both fonts and both orientations, returns the best option.
     */
    private TextFitResult calculateBestFit(String text, int availWidth, int availHeight) {
        TextFitResult best = null;
        
        // Try standard font horizontal
        TextFitResult stdH = tryFit(text, availWidth, availHeight, false, false);
        if (best == null || stdH.score > best.score) best = stdH;
        
        // Try standard font vertical
        TextFitResult stdV = tryFit(text, availHeight, availWidth, false, true);
        if (stdV.score > best.score) best = stdV;
        
        // Try micro font horizontal
        TextFitResult microH = tryFit(text, availWidth, availHeight, true, false);
        if (microH.score > best.score) best = microH;
        
        // Try micro font vertical  
        TextFitResult microV = tryFit(text, availHeight, availWidth, true, true);
        if (microV.score > best.score) best = microV;
        
        return best;
    }

    /**
     * Try fitting text in given dimensions with specified font.
     * Returns a fit result with quality score.
     */
    private TextFitResult tryFit(String text, int width, int height, boolean useMicro, boolean vertical) {
        int charWidth = useMicro ? BitmapFont.MICRO_CHAR_WIDTH : BitmapFont.CHAR_WIDTH;
        int charSpacing = useMicro ? BitmapFont.MICRO_CHAR_SPACING : BitmapFont.CHAR_SPACING;
        int charHeight = useMicro ? BitmapFont.MICRO_CHAR_HEIGHT : BitmapFont.CHAR_HEIGHT;
        int lineSpacing = useMicro ? 1 : 2;
        
        int fullCharWidth = charWidth + charSpacing;
        int lineHeight = charHeight + lineSpacing;
        
        int textLen = text.length();
        int singleLineWidth = textLen * fullCharWidth - charSpacing;
        
        // Check if single line fits
        if (singleLineWidth <= width && charHeight <= height) {
            // Perfect fit - single line
            int score = 100 + (useMicro ? 0 : 10); // Prefer standard font
            return new TextFitResult(true, useMicro, vertical, 1, score);
        }
        
        // Calculate multiline fit
        int maxCharsPerLine = Math.max(1, width / fullCharWidth);
        int linesNeeded = (int) Math.ceil((double) textLen / maxCharsPerLine);
        int maxLines = Math.max(1, height / lineHeight);
        
        if (linesNeeded <= maxLines) {
            // Fits with multiple lines
            int score = 80 - linesNeeded * 5 + (useMicro ? 0 : 5);
            return new TextFitResult(true, useMicro, vertical, linesNeeded, score);
        }
        
        // Doesn't fit well - will be truncated
        int score = 20 - (linesNeeded - maxLines) * 10 + (useMicro ? 5 : 0); // Prefer micro when truncating
        return new TextFitResult(false, useMicro, vertical, maxLines, Math.max(0, score));
    }

    /**
     * Render text using the calculated fit result.
     * Handles both horizontal and vertical orientations.
     */
    private void renderTextWithFit(String displayName, TextFitResult fit, 
                                    int tileWidth, int tileHeight, int offsetX, int offsetZ) {
        if (fit.vertical) {
            renderVerticalText(displayName, fit, tileWidth, tileHeight, offsetX, offsetZ);
        } else {
            renderHorizontalText(displayName, fit, tileWidth, tileHeight, offsetX, offsetZ);
        }
    }

    /**
     * Render text horizontally (normal orientation).
     */
    private void renderHorizontalText(String displayName, TextFitResult fit,
                                       int tileWidth, int tileHeight, int offsetX, int offsetZ) {
        int margin = 2;
        int availWidth = tileWidth - margin * 2;
        
        int charWidth = fit.useMicro ? BitmapFont.MICRO_CHAR_WIDTH : BitmapFont.CHAR_WIDTH;
        int charSpacing = fit.useMicro ? BitmapFont.MICRO_CHAR_SPACING : BitmapFont.CHAR_SPACING;
        int charHeight = fit.useMicro ? BitmapFont.MICRO_CHAR_HEIGHT : BitmapFont.CHAR_HEIGHT;
        int lineSpacing = fit.useMicro ? 1 : 2;
        int fullCharWidth = charWidth + charSpacing;
        
        // Calculate line splitting
        String[] lines;
        if (fit.lines == 1) {
            lines = new String[]{displayName};
        } else {
            int maxCharsPerLine = Math.max(1, availWidth / fullCharWidth);
            int actualLines = Math.min(fit.lines, (int) Math.ceil((double) displayName.length() / maxCharsPerLine));
            lines = BitmapFont.splitBalanced(displayName, actualLines);
        }
        
        // Calculate total text height and starting Y
        int totalHeight = lines.length * charHeight + (lines.length - 1) * lineSpacing;
        int startY = (tileHeight - totalHeight) / 2;
        
        // Draw each line centered
        for (String line : lines) {
            int lineWidth = fit.useMicro ? BitmapFont.getMicroTextWidth(line) : BitmapFont.getTextWidth(line, 1);
            int startX = (tileWidth - lineWidth) / 2;
            
            // Adjust for offset (when part of merged group)
            int localX = startX - offsetX;
            int localY = startY - offsetZ;
            
            drawTextWithOutlineClipped(line, localX, localY, tileWidth, tileHeight, fit.useMicro);
            startY += charHeight + lineSpacing;
        }
    }

    /**
     * Render text vertically (rotated 90 degrees, reading top-to-bottom).
     * Each character is on its own line.
     */
    private void renderVerticalText(String displayName, TextFitResult fit,
                                     int tileWidth, int tileHeight, int offsetX, int offsetZ) {
        int charWidth = fit.useMicro ? BitmapFont.MICRO_CHAR_WIDTH : BitmapFont.CHAR_WIDTH;
        int charHeight = fit.useMicro ? BitmapFont.MICRO_CHAR_HEIGHT : BitmapFont.CHAR_HEIGHT;
        int charSpacing = fit.useMicro ? 1 : 2;
        
        // For vertical text, each character is stacked
        int totalHeight = displayName.length() * (charHeight + charSpacing) - charSpacing;
        int startY = (tileHeight - totalHeight) / 2;
        int startX = (tileWidth - charWidth) / 2;
        
        // Adjust for offset
        int localX = startX - offsetX;
        int localY = startY - offsetZ;
        
        // Draw each character as a separate "line"
        for (int i = 0; i < displayName.length(); i++) {
            String charStr = String.valueOf(displayName.charAt(i));
            drawTextWithOutlineClipped(charStr, localX, localY, tileWidth, tileHeight, fit.useMicro);
            localY += charHeight + charSpacing;
        }
    }

    /**
     * Render text across a merged tile area.
     * Only called from anchor tile.
     */
    private void renderMergedText(String displayName, TextFitResult fit,
                                   int mergedWidth, int mergedHeight,
                                   int offsetX, int offsetZ, int tileWidth, int tileHeight) {
        if (fit.vertical) {
            renderMergedVerticalText(displayName, fit, mergedWidth, mergedHeight, 
                                     offsetX, offsetZ, tileWidth, tileHeight);
        } else {
            renderMergedHorizontalText(displayName, fit, mergedWidth, mergedHeight,
                                       offsetX, offsetZ, tileWidth, tileHeight);
        }
    }

    /**
     * Render horizontal text across merged tiles.
     */
    private void renderMergedHorizontalText(String displayName, TextFitResult fit,
                                             int mergedWidth, int mergedHeight,
                                             int offsetX, int offsetZ, int tileWidth, int tileHeight) {
        int margin = 2;
        int availWidth = mergedWidth - margin * 2;
        
        int charWidth = fit.useMicro ? BitmapFont.MICRO_CHAR_WIDTH : BitmapFont.CHAR_WIDTH;
        int charSpacing = fit.useMicro ? BitmapFont.MICRO_CHAR_SPACING : BitmapFont.CHAR_SPACING;
        int charHeight = fit.useMicro ? BitmapFont.MICRO_CHAR_HEIGHT : BitmapFont.CHAR_HEIGHT;
        int lineSpacing = fit.useMicro ? 1 : 2;
        int fullCharWidth = charWidth + charSpacing;
        
        // Calculate line splitting
        String[] lines;
        if (fit.lines == 1) {
            lines = new String[]{displayName};
        } else {
            int maxCharsPerLine = Math.max(1, availWidth / fullCharWidth);
            int actualLines = Math.min(fit.lines, (int) Math.ceil((double) displayName.length() / maxCharsPerLine));
            lines = BitmapFont.splitBalanced(displayName, actualLines);
        }
        
        // Calculate total text height and starting Y in merged coordinates
        int totalHeight = lines.length * charHeight + (lines.length - 1) * lineSpacing;
        int mergedStartY = (mergedHeight - totalHeight) / 2;
        
        // Draw each line centered in merged area
        for (String line : lines) {
            int lineWidth = fit.useMicro ? BitmapFont.getMicroTextWidth(line) : BitmapFont.getTextWidth(line, 1);
            int mergedStartX = (mergedWidth - lineWidth) / 2;
            
            // Convert to local tile coordinates
            int localX = mergedStartX - offsetX;
            int localY = mergedStartY - offsetZ;
            
            drawTextWithOutlineClipped(line, localX, localY, tileWidth, tileHeight, fit.useMicro);
            mergedStartY += charHeight + lineSpacing;
        }
    }

    /**
     * Render vertical text across merged tiles.
     */
    private void renderMergedVerticalText(String displayName, TextFitResult fit,
                                           int mergedWidth, int mergedHeight,
                                           int offsetX, int offsetZ, int tileWidth, int tileHeight) {
        int charWidth = fit.useMicro ? BitmapFont.MICRO_CHAR_WIDTH : BitmapFont.CHAR_WIDTH;
        int charHeight = fit.useMicro ? BitmapFont.MICRO_CHAR_HEIGHT : BitmapFont.CHAR_HEIGHT;
        int charSpacing = fit.useMicro ? 1 : 2;
        
        // For vertical text, each character is stacked
        int totalHeight = displayName.length() * (charHeight + charSpacing) - charSpacing;
        int mergedStartY = (mergedHeight - totalHeight) / 2;
        int mergedStartX = (mergedWidth - charWidth) / 2;
        
        // Convert to local and draw
        int localX = mergedStartX - offsetX;
        int localY = mergedStartY - offsetZ;
        
        for (int i = 0; i < displayName.length(); i++) {
            String charStr = String.valueOf(displayName.charAt(i));
            drawTextWithOutlineClipped(charStr, localX, localY, tileWidth, tileHeight, fit.useMicro);
            localY += charHeight + charSpacing;
        }
    }

    /**
     * Draw text with outline, clipped to the tile boundaries.
     * Handles text that may be partially outside the visible area.
     *
     * @param text The text to draw
     * @param startX Starting X in local tile coordinates (may be negative)
     * @param startY Starting Y in local tile coordinates (may be negative)
     * @param tileWidth Width of the tile
     * @param tileHeight Height of the tile
     * @param useMicro Whether to use micro font
     */
    private void drawTextWithOutlineClipped(String text, int startX, int startY,
                                             int tileWidth, int tileHeight, boolean useMicro) {
        if (text == null || text.isEmpty()) return;
        
        int charWidth = useMicro ? BitmapFont.MICRO_CHAR_WIDTH : BitmapFont.CHAR_WIDTH;
        int charHeight = useMicro ? BitmapFont.MICRO_CHAR_HEIGHT : BitmapFont.CHAR_HEIGHT;
        int charSpacing = useMicro ? BitmapFont.MICRO_CHAR_SPACING : BitmapFont.CHAR_SPACING;
        int fullCharWidth = charWidth + charSpacing;
        
        // Calculate which characters are potentially visible
        int textWidth = text.length() * fullCharWidth - charSpacing;
        int textHeight = charHeight;
        
        // Early exit if text is completely outside tile (with outline margin)
        int outlineMargin = 1;
        if (startX + textWidth + outlineMargin < 0 || startX - outlineMargin >= tileWidth ||
            startY + textHeight + outlineMargin < 0 || startY - outlineMargin >= tileHeight) {
            return;
        }
        
        // Draw outline first (8 directions)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx != 0 || dy != 0) {
                    drawTextClipped(text, startX + dx, startY + dy, tileWidth, tileHeight, 
                                   BitmapFont.BLACK, useMicro, charWidth, charSpacing, charHeight);
                }
            }
        }
        
        // Draw main text on top
        drawTextClipped(text, startX, startY, tileWidth, tileHeight, 
                       BitmapFont.WHITE, useMicro, charWidth, charSpacing, charHeight);
    }

    /**
     * Draw text clipped to tile boundaries.
     * Only draws pixels that fall within the tile.
     */
    private void drawTextClipped(String text, int startX, int startY, int tileWidth, int tileHeight,
                                  int color, boolean useMicro, int charWidth, int charSpacing, int charHeight) {
        int x = startX;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            
            // Skip characters completely outside tile
            if (x + charWidth > 0 && x < tileWidth) {
                drawCharClipped(c, x, startY, tileWidth, tileHeight, color, useMicro, charWidth, charHeight);
            }
            
            x += charWidth + charSpacing;
            
            // Early exit if we've passed the right edge
            if (x >= tileWidth) break;
        }
    }

    /**
     * Draw a single character clipped to tile boundaries.
     */
    private void drawCharClipped(char c, int x, int y, int tileWidth, int tileHeight,
                                  int color, boolean useMicro, int charWidth, int charHeight) {
        int[] glyph = useMicro ? getMicroGlyph(c) : getStandardGlyph(c);
        if (glyph == null) return;
        
        for (int row = 0; row < charHeight; row++) {
            int py = y + row;
            if (py < 0 || py >= tileHeight) continue;
            
            int rowBits = glyph[row];
            for (int col = 0; col < charWidth; col++) {
                int px = x + col;
                if (px < 0 || px >= tileWidth) continue;
                
                boolean pixelOn = (rowBits & (1 << (charWidth - 1 - col))) != 0;
                if (pixelOn) {
                    this.image.data[py * tileWidth + px] = color;
                }
            }
        }
    }

    /**
     * Get glyph data for a character (standard 5x7 font).
     */
    private static int[] getStandardGlyph(char c) {
        // Access via reflection-like approach or hardcode common chars
        // For now, use a helper that matches BitmapFont's internal structure
        return BitmapFont.getGlyph(c);
    }

    /**
     * Get glyph data for a character (micro 3x5 font).
     */
    private static int[] getMicroGlyph(char c) {
        return BitmapFont.getMicroGlyph(c);
    }

    private static float shadeFromHeights(int blockPixelX, int blockPixelZ, int blockPixelWidth, int blockPixelHeight,
                                          short height, short north, short south, short west, short east,
                                          short northWest, short northEast, short southWest, short southEast) {
        float u = ((float) blockPixelX + 0.5F) / (float) blockPixelWidth;
        float v = ((float) blockPixelZ + 0.5F) / (float) blockPixelHeight;
        float ud = (u + v) / 2.0F;
        float vd = (1.0F - u + v) / 2.0F;
        float dhdx1 = (float) (height - west) * (1.0F - u) + (float) (east - height) * u;
        float dhdz1 = (float) (height - north) * (1.0F - v) + (float) (south - height) * v;
        float dhdx2 = (float) (height - northWest) * (1.0F - ud) + (float) (southEast - height) * ud;
        float dhdz2 = (float) (height - northEast) * (1.0F - vd) + (float) (southWest - height) * vd;
        float dhdx = dhdx1 * 2.0F + dhdx2;
        float dhdz = dhdz1 * 2.0F + dhdz2;
        float dy = 3.0F;
        float invS = 1.0F / (float) Math.sqrt(dhdx * dhdx + dy * dy + dhdz * dhdz);
        float nx = dhdx * invS;
        float ny = dy * invS;
        float nz = dhdz * invS;
        float lx = -0.2F;
        float ly = 0.8F;
        float lz = 0.5F;
        float invL = 1.0F / (float) Math.sqrt(lx * lx + ly * ly + lz * lz);
        lx *= invL;
        ly *= invL;
        lz *= invL;
        float lambert = Math.max(0.0F, nx * lx + ny * ly + nz * lz);
        float ambient = 0.4F;
        float diffuse = 0.6F;
        return ambient + diffuse * lambert;
    }

    private static void getBlockColor(int blockId, int biomeTintColor, @Nonnull MapColor outColor) {
        BlockType block = BlockType.getAssetMap().getAsset(blockId);
        int biomeTintR = biomeTintColor >> 16 & 255;
        int biomeTintG = biomeTintColor >> 8 & 255;
        int biomeTintB = biomeTintColor & 255;
        com.hypixel.hytale.protocol.Color[] tintUp = block.getTintUp();
        boolean hasTint = tintUp != null && tintUp.length > 0;
        int selfTintR = hasTint ? tintUp[0].red & 255 : 255;
        int selfTintG = hasTint ? tintUp[0].green & 255 : 255;
        int selfTintB = hasTint ? tintUp[0].blue & 255 : 255;
        float biomeTintMultiplier = (float) block.getBiomeTintUp() / 100.0F;
        int tintColorR = (int) ((float) selfTintR + (float) (biomeTintR - selfTintR) * biomeTintMultiplier);
        int tintColorG = (int) ((float) selfTintG + (float) (biomeTintG - selfTintG) * biomeTintMultiplier);
        int tintColorB = (int) ((float) selfTintB + (float) (biomeTintB - selfTintB) * biomeTintMultiplier);
        com.hypixel.hytale.protocol.Color particleColor = block.getParticleColor();
        if (particleColor != null && biomeTintMultiplier < 1.0F) {
            tintColorR = tintColorR * (particleColor.red & 255) / 255;
            tintColorG = tintColorG * (particleColor.green & 255) / 255;
            tintColorB = tintColorB * (particleColor.blue & 255) / 255;
        }

        outColor.r = tintColorR & 255;
        outColor.g = tintColorG & 255;
        outColor.b = tintColorB & 255;
        outColor.a = 255;
    }

    private static void applyClaimColor(Color claimColor, @Nonnull MapColor outColor, boolean isBorder) {
        // Blend the claim color with the terrain color
        // Border pixels get a stronger tint
        float blendFactor = isBorder ? 0.7f : 0.4f;

        outColor.r = (int) (outColor.r * (1 - blendFactor) + claimColor.getRed() * blendFactor);
        outColor.g = (int) (outColor.g * (1 - blendFactor) + claimColor.getGreen() * blendFactor);
        outColor.b = (int) (outColor.b * (1 - blendFactor) + claimColor.getBlue() * blendFactor);
    }

    /**
     * Applies a subtle green tint to indicate PvP-disabled (safe) zones.
     */
    private static void applyPvPSafeOverlay(@Nonnull MapColor outColor, boolean isBorder) {
        // Add subtle green tint for safe zones
        // This creates a visual "safe zone" indicator
        float blendFactor = isBorder ? 0.25f : 0.15f;

        // Boost green channel slightly to indicate safe zone
        outColor.g = Math.min(255, (int) (outColor.g * (1 - blendFactor) + 200 * blendFactor));
        // Reduce red slightly to make the green more visible
        outColor.r = (int) (outColor.r * (1 - blendFactor * 0.3f));
    }

    private static void getFluidColor(int fluidId, int environmentId, int fluidDepth, @Nonnull MapColor outColor) {
        int tintColorR = 255;
        int tintColorG = 255;
        int tintColorB = 255;
        Environment environment = Environment.getAssetMap().getAsset(environmentId);
        com.hypixel.hytale.protocol.Color waterTint = environment.getWaterTint();
        if (waterTint != null) {
            tintColorR = tintColorR * (waterTint.red & 255) / 255;
            tintColorG = tintColorG * (waterTint.green & 255) / 255;
            tintColorB = tintColorB * (waterTint.blue & 255) / 255;
        }

        Fluid fluid = Fluid.getAssetMap().getAsset(fluidId);
        com.hypixel.hytale.protocol.Color particleColor = fluid.getParticleColor();
        if (particleColor != null) {
            tintColorR = tintColorR * (particleColor.red & 255) / 255;
            tintColorG = tintColorG * (particleColor.green & 255) / 255;
            tintColorB = tintColorB * (particleColor.blue & 255) / 255;
        }

        float depthMultiplier = Math.min(1.0F, 1.0F / (float) fluidDepth);
        outColor.r = (int) ((float) tintColorR + (float) ((outColor.r & 255) - tintColorR) * depthMultiplier) & 255;
        outColor.g = (int) ((float) tintColorG + (float) ((outColor.g & 255) - tintColorG) * depthMultiplier) & 255;
        outColor.b = (int) ((float) tintColorB + (float) ((outColor.b & 255) - tintColorB) * depthMultiplier) & 255;
    }

    @Nonnull
    public static CompletableFuture<ClaimImageBuilder> build(long index, int imageWidth, int imageHeight, World world) {
        return CompletableFuture.completedFuture(new ClaimImageBuilder(index, imageWidth, imageHeight, world))
                .thenCompose(ClaimImageBuilder::fetchChunk)
                .thenCompose((builder) -> builder != null ? builder.sampleNeighborsSync() : CompletableFuture.completedFuture(null))
                .thenApplyAsync((builder) -> builder != null ? builder.generateImageAsync() : null);
    }

    /**
     * Helper class for color manipulation during map generation.
     */
    private static class MapColor {
        public int r;
        public int g;
        public int b;
        public int a;

        public int pack() {
            return (this.r & 255) << 24 | (this.g & 255) << 16 | (this.b & 255) << 8 | this.a & 255;
        }

        public void multiply(float value) {
            this.r = Math.min(255, Math.max(0, (int) ((float) this.r * value)));
            this.g = Math.min(255, Math.max(0, (int) ((float) this.g * value)));
            this.b = Math.min(255, Math.max(0, (int) ((float) this.b * value)));
        }
    }
}
