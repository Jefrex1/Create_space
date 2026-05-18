package com.jefrex.createspace.registry;

import com.jefrex.createspace.block.entity.TurbineBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "create_space");

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TurbineBlockEntity>> TURBINE =
            BLOCK_ENTITY_TYPES.register("turbine", () ->
                    BlockEntityType.Builder.of(TurbineBlockEntity::new,
                                    com.jefrex.createspace.block.ModBlocks.TURBINE.get())
                            .build(null));
}