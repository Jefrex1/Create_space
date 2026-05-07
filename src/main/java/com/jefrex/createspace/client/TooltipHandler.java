package com.jefrex.createspace.client;

import com.simibubi.create.content.equipment.armor.DivingBootsItem;
import com.simibubi.create.content.equipment.armor.DivingHelmetItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class TooltipHandler {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        if (stack.getItem() instanceof DivingHelmetItem) {
            event.getToolTip().add(1,
                    Component.translatable("tooltip.createspace.diving_helmet_space")
                            .withStyle(ChatFormatting.AQUA)
            );
        }

        if (stack.getItem() instanceof DivingBootsItem) {
            event.getToolTip().add(1,
                    Component.translatable("tooltip.createspace.diving_boots_space")
                            .withStyle(ChatFormatting.AQUA)
            );
        }
    }
}