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

        ResourceLocation currentDim = entity.level().dimension().location();
        ResourceLocation spaceDim = ResourceLocation.fromNamespaceAndPath("create_space", "space");

        if (currentDim.equals(spaceDim)) {
            ci.cancel();
        }
    }
}