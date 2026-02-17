package com.coinswap;

import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;

public final class VillagerCoin {
    public static final String MOD_ID = "villager_coin";

    public static void init() {
        VillagerCoinConfig.get();

        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) -> {
            dispatcher.register(Commands.literal("villagercoin")
                    .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .then(Commands.literal("reload")
                            .executes(context -> {
                                VillagerCoinConfig.forceReload();
                                context.getSource()
                                        .sendSuccess(() -> Component.literal("VillagerCoin config reloaded!"), true);
                                return 1;
                            }))
                    .then(Commands.literal("info")
                            .executes(context -> {
                                String id = VillagerCoinConfig.get().currencyItem;
                                context.getSource().sendSuccess(
                                        () -> Component.literal("Current VillagerCoin currency: " + id), true);
                                return 1;
                            }))
                    .then(Commands.literal("set")
                            .then(Commands.argument("item", ItemArgument.item(registry))
                                    .executes(context -> {
                                        ItemInput itemInput = ItemArgument.getItem(context, "item");
                                        Item item = itemInput.getItem();
                                        String id = BuiltInRegistries.ITEM.getKey(item).toString();
                                        VillagerCoinConfig config = VillagerCoinConfig.get();
                                        config.currencyItem = id;
                                        config.save();
                                        VillagerCoinConfig.forceReload();
                                        context.getSource().sendSuccess(
                                                () -> Component.literal("VillagerCoin currency set to: " + id), true);
                                        return 1;
                                    }))));
        });
    }
}
