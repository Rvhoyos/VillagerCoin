package com.coinswap;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Common entrypoint for the Villager Coin mod, called by the Fabric and
 * NeoForge loader entrypoints. Holds the mod id, initialization, and the
 * {@code /villagercoin} command tree.
 */
public final class VillagerCoin {
        public static final String MOD_ID = "villager_coin";

        /**
         * Initializes the mod by loading (and creating if absent) the config.
         * Trade replacement itself is driven entirely by
         * {@link com.coinswap.mixin.AbstractVillagerMixin}.
         */
        public static void init() {
                VillagerCoinConfig.get();
        }

        /**
         * Registers the {@code /villagercoin} command (requires
         * {@link Commands#LEVEL_GAMEMASTERS}):
         * {@code reload} re-reads the config from disk, {@code info} prints the
         * active currency id, and {@code set <item>} persists a new currency and
         * reloads so open worlds pick it up live.
         */
        public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher,
                        CommandBuildContext registry) {
                dispatcher.register(Commands.literal("villagercoin")
                                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .then(Commands.literal("reload")
                                                .executes(context -> {
                                                        VillagerCoinConfig.forceReload();
                                                        context.getSource()
                                                                        .sendSuccess(() -> Component.literal(
                                                                                        "VillagerCoin config reloaded!"),
                                                                                        true);
                                                        return 1;
                                                }))
                                .then(Commands.literal("info")
                                                .executes(context -> {
                                                        String id = VillagerCoinConfig.get().currencyItem;
                                                        context.getSource().sendSuccess(
                                                                        () -> Component.literal(
                                                                                        "Current VillagerCoin currency: "
                                                                                                        + id),
                                                                        true);
                                                        return 1;
                                                }))
                                .then(Commands.literal("set")
                                                .then(Commands.argument("item", ItemArgument.item(registry))
                                                                .executes(context -> {
                                                                        ItemInput itemInput = ItemArgument
                                                                                        .getItem(context, "item");
                                                                        Item item = itemInput.getItem();
                                                                        String id = BuiltInRegistries.ITEM.getKey(item)
                                                                                        .toString();
                                                                        VillagerCoinConfig config = VillagerCoinConfig
                                                                                        .get();
                                                                        config.currencyItem = id;
                                                                        config.save();
                                                                        VillagerCoinConfig.forceReload();
                                                                        context.getSource().sendSuccess(
                                                                                        () -> Component.literal(
                                                                                                        "VillagerCoin currency set to: "
                                                                                                                        + id),
                                                                                        true);
                                                                        return 1;
                                                                }))));
        }
}
