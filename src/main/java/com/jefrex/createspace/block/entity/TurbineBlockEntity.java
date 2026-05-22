package com.jefrex.createspace.block.entity;

import com.jefrex.createspace.block.TurbineBlock;
import com.jefrex.createspace.particle.TurbineFlameParticleOptions;
import com.jefrex.createspace.registry.ModBlockEntities;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.ryanhcode.sable.api.block.propeller.BlockEntityPropeller;
import dev.ryanhcode.sable.api.block.propeller.BlockEntitySubLevelPropellerActor;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class TurbineBlockEntity extends BlockEntity
        implements BlockEntitySubLevelPropellerActor, BlockEntityPropeller, IHaveGoggleInformation {

    public static final double MAX_THRUST = 500.0;
    public static final double MAX_AIRFLOW = 200.0;

    private int redstoneSignal = 0;

    public TurbineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURBINE.get(), pos, state);
    }

    public void setRedstoneSignal(int signal) {
        if (this.redstoneSignal != signal) {
            this.redstoneSignal = signal;
            setChanged();
            // Синхронізуємо з клієнтом
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }

    public int getRedstoneSignal() {
        return redstoneSignal;
    }

    private float getPowerPercent() {
        return redstoneSignal / 15f;
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
        return MAX_AIRFLOW * getPowerPercent();
    }

    @Override
    public double getThrust() {
        return MAX_THRUST * getPowerPercent();
    }

    @Override
    public boolean isActive() {
        return this.getBlockState().getValue(TurbineBlock.POWERED) && redstoneSignal > 0;
    }

    @Override
    public Level getLevel() {
        return this.level;
    }

    @Override
    public BlockPos getBlockPos() {
        return this.worldPosition;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("RedstoneSignal", redstoneSignal);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        redstoneSignal = tag.getInt("RedstoneSignal");
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        CreateLang.translate("gui.goggles.kinetic_stats")
                .forGoggles(tooltip);

        CreateLang.text("Thrust: ")
                .add(CreateLang.number((float) getThrust()).text(" N"))
                .style(ChatFormatting.AQUA)
                .forGoggles(tooltip, 1);

        CreateLang.text("Airflow: ")
                .add(CreateLang.number((float) getAirflow()).text(" m³/s"))
                .style(ChatFormatting.GREEN)
                .forGoggles(tooltip, 1);

        CreateLang.text("Redstone: ")
                .add(CreateLang.number(redstoneSignal))
                .style(ChatFormatting.RED)
                .forGoggles(tooltip, 1);

        CreateLang.text("Active: ")
                .add(CreateLang.text(isActive() ? "Yes" : "No"))
                .style(isActive() ? ChatFormatting.GREEN : ChatFormatting.GRAY)
                .forGoggles(tooltip, 1);

        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("RedstoneSignal", redstoneSignal);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        redstoneSignal = tag.getInt("RedstoneSignal");
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            boolean powered = level.hasNeighborSignal(worldPosition);
            redstoneSignal = powered ? level.getBestNeighborSignal(worldPosition) : 0;
            setChanged();
        }
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