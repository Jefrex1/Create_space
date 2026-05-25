package com.jefrex.createspace.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MoonGrassBlock extends BushBlock {

    public static final MapCodec<MoonGrassBlock> CODEC = simpleCodec(MoonGrassBlock::new);

    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 4, 14);

    @Override
    public MapCodec<MoonGrassBlock> codec() {
        return CODEC;
    }

    public MoonGrassBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                               CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(ModBlocks.REGOLITH.get());
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext useContext) {
        return true;
    }
}