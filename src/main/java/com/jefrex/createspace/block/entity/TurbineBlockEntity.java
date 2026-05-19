package com.jefrex.createspace.block.entity;

import com.jefrex.createspace.block.TurbineBlock;
import com.jefrex.createspace.particle.TurbineFlameParticleOptions;
import com.jefrex.createspace.registry.ModBlockEntities;
import dev.ryanhcode.sable.api.block.propeller.BlockEntityPropeller;
import dev.ryanhcode.sable.api.block.propeller.BlockEntitySubLevelPropellerActor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class TurbineBlockEntity extends BlockEntity
        implements BlockEntitySubLevelPropellerActor, BlockEntityPropeller {

    public TurbineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURBINE.get(), pos, state);
    }

    @Override
    public BlockEntityPropeller getPropeller() {
        return this;
    }

    @Override
    public Direction getBlockDirection() {
        return this.getBlockState().getValue(TurbineBlock.FACING);
    }

    @Override
    public double getAirflow() {
        return 200.0;
    }

    @Override
    public double getThrust() {
        return 500.0;
    }

    @Override
    public boolean isActive() {
        return this.getBlockState().getValue(TurbineBlock.POWERED);
    }

    @Override
    public Level getLevel() {
        return this.level;
    }

    @Override
    public BlockPos getBlockPos() {
        return this.worldPosition;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TurbineBlockEntity be) {
        if (!state.getValue(TurbineBlock.POWERED)) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        Direction facing = state.getValue(TurbineBlock.FACING);
        double fx = facing.getStepX();
        double fy = facing.getStepY();
        double fz = facing.getStepZ();

        for (ServerPlayer player : serverLevel.players()) {
            for (int i = 0; i < 5; i++) {
                double radius = Math.random() * 0.3;
                double angle = Math.random() * Math.PI * 2;

                double ox = (fy != 0) ? Math.cos(angle) * radius : 0;
                double oy = (fy != 0) ? 0 : Math.sin(angle) * radius;
                double oz = (fy != 0) ? Math.sin(angle) * radius : (fx != 0 ? 0 : Math.cos(angle) * radius);

                double spawnBack = -0.3;

                double cx = pos.getX() + 0.5 + fx * (0.5 - spawnBack) + ox;
                double cy = pos.getY() + 0.5 + fy * (0.5 - spawnBack) + oy;
                double cz = pos.getZ() + 0.5 + fz * (0.5 - spawnBack) + oz;

                double speed = 0.4 + Math.random() * 0.3;

                serverLevel.sendParticles(player,
                        TurbineFlameParticleOptions.INSTANCE,
                        true,
                        cx, cy, cz,
                        0,
                        fx * speed + (Math.random() - 0.5) * 0.05,
                        fy * speed + (Math.random() - 0.5) * 0.05,
                        fz * speed + (Math.random() - 0.5) * 0.05,
                        1.0
                );
            }
        }
    }
}