package com.coinswap.forge;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;
import com.coinswap.VillagerCoin;

/** Forge entrypoint. Initializes the mod and registers commands via Forge event bus. */
@Mod(VillagerCoin.MOD_ID)
public final class VillagerCoinForge {
    public VillagerCoinForge() {
        // Run our common setup.
        VillagerCoin.init();

        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        VillagerCoin.registerCommands(event.getDispatcher(), event.getBuildContext());
    }
}
