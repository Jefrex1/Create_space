// particle/TurbineFlameParticleType.java
package com.jefrex.createspace.particle;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class TurbineFlameParticleType extends ParticleType<TurbineFlameParticleOptions> {

    public TurbineFlameParticleType() {
        super(false);
    }

    @Override
    public MapCodec<TurbineFlameParticleOptions> codec() {
        return TurbineFlameParticleOptions.CODEC;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, TurbineFlameParticleOptions> streamCodec() {
        return TurbineFlameParticleOptions.STREAM_CODEC;
    }
}