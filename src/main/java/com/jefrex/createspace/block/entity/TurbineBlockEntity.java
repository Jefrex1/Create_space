package com.jefrex.createspace.block.entity;

import com.jefrex.createspace.block.TurbineBlock;
import com.jefrex.createspace.particle.TurbineFlameParticleOptions;
import com.jefrex.createspace.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class TurbineBlockEntity extends BlockEntity {

    public TurbineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURBINE.get(), pos, state);
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
                        0,        // count = 0 !
                        fx * speed + (Math.random() - 0.5) * 0.05,  // dx = реальна швидкість X
                        fy * speed + (Math.random() - 0.5) * 0.05,  // dy = реальна швидкість Y
                        fz * speed + (Math.random() - 0.5) * 0.05,  // dz = реальна швидкість Z
                        1.0       // speed при count=0 це множник для dx/dy/dz
                );
            }
        }
    }
}