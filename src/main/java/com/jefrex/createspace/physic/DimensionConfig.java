package com.jefrex.createspace.physic;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class DimensionConfig {

    public String dimension;
    public double vacuum_damage        = 2.5;
    public double gravity              = 0.0;
    public double bounce_factor        = 0.5;
    public double horizontal_stabilizer = 1.095;
    public double vertical_stabilizer  = 1.018;
    public double terminal_velocity    = 1.5;
    public boolean has_atmosphere_blocks = true;
    public double fall_damage_divider  = 5.0;

    // ── внутрішнє сховище ──────────────────────────────────────────────
    private static final Map<ResourceLocation, DimensionConfig> REGISTRY = new HashMap<>();
    private static final Gson GSON = new Gson();
    private static final String CONFIG_PATH = "create_space/dimensions.json";

    public static void load() {
        REGISTRY.clear();

        try (InputStream is = DimensionConfig.class
                .getResourceAsStream("/assets/create_space/dimensions.json")) {

            if (is == null) {
                System.err.println("[CreateSpace] dimensions.json не знайдено!");
                return;
            }

            try (Reader r = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                Type listType = new TypeToken<List<DimensionConfig>>(){}.getType();
                List<DimensionConfig> list = GSON.fromJson(r, listType);
                if (list != null) {
                    for (DimensionConfig cfg : list) {
                        REGISTRY.put(ResourceLocation.parse(cfg.dimension), cfg);
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("[CreateSpace] Помилка читання dimensions.json: " + e.getMessage());
        }
    }

    /** Повертає конфіг для виміру або null, якщо вимір не в списку */
    public static DimensionConfig get(ResourceLocation dimension) {
        return REGISTRY.get(dimension);
    }

    /** Чи зареєстрований цей вимір взагалі */
    public static boolean isSpaceDimension(ResourceLocation dimension) {
        return REGISTRY.containsKey(dimension);
    }

}