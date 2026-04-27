package com.coinswap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.resources.ResourceLocation;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * JSON-backed configuration for the Villager Coin mod.
 * Stored at {@code config/villager_coin.json}. Singleton with lazy loading
 * and {@link #forceReload()} for live config changes without restart.
 */
public class VillagerCoinConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File("config/villager_coin.json");

    public String _comment = "The mod will replace Emeralds AND the 'legacyCurrencyItem' with 'currencyItem'.";
    public String currencyItem = "minecraft:emerald";
    public String legacyCurrencyItem = "minecraft:emerald";

    private static VillagerCoinConfig instance;

    public static VillagerCoinConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public static void forceReload() {
        instance = load();
    }

    public static VillagerCoinConfig load() {
        if (!CONFIG_FILE.exists()) {
            VillagerCoinConfig config = new VillagerCoinConfig();
            config.save();
            return config;
        }
        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            VillagerCoinConfig config = GSON.fromJson(reader, VillagerCoinConfig.class);
            return config != null ? config : new VillagerCoinConfig();
        } catch (IOException e) {
            return new VillagerCoinConfig();
        }
    }

    public void save() {
        File folder = CONFIG_FILE.getParentFile();
        if (folder != null && !folder.exists()) {
            folder.mkdirs();
        }
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(this, writer);
        } catch (IOException ignored) {
        }
    }

    /** Resolves {@link #currencyItem} to an {@link Item} instance, falling back to emerald. */
    public Item getCurrencyItemInstance() {
        return resolveItem(currencyItem, Items.EMERALD);
    }

    /** Resolves {@link #legacyCurrencyItem} to an {@link Item} instance, falling back to emerald. */
    public Item getLegacyItemInstance() {
        return resolveItem(legacyCurrencyItem, Items.EMERALD);
    }

    /** Looks up an item by registry ID string, returning {@code fallback} if not found. */
    private Item resolveItem(String id, Item fallback) {
        try {
            Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(id));
            if (item != Items.AIR)
                return item;
        } catch (Exception ignored) {
        }
        return fallback;
    }
}
