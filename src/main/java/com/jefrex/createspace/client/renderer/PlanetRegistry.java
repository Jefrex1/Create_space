package com.jefrex.createspace.client.renderer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class PlanetRegistry extends SimplePreparableReloadListener<List<PlanetConfig>> {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final ResourceLocation PLANETS_JSON =
            ResourceLocation.fromNamespaceAndPath("create_space", "planets.json");

    private static List<PlanetConfig> planets = new ArrayList<>();

    public static List<PlanetConfig> getPlanets() {
        return planets;
    }

    @Override
    protected List<PlanetConfig> prepare(ResourceManager manager, ProfilerFiller profiler) {
        try {
            var resource = manager.getResource(PLANETS_JSON);
            if (resource.isEmpty()) {
                LOGGER.warn("[CreateSpace] planets.json not found!");
                return List.of();
            }

            try (var reader = new InputStreamReader(resource.get().open())) {
                Type listType = new TypeToken<List<PlanetConfig>>(){}.getType();
                List<PlanetConfig> loaded = GSON.fromJson(reader, listType);
                return loaded.stream()
                        .map(PlanetConfig::withDefaults)
                        .toList();
            }
        } catch (Exception e) {
            LOGGER.error("[CreateSpace] Failed to load planets.json", e);
            return List.of();
        }
    }

    @Override
    protected void apply(List<PlanetConfig> configs, ResourceManager manager, ProfilerFiller profiler) {
        planets = configs;
        LOGGER.info("[CreateSpace] Loaded {} planet(s)", planets.size());
    }
}