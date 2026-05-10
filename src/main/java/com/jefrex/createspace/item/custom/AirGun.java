package com.jefrex.createspace.item.custom;

import com.jefrex.createspace.network.AirGunShootPayload;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public class AirGun extends Item {

    public static final int MAX_CHARGES = 3;
    private static final String TAG_CHARGE = "air_gun_charge";
    private static final int COOLDOWN_TICKS = 10;

    public static ItemStack getAirGunStack(Player player) {
        if (player.getMainHandItem().getItem() instanceof AirGun)
            return player.getMainHandItem();
        if (player.getOffhandItem().getItem() instanceof AirGun)
            return player.getOffhandItem();
        return ItemStack.EMPTY;
    }

    public AirGun(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.getCooldowns().isOnCooldown(stack.getItem())) {
            return InteractionResultHolder.fail(stack);
        }

        int current = getCharge(stack);
        if (current < MAX_CHARGES) {
            if (!level.isClientSide()) {
                setCharge(stack, current + 1);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.DISPENSER_DISPENSE, SoundSource.PLAYERS, 0.5F, 1.2F);
            }
            player.getCooldowns().addCooldown(stack.getItem(), COOLDOWN_TICKS);
            return InteractionResultHolder.success(stack);
        }

        return InteractionResultHolder.pass(stack);
    }

    public static void shoot(Player player, ItemStack stack, Level level) {
        int charge = getCharge(stack);
        if (charge <= 0) return;

        int amountToDamage = charge;

        setCharge(stack, 0);

        Vec3 lookAngle = player.getLookAngle();
        double power = 0.15 * charge;

        double pushX = -lookAngle.x * power;
        double pushY = -lookAngle.y * power;
        double pushZ = -lookAngle.z * power;

        player.setDeltaMovement(player.getDeltaMovement().add(pushX, pushY, pushZ));
        player.hurtMarked = true;

        if (!level.isClientSide()) {
            stack.hurtAndBreak(amountToDamage, player, Player.getSlotForHand(player.getUsedItemHand()));
        }

        var data = player.getPersistentData();

        if (data.contains("space_vx")) {
            data.putDouble("space_vx", data.getDouble("space_vx") + pushX);
            data.putDouble("space_vy", data.getDouble("space_vy") + pushY);
            data.putDouble("space_vz", data.getDouble("space_vz") + pushZ);
        } else {
            data.putDouble("space_vx", pushX);
            data.putDouble("space_vy", pushY);
            data.putDouble("space_vz", pushZ);
        }

        data.putBoolean("space_was_on_ground", false);
        data.putLong("space_shot_tick", level.getGameTime());

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WIND_CHARGE_BURST, SoundSource.PLAYERS,
                0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getCharge(stack) > 0 || stack.isDamaged();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int charge = getCharge(stack);
        if (charge > 0) {
            return Math.round(13.0F * ((float) charge / MAX_CHARGES));
        }
        return Math.round(13.0F * (1.0F - (float)stack.getDamageValue() / stack.getMaxDamage()));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        if (getCharge(stack) > 0) {
            return 0xFFFFFF;
        }
        return net.minecraft.util.Mth.hsvToRgb(Math.max(0.0F, (1.0F - (float)stack.getDamageValue() / stack.getMaxDamage())) / 3.0F, 1.0F, 1.0F);
    }

    public static void setCharge(ItemStack stack, int value) {
        CustomData current = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = current.copyTag();
        tag.putInt(TAG_CHARGE, value);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static int getCharge(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return 0;
        return data.copyTag().getInt(TAG_CHARGE);
    }

    @EventBusSubscriber
    public static class Events {

        @SubscribeEvent
        public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
            var player = event.getEntity();
            ItemStack stack = AirGun.getAirGunStack(player);
            if (!stack.isEmpty()) {
                event.setCanceled(true);
                if (!player.level().isClientSide()) {
                    shoot(player, stack, player.level());
                }
            }
        }

        @SubscribeEvent
        public static void onAttackEntity(AttackEntityEvent event) {
            var player = event.getEntity();
            ItemStack stack = AirGun.getAirGunStack(player);
            if (!stack.isEmpty()) {
                event.setCanceled(true);
                if (!player.level().isClientSide()) {
                    shoot(player, stack, player.level());
                }
            }
        }

        @SubscribeEvent
        public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
            var player = event.getEntity();
            ItemStack stack = AirGun.getAirGunStack(player);

            if (!stack.isEmpty() && AirGun.getCharge(stack) > 0) {
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(new AirGunShootPayload());
            }
        }
    }
}