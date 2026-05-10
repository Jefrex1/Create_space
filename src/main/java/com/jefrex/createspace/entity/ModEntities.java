package com.jefrex.createspace.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// ModEntities.java
public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, "create_space");

    public static final DeferredHolder<EntityType<?>, EntityType<MeteoriteEntity>> METEORITE =
            ENTITY_TYPES.register("meteorite", () ->
                    EntityType.Builder.<MeteoriteEntity>of(MeteoriteEntity::new, MobCategory.MISC)
                            .sized(2.0f, 2.0f)
                            .build("meteorite")
            );
}