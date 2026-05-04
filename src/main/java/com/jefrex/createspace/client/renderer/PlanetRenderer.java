package com.jefrex.createspace.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import net.minecraft.client.Camera;
import net.minecraft.resources.ResourceLocation;
import org.joml.*;

public class PlanetRenderer {

    private static final ResourceLocation SHADER =
            ResourceLocation.fromNamespaceAndPath("create_space", "planet");

    public static final double PLANET_X = 30;
    public static final double PLANET_Y = 100;
    public static final double PLANET_Z = -52;
    public static final float PLANET_RADIUS = 500.0f;

    private static float rotation = 0.0f;

    public static void render(PoseStack poseStack, Matrix4f projectionMatrix, Camera camera) {
        ShaderProgram shader = VeilRenderSystem.setShader(SHADER);
        if (shader == null) return;

        rotation += 0.00025f;

        var camPos = camera.getPosition();

        Vector3f relativePos = new Vector3f(
                (float)(PLANET_X - camPos.x),
                (float)(PLANET_Y - camPos.y),
                (float)(PLANET_Z - camPos.z)
        );

        Vector3f lookVector = new Vector3f(camera.getLookVector());
        Vector3f upVector = new Vector3f(camera.getUpVector());
        Matrix4f rotationOnlyView = new Matrix4f().setLookAt(
                0, 0, 0,
                lookVector.x, lookVector.y, lookVector.z,
                upVector.x, upVector.y, upVector.z
        );

        Vector4f spherePosView = new Vector4f(relativePos, 1.0f).mul(rotationOnlyView);

        Matrix4f invProj = new Matrix4f(projectionMatrix).invert();
        Matrix4f invView = new Matrix4f(rotationOnlyView).invert();
        Matrix4f planetRotation = new Matrix4f().rotateY(rotation);

        var uSpherePos = shader.getUniform("uSpherePos");
        if (uSpherePos != null) uSpherePos.setVector(
                -spherePosView.x,
                -spherePosView.y,
                -spherePosView.z
        );

        var uScale = shader.getUniform("uScale");
        if (uScale != null) uScale.setFloat(PLANET_RADIUS);

        var uInvProj = shader.getUniform("uInvProj");
        if (uInvProj != null) uInvProj.setMatrix(invProj);

        var uInvView = shader.getUniform("uInvView");
        if (uInvView != null) uInvView.setMatrix(invView);

        var uPlanetRotation = shader.getUniform("uPlanetRotation");
        if (uPlanetRotation != null) uPlanetRotation.setMatrix(planetRotation);

        var uTime = shader.getUniform("uTime");
        if (uTime != null) uTime.setFloat(rotation * 0.5f);

        RenderSystem.setShaderTexture(1, ResourceLocation.fromNamespaceAndPath("create_space", "textures/planet/clouds.png"));

        var uSpherePosWorld = shader.getUniform("uSpherePosWorld");
        if (uSpherePosWorld != null) uSpherePosWorld.setVector(
                relativePos.x,
                relativePos.y,
                relativePos.z
        );

        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buf = tesselator.begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);

        buf.addVertex(-1.0f, -1.0f, 0.0f);
        buf.addVertex( 1.0f, -1.0f, 0.0f);
        buf.addVertex( 1.0f,  1.0f, 0.0f);
        buf.addVertex(-1.0f,  1.0f, 0.0f);

        shader.bind();
        BufferUploader.drawWithShader(buf.buildOrThrow());
        ShaderProgram.unbind();

        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }
}