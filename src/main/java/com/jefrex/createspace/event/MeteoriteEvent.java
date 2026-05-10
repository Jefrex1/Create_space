package com.jefrex.createspace.event;

import com.jefrex.createspace.entity.MeteoriteEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;

@EventBusSubscriber(modid = "create_space", bus = EventBusSubscriber.Bus.GAME)
public class MeteoriteEvent {

    //summon create_space:meteorite ~-20 ~50 ~-13

    private static final int CHANCE = 50; // 1/1000 шанс кожен тік

    @SubscribeEvent
    public static void onWorldTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();

        for (ServerLevel level : server.getAllLevels()) {
            // Тільки в overworld і тільки якщо йде дощ
            if (!level.dimension().equals(Level.OVERWORLD)) continue;
            if (!level.isRaining()) continue;

            if (level.random.nextInt(CHANCE) != 0) continue;

            // Вибираємо випадкового гравця як ціль
            List<ServerPlayer> players = level.players();
            if (players.isEmpty()) continue;

            ServerPlayer target = players.get(level.random.nextInt(players.size()));
            spawnMeteorite(level, target);
        }
    }

    // Замість spawnFireball — спавнимо ентіті:
    private static void spawnMeteorite(ServerLevel level, ServerPlayer target) {
        int offsetX = level.random.nextInt(128) - 64;
        int offsetZ = level.random.nextInt(128) - 64;
        int x = (int) target.getX() + offsetX;
        int z = (int) target.getZ() + offsetZ;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);

        BlockPos impactPos = new BlockPos(x, y, z);

        // Спавнимо кастомну ентіті
        MeteoriteEntity meteorite = new MeteoriteEntity(level, impactPos);
        level.addFreshEntity(meteorite);

        target.sendSystemMessage(
                Component.literal("⚠ Метеорит летить!")
                        .withStyle(ChatFormatting.RED)
        );
    }

    private static void spawnFireball(ServerLevel level, BlockPos targetPos) {
        // Просто робимо вибух і кладемо блоки — без ентіті
        level.explode(
                null,
                targetPos.getX() + 0.5,
                targetPos.getY(),
                targetPos.getZ() + 0.5,
                5.0f,
                Level.ExplosionInteraction.TNT
        );

        // Кладемо обсидіан як кратер
        level.setBlock(targetPos, Blocks.OBSIDIAN.defaultBlockState(), 3);

        // Звук удару
        level.playSound(null, targetPos, SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.BLOCKS, 2.0f, 0.8f);
    }

    private static void placeMeteorite(ServerLevel level, BlockPos pos) {
        // Кладемо невеликий кратер з обсидіану + блок заліза як "метеорит"
        level.setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), 3);
        level.setBlock(pos.above(), Blocks.IRON_ORE.defaultBlockState(), 3);

        // Вибух для ефекту
        level.explode(
                null,
                pos.getX(), pos.getY(), pos.getZ(),
                4.0f, // радіус
                Level.ExplosionInteraction.TNT
        );
    }
}