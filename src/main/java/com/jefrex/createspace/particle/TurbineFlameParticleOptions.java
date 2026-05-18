package com.jefrex.createspace.particle;

import com.jefrex.createspace.registry.ModParticles;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class TurbineFlameParticleOptions implements ParticleOptions {

    public static final TurbineFlameParticleOptions INSTANCE = new TurbineFlameParticleOptions();

    public static final MapCodec<TurbineFlameParticleOptions> CODEC = MapCodec.unit(INSTANCE);

    public static final StreamCodec<RegistryFriendlyByteBuf, TurbineFlameParticleOptions> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public ParticleType<?> getType() {
        return ModParticles.TURBINE_FLAME.get();
    }
}