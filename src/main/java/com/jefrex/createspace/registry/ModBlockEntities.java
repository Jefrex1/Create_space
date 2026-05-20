package com.jefrex.createspace.registry;

import com.jefrex.createspace.block.entity.RotationalAccumulatorBlockEntity;
import com.jefrex.createspace.block.entity.TurbineBlockEntity;
import com.jefrex.createspace.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "create_space");

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TurbineBlockEntity>> TURBINE =
            BLOCK_ENTITY_TYPES.register("turbine", () ->
                    BlockEntityType.Builder.of(TurbineBlockEntity::new,
                                    ModBlocks.TURBINE.get())
                            .build(null));

    @SuppressWarnings("unchecked")
    private static final BlockEntityType<?>[] ACCUMULATOR_TYPE_REF = new BlockEntityType[1];

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RotationalAccumulatorBlockEntity>>
            ROTATIONAL_ACCUMULATOR = BLOCK_ENTITY_TYPES.register("rotational_accumulator", () -> {
        BlockEntityType<RotationalAccumulatorBlockEntity> type =
                BlockEntityType.Builder.of(
                        (pos, state) -> new RotationalAccumulatorBlockEntity(
                                (BlockEntityType<RotationalAccumulatorBlockEntity>)
                                        ACCUMULATOR_TYPE_REF[0], pos, state),
                        ModBlocks.ROTATIONAL_ACCUMULATOR.get()
                ).build(null);
        ACCUMULATOR_TYPE_REF[0] = type;
        return type;
    });

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}