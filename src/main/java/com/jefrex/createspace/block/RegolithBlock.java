package com.jefrex.createspace.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class RegolithBlock extends Block {

    public RegolithBlock() {
        super(BlockBehaviour.Properties.of()
                //.mapColor(MapColor.COLOR_LIGHT_GRAY)
                .strength(0.8f, 3.0f)
                .requiresCorrectToolForDrops()
                .sound(SoundType.SAND)
                .instrument(net.minecraft.world.level.block.state.properties.NoteBlockInstrument.BASEDRUM));
    }
}