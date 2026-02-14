# LipoItems

**Next-Generation Custom Items, Blocks & Armor Plugin for Minecraft 1.21.11**

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-blue.svg)](https://minecraft.net)
[![Paper](https://img.shields.io/badge/Paper-1.21.11-orange.svg)](https://papermc.io)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

## Features

- ✅ **Custom Items** - Create items with custom models, behaviors, and attributes
- ✅ **Real Custom Blocks** - Not just retextured vanilla blocks (uses note block/mushroom mechanics)
- ✅ **Custom Armor** - Full equipment layer support (1.21.4+ component system + legacy trim support)
- ✅ **Extensible Behavior System** - Create behaviors in external addons
- ✅ **Auto-Generated Resource Packs** - No manual pack creation needed
- ✅ **Version Migration** - Update items between plugin versions
- ✅ **1.21.11 Support** - Latest Paper/Purpur compatibility

## Installation

1. Download `LipoItems-1.0.0.jar`
2. Place in `plugins/` folder
3. Start server
4. Edit configs in `plugins/LipoItems/`

## Quick Start

### Creating a Custom Item

Create `plugins/LipoItems/items/ruby_sword.yml`:

```yaml
id: lipoitems:ruby_sword
material: DIAMOND_SWORD
display-name: "<gradient:#ff0000:#ff8888>Ruby Sword</gradient>"
lore:
  - "<dark_gray>A sword forged from pure ruby."
  - ""
  - "<gold>⚔ Damage: <yellow>+8"

settings:
  enchantable: true
  tags:
    - "minecraft:swords"

behaviors:
  - type: lipoitems:fire_aspect
    damage: 3
    duration: 5

data:
  attribute-modifiers:
    - attribute: GENERIC_ATTACK_DAMAGE
      operation: ADD_NUMBER
      amount: 8
      slot: HAND
```

Then reload: `/lipoitems reload`

Give item: `/givecustom <player> lipoitems:ruby_sword`

### Creating a Custom Block

Create `plugins/LipoItems/blocks/ruby_ore.yml`:

```yaml
id: lipoitems:ruby_ore
display-name: "Ruby Ore"
base-material: NOTE_BLOCK
properties:
  hardness: 3.0
  required-tool: PICKAXE
  minimum-tier: IRON
drops:
  - item: lipoitems:raw_ruby
    amount: 1
    chance: 1.0
```

## API for Addon Developers

### Maven Dependency

```xml
<repository>
    <id>your-repo</id>
    <url>https://your-repo.com/maven</url>
</repository>

<dependency>
    <groupId>dev.lipasquide</groupId>
    <artifactId>lipoitems-api</artifactId>
    <version>1.0.0</version>
    <scope>provided</scope>
</dependency>
```

### Creating a Custom Behavior

```java
public class ExplosiveBowBehavior implements ItemBehavior {

    private final float power;

    public ExplosiveBowBehavior(float power) {
        this.power = power;
    }

    @Override
    public InteractionResult use(Item<ItemStack> item, Player player, EquipmentSlot hand) {
        // Custom logic here
        player.getWorld().createExplosion(player.getLocation(), power);
        return InteractionResult.SUCCESS;
    }
}

// Register in your plugin
public class MyAddon extends JavaPlugin {
    @Override
    public void onEnable() {
        LipoItemsAPI api = getServer().getServicesManager()
            .load(LipoItemsAPI.class);

        api.registerBehavior(this, "myaddon:explosive_bow", 
            (plugin, path, node, id, args) -> {
                float power = ((Number) args.getOrDefault("power", 2.0)).floatValue();
                return new ExplosiveBowBehavior(power);
            });
    }
}
```

### Building Items Programmatically

```java
LipoItemsAPI api = ...;

// Build item for player
ItemStack item = api.buildItemStack("lipoitems:ruby_sword", player, 1);

// Check if item is custom
if (api.isCustomItem(item)) {
    String id = api.getCustomItemId(item);
    // Do something
}
```

## Commands

| Command | Permission | Description |
|---------|-----------|-------------|
| `/lipoitems` | `lipoitems.use` | Main command |
| `/lipoitems reload` | `lipoitems.admin` | Reload plugin |
| `/lipoitems items` | `lipoitems.use` | List loaded items |
| `/lipoitems blocks` | `lipoitems.use` | List loaded blocks |
| `/givecustom <player> <item> [amount]` | `lipoitems.give` | Give custom item |

## Configuration

See `config.yml` for all options:

- Resource pack auto-generation
- Custom model data allocation
- Block storage methods
- Performance settings

## Supported Versions

- **Minecraft**: 1.21.11
- **Paper**: 1.21.11-R0.1-SNAPSHOT
- **Java**: 21+

## License

MIT License - See [LICENSE](LICENSE) for details.
---

**Made with ❤️ by Lipasquide**
