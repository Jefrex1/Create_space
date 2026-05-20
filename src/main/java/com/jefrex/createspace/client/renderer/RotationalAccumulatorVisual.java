package com.jefrex.createspace.client.renderer;

import com.jefrex.createspace.block.entity.RotationalAccumulatorBlockEntity;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;

public class RotationalAccumulatorVisual
        extends SingleAxisRotatingVisual<RotationalAccumulatorBlockEntity> {

    public RotationalAccumulatorVisual(VisualizationContext context,
                                       RotationalAccumulatorBlockEntity blockEntity,
                                       float partialTick) {
        super(context, blockEntity, partialTick, Models.partial(AllPartialModels.SHAFT));
    }
}