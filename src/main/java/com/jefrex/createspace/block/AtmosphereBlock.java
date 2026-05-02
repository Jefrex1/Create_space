package com.jefrex.createspace.block;

import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class AtmosphereBlock extends AirBlock {
    public AtmosphereBlock() {
        super(BlockBehaviour.Properties.of()
                .noCollission()
                .noLootTable()
                .replaceable()
                .isViewBlocking((state, level, pos) -> false)
                .isSuffocating((state, level, pos) -> false)
        );
    }
}