# Villager Coin (v1.3.0)

Set any item as the currency for Villager trading!
Want villagers to trade **Diamonds**? **Gold Ingots**? **Cookies**? You can do that.

**Version 1.3.0 Update:**
- Added support for Minecraft 26.1.2 (no feature changes)


**Features:**
- **Server-Side Compatible:** Clients do not need the mod installed to play! Just drop it on the server.
- **Dynamic Updates:** Change currency instantly without restarting.
- **Optimized:** Zero performance impact when not trading.

## How to change currency
You can change the currency in-game or via config.

**In-Game:**
Run the command:
`/villagercoin set <item_id>`
Example: `/villagercoin set minecraft:diamond`

**Config File:**
Edit `config/villager_coin.json` and change `currencyItem`.

## Commands
- `/villagercoin set <item>` - Sets the currency.
- `/villagercoin reload` - Reloads the config (updates villagers instantly).
- `/villagercoin info` - Checks current currency.
