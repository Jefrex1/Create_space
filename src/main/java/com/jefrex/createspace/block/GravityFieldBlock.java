package com.jefrex.createspace.block;

import com.jefrex.createspace.CreateSpace;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

import java.util.*;

public class GravityFieldBlock extends Block {

    private static final int MAX_SPREAD = 20;
    private static final int BLOCKS_PER_TICK = 50;

    public GravityFieldBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.0f));
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos,
                        BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide()) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide()) {
            removeAtmosphere(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos,
                     net.minecraft.util.RandomSource random) {
        spreadAtmosphere(level, pos);
        level.scheduleTick(pos, this, 2);
    }

    private void spreadAtmosphere(Level level, BlockPos center) {
        Map<BlockPos, Integer> reachable = new HashMap<>();
        Queue<BlockPos> queue = new ArrayDeque<>();

        queue.add(center);
        reachable.put(center, 0);

        int placed = 0;

        while (!queue.isEmpty()) {
            var current = queue.poll();
            int currentDepth = reachable.get(current);

            if (currentDepth >= MAX_SPREAD) continue;

            for (var dir : net.minecraft.core.Direction.values()) {
                var neighbor = current.relative(dir);
                if (reachable.containsKey(neighbor)) continue;

                var neighborState = level.getBlockState(neighbor);
                boolean passable = neighborState.isAir()
                        || neighborState.is(ModBlocks.ATMOSPHERE.get());
                if (!passable) continue;

                int neighborDepth = currentDepth + 1;
                reachable.put(neighbor.immutable(), neighborDepth);
                queue.add(neighbor.immutable());

                if (!neighborState.is(ModBlocks.ATMOSPHERE.get()) && placed < BLOCKS_PER_TICK) {
                    level.setBlock(neighbor, ModBlocks.ATMOSPHERE.get().defaultBlockState(), 3);
                    placed++;
                }
            }
        }

        BlockPos.betweenClosedStream(
                center.offset(-MAX_SPREAD, -MAX_SPREAD, -MAX_SPREAD),
                center.offset(MAX_SPREAD, MAX_SPREAD, MAX_SPREAD)
        ).forEach(pos -> {
            if (!level.getBlockState(pos).is(ModBlocks.ATMOSPHERE.get())) return;
            if (!reachable.containsKey(pos)) {
                level.setBlock(pos.immutable(), Blocks.AIR.defaultBlockState(), 3);
            }
        });
    }

    private void removeAtmosphere(Level level, BlockPos center) {
        Queue<BlockPos> queue = new ArrayDeque<>();
        HashSet<BlockPos> visited = new HashSet<>();

        queue.add(center);
        visited.add(center);

        while (!queue.isEmpty()) {
            var current = queue.poll();

            for (var dir : net.minecraft.core.Direction.values()) {
                var neighbor = current.relative(dir);
                if (visited.contains(neighbor)) continue;
                visited.add(neighbor);

                if (neighbor.distSqr(center) > MAX_SPREAD * MAX_SPREAD) continue;

                if (level.getBlockState(neighbor).is(ModBlocks.ATMOSPHERE.get())) {
                    level.setBlock(neighbor, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                    queue.add(neighbor);
                }
            }
        }
    }
}