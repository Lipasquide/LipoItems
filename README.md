# LipoItems

Advanced custom items plugin for Minecraft 1.21+ with Item Model support.

## Features

- **Modern Item Model System** (1.21.4+) - Uses the new `item-model` component
- **Legacy Custom Model Data Support** - Backwards compatible with older versions
- **Advanced Behaviors**:
  - Trident with Loyalty, Riptide, Channeling
  - Armor with trim support
  - Food with custom nutrition and effects
  - Block placer
- **Comprehensive Recipe System**:
  - Shaped & Shapeless crafting
  - Furnace, Blast Furnace, Smoker, Campfire
  - Smithing Table (upgrade & trim)
  - Stonecutter
- **Network Synchronization** - ProtocolLib integration for client-server sync
- **Public API** - Easy integration with other plugins

## Requirements

- Paper/Spigot 1.21+
- Java 21+
- ProtocolLib (optional, for network features)

## Installation

1. Download the latest release
2. Place `LipoItems.jar` in your `plugins/` folder
3. Restart your server
4. Configure items in `plugins/LipoItems/items/`
5. Configure recipes in `plugins/LipoItems/recipes/`

## Usage

### Creating Custom Items

Create a YAML file in `plugins/LipoItems/items/`:

```yaml
# items/my_items.yml
ruby_sword:
  id: lipoitems:ruby_sword
  material: minecraft:diamond_sword
  custom-model-data: 1001
  item-model: lipoitems:ruby_sword  # 1.21.4+
  display:
    name: "<gradient:red:gold>Ruby Sword"
    lore:
      - "<gray>A sword made of pure ruby"
      - "<dark_gray>Very rare and powerful"
  settings:
    max-stack-size: 1
    durability: 1561
    attack-damage: 8
    tags: [swords, gem_items]

storm_trident:
  id: lipoitems:storm_trident
  material: minecraft:trident
  custom-model-data: 2001
  display:
    name: "<blue>Storm Trident"
  behavior:
    trident:
      loyalty: true
      channeling: true
      damage: 12.0
      return-speed: 0.15
```

### Creating Recipes

Create a YAML file in `plugins/LipoItems/recipes/`:

```yaml
# recipes/my_recipes.yml
shaped:
  ruby_sword:
    shape:
      - " R "
      - " R "
      - " S "
    ingredients:
      R: lipoitems:ruby
      S: minecraft:stick
    result: lipoitems:ruby_sword

furnace:
  ruby:
    input: minecraft:redstone_block
    result: lipoitems:ruby
    experience: 1.0
    cooking_time: 200
```

### Commands

- `/lipoitems` - Show plugin info
- `/lipoitems give <player> <item> [count]` - Give custom item
- `/lipoitems list` - List all custom items
- `/lipoitems reload` - Reload configuration
- `/givecustomitem <player> <item> [count]` - Alias for give

### Permissions

- `lipoitems.admin` - Full admin access
- `lipoitems.give` - Permission to give items

## API Usage

```java
// Get the API
LipoItemsAPI api = LipoItemsPlugin.getInstance().getApi();

// Get a custom item
CustomItem item = api.getItem("lipoitems:ruby_sword");

// Build an ItemStack
ItemStack stack = api.buildItemStack("lipoitems:ruby_sword", player, 1);

// Register custom item from your plugin
api.registerItem(myPlugin, customItem);
```

## Resource Pack

The plugin auto-generates resource pack files in `plugins/LipoItems/pack/`.

To use:
1. Copy the generated pack files to your resource pack
2. Add your custom textures to `assets/lipoitems/textures/item/`
3. Distribute the resource pack to players

## Building from Source

```bash
git clone https://github.com/yourusername/LipoItems.git
cd LipoItems
mvn clean package
```

## License

MIT License - See LICENSE file for details.

## Credits

Created by lipasquide
