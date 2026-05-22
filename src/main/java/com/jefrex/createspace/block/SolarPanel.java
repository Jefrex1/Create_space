package com.jefrex.createspace.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.core.Direction.Axis;

public class SolarPanel extends Block {

    public static final EnumProperty<Axis> AXIS = BlockStateProperties.AXIS;

    private static final VoxelShape SHAPE_Y = Block.box(0, 6, 0, 16, 10, 16);
    private static final VoxelShape SHAPE_Z = Block.box(0, 0, 6, 16, 16, 10);
    private static final VoxelShape SHAPE_X = Block.box(6, 0, 0, 10, 16, 16);

    public SolarPanel() {
        super(Properties.of()
                .strength(0.8f, 3.0f)
                .sound(SoundType.GLASS)
                .noOcclusion()
                .instrument(net.minecraft.world.level.block.state.properties.NoteBlockInstrument.BASEDRUM));
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Axis.Y));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(AXIS, context.getClickedFace().getAxis());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(AXIS)) {
            case Z -> SHAPE_Y;  // горизонтальна
            case Y -> SHAPE_X;  // вертикальна вздовж X
            case X -> SHAPE_Z;  // вертикальна вздовж Z
        };
    }
}