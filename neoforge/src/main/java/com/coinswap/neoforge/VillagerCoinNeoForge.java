package com.coinswap.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import com.coinswap.VillagerCoin;

@Mod(VillagerCoin.MOD_ID)
public final class VillagerCoinNeoForge {
    public VillagerCoinNeoForge(IEventBus modEventBus) {
        // Run our common setup.
        VillagerCoin.init();

        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        VillagerCoin.registerCommands(event.getDispatcher(), event.getBuildContext());
    }
}
