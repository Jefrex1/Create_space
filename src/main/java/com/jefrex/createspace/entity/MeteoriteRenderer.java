package com.jefrex.createspace.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class MeteoriteRenderer extends EntityRenderer<MeteoriteEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("create_space", "textures/entity/meteorite.png");

    public MeteoriteRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(MeteoriteEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {

        poseStack.pushPose();

        float s = entity.getMeteorSize(); // береться тут, один раз

        poseStack.translate(0, 1, 0);
        poseStack.mulPose(Axis.ZP.rotationDegrees(35));
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getYRot() + partialTick * 4));
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot() + partialTick * 6));
        poseStack.scale(s, s, s); // масштаб вже тут

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutout(TEXTURE));

        renderCube(poseStack, vertexConsumer, packedLight);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private void renderCube(PoseStack poseStack, VertexConsumer consumer, int light) {
        Matrix4f matrix = poseStack.last().pose();
        int overlay = OverlayTexture.NO_OVERLAY;
        float s = 0.5f; // фіксований розмір грані, масштаб задається через poseStack.scale вище

        renderFace(matrix, consumer, overlay, light,
                -s, -s,  s,  s, -s,  s,  s,  s,  s, -s,  s,  s,  0,  0,  1);
        renderFace(matrix, consumer, overlay, light,
                s, -s, -s, -s, -s, -s, -s,  s, -s,  s,  s, -s,  0,  0, -1);
        renderFace(matrix, consumer, overlay, light,
                -s, -s, -s, -s, -s,  s, -s,  s,  s, -s,  s, -s, -1,  0,  0);
        renderFace(matrix, consumer, overlay, light,
                s, -s,  s,  s, -s, -s,  s,  s, -s,  s,  s,  s,  1,  0,  0);
        renderFace(matrix, consumer, overlay, light,
                -s,  s,  s,  s,  s,  s,  s,  s, -s, -s,  s, -s,  0,  1,  0);
        renderFace(matrix, consumer, overlay, light,
                -s, -s, -s,  s, -s, -s,  s, -s,  s, -s, -s,  s,  0, -1,  0);
    }

    private void renderFace(Matrix4f matrix, VertexConsumer consumer, int overlay, int light,
                            float x1, float y1, float z1,
                            float x2, float y2, float z2,
                            float x3, float y3, float z3,
                            float x4, float y4, float z4,
                            float nx, float ny, float nz) {
        consumer.addVertex(matrix, x1, y1, z1).setColor(255,255,255,255).setUv(0,1).setOverlay(overlay).setLight(light).setNormal(nx,ny,nz);
        consumer.addVertex(matrix, x2, y2, z2).setColor(255,255,255,255).setUv(1,1).setOverlay(overlay).setLight(light).setNormal(nx,ny,nz);
        consumer.addVertex(matrix, x3, y3, z3).setColor(255,255,255,255).setUv(1,0).setOverlay(overlay).setLight(light).setNormal(nx,ny,nz);
        consumer.addVertex(matrix, x4, y4, z4).setColor(255,255,255,255).setUv(0,0).setOverlay(overlay).setLight(light).setNormal(nx,ny,nz);
    }

    @Override
    public ResourceLocation getTextureLocation(MeteoriteEntity entity) {
        return TEXTURE;
    }
}