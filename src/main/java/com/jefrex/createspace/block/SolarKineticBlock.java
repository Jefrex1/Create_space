package com.jefrex.createspace.block;

import com.jefrex.createspace.block.entity.SolarKineticBlockEntity;
import com.jefrex.createspace.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SolarKineticBlock extends KineticBlock implements IBE<SolarKineticBlockEntity> {

    public static final MapCodec<SolarKineticBlock> CODEC =
            simpleCodec(SolarKineticBlock::new);

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    private static final VoxelShape SHAPE_NORTH = makeShape(Direction.NORTH);
    private static final VoxelShape SHAPE_SOUTH = makeShape(Direction.SOUTH);
    private static final VoxelShape SHAPE_EAST = makeShape(Direction.EAST);
    private static final VoxelShape SHAPE_WEST = makeShape(Direction.WEST);
    private static final VoxelShape SHAPE_UP = makeShape(Direction.UP);
    private static final VoxelShape SHAPE_DOWN = makeShape(Direction.DOWN);

    public SolarKineticBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.UP)
                .setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getClickedFace())
                .setValue(POWERED, false);
    }

    @Override
    public MapCodec<? extends KineticBlock> codec() {
        return CODEC;
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos,
                                   BlockState state, Direction face) {
        return face == state.getValue(FACING).getOpposite();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            case UP -> SHAPE_UP;
            case DOWN -> SHAPE_DOWN;
        };
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    private static VoxelShape makeShape(Direction facing) {
        VoxelShape shape = Shapes.empty();
        shape = Shapes.join(shape, rotate(Block.box(1, 1, 4, 15, 15, 12), facing), BooleanOp.OR);
        shape = Shapes.join(shape, rotate(Block.box(0, 0, 2, 16, 6, 8), facing), BooleanOp.OR);
        shape = Shapes.join(shape, rotate(Block.box(0, 10, 2, 16, 16, 8), facing), BooleanOp.OR);
        shape = Shapes.join(shape, rotate(Block.box(2, 2, 12, 14, 14, 15), facing), BooleanOp.OR);
        shape = Shapes.join(shape, rotate(Block.box(0, 2, 12, 2, 14, 16), facing), BooleanOp.OR);
        shape = Shapes.join(shape, rotate(Block.box(14, 2, 12, 16, 14, 16), facing), BooleanOp.OR);
        shape = Shapes.join(shape, rotate(Block.box(0, 14, 12, 16, 16, 16), facing), BooleanOp.OR);
        shape = Shapes.join(shape, rotate(Block.box(0, 0, 12, 16, 2, 16), facing), BooleanOp.OR);
        return shape.optimize();
    }

    private static VoxelShape rotate(VoxelShape shape, Direction facing) {
        return switch (facing) {
            case NORTH -> shape;
            case SOUTH -> rotateY(rotateY(shape));
            case EAST -> rotateY(shape);
            case WEST -> rotateY(rotateY(rotateY(shape)));
            case UP -> rotateX(shape);
            case DOWN -> rotateX(rotateX(rotateX(shape)));
        };
    }

    private static VoxelShape rotateY(VoxelShape shape) {
        VoxelShape[] result = {Shapes.empty()};
        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
                result[0] = Shapes.join(result[0],
                        Shapes.box(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX),
                        BooleanOp.OR));
        return result[0];
    }

    private static VoxelShape rotateX(VoxelShape shape) {
        VoxelShape[] result = {Shapes.empty()};
        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
                result[0] = Shapes.join(result[0],
                        Shapes.box(minX, 1 - maxZ, minY, maxX, 1 - minZ, maxY),
                        BooleanOp.OR));
        return result[0];
    }

    @Override
    public Class<SolarKineticBlockEntity> getBlockEntityClass() {
        return SolarKineticBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SolarKineticBlockEntity> getBlockEntityType() {
        return ModBlockEntities.SOLAR_KINETIC.get();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SolarKineticBlockEntity(
                ModBlockEntities.SOLAR_KINETIC.get(), pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            net.minecraft.world.level.Level level, BlockState state,
            BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        if (type == ModBlockEntities.SOLAR_KINETIC.get()) {
            return (lvl, pos, st, be) ->
                    SolarKineticBlockEntity.tick(lvl, pos, st,
                            (SolarKineticBlockEntity) be);
        }
        return null;
    }
}