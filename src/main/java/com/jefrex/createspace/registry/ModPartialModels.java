package com.jefrex.createspace.registry;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;

public class ModPartialModels {
    public static final PartialModel ACCUMULATOR_CORE =
            PartialModel.of(ResourceLocation.fromNamespaceAndPath("create_space", "block/rotational_accumulator_accelerator"));

    public static void init() {}
}