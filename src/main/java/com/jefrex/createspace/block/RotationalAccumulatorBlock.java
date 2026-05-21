package com.jefrex.createspace.block;

import com.jefrex.createspace.block.entity.RotationalAccumulatorBlockEntity;
import com.jefrex.createspace.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
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
import org.jetbrains.annotations.Nullable;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

public class RotationalAccumulatorBlock extends KineticBlock implements IBE<RotationalAccumulatorBlockEntity> {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public static final MapCodec<RotationalAccumulatorBlock> CODEC =
            simpleCodec(RotationalAccumulatorBlock::new);

    public RotationalAccumulatorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public MapCodec<? extends KineticBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getNearestLookingDirection().getOpposite())
                .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true; // повідомляємо що блок підтримує компаратор
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof RotationalAccumulatorBlockEntity be) {
            // Компаратор видає від 0 до 15 в залежності від заряду
            return Math.round(be.getChargePercent() * 15f);
        }
        return 0;
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos,
                                   BlockState state, Direction face) {
        if (face.getAxis() != state.getValue(FACING).getAxis()) return false;

        boolean powered = state.getValue(POWERED);
        Direction facing = state.getValue(FACING);

        if (powered) {
            // З редстоуном: тільки вихідний шафт
            return face == facing;
        }

        // Без редстоуну: перевіряємо чи заповнений
        if (world instanceof Level level) {
            if (level.getBlockEntity(pos) instanceof RotationalAccumulatorBlockEntity be) {
                if (be.getCharge() < RotationalAccumulatorBlockEntity.MAX_CHARGE) {
                    // Не заповнений — тільки вхідний шафт (заряджається, не пропускає)
                    return face == facing.getOpposite();
                }
            }
        }

        // Заповнений — обидва шафти (пропускає наскрізь)
        return true;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos,
                                Block block, BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide) return;

        boolean powered = level.hasNeighborSignal(pos);
        boolean wasPowered = state.getValue(POWERED);
        int signal = powered ? level.getBestNeighborSignal(pos) : 0;

        if (powered != wasPowered) {
            // 1. Оновлюємо blockstate
            BlockState newState = state.setValue(POWERED, powered);
            level.setBlock(pos, newState, 3);

            withBlockEntityDo(level, pos, be -> {
                be.setRedstoneSignal(signal);
                be.detachKinetics();
                be.attachKinetics();
            });

            // 2. Примусово повідомляємо сусідні кінетичні блоки
            //    що наші шафти змінились — вони перебудують свої з'єднання
            notifyNeighboringKineticBlocks(level, pos, newState);

        } else if (powered) {
            withBlockEntityDo(level, pos, be -> {
                be.setRedstoneSignal(signal);
            });
        }
    }

    private void notifyNeighboringKineticBlocks(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        // Перевіряємо обидва кінці осі
        for (Direction dir : new Direction[]{facing, facing.getOpposite()}) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);
            if (neighborState.getBlock() instanceof KineticBlock) {
                level.neighborChanged(neighborState, neighborPos,
                        this, pos, false);
                // Також примушуємо BE перебудувати з'єднання
                if (level.getBlockEntity(neighborPos) instanceof KineticBlockEntity kbe) {
                    kbe.detachKinetics();
                    kbe.attachKinetics();
                }
            }
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RotationalAccumulatorBlockEntity(
                ModBlockEntities.ROTATIONAL_ACCUMULATOR.get(), pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        if (type == ModBlockEntities.ROTATIONAL_ACCUMULATOR.get()) {
            return (lvl, pos, st, be) -> RotationalAccumulatorBlockEntity.tick(
                    lvl, pos, st, (RotationalAccumulatorBlockEntity) be);
        }
        return null;
    }

    @Override
    public Class<RotationalAccumulatorBlockEntity> getBlockEntityClass() {
        return RotationalAccumulatorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends RotationalAccumulatorBlockEntity> getBlockEntityType() {
        return ModBlockEntities.ROTATIONAL_ACCUMULATOR.get();
    }
}