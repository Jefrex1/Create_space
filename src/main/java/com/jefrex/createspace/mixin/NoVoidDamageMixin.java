package com.jefrex.createspace.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class NoVoidDamageMixin {

    @Inject(method = "checkBelowWorld", at = @At("HEAD"), cancellable = true)
    private void cancelVoidKill(CallbackInfo ci) {
        Entity entity = (Entity)(Object)this;

        // Тільки в космічному вимірі
        if (!entity.level().dimension().location()
                .equals(ResourceLocation.fromNamespaceAndPath("create_space", "space"))) return;

        ci.cancel(); // скасовуємо void kill повністю
    }
}