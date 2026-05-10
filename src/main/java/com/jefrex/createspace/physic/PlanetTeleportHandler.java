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
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.util.*;

@EventBusSubscriber
public class PlanetTeleportHandler {

    private static final Map<UUID, Integer> cooldowns = new HashMap<>();
    private static final int COOLDOWN_TICKS = 20;
    private static final Map<UUID, String> playerOriginPlanet = new HashMap<>();
    private static final Set<UUID> teleportedSubLevels = new HashSet<>();

    record PlotBlock(BlockPos plotPos, BlockState state, CompoundTag nbt) {}

    // Зберігаємо: UUID entity на якій сидів гравець -> UUID гравця
    // (заповнюємо до телепорту, застосовуємо після)
    private static final Map<UUID, UUID> playerVehicleMap = new HashMap<>(); // playerUUID -> vehicleUUID (старий, в plot)
    private static final Map<UUID, UUID> spawnedVehicleMap = new HashMap<>(); // старий vehicleUUID -> новий entity UUID в space

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        cooldowns.replaceAll((uuid, ticks) -> ticks - 1);
        cooldowns.values().removeIf(t -> t <= 0);
        teleportedSubLevels.clear();

        for (ServerLevel level : server.getAllLevels()) {
            for (ServerPlayer player : new ArrayList<>(level.players())) {
                if (cooldowns.containsKey(player.getUUID())) continue;

                // --- Вхід у планету ---
                for (PlanetConfig planet : PlanetRegistry.getPlanets()) {
                    if (planet.dimension() == null) continue;
                    double dx = player.getX() - planet.x();
                    double dy = player.getY() - planet.y();
                    double dz = player.getZ() - planet.z();
                    if (dx * dx + dy * dy + dz * dz <= (double) planet.radius() * planet.radius()) {
                        ServerLevel targetLevel = server.getLevel(ResourceKey.create(
                                Registries.DIMENSION,
                                ResourceLocation.parse(planet.dimension())));
                        if (targetLevel == null) continue;
                        playerOriginPlanet.put(player.getUUID(), planet.id());
                        var spawnPos = targetLevel.getSharedSpawnPos();
                        player.teleportTo(targetLevel,
                                spawnPos.getX() + 0.5, 900, spawnPos.getZ() + 0.5,
                                player.getYRot(), player.getXRot());
                        cooldowns.put(player.getUUID(), COOLDOWN_TICKS);
                        break;
                    }
                }

                if (cooldowns.containsKey(player.getUUID())) continue;

                // --- Вихід з планети ---
                if (player.getY() > 1000) {
                    String originId = playerOriginPlanet.get(player.getUUID());
                    PlanetConfig targetPlanet = PlanetRegistry.getPlanets().stream()
                            .filter(p -> p.dimension() != null)
                            .filter(p -> originId != null
                                    ? p.id().equals(originId)
                                    : p.dimension().equals(level.dimension().location().toString()))
                            .findFirst().orElse(null);
                    if (targetPlanet == null) continue;

                    ServerLevel spaceLevel = server.getLevel(ResourceKey.create(
                            Registries.DIMENSION,
                            ResourceLocation.parse("create_space:space")));
                    if (spaceLevel == null) continue;

                    double spawnX = targetPlanet.x() + targetPlanet.radius() * 1.05;
                    double spawnY = targetPlanet.y();
                    double spawnZ = targetPlanet.z();

                    SubLevel trackingSubLevel = Sable.HELPER.getTrackingSubLevel(player);

                    if (trackingSubLevel instanceof ServerSubLevel serverSubLevel) {
                        UUID subLevelId = serverSubLevel.getUniqueId();
                        if (teleportedSubLevels.contains(subLevelId)) {
                            cooldowns.put(player.getUUID(), COOLDOWN_TICKS);
                            continue;
                        }
                        teleportedSubLevels.add(subLevelId);

                        // --- Зберігаємо стан фізики ---
                        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
                        if (container == null) continue;
                        SubLevelPhysicsSystem physicsSystem = container.physicsSystem();
                        RigidBodyHandle handle = physicsSystem.getPhysicsHandle(serverSubLevel);

                        Vector3d linearVelocity = Sable.HELPER.getVelocity(
                                level, serverSubLevel.logicalPose().position(), new Vector3d());
                        Vector3d angularVelocity = handle != null
                                ? new Vector3d(handle.getAngularVelocity())
                                : new Vector3d();
                        Quaterniond orientation = new Quaterniond(
                                serverSubLevel.logicalPose().orientation());

                        // --- Збираємо екіпаж і їх offsets ---
                        List<ServerPlayer> crew = new ArrayList<>();
                        for (ServerLevel lvl : server.getAllLevels()) {
                            for (ServerPlayer other : lvl.players()) {
                                SubLevel otherSub = Sable.HELPER.getTrackingSubLevel(other);
                                if (otherSub != null && otherSub.getUniqueId().equals(subLevelId)) {
                                    crew.add(other);
                                }
                            }
                        }

                        double baseX = player.getX(), baseY = player.getY(), baseZ = player.getZ();
                        Map<UUID, double[]> crewOffsets = new HashMap<>();
                        for (ServerPlayer member : crew) {
                            crewOffsets.put(member.getUUID(), new double[]{
                                    member.getX() - baseX,
                                    member.getY() - baseY,
                                    member.getZ() - baseZ
                            });
                        }

                        // --- Читаємо блоки з plot ---
                        LevelPlot plot = serverSubLevel.getPlot();
                        ServerLevel plotLevel = (ServerLevel) plot.getSubLevel().getLevel();
                        BlockPos plotCenter = plot.getCenterBlock();

                        List<PlotBlock> plotBlocks = new ArrayList<>();
                        int plotMinX = Integer.MAX_VALUE, plotMinY = Integer.MAX_VALUE, plotMinZ = Integer.MAX_VALUE;
                        int plotMaxX = Integer.MIN_VALUE, plotMaxY = Integer.MIN_VALUE, plotMaxZ = Integer.MIN_VALUE;

                        for (PlotChunkHolder holder : plot.getLoadedChunks()) {
                            var chunk = holder.getChunk();
                            if (chunk == null) continue;
                            var chunkPos = chunk.getPos();
                            for (int x = chunkPos.getMinBlockX(); x <= chunkPos.getMaxBlockX(); x++) {
                                for (int z = chunkPos.getMinBlockZ(); z <= chunkPos.getMaxBlockZ(); z++) {
                                    for (int y = plotLevel.getMinBuildHeight(); y < plotLevel.getMaxBuildHeight(); y++) {
                                        BlockPos plotPos = new BlockPos(x, y, z);
                                        BlockState state = chunk.getBlockState(plotPos);
                                        if (state.isAir()) continue;

                                        CompoundTag nbt = null;
                                        BlockEntity be = plotLevel.getBlockEntity(plotPos);
                                        if (be != null) {
                                            nbt = be.saveWithFullMetadata(plotLevel.registryAccess());
                                        }

                                        plotBlocks.add(new PlotBlock(plotPos, state, nbt));

                                        plotMinX = Math.min(plotMinX, x);
                                        plotMinY = Math.min(plotMinY, y);
                                        plotMinZ = Math.min(plotMinZ, z);
                                        plotMaxX = Math.max(plotMaxX, x);
                                        plotMaxY = Math.max(plotMaxY, y);
                                        plotMaxZ = Math.max(plotMaxZ, z);
                                    }
                                }
                            }
                        }

                        // --- Збираємо entities з plot ---
                        // Знаходимо всіх non-player entities в plot bounds
                        // Зберігаємо: старий UUID -> NBT + offset від plotCenter
                        record PlotEntity(UUID oldUUID, CompoundTag nbt, double dx, double dy, double dz) {}
                        List<PlotEntity> plotEntities = new ArrayList<>();

                        // UUID vehicle -> UUID гравця що на ньому сидить
                        Map<UUID, UUID> vehicleToPassenger = new HashMap<>();

                        if (plotMinX != Integer.MAX_VALUE) {
                            AABB plotAABB = new AABB(plotMinX, plotMinY, plotMinZ, plotMaxX + 1, plotMaxY + 1, plotMaxZ + 1);
                            List<Entity> entitiesInPlot = plotLevel.getEntitiesOfClass(Entity.class, plotAABB,
                                    e -> !(e instanceof ServerPlayer));

                            for (Entity e : entitiesInPlot) {
                                // Запам'ятовуємо хто на чому сидить
                                for (ServerPlayer member : crew) {
                                    if (member.getVehicle() != null &&
                                            member.getVehicle().getUUID().equals(e.getUUID())) {
                                        vehicleToPassenger.put(e.getUUID(), member.getUUID());
                                        member.stopRiding();
                                    }
                                }

                                CompoundTag nbt = new CompoundTag();
                                e.save(nbt);

                                double edx = e.getX() - plotCenter.getX();
                                double edy = e.getY() - plotCenter.getY();
                                double edz = e.getZ() - plotCenter.getZ();

                                plotEntities.add(new PlotEntity(e.getUUID(), nbt, edx, edy, edz));
                                e.discard();
                            }
                        }

                        // --- Корабель порожній ---
                        if (plotBlocks.isEmpty()) {
                            container.removeSubLevel(serverSubLevel, SubLevelRemovalReason.REMOVED);
                            for (ServerPlayer member : crew) {
                                double[] offset = crewOffsets.get(member.getUUID());
                                member.teleportTo(spaceLevel,
                                        spawnX + offset[0], spawnY + offset[1], spawnZ + offset[2],
                                        member.getYRot(), member.getXRot());
                                playerOriginPlanet.remove(member.getUUID());
                                cooldowns.put(member.getUUID(), COOLDOWN_TICKS);
                            }
                            continue;
                        }

                        // --- Матеріалізуємо блоки у spaceLevel ---
                        BlockPos spawnBlock = BlockPos.containing(spawnX, spawnY, spawnZ);
                        List<BlockPos> worldTargetBlocks = new ArrayList<>();

                        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
                        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

                        for (PlotBlock pb : plotBlocks) {
                            int dx = pb.plotPos().getX() - plotCenter.getX();
                            int dy = pb.plotPos().getY() - plotCenter.getY();
                            int dz = pb.plotPos().getZ() - plotCenter.getZ();
                            BlockPos targetPos = spawnBlock.offset(dx, dy, dz);

                            spaceLevel.setBlock(targetPos, pb.state(), 2);

                            if (pb.nbt() != null) {
                                BlockEntity newBe = spaceLevel.getBlockEntity(targetPos);
                                if (newBe != null) {
                                    CompoundTag tag = pb.nbt().copy();
                                    tag.putInt("x", targetPos.getX());
                                    tag.putInt("y", targetPos.getY());
                                    tag.putInt("z", targetPos.getZ());
                                    newBe.loadWithComponents(tag, spaceLevel.registryAccess());
                                }
                            }

                            worldTargetBlocks.add(targetPos);

                            minX = Math.min(minX, targetPos.getX());
                            minY = Math.min(minY, targetPos.getY());
                            minZ = Math.min(minZ, targetPos.getZ());
                            maxX = Math.max(maxX, targetPos.getX());
                            maxY = Math.max(maxY, targetPos.getY());
                            maxZ = Math.max(maxZ, targetPos.getZ());
                        }

                        BoundingBox3i worldBounds = new BoundingBox3i(minX, minY, minZ, maxX, maxY, maxZ);

                        // --- Видаляємо старий суб-рівень ---
                        container.removeSubLevel(serverSubLevel, SubLevelRemovalReason.REMOVED);

                        // --- Збираємо новий суб-рівень у spaceLevel ---
                        ServerSubLevel newSubLevel;
                        try {
                            newSubLevel = SubLevelAssemblyHelper.assembleBlocks(
                                    spaceLevel,
                                    worldTargetBlocks.get(0),
                                    worldTargetBlocks,
                                    worldBounds
                            );
                        } catch (Exception e) {
                            Sable.LOGGER.error("Failed to assemble sub-level in space dimension", e);
                            continue;
                        }

                        // --- Відновлюємо фізику ---
                        ServerSubLevelContainer spaceContainer = SubLevelContainer.getContainer(spaceLevel);
                        if (spaceContainer == null) continue;
                        SubLevelPhysicsSystem spacePhysics = spaceContainer.physicsSystem();
                        PhysicsPipeline spacePipeline = spacePhysics.getPipeline();

                        spacePipeline.teleport(newSubLevel,
                                new Vector3d(spawnX, spawnY, spawnZ), orientation);
                        newSubLevel.updateLastPose();
                        spacePipeline.addLinearAndAngularVelocity(newSubLevel, linearVelocity, angularVelocity);

                        // --- Спавнимо entities у spaceLevel (вже в plot новго суб-рівня) ---
                        // Новий plot center
                        LevelPlot newPlot = newSubLevel.getPlot();
                        ServerLevel newPlotLevel = (ServerLevel) newPlot.getSubLevel().getLevel();
                        BlockPos newPlotCenter = newPlot.getCenterBlock();

                        // oldUUID -> новий Entity (щоб потім посадити гравців)
                        Map<UUID, Entity> spawnedEntities = new HashMap<>();

                        for (PlotEntity pe : plotEntities) {
                            CompoundTag tag = pe.nbt().copy();

                            // Нова позиція = новий plot center + той самий offset
                            double newEX = newPlotCenter.getX() + pe.dx();
                            double newEY = newPlotCenter.getY() + pe.dy();
                            double newEZ = newPlotCenter.getZ() + pe.dz();

                            // Оновлюємо позицію в NBT
                            tag.remove("UUID"); // щоб отримав новий UUID
                            var posList = new net.minecraft.nbt.ListTag();
                            posList.add(net.minecraft.nbt.DoubleTag.valueOf(newEX));
                            posList.add(net.minecraft.nbt.DoubleTag.valueOf(newEY));
                            posList.add(net.minecraft.nbt.DoubleTag.valueOf(newEZ));
                            tag.put("Pos", posList);

                            Optional<Entity> spawned = EntityType.create(tag, newPlotLevel);
                            spawned.ifPresent(e -> {
                                newPlotLevel.addFreshEntity(e);
                                spawnedEntities.put(pe.oldUUID(), e);
                            });
                        }

                        // --- Телепортуємо екіпаж ---
                        for (ServerPlayer member : crew) {
                            double[] offset = crewOffsets.get(member.getUUID());
                            member.teleportTo(spaceLevel,
                                    spawnX + offset[0], spawnY + offset[1], spawnZ + offset[2],
                                    member.getYRot(), member.getXRot());
                            playerOriginPlanet.remove(member.getUUID());
                            cooldowns.put(member.getUUID(), COOLDOWN_TICKS);

                            // --- Пересаджуємо на vehicle якщо треба ---
                            // Шукаємо чи цей гравець мав vehicle
                            for (Map.Entry<UUID, UUID> entry : vehicleToPassenger.entrySet()) {
                                if (entry.getValue().equals(member.getUUID())) {
                                    Entity newVehicle = spawnedEntities.get(entry.getKey());
                                    if (newVehicle != null) {
                                        member.startRiding(newVehicle, true);
                                    }
                                    break;
                                }
                            }
                        }

                    } else {
                        // Гравець летить сам, без суб-рівня
                        player.teleportTo(spaceLevel, spawnX, spawnY, spawnZ,
                                player.getYRot(), player.getXRot());
                        playerOriginPlanet.remove(player.getUUID());
                        cooldowns.put(player.getUUID(), COOLDOWN_TICKS);
                    }
                }
            }
        }
    }
}