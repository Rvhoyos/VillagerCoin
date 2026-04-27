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
 * Main mod class for Villager Coin. Handles initialization and command registration.
 * Platform-specific entrypoints (Fabric/Forge) delegate to this class.
 */
public final class VillagerCoin {
        public static final String MOD_ID = "villager_coin";

        /** Initializes the mod by eagerly loading the config. */
        public static void init() {
                VillagerCoinConfig.get();
        }

        /**
         * Registers the {@code /villagercoin} command tree (set, reload, info).
         * Requires gamemaster (OP level 2) permissions.
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
