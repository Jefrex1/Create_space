package com.jefrex.createspace.client.renderer;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.*;

public class SkyRegistry extends SimplePreparableReloadListener<Map<String, List<SkyObjectConfig>>> {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // "minecraft:overworld" -> [mars, venus, ...]
    private static Map<String, List<SkyObjectConfig>> skyObjects = new HashMap<>();

    public static List<SkyObjectConfig> getForDimension(String dimensionId) {
        return skyObjects.getOrDefault(dimensionId, List.of());
    }

    @Override
    protected Map<String, List<SkyObjectConfig>> prepare(
            ResourceManager manager, ProfilerFiller profiler) {

        Map<String, List<SkyObjectConfig>> result = new HashMap<>();

        // Шукаємо всі файли у create_space/sky/**/*.json
        var resources = manager.listResources(
                "sky",
                loc -> loc.getNamespace().equals("create_space")
                        && loc.getPath().endsWith(".json")
        );

        LOGGER.info("[CreateSpace] Sky scan found {} files", resources.size());
        for (var entry : resources.entrySet()) {
            LOGGER.info("[CreateSpace] Sky file: {}", entry.getKey());
        }

        for (var entry : resources.entrySet()) {
            ResourceLocation loc = entry.getKey();

            // loc.getPath() = "sky/minecraft/overworld.json"
            // -> dimensionId = "minecraft:overworld"
            String dimId = pathToDimensionId(loc.getPath());
            if (dimId == null) continue;

            try (var reader = new InputStreamReader(entry.getValue().open())) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                JsonArray arr = root.getAsJsonArray("sky_objects");

                Type listType = new TypeToken<List<SkyObjectConfig>>(){}.getType();
                List<SkyObjectConfig> objects = GSON.fromJson(arr, listType);

                List<SkyObjectConfig> withDefaults = objects.stream()
                        .map(SkyObjectConfig::withDefaults)
                        .toList();

                result.merge(dimId, withDefaults, (a, b) -> {
                    var merged = new ArrayList<>(a);
                    merged.addAll(b);
                    return merged;
                });

                LOGGER.info("[CreateSpace] Loaded {} sky object(s) for '{}'",
                        withDefaults.size(), dimId);

            } catch (Exception e) {
                LOGGER.error("[CreateSpace] Failed to load sky file: {}", loc, e);
            }
        }

        return result;
    }

    @Override
    protected void apply(Map<String, List<SkyObjectConfig>> data,
                         ResourceManager manager, ProfilerFiller profiler) {
        skyObjects = data;
    }

    /**
     * "sky/minecraft/overworld.json" -> "minecraft:overworld"
     * "sky/create_space/moon.json"   -> "create_space:moon"
     */
    private static String pathToDimensionId(String path) {
        // path зазвичай виглядає як "sky/minecraft/overworld.json"
        // нам треба "minecraft:overworld"
        String[] parts = path.replace(".json", "").split("/");
        if (parts.length >= 3) {
            return parts[1] + ":" + parts[2];
        }
        return null;
    }
}