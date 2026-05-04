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
    private static final Map<String, Float> rotations = new HashMap<>();

    public static void render(PoseStack poseStack, Matrix4f projectionMatrix, Camera camera) {
        for (PlanetConfig planet : PlanetRegistry.getPlanets()) {
            renderPlanet(planet, projectionMatrix, camera);
        }
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

        // ВАЖЛИВО: Стани для перекриття блоками
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        // Малюємо квад дуже далеко (на фоні), але шейдер gl_FragDepth виправить це для кожної точки
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