package com.jefrex.createspace;

import com.jefrex.createspace.block.ModBlocks;
import com.jefrex.createspace.client.renderer.*;
import com.jefrex.createspace.particle.TurbineFlameParticle;
import com.jefrex.createspace.registry.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;

public class CreateSpaceClient {

    public CreateSpaceClient(ModContainer container, IEventBus modBus) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        modBus.addListener(this::onRegisterReloadListeners);
        modBus.addListener(this::onClientSetup);
        modBus.addListener(CreateSpaceClient::registerParticleFactories); // ← додано

        NeoForge.EVENT_BUS.addListener(this::onRenderLevel);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        CreateSpace.LOGGER.info("HELLO FROM CLIENT SETUP");
        CreateSpace.LOGGER.info("MINECRAFT NAME >> {}",
                Minecraft.getInstance().getUser().getName());

        ItemBlockRenderTypes.setRenderLayer(ModBlocks.ATMOSPHERE.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.AIR_FIELD_AIR.get(), RenderType.translucent());
    }

    private void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new PlanetRegistry());
        event.registerReloadListener(new SkyRegistry());
    }

    // @SubscribeEvent прибрано — реєструємо через addListener вище
    public static void registerParticleFactories(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(
                ModParticles.TURBINE_FLAME.get(),
                TurbineFlameParticle.Provider::new
        );
    }

    private void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

        var mc = Minecraft.getInstance();
        if (mc.level == null) return;

        String dimId = mc.level.dimension().location().toString();

        if (mc.level.dimension().equals(CreateSpace.SPACE_LEVEL)) {
            PlanetRenderer.render(
                    event.getPoseStack(),
                    event.getProjectionMatrix(),
                    mc.gameRenderer.getMainCamera()
            );
            return;
        }

        List<SkyObjectConfig> objects = SkyRegistry.getForDimension(dimId);
        if (!objects.isEmpty()) {
            SkyObjectAdapter.render(
                    event.getPoseStack(),
                    event.getProjectionMatrix(),
                    mc.gameRenderer.getMainCamera(),
                    objects
            );
        }
    }
}