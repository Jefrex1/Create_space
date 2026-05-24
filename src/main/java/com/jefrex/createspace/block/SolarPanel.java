package com.jefrex.createspace.block;

import com.jefrex.createspace.block.entity.SolarKineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
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
        Direction clickedFace = context.getClickedFace();
        Axis axis;

        if (clickedFace.getAxis().isHorizontal()) {
            axis = Axis.Z;
        } else {
            axis = context.getHorizontalDirection().getAxis() == Axis.Z ? Axis.X : Axis.Y;
        }

        return this.defaultBlockState().setValue(AXIS, axis);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(AXIS)) {
            case Z -> SHAPE_Y;
            case Y -> SHAPE_X;
            case X -> SHAPE_Z;
        };
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos,
                        BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide) notifyNearbyAdapter(level, pos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !newState.is(this)) notifyNearbyAdapter(level, pos);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private static void notifyNearbyAdapter(Level level, BlockPos panelPos) {
        for (Direction dir : Direction.values()) {
            for (int dist = 1; dist <= SolarKineticBlockEntity.MAX_DEPTH + 1; dist++) {
                BlockPos checkPos = panelPos.relative(dir, dist);
                BlockState bs = level.getBlockState(checkPos);
                if (bs.getBlock() instanceof SolarKineticBlock) {
                    Direction adapterFacing = bs.getValue(SolarKineticBlock.FACING);
                    if (adapterFacing == dir.getOpposite()) {
                        if (level.getBlockEntity(checkPos) instanceof SolarKineticBlockEntity be) {
                            SolarKineticBlockEntity.forceUpdate(level, checkPos, bs, be);
                        }
                    }
                    break;
                }
                if (!(bs.getBlock() instanceof SolarPanel)) break;
            }
        }
    }
}
