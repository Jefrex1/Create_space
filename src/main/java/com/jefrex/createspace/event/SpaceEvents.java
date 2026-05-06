package com.jefrex.createspace.event;

import com.jefrex.createspace.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber
public class SpaceEvents {

    private static final ResourceLocation SPACE_DIM = ResourceLocation.fromNamespaceAndPath("create_space", "space");
    private static final int GRACE_TICKS = 20;
    private static final float DAMAGE_PER_TICK = 2.5f;

    private static final Map<UUID, Integer> noAirTicks = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        if (player.level().isClientSide()) return;
        if (!player.level().dimension().location().equals(SPACE_DIM)) return;

        UUID id = player.getUUID();

        if (isInAirField(player)) {
            noAirTicks.remove(id);
            return;
        }

        int ticks = noAirTicks.getOrDefault(id, 0) + 1;
        noAirTicks.put(id, ticks);

        if (ticks > GRACE_TICKS) {
            player.hurt(player.damageSources().freeze(), DAMAGE_PER_TICK);
        }
    }

    private static boolean isInAirField(Player player) {
        var feetPos = player.blockPosition();
        var headPos = feetPos.above();

        if (player.level().getBlockState(feetPos).getBlock() == ModBlocks.AIR_FIELD_AIR.get() ||
                player.level().getBlockState(headPos).getBlock() == ModBlocks.AIR_FIELD_AIR.get() ||
                player.level().getBlockState(feetPos).is(ModBlocks.ATMOSPHERE.get()) ||
                player.level().getBlockState(headPos).is(ModBlocks.ATMOSPHERE.get())) {
            return true;
        }

        var container = dev.ryanhcode.sable.api.sublevel.SubLevelContainer
                .getContainer(player.level());
        if (container == null) return false;

        for (var subLevel : container.getAllSubLevels()) {
            var localPos = subLevel.logicalPose()
                    .transformPositionInverse(player.position());
            var localFeet = BlockPos.containing(localPos.x, localPos.y, localPos.z);
            var localHead = localFeet.above();

            try {
                if (subLevel.getLevel().getBlockState(localFeet).getBlock() == ModBlocks.AIR_FIELD_AIR.get() ||
                        subLevel.getLevel().getBlockState(localHead).getBlock() == ModBlocks.AIR_FIELD_AIR.get() ||
                        subLevel.getLevel().getBlockState(localFeet).is(ModBlocks.ATMOSPHERE.get()) ||
                        subLevel.getLevel().getBlockState(localHead).is(ModBlocks.ATMOSPHERE.get())) {
                    return true;
                }
            } catch (Exception ignored) {}
        }
        return false;
    }
}