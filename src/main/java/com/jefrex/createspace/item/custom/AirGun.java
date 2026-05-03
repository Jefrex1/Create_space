package com.jefrex.createspace.item.custom;

import com.jefrex.createspace.CreateSpace;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

import net.minecraft.world.item.UseAnim;

public class AirGun extends Item {
    public AirGun(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        Vec3 lookAngle = player.getLookAngle();
        double power = 0.25;

        player.setDeltaMovement(
                -lookAngle.x * power,
                -lookAngle.y * power,
                -lookAngle.z * power
        );

        level.playSound((Player)null, player.getX(), player.getY(), player.getZ(), SoundEvents.WIND_CHARGE_BURST, SoundSource.NEUTRAL, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

        // якщо в космосі — оновлюємо збережену швидкість
        var data = player.getPersistentData();
        data.putDouble("space_vx", -lookAngle.x * power);
        data.putDouble("space_vy", -lookAngle.y * power);
        data.putDouble("space_vz", -lookAngle.z * power);
        data.putBoolean("space_was_on_ground", false);

        return InteractionResultHolder.success(player.getItemInHand(hand));
    }



}