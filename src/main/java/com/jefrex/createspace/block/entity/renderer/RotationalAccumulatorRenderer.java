package com.jefrex.createspace.block.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.jefrex.createspace.block.entity.RotationalAccumulatorBlockEntity;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import static com.simibubi.create.content.contraptions.gantry.GantryCarriageRenderer.getAngleForBE;

public class RotationalAccumulatorRenderer extends KineticBlockEntityRenderer<RotationalAccumulatorBlockEntity> {

    public RotationalAccumulatorRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(RotationalAccumulatorBlockEntity be, float partialTicks, PoseStack ms,
                              MultiBufferSource buffer, int light, int overlay) {
        BlockState state = be.getBlockState();
        Direction facing = state.getValue(com.jefrex.createspace.block.RotationalAccumulatorBlock.FACING);

        SuperByteBuffer shaft = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state, facing);

        float angle = getAngleForBE(be, be.getBlockPos(), facing.getAxis());

        kineticRotationTransform(shaft, be, facing.getAxis(), angle, light);
        shaft.renderInto(ms, buffer.getBuffer(RenderType.solid()));
    }

    @Override
    protected SuperByteBuffer getRotatedModel(RotationalAccumulatorBlockEntity be, BlockState state) {
        Direction facing = state.getValue(com.jefrex.createspace.block.RotationalAccumulatorBlock.FACING);
        return CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state, facing);
    }
}