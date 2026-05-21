package com.jefrex.createspace.item;

import com.jefrex.createspace.CreateSpace;
import com.jefrex.createspace.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateSpace.MODID);

    public static final Supplier<CreativeModeTab> CREATE_SPACE_TAB = CREATIVE_MODE_TABS.register("create_space_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.AIRGUN.get()))
                    .title(Component.translatable("creativetab.createspace.createspacetab"))
                    .displayItems((itemDisplayParameters, output) -> {

                        //Items
                        output.accept(ModItems.AIRGUN);

                        //Mechanics Blocks
                        output.accept(ModBlocks.GRAVITY_FIELD);
                        output.accept(ModBlocks.AIR_FIELD);
                        output.accept(ModBlocks.TURBINE);
                        output.accept(ModBlocks.ROTATIONAL_ACCUMULATOR);

                        //Blocks
                        output.accept(ModBlocks.REGOLITH);
                        output.accept(ModBlocks.LUNAR_BIT);

                        //Ores
                        output.accept(ModItems.ALUMINIUM_NUGGET);
                        output.accept(ModItems.ALUMINIUM_INGOT);
                        output.accept(ModBlocks.ALUMINIUM_ORE);


                        //💀💀🥀🥀
                        output.accept(ModBlocks.BRDISH);
                        output.accept(ModBlocks.CHEREMSHA);
                    }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }

}
