package com.jefrex.createspace.client.renderer;

import com.jefrex.createspace.block.RotationalAccumulatorBlock;
import com.jefrex.createspace.block.entity.RotationalAccumulatorBlockEntity;
import com.jefrex.createspace.registry.ModPartialModels;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;

public class RotationalAccumulatorRenderer
        extends KineticBlockEntityRenderer<RotationalAccumulatorBlockEntity> {

    /** Максимальна швидкість (RPM) когвіла при 100% заряді */
    private static final float MAX_COG_RPM = 256f;

    public RotationalAccumulatorRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(RotationalAccumulatorBlockEntity be,
                              float partialTicks,
                              PoseStack ms,
                              MultiBufferSource buffer,
                              int light,
                              int overlay) {

        BlockState state = be.getBlockState();
        Direction facing = state.getValue(RotationalAccumulatorBlock.FACING);
        Axis axis = facing.getAxis();
        BlockPos pos = be.getBlockPos();

        VertexConsumer solid = buffer.getBuffer(RenderType.solid());

        // ── 1. Шафт «входу» (з боку, протилежного FACING) ──────────────────
        BlockState shaftStateIn = shaft(axis); // BlockState шафта з потрібною віссю
        SuperByteBuffer shaftInBuffer = CachedBuffers.block(KINETIC_BLOCK, shaftStateIn);
        renderRotatingBuffer(be, shaftInBuffer, ms, solid, light);

        // ── 2. Шафт «виходу» (з боку FACING) ───────────────────────────────
        // Другий шафт — той самий model, просто буде symmetric по осі
        SuperByteBuffer shaftOutBuffer = CachedBuffers.block(KINETIC_BLOCK, shaftStateIn);
        renderRotatingBuffer(be, shaftOutBuffer, ms, solid, light);

        // ── 3. Когвіл з кутом, залежним від заряду ──────────────────────────
        float chargePercent = be.getChargePercent();
        float cogRpm = MAX_COG_RPM * (float) Math.sqrt(chargePercent);
        float cogAngle = getCogAngleForSpeed(be, pos, axis, cogRpm);

        SuperByteBuffer cogBuffer = CachedBuffers.partial(ModPartialModels.ACCUMULATOR_CORE, state);
        kineticRotationTransform(cogBuffer, be, axis, cogAngle, light)
                .renderInto(ms, solid);
    }

    /**
     * Розраховує кут обертання для заданої швидкості RPM —
     * аналог {@link KineticBlockEntityRenderer#getAngleForBe}, але зі своїм RPM.
     */
    private static float getCogAngleForSpeed(RotationalAccumulatorBlockEntity be,
                                             BlockPos pos, Axis axis, float rpm) {
        float time = AnimationTickHolder.getRenderTime(be.getLevel());
        float offset = getRotationOffsetForPosition(be, pos, axis);
        // Create використовує: angle = (time * speed * 3/10 + offset) % 360 → радіани
        return ((time * rpm * 3f / 10 + offset) % 360) / 180f * (float) Math.PI;
    }

    protected Axis getRotationAxisOf(RotationalAccumulatorBlockEntity be) {
        return be.getBlockState()
                .getValue(RotationalAccumulatorBlock.FACING)
                .getAxis();
    }
}