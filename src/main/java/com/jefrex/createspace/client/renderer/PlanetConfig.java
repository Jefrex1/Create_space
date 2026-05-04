package com.jefrex.createspace.client.renderer;

public record PlanetConfig(
        String id,
        double x,
        double y,
        double z,
        float radius,
        String texture,
        String cloudTexture,
        float cloudSpeed,
        float rotationSpeed,
        SunConfig sun
) {
    public record SunConfig(float x, float y, float z) {}

    public static PlanetConfig withDefaults(PlanetConfig c) {
        return new PlanetConfig(
                c.id(),
                c.x(), c.y(), c.z(),
                c.radius() > 0 ? c.radius() : 50.0f,
                c.texture() != null ? c.texture() : "create_space:textures/planet/earth.png",
                c.cloudTexture(),
                c.cloudSpeed() > 0 ? c.cloudSpeed() : 0.06f,
                c.rotationSpeed() > 0 ? c.rotationSpeed() : 0.00025f,
                c.sun() != null ? c.sun() : new SunConfig(1.0f, 0.5f, 0.3f)
        );
    }
}