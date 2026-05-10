package com.jefrex.createspace.entity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

public class MeteoriteEntity extends Entity {

    private int ticksAlive = 0;
    private BlockPos targetPos;

    public MeteoriteEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public MeteoriteEntity(Level level, BlockPos target) {
        this(ModEntities.METEORITE.get(), level);
        this.targetPos = target;
        // Стартова позиція — зверху над ціллю
        this.setPos(
                target.getX() - 40,  // стартує збоку
                target.getY() + 80,  // і зверху
                target.getZ() - 40
        );
    }

    @Override
    public void tick() {
        super.tick();
        ticksAlive++;

        // Якщо заспавнений через /summon — знайти землю під собою
        if (targetPos == null) {
            int x = (int) this.getX();
            int z = (int) this.getZ();
            int y = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            targetPos = new BlockPos(x, y, z);
        }

        this.setDeltaMovement(0.3, -0.8, 0.2);
        this.move(MoverType.SELF, this.getDeltaMovement());

        this.setYRot(this.getYRot() + 4.0f);
        this.setXRot(this.getXRot() + 6.0f);

        // Частинки
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    this.getX(), this.getY(), this.getZ(),
                    5, 0.3, 0.3, 0.3, 0.05);
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    this.getX(), this.getY(), this.getZ(),
                    3, 0.2, 0.2, 0.2, 0.02);
        }

        // Перевірка удару
        if (this.getY() <= targetPos.getY() + 1) {
            onImpact();
        }

        if (ticksAlive > 200) this.discard();
    }

    private void onImpact() {
        if (this.level() instanceof ServerLevel serverLevel) {
            BlockPos pos = this.blockPosition();

            // Вибух
            serverLevel.explode(null,
                    pos.getX(), pos.getY(), pos.getZ(),
                    5.0f, Level.ExplosionInteraction.TNT);

            // Кладемо обсидіан
            serverLevel.setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), 3);

            // Повідомлення гравцям поруч
            serverLevel.players().forEach(p -> {
                if (p.distanceToSqr(this) < 128 * 128) {
                    p.sendSystemMessage(
                            Component.literal("💥 Метеорит впав!")
                                    .withStyle(ChatFormatting.DARK_RED)
                    );
                }
            });
        }
        this.discard();
    }

    // Обов'язкові методи
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("TargetX")) {
            targetPos = new BlockPos(tag.getInt("TargetX"), tag.getInt("TargetY"), tag.getInt("TargetZ"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (targetPos != null) {
            tag.putInt("TargetX", targetPos.getX());
            tag.putInt("TargetY", targetPos.getY());
            tag.putInt("TargetZ", targetPos.getZ());
        }
    }
}