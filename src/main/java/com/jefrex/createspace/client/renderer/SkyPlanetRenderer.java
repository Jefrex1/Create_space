package com.jefrex.createspace.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.joml.*;
import org.lwjgl.opengl.GL13;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SkyPlanetRenderer {

    private static final ResourceLocation SHADER = ResourceLocation.fromNamespaceAndPath("create_space", "planet");
    private static final ResourceLocation RING_SHADER = ResourceLocation.fromNamespaceAndPath("create_space", "ring");

    private static final float SKY_DISTANCE = 10000.0f;

    private static final Map<String, Float> rotations  = new HashMap<>();
    private static final Map<String, Float> yawOffsets = new HashMap<>();

    public static void render(Matrix4f projectionMatrix, Camera camera, List<SkyObjectConfig> objects) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        for (SkyObjectConfig obj : objects) {
            float alpha = getSkyAlpha(obj);
            if (alpha <= 0.0f) continue;

            renderSkyPlanet(obj, projectionMatrix, camera, alpha, objects);

            if (obj.ring() != null) {
                renderSkyRing(obj, projectionMatrix, camera, alpha, objects);
            }
        }
    }

    private static Vector3f getSunDirection(List<SkyObjectConfig> objects) {
        return objects.stream()
                .filter(SkyObjectConfig::isStar)
                .findFirst()
                .map(sun -> {
                    float yawOff = yawOffsets.getOrDefault(sun.id() + "_yaw", 0.0f);
                    float yaw    = (float) java.lang.Math.toRadians(sun.yaw() + yawOff);
                    float pitch  = (float) java.lang.Math.toRadians(sun.pitch());
                    float cosP   = (float) java.lang.Math.cos(pitch);
                    return new Vector3f(
                            cosP * (float) java.lang.Math.sin(yaw),
                            (float) java.lang.Math.sin(pitch),
                            cosP * (float) java.lang.Math.cos(yaw)
                    ).normalize();
                })
                .orElse(null);
    }

    private static float getSkyAlpha(SkyObjectConfig obj) {
        String fade = obj.fade();
        Minecraft mc = Minecraft.getInstance();
        if (fade == null || mc.level == null) return 1.0f;
        float stars = mc.level.getStarBrightness(1.0f);
        return switch (fade) {
            case "night" -> stars;
            case "day"   -> 1.0f - stars;
            default      -> 1.0f;
        };
    }

    private static Vector3f getSkyDirectionInViewSpace(SkyObjectConfig obj, Camera camera) {
        float yawOff = yawOffsets.getOrDefault(obj.id() + "_yaw", 0.0f);
        float totalYaw   = (float) java.lang.Math.toRadians(obj.yaw() + yawOff);
        float totalPitch = (float) java.lang.Math.toRadians(obj.pitch());

        float cosP = (float) java.lang.Math.cos(totalPitch);
        Vector3f worldDir = new Vector3f(
                cosP * (float) java.lang.Math.sin(totalYaw),
                (float) java.lang.Math.sin(totalPitch),
                cosP * (float) java.lang.Math.cos(totalYaw)
        );

        Vector3f look = new Vector3f(camera.getLookVector());
        Vector3f up   = new Vector3f(camera.getUpVector());
        Matrix4f view = new Matrix4f().setLookAt(
                0, 0, 0, look.x, look.y, look.z, up.x, up.y, up.z);

        Vector4f dirView = new Vector4f(worldDir, 0.0f).mul(view);
        return new Vector3f(dirView.x, dirView.y, dirView.z);
    }

    private static void renderSkyPlanet(SkyObjectConfig obj, Matrix4f projectionMatrix,
                                        Camera camera, float alpha, List<SkyObjectConfig> objects) {
        ShaderProgram shader = VeilRenderSystem.setShader(SHADER);
        if (shader == null) return;

        // Оновлення ротації і yaw
        float rotation = rotations.getOrDefault(obj.id(), 0.0f) + obj.rotationSpeed();
        rotations.put(obj.id(), rotation);
        float yawOff = yawOffsets.getOrDefault(obj.id() + "_yaw", 0.0f) + obj.yawSpeed();
        yawOffsets.put(obj.id() + "_yaw", yawOff);

        // Позиція сфери у view space
        Vector3f dirView     = getSkyDirectionInViewSpace(obj, camera);
        float    renderRadius = SKY_DISTANCE * (float) java.lang.Math.tan(obj.angularSize());
        Vector3f spherePos   = new Vector3f(dirView).mul(SKY_DISTANCE);

        // Матриці
        Vector3f look = new Vector3f(camera.getLookVector());
        Vector3f up   = new Vector3f(camera.getUpVector());
        Matrix4f view    = new Matrix4f().setLookAt(0, 0, 0, look.x, look.y, look.z, up.x, up.y, up.z);
        Matrix4f invView = new Matrix4f(view).invert();
        Matrix4f invProj = new Matrix4f(projectionMatrix).invert();
        Matrix4f planetRotation = new Matrix4f().rotateY(rotation);

        // uSpherePosWorld — центр сфери в world space
        Vector4f spherePosWorld4 = new Vector4f(spherePos.x, spherePos.y, spherePos.z, 1.0f).mul(invView);
        Vector3f spherePosWorld  = new Vector3f(spherePosWorld4.x, spherePosWorld4.y, spherePosWorld4.z);

        // Напрямок сонця — динамічно з позиції зірки, fallback на конфіг
        Vector3f sunDir = getSunDirection(objects);
        if (sunDir == null) {
            sunDir = obj.sun() != null
                    ? new Vector3f(obj.sun().x(), obj.sun().y(), obj.sun().z()).normalize()
                    : new Vector3f(0f, 1f, 0f);
        }

        PlanetConfig.AtmosphereConfig atm = obj.atmosphere();

        // Передача всіх уніформів
        setUniformInt(shader,   "PlanetTexture",       0);
        setUniformInt(shader,   "CloudTexture",        1);
        setUniformVec3(shader,  "uSpherePos",          -spherePos.x, -spherePos.y, -spherePos.z);
        setUniformVec3(shader,  "uSpherePosWorld",     spherePosWorld.x, spherePosWorld.y, spherePosWorld.z);
        setUniformFloat(shader, "uScale",              renderRadius);
        setUniformMat4(shader,  "uInvProj",            invProj);
        setUniformMat4(shader,  "uInvView",            invView);
        setUniformMat4(shader,  "uPlanetRotation",     planetRotation);
        setUniformFloat(shader, "uTime",               rotation * 0.5f);
        setUniformFloat(shader, "uPlasmaTime",         (System.currentTimeMillis() % 1000000) / 1000.0f);
        setUniformVec3(shader,  "uSunDir",             sunDir.x, sunDir.y, sunDir.z);
        setUniformFloat(shader, "uHasClouds",          obj.cloudTexture() != null ? 1f : 0f);
        setUniformFloat(shader, "uCloudSpeed",         obj.cloudSpeed());
        setUniformFloat(shader, "uIsStar",             obj.isStar() ? 1f : 0f);
        setUniformVec3(shader,  "uStarColor",          1f, 0.9f, 0.6f);
        setUniformFloat(shader, "uPlasmaSpeed",        0.3f);
        setUniformFloat(shader, "uPlasmaScale",        3.0f);
        setUniformFloat(shader, "uCoronalIntensity",   1.5f);
        setUniformFloat(shader, "uAtmosphereRadius",   renderRadius * atm.radius());
        setUniformVec3(shader,  "uRayleighCoeff",      atm.rayleighR(), atm.rayleighG(), atm.rayleighB());
        setUniformFloat(shader, "uMieCoeff",           atm.mie());
        setUniformFloat(shader, "uAtmosphereStrength", atm.strength() * alpha);

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        float zDepth = 0.5f;
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buf = tesselator.begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buf.addVertex(-1.0f, -1.0f, zDepth).setUv(0f, 0f);
        buf.addVertex( 1.0f, -1.0f, zDepth).setUv(1f, 0f);
        buf.addVertex( 1.0f,  1.0f, zDepth).setUv(1f, 1f);
        buf.addVertex(-1.0f,  1.0f, zDepth).setUv(0f, 1f);

        shader.bind();
        RenderSystem.setShaderTexture(0, ResourceLocation.parse(obj.texture()));
        if (obj.cloudTexture() != null) {
            RenderSystem.activeTexture(GL13.GL_TEXTURE1);
            RenderSystem.setShaderTexture(1, ResourceLocation.parse(obj.cloudTexture()));
            RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        }

        BufferUploader.drawWithShader(buf.buildOrThrow());
        ShaderProgram.unbind();
        RenderSystem.depthMask(true);
    }

    private static void renderSkyRing(SkyObjectConfig obj, Matrix4f projectionMatrix,
                                      Camera camera, float alpha, List<SkyObjectConfig> objects) {
        ShaderProgram shader = VeilRenderSystem.setShader(RING_SHADER);
        if (shader == null) return;

        setUniformInt(shader, "RingTexture", 0);

        Vector3f dirView     = getSkyDirectionInViewSpace(obj, camera);
        float    renderRadius = SKY_DISTANCE * (float) java.lang.Math.tan(obj.angularSize());
        Vector3f spherePos   = new Vector3f(dirView).mul(SKY_DISTANCE);

        Vector3f look = new Vector3f(camera.getLookVector());
        Vector3f up   = new Vector3f(camera.getUpVector());
        Matrix4f view = new Matrix4f().setLookAt(
                0, 0, 0, look.x, look.y, look.z, up.x, up.y, up.z);

        Vector4f ringNormal = new Vector4f(0f, 1f, 0f, 0f);
        ringNormal.rotateX((float) java.lang.Math.toRadians(obj.ring().tilt()));
        ringNormal.mul(view);

        Matrix4f invProj = new Matrix4f(projectionMatrix).invert();

        // Напрямок сонця — динамічно з позиції зірки, fallback на конфіг
        Vector3f sunDir = getSunDirection(objects);
        if (sunDir == null) {
            sunDir = obj.sun() != null
                    ? new Vector3f(obj.sun().x(), obj.sun().y(), obj.sun().z()).normalize()
                    : new Vector3f(0f, 1f, 0f);
        }
        Vector4f sunDirView = new Vector4f(sunDir, 0f).mul(view);

        setUniformVec3(shader,  "uSpherePos",      spherePos.x, spherePos.y, spherePos.z);
        setUniformVec3(shader,  "uRingNormalView", ringNormal.x, ringNormal.y, ringNormal.z);
        setUniformFloat(shader, "uScale",          renderRadius);
        setUniformMat4(shader,  "uInvProj",        invProj);
        setUniformFloat(shader, "uInnerRadius",    obj.ring().innerRadius());
        setUniformFloat(shader, "uOuterRadius",    obj.ring().outerRadius());
        setUniformFloat(shader, "uOpacity",        obj.ring().opacity() * alpha);
        setUniformVec3(shader,  "uSunDirView",     sunDirView.x, sunDirView.y, sunDirView.z);

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        float zDepth = 0.5f;
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buf = tesselator.begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buf.addVertex(-1.0f, -1.0f, zDepth).setUv(0f, 0f);
        buf.addVertex( 1.0f, -1.0f, zDepth).setUv(1f, 0f);
        buf.addVertex( 1.0f,  1.0f, zDepth).setUv(1f, 1f);
        buf.addVertex(-1.0f,  1.0f, zDepth).setUv(0f, 1f);

        shader.bind();
        RenderSystem.setShaderTexture(0, ResourceLocation.parse(obj.ring().texture()));

        BufferUploader.drawWithShader(buf.buildOrThrow());
        ShaderProgram.unbind();
        RenderSystem.depthMask(true);
    }

    private static void setUniformVec3(ShaderProgram s, String n, float x, float y, float z) {
        var u = s.getUniform(n); if (u != null) u.setVector(x, y, z);
    }
    private static void setUniformFloat(ShaderProgram s, String n, float v) {
        var u = s.getUniform(n); if (u != null) u.setFloat(v);
    }
    private static void setUniformMat4(ShaderProgram s, String n, Matrix4f m) {
        var u = s.getUniform(n); if (u != null) u.setMatrix(m);
    }
    private static void setUniformInt(ShaderProgram s, String n, int v) {
        var u = s.getUniform(n); if (u != null) u.setInt(v);
    }
}