package com.jefrex.createspace.item;

import com.jefrex.createspace.CreateSpace;
import com.jefrex.createspace.item.custom.AirGun;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreateSpace.MODID);

    public static final DeferredItem<Item> AIRGUN = ITEMS.register("air_gun",
            () -> new AirGun(new Item.Properties().durability(128)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
