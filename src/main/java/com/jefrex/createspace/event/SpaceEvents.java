package com.jefrex.createspace.event;

import com.jefrex.createspace.block.ModBlocks;
import com.jefrex.createspace.physic.DimensionConfig;
import com.jefrex.createspace.physic.SpacePhysicsHandler;
import com.simibubi.create.content.equipment.armor.BacktankUtil;
import com.simibubi.create.content.equipment.armor.DivingHelmetItem;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber
public class SpaceEvents {

    private static final int GRACE_TICKS = 0;
    private static final Map<UUID, Integer> noAirTicks = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        ResourceLocation dim = player.level().dimension().location();
        DimensionConfig cfg = DimensionConfig.get(dim);
        if (cfg == null) return; // вимір не в конфізі — ігноруємо

        UUID id = player.getUUID();

        if (isInAirField(player) || hasDivingGear(player)) {
            noAirTicks.remove(id);

            // витрачаємо повітря з балону тільки якщо не в атмосфері
            if (hasDivingGear(player) && !isInAirField(player)) {
                if (player.level().getGameTime() % 20 == 0) {
                    var backtanks = BacktankUtil.getAllWithAir(player);
                    if (!backtanks.isEmpty()) {
                        BacktankUtil.consumeAir(player, backtanks.get(0), 1);
                    }
                }
            }
            return;
        }

        int ticks = noAirTicks.getOrDefault(id, 0) + 1;
        noAirTicks.put(id, ticks);

        if (ticks > GRACE_TICKS) {
            // використовуємо vacuum_damage з конфігу виміру
            player.hurt(player.damageSources().freeze(), (float) cfg.vacuum_damage);
        }
    }

    @SubscribeEvent
    public static void onFallDamage(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();

        ResourceLocation dim = entity.level().dimension().location();
        DimensionConfig cfg = DimensionConfig.get(dim);
        if (cfg == null) return;

        if (SpacePhysicsHandler.isInAtmosphere(entity)) return;

        // використовуємо fall_damage_divider з конфігу виміру
        event.setDistance(event.getDistance() / (float) cfg.fall_damage_divider);
    }

    private static boolean isInAirField(Player player) {
        // позиція очей гравця
        BlockPos eyePos = BlockPos.containing(player.getEyePosition());
        BlockPos feetPos = player.blockPosition();

        for (BlockPos check : new BlockPos[]{feetPos, eyePos}) {
            if (player.level().getBlockState(check).getBlock() == ModBlocks.AIR_FIELD_AIR.get() ||
                    player.level().getBlockState(check).is(ModBlocks.ATMOSPHERE.get())) {
                return true;
            }
        }

        // sublevels
        var container = dev.ryanhcode.sable.api.sublevel.SubLevelContainer
                .getContainer(player.level());
        if (container == null) return false;

        for (var subLevel : container.getAllSubLevels()) {
            var localFeetPos = subLevel.logicalPose().transformPositionInverse(player.position());
            var localEyePos = subLevel.logicalPose().transformPositionInverse(player.getEyePosition());

            for (Vec3 localPos : new Vec3[]{localFeetPos, localEyePos}) {
                BlockPos lp = BlockPos.containing(localPos.x, localPos.y, localPos.z);
                try {
                    if (subLevel.getLevel().getBlockState(lp).getBlock() == ModBlocks.AIR_FIELD_AIR.get() ||
                            subLevel.getLevel().getBlockState(lp).is(ModBlocks.ATMOSPHERE.get())) {
                        return true;
                    }
                } catch (Exception ignored) {}
            }
        }
        return false;
    }

    private static boolean hasDivingGear(Player player) {
        return DivingHelmetItem.isWornBy(player) &&
                !BacktankUtil.getAllWithAir(player).isEmpty();
    }
}