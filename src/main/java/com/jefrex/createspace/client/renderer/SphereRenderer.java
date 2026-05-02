package com.jefrex.createspace.client.renderer;

import com.jefrex.createspace.entity.SphereEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class SphereRenderer extends EntityRenderer<SphereEntity> {

    private static final ResourceLocation SHADER =
            ResourceLocation.fromNamespaceAndPath("create_space", "test");

    public SphereRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(SphereEntity entity, float yaw, float partialTick,
                       PoseStack stack, MultiBufferSource source, int light) {

        ShaderProgram shader = VeilRenderSystem.setShader(SHADER);
        if (shader == null) return;

        stack.pushPose();
        RenderSystem.disableCull();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buf = tesselator.begin(
                VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX);

        Matrix4f mat = stack.last().pose();
        int stacks = 16, slices = 16;
        float r = 0.5f;

        for (int i = 0; i < stacks; i++) {
            float phi1 = (float)(Math.PI * i / stacks);
            float phi2 = (float)(Math.PI * (i + 1) / stacks);
            for (int j = 0; j < slices; j++) {
                float theta1 = (float)(2 * Math.PI * j / slices);
                float theta2 = (float)(2 * Math.PI * (j + 1) / slices);

                float x1 = (float)(r * Math.sin(phi1) * Math.cos(theta1));
                float y1 = (float)(r * Math.cos(phi1));
                float z1 = (float)(r * Math.sin(phi1) * Math.sin(theta1));

                float x2 = (float)(r * Math.sin(phi2) * Math.cos(theta1));
                float y2 = (float)(r * Math.cos(phi2));
                float z2 = (float)(r * Math.sin(phi2) * Math.sin(theta1));

                float x3 = (float)(r * Math.sin(phi1) * Math.cos(theta2));
                float y3 = (float)(r * Math.cos(phi1));
                float z3 = (float)(r * Math.sin(phi1) * Math.sin(theta2));

                float x4 = (float)(r * Math.sin(phi2) * Math.cos(theta2));
                float y4 = (float)(r * Math.cos(phi2));
                float z4 = (float)(r * Math.sin(phi2) * Math.sin(theta2));

                float u1 = (float)j / slices;
                float u2 = (float)(j + 1) / slices;
                float v1 = (float)i / stacks;
                float v2 = (float)(i + 1) / stacks;

                buf.addVertex(mat, x1, y1, z1).setUv(u1, v1);
                buf.addVertex(mat, x2, y2, z2).setUv(u1, v2);
                buf.addVertex(mat, x3, y3, z3).setUv(u2, v1);

                buf.addVertex(mat, x2, y2, z2).setUv(u1, v2);
                buf.addVertex(mat, x4, y4, z4).setUv(u2, v2);
                buf.addVertex(mat, x3, y3, z3).setUv(u2, v1);
            }
        }

        shader.bind();
        BufferUploader.drawWithShader(buf.buildOrThrow());
        ShaderProgram.unbind();

        RenderSystem.enableCull();
        stack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(SphereEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("create_space", "textures/entity/sphere.png");
    }
}