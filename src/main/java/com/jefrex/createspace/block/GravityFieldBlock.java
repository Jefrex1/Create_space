package com.jefrex.createspace.block;

import com.jefrex.createspace.CreateSpace;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;

public class GravityFieldBlock extends Block {

    private static final int MAX_SPREAD = 20; // максимальна відстань поширення
    private static final int BLOCKS_PER_TICK = 50; // скільки блоків заповнює за тік

    public GravityFieldBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.0f));
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos,
                        BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide()) {
            // запускаємо тіковий spread
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
        // продовжуємо тікати
        level.scheduleTick(pos, this, 2);
    }

    private void spreadAtmosphere(Level level, BlockPos center) {
        // BFS від центру — знаходимо повітря поруч з атмосферою або генератором
        Queue<BlockPos> queue = new ArrayDeque<>();
        HashSet<BlockPos> visited = new HashSet<>();

        queue.add(center);
        visited.add(center);

        int placed = 0;

        while (!queue.isEmpty() && placed < BLOCKS_PER_TICK) {
            var current = queue.poll();

            // перевіряємо всіх сусідів
            for (var dir : net.minecraft.core.Direction.values()) {
                var neighbor = current.relative(dir);

                if (visited.contains(neighbor)) continue;
                visited.add(neighbor);

                // перевіряємо відстань від генератора
                if (neighbor.distSqr(center) > MAX_SPREAD * MAX_SPREAD) continue;

                var neighborState = level.getBlockState(neighbor);

                // якщо повітря — заповнюємо атмосферою
                if (neighborState.isAir() && !neighborState.is(ModBlocks.ATMOSPHERE.get())) {
                    level.setBlock(neighbor, ModBlocks.ATMOSPHERE.get().defaultBlockState(), 3);
                    placed++;
                    queue.add(neighbor);
                }
                // якщо вже атмосфера — продовжуємо пошук від неї
                else if (neighborState.is(ModBlocks.ATMOSPHERE.get())) {
                    queue.add(neighbor);
                }
                // якщо блок — обходимо його (нічого не робимо, просто не додаємо в чергу)
            }
        }
    }

    private void removeAtmosphere(Level level, BlockPos center) {
        // видаляємо всю атмосферу через BFS
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