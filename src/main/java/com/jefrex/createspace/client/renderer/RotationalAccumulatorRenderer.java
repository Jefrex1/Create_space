// client/renderer/RotationalAccumulatorRenderer.java
package com.jefrex.createspace.client.renderer;

import com.jefrex.createspace.block.entity.RotationalAccumulatorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class RotationalAccumulatorRenderer
        extends KineticBlockEntityRenderer<RotationalAccumulatorBlockEntity> {

    public RotationalAccumulatorRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }
}