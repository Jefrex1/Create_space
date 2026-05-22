package com.jefrex.createspace.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

public class BrdishBlock extends Block {

    public BrdishBlock() {
        super(Properties.of()
                .strength(0.8f, 3.0f)
                .sound(SoundType.SAND)
                .instrument(net.minecraft.world.level.block.state.properties.NoteBlockInstrument.BASEDRUM));
    }
}