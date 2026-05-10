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
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
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
    record PlotEntity(UUID oldUUID, CompoundTag nbt, double dx, double dy, double dz) {}

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

                // ============================================================
                // --- Вхід у планету (з overworld/space у planet dimension) ---
                // ============================================================
                for (PlanetConfig planet : PlanetRegistry.getPlanets()) {
                    if (planet.dimension() == null) continue;
                    double dx = player.getX() - planet.x();
                    double dy = player.getY() - planet.y();
                    double dz = player.getZ() - planet.z();
                    if (dx * dx + dy * dy + dz * dz > (double) planet.radius() * planet.radius()) continue;

                    ServerLevel targetLevel = server.getLevel(ResourceKey.create(
                            Registries.DIMENSION,
                            ResourceLocation.parse(planet.dimension())));
                    if (targetLevel == null) continue;

                    playerOriginPlanet.put(player.getUUID(), planet.id());

                    SubLevel trackingSubLevel = Sable.HELPER.getTrackingSubLevel(player);

                    if (trackingSubLevel instanceof ServerSubLevel serverSubLevel) {
                        // Гравець на суб-рівні — переносимо його разом з кораблем
                        UUID subLevelId = serverSubLevel.getUniqueId();
                        if (teleportedSubLevels.contains(subLevelId)) {
                            cooldowns.put(player.getUUID(), COOLDOWN_TICKS);
                            break;
                        }
                        teleportedSubLevels.add(subLevelId);

                        var spawnPos = targetLevel.getSharedSpawnPos();
                        double spawnX = spawnPos.getX() + 0.5;
                        double spawnY = 900;
                        double spawnZ = spawnPos.getZ() + 0.5;

                        transferSubLevel(
                                server, level, targetLevel,
                                serverSubLevel, player,
                                spawnX, spawnY, spawnZ
                        );
                    } else {
                        // Гравець без корабля
                        var spawnPos = targetLevel.getSharedSpawnPos();
                        player.teleportTo(targetLevel,
                                spawnPos.getX() + 0.5, 900, spawnPos.getZ() + 0.5,
                                player.getYRot(), player.getXRot());
                        cooldowns.put(player.getUUID(), COOLDOWN_TICKS);
                    }
                    break;
                }

                if (cooldowns.containsKey(player.getUUID())) continue;

                // ============================================================
                // --- Вихід з планети (вгору на Y>1000 → space)            ---
                // ============================================================
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

                        transferSubLevel(
                                server, level, spaceLevel,
                                serverSubLevel, player,
                                spawnX, spawnY, spawnZ
                        );
                    } else {
                        player.teleportTo(spaceLevel, spawnX, spawnY, spawnZ,
                                player.getYRot(), player.getXRot());
                        playerOriginPlanet.remove(player.getUUID());
                        cooldowns.put(player.getUUID(), COOLDOWN_TICKS);
                    }
                }
            }
        }
    }

    /**
     * Переносить суб-рівень (корабель) з одного виміру в інший,
     * разом з екіпажем та entities.
     */
    private static void transferSubLevel(
            net.minecraft.server.MinecraftServer server,
            ServerLevel fromLevel,
            ServerLevel toLevel,
            ServerSubLevel serverSubLevel,
            ServerPlayer triggeringPlayer,
            double spawnX, double spawnY, double spawnZ
    ) {
        UUID subLevelId = serverSubLevel.getUniqueId();

        // --- Зберігаємо стан фізики ---
        ServerSubLevelContainer container = SubLevelContainer.getContainer(fromLevel);
        if (container == null) return;
        SubLevelPhysicsSystem physicsSystem = container.physicsSystem();
        RigidBodyHandle handle = physicsSystem.getPhysicsHandle(serverSubLevel);

        Vector3d linearVelocity = Sable.HELPER.getVelocity(
                fromLevel, serverSubLevel.logicalPose().position(), new Vector3d());
        Vector3d angularVelocity = handle != null
                ? new Vector3d(handle.getAngularVelocity())
                : new Vector3d();
        Quaterniond orientation = new Quaterniond(serverSubLevel.logicalPose().orientation());

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

        double baseX = triggeringPlayer.getX();
        double baseY = triggeringPlayer.getY();
        double baseZ = triggeringPlayer.getZ();
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
        List<PlotEntity> plotEntities = new ArrayList<>();
        Map<UUID, UUID> vehicleToPassenger = new HashMap<>(); // vehicleUUID -> playerUUID

        if (plotMinX != Integer.MAX_VALUE) {
            AABB plotAABB = new AABB(plotMinX, plotMinY, plotMinZ,
                    plotMaxX + 1, plotMaxY + 1, plotMaxZ + 1);
            List<Entity> entitiesInPlot = plotLevel.getEntitiesOfClass(Entity.class, plotAABB,
                    e -> !(e instanceof ServerPlayer));

            for (Entity e : entitiesInPlot) {
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

        // --- Корабель порожній — просто телепортуємо гравців ---
        if (plotBlocks.isEmpty()) {
            container.removeSubLevel(serverSubLevel, SubLevelRemovalReason.REMOVED);
            for (ServerPlayer member : crew) {
                double[] offset = crewOffsets.get(member.getUUID());
                member.teleportTo(toLevel,
                        spawnX + offset[0], spawnY + offset[1], spawnZ + offset[2],
                        member.getYRot(), member.getXRot());
                playerOriginPlanet.remove(member.getUUID());
                cooldowns.put(member.getUUID(), COOLDOWN_TICKS);
            }
            return;
        }

        // --- Матеріалізуємо блоки у toLevel ---
        BlockPos spawnBlock = BlockPos.containing(spawnX, spawnY, spawnZ);
        List<BlockPos> worldTargetBlocks = new ArrayList<>();

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        for (PlotBlock pb : plotBlocks) {
            int dx = pb.plotPos().getX() - plotCenter.getX();
            int dy = pb.plotPos().getY() - plotCenter.getY();
            int dz = pb.plotPos().getZ() - plotCenter.getZ();
            BlockPos targetPos = spawnBlock.offset(dx, dy, dz);

            toLevel.setBlock(targetPos, pb.state(), 2);

            if (pb.nbt() != null) {
                BlockEntity newBe = toLevel.getBlockEntity(targetPos);
                if (newBe != null) {
                    CompoundTag tag = pb.nbt().copy();
                    tag.putInt("x", targetPos.getX());
                    tag.putInt("y", targetPos.getY());
                    tag.putInt("z", targetPos.getZ());
                    newBe.loadWithComponents(tag, toLevel.registryAccess());
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

        // --- Збираємо новий суб-рівень у toLevel ---
        ServerSubLevel newSubLevel;
        try {
            newSubLevel = SubLevelAssemblyHelper.assembleBlocks(
                    toLevel,
                    worldTargetBlocks.get(0),
                    worldTargetBlocks,
                    worldBounds
            );
        } catch (Exception e) {
            Sable.LOGGER.error("Failed to assemble sub-level in target dimension", e);
            return;
        }

        // --- Відновлюємо фізику ---
        ServerSubLevelContainer toContainer = SubLevelContainer.getContainer(toLevel);
        if (toContainer == null) return;
        SubLevelPhysicsSystem toPhysics = toContainer.physicsSystem();
        PhysicsPipeline toPipeline = toPhysics.getPipeline();

        toPipeline.teleport(newSubLevel, new Vector3d(spawnX, spawnY, spawnZ), orientation);
        newSubLevel.updateLastPose();
        toPipeline.addLinearAndAngularVelocity(newSubLevel, linearVelocity, angularVelocity);

        // --- Спавнимо entities у новому plot ---
        LevelPlot newPlot = newSubLevel.getPlot();
        ServerLevel newPlotLevel = (ServerLevel) newPlot.getSubLevel().getLevel();
        BlockPos newPlotCenter = newPlot.getCenterBlock();

        Map<UUID, Entity> spawnedEntities = new HashMap<>();

        for (PlotEntity pe : plotEntities) {
            CompoundTag tag = pe.nbt().copy();

            double newEX = newPlotCenter.getX() + pe.dx();
            double newEY = newPlotCenter.getY() + pe.dy();
            double newEZ = newPlotCenter.getZ() + pe.dz();

            tag.remove("UUID");
            ListTag posList = new ListTag();
            posList.add(DoubleTag.valueOf(newEX));
            posList.add(DoubleTag.valueOf(newEY));
            posList.add(DoubleTag.valueOf(newEZ));
            tag.put("Pos", posList);

            EntityType.create(tag, newPlotLevel).ifPresent(e -> {
                newPlotLevel.addFreshEntity(e);
                spawnedEntities.put(pe.oldUUID(), e);
            });
        }

        // --- Телепортуємо екіпаж ---
        for (ServerPlayer member : crew) {
            double[] offset = crewOffsets.get(member.getUUID());
            member.teleportTo(toLevel,
                    spawnX + offset[0], spawnY + offset[1], spawnZ + offset[2],
                    member.getYRot(), member.getXRot());
            playerOriginPlanet.remove(member.getUUID());
            cooldowns.put(member.getUUID(), COOLDOWN_TICKS);

            // Пересаджуємо на vehicle
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
    }
}