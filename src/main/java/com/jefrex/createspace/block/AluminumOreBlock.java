package com.jefrex.createspace.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

public class AluminumOreBlock extends Block {

    public AluminumOreBlock() {
        super(Properties.of()
                .strength(5.0f, 3.0f)
                .requiresCorrectToolForDrops()
                .sound(SoundType.BASALT)
                .instrument(net.minecraft.world.level.block.state.properties.NoteBlockInstrument.BASEDRUM));
    }
}