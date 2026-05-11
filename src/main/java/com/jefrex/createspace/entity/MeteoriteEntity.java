package com.jefrex.createspace.entity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.Optional;

public class MeteoriteEntity extends Entity {

    private static final EntityDataAccessor<Float> SIZE =
            SynchedEntityData.defineId(MeteoriteEntity.class, EntityDataSerializers.FLOAT);

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SIZE, 1.0f);
    }

    public float getMeteorSize() {
        return this.entityData.get(SIZE);
    }

    public void setMeteorSize(float size) {
        this.entityData.set(SIZE, size);
    }

    private int ticksAlive = 0;
    private BlockPos targetPos;

    public MeteoriteEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public MeteoriteEntity(Level level, BlockPos target) {
        this(ModEntities.METEORITE.get(), level);
        this.targetPos = target;

        float randomSize = 1.0f + level.random.nextFloat() * 9.0f;
        setMeteorSize(randomSize);

        this.setPos(target.getX() - 40, target.getY() + 80, target.getZ() - 40);
    }

    @Override
    public void tick() {
        super.tick();
        ticksAlive++;

        if (targetPos == null) {
            int x = (int) this.getX();
            int z = (int) this.getZ();
            int y = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            targetPos = new BlockPos(x, y, z);

            if (getMeteorSize() == 1.0f) {
                float randomSize = 1.0f + this.level().random.nextFloat() * 9.0f;
                setMeteorSize(randomSize);
            }
        }

        this.setDeltaMovement(0.3, -0.8, 0.2);
        this.move(MoverType.SELF, this.getDeltaMovement());

        if (this.level() instanceof ServerLevel serverLevel) {
            BlockPos currentPos = this.blockPosition();
            float size = getMeteorSize();
            int breakRadius = size < 3.5f ? 1 : size < 7.0f ? 2 : 3;

            for (int x = -breakRadius; x <= breakRadius; x++) {
                for (int y = -breakRadius; y <= breakRadius; y++) {
                    for (int z = -breakRadius; z <= breakRadius; z++) {
                        if (x * x + y * y + z * z > breakRadius * breakRadius) continue;

                        BlockPos breakPos = currentPos.offset(x, y, z);
                        if (!serverLevel.getBlockState(breakPos).isAir()
                                && !serverLevel.getBlockState(breakPos).is(Blocks.BEDROCK)) {
                            serverLevel.destroyBlock(breakPos, false);
                        }
                    }
                }
            }
        }

        this.setYRot(this.getYRot() + 4.0f);
        this.setXRot(this.getXRot() + 6.0f);

        if (this.level() instanceof ServerLevel serverLevel) {
            for (ServerPlayer player : serverLevel.players()) {
                serverLevel.sendParticles(player, ParticleTypes.FLAME,
                        true,
                        this.getX(), this.getY(), this.getZ(),
                        15, 0.8, 0.8, 0.8, 0.1);

                serverLevel.sendParticles(player, ParticleTypes.LARGE_SMOKE,
                        true,
                        this.getX(), this.getY(), this.getZ(),
                        8, 0.5, 0.5, 0.5, 0.05);

                serverLevel.sendParticles(player, ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        true,
                        this.getX(), this.getY() + 1, this.getZ(),
                        10, 0.3, 1.0, 0.3, 0.02);
            }
        }

        boolean isStuck = !this.level().getBlockState(this.blockPosition()).isAir()
                && !this.level().getBlockState(this.blockPosition().below()).isAir()
                && !this.level().getBlockState(this.blockPosition().above()).isAir();

        if (isStuck || this.getY() <= targetPos.getY() - 3) {
            onImpact();
        }

        if (ticksAlive > 200) this.discard();
    }

    private void onImpact() {
        if (this.level() instanceof ServerLevel serverLevel) {
            BlockPos pos = this.blockPosition();
            float size = getMeteorSize();

            float explosionRadius = 3.0f + size * 2.0f;
            serverLevel.explode(null,
                    pos.getX(), pos.getY(), pos.getZ(),
                    explosionRadius,
                    Level.ExplosionInteraction.TNT);

            String structureName;
            int yOffset;
            if (size < 3.5f) {
                structureName = "small_meteorite";
                yOffset = -3;
            } else if (size < 7.0f) {
                structureName = "medium_meteorite";
                yOffset = -6;
            } else {
                structureName = "large_meteorite";
                yOffset = -10;
            }

            placeStructure(serverLevel, pos, structureName, yOffset);

            String sizeLabel = size < 3.5f ? "малий" : size < 7.0f ? "середній" : "ВЕЛИЧЕЗНИЙ";
            serverLevel.players().forEach(p -> {
                if (p.distanceToSqr(this) < 256 * 256) {
                    p.sendSystemMessage(
                            Component.literal("💥 " + sizeLabel + " метеорит впав!")
                                    .withStyle(size >= 7.0f ? ChatFormatting.DARK_RED : ChatFormatting.RED)
                    );
                }
            });
        }
        this.discard();
    }

    private void placeStructure(ServerLevel level, BlockPos pos, String structureName, int yOffset) {
        ResourceLocation structureId = ResourceLocation.fromNamespaceAndPath("create_space", structureName);

        Optional<StructureTemplate> template = level.getServer()
                .getStructureManager()
                .get(structureId);

        if (template.isEmpty()) {
            System.out.println("[create_space] Структура не знайдена: " + structureId);
            return;
        }

        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setMirror(Mirror.NONE)
                .setRotation(Rotation.NONE)
                .setIgnoreEntities(false);

        Vec3i structureSize = template.get().getSize();

        BlockPos offset = pos.offset(
                -structureSize.getX() / 2,
                yOffset,
                -structureSize.getZ() / 2
        );

        template.get().placeInWorld(level, offset, offset, settings, level.random, 3);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("MeteorSize", getMeteorSize());
        if (targetPos != null) {
            tag.putInt("TargetX", targetPos.getX());
            tag.putInt("TargetY", targetPos.getY());
            tag.putInt("TargetZ", targetPos.getZ());
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("MeteorSize")) setMeteorSize(tag.getFloat("MeteorSize"));
        if (tag.contains("TargetX")) {
            targetPos = new BlockPos(tag.getInt("TargetX"), tag.getInt("TargetY"), tag.getInt("TargetZ"));
        }
    }
}