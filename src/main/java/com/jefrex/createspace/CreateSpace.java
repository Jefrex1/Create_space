package com.jefrex.createspace;

import com.jefrex.createspace.block.ModBlocks;
import com.jefrex.createspace.entity.MeteoriteRenderer;
import com.jefrex.createspace.entity.ModEntities;
import com.jefrex.createspace.event.MeteoriteEvent;
import com.jefrex.createspace.item.ModCreativeModeTabs;
import com.jefrex.createspace.item.ModItems;
import com.jefrex.createspace.network.AirGunShootPayload;
import com.jefrex.createspace.physic.DimensionConfig;
import com.jefrex.createspace.registry.ModBlockEntities;
import com.jefrex.createspace.registry.ModParticles;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

@Mod(CreateSpace.MODID)
public class CreateSpace {
    public static final String MODID = "create_space";
    public static final Logger LOGGER = LogUtils.getLogger();


    public CreateSpace(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("=== CREATE SPACE CONSTRUCTOR CALLED ===");
        modEventBus.addListener(this::commonSetup);

        ModCreativeModeTabs.register(modEventBus);

        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModParticles.PARTICLE_TYPES.register(modEventBus);
        // CreateSpace.java
        modEventBus.addListener((FMLCommonSetupEvent e) -> DimensionConfig.load());

        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(MeteoriteEvent.class);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);

        modEventBus.addListener(this::addCreative);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            new CreateSpaceClient(modContainer, modEventBus);
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if(event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
             event.accept(ModItems.AIRGUN);
        }
        if(event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModBlocks.GRAVITY_FIELD);
        }
    }

    public static final ResourceKey<Level> SPACE_LEVEL =
            ResourceKey.create(Registries.DIMENSION,
                    ResourceLocation.fromNamespaceAndPath(MODID, "space"));


    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

    @EventBusSubscriber(modid = "create_space", bus = EventBusSubscriber.Bus.MOD)
    public class ModNetworking {

        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event) {
            final PayloadRegistrar registrar = event.registrar("1.0.0"); // версія протоколу

            registrar.playToServer(
                    AirGunShootPayload.TYPE,
                    AirGunShootPayload.STREAM_CODEC,
                    AirGunShootPayload::handle
            );
        }
        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntities.METEORITE.get(), MeteoriteRenderer::new);
        }
    }
}