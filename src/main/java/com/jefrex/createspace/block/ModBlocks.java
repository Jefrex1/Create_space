package com.jefrex.createspace.block;

import com.jefrex.createspace.CreateSpace;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CreateSpace.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreateSpace.MODID);

    public static final DeferredBlock<Block> GRAVITY_FIELD =
            BLOCKS.register("gravity_field", GravityFieldBlock::new);

    public static final DeferredBlock<Block> ATMOSPHERE =
            BLOCKS.register("atmosphere", AtmosphereBlock::new);

    // предмет тільки для gravity_field, атмосфера не має предмету
    public static final DeferredItem<BlockItem> GRAVITY_FIELD_ITEM =
            ITEMS.registerSimpleBlockItem("gravity_field", GRAVITY_FIELD);

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
    }
}