package com.jefrex.createspace.block.entity;

import com.jefrex.createspace.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class SolarKineticBlockEntity extends GeneratingKineticBlockEntity {

    public static final float OUTPUT_RPM = 32f;

    private float sunFactor = 0f;

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

        float newFactor = computeSunFactor(level, pos);

        if (Math.abs(newFactor - be.sunFactor) > 0.005f) {
            be.sunFactor = newFactor;
            be.updateGeneratedRotation();
            be.setChanged();
            be.notifyUpdate();
        }
    }

    private static float computeSunFactor(Level level, BlockPos pos) {
        if (!level.canSeeSky(pos.above())) return 0f;
        if (level.isRaining() || level.isThundering()) return 0f;

        // 0-12000 = день, 12000-24000 = ніч
        long time = level.getDayTime() % 24000;
        if (time >= 12000) return 0f;

        // Максимум в полудень (6000), нуль на світанку (0) і заході (12000)
        float dayProgress = time / 12000f; // 0.0 → 1.0
        float sinAngle = (float) Math.sin(dayProgress * Math.PI); // 0 → 1 → 0
        return sinAngle;
    }

    @Override
    public float getGeneratedSpeed() {
        return sunFactor > 0f ? OUTPUT_RPM : 0f;
    }

    public float getSunFactor() { return sunFactor; }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            sunFactor = computeSunFactor(level, worldPosition);
            updateGeneratedRotation();
        }
    }

    @Override
    public float calculateAddedStressCapacity() {
        return (1024f / OUTPUT_RPM) * sunFactor;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putFloat("SunFactor", sunFactor);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        sunFactor = tag.getFloat("SunFactor");
    }

    @Override
    public float calculateStressApplied() {
        return 0f;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        if (sunFactor > 0f) {
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
                .style(sunFactor > 0f ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY)
                .forGoggles(tooltip, 1);

        CreateLang.text("Sun: ")
                .add(CreateLang.number(sunFactor * 100f).text("%"))
                .style(ChatFormatting.YELLOW)
                .forGoggles(tooltip, 1);

        return true;
    }

    private String getStopReason() {
        if (level == null) return "Inactive";
        if (level.isRaining()) return "Raining";
        if (level.isThundering()) return "Thunderstorm";
        if (!level.canSeeSky(worldPosition.above())) return "Blocked";
        long time = level.getDayTime() % 24000;
        if (time >= 12000) return "Night";
        return "Inactive";
    }
}