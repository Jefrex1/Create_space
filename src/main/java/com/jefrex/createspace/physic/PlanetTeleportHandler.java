package com.jefrex.createspace.physic;

import com.jefrex.createspace.client.renderer.PlanetConfig;
import com.jefrex.createspace.client.renderer.PlanetRegistry;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@EventBusSubscriber
public class PlanetTeleportHandler {

    private static final int COOLDOWN_TICKS = 20;

    private static final int STAGING_BASE_X    = 20_000_000;
    private static final int STAGING_Y         = 64;
    private static final int STAGING_BASE_Z    = 20_000_000;
    private static final int STAGING_SLOT_SIZE = 512;
    private static final AtomicInteger stagingSlotCounter = new AtomicInteger(0);

    private static final Map<UUID, Integer> cooldowns          = new HashMap<>();
    private static final Map<UUID, String>  playerOriginPlanet = new HashMap<>();
    private static final Map<UUID, Long>    teleportedSubLevels = new HashMap<>();

    record SavedBlock(BlockState state, CompoundTag nbt, int dx, int dy, int dz) {}

    // =========================================================================

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        cooldowns.remove(id);
        playerOriginPlanet.remove(id);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        long currentTick = server.getTickCount();

        cooldowns.replaceAll((uuid, ticks) -> ticks - 1);
        cooldowns.values().removeIf(t -> t <= 0);
        teleportedSubLevels.values().removeIf(tick -> currentTick - tick > 2L);

        for (ServerLevel level : server.getAllLevels()) {
            String dimensionId = level.dimension().location().toString();

            for (ServerPlayer player : new ArrayList<>(level.players())) {
                if (cooldowns.containsKey(player.getUUID())) continue;

                // --- Вхід у планету ---
                PlanetConfig enteredPlanet = findPlanetAtPosition(
                        player.getX(), player.getY(), player.getZ());

                if (enteredPlanet != null && enteredPlanet.dimension() != null) {
                    ServerLevel targetLevel = server.getLevel(ResourceKey.create(
                            Registries.DIMENSION,
                            ResourceLocation.parse(enteredPlanet.dimension())));

                    if (targetLevel != null && targetLevel != level) {
                        playerOriginPlanet.put(player.getUUID(), enteredPlanet.id());
                        BlockPos spawnPos = targetLevel.getSharedSpawnPos();
                        handleTeleport(server, level, targetLevel, player,
                                spawnPos.getX() + 0.5, 800, spawnPos.getZ() + 0.5,
                                false);
                        continue;
                    }
                }

                if (cooldowns.containsKey(player.getUUID())) continue;

                // --- Вихід з планети ---
                boolean isInSpace = dimensionId.equals("create_space:space");
                if (!isInSpace && player.getY() > 1000) {
                    String originId = playerOriginPlanet.get(player.getUUID());
                    PlanetConfig targetPlanet = PlanetRegistry.getPlanets().stream()
                            .filter(p -> p.dimension() != null)
                            .filter(p -> originId != null
                                    ? p.id().equals(originId)
                                    : p.dimension().equals(dimensionId))
                            .findFirst().orElse(null);
                    if (targetPlanet == null) continue;

                    ServerLevel spaceLevel = server.getLevel(ResourceKey.create(
                            Registries.DIMENSION,
                            ResourceLocation.parse("create_space:space")));
                    if (spaceLevel == null) continue;

                    handleTeleport(server, level, spaceLevel, player,
                            targetPlanet.x() + targetPlanet.radius() * 1.05,
                            targetPlanet.y(),
                            targetPlanet.z(),
                            true);
                }
            }
        }
    }

    // =========================================================================

    private static PlanetConfig findPlanetAtPosition(double x, double y, double z) {
        for (PlanetConfig planet : PlanetRegistry.getPlanets()) {
            double dx = x - planet.x(), dy = y - planet.y(), dz = z - planet.z();
            double r = planet.radius();
            if (dx * dx + dy * dy + dz * dz <= r * r) return planet;
        }
        return null;
    }

    private static void handleTeleport(
            MinecraftServer server,
            ServerLevel fromLevel, ServerLevel toLevel,
            ServerPlayer player,
            double spawnX, double spawnY, double spawnZ,
            boolean clearOrigin
    ) {
        SubLevel tracking = Sable.HELPER.getTrackingSubLevel(player);

        if (tracking instanceof ServerSubLevel serverSubLevel) {
            UUID subLevelId = serverSubLevel.getUniqueId();
            if (teleportedSubLevels.containsKey(subLevelId)) {
                cooldowns.put(player.getUUID(), COOLDOWN_TICKS);
                return;
            }
            teleportedSubLevels.put(subLevelId, (long) server.getTickCount());
            transferSubLevel(server, fromLevel, toLevel, serverSubLevel,
                    spawnX, spawnY, spawnZ, clearOrigin);
        } else {
            player.teleportTo(toLevel, spawnX, spawnY, spawnZ,
                    player.getYRot(), player.getXRot());
            if (clearOrigin) playerOriginPlanet.remove(player.getUUID());
            cooldowns.put(player.getUUID(), COOLDOWN_TICKS);
        }
    }

    // =========================================================================

    private static void transferSubLevel(
            MinecraftServer server,
            ServerLevel fromLevel, ServerLevel toLevel,
            ServerSubLevel serverSubLevel,
            double spawnX, double spawnY, double spawnZ,
            boolean clearOrigin
    ) {
        ServerSubLevelContainer container = SubLevelContainer.getContainer(fromLevel);
        if (container == null) {
            Sable.LOGGER.error("[PlanetTeleport] No SubLevelContainer for fromLevel");
            return;
        }

        RigidBodyHandle handle = container.physicsSystem().getPhysicsHandle(serverSubLevel);
        Vector3d linearVelocity  = Sable.HELPER.getVelocity(
                fromLevel, serverSubLevel.logicalPose().position(), new Vector3d());
        Vector3d angularVelocity = handle != null
                ? new Vector3d(handle.getAngularVelocity()) : new Vector3d();
        Quaterniond orientation  = new Quaterniond(serverSubLevel.logicalPose().orientation());

        UUID subLevelId = serverSubLevel.getUniqueId();
        List<ServerPlayer> crew = collectCrew(server, subLevelId);

        Vector3d subCenter = serverSubLevel.logicalPose().position();
        Map<UUID, double[]> crewOffsets = new HashMap<>();
        for (ServerPlayer member : crew) {
            crewOffsets.put(member.getUUID(), new double[]{
                    member.getX() - subCenter.x,
                    member.getY() - subCenter.y,
                    member.getZ() - subCenter.z
            });
        }

        // --- Читаємо блоки з plot ---
        LevelPlot plot        = serverSubLevel.getPlot();
        ServerLevel plotLevel = (ServerLevel) plot.getSubLevel().getLevel();
        BlockPos plotCenter   = plot.getCenterBlock();

        List<SavedBlock> savedBlocks = readPlotBlocks(plotLevel, plot, plotCenter);

        if (savedBlocks.isEmpty()) {
            Sable.LOGGER.warn("[PlanetTeleport] SubLevel {} has no blocks", subLevelId);
            container.removeSubLevel(serverSubLevel, SubLevelRemovalReason.REMOVED);
            teleportCrew(crew, crewOffsets, toLevel, spawnX, spawnY, spawnZ, clearOrigin);
            return;
        }

        // --- Видаляємо старий суб-рівень ---
        container.removeSubLevel(serverSubLevel, SubLevelRemovalReason.REMOVED);

        // --- Перевіряємо контейнер toLevel ---
        ServerSubLevelContainer toContainer = SubLevelContainer.getContainer(toLevel);
        if (toContainer == null) {
            Sable.LOGGER.error("[PlanetTeleport] No SubLevelContainer for toLevel");
            teleportCrew(crew, crewOffsets, toLevel, spawnX, spawnY, spawnZ, clearOrigin);
            return;
        }

        // --- Staging area ---
        int slot = stagingSlotCounter.getAndUpdate(v -> v >= 10_000 ? 0 : v + 1);
        BlockPos stagingAnchor = new BlockPos(
                STAGING_BASE_X,
                STAGING_Y,
                STAGING_BASE_Z + slot * STAGING_SLOT_SIZE
        );

        List<BlockPos> stagingPositions = new ArrayList<>(savedBlocks.size());
        int[] wb = {
                Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE,
                Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE
        };

        for (SavedBlock sb : savedBlocks) {
            BlockPos stagingPos = stagingAnchor.offset(sb.dx(), sb.dy(), sb.dz());
            stagingPositions.add(stagingPos);
            toLevel.setBlock(stagingPos, sb.state(), 2 | 16);

            if (sb.nbt() != null) {
                BlockEntity be = toLevel.getBlockEntity(stagingPos);
                if (be != null) {
                    CompoundTag tag = sb.nbt().copy();
                    tag.putInt("x", stagingPos.getX());
                    tag.putInt("y", stagingPos.getY());
                    tag.putInt("z", stagingPos.getZ());
                    be.loadWithComponents(tag, toLevel.registryAccess());
                }
            }

            if (stagingPos.getX() < wb[0]) wb[0] = stagingPos.getX();
            if (stagingPos.getY() < wb[1]) wb[1] = stagingPos.getY();
            if (stagingPos.getZ() < wb[2]) wb[2] = stagingPos.getZ();
            if (stagingPos.getX() > wb[3]) wb[3] = stagingPos.getX();
            if (stagingPos.getY() > wb[4]) wb[4] = stagingPos.getY();
            if (stagingPos.getZ() > wb[5]) wb[5] = stagingPos.getZ();
        }

        BoundingBox3i stagingBounds = new BoundingBox3i(
                wb[0], wb[1], wb[2], wb[3], wb[4], wb[5]);

        // --- assembleBlocks ---
        ServerSubLevel newSubLevel;
        try {
            newSubLevel = SubLevelAssemblyHelper.assembleBlocks(
                    toLevel,
                    stagingPositions.getFirst(),
                    stagingPositions,
                    stagingBounds
            );
        } catch (Exception e) {
            Sable.LOGGER.error("[PlanetTeleport] assembleBlocks failed", e);
            cleanupStaging(toLevel, stagingPositions);
            teleportCrew(crew, crewOffsets, toLevel, spawnX, spawnY, spawnZ, clearOrigin);
            return;
        }

        // --- Фізика ---
        PhysicsPipeline toPipeline = toContainer.physicsSystem().getPipeline();
        toPipeline.teleport(newSubLevel, new Vector3d(spawnX, spawnY, spawnZ), orientation);
        newSubLevel.updateLastPose();
        toPipeline.addLinearAndAngularVelocity(newSubLevel, linearVelocity, angularVelocity);

        // --- Телепортуємо екіпаж ---
        teleportCrew(crew, crewOffsets, toLevel, spawnX, spawnY, spawnZ, clearOrigin);
    }

    // =========================================================================

    private static void cleanupStaging(ServerLevel level, List<BlockPos> positions) {
        for (BlockPos pos : positions) {
            try { level.removeBlock(pos, false); }
            catch (Exception ignored) {}
        }
    }

    private static List<ServerPlayer> collectCrew(MinecraftServer server, UUID subLevelId) {
        List<ServerPlayer> crew = new ArrayList<>();
        for (ServerLevel lvl : server.getAllLevels()) {
            for (ServerPlayer other : lvl.players()) {
                SubLevel otherSub = Sable.HELPER.getTrackingSubLevel(other);
                if (otherSub != null && otherSub.getUniqueId().equals(subLevelId))
                    crew.add(other);
            }
        }
        return crew;
    }

    private static List<SavedBlock> readPlotBlocks(
            ServerLevel plotLevel, LevelPlot plot, BlockPos plotCenter
    ) {
        List<SavedBlock> blocks = new ArrayList<>();
        for (PlotChunkHolder holder : plot.getLoadedChunks()) {
            var chunk = holder.getChunk();
            if (chunk == null) continue;
            var chunkPos = chunk.getPos();
            for (int x = chunkPos.getMinBlockX(); x <= chunkPos.getMaxBlockX(); x++) {
                for (int z = chunkPos.getMinBlockZ(); z <= chunkPos.getMaxBlockZ(); z++) {
                    for (int y = plotLevel.getMinBuildHeight(); y < plotLevel.getMaxBuildHeight(); y++) {
                        BlockPos pos     = new BlockPos(x, y, z);
                        BlockState state = chunk.getBlockState(pos);
                        if (state.isAir()) continue;

                        CompoundTag nbt = null;
                        BlockEntity be  = plotLevel.getBlockEntity(pos);
                        if (be != null)
                            nbt = be.saveWithFullMetadata(plotLevel.registryAccess());

                        blocks.add(new SavedBlock(
                                state, nbt,
                                x - plotCenter.getX(),
                                y - plotCenter.getY(),
                                z - plotCenter.getZ()
                        ));
                    }
                }
            }
        }
        return blocks;
    }

    private static void teleportCrew(
            List<ServerPlayer> crew,
            Map<UUID, double[]> offsets,
            ServerLevel toLevel,
            double spawnX, double spawnY, double spawnZ,
            boolean clearOrigin
    ) {
        for (ServerPlayer member : crew) {
            double[] offset = offsets.getOrDefault(member.getUUID(), new double[]{0, 0, 0});
            member.teleportTo(toLevel,
                    spawnX + offset[0],
                    spawnY + offset[1],
                    spawnZ + offset[2],
                    member.getYRot(), member.getXRot());
            if (clearOrigin) playerOriginPlanet.remove(member.getUUID());
            cooldowns.put(member.getUUID(), COOLDOWN_TICKS);
        }
    }
}