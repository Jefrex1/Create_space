package com.jefrex.createspace.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;

public class LunarBitBlock extends HorizontalDirectionalBlock {
    // Кодек для NeoForge/Minecraft 1.20.x+
    public static final MapCodec<LunarBitBlock> CODEC = simpleCodec(LunarBitBlock::new);

    // Властивість для варіанту моделі (0 - lunar_bit, 1 - lunar_bit_v2)
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 1);

    // Хітбокси для першої моделі (v1)
    private static final VoxelShape V1_BASE = Shapes.or(
            Block.box(3.0D, 0.0D, 2.0D, 7.0D, 1.0D, 7.0D),
            Block.box(4.0D, 0.0D, 4.0D, 8.0D, 3.0D, 9.0D),
            Block.box(5.0D, 0.0D, 3.0D, 10.0D, 2.0D, 7.0D)
    );

    // Хітбокс для другої моделі (v2)
    private static final VoxelShape V2_BASE = Block.box(4.0D, 0.0D, 3.0D, 8.0D, 1.0D, 7.0D);

    // Мапи для збереження повернутих хітбоксів
    private static final Map<Direction, VoxelShape> V1_SHAPES = new EnumMap<>(Direction.class);
    private static final Map<Direction, VoxelShape> V2_SHAPES = new EnumMap<>(Direction.class);

    public LunarBitBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(VARIANT, 0));
        runCalculation();
    }

    @Override
    protected @NotNull MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();

        // Використовуємо координати як "зерно" для рандому
        // Це гарантує, що на цій позиції завжди буде один і той самий рандомний варіант
        long seed = pos.asLong();
        java.util.Random rand = new java.util.Random(seed);

        Direction[] horizontalDirs = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        Direction randomDir = horizontalDirs[rand.nextInt(horizontalDirs.length)];
        int randomVariant = rand.nextInt(2);

        return this.defaultBlockState()
                .setValue(FACING, randomDir)
                .setValue(VARIANT, randomVariant);
    }

    @Override
    protected @NotNull VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction dir = state.getValue(FACING);
        // Повертаємо потрібний хітбокс залежно від обраного варіанту
        return state.getValue(VARIANT) == 0 ? V1_SHAPES.get(dir) : V2_SHAPES.get(dir);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos surfacePos = pos.below();
        return level.getBlockState(surfacePos).isFaceSturdy(level, surfacePos, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        if (facing == Direction.DOWN && !state.canSurvive(level, currentPos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, facing, facingState, level, currentPos, facingPos);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, VARIANT);
    }

    private void runCalculation() {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            V1_SHAPES.put(dir, calculateRotation(dir, V1_BASE));
            V2_SHAPES.put(dir, calculateRotation(dir, V2_BASE));
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