package com.jefrex.createspace.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;

public class LunarBitBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<LunarBitBlock> CODEC = simpleCodec(LunarBitBlock::new);

    private static final VoxelShape BASE_SHAPE = Shapes.or(
            Block.box(3.0D, 0.0D, 2.0D, 7.0D, 1.0D, 7.0D),
            Block.box(4.0D, 0.0D, 4.0D, 8.0D, 3.0D, 9.0D),
            Block.box(5.0D, 0.0D, 3.0D, 10.0D, 2.0D, 7.0D)
    );

    // Мапа для збереження повернутих хітбоксів
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    public LunarBitBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
        runCalculation();
    }

    @Override
    protected @NotNull MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        BlockPos surfacePos = pos.below();
        BlockState surfaceState = level.getBlockState(surfacePos);

        return surfaceState.isFaceSturdy(level, surfacePos, Direction.UP);
    }

    @Override
    protected @NotNull VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.getOrDefault(state.getValue(FACING), BASE_SHAPE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction facing, BlockState facingState, net.minecraft.world.level.LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        if (facing == Direction.DOWN && !state.canSurvive(level, currentPos)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, facing, facingState, level, currentPos, facingPos);
    }

    private void runCalculation() {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            SHAPES.put(dir, calculateRotation(dir, BASE_SHAPE));
        }
    }

    private static VoxelShape calculateRotation(Direction dir, VoxelShape shape) {
        VoxelShape[] buffer = { shape };
        int rotations = (dir.get2DDataValue() - Direction.NORTH.get2DDataValue() + 4) % 4;

        for (int i = 0; i < rotations; i++) {
            VoxelShape nextShape = Shapes.empty();
            for (var box : buffer[0].toAabbs()) {
                nextShape = Shapes.or(nextShape, Block.box(
                        16 - box.maxZ * 16, box.minY * 16, box.minX * 16,
                        16 - box.minZ * 16, box.maxY * 16, box.maxX * 16
                ));
            }
            buffer[0] = nextShape;
        }
        return buffer[0];
    }
}