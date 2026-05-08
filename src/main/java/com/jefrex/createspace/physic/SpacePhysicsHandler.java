package com.jefrex.createspace.physic;

import com.jefrex.createspace.CreateSpace;
import com.jefrex.createspace.block.ModBlocks;
import com.jefrex.createspace.physic.DimensionConfig;
import com.simibubi.create.content.equipment.armor.BacktankUtil;
import com.simibubi.create.content.equipment.armor.DivingBootsItem;
import com.simibubi.create.content.equipment.armor.DivingHelmetItem;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = CreateSpace.MODID)
public class SpacePhysicsHandler {

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        ResourceLocation dim = entity.level().dimension().location();

        DimensionConfig cfg = DimensionConfig.get(dim);
        if (cfg == null) return; // вимір не в конфізі — ігноруємо

        boolean inAtmosphere = isInAtmosphere(entity, cfg);

        if (entity instanceof Player player) {
            applySpacePhysics(player, inAtmosphere, cfg);
        } else {
            applyGenericSpacePhysics(entity, inAtmosphere, cfg);
        }
    }

    @SubscribeEvent
    public static void onSpaceBreathe(LivingBreatheEvent event) {
        LivingEntity entity = event.getEntity();
        ResourceLocation dim = entity.level().dimension().location();

        if (!DimensionConfig.isSpaceDimension(dim)) return;

        if (DivingHelmetItem.isWornBy(entity) &&
                !BacktankUtil.getAllWithAir(entity).isEmpty()) {
            event.setCanBreathe(true);
            event.setRefillAirAmount(entity.getMaxAirSupply());
        }
    }

    private static void applySpacePhysics(Player player, boolean inAtmosphere, DimensionConfig cfg) {
        if (inAtmosphere || player.onGround() || player.isSwimming() || player.getAbilities().flying) {
            player.setNoGravity(false);
            player.getPersistentData().remove("last_vel_x");
            player.getPersistentData().remove("last_vel_y");
            player.getPersistentData().remove("last_vel_z");
            return;
        }

        player.setNoGravity(true);
        Vec3 currentVel = player.getDeltaMovement();
        var data = player.getPersistentData();

        double lastX = data.contains("last_vel_x") ? data.getDouble("last_vel_x") : currentVel.x;
        double lastY = data.contains("last_vel_y") ? data.getDouble("last_vel_y") : currentVel.y;
        double lastZ = data.contains("last_vel_z") ? data.getDouble("last_vel_z") : currentVel.z;

        if (player.horizontalCollision || player.verticalCollision) {
            double nextX = currentVel.x;
            double nextY = currentVel.y;
            double nextZ = currentVel.z;

            if (Math.abs(currentVel.x) < 0.005 && Math.abs(lastX) > 0.005)
                nextX = -lastX * cfg.bounce_factor;
            else nextX = lastX;

            if (Math.abs(currentVel.z) < 0.005 && Math.abs(lastZ) > 0.005)
                nextZ = -lastZ * cfg.bounce_factor;
            else nextZ = lastZ;

            nextY = player.verticalCollision ? -lastY * cfg.bounce_factor : lastY;
            currentVel = new Vec3(nextX, nextY, nextZ);
        }

        double finalX = currentVel.x * cfg.horizontal_stabilizer;
        double finalY = currentVel.y * cfg.vertical_stabilizer - cfg.gravity; // місячна гравітація
        double finalZ = currentVel.z * cfg.horizontal_stabilizer;

        Vec3 finalMotion = new Vec3(finalX, finalY, finalZ);

        if (finalMotion.lengthSqr() > cfg.terminal_velocity * cfg.terminal_velocity)
            finalMotion = finalMotion.scale(0.92);

        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (boots.getItem() instanceof DivingBootsItem)
            finalMotion = finalMotion.add(0, -0.005, 0);

        player.setDeltaMovement(finalMotion);
        data.putDouble("last_vel_x", finalMotion.x);
        data.putDouble("last_vel_y", finalMotion.y);
        data.putDouble("last_vel_z", finalMotion.z);

        if (player instanceof LivingEntity living) {
            living.xxa = 0; living.yya = 0; living.zza = 0;
        }
    }

    private static void applyGenericSpacePhysics(Entity entity, boolean inAtmosphere, DimensionConfig cfg) {
        if (inAtmosphere) {
            entity.setNoGravity(false);
            return;
        }
        entity.setNoGravity(true);
        Vec3 newMotion = entity.getDeltaMovement().scale(1.019);

        if (newMotion.lengthSqr() > cfg.terminal_velocity * cfg.terminal_velocity)
            newMotion = newMotion.scale(0.9);

        entity.setDeltaMovement(newMotion);
    }

    public static boolean isInAtmosphere(Entity entity, DimensionConfig cfg) {
        if (!cfg.has_atmosphere_blocks) return false; // місяць — атмосфери немає взагалі

        BlockPos pos = entity.blockPosition();
        if (isAtmosphereBlock(entity, pos) || isAtmosphereBlock(entity, pos.above())) return true;

        var container = dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(entity.level());
        if (container != null) {
            for (var subLevel : container.getAllSubLevels()) {
                Vec3 localPos = subLevel.logicalPose().transformPositionInverse(entity.position());
                BlockPos lp = BlockPos.containing(localPos.x, localPos.y, localPos.z);
                try {
                    if (subLevel.getLevel().getBlockState(lp).is(ModBlocks.ATMOSPHERE.get()) ||
                            subLevel.getLevel().getBlockState(lp.above()).is(ModBlocks.ATMOSPHERE.get()))
                        return true;
                } catch (Exception ignored) {}
            }
        }
        return false;
    }

    // Перевантаження для зворотної сумісності зі SpaceEvents
    public static boolean isInAtmosphere(Entity entity) {
        DimensionConfig cfg = DimensionConfig.get(entity.level().dimension().location());
        if (cfg == null) return false;
        return isInAtmosphere(entity, cfg);
    }

    private static boolean isAtmosphereBlock(Entity entity, BlockPos pos) {
        return entity.level().getBlockState(pos).is(ModBlocks.ATMOSPHERE.get());
    }
}