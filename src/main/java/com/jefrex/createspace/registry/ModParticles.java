// registry/ModParticles.java
package com.jefrex.createspace.registry;

import com.jefrex.createspace.CreateSpace;
import com.jefrex.createspace.particle.TurbineFlameParticleType;
import com.jefrex.createspace.particle.TurbineFlameParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.core.registries.Registries;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, CreateSpace.MODID);

    public static final DeferredHolder<ParticleType<?>, TurbineFlameParticleType> TURBINE_FLAME =
            PARTICLE_TYPES.register("turbine_flame", TurbineFlameParticleType::new);
}