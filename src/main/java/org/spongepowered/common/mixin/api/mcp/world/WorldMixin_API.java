/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.mixin.api.mcp.world;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.IFluidState;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.network.IPacket;
import net.minecraft.particles.IParticleData;
import net.minecraft.profiler.IProfiler;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.tags.NetworkTagManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.GameRules;
import net.minecraft.world.LightType;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.AbstractChunkProvider;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.storage.MapData;
import net.minecraft.world.storage.WorldInfo;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Server;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.data.property.Property;
import org.spongepowered.api.data.value.MergeFunction;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.sound.SoundType;
import org.spongepowered.api.effect.sound.music.MusicDisc;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.fluid.FluidType;
import org.spongepowered.api.scheduler.ScheduledUpdateList;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.text.BookView;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.chat.ChatType;
import org.spongepowered.api.text.title.Title;
import org.spongepowered.api.util.AABB;
import org.spongepowered.api.util.TemporalUnits;
import org.spongepowered.api.world.BlockChangeFlag;
import org.spongepowered.api.world.BlockChangeFlags;
import org.spongepowered.api.world.HeightType;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.biome.BiomeType;
import org.spongepowered.api.world.chunk.Chunk;
import org.spongepowered.api.world.gen.TerrainGenerator;
import org.spongepowered.api.world.storage.WorldProperties;
import org.spongepowered.api.world.volume.archetype.ArchetypeVolume;
import org.spongepowered.api.world.volume.biome.stream.BiomeVolumeStream;
import org.spongepowered.api.world.volume.block.stream.BlockVolumeStream;
import org.spongepowered.api.world.volume.entity.ImmutableEntityVolume;
import org.spongepowered.api.world.volume.entity.UnmodifiableEntityVolume;
import org.spongepowered.api.world.volume.entity.stream.EntityStream;
import org.spongepowered.api.world.weather.Weather;
import org.spongepowered.api.world.weather.Weathers;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.block.SpongeBlockSnapshotBuilder;
import org.spongepowered.common.bridge.world.chunk.ChunkBridge;
import org.spongepowered.common.effect.particle.SpongeParticleEffect;
import org.spongepowered.common.effect.particle.SpongeParticleHelper;
import org.spongepowered.common.event.tracking.TrackingUtil;
import org.spongepowered.common.registry.type.world.BlockChangeFlagRegistryModule;
import org.spongepowered.common.util.Constants;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Mixin(net.minecraft.world.World.class)
public abstract class WorldMixin_API implements IWorldMixin_API<World>, World, IEnvironmentBlockReaderMixin_API, AutoCloseable {
    @Shadow protected static @Final Logger LOGGER;
    @Shadow private static @Final Direction[] FACING_VALUES;
    @Shadow public @Final List<TileEntity> loadedTileEntityList;
    @Shadow public @Final List<TileEntity> tickableTileEntities;
    @Shadow protected @Final List<TileEntity> addedTileEntityList;
    @Shadow protected @Final List<TileEntity> tileEntitiesToBeRemoved;
    @Shadow private @Final long cloudColour;
    @Shadow private @Final Thread mainThread;
    @Shadow private int skylightSubtracted;
    @Shadow protected int updateLCG;
    @Shadow protected @Final int DIST_HASH_MAGIC;
    @Shadow protected float prevRainingStrength;
    @Shadow protected float rainingStrength;
    @Shadow protected float prevThunderingStrength;
    @Shadow protected float thunderingStrength;
    @Shadow private int lastLightningBolt;
    @Shadow public @Final Random rand;
    @Shadow public @Final Dimension dimension;
    @Shadow protected @Final AbstractChunkProvider chunkProvider;
    @Shadow protected @Final WorldInfo worldInfo;
    @Shadow private @Final IProfiler profiler;
    @Shadow public @Final boolean isRemote;
    @Shadow protected boolean processingLoadedTiles;
    @Shadow private @Final WorldBorder worldBorder;
    // Shadowed methods and fields for reference. All should be prefixed with 'shadow$' to avoid confusion

    @Shadow public abstract Biome shadow$getBiome(BlockPos p_180494_1_);
    @Shadow public abstract boolean shadow$isRemote();
    @Shadow public abstract @javax.annotation.Nullable MinecraftServer shadow$getServer();
    @Shadow public abstract BlockState shadow$getGroundAboveSeaLevel(BlockPos p_184141_1_);
    @Shadow public static boolean shadow$isValid(BlockPos p_175701_0_) {
        throw new UnsupportedOperationException("Shadowed isInWorldBounds");
    }
    @Shadow public static boolean shadow$isOutsideBuildHeight(BlockPos p_189509_0_) {
        throw new UnsupportedOperationException("Shadowed isOutisdeBuildheight");
    }
    @Shadow public static boolean shadow$isYOutOfBounds(int p_217405_0_) {
        throw new UnsupportedOperationException("Shadowed isOutsideBuildHeight");
    }
    @Shadow public abstract net.minecraft.world.chunk.Chunk shadow$getChunkAt(BlockPos p_175726_1_);
    @Shadow public abstract net.minecraft.world.chunk.Chunk shadow$getChunk(int p_212866_1_, int p_212866_2_);
    @Shadow public abstract IChunk shadow$getChunk(int p_217353_1_, int p_217353_2_, ChunkStatus p_217353_3_, boolean p_217353_4_);
    @Shadow public abstract boolean shadow$setBlockState(BlockPos p_180501_1_, BlockState p_180501_2_, int p_180501_3_);
    @Shadow public abstract void shadow$func_217393_a(BlockPos p_217393_1_, BlockState p_217393_2_, BlockState p_217393_3_);
    @Shadow public abstract boolean shadow$removeBlock(BlockPos p_217377_1_, boolean p_217377_2_);
    @Shadow public abstract boolean shadow$destroyBlock(BlockPos p_175655_1_, boolean p_175655_2_);
    @Shadow public abstract boolean shadow$setBlockState(BlockPos p_175656_1_, BlockState p_175656_2_);
    @Shadow public abstract void shadow$notifyBlockUpdate(BlockPos p_184138_1_, BlockState p_184138_2_, BlockState p_184138_3_, int p_184138_4_);
    @Shadow public abstract void shadow$notifyNeighbors(BlockPos p_195592_1_, Block p_195592_2_);
    @Shadow public abstract void shadow$func_225319_b(BlockPos p_225319_1_, BlockState p_225319_2_, BlockState p_225319_3_);
    @Shadow public abstract void shadow$notifyNeighborsOfStateChange(BlockPos p_195593_1_, Block p_195593_2_);
    @Shadow public abstract void shadow$notifyNeighborsOfStateExcept(BlockPos p_175695_1_, Block p_175695_2_, Direction p_175695_3_);
    @Shadow public abstract void shadow$neighborChanged(BlockPos p_190524_1_, Block p_190524_2_, BlockPos p_190524_3_);
    @Shadow public abstract int shadow$getLightSubtracted(BlockPos p_201669_1_, int p_201669_2_);
    @Shadow public abstract int shadow$getHeight(Heightmap.Type p_201676_1_, int p_201676_2_, int p_201676_3_);
    @Shadow public abstract int shadow$getLightFor(LightType p_175642_1_, BlockPos p_175642_2_);
    @Shadow public abstract BlockState shadow$getBlockState(BlockPos p_180495_1_);
    @Shadow public abstract IFluidState getFluidState(BlockPos p_204610_1_);
    @Shadow public abstract boolean shadow$isDaytime();
    @Shadow public abstract void shadow$playSound(@javax.annotation.Nullable PlayerEntity p_184133_1_, BlockPos p_184133_2_, SoundEvent p_184133_3_, SoundCategory p_184133_4_, float p_184133_5_, float p_184133_6_);
    @Shadow public abstract void shadow$playSound(@javax.annotation.Nullable PlayerEntity p_184148_1_, double p_184148_2_, double p_184148_4_, double p_184148_6_, SoundEvent p_184148_8_, SoundCategory p_184148_9_, float p_184148_10_, float p_184148_11_);
    @Shadow public abstract void shadow$playMovingSound(@javax.annotation.Nullable PlayerEntity p_217384_1_, Entity p_217384_2_, SoundEvent p_217384_3_, SoundCategory p_217384_4_, float p_217384_5_, float p_217384_6_);
    @Shadow public abstract void shadow$playSound(double p_184134_1_, double p_184134_3_, double p_184134_5_, SoundEvent p_184134_7_, SoundCategory p_184134_8_, float p_184134_9_, float p_184134_10_, boolean p_184134_11_);
    @Shadow public abstract void shadow$addParticle(IParticleData p_195594_1_, double p_195594_2_, double p_195594_4_, double p_195594_6_, double p_195594_8_, double p_195594_10_, double p_195594_12_);
    @Shadow public abstract void shadow$addOptionalParticle(IParticleData p_195589_1_, double p_195589_2_, double p_195589_4_, double p_195589_6_, double p_195589_8_, double p_195589_10_, double p_195589_12_);
    @Shadow public abstract void shadow$addOptionalParticle(IParticleData p_217404_1_, boolean p_217404_2_, double p_217404_3_, double p_217404_5_, double p_217404_7_, double p_217404_9_, double p_217404_11_, double p_217404_13_);
    @Shadow public abstract float shadow$getCelestialAngleRadians(float p_72929_1_);
    @Shadow public abstract boolean shadow$addTileEntity(TileEntity p_175700_1_);
    @Shadow public abstract void shadow$addTileEntities(Collection<TileEntity> p_147448_1_);
    @Shadow public abstract void shadow$func_217391_K(); // tileTileEntities
    @Shadow public abstract void shadow$func_217390_a(Consumer<Entity> p_217390_1_, Entity p_217390_2_);
    @Shadow public abstract boolean shadow$checkBlockCollision(AxisAlignedBB p_72829_1_);
    @Shadow public abstract boolean shadow$isFlammableWithin(AxisAlignedBB p_147470_1_);
    @Shadow public abstract boolean shadow$isMaterialInBB(AxisAlignedBB p_72875_1_, Material p_72875_2_);
    @Shadow public abstract net.minecraft.world.Explosion shadow$createExplosion(@javax.annotation.Nullable Entity p_217385_1_, double p_217385_2_, double p_217385_4_, double p_217385_6_, float p_217385_8_, net.minecraft.world.Explosion.Mode p_217385_9_);
    @Shadow public abstract net.minecraft.world.Explosion shadow$createExplosion(@javax.annotation.Nullable Entity p_217398_1_, double p_217398_2_, double p_217398_4_, double p_217398_6_, float p_217398_8_, boolean p_217398_9_, net.minecraft.world.Explosion.Mode p_217398_10_);
    @Shadow public abstract net.minecraft.world.Explosion shadow$createExplosion(@javax.annotation.Nullable Entity p_217401_1_, @javax.annotation.Nullable DamageSource p_217401_2_, double p_217401_3_, double p_217401_5_, double p_217401_7_, float p_217401_9_, boolean p_217401_10_, net.minecraft.world.Explosion.Mode p_217401_11_);
    @Shadow public abstract boolean shadow$extinguishFire(@javax.annotation.Nullable PlayerEntity p_175719_1_, BlockPos p_175719_2_, Direction p_175719_3_);
    @Shadow public abstract @javax.annotation.Nullable TileEntity shadow$getTileEntity(BlockPos p_175625_1_);
    @Shadow private @javax.annotation.Nullable TileEntity shadow$getPendingTileEntityAt(BlockPos p_189508_1_) {
        // Shadowed
        return null;
    }
    @Shadow public abstract void shadow$setTileEntity(BlockPos p_175690_1_, @javax.annotation.Nullable TileEntity p_175690_2_);
    @Shadow public abstract void shadow$removeTileEntity(BlockPos p_175713_1_);
    @Shadow public abstract boolean shadow$isBlockPresent(BlockPos p_195588_1_);
    @Shadow public abstract boolean shadow$isTopSolid(BlockPos p_217400_1_, Entity p_217400_2_) ;
    @Shadow public abstract void shadow$calculateInitialSkylight();
    @Shadow public abstract void shadow$setAllowedSpawnTypes(boolean p_72891_1_, boolean p_72891_2_);
    @Shadow protected abstract void shadow$calculateInitialWeather();
    @Shadow public abstract void shadow$close() throws IOException;
    @Shadow public abstract ChunkStatus shadow$getChunkStatus();
    @Shadow public abstract List<Entity> shadow$getEntitiesInAABBexcluding(@javax.annotation.Nullable Entity p_175674_1_, AxisAlignedBB p_175674_2_, @javax.annotation.Nullable Predicate<? super Entity> p_175674_3_);
    @Shadow public abstract List<Entity> shadow$getEntitiesWithinAABB(@javax.annotation.Nullable EntityType<?> p_217394_1_, AxisAlignedBB p_217394_2_, Predicate<? super Entity> p_217394_3_);
    @Shadow public abstract <T extends Entity> List<T> shadow$getEntitiesWithinAABB(Class<? extends T> p_175647_1_, AxisAlignedBB p_175647_2_, @javax.annotation.Nullable Predicate<? super T> p_175647_3_);
    @Shadow public abstract <T extends Entity> List<T> shadow$func_225316_b(Class<? extends T> p_225316_1_, AxisAlignedBB p_225316_2_, @javax.annotation.Nullable Predicate<? super T> p_225316_3_);
    @Shadow public abstract @javax.annotation.Nullable Entity shadow$getEntityByID(int p_73045_1_);
    @Shadow public abstract void shadow$markChunkDirty(BlockPos p_175646_1_, TileEntity p_175646_2_);
    @Shadow public abstract int shadow$getSeaLevel();
    @Shadow public abstract net.minecraft.world.World shadow$getWorld();
    @Shadow public abstract WorldType shadow$getWorldType();
    @Shadow public abstract int shadow$getStrongPower(BlockPos p_175676_1_);
    @Shadow public abstract boolean shadow$isSidePowered(BlockPos p_175709_1_, Direction p_175709_2_);
    @Shadow public abstract int shadow$getRedstonePower(BlockPos p_175651_1_, Direction p_175651_2_);
    @Shadow public abstract boolean shadow$isBlockPowered(BlockPos p_175640_1_);
    @Shadow public abstract int shadow$getRedstonePowerFromNeighbors(BlockPos p_175687_1_);
    @Shadow public abstract void shadow$setGameTime(long gametime);
    @Shadow public abstract long shadow$getSeed();
    @Shadow public abstract long shadow$getGameTime();
    @Shadow public abstract long shadow$getDayTime();
    @Shadow public abstract void shadow$setDayTime(long delay);
    @Shadow protected abstract void shadow$advanceTime();
    @Shadow public abstract BlockPos shadow$getSpawnPoint();
    @Shadow public abstract void shadow$setSpawnPoint(BlockPos p_175652_1_);
    @Shadow public abstract boolean shadow$isBlockModifiable(PlayerEntity p_175660_1_, BlockPos p_175660_2_);
    @Shadow public abstract void shadow$setEntityState(Entity p_72960_1_, byte p_72960_2_);
    @Shadow public abstract AbstractChunkProvider shadow$getChunkProvider();
    @Shadow public abstract void shadow$addBlockEvent(BlockPos p_175641_1_, Block p_175641_2_, int p_175641_3_, int p_175641_4_);
    @Shadow public abstract WorldInfo shadow$getWorldInfo();
    @Shadow public abstract GameRules shadow$getGameRules();
    @Shadow public abstract float shadow$getThunderStrength(float p_72819_1_);
    @Shadow public abstract float shadow$getRainStrength(float p_72867_1_);
    @Shadow public abstract boolean shadow$isThundering();
    @Shadow public abstract boolean shadow$isRaining();
    @Shadow public abstract boolean shadow$isRainingAt(BlockPos p_175727_1_);
    @Shadow public abstract boolean shadow$isBlockinHighHumidity(BlockPos p_180502_1_);
    @Shadow public abstract @javax.annotation.Nullable MapData shadow$getMapData(String p_217406_1_);
    @Shadow public abstract void shadow$registerMapData(MapData p_217399_1_);
    @Shadow public abstract int shadow$getNextMapId();
    @Shadow public abstract void shadow$playBroadcastSound(int p_175669_1_, BlockPos p_175669_2_, int p_175669_3_);
    @Shadow public abstract int shadow$getActualHeight();
    @Shadow public abstract CrashReportCategory shadow$fillCrashReport(CrashReport p_72914_1_);
    @Shadow public abstract void shadow$sendBlockBreakProgress(int p_175715_1_, BlockPos p_175715_2_, int p_175715_3_);
    @Shadow public abstract Scoreboard shadow$getScoreboard();
    @Shadow public abstract void shadow$updateComparatorOutputLevel(BlockPos p_175666_1_, Block p_175666_2_);
    @Shadow public abstract DifficultyInstance shadow$getDifficultyForLocation(BlockPos p_175649_1_);
    @Shadow public abstract int shadow$getSkylightSubtracted();
    @Shadow public abstract void shadow$setLastLightningBolt(int p_175702_1_);
    @Shadow public abstract WorldBorder shadow$getWorldBorder();
    @Shadow public abstract void shadow$sendPacketToServer(IPacket<?> p_184135_1_);
    @Shadow public abstract @javax.annotation.Nullable BlockPos shadow$findNearestStructure(String p_211157_1_, BlockPos p_211157_2_, int p_211157_3_, boolean p_211157_4_);
    @Shadow public abstract Dimension shadow$getDimension();



    @Shadow public abstract Random shadow$getRandom();
    @Shadow public abstract boolean shadow$hasBlockState(BlockPos p_217375_1_, Predicate<BlockState> p_217375_2_);
    @Shadow public abstract RecipeManager shadow$getRecipeManager();
    @Shadow public abstract NetworkTagManager shadow$getTags();
    @Shadow public abstract BlockPos shadow$func_217383_a(int p_217383_1_, int p_217383_2_, int p_217383_3_, int p_217383_4_);
    @Shadow public abstract boolean shadow$isSaveDisabled();
    @Shadow public abstract IProfiler shadow$getProfiler();
    @Shadow public abstract BlockPos shadow$getHeight(Heightmap.Type p_205770_1_, BlockPos p_205770_2_);

    @Override
    public Random getRandom() {
        return this.rand;
    }

    @Override
    public Vector3i getBlockMin() {
        return Constants.World.BLOCK_MIN;
    }

    @Override
    public Vector3i getBlockMax() {
        return Constants.World.BLOCK_MAX;
    }

    @Override
    public Vector3i getBlockSize() {
        return Constants.World.BLOCK_SIZE;
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        return false;
    }

    @Override
    public World getView(Vector3i newMin, Vector3i newMax) {
        return null;
    }

    @Override
    public UnmodifiableEntityVolume<?> asUnmodifiableEntityVolume() {
        return null;
    }

    @Override
    public ImmutableEntityVolume asImmutableEntityVolume() {
        return null;
    }

    @Override
    public Optional<org.spongepowered.api.entity.Entity> getEntity(UUID uuid) {
        return Optional.empty();
    }

    @Override
    public Collection<? extends org.spongepowered.api.entity.Entity> getEntities(AABB box, Predicate<? super org.spongepowered.api.entity.Entity> filter) {
        return null;
    }

    @Override
    public <E extends org.spongepowered.api.entity.Entity> Collection<? extends E> getEntities(Class<? extends E> entityClass, AABB box, @javax.annotation.Nullable Predicate<? super E> predicate) {
        return null;
    }

    @Override
    public long getSeed() {
        return 0;
    }

    @Override
    public TerrainGenerator<?> getTerrainGenerator() {
        return null;
    }

    @Override
    public WorldProperties getProperties() {
        return null;
    }

    @Override
    public <V> Optional<V> getProperty(int x, int y, int z, Property<V> property) {
        return Optional.empty();
    }

    @Override
    public OptionalInt getIntProperty(int x, int y, int z, Property<Integer> property) {
        return null;
    }

    @Override
    public OptionalDouble getDoubleProperty(int x, int y, int z, Property<Double> property) {
        return null;
    }

    @Override
    public <V> Optional<V> getProperty(int x, int y, int z, org.spongepowered.api.util.Direction direction, Property<V> property) {
        return Optional.empty();
    }

    @Override
    public OptionalInt getIntProperty(int x, int y, int z, org.spongepowered.api.util.Direction direction, Property<Integer> property) {
        return null;
    }

    @Override
    public OptionalDouble getDoubleProperty(int x, int y, int z, org.spongepowered.api.util.Direction direction, Property<Double> property) {
        return null;
    }

    @Override
    public Map<Property<?>, ?> getProperties(int x, int y, int z) {
        return null;
    }

    @Override
    public Collection<org.spongepowered.api.util.Direction> getFacesWithProperty(int x, int y, int z, Property<?> property) {
        return null;
    }

    @Override
    public org.spongepowered.api.entity.Entity createEntity(org.spongepowered.api.entity.EntityType<?> type, Vector3d position) throws IllegalArgumentException, IllegalStateException {
        return null;
    }

    @Override
    public org.spongepowered.api.entity.Entity createEntityNaturally(org.spongepowered.api.entity.EntityType<?> type, Vector3d position) throws IllegalArgumentException, IllegalStateException {
        return null;
    }

    @Override
    public Optional<org.spongepowered.api.entity.Entity> createEntity(DataContainer entityContainer) {
        return Optional.empty();
    }

    @Override
    public Optional<org.spongepowered.api.entity.Entity> createEntity(DataContainer entityContainer, Vector3d position) {
        return Optional.empty();
    }

    @Override
    public Collection<org.spongepowered.api.entity.Entity> spawnEntities(Iterable<? extends org.spongepowered.api.entity.Entity> entities) {
        return null;
    }

    @Override
    public int getHeight(HeightType type, int x, int z) {
        return 0;
    }

    @Override
    public <E> Optional<E> get(int x, int y, int z, Key<? extends Value<E>> key) {
        return Optional.empty();
    }

    @Override
    public <E, V extends Value<E>> Optional<V> getValue(int x, int y, int z, Key<V> key) {
        return Optional.empty();
    }

    @Override
    public boolean supports(int x, int y, int z, Key<?> key) {
        return false;
    }

    @Override
    public Set<Key<?>> getKeys(int x, int y, int z) {
        return null;
    }

    @Override
    public Set<Value.Immutable<?>> getValues(int x, int y, int z) {
        return null;
    }

    @Override
    public <E> DataTransactionResult offer(int x, int y, int z, Key<? extends Value<E>> key, E value) {
        return null;
    }

    @Override
    public DataTransactionResult remove(int x, int y, int z, Key<?> key) {
        return null;
    }

    @Override
    public DataTransactionResult undo(int x, int y, int z, DataTransactionResult result) {
        return null;
    }

    @Override
    public DataTransactionResult copyFrom(int xTo, int yTo, int zTo, DataHolder from) {
        return null;
    }

    @Override
    public DataTransactionResult copyFrom(int xTo, int yTo, int zTo, DataHolder from, MergeFunction function) {
        return null;
    }

    @Override
    public DataTransactionResult copyFrom(int xTo, int yTo, int zTo, int xFrom, int yFrom, int zFrom, MergeFunction function) {
        return null;
    }

    @Override
    public boolean validateRawData(int x, int y, int z, DataView container) {
        return false;
    }

    @Override
    public void setRawData(int x, int y, int z, DataView container) throws InvalidDataException {

    }

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
    public ScheduledUpdateList<BlockType> getScheduledBlockUpdates() {
        return null;
    }

    @Override
    public ScheduledUpdateList<FluidType> getScheduledFluidUpdates() {
        return null;
    }

    @Override
    public boolean setBlock(int x, int y, int z, org.spongepowered.api.block.BlockState state, BlockChangeFlag flag) {
        return false;
    }

    @Override
    public boolean spawnEntity(org.spongepowered.api.entity.Entity entity) {
        return false;
    }

    @Override
    public boolean removeBlock(int x, int y, int z) {
        return false;
    }

    @Override
    public Optional<Player> getClosestPlayer(int x, int y, int z, double distance, Predicate<? super Player> predicate) {
        return Optional.empty();
    }

    @Override
    public BlockSnapshot createSnapshot(int x, int y, int z) {
        if (!containsBlock(x, y, z)) {
            return BlockSnapshot.NONE;
        }
        if (!this.shadow$chunkExists(x >> 4, z >> 4)) {
            return BlockSnapshot.NONE;
        }
        final BlockPos pos = new BlockPos(x, y, z);
        final SpongeBlockSnapshotBuilder builder = SpongeBlockSnapshotBuilder.pooled();
        builder.worldId(this.getProperties().getUniqueId())
                .position(new Vector3i(x, y, z));
        final IChunk chunk = this.shadow$getChunk(pos);
        final net.minecraft.block.BlockState state = chunk.getBlockState(pos);
        builder.blockState(state);
        if (chunk instanceof net.minecraft.world.chunk.Chunk) {
            @javax.annotation.Nullable final net.minecraft.tileentity.TileEntity tile = ((net.minecraft.world.chunk.Chunk) chunk).getTileEntity(pos, net.minecraft.world.chunk.Chunk.CreateEntityType.CHECK);
            if (tile != null) {
                TrackingUtil.addTileEntityToBuilder(tile, builder);
            }
        }
        ((ChunkBridge) chunk).bridge$getBlockOwnerUUID(pos).ifPresent(builder::creator);
        ((ChunkBridge) chunk).bridge$getBlockNotifierUUID(pos).ifPresent(builder::notifier);

        builder.flag(BlockChangeFlags.NONE);


        return builder.build();
    }

    @Override
    public boolean restoreSnapshot(BlockSnapshot snapshot, boolean force, BlockChangeFlag flag) {
        return snapshot.restore(force, flag);
    }

    @Override
    public boolean restoreSnapshot(int x, int y, int z, BlockSnapshot snapshot, boolean force, BlockChangeFlag flag) {
        return snapshot.withLocation(Location.of(this, x, y, z))
                .restore(force, flag);
    }

    @Override
    public Chunk getChunkAtBlock(Vector3i blockPosition) {
        return (Chunk) this.shadow$getChunkAt(new BlockPos(blockPosition.getX(), blockPosition.getY(), blockPosition.getZ()));
    }

    @Override
    public Chunk getChunkAtBlock(int bx, int by, int bz) {
        return (Chunk) this.shadow$getChunkAt(new BlockPos(bx, by, bz));
    }

    @Override
    public Chunk getChunk(Vector3i chunkPos) {
        return (Chunk) this.shadow$getChunk(chunkPos.getX() >> 4, chunkPos.getZ() >> 4, ChunkStatus.FULL, false);
    }

    @Override
    public Collection<? extends Player> getPlayers() {
        return IWorldMixin_API.super.getPlayers();
    }

    @Override
    public boolean setBlock(Vector3i position, org.spongepowered.api.block.BlockState block) {
        return this.shadow$setBlockState(new BlockPos(position.getX(), position.getY(), position.getZ()), (BlockState) block, Constants.BlockChangeFlags.ALL);
    }

    @Override
    public boolean setBlock(int x, int y, int z, org.spongepowered.api.block.BlockState block) {
        return this.shadow$setBlockState(new BlockPos(x, y, z), (BlockState) block, Constants.BlockChangeFlags.ALL);
    }

    @Override
    public boolean setBlock(Vector3i position, org.spongepowered.api.block.BlockState state, BlockChangeFlag flag) {
        return this.shadow$setBlockState(new BlockPos(position.getX(), position.getY(), position.getZ()), (BlockState) state, BlockChangeFlagRegistryModule.toNative(flag));
    }

    @Override
    public Chunk getChunk(int cx, int cy, int cz) {
        return (Chunk) this.shadow$getChunk(cx, cz);
    }

    @Override
    public Optional<Chunk> loadChunk(int cx, int cy, int cz, boolean shouldGenerate) {
        return Optional.ofNullable((Chunk) this.shadow$getChunk(cx >> 4, cz >> 4, shouldGenerate ? ChunkStatus.FULL : ChunkStatus.EMPTY, false));
    }


    @Override
    public Iterable<Chunk> getLoadedChunks() {
        throw new UnsupportedOperationException("Unfortunately, you've found a World that doesn't know how to iterate it's loaded chunks.");
    }

    @Override
    public boolean isLoaded() {
        throw new UnsupportedOperationException("Unfortunately, Sponge doesn't know how to make a non-managed world loaded.");
    }

    @Override
    public Context getContext() {
        return null;
    }

    @Override
    public void sendMessage(ChatType type, Text message) {

    }

    @Override
    public MessageChannel getMessageChannel() {
        return MessageChannel.toNone();
    }

    @Override
    public void setMessageChannel(MessageChannel channel) {
        throw new UnsupportedOperationException("Unfortunately, you've found a World that doesn't know how to change it's message channel.");
    }

    @Override
    public Location getLocation(Vector3i position) {
        return Location.of(this, position);
    }

    @Override
    public Location getLocation(Vector3d position) {
        return Location.of(this, position);
    }

    @Override
    public ArchetypeVolume createArchetypeVolume(Vector3i min, Vector3i max, Vector3i origin) {

        return null;
    }

    @Override
    public Optional<UUID> getCreator(int x, int y, int z) {
        // By default, this does nothing - We use a separate mixin to implement this onto ServerWorld.
        return Optional.empty();
    }

    @Override
    public Optional<UUID> getNotifier(int x, int y, int z) {
        // By default, this does nothing - We use a separate mixin to implement this onto ServerWorld.
        return Optional.empty();
    }

    @Override
    public void setCreator(int x, int y, int z, @Nullable UUID uuid) {
        // By default, this does nothing - We use a separate mixin to implement this onto ServerWorld.
    }

    @Override
    public void setNotifier(int x, int y, int z, @Nullable UUID uuid) {
        // By default, this does nothing - We use a separate mixin to implement this onto ServerWorld.
    }

    @Override
    public Weather getWeather() {
        // TODO - determine how to implement this properly
        return Weathers.CLEAR;
    }

    @Override
    public Duration getRemainingWeatherDuration() {
        // TODO - determine how to implement this properly
        return Duration.of(0, TemporalUnits.MINECRAFT_TICKS);
    }

    @Override
    public Duration getRunningWeatherDuration() {
        // TODO - determine how to implement this properly
        return Duration.of(0, TemporalUnits.MINECRAFT_TICKS);
    }

    @Override
    public void setWeather(Weather weather) {

    }

    @Override
    public void setWeather(Weather weather, Duration duration) {

    }

    @Override
    public void spawnParticles(ParticleEffect particleEffect, Vector3d position, int radius) {
        checkNotNull(particleEffect, "The particle effect cannot be null!");
        checkNotNull(position, "The position cannot be null");
        checkArgument(radius > 0, "The radius has to be greater then zero!");

        final List<IPacket<?>> packets = SpongeParticleHelper.toPackets((SpongeParticleEffect) particleEffect, position);

        if (!packets.isEmpty()) {
            final PlayerList playerList = this.shadow$getServer().getPlayerList();

            final double x = position.getX();
            final double y = position.getY();
            final double z = position.getZ();

            for (final IPacket<?> packet : packets) {
                // TODO - Might have to spoof this because of the separation from type and id
                playerList.sendToAllNearExcept(null, x, y, z, radius, this.shadow$getDimension().getType(), packet);
            }
        }
    }


    @Override
    public void playSound(SoundType sound, org.spongepowered.api.effect.sound.SoundCategory category, Vector3d position, double volume, double pitch, double minVolume) {
        throw new UnsupportedOperationException("Unfortunately you've found a World that doesn't implement playSound");
    }

    @Override
    public void stopSounds() {
        throw new UnsupportedOperationException("Unfortunately you've found a World that doesn't implement stopSounds");
    }

    @Override
    public void stopSounds(SoundType sound) {
        throw new UnsupportedOperationException("Unfortunately you've found a World that doesn't implement stopSounds");
    }

    @Override
    public void stopSounds(org.spongepowered.api.effect.sound.SoundCategory category) {
        throw new UnsupportedOperationException("Unfortunately you've found a World that doesn't implement stopSounds");
    }

    @Override
    public void stopSounds(SoundType sound, org.spongepowered.api.effect.sound.SoundCategory category) {
        throw new UnsupportedOperationException("Unfortunately you've found a World that doesn't implement stopSounds");
    }

    @Override
    public void playMusicDisc(Vector3i position, MusicDisc musicDiscType) {
        throw new UnsupportedOperationException("Unfortunately you've found a World that doesn't implement playMusicDisc");
    }

    @Override
    public void stopMusicDisc(Vector3i position) {
        throw new UnsupportedOperationException("Unfortunately you've found a World that doesn't implement stopMusicDisc");
    }

    @Override
    public void sendTitle(Title title) {
        throw new UnsupportedOperationException("Unfortunately you've found a World that doesn't implement sendTitle");
    }

    @Override
    public void sendBookView(BookView bookView) {
        throw new UnsupportedOperationException("Unfortunately you've found a World that doesn't implement sendBookView");
    }

    @Override
    public void sendBlockChange(int x, int y, int z, org.spongepowered.api.block.BlockState state) {
        throw new UnsupportedOperationException("Unfortunately you've found a World that doesn't implement sendBlockChange");
    }

    @Override
    public void resetBlockChange(int x, int y, int z) {
        throw new UnsupportedOperationException("Unfortunately you've found a World that doesn't implement resetBlockChange");
    }

    @Override
    public Server getServer() {
        return (Server) this.shadow$getServer();
    }
}
