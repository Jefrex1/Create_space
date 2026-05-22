package com.jefrex.createspace.client.renderer;

import com.jefrex.createspace.block.entity.SolarKineticBlockEntity;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import net.minecraft.core.Direction;

import java.util.function.Consumer;

public class SolarKineticVisual
        extends KineticBlockEntityVisual<SolarKineticBlockEntity>
        implements SimpleTickableVisual {

    private final RotatingInstance shaft;

    public SolarKineticVisual(VisualizationContext context,
                              SolarKineticBlockEntity blockEntity,
                              float partialTick) {
        super(context, blockEntity, partialTick);

        shaft = instancerProvider()
                .instancer(AllInstanceTypes.ROTATING,
                        Models.partial(AllPartialModels.SHAFT_HALF))
                .createInstance()
                .setup(blockEntity, Direction.Axis.Y, getShaftSpeed())
                .setPosition(getVisualPosition())
                .rotateToFace(Direction.SOUTH, Direction.DOWN);
        shaft.setChanged();
    }

    private float getShaftSpeed() {
        return blockEntity.getSunFactor() > 0f ? SolarKineticBlockEntity.OUTPUT_RPM : 0f;
    }

    @Override
    public void tick(Context context) {
        applyOverstressEffect(blockEntity, shaft);
    }

    @Override
    public void update(float partialTick) {
        shaft.setup(blockEntity, Direction.Axis.Y, getShaftSpeed()).setChanged();
    }

    @Override
    public void updateLight(float partialTick) {
        relight(shaft);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(shaft);
    }

    @Override
    protected void _delete() {
        shaft.delete();
    }
}