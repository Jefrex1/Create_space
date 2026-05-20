package com.jefrex.createspace.block.entity;

import com.jefrex.createspace.block.RotationalAccumulatorBlock;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import com.simibubi.create.foundation.utility.CreateLang;
import net.minecraft.network.chat.Component;
import java.util.List;

import java.util.List;

public class RotationalAccumulatorBlockEntity extends GeneratingKineticBlockEntity {

    public static final float MAX_CHARGE = 80f;
    public static final float MAX_OUTPUT_RPM = 256f;

    private float charge = 0f;
    private int redstoneSignal = 0;
    private float inputRpm = 0f;

    public RotationalAccumulatorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    public static void tick(Level level, BlockPos pos, BlockState state,
                            RotationalAccumulatorBlockEntity be) {
        if (level.isClientSide) return;

        boolean powered = state.getValue(RotationalAccumulatorBlock.POWERED);

        if (!powered) {
            float incoming = Math.abs(be.getSpeed());
            be.inputRpm = incoming;
            if (incoming > 0 && be.charge < MAX_CHARGE) {
                float rotationsThisTick = incoming / 1200f;
                float prevCharge = be.charge;
                be.charge = Math.min(MAX_CHARGE, be.charge + rotationsThisTick);
                be.setChanged();

                // Щойно заповнився — переприєднуємо щоб відкрити другий шафт
                if (prevCharge < MAX_CHARGE && be.charge >= MAX_CHARGE) {
                    be.detachKinetics();
                    be.attachKinetics();
                }

                if (level.getGameTime() % 20 == 0) be.notifyUpdate();
            }
        } else {
            // Розрядка: списуємо рівно стільки оборотів скільки віддаємо
            if (be.charge > 0) {
                float outputRpm = be.calculateOutputRpm();
                float rotationsThisTick = outputRpm / 1200f; // скільки оборотів за цей тік

                if (be.charge >= rotationsThisTick) {
                    be.charge -= rotationsThisTick;
                } else {
                    // Заряду менше ніж треба для повного тіку — віддаємо останнє і зупиняємось
                    be.charge = 0f;
                }

                be.setChanged();
                be.updateGeneratedRotation();
                be.notifyUpdate();
            } else {
                // Заряд = 0, зупиняємось
                if (Math.abs(be.getGeneratedSpeed()) > 0) {
                    be.updateGeneratedRotation();
                    be.notifyUpdate();
                }
            }
        }
    }

    public float calculateOutputRpm() {
        if (redstoneSignal == 0 || charge <= 0) return 0f;
        return (redstoneSignal / 15f) * MAX_OUTPUT_RPM;
    }

    @Override
    public float getGeneratedSpeed() {
        if (redstoneSignal == 0 || charge <= 0) return 0f;
        return calculateOutputRpm();
    }

    public void setRedstoneSignal(int signal) {
        if (this.redstoneSignal != signal) {
            this.redstoneSignal = signal;
            setChanged();
            // НЕ викликаємо updateGeneratedRotation тут —
            // це зробить attachKinetics після перебудови мережі
        }
    }

    public float getCharge() { return charge; }
    public float getChargePercent() { return charge / MAX_CHARGE; }
    public int getRedstoneSignal() { return redstoneSignal; }
    public float getInputRpm() { return inputRpm; }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putFloat("Charge", charge);
        tag.putInt("RedstoneSignal", redstoneSignal);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        charge = tag.getFloat("Charge");
        redstoneSignal = tag.getInt("RedstoneSignal");
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        // спочатку стандартна інфа від Create (стрес тощо)
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        // заряд
        CreateLang.translate("gui.goggles.kinetic_stats") // або просто текст
                .forGoggles(tooltip);

        CreateLang.text("Charge: ")
                .add(CreateLang.number(charge).text(" / " + (int) MAX_CHARGE))
                .style(net.minecraft.ChatFormatting.AQUA)
                .forGoggles(tooltip, 1);

        CreateLang.text("Output RPM: ")
                .add(CreateLang.number(calculateOutputRpm()))
                .style(net.minecraft.ChatFormatting.GREEN)
                .forGoggles(tooltip, 1);

        CreateLang.text("Redstone: ")
                .add(CreateLang.number(redstoneSignal))
                .style(net.minecraft.ChatFormatting.RED)
                .forGoggles(tooltip, 1);

        return true;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            // Примусово приєднуємось до існуючої мережі при завантаженні
            detachKinetics();
            attachKinetics();
        }
    }
}