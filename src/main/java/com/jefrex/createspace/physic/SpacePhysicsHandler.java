package com.jefrex.createspace.physic;

import com.jefrex.createspace.CreateSpace;
import com.jefrex.createspace.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = CreateSpace.MODID)
public class SpacePhysicsHandler {

    private static final ResourceKey SPACE_DIM = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath("create_space", "space")
    );

    private static final double IMPULSE_SCALE = 0.33;

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        var entity = event.getEntity();
        if (!entity.level().dimension().equals(SPACE_DIM)) return;

        if (entity instanceof Player player) {
            handlePlayer(player);
        } else {
            handleGenericEntity(entity);
        }
    }

    private static void handlePlayer(Player player) {
        if (isInAtmosphere(player)) {
            player.getPersistentData().remove("space_vx");
            player.getPersistentData().remove("space_vy");
            player.getPersistentData().remove("space_vz");
            player.getPersistentData().remove("space_was_on_ground");
            return;
        }

        var data = player.getPersistentData();
        var delta = player.getDeltaMovement();
        boolean onGround = player.onGround();

        if (onGround) {
            data.remove("space_vx");
            data.remove("space_vy");
            data.remove("space_vz");
            data.putBoolean("space_was_on_ground", true);
            return;
        }

        if (data.getBoolean("space_was_on_ground")) {
            data.putDouble("space_vx", delta.x);
            data.putDouble("space_vy", delta.y);
            data.putDouble("space_vz", delta.z);
            data.putBoolean("space_was_on_ground", false);
            return;
        }

        if (!data.contains("space_vx")) {
            data.putDouble("space_vx", delta.x);
            data.putDouble("space_vy", delta.y);
            data.putDouble("space_vz", delta.z);
            return;
        }

        double vx = data.getDouble("space_vx");
        double vy = data.getDouble("space_vy");
        double vz = data.getDouble("space_vz");

        if (Math.abs(delta.x) < Math.abs(vx) * 0.5) vx = delta.x;
        if (Math.abs(delta.y) < Math.abs(vy) * 0.5) vy = delta.y;
        if (Math.abs(delta.z) < Math.abs(vz) * 0.5) vz = delta.z;

        player.setDeltaMovement(vx, vy, vz);
        data.putDouble("space_vx", vx);
        data.putDouble("space_vy", vy);
        data.putDouble("space_vz", vz);
    }

    private static void handleGenericEntity(Entity entity) {
        if (isInAtmosphere(entity)) {
            entity.setNoGravity(false);
            return;
        }

        entity.setNoGravity(true);
        var delta = entity.getDeltaMovement();

        if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
            CreateSpace.LOGGER.info("before: y={}, onGround={}", delta.y, entity.onGround());
            living.setDeltaMovement(delta.x / 0.98, delta.y / 0.98, delta.z / 0.98);
            living.setJumping(false);
            living.xxa = 0;
            living.yya = 0;
            living.zza = 0;
            CreateSpace.LOGGER.info("after: y={}", living.getDeltaMovement().y);
        } else {
            entity.setDeltaMovement(delta.x / 0.98, delta.y / 0.98, delta.z / 0.98);
        }
    }

    private static boolean isInAtmosphere(Entity entity) {
        var feetPos = entity.blockPosition();
        var headPos = feetPos.above();

        if (entity.level().getBlockState(feetPos).is(ModBlocks.ATMOSPHERE.get()) ||
                entity.level().getBlockState(headPos).is(ModBlocks.ATMOSPHERE.get())) {
            return true;
        }

        var container = dev.ryanhcode.sable.api.sublevel.SubLevelContainer
                .getContainer(entity.level());
        if (container == null) return false;

        for (var subLevel : container.getAllSubLevels()) {
            var localPos = subLevel.logicalPose()
                    .transformPositionInverse(entity.position());
            var localFeet = BlockPos.containing(localPos.x, localPos.y, localPos.z);
            var localHead = localFeet.above();

            try {
                if (subLevel.getLevel().getBlockState(localFeet)
                        .is(ModBlocks.ATMOSPHERE.get()) ||
                        subLevel.getLevel().getBlockState(localHead)
                                .is(ModBlocks.ATMOSPHERE.get())) {
                    return true;
                }
            } catch (Exception ignored) {}
        }
        return false;
    }
}