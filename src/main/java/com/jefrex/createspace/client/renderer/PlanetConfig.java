package com.jefrex.createspace.client.renderer;

public record PlanetConfig(
        String id,
        double x, double y, double z,
        float radius,
        String texture,
        String cloudTexture,
        float cloudSpeed,
        float rotationSpeed,
        float atmosphereStrength,
        boolean isStar,
        SunConfig sun,
        String dimension,
        RingConfig ring  // НОВЕ
) {
    public record SunConfig(float x, float y, float z) {}

    public record RingConfig(
            String texture,   // текстура кілець
            float innerRadius, // внутрішній радіус (множник від radius планети)
            float outerRadius, // зовнішній радіус
            float tilt,        // нахил в градусах
            float opacity      // прозорість 0.0-1.0
    ) {}

    public static PlanetConfig withDefaults(PlanetConfig c) {
        return new PlanetConfig(
                c.id(),
                c.x(), c.y(), c.z(),
                c.radius() > 0 ? c.radius() : 50.0f,
                c.texture() != null ? c.texture() : "create_space:textures/planet/earth.png",
                c.cloudTexture(),
                c.cloudSpeed() > 0 ? c.cloudSpeed() : 0.06f,
                c.rotationSpeed() > 0 ? c.rotationSpeed() : 0.00025f,
                c.atmosphereStrength() >= 0 ? c.atmosphereStrength() : 1.0f,
                c.isStar(),
                c.sun() != null ? c.sun() : new SunConfig(1.0f, 0.5f, 0.3f),
                c.dimension(),
                c.ring()  // може бути null якщо немає кілець
        );
    }
}