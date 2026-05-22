package com.jefrex.createspace.block;

import com.jefrex.createspace.block.entity.SolarKineticBlockEntity;
import com.jefrex.createspace.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class SolarKineticBlock extends KineticBlock implements IBE<SolarKineticBlockEntity> {

    public static final MapCodec<SolarKineticBlock> CODEC =
            simpleCodec(SolarKineticBlock::new);

    public SolarKineticBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends KineticBlock> codec() {
        return CODEC;
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos,
                                   BlockState state, Direction face) {
        return face == Direction.DOWN;
    }


    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return Direction.Axis.Y;
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