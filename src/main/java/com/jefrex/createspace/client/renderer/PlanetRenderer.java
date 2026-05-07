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

import java.lang.Math;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlanetRenderer {

    private static final ResourceLocation SHADER = ResourceLocation.fromNamespaceAndPath("create_space", "planet");
    private static final ResourceLocation RING_SHADER = ResourceLocation.fromNamespaceAndPath("create_space", "ring");
    private static final ResourceLocation SKY_SHADER = ResourceLocation.fromNamespaceAndPath("create_space", "skybox");
    private static final ResourceLocation SKY_TEXTURE = ResourceLocation.fromNamespaceAndPath("create_space", "textures/sky/space_sky.png");

    private static final Map<String, Float> rotations = new HashMap<>();


    public static void render(PoseStack poseStack, Matrix4f projectionMatrix, Camera camera) {
        renderSkybox(projectionMatrix, camera);

        var camPos = camera.getPosition();

        List<PlanetConfig> sorted = PlanetRegistry.getPlanets().stream()
                .sorted((a, b) -> {
                    double da = Math.pow(a.x() - camPos.x, 2) + Math.pow(a.y() - camPos.y, 2) + Math.pow(a.z() - camPos.z, 2);
                    double db = Math.pow(b.x() - camPos.x, 2) + Math.pow(b.y() - camPos.y, 2) + Math.pow(b.z() - camPos.z, 2);
                    return Double.compare(db, da);
                })
                .toList();

        for (PlanetConfig planet : sorted) {
            renderPlanet(planet, projectionMatrix, camera);
            if (planet.ring() != null) {
                renderRing(planet, projectionMatrix, camera);
            }
        }
    }

    private static Vector3f getSunPosition() {
        return PlanetRegistry.getPlanets().stream()
                .filter(PlanetConfig::isStar)
                .findFirst()
                .map(s -> new Vector3f((float)s.x(), (float)s.y(), (float)s.z()))
                .orElse(null);
    }

    private static void renderSkybox(Matrix4f projectionMatrix, Camera camera) {
        ShaderProgram shader = VeilRenderSystem.setShader(SKY_SHADER);
        if (shader == null) return;

        Matrix4f invProj = new Matrix4f(projectionMatrix).invert();
        Matrix4f viewMatrix = new Matrix4f().rotation(camera.rotation());

        setUniformMat4(shader, "uInvProj", invProj);
        setUniformMat4(shader, "uViewMatrix", viewMatrix);
        setUniformFloat(shader, "uRotation", (System.currentTimeMillis() % 360000) / 20000.0f);
        setUniformInt(shader, "SkyboxTexture", 0);

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buf = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
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
        setUniformFloat(shader, "uPlasmaTime", (System.currentTimeMillis() % 1000000) / 1000.0f);
        setUniformVec3(shader, "uSpherePosWorld", relativePos.x, relativePos.y, relativePos.z);

        Vector3f sunPos = getSunPosition();
        Vector3f sunDir = sunPos != null
                ? new Vector3f(
                (float)(sunPos.x - planet.x()),
                (float)(sunPos.y - planet.y()),
                (float)(sunPos.z - planet.z())
        ).normalize()
                : new Vector3f(planet.sun().x(), planet.sun().y(), planet.sun().z()).normalize();
        setUniformVec3(shader, "uSunDir", sunDir.x, sunDir.y, sunDir.z);

        setUniformFloat(shader, "uHasClouds", planet.cloudTexture() != null ? 1.0f : 0.0f);
        setUniformFloat(shader, "uCloudSpeed", planet.cloudSpeed());

        setUniformFloat(shader, "uIsStar", planet.isStar() ? 1.0f : 0.0f);
        setUniformVec3(shader, "uStarColor", 1.0f, 0.9f, 0.6f);

        setUniformFloat(shader, "uPlasmaSpeed",      0.3f);
        setUniformFloat(shader, "uPlasmaScale",      3.0f);
        setUniformFloat(shader, "uCoronalIntensity", 1.5f);

        float atmRadius = planet.isStar()
                ? planet.radius() * 6.0f
                : planet.radius() * 1.06f;
        setUniformFloat(shader, "uAtmosphereRadius", atmRadius);
        setUniformVec3(shader, "uRayleighCoeff", 5.8e-3f, 13.5e-3f, 33.1e-3f);
        setUniformFloat(shader, "uMieCoeff", 21e-3f);
        setUniformFloat(shader, "uAtmosphereStrength", planet.atmosphereStrength());

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

    private static void renderRing(PlanetConfig planet, Matrix4f projectionMatrix, Camera camera) {
        ShaderProgram shader = VeilRenderSystem.setShader(RING_SHADER);
        if (shader == null) return;

        setUniformInt(shader, "RingTexture", 0);

        var camPos = camera.getPosition();
        Vector3f relativePos = new Vector3f(
                (float)(planet.x() - camPos.x),
                (float)(planet.y() - camPos.y),
                (float)(planet.z() - camPos.z)
        );

        Vector3f lookVector = new Vector3f(camera.getLookVector());
        Vector3f upVector   = new Vector3f(camera.getUpVector());

        // Матриця повороту камери
        Matrix4f rotationOnlyView = new Matrix4f().setLookAt(
                0, 0, 0,
                lookVector.x, lookVector.y, lookVector.z,
                upVector.x, upVector.y, upVector.z
        );

        // Позиція центру планети в просторі камери
        Vector4f spherePosView = new Vector4f(relativePos, 1.0f).mul(rotationOnlyView);

        // ОБЧИСЛЕННЯ НОРМАЛІ КІЛЬЦЯ
        // Починаємо з вертикалі (локальна вісь Y планети)
        Vector4f ringNormal = new Vector4f(0.0f, 1.0f, 0.0f, 0.0f);
        // Нахиляємо згідно з конфігом (наприклад, 23 градуси для Сатурна)
        ringNormal.rotateX((float) Math.toRadians(planet.ring().tilt()));
        // Трансформуємо в простір камери, щоб шейдер бачив нормаль так само, як бачить камеру
        ringNormal.mul(rotationOnlyView);

        Matrix4f invProj = new Matrix4f(projectionMatrix).invert();

        // Передаємо уніформи
        setUniformVec3(shader, "uSpherePos", spherePosView.x, spherePosView.y, spherePosView.z);
        setUniformVec3(shader, "uRingNormalView", ringNormal.x, ringNormal.y, ringNormal.z);
        setUniformFloat(shader, "uScale", planet.radius());
        setUniformMat4(shader, "uInvProj", invProj);
        setUniformFloat(shader, "uInnerRadius", planet.ring().innerRadius());
        setUniformFloat(shader, "uOuterRadius", planet.ring().outerRadius());
        setUniformFloat(shader, "uOpacity", planet.ring().opacity());

        // Сонце для освітлення
        Vector3f sunPos = getSunPosition();
        Vector3f sunDirWorld = sunPos != null
                ? new Vector3f((float)(sunPos.x - planet.x()), (float)(sunPos.y - planet.y()), (float)(sunPos.z - planet.z())).normalize()
                : new Vector3f(planet.sun().x(), planet.sun().y(), planet.sun().z()).normalize();

// ПЕРЕВОДИМО В VIEW SPACE
// Використовуємо ту саму rotationOnlyView, що і для нормалі
        Vector4f sunDirView = new Vector4f(sunDirWorld, 0.0f).mul(rotationOnlyView);
        setUniformVec3(shader, "uSunDirView", sunDirView.x, sunDirView.y, sunDirView.z);

        // Рендер квада на весь екран
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buf = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        buf.addVertex(-1.0f, -1.0f, 1.0f);
        buf.addVertex( 1.0f, -1.0f, 1.0f);
        buf.addVertex( 1.0f,  1.0f, 1.0f);
        buf.addVertex(-1.0f,  1.0f, 1.0f);

        shader.bind();
        TextureManager tm = Minecraft.getInstance().getTextureManager();
        GlStateManager._activeTexture(GL13.GL_TEXTURE0);
        GlStateManager._bindTexture(tm.getTexture(ResourceLocation.parse(planet.ring().texture())).getId());

        BufferUploader.drawWithShader(buf.buildOrThrow());
        ShaderProgram.unbind();
        RenderSystem.depthMask(true);
    }
}