package com.coinswap.neoforge;

import net.neoforged.fml.common.Mod;

import com.coinswap.VillagerCoin;

@Mod(VillagerCoin.MOD_ID)
public final class VillagerCoinNeoForge {
    public VillagerCoinNeoForge() {
        // Run our common setup.
        VillagerCoin.init();
    }
}
