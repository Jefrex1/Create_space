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
        RingConfig ring,
        AtmosphereConfig atmosphere  // НОВЕ
) {
    public record SunConfig(float x, float y, float z) {}

    public record RingConfig(
            String texture,
            float innerRadius,
            float outerRadius,
            float tilt,
            float opacity
    ) {}

    public record AtmosphereConfig(
            float rayleighR,       // червоний канал розсіювання (земля: 5.8e-3)
            float rayleighG,       // зелений канал (земля: 13.5e-3)
            float rayleighB,       // синій канал (земля: 33.1e-3)
            float mie,             // розсіювання Мі (туман, пил)
            float strength,        // загальна сила атмосфери
            float radius           // множник радіусу атмосфери (1.06 = тонка, 6.0 = зірка)
    ) {
        public static AtmosphereConfig defaultEarth() {
            return new AtmosphereConfig(5.8e-3f, 13.5e-3f, 33.1e-3f, 21e-3f, 5.0f, 1.06f);
        }
        public static AtmosphereConfig none() {
            return new AtmosphereConfig(0f, 0f, 0f, 0f, 0f, 1.001f);
        }
    }

    public static PlanetConfig withDefaults(PlanetConfig c) {
        AtmosphereConfig atm = c.atmosphere() != null ? c.atmosphere() : AtmosphereConfig.none();

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
                c.ring(),
                atm
        );
    }
}