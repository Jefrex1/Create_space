package com.jefrex.createspace.network;

import com.jefrex.createspace.item.custom.AirGun;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import io.netty.buffer.ByteBuf;

public record AirGunShootPayload() implements CustomPacketPayload {

    public static final Type<AirGunShootPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("create_space", "air_gun_shoot"));

    public static final StreamCodec<ByteBuf, AirGunShootPayload> STREAM_CODEC = StreamCodec.unit(new AirGunShootPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final AirGunShootPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            ItemStack stack = AirGun.getAirGunStack(player);

            if (!stack.isEmpty()) {
                AirGun.shoot(player, stack, player.level());
            }
        });
    }
}