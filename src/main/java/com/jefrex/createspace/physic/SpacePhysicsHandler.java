package com.jefrex.createspace.physic;

import com.jefrex.createspace.CreateSpace;
import com.jefrex.createspace.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = CreateSpace.MODID)
public class SpacePhysicsHandler {

    private static final ResourceKey SPACE_DIM = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath("create_space", "space")
    );

    private static final double HORIZONTAL_STABILIZER = 1.095;
    private static final double VERTICAL_STABILIZER = 1.018;
    private static final double TERMINAL_VELOCITY = 1.5;
    private static final double BOUNCE_FACTOR = 0.5; // Енергія відскоку (50%)

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (!entity.level().dimension().equals(SPACE_DIM)) return;

        boolean inAtmosphere = isInAtmosphere(entity);

        if (entity instanceof Player player) {
            applySpacePhysics(player, inAtmosphere);
        } else {
            applyGenericSpacePhysics(entity, inAtmosphere);
        }
    }

    private static void applySpacePhysics(Player player, boolean inAtmosphere) {
        if (inAtmosphere || player.onGround() || player.isSwimming() || player.getAbilities().flying) {
            player.setNoGravity(false);
            // Чистимо дані, щоб уникнути фантомних відскоків
            player.getPersistentData().remove("last_vel_x");
            player.getPersistentData().remove("last_vel_y");
            player.getPersistentData().remove("last_vel_z");
            return;
        }

        player.setNoGravity(true);
        Vec3 currentVel = player.getDeltaMovement();
        var data = player.getPersistentData();

        // Отримуємо швидкість з минулого тіку
        double lastX = data.contains("last_vel_x") ? data.getDouble("last_vel_x") : currentVel.x;
        double lastY = data.contains("last_vel_y") ? data.getDouble("last_vel_y") : currentVel.y;
        double lastZ = data.contains("last_vel_z") ? data.getDouble("last_vel_z") : currentVel.z;

        // --- ЛОГІКА РОЗУМНОГО ВІДСКОКУ "БІЛЬЯРДНА КУЛЯ" ---
        if (player.horizontalCollision || player.verticalCollision) {
            double nextX = currentVel.x;
            double nextY = currentVel.y;
            double nextZ = currentVel.z;

            // Перевірка удару об стіну по осі X
            // Якщо швидкість по X впала до мінімуму, а була значною — інвертуємо її
            if (Math.abs(currentVel.x) < 0.005 && Math.abs(lastX) > 0.005) {
                nextX = -lastX * BOUNCE_FACTOR;
            } else {
                // Якщо удару по X не було, відновлюємо інерцію з пам'яті (щоб не зупинятися при боковому ковзанні)
                nextX = lastX;
            }

            // Перевірка удару об стіну по осі Z
            if (Math.abs(currentVel.z) < 0.005 && Math.abs(lastZ) > 0.005) {
                nextZ = -lastZ * BOUNCE_FACTOR;
            } else {
                nextZ = lastZ;
            }

            // Перевірка вертикального удару (стеля/підлога)
            if (player.verticalCollision) {
                nextY = -lastY * BOUNCE_FACTOR;
            } else {
                nextY = lastY;
            }

            currentVel = new Vec3(nextX, nextY, nextZ);
        }

        // --- КОМПЕНСАЦІЯ ТЕРТЯ ТА СТАБІЛІЗАЦІЯ ---
        double finalX = currentVel.x * HORIZONTAL_STABILIZER;
        double finalY = currentVel.y * VERTICAL_STABILIZER;
        double finalZ = currentVel.z * HORIZONTAL_STABILIZER;

        Vec3 finalMotion = new Vec3(finalX, finalY, finalZ);

        // М'яке обмеження швидкості для стабільності двигуна
        if (finalMotion.lengthSqr() > TERMINAL_VELOCITY * TERMINAL_VELOCITY) {
            finalMotion = finalMotion.scale(0.92);
        }

        player.setDeltaMovement(finalMotion);

        // Оновлюємо пам'ять швидкості для наступного тіку
        data.putDouble("last_vel_x", finalMotion.x);
        data.putDouble("last_vel_y", finalMotion.y);
        data.putDouble("last_vel_z", finalMotion.z);

        // Повністю вимикаємо WASD-маневрування ногами
        if (player instanceof LivingEntity living) {
            living.xxa = 0; living.yya = 0; living.zza = 0;
        }
    }

    private static void applyGenericSpacePhysics(Entity entity, boolean inAtmosphere) {
        if (inAtmosphere) {
            entity.setNoGravity(false);
            return;
        }

        entity.setNoGravity(true);
        Vec3 motion = entity.getDeltaMovement();

        // Для звичайних об'єктів просто підтримуємо інерцію
        double cancel = 1.019;
        Vec3 newMotion = motion.scale(cancel);

        if (newMotion.lengthSqr() > TERMINAL_VELOCITY * TERMINAL_VELOCITY) {
            newMotion = newMotion.scale(0.9);
        }

        entity.setDeltaMovement(newMotion);
    }

    private static boolean isInAtmosphere(Entity entity) {
        BlockPos pos = entity.blockPosition();
        if (isAtmosphereBlock(entity, pos) || isAtmosphereBlock(entity, pos.above())) {
            return true;
        }

        var container = dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(entity.level());
        if (container != null) {
            for (var subLevel : container.getAllSubLevels()) {
                Vec3 localPos = subLevel.logicalPose().transformPositionInverse(entity.position());
                BlockPos lp = BlockPos.containing(localPos.x, localPos.y, localPos.z);
                try {
                    if (subLevel.getLevel().getBlockState(lp).is(ModBlocks.ATMOSPHERE.get()) ||
                            subLevel.getLevel().getBlockState(lp.above()).is(ModBlocks.ATMOSPHERE.get())) {
                        return true;
                    }
                } catch (Exception ignored) {}
            }
        }
        return false;
    }

    private static boolean isAtmosphereBlock(Entity entity, BlockPos pos) {
        return entity.level().getBlockState(pos).is(ModBlocks.ATMOSPHERE.get());
    }
}