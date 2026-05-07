package com.jefrex.createspace;

import com.jefrex.createspace.client.renderer.PlanetRegistry;
import com.jefrex.createspace.client.renderer.PlanetRenderer;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = CreateSpace.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = CreateSpace.MODID, value = Dist.CLIENT)
public class CreateSpaceClient {

    public CreateSpaceClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        CreateSpace.LOGGER.info("HELLO FROM CLIENT SETUP");
        CreateSpace.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {

    }

    @SubscribeEvent
    static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new PlanetRegistry());
    }

    @SubscribeEvent
    static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.level == null) return;
        //if (!mc.level.dimension().equals(CreateSpace.SPACE_LEVEL)) return;

        PlanetRenderer.render(
                event.getPoseStack(),
                event.getProjectionMatrix(),
                mc.gameRenderer.getMainCamera()
        );
    }

}