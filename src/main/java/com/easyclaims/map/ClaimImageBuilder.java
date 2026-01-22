package com.easyclaims.map;

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
import com.easyclaims.EasyClaimsAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.Color;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
        UUID claimOwner = EasyClaimsAccess.getClaimOwner(worldName, chunkX, chunkZ);
        Color claimColor = claimOwner != null ? ClaimColorGenerator.getPlayerColor(claimOwner) : null;

        // Get neighboring claim owners to determine borders (reuse array to reduce allocations)
        nearbyOwners[0] = EasyClaimsAccess.getClaimOwner(worldName, chunkX, chunkZ + 1); // SOUTH
        nearbyOwners[1] = EasyClaimsAccess.getClaimOwner(worldName, chunkX, chunkZ - 1); // NORTH
        nearbyOwners[2] = EasyClaimsAccess.getClaimOwner(worldName, chunkX + 1, chunkZ); // EAST
        nearbyOwners[3] = EasyClaimsAccess.getClaimOwner(worldName, chunkX - 1, chunkZ); // WEST

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

                // Apply claim overlay if this chunk is claimed
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
                }

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

                // Apply fluid tinting
                if (height < 320) {
                    int fluidId = this.fluidSamples[sampleIndex];
                    if (fluidId != 0) {
                        short fluidDepth = this.fluidDepthSamples[sampleIndex];
                        int environmentId = this.environmentSamples[sampleIndex];
                        getFluidColor(fluidId, environmentId, fluidDepth, this.outColor);
                    }
                }

                this.image.data[iz * this.image.width + ix] = this.outColor.pack();
            }
        }

        // Draw owner name and trusted players text on claimed chunks
        if (claimOwner != null) {
            drawClaimText(worldName, chunkX, chunkZ);
        }

        return this;
    }

    /**
     * Draws owner name and trusted player names on the map tile.
     * Text is centered and may extend beyond tile boundaries.
     */
    private void drawClaimText(String worldName, int chunkX, int chunkZ) {
        String ownerName = EasyClaimsAccess.getOwnerName(worldName, chunkX, chunkZ);
        List<String> trustedNames = EasyClaimsAccess.getTrustedPlayerNames(worldName, chunkX, chunkZ);

        if (ownerName == null) {
            return;
        }

        // Calculate vertical positioning
        int lineHeight = BitmapFont.CHAR_HEIGHT + 2; // 7 + 2 = 9 pixels per line
        int totalLines = 1 + Math.min(trustedNames.size(), 2); // Owner + up to 2 trusted
        int startY = (this.image.height - (totalLines * lineHeight)) / 2;

        // Draw owner name (white text with black outline for crisp visibility)
        BitmapFont.drawTextCenteredWithOutline(
            this.image.data, this.image.width, this.image.height,
            ownerName, startY,
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
