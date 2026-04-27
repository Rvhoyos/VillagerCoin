# Villager Coin (v1.1.0)

Set any item as the currency for Villager trading!
Want villagers to trade **Diamonds**? **Gold Ingots**? **Cookies**? You can do that.

**Supports:** Minecraft 1.20.1 (Fabric + Forge) | Java 17

**Features:**
- **Server-Side Only:** Clients do not need the mod installed to play! Just drop it on the server.
- **Dynamic Updates:** Change currency instantly without restarting.
- **Smart Persistence:** Villager trades auto-migrate when the currency changes, even across server restarts.
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

## Building

```bash
# Build all platforms
./gradlew build

# Build specific platform
./gradlew :fabric:build
./gradlew :forge:build
```

Output jars are in `fabric/build/libs/` and `forge/build/libs/`.
