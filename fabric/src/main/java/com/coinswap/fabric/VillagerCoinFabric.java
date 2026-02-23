package com.coinswap.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import com.coinswap.VillagerCoin;

public final class VillagerCoinFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // Run our common setup.
        VillagerCoin.init();

        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> {
            VillagerCoin.registerCommands(dispatcher, registry);
        });
    }
}
