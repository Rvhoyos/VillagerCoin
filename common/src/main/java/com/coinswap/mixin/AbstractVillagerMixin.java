package com.coinswap.mixin;

import com.coinswap.VillagerCoinConfig;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Mixin for AbstractVillager to handle currency replacement.
 * This covers both {@link net.minecraft.world.entity.npc.Villager} and
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
    private void onAddAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
        // If we have an in-memory currency, save it. Otherwise save global.
        String currencyToSave = (this.villagerCoin_currentCurrency != null)
                ? this.villagerCoin_currentCurrency
                : VillagerCoinConfig.get().currencyItem;
        tag.putString(CURRENCY_TAG, currencyToSave);
    }

    /**
     * Injects into the load logic to check for currency mismatches.
     * This constitutes the "Smart Persistence" layer: migrating trades immediately
     * on load.
     */
    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void onReadAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
        if (this.offers == null)
            return;

        VillagerCoinConfig.forceReload(); // Ensure fresh config on load
        VillagerCoinConfig config = VillagerCoinConfig.get();

        Item legacyItemFromNbt;

        if (tag.contains(CURRENCY_TAG)) {
            String savedCurrency = tag.getString(CURRENCY_TAG);
            this.villagerCoin_currentCurrency = savedCurrency;
            Item resolved = BuiltInRegistries.ITEM.get(new ResourceLocation(savedCurrency));
            legacyItemFromNbt = (resolved != Items.AIR) ? resolved : Items.EMERALD;
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
            Item resolved = BuiltInRegistries.ITEM.get(new ResourceLocation(this.villagerCoin_currentCurrency));
            Item legacyItem = (resolved != Items.AIR) ? resolved : Items.EMERALD;

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
    private void onAddOffersFromItemListings(MerchantOffers offers,
            VillagerTrades.ItemListing[] listings, int slots, CallbackInfo ci) {
        processOffers(offers, Items.EMERALD);
        // Ensure we track that this villager is now up to date
        this.villagerCoin_currentCurrency = VillagerCoinConfig.get().currencyItem;
    }

    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    /**
     * Iterates through a villager's offers and replaces any legacy/emerald items
     * with the configured currency. Replaces cost A, cost B, and result slots.
     * Filters out self-referential trades (cost == result) as an exploit guard.
     *
     * @param offers    the villager's current trade offers
     * @param legacyItem the item to replace (in addition to {@link Items#EMERALD})
     */
    private void processOffers(MerchantOffers offers, Item legacyItem) {
        if (offers == null || offers.isEmpty())
            return;

        VillagerCoinConfig config = VillagerCoinConfig.get();
        Item currency = config.getCurrencyItemInstance();

        boolean anyChanged = false;
        List<MerchantOffer> newOffers = new ArrayList<>();

        LOGGER.info("VillagerCoin: Processing {} offers. LegacyItem: {}, Currency: {}", offers.size(), legacyItem,
                currency);

        for (MerchantOffer offer : offers) {
            ItemStack costA = offer.getBaseCostA();
            ItemStack costB = offer.getCostB();
            ItemStack result = offer.getResult();

            boolean changed = false;

            // Check Cost A
            if (costA.getItem() == Items.EMERALD || costA.getItem() == legacyItem) {
                costA = new ItemStack(currency, costA.getCount());
                changed = true;
            }

            // Check Cost B
            if (!costB.isEmpty()) {
                if (costB.getItem() == Items.EMERALD || costB.getItem() == legacyItem) {
                    costB = new ItemStack(currency, costB.getCount());
                    changed = true;
                }
            }

            // Check Result
            Item resultItem = result.getItem();

            if (resultItem == Items.EMERALD || resultItem == legacyItem) {
                result = new ItemStack(currency, result.getCount());
                changed = true;
            } else if (resultItem.toString().contains("emerald")) {
                LOGGER.warn(
                        "VillagerCoin: Found suspicious item {} that looks like Emerald but did not match Items.EMERALD via == logic. LegacyItem: {}",
                        resultItem, legacyItem);
            }

            if (changed) {
                anyChanged = true;
                if (costA.getItem() == result.getItem()) {
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
