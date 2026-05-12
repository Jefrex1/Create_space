package com.jefrex.createspace.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

public class CheremshaBlock extends Block {

    public CheremshaBlock() {
        super(Properties.of()
                .strength(0.8f, 3.0f)
                .requiresCorrectToolForDrops()
                .sound(SoundType.SAND)
                .instrument(net.minecraft.world.level.block.state.properties.NoteBlockInstrument.BASEDRUM));
    }
}