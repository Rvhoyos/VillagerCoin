package com.coinswap.mixin;

import com.coinswap.VillagerCoinConfig;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Mixin for AbstractVillager to handle currency replacement.
 * This covers both {@link net.minecraft.world.entity.npc.villager.Villager} and
 * {@link net.minecraft.world.entity.npc.WanderingTrader} as they both extend
 * AbstractVillager.
 */
@Mixin(AbstractVillager.class)
public abstract class AbstractVillagerMixin {

    @Shadow
    protected MerchantOffers offers;

    private static final String CURRENCY_TAG = "VillagerCoin_Currency";

    // In-memory tracker of what currency this villager THINKS it has
    @Unique
    private String villagerCoin_currentCurrency;

    /**
     * Injects into the save logic to store the current currency version.
     * This ensures that when the villager is loaded later, we know what currency
     * it was using, allowing for auto-migration if the config has changed.
     */
    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void onAddAdditionalSaveData(ValueOutput output, CallbackInfo ci) {
        // If we have an in-memory currency, save it. Otherwise save global.
        String currencyToSave = (this.villagerCoin_currentCurrency != null)
                ? this.villagerCoin_currentCurrency
                : VillagerCoinConfig.get().currencyItem;
        output.putString(CURRENCY_TAG, currencyToSave);
    }

    /**
     * Injects into the load logic to check for currency mismatches.
     * This constitutes the "Smart Persistence" layer: migrating trades immediately
     * on load.
     */
    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void onReadAdditionalSaveData(ValueInput input, CallbackInfo ci) {
        if (this.offers == null)
            return;

        VillagerCoinConfig.forceReload(); // Ensure fresh config on load
        VillagerCoinConfig config = VillagerCoinConfig.get();

        Optional<String> savedCurrency = input.getString(CURRENCY_TAG);
        Item legacyItemFromNbt = null;

        if (savedCurrency.isPresent()) {
            this.villagerCoin_currentCurrency = savedCurrency.get();
            legacyItemFromNbt = BuiltInRegistries.ITEM.get(Identifier.parse(savedCurrency.get()))
                    .map(Holder.Reference::value).orElse(Items.EMERALD);
        } else {
            // Fallback for pre-mod villagers: Use the configured legacy item
            this.villagerCoin_currentCurrency = BuiltInRegistries.ITEM.getKey(config.getLegacyItemInstance())
                    .toString();
            legacyItemFromNbt = config.getLegacyItemInstance();
        }

        processOffers(this.offers, legacyItemFromNbt);

        // Update our internal state to the new currency
        this.villagerCoin_currentCurrency = config.currencyItem;
    }

    /**
     * Interactive Update / Live Refresh.
     * When the player opens the trading GUI (calls getOffers), check if the
     * currency matches the config.
     * If not, update immediately. This ensures "Live Updates" without needing a
     * server restart.
     */
    @Inject(method = "getOffers", at = @At("RETURN"))
    private void onGetOffers(CallbackInfoReturnable<MerchantOffers> cir) {
        if (this.offers == null)
            return;

        VillagerCoinConfig config = VillagerCoinConfig.get();

        if (this.villagerCoin_currentCurrency == null) {
            // Fallback for untagged villagers during interaction: Assume Legacy Config
            this.villagerCoin_currentCurrency = BuiltInRegistries.ITEM.getKey(config.getLegacyItemInstance())
                    .toString();
        }

        if (!this.villagerCoin_currentCurrency.equals(config.currencyItem)) {
            Item legacyItem = BuiltInRegistries.ITEM.get(Identifier.parse(this.villagerCoin_currentCurrency))
                    .map(Holder.Reference::value).orElse(Items.EMERALD);

            processOffers(this.offers, legacyItem);

            // Critical: Update the tag so we don't process again unnecessarily
            this.villagerCoin_currentCurrency = config.currencyItem;
        }
    }

    /**
     * Handles new trade generation (leveling up, spawning).
     * This intercepts the new trades before they are added and converts them to the
     * correct currency.
     */
    @Inject(method = "addOffersFromItemListings", at = @At("RETURN"))
    private void onAddOffersFromItemListings(ServerLevel level, MerchantOffers offers,
            VillagerTrades.ItemListing[] listings, int slots, CallbackInfo ci) {
        processOffers(offers, Items.EMERALD);
        // Ensure we track that this villager is now up to date
        this.villagerCoin_currentCurrency = VillagerCoinConfig.get().currencyItem;
    }

    /**
     * Core logic to iterate through offers and replace the currency item.
     * Replaces both the Cost (buy item) and the Result (sell item) if they match
     * the legacy/emerald item.
     * Use {@code Items.EMERALD} as legacyItem to strictly replace Emeralds.
     */
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    /**
     * Core logic to iterate through offers and replace the currency item.
     * Replaces both the Cost (buy item) and the Result (sell item) if they match
     * the legacy/emerald item.
     * Use {@code Items.EMERALD} as legacyItem to strictly replace Emeralds.
     */
    private void processOffers(MerchantOffers offers, Item legacyItem) {
        if (offers == null || offers.isEmpty())
            return;

        VillagerCoinConfig config = VillagerCoinConfig.get();
        Item currency = config.getCurrencyItemInstance();

        boolean anyChanged = false;
        List<MerchantOffer> newOffers = new ArrayList<>();

        // We can't access `this.level()` easily here because it's a mixin to
        // `AbstractVillager` and we are in `processOffers` which is static-ish but not
        // static?
        // Wait, processOffers is an instance method (it calls non-static stuff?).
        // No, processOffers is private void. It has access to `this`.

        LOGGER.info("VillagerCoin: Processing {} offers. LegacyItem: {}, Currency: {}", offers.size(), legacyItem,
                currency);

        for (MerchantOffer offer : offers) {
            ItemCost costA = offer.getItemCostA();
            Optional<ItemCost> costB = offer.getItemCostB();
            ItemStack result = offer.getResult();

            boolean changed = false;

            // Check Cost A
            Item itemA = costA.item().value();
            if (itemA == Items.EMERALD || itemA == legacyItem) {
                costA = new ItemCost(currency, costA.count());
                changed = true;
            }

            // Check Cost B
            if (costB.isPresent()) {
                Item itemB = costB.get().item().value();
                if (itemB == Items.EMERALD || itemB == legacyItem) {
                    costB = Optional.of(new ItemCost(currency, costB.get().count()));
                    changed = true;
                }
            }

            // Check Result
            Item resultItem = result.getItem();

            if (resultItem == Items.EMERALD || resultItem == legacyItem) {
                // FIXED: Do NOT apply components from old emeralds to new currency items.
                // This caused client sync issues where the client saw the old item despite the
                // server update.
                ItemStack newResult = new ItemStack(currency, result.getCount());

                result = newResult;
                changed = true;
            } else if (resultItem.toString().contains("emerald")) {
                LOGGER.warn(
                        "VillagerCoin: Found suspicious item {} that looks like Emerald but did not match Items.EMERALD via == logic. LegacyItem: {}",
                        resultItem, legacyItem);
            }

            if (changed) {
                anyChanged = true;
                if (costA.item().value() == result.getItem()) {
                    LOGGER.warn("VillagerCoin: Prevented exploited trace (CostA == Result). Removed from list.");
                    continue;
                }

                MerchantOffer newOffer = new MerchantOffer(
                        costA,
                        costB,
                        result,
                        offer.getUses(),
                        offer.getMaxUses(),
                        offer.getXp(),
                        offer.getPriceMultiplier(),
                        offer.getDemand());
                newOffer.setSpecialPriceDiff(offer.getSpecialPriceDiff());
                newOffers.add(newOffer);
            } else {
                newOffers.add(offer);
            }
        }

        if (anyChanged) {
            offers.clear();
            offers.addAll(newOffers);
            LOGGER.info("VillagerCoin: Successfully updated offers.");
        }
    }
}
