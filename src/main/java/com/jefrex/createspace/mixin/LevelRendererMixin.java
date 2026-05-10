package com.jefrex.createspace.mixin;

import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    private boolean isOverworld() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        return mc.level.dimension().location()
                .equals(ResourceLocation.fromNamespaceAndPath("minecraft", "overworld"));
    }

    @Redirect(
            method = "renderSky(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/BufferUploader;drawWithShader(Lcom/mojang/blaze3d/vertex/MeshData;)V",
                    ordinal = 1  // sun
            )
    )
    private void skipSunDraw(MeshData meshData) {
        if (isOverworld()) {
            meshData.close(); // закриваємо щоб не було warning
        } else {
            BufferUploader.drawWithShader(meshData);
        }
    }

    @Redirect(
            method = "renderSky(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/BufferUploader;drawWithShader(Lcom/mojang/blaze3d/vertex/MeshData;)V",
                    ordinal = 2  // moon
            )
    )
    private void skipMoonDraw(MeshData meshData) {
        if (isOverworld()) {
            meshData.close();
        } else {
            BufferUploader.drawWithShader(meshData);
        }
    }
}