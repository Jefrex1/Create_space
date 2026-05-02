package com.jefrex.createspace.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.EXTEfx;

@EventBusSubscriber(modid = "create_space", value = Dist.CLIENT)
public class SpaceReverbHandler {

    private static int reverbEffect = -1;
    private static int reverbSlot = -1;
    private static boolean initialized = false;

    private static final ResourceKey SPACE_DIM = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath("create_space", "space")
    );

    public static void init() {
        if (initialized) return;

        // створюємо auxiliary effect slot
        reverbSlot = EXTEfx.alGenAuxiliaryEffectSlots();
        reverbEffect = EXTEfx.alGenEffects();

        // налаштовуємо реверб як "космічний" (великий простір, довге затухання)
        EXTEfx.alEffecti(reverbEffect, EXTEfx.AL_EFFECT_TYPE, EXTEfx.AL_EFFECT_REVERB);
        EXTEfx.alEffectf(reverbEffect, EXTEfx.AL_REVERB_DECAY_TIME, 8.0f);       // довге затухання
        EXTEfx.alEffectf(reverbEffect, EXTEfx.AL_REVERB_DECAY_HFRATIO, 0.5f);   // темне ехо
        EXTEfx.alEffectf(reverbEffect, EXTEfx.AL_REVERB_DIFFUSION, 0.3f);        // мало дифузії
        EXTEfx.alEffectf(reverbEffect, EXTEfx.AL_REVERB_GAIN, 0.8f);
        EXTEfx.alEffectf(reverbEffect, EXTEfx.AL_REVERB_GAINHF, 0.1f);           // приглушені високі
        EXTEfx.alEffectf(reverbEffect, EXTEfx.AL_REVERB_ROOM_ROLLOFF_FACTOR, 0.0f);

        // підключаємо ефект до слоту
        EXTEfx.alAuxiliaryEffectSloti(reverbSlot, EXTEfx.AL_EFFECTSLOT_EFFECT, reverbEffect);

        initialized = true;
    }

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!mc.player.level().dimension().equals(SPACE_DIM)) return;

        if (!initialized) init();

        // застосовуємо реверб до джерела звуку
        // SoundEngine не дає прямого доступу до AL source ID через публічний API
        // тому використовуємо рефлексію або mixin
    }
}