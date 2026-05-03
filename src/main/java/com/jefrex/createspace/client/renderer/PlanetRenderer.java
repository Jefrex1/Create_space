package com.jefrex.createspace.client.renderer;

import com.jefrex.createspace.CreateSpace;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import net.minecraft.client.Camera;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class PlanetRenderer {

    private static final ResourceLocation SHADER =
            ResourceLocation.fromNamespaceAndPath("create_space", "planet");

    // позиція планети у світових координатах
    public static final double PLANET_X = 0;
    public static final double PLANET_Y = 200;
    public static final double PLANET_Z = 0;
    public static final float PLANET_RADIUS = 30.0f;

    public static void render(PoseStack poseStack, Matrix4f projectionMatrix, Camera camera) {
        ShaderProgram shader = VeilRenderSystem.setShader(SHADER);
        if (shader == null) return;

        // зсуваємо на позицію планети відносно камери
        double camX = camera.getPosition().x;
        double camY = camera.getPosition().y;
        double camZ = camera.getPosition().z;

// рахуємо відносну позицію у double точності
        double relX = PLANET_X - camX;
        double relY = PLANET_Y - camY;
        double relZ = PLANET_Z - camZ;

        poseStack.pushPose();
        poseStack.translate(relX, relY, relZ);

        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buf = tesselator.begin(
                VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX);

        Matrix4f mat = poseStack.last().pose();
        int stacks = 32, slices = 32;
        float r = PLANET_RADIUS;

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

        BufferUploader.drawWithShader(buf.buildOrThrow());

        RenderSystem.enableCull();
        poseStack.popPose();

    }
}