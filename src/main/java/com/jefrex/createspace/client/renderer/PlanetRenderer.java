package com.jefrex.createspace.client.renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import org.joml.*;
import org.lwjgl.opengl.GL13;

import java.util.HashMap;
import java.util.Map;

public class PlanetRenderer {

    private static final ResourceLocation SHADER = ResourceLocation.fromNamespaceAndPath("create_space", "planet");
    private static final ResourceLocation SKY_SHADER = ResourceLocation.fromNamespaceAndPath("create_space", "skybox");
    private static final ResourceLocation SKY_TEXTURE = ResourceLocation.fromNamespaceAndPath("create_space", "textures/sky/space_sky.png");

    private static final Map<String, Float> rotations = new HashMap<>();

    public static void render(PoseStack poseStack, Matrix4f projectionMatrix, Camera camera) {
        // 1. Рендеримо скайбокс на фоні
        renderSkybox(projectionMatrix, camera);

        // 2. Рендеримо планети поверх неба
        for (PlanetConfig planet : PlanetRegistry.getPlanets()) {
            renderPlanet(planet, projectionMatrix, camera);
        }
    }

    private static void renderSkybox(Matrix4f projectionMatrix, Camera camera) {
        ShaderProgram shader = VeilRenderSystem.setShader(SKY_SHADER);
        if (shader == null) return;

        // Інвертуємо проекцію для променів
        Matrix4f invProj = new Matrix4f(projectionMatrix).invert();

        // Отримуємо чисте обертання камери через кватерніон
        // Це змусить небо повертатися, коли ти крутиш мишкою
        Matrix4f viewMatrix = new Matrix4f().rotation(camera.rotation());

        setUniformMat4(shader, "uInvProj", invProj);
        setUniformMat4(shader, "uViewMatrix", viewMatrix); // Передаємо матрицю обертання
        setUniformFloat(shader, "uRotation", (System.currentTimeMillis() % 360000) / 20000.0f);
        setUniformInt(shader, "SkyboxTexture", 0);

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buf = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        // Квад на весь екран
        buf.addVertex(-1.0f, -1.0f, 1.0f);
        buf.addVertex( 1.0f, -1.0f, 1.0f);
        buf.addVertex( 1.0f,  1.0f, 1.0f);
        buf.addVertex(-1.0f,  1.0f, 1.0f);

        shader.bind();
        TextureManager tm = Minecraft.getInstance().getTextureManager();
        GlStateManager._activeTexture(GL13.GL_TEXTURE0);
        GlStateManager._bindTexture(tm.getTexture(SKY_TEXTURE).getId());

        BufferUploader.drawWithShader(buf.buildOrThrow());
        ShaderProgram.unbind();

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }

    private static void renderPlanet(PlanetConfig planet, Matrix4f projectionMatrix, Camera camera) {
        ShaderProgram shader = VeilRenderSystem.setShader(SHADER);
        if (shader == null) return;

        setUniformInt(shader, "PlanetTexture", 0);
        setUniformInt(shader, "CloudTexture", 1);

        float rotation = rotations.getOrDefault(planet.id(), 0.0f) + planet.rotationSpeed();
        rotations.put(planet.id(), rotation);

        var camPos = camera.getPosition();
        Vector3f relativePos = new Vector3f(
                (float)(planet.x() - camPos.x),
                (float)(planet.y() - camPos.y),
                (float)(planet.z() - camPos.z)
        );

        Vector3f lookVector = new Vector3f(camera.getLookVector());
        Vector3f upVector   = new Vector3f(camera.getUpVector());
        Matrix4f rotationOnlyView = new Matrix4f().setLookAt(0, 0, 0, lookVector.x, lookVector.y, lookVector.z, upVector.x, upVector.y, upVector.z);

        Vector4f spherePosView  = new Vector4f(relativePos, 1.0f).mul(rotationOnlyView);
        Matrix4f invProj        = new Matrix4f(projectionMatrix).invert();
        Matrix4f invView        = new Matrix4f(rotationOnlyView).invert();
        Matrix4f planetRotation = new Matrix4f().rotateY(rotation);

        setUniformVec3(shader, "uSpherePos", -spherePosView.x, -spherePosView.y, -spherePosView.z);
        setUniformFloat(shader, "uScale", planet.radius());
        setUniformMat4(shader, "uInvProj", invProj);
        setUniformMat4(shader, "uInvView", invView);
        setUniformMat4(shader, "uPlanetRotation", planetRotation);
        setUniformFloat(shader, "uTime", rotation * 0.5f);
        setUniformVec3(shader, "uSpherePosWorld", relativePos.x, relativePos.y, relativePos.z);

        PlanetConfig.SunConfig sun = planet.sun();
        Vector3f sunDir = new Vector3f(sun.x(), sun.y(), sun.z()).normalize();
        setUniformVec3(shader, "uSunDir", sunDir.x, sunDir.y, sunDir.z);

        setUniformFloat(shader, "uHasClouds", planet.cloudTexture() != null ? 1.0f : 0.0f);
        setUniformFloat(shader, "uCloudSpeed", planet.cloudSpeed());

        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        float zDepth = 1.0f;
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buf = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        buf.addVertex(-1.0f, -1.0f, zDepth);
        buf.addVertex( 1.0f, -1.0f, zDepth);
        buf.addVertex( 1.0f,  1.0f, zDepth);
        buf.addVertex(-1.0f,  1.0f, zDepth);

        shader.bind();
        TextureManager tm = Minecraft.getInstance().getTextureManager();

        GlStateManager._activeTexture(GL13.GL_TEXTURE0);
        GlStateManager._bindTexture(tm.getTexture(ResourceLocation.parse(planet.texture())).getId());

        GlStateManager._activeTexture(GL13.GL_TEXTURE1);
        if (planet.cloudTexture() != null) {
            GlStateManager._bindTexture(tm.getTexture(ResourceLocation.parse(planet.cloudTexture())).getId());
        }

        GlStateManager._activeTexture(GL13.GL_TEXTURE0);

        BufferUploader.drawWithShader(buf.buildOrThrow());
        ShaderProgram.unbind();
    }

    private static void setUniformVec3(ShaderProgram s, String name, float x, float y, float z) {
        var u = s.getUniform(name); if (u != null) u.setVector(x, y, z);
    }
    private static void setUniformFloat(ShaderProgram s, String name, float v) {
        var u = s.getUniform(name); if (u != null) u.setFloat(v);
    }
    private static void setUniformMat4(ShaderProgram s, String name, Matrix4f m) {
        var u = s.getUniform(name); if (u != null) u.setMatrix(m);
    }
    private static void setUniformInt(ShaderProgram s, String name, int v) {
        var u = s.getUniform(name); if (u != null) u.setInt(v);
    }
}