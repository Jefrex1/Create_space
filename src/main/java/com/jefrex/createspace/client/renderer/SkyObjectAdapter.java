package com.jefrex.createspace.client.renderer;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public class SkyObjectAdapter {

    private static final float SKY_DISTANCE = 10000.0f;

    public static void render(
            com.mojang.blaze3d.vertex.PoseStack poseStack,
            org.joml.Matrix4f projectionMatrix,
            Camera camera,
            List<SkyObjectConfig> objects
    ) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        // Використовуємо getSunAngle() — той самий кут що і у ванільного сонця
        float sunAngleRad = mc.level.getSunAngle(1.0f);
        float baseAngle = (float) Math.toDegrees(sunAngleRad) + 90.0f;

        var camPos = camera.getPosition();
        float camY = (float) camPos.y;

        // --- Скайбокс космосу ---
        String dimId = mc.level.dimension().location().toString();
        boolean isMoon = dimId.equals("create_space:moon");

        float skyboxAlpha;
        if (isMoon) {
            // На місяці — завжди скайбокс космосу
            skyboxAlpha = 1.0f;
        } else {
            // В overworld — плавно з'являємось від Y=500 до Y=700
            skyboxAlpha = (camY - 500.0f) / 200.0f;
            skyboxAlpha = Math.max(0.0f, Math.min(1.0f, skyboxAlpha));
        }

        if (skyboxAlpha > 0.0f) {
            PlanetRenderer.renderSkybox(projectionMatrix, camera, skyboxAlpha);
        }
        // --- Кінець скайбоксу ---

        List<PlanetConfig> planets = new ArrayList<>();
        for (SkyObjectConfig obj : objects) {
            float alpha = getSkyAlpha(obj, camY, mc);
            if (alpha <= 0.0f) continue;
            planets.add(convertToPlanetConfig(obj, objects, camPos.x, camPos.y, camPos.z, camY, alpha, baseAngle));
        }

        PlanetRenderer.renderPlanets(poseStack, projectionMatrix, camera, planets);
    }

    private static float getSkyAlpha(SkyObjectConfig obj, float camY, Minecraft mc) {
        if (mc.level == null) return 0.0f;

        float heightAlpha = 1.0f;
        if (obj.pitch() <= -45.0f) {
            float minH = 500.0f;
            float maxH = 600.0f;
            heightAlpha = (camY - minH) / (maxH - minH);
            heightAlpha = Math.max(0.0f, Math.min(1.0f, heightAlpha));
            if (heightAlpha <= 0.0f) return 0.0f;
        }

        String fade = obj.fade();
        float fadeAlpha = 1.0f;
        if (fade != null) {
            float stars = mc.level.getStarBrightness(1.0f);
            fadeAlpha = switch (fade) {
                case "night" -> stars;
                case "day"   -> 1.0f - stars;
                default      -> 1.0f;
            };
        }

        return heightAlpha * fadeAlpha;
    }

    private static float[] rotateAroundZ(float dx, float dy, float dz, float angleDeg) {
        float rad = (float) Math.toRadians(angleDeg);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);
        return new float[]{ cos * dx - sin * dy, sin * dx + cos * dy, dz };
    }

    private static float[] dirFromYawPitch(float yawDeg, float pitchDeg) {
        float yaw   = (float) Math.toRadians(yawDeg);
        float pitch = (float) Math.toRadians(pitchDeg);
        float cosP  = (float) Math.cos(pitch);
        return new float[]{
                cosP * (float) Math.sin(yaw),
                (float) Math.sin(pitch),
                cosP * (float) Math.cos(yaw)
        };
    }

    private static PlanetConfig convertToPlanetConfig(
            SkyObjectConfig obj,
            List<SkyObjectConfig> allObjects,
            double camX, double camY, double camZ,
            float camYFloat,
            float alpha,
            float baseAngle
    ) {
        float[] dir = dirFromYawPitch(obj.yaw(), obj.pitch());
        float dx = dir[0], dy = dir[1], dz = dir[2];

        boolean isStatic = obj.pitch() <= -45.0f;

        if (!isStatic) {
            float[] rotated = rotateAroundZ(dx, dy, dz, baseAngle);
            dx = rotated[0];
            dy = rotated[1];
            dz = rotated[2];
        }

        if (obj.pitch() <= -45.0f) {
            float t = Math.max(0.0f, Math.min(1.0f, (camYFloat - 400.0f) / 600.0f));
            dy = dy + (0.0f - dy) * (1.0f - t);
            float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len > 0.001f) { dx /= len; dy /= len; dz /= len; }
        }

        double x = camX + dx * SKY_DISTANCE;
        double y = dy * SKY_DISTANCE;
        double z = camZ + dz * SKY_DISTANCE;

        float radius = SKY_DISTANCE * (float) Math.tan(obj.angularSize());

        PlanetConfig.SunConfig sunConfig;
        if (obj.isStar()) {
            sunConfig = new PlanetConfig.SunConfig(dx, dy, dz);
        } else {
            if (obj.sun() != null) {
                // JSON має явний sun — використовуємо статично
                sunConfig = obj.sun();
            } else {
                SkyObjectConfig star = allObjects.stream()
                        .filter(SkyObjectConfig::isStar)
                        .findFirst()
                        .orElse(null);

                if (star != null) {
                    float[] starDir = dirFromYawPitch(star.yaw(), star.pitch());
                    float[] starRot = rotateAroundZ(starDir[0], starDir[1], starDir[2], baseAngle);
                    float lx = starRot[0] - dx;
                    float ly = starRot[1] - dy;
                    float lz = starRot[2] - dz;
                    float len = (float) Math.sqrt(lx * lx + ly * ly + lz * lz);
                    sunConfig = len > 0.001f
                            ? new PlanetConfig.SunConfig(lx / len, ly / len, lz / len)
                            : new PlanetConfig.SunConfig(1f, 0.5f, 0.3f);
                } else {
                    sunConfig = new PlanetConfig.SunConfig(1f, 0.5f, 0.3f);
                }
            }
        }

        PlanetConfig.AtmosphereConfig atm = obj.atmosphere() != null
                ? obj.atmosphere()
                : PlanetConfig.AtmosphereConfig.none();

        PlanetConfig.AtmosphereConfig atmWithAlpha = new PlanetConfig.AtmosphereConfig(
                atm.rayleighR(), atm.rayleighG(), atm.rayleighB(),
                atm.mie(), atm.strength() * alpha, atm.radius()
        );

        return new PlanetConfig(
                obj.id(),
                x, y, z,
                radius,
                obj.texture(),
                obj.cloudTexture(),
                obj.cloudSpeed(),
                obj.rotationSpeed(),
                atm.strength() * alpha,
                obj.isStar(),
                sunConfig,
                null,
                obj.ring(),
                atmWithAlpha,
                alpha
        );
    }
}