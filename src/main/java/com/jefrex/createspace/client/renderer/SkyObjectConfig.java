package com.jefrex.createspace.client.renderer;

public record SkyObjectConfig(
        String id,
        String texture,
        String cloudTexture,
        float cloudSpeed,
        float rotationSpeed,
        boolean isStar,
        boolean ground,        // НОВЕ
        float groundScale,     // НОВЕ
        float yaw,
        float pitch,
        float angularSize,
        float yawSpeed,
        float pitchSpeed,
        String fade,
        PlanetConfig.SunConfig sun,
        PlanetConfig.RingConfig ring,
        PlanetConfig.AtmosphereConfig atmosphere
) {
    public static SkyObjectConfig withDefaults(SkyObjectConfig c) {
        return new SkyObjectConfig(
                c.id(),
                c.texture() != null ? c.texture()
                        : "create_space:textures/planet/earth.png",
                c.cloudTexture(),
                c.cloudSpeed() > 0 ? c.cloudSpeed() : 0.0f,
                c.rotationSpeed() > 0 ? c.rotationSpeed() : 0.000f,
                c.isStar(),
                c.ground(),                                      // НОВЕ
                c.groundScale() > 0 ? c.groundScale() : 6.0f,  // НОВЕ
                c.yaw(),
                c.pitch(),
                c.angularSize() > 0 ? c.angularSize() : 0.01f,
                c.yawSpeed(),
                c.pitchSpeed(),
                c.fade(),
                c.sun() != null ? c.sun()
                        : new PlanetConfig.SunConfig(1.0f, 0.5f, 0.3f),
                c.ring(),
                c.atmosphere() != null ? c.atmosphere()
                        : PlanetConfig.AtmosphereConfig.none()
        );
    }
}