package com.jefrex.createspace.block;

import com.jefrex.createspace.block.entity.TurbineBlockEntity;
import com.jefrex.createspace.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TurbineBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty MIRRORED = BooleanProperty.create("mirrored");
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    private static final VoxelShape SHAPE_DOWN = makeShape(Direction.DOWN);
    private static final VoxelShape SHAPE_UP = makeShape(Direction.UP);
    private static final VoxelShape SHAPE_NORTH = makeShape(Direction.NORTH);
    private static final VoxelShape SHAPE_SOUTH = makeShape(Direction.SOUTH);
    private static final VoxelShape SHAPE_EAST = makeShape(Direction.EAST);
    private static final VoxelShape SHAPE_WEST = makeShape(Direction.WEST);

    public TurbineBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(2.0f)
                .sound(SoundType.METAL)
                .noOcclusion());
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.UP)
                .setValue(MIRRORED, false)
                .setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, MIRRORED, POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace();
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            facing = facing.getOpposite();
        }
        boolean powered = context.getLevel().hasNeighborSignal(context.getClickedPos());
        return this.defaultBlockState()
                .setValue(FACING, facing)
                .setValue(POWERED, powered);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos,
                                Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (level.isClientSide) return;

        boolean powered = level.hasNeighborSignal(pos);
        int signal = powered ? level.getBestNeighborSignal(pos) : 0;

        if (state.getValue(POWERED) != powered) {
            level.setBlock(pos, state.setValue(POWERED, powered), 3);
        }

        if (level.getBlockEntity(pos) instanceof TurbineBlockEntity be) {
            be.setRedstoneSignal(signal);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case DOWN -> SHAPE_DOWN;
            case UP -> SHAPE_UP;
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
        };
    }

    private static VoxelShape makeShape(Direction facing) {
        VoxelShape shape = Shapes.empty();
        // Корпус
        shape = Shapes.join(shape, rotate(Block.box(0, 11, 0, 16, 15, 16), facing), BooleanOp.OR);
        // Верхній диск
        shape = Shapes.join(shape, rotate(Block.box(2, 15, 2, 14, 16, 14), facing), BooleanOp.OR);
        // Кільце
        shape = Shapes.join(shape, rotate(Block.box(2, 8, 2, 14, 9, 14), facing), BooleanOp.OR);
        // Обідок
        shape = Shapes.join(shape, rotate(Block.box(1, 9, 1, 15, 11, 15), facing), BooleanOp.OR);
        // Конус (три ступені з моделі)
        shape = Shapes.join(shape, rotate(Block.box(2.5, 3, 2.5, 13.5, 5, 13.5), facing), BooleanOp.OR);
        shape = Shapes.join(shape, rotate(Block.box(3, 5, 3, 13, 7, 13), facing), BooleanOp.OR);
        shape = Shapes.join(shape, rotate(Block.box(4, 7, 4, 12, 8, 12), facing), BooleanOp.OR);
        return shape.optimize();
    }

    private static VoxelShape rotate(VoxelShape shape, Direction facing) {
        return switch (facing) {
            case DOWN -> shape;
            case UP -> rotateX(rotateX(shape));
            case SOUTH -> rotateX(shape);
            case NORTH -> rotateY(rotateY(rotateX(shape)));
            case EAST -> rotateY(rotateY(rotateY(rotateX(shape))));
            case WEST -> rotateY(rotateX(shape));
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
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TurbineBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (!level.isClientSide) {
            return (lvl, pos, st, be) -> {
                if (be instanceof TurbineBlockEntity turbineBe) {
                    TurbineBlockEntity.serverTick(lvl, pos, st, turbineBe);
                }
            };
        }
        return null;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return false;
    }
}