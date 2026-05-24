package com.jefrex.createspace.block.entity;

import com.jefrex.createspace.block.SolarKineticBlock;
import com.jefrex.createspace.block.SolarPanel;
import com.jefrex.createspace.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class SolarKineticBlockEntity extends GeneratingKineticBlockEntity {

    public static final float OUTPUT_RPM = 32f;
    public static final float STRESS_PER_PANEL = 256f;
    public static final int MAX_DEPTH = 6;
    public static final int MAX_WIDTH = 3;

    private float sunFactor = 0f;
    private int panelCount = 0;

    public SolarKineticBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public SolarKineticBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.SOLAR_KINETIC.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    public static void tick(Level level, BlockPos pos, BlockState state,
                            SolarKineticBlockEntity be) {
        if (level.isClientSide) return;
        if (level.getGameTime() % 20 != 0) return;
        doUpdate(level, pos, state, be);
    }

    public static void forceUpdate(Level level, BlockPos pos, BlockState state,
                                   SolarKineticBlockEntity be) {
        if (level.isClientSide) return;
        doUpdate(level, pos, state, be);
    }

    private static void doUpdate(Level level, BlockPos pos, BlockState state,
                                 SolarKineticBlockEntity be) {
        float newFactor = computeSunFactor(level);
        int newPanelCount = countConnectedPanels(level, pos, state);

        boolean changed = Math.abs(newFactor - be.sunFactor) > 0.005f
                || newPanelCount != be.panelCount;

        if (changed) {
            be.sunFactor = newFactor;
            be.panelCount = newPanelCount;
            be.updateGeneratedRotation();
            be.setChanged();
            be.notifyUpdate();
        }

        boolean shouldBePowered = newPanelCount > 0;
        boolean isPowered = state.getValue(SolarKineticBlock.POWERED);
        if (shouldBePowered != isPowered) {
            level.setBlock(pos, state.setValue(SolarKineticBlock.POWERED, shouldBePowered), 3);
            // Якщо POWERED змінився але changed був false — все одно оновлюємо rotation
            if (!changed) {
                be.updateGeneratedRotation();
                be.notifyUpdate();
            }
        }
    }

    private static Direction[] getPlaneDirections(Direction facing) {
        return switch (facing) {
            case UP, DOWN -> new Direction[]{ Direction.NORTH, Direction.EAST };
            case NORTH, SOUTH -> new Direction[]{ Direction.EAST, Direction.UP };
            case EAST, WEST -> new Direction[]{ Direction.NORTH, Direction.UP };
        };
    }

    private static int countConnectedPanels(Level level, BlockPos adapterPos, BlockState adapterState) {
        Direction facing = adapterState.getValue(SolarKineticBlock.FACING);
        Direction[] planeDirs = getPlaneDirections(facing);

        BlockPos startPos = adapterPos.relative(facing, 1);

        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();

        if (level.getBlockState(startPos).getBlock() instanceof SolarPanel) {
            queue.add(startPos);
            visited.add(startPos);
        }

        int count = 0;

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();

            if (!isInBounds(current, adapterPos, facing, planeDirs)) continue;

            if (level.canSeeSky(current.above())) {
                count++;
            }

            BlockPos[] neighbors = {
                    current.relative(facing),
                    current.relative(planeDirs[0]),
                    current.relative(planeDirs[0].getOpposite()),
                    current.relative(planeDirs[1]),
                    current.relative(planeDirs[1].getOpposite()),
            };

            for (BlockPos neighbor : neighbors) {
                if (!visited.contains(neighbor)
                        && isInBounds(neighbor, adapterPos, facing, planeDirs)
                        && level.getBlockState(neighbor).getBlock() instanceof SolarPanel) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        return count;
    }

    private static boolean isInBounds(BlockPos pos, BlockPos adapterPos,
                                      Direction facing, Direction[] planeDirs) {
        int dx = pos.getX() - adapterPos.getX();
        int dy = pos.getY() - adapterPos.getY();
        int dz = pos.getZ() - adapterPos.getZ();

        int depth = dx * facing.getStepX()
                + dy * facing.getStepY()
                + dz * facing.getStepZ();
        if (depth < 1 || depth > MAX_DEPTH) return false;

        int w1 = dx * planeDirs[0].getStepX()
                + dy * planeDirs[0].getStepY()
                + dz * planeDirs[0].getStepZ();
        if (Math.abs(w1) > MAX_WIDTH / 2) return false;

        int w2 = dx * planeDirs[1].getStepX()
                + dy * planeDirs[1].getStepY()
                + dz * planeDirs[1].getStepZ();
        if (Math.abs(w2) > MAX_WIDTH / 2) return false;

        return true;
    }

    private static float computeSunFactor(Level level) {
        long time = level.getDayTime() % 24000;
        if (time >= 12000) return 0f;
        if (level.isRaining() || level.isThundering()) return 0f;

        float dayProgress = time / 12000f;
        return (float) Math.sin(dayProgress * Math.PI);
    }

    @Override
    public float getGeneratedSpeed() {
        return (sunFactor > 0f && panelCount > 0) ? OUTPUT_RPM : 0f;
    }

    public float getSunFactor() { return sunFactor; }
    public int getPanelCount() { return panelCount; }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            BlockState state = level.getBlockState(worldPosition);
            sunFactor = computeSunFactor(level);
            panelCount = countConnectedPanels(level, worldPosition, state);
            updateGeneratedRotation();
        }
    }

    @Override
    public float calculateAddedStressCapacity() {
        return STRESS_PER_PANEL * panelCount * sunFactor;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putFloat("SunFactor", sunFactor);
        tag.putInt("PanelCount", panelCount);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        sunFactor = tag.getFloat("SunFactor");
        panelCount = tag.getInt("PanelCount");
    }

    @Override
    public float calculateStressApplied() {
        return 0f;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        if (sunFactor > 0f && panelCount > 0) {
            CreateLang.text("Status: ")
                    .add(CreateLang.text("Generating").style(ChatFormatting.GREEN))
                    .forGoggles(tooltip);
        } else {
            CreateLang.text("Status: ")
                    .add(CreateLang.text(getStopReason()).style(ChatFormatting.RED))
                    .forGoggles(tooltip);
        }

        CreateLang.text("Output: ")
                .add(CreateLang.number(getGeneratedSpeed()).text(" RPM"))
                .style(sunFactor > 0f && panelCount > 0 ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY)
                .forGoggles(tooltip, 1);

        CreateLang.text("Sun: ")
                .add(CreateLang.number(sunFactor * 100f).text("%"))
                .style(ChatFormatting.YELLOW)
                .forGoggles(tooltip, 1);

        CreateLang.text("Panels: ")
                .add(CreateLang.number(panelCount).text(" / " + (MAX_WIDTH * MAX_DEPTH)))
                .style(panelCount > 0 ? ChatFormatting.GREEN : ChatFormatting.RED)
                .forGoggles(tooltip, 1);

        return true;
    }

    private String getStopReason() {
        if (panelCount == 0) return "No Panels";
        if (level == null) return "Inactive";
        if (level.isRaining()) return "Raining";
        if (level.isThundering()) return "Thunderstorm";
        long time = level.getDayTime() % 24000;
        if (time >= 12000) return "Night";
        return "Inactive";
    }
}