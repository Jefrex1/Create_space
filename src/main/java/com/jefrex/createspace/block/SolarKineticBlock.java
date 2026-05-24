package com.jefrex.createspace.block;

import com.jefrex.createspace.block.entity.SolarKineticBlockEntity;
import com.jefrex.createspace.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

public class SolarKineticBlock extends KineticBlock implements IBE<SolarKineticBlockEntity> {

    public static final MapCodec<SolarKineticBlock> CODEC =
            simpleCodec(SolarKineticBlock::new);

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

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
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
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