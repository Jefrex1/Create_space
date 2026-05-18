package com.jefrex.createspace.block;

import com.jefrex.createspace.CreateSpace;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
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

    public static final DeferredItem<BlockItem> GRAVITY_FIELD_ITEM =
            ITEMS.registerSimpleBlockItem("gravity_field", GRAVITY_FIELD);

    public static final DeferredBlock<Block> AIR_FIELD =
            BLOCKS.register("air_field", AirFieldBlock::new);

    public static final DeferredBlock<Block> AIR_FIELD_AIR =
            BLOCKS.register("air_field_air", AirFieldAirBlock::new);

    public static final DeferredItem<BlockItem> AIR_FIELD_ITEM =
            ITEMS.registerSimpleBlockItem("air_field", AIR_FIELD);

    public static final DeferredBlock<Block> REGOLITH =
            BLOCKS.register("regolith", RegolithBlock::new);

    public static final DeferredItem<BlockItem> REGOLITH_ITEM =
            ITEMS.registerSimpleBlockItem("regolith", REGOLITH);

    public static final DeferredBlock<LunarBitBlock> LUNAR_BIT =
            BLOCKS.registerBlock("lunar_bit", LunarBitBlock::new,
                    BlockBehaviour.Properties.of()
                            .strength(0.1f, 3.0f)
                            .sound(SoundType.STONE)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .noOcclusion());

    public static final DeferredItem<BlockItem> LUNAR_BIT_ITEM =
            ITEMS.registerSimpleBlockItem("lunar_bit", LUNAR_BIT);

    public static final DeferredBlock<Block> CHEREMSHA =
            BLOCKS.register("cheremsha", CheremshaBlock::new);

    public static final DeferredItem<BlockItem> CHEREMSHA_ITEM =
            ITEMS.registerSimpleBlockItem("cheremsha", CHEREMSHA);

    public static final DeferredBlock<Block> BRDISH =
            BLOCKS.register("brdish", CheremshaBlock::new);

    public static final DeferredItem<BlockItem> BRDISH_ITEM =
            ITEMS.registerSimpleBlockItem("brdish", BRDISH);

    public static final DeferredBlock<TurbineBlock> TURBINE =
            BLOCKS.register("turbine", TurbineBlock::new);

    public static final DeferredItem<BlockItem> TURBINE_ITEM =
            ITEMS.registerSimpleBlockItem("turbine", TURBINE);

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
    }
}