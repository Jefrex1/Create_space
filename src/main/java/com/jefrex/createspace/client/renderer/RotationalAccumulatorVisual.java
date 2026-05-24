package com.jefrex.createspace.client.renderer;

import com.jefrex.createspace.block.RotationalAccumulatorBlock;
import com.jefrex.createspace.block.entity.RotationalAccumulatorBlockEntity;
import com.jefrex.createspace.registry.ModPartialModels;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;

import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;

import java.util.function.Consumer;

public class RotationalAccumulatorVisual
        extends KineticBlockEntityVisual<RotationalAccumulatorBlockEntity>
        implements SimpleTickableVisual {

    // Шафт входу — виходить з боку, ПРОТИЛЕЖНОГО від FACING
    private final RotatingInstance shaftIn;
    // Шафт виходу — виходить з боку FACING
    private final RotatingInstance shaftOut;
    // Когвіл у центрі — крутиться зі швидкістю пропорційною заряду
    private final RotatingInstance cogwheel;

    public RotationalAccumulatorVisual(VisualizationContext context,
                                       RotationalAccumulatorBlockEntity blockEntity,
                                       float partialTick) {
        super(context, blockEntity, partialTick);

        Direction facing = blockEntity.getBlockState().getValue(RotationalAccumulatorBlock.FACING);
        Axis axis = facing.getAxis();

        // SHAFT_HALF базово орієнтований на SOUTH.
        // rotateToFace(SOUTH, target) повертає модель так, щоб вона дивилась у target.

        // Шафт входу: виходить з боку facing.getOpposite()
        shaftIn = instancerProvider()
                .instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.SHAFT_HALF))
                .createInstance()
                .setup(blockEntity, axis, getInputSpeed())
                .setPosition(getVisualPosition())
                .rotateToFace(Direction.SOUTH, facing.getOpposite());
        shaftIn.setChanged();

        // Шафт виходу: виходить з боку facing
        shaftOut = instancerProvider()
                .instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.SHAFT_HALF))
                .createInstance()
                .setup(blockEntity, axis, getOutputSpeed())
                .setPosition(getVisualPosition())
                .rotateToFace(Direction.SOUTH, facing);
        shaftOut.setChanged();

        // Когвіл: SHAFTLESS_COGWHEEL щоб не було зайвого шафта всередині моделі
        cogwheel = instancerProvider()
                .instancer(AllInstanceTypes.ROTATING, Models.partial(ModPartialModels.ACCUMULATOR_CORE))
                .createInstance()
                .setup(blockEntity, axis, 0)
                .setPosition(getVisualPosition())
                .rotateToFace(rotationAxis());
        cogwheel.setChanged();
    }

    // ── Швидкості ────────────────────────────────────────────────────────────

    /**
     * Шафт входу крутиться тільки коли НЕ генерує (заряджається).
     * Швидкість — з вхідної кінетичної мережі.
     */
    private float getInputSpeed() {
        if (blockEntity.isGenerating()) return 0f;
        return blockEntity.getSpeed();
    }

    /**
     * Шафт виходу крутиться тільки коли генерує (є редстоун + є заряд).
     * Швидкість — calculateOutputRpm().
     */
    private float getOutputSpeed() {
        if (!blockEntity.isGenerating()) return 0f;
        return blockEntity.calculateOutputRpm();
    }

    private float getCogSpeed() {
        float chargeSpeed = RotationalAccumulatorBlockEntity.MAX_OUTPUT_RPM
                * (float) Math.sqrt(blockEntity.getChargePercent());

        if (!blockEntity.isGenerating()) {
            return chargeSpeed - blockEntity.getSpeed();
        }

        return chargeSpeed;
    }

    // ── SimpleTickableVisual ─────────────────────────────────────────────────

    @Override
    public void tick(Context context) {
        applyOverstressEffect(blockEntity, shaftIn, shaftOut);
    }

    @Override
    public void update(float partialTick) {
        Direction facing = blockEntity.getBlockState().getValue(RotationalAccumulatorBlock.FACING);
        Axis axis = facing.getAxis();

        shaftIn.setup(blockEntity, axis, getInputSpeed()).setChanged();
        shaftOut.setup(blockEntity, axis, getOutputSpeed()).setChanged();
        cogwheel.setup(blockEntity, axis, getCogSpeed()).setChanged();
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    public void updateLight(float partialTick) {
        relight(shaftIn, shaftOut, cogwheel);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(shaftIn);
        consumer.accept(shaftOut);
        consumer.accept(cogwheel);
    }

    @Override
    protected void _delete() {
        shaftIn.delete();
        shaftOut.delete();
        cogwheel.delete();
    }
}