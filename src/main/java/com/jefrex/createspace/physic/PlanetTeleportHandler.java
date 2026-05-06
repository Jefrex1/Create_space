package com.jefrex.createspace.physic;

import com.jefrex.createspace.client.renderer.PlanetConfig;
import com.jefrex.createspace.client.renderer.PlanetRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.*;

@EventBusSubscriber
public class PlanetTeleportHandler {

    private static final Map<UUID, Integer> cooldowns = new HashMap<>();
    private static final int COOLDOWN_TICKS = 20;

    private static final Map<UUID, String> playerOriginPlanet = new HashMap<>();

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        cooldowns.replaceAll((uuid, ticks) -> ticks - 1);
        cooldowns.values().removeIf(t -> t <= 0);

        for (ServerLevel level : server.getAllLevels()) {
            List<ServerPlayer> players = new ArrayList<>(level.players());

            for (ServerPlayer player : players) {
                if (cooldowns.containsKey(player.getUUID())) continue;

                for (PlanetConfig planet : PlanetRegistry.getPlanets()) {
                    if (planet.dimension() == null) continue;

                    double dx = player.getX() - planet.x();
                    double dy = player.getY() - planet.y();
                    double dz = player.getZ() - planet.z();

                    if (dx*dx + dy*dy + dz*dz <= (double) planet.radius() * planet.radius()) {
                        ResourceKey<Level> targetDim = ResourceKey.create(
                                net.minecraft.core.registries.Registries.DIMENSION,
                                ResourceLocation.parse(planet.dimension())
                        );

                        ServerLevel targetLevel = server.getLevel(targetDim);
                        if (targetLevel == null) continue;

                        playerOriginPlanet.put(player.getUUID(), planet.id());

                        var spawnPos = targetLevel.getSharedSpawnPos();
                        player.teleportTo(
                                targetLevel,
                                spawnPos.getX() + 0.5,
                                900,
                                spawnPos.getZ() + 0.5,
                                player.getYRot(),
                                player.getXRot()
                        );

                        cooldowns.put(player.getUUID(), COOLDOWN_TICKS);
                        break;
                    }
                }

                if (player.getY() > 1000) {
                    String originId = playerOriginPlanet.get(player.getUUID());

                    PlanetConfig targetPlanet = PlanetRegistry.getPlanets().stream()
                            .filter(p -> p.dimension() != null)
                            .filter(p -> originId != null
                                    ? p.id().equals(originId)
                                    : p.dimension().equals(level.dimension().location().toString()))
                            .findFirst()
                            .orElse(null);

                    if (targetPlanet == null) continue;

                    ResourceKey<Level> spaceDim = ResourceKey.create(
                            net.minecraft.core.registries.Registries.DIMENSION,
                            ResourceLocation.parse(targetPlanet.dimension())
                    );

                    ServerLevel spaceLevel = server.getLevel(
                            ResourceKey.create(
                                    net.minecraft.core.registries.Registries.DIMENSION,
                                    ResourceLocation.parse("create_space:space")
                            )
                    );

                    if (spaceLevel == null) continue;

                    double spawnX = targetPlanet.x() + targetPlanet.radius() * 1.05;
                    double spawnY = targetPlanet.y();
                    double spawnZ = targetPlanet.z();

                    player.teleportTo(
                            spaceLevel,
                            spawnX, spawnY, spawnZ,
                            player.getYRot(),
                            player.getXRot()
                    );

                    cooldowns.put(player.getUUID(), COOLDOWN_TICKS);
                }
            }
        }
    }
}