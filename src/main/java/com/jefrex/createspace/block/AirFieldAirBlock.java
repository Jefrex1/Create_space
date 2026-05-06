package com.jefrex.createspace.block;

import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class AirFieldAirBlock extends AirBlock {

    public AirFieldAirBlock() {
        super(BlockBehaviour.Properties.of()
                .air()
                .noCollission()
                .noLootTable()
                .replaceable()
                .strength(-1.0f) // не можна зламати
                .lightLevel(s -> 0));
    }
}