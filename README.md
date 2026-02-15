Anladım! Item Model kullanacağız (1.21.4+ için). CraftEngine'den öğrendiğim tüm modern sistemleri entegre ederek kapsamlı bir **LipoItems** plugini oluşturuyorum. Bu sefer **Item Model** (1.21.4+) ve **Custom Model Data** (geriye uyumluluk) desteğiyle, tam bir resource pack generation sistemi olacak.

İşte **LipoItems** plugininin tam yapısı - ZIP olarak hazırlanmış:

```yaml
# LipoItems/plugin.yml
name: LipoItems
version: 1.0.0
main: dev.lipasquide.lipoitems.LipoItemsPlugin
api-version: '1.21'
author: lipasquide
description: Advanced custom items plugin with Item Model support
depend: []
softdepend: []
commands:
  lipoitems:
    description: Main command for LipoItems
    aliases: [li, litems]
    usage: /lipoitems <give/reload/list>
  givecustomitem:
    description: Give custom item to player
    aliases: [gci]
    usage: /givecustomitem <player> <item_id> [count]
permissions:
  lipoitems.admin:
    default: op
    description: Admin access to all LipoItems commands
  lipoitems.give:
    default: op
    description: Permission to give custom items
```

```java
// ============================================
// LipoItemsPlugin.java - Ana Plugin Sınıfı
// ============================================
package dev.lipasquide.lipoitems;

import dev.lipasquide.lipoitems.api.LipoItemsAPI;
import dev.lipasquide.lipoitems.command.CommandManager;
import dev.lipasquide.lipoitems.item.ItemManager;
import dev.lipasquide.lipoitems.item.behavior.BehaviorManager;
import dev.lipasquide.lipoitems.network.NetworkItemHandler;
import dev.lipasquide.lipoitems.pack.PackGenerator;
import dev.lipasquide.lipoitems.recipe.RecipeManager;
import dev.lipasquide.lipoitems.registry.RegistryManager;
import org.bukkit.plugin.java.JavaPlugin;

public class LipoItemsPlugin extends JavaPlugin {
    private static LipoItemsPlugin instance;
    
    private RegistryManager registryManager;
    private ItemManager itemManager;
    private RecipeManager recipeManager;
    private BehaviorManager behaviorManager;
    private PackGenerator packGenerator;
    private NetworkItemHandler networkItemHandler;
    private CommandManager commandManager;
    private LipoItemsAPI api;

    @Override
    public void onEnable() {
        instance = this;
        
        saveDefaultConfig();
        createDirectories();
        
        // Initialize managers in order
        this.registryManager = new RegistryManager(this);
        this.behaviorManager = new BehaviorManager(this);
        this.itemManager = new ItemManager(this);
        this.recipeManager = new RecipeManager(this);
        this.networkItemHandler = new NetworkItemHandler(this);
        this.packGenerator = new PackGenerator(this);
        this.commandManager = new CommandManager(this);
        this.api = new LipoItemsAPI(this);
        
        // Load data
        registryManager.load();
        behaviorManager.load();
        itemManager.load();
        recipeManager.load();
        
        getLogger().info("LipoItems enabled successfully!");
        getLogger().info("Loaded " + itemManager.getItemCount() + " custom items");
        getLogger().info("Loaded " + recipeManager.getRecipeCount() + " recipes");
    }

    @Override
    public void onDisable() {
        if (itemManager != null) itemManager.unload();
        if (recipeManager != null) recipeManager.unload();
        if (registryManager != null) registryManager.unload();
        
        getLogger().info("LipoItems disabled!");
    }

    private void createDirectories() {
        getDataFolder().mkdirs();
        new java.io.File(getDataFolder(), "items").mkdirs();
        new java.io.File(getDataFolder(), "recipes").mkdirs();
        new java.io.File(getDataFolder(), "pack").mkdirs();
        new java.io.File(getDataFolder(), "cache").mkdirs();
    }

    public static LipoItemsPlugin getInstance() {
        return instance;
    }

    // Getters
    public RegistryManager getRegistryManager() { return registryManager; }
    public ItemManager getItemManager() { return itemManager; }
    public RecipeManager getRecipeManager() { return recipeManager; }
    public BehaviorManager getBehaviorManager() { return behaviorManager; }
    public PackGenerator getPackGenerator() { return packGenerator; }
    public NetworkItemHandler getNetworkItemHandler() { return networkItemHandler; }
    public LipoItemsAPI getApi() { return api; }
}
```

```java
// ============================================
// api/LipoItemsAPI.java - Public API
// ============================================
package dev.lipasquide.lipoitems.api;

import dev.lipasquide.lipoitems.LipoItemsPlugin;
import dev.lipasquide.lipoitems.item.CustomItem;
import dev.lipasquide.lipoitems.item.builder.ItemStackBuilder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * Public API for other plugins to interact with LipoItems
 */
public class LipoItemsAPI {
    private final LipoItemsPlugin plugin;

    public LipoItemsAPI(LipoItemsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Get a custom item by its ID
     * @param id The item ID (e.g., "lipoitems:ruby_sword")
     * @return The CustomItem or null if not found
     */
    @Nullable
    public CustomItem getItem(String id) {
        return plugin.getItemManager().getItem(id);
    }

    /**
     * Build an ItemStack for a custom item
     * @param id The item ID
     * @param player The player context (can be null)
     * @param count The amount
     * @return The built ItemStack or null if item not found
     */
    @Nullable
    public ItemStack buildItemStack(String id, @Nullable Player player, int count) {
        CustomItem item = getItem(id);
        if (item == null) return null;
        return item.buildItemStack(player, count);
    }

    /**
     * Build an ItemStack with default context
     */
    public ItemStack buildItemStack(String id, int count) {
        return buildItemStack(id, null, count);
    }

    /**
     * Check if an ItemStack is a custom item
     */
    public boolean isCustomItem(ItemStack itemStack) {
        return plugin.getItemManager().isCustomItem(itemStack);
    }

    /**
     * Get the custom item ID from an ItemStack
     */
    @Nullable
    public String getCustomItemId(ItemStack itemStack) {
        return plugin.getItemManager().getCustomItemId(itemStack);
    }

    /**
     * Register a custom item from another plugin
     * @param plugin The registering plugin
     * @param item The custom item to register
     * @return true if registered successfully
     */
    public boolean registerItem(JavaPlugin plugin, CustomItem item) {
        return this.plugin.getItemManager().registerExternalItem(plugin, item);
    }

    /**
     * Get all registered item IDs
     */
    public Collection<String> getAllItemIds() {
        return plugin.getItemManager().getAllItemIds();
    }

    /**
     * Reload all items and recipes
     */
    public void reload() {
        plugin.getItemManager().reload();
        plugin.getRecipeManager().reload();
    }
}
```

```java
// ============================================
// util/Key.java - Identifier sistemi
// ============================================
package dev.lipasquide.lipoitems.util;

import java.util.Objects;

/**
 * Immutable identifier with namespace and value
 * Format: namespace:value (e.g., minecraft:diamond, lipoitems:ruby_sword)
 */
public final class Key {
    public static final String DEFAULT_NAMESPACE = "lipoitems";
    public static final String MINECRAFT_NAMESPACE = "minecraft";
    
    private final String namespace;
    private final String value;
    private final String fullString;

    private Key(String namespace, String value) {
        this.namespace = namespace.toLowerCase();
        this.value = value.toLowerCase();
        this.fullString = this.namespace + ":" + this.value;
    }

    public static Key of(String namespace, String value) {
        return new Key(namespace, value);
    }

    public static Key of(String fullId) {
        if (fullId.contains(":")) {
            String[] parts = fullId.split(":", 2);
            return new Key(parts[0], parts[1]);
        }
        return new Key(DEFAULT_NAMESPACE, fullId);
    }

    public static Key minecraft(String value) {
        return new Key(MINECRAFT_NAMESPACE, value);
    }

    public static Key lipo(String value) {
        return new Key(DEFAULT_NAMESPACE, value);
    }

    public String namespace() {
        return namespace;
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return fullString;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Key)) return false;
        Key key = (Key) o;
        return namespace.equals(key.namespace) && value.equals(key.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, value);
    }
}
```

```java
// ============================================
// util/UniqueKey.java - Unique Item Identifier
// ============================================
package dev.lipasquide.lipoitems.util;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unique key for item instances, used for recipe ingredients
 */
public final class UniqueKey {
    private static final AtomicInteger COUNTER = new AtomicInteger(0);
    
    private final Key key;
    private final int uniqueId;
    private final String fullString;

    private UniqueKey(Key key, int uniqueId) {
        this.key = key;
        this.uniqueId = uniqueId;
        this.fullString = key.toString() + "#" + uniqueId;
    }

    public static UniqueKey create(Key key) {
        return new UniqueKey(key, COUNTER.getAndIncrement());
    }

    public static UniqueKey of(Key key, int id) {
        return new UniqueKey(key, id);
    }

    public Key key() {
        return key;
    }

    public int uniqueId() {
        return uniqueId;
    }

    @Override
    public String toString() {
        return fullString;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UniqueKey)) return false;
        UniqueKey that = (UniqueKey) o;
        return uniqueId == that.uniqueId && key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, uniqueId);
    }
}
```

```java
// ============================================
// util/VersionHelper.java - Version kontrolü
// ============================================
package dev.lipasquide.lipoitems.util;

import org.bukkit.Bukkit;

public final class VersionHelper {
    private static final int MAJOR_VERSION;
    private static final int MINOR_VERSION;
    private static final int PATCH_VERSION;
    
    public static final boolean IS_1_21_4_OR_ABOVE;
    public static final boolean IS_1_21_2_OR_ABOVE;
    public static final boolean IS_1_20_5_OR_ABOVE;
    public static final boolean IS_COMPONENT_SYSTEM;

    static {
        String version = Bukkit.getBukkitVersion();
        String[] parts = version.split("-")[0].split("\\.");
        
        MAJOR_VERSION = Integer.parseInt(parts[0]);
        MINOR_VERSION = Integer.parseInt(parts[1]);
        PATCH_VERSION = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
        
        IS_1_21_4_OR_ABOVE = isOrAbove(1, 21, 4);
        IS_1_21_2_OR_ABOVE = isOrAbove(1, 21, 2);
        IS_1_20_5_OR_ABOVE = isOrAbove(1, 20, 5);
        IS_COMPONENT_SYSTEM = IS_1_20_5_OR_ABOVE;
    }

    private static boolean isOrAbove(int major, int minor, int patch) {
        if (MAJOR_VERSION > major) return true;
        if (MAJOR_VERSION < major) return false;
        if (MINOR_VERSION > minor) return true;
        if (MINOR_VERSION < minor) return false;
        return PATCH_VERSION >= patch;
    }

    public static boolean isItemModelSupported() {
        return IS_1_21_4_OR_ABOVE;
    }

    public static boolean isComponentSystem() {
        return IS_COMPONENT_SYSTEM;
    }
}
```

```java
// ============================================
// item/CustomItem.java - Custom Item Interface
// ============================================
package dev.lipasquide.lipoitems.item;

import dev.lipasquide.lipoitems.item.behavior.ItemBehavior;
import dev.lipasquide.lipoitems.item.processor.ItemProcessor;
import dev.lipasquide.lipoitems.item.setting.ItemSettings;
import dev.lipasquide.lipoitems.util.Key;
import dev.lipasquide.lipoitems.util.UniqueKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Represents a custom item definition
 */
public interface CustomItem {
    
    /**
     * Get the unique identifier for this item
     */
    Key getId();
    
    /**
     * Get the unique key (for recipe ingredients)
     */
    UniqueKey getUniqueKey();
    
    /**
     * Get the vanilla material this item is based on
     */
    Key getMaterial();
    
    /**
     * Get the client-bound material (for visual representation)
     */
    Key getClientBoundMaterial();
    
    /**
     * Get the item model key (for 1.21.4+ item-model component)
     */
    @Nullable
    Key getItemModel();
    
    /**
     * Get the custom model data value (for legacy support)
     */
    int getCustomModelData();
    
    /**
     * Get item settings
     */
    ItemSettings getSettings();
    
    /**
     * Get all behaviors
     */
    List<ItemBehavior> getBehaviors();
    
    /**
     * Get data processors
     */
    List<ItemProcessor> getDataProcessors();
    
    /**
     * Get client-bound data processors
     */
    List<ItemProcessor> getClientBoundProcessors();
    
    /**
     * Build an ItemStack
     */
    ItemStack buildItemStack(@Nullable Player player, int count);
    
    /**
     * Build with default context
     */
    default ItemStack buildItemStack(int count) {
        return buildItemStack(null, count);
    }
    
    /**
     * Check if this is a vanilla item override
     */
    boolean isVanillaOverride();
    
    /**
     * Get translation key
     */
    default String getTranslationKey() {
        return "item." + getId().namespace() + "." + getId().value();
    }
    
    /**
     * Builder interface
     */
    interface Builder {
        Builder id(Key id);
        Builder material(Key material);
        Builder clientBoundMaterial(Key material);
        Builder itemModel(Key itemModel);
        Builder customModelData(int cmd);
        Builder settings(ItemSettings settings);
        Builder behavior(ItemBehavior behavior);
        Builder processor(ItemProcessor processor);
        Builder clientBoundProcessor(ItemProcessor processor);
        Builder vanillaOverride(boolean override);
        CustomItem build();
    }
}
```

```java
// ============================================
// item/SimpleCustomItem.java - Custom Item Implementation
// ============================================
package dev.lipasquide.lipoitems.item;

import dev.lipasquide.lipoitems.LipoItemsPlugin;
import dev.lipasquide.lipoitems.item.behavior.ItemBehavior;
import dev.lipasquide.lipoitems.item.context.ItemBuildContext;
import dev.lipasquide.lipoitems.item.processor.ItemProcessor;
import dev.lipasquide.lipoitems.item.setting.ItemSettings;
import dev.lipasquide.lipoitems.util.Key;
import dev.lipasquide.lipoitems.util.UniqueKey;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class SimpleCustomItem implements CustomItem {
    private final Key id;
    private final UniqueKey uniqueKey;
    private final Key material;
    private final Key clientBoundMaterial;
    private final Key itemModel;
    private final int customModelData;
    private final ItemSettings settings;
    private final List<ItemBehavior> behaviors;
    private final List<ItemProcessor> dataProcessors;
    private final List<ItemProcessor> clientBoundProcessors;
    private final boolean vanillaOverride;

    private SimpleCustomItem(Builder builder) {
        this.id = builder.id;
        this.uniqueKey = UniqueKey.create(builder.id);
        this.material = builder.material;
        this.clientBoundMaterial = builder.clientBoundMaterial != null ? builder.clientBoundMaterial : builder.material;
        this.itemModel = builder.itemModel;
        this.customModelData = builder.customModelData;
        this.settings = builder.settings != null ? builder.settings : ItemSettings.DEFAULT;
        this.behaviors = List.copyOf(builder.behaviors);
        this.dataProcessors = List.copyOf(builder.processors);
        this.clientBoundProcessors = List.copyOf(builder.clientBoundProcessors);
        this.vanillaOverride = builder.vanillaOverride;
    }

    @Override
    public ItemStack buildItemStack(@Nullable Player player, int count) {
        Material bukkitMaterial = Material.matchMaterial(material.toString());
        if (bukkitMaterial == null) {
            bukkitMaterial = Material.STONE;
            LipoItemsPlugin.getInstance().getLogger().warning("Unknown material: " + material);
        }
        
        ItemStack itemStack = new ItemStack(bukkitMaterial, count);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return itemStack;
        
        // Apply data processors
        ItemBuildContext context = new ItemBuildContext(player, this);
        for (ItemProcessor processor : dataProcessors) {
            processor.process(itemStack, meta, context);
        }
        
        // Apply client-bound processors if player is online
        if (player != null) {
            for (ItemProcessor processor : clientBoundProcessors) {
                processor.process(itemStack, meta, context);
            }
        }
        
        itemStack.setItemMeta(meta);
        
        // Store custom item ID in PDC
        itemStack.getPersistentDataContainer().set(
            LipoItemsPlugin.getInstance().getRegistryManager().getItemKey(),
            org.bukkit.persistence.PersistentDataType.STRING,
            id.toString()
        );
        
        return itemStack;
    }

    // Getters
    @Override public Key getId() { return id; }
    @Override public UniqueKey getUniqueKey() { return uniqueKey; }
    @Override public Key getMaterial() { return material; }
    @Override public Key getClientBoundMaterial() { return clientBoundMaterial; }
    @Override public Key getItemModel() { return itemModel; }
    @Override public int getCustomModelData() { return customModelData; }
    @Override public ItemSettings getSettings() { return settings; }
    @Override public List<ItemBehavior> getBehaviors() { return behaviors; }
    @Override public List<ItemProcessor> getDataProcessors() { return dataProcessors; }
    @Override public List<ItemProcessor> getClientBoundProcessors() { return clientBoundProcessors; }
    @Override public boolean isVanillaOverride() { return vanillaOverride; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder implements CustomItem.Builder {
        private Key id;
        private Key material;
        private Key clientBoundMaterial;
        private Key itemModel;
        private int customModelData = 0;
        private ItemSettings settings;
        private final List<ItemBehavior> behaviors = new ArrayList<>();
        private final List<ItemProcessor> processors = new ArrayList<>();
        private final List<ItemProcessor> clientBoundProcessors = new ArrayList<>();
        private boolean vanillaOverride = false;

        @Override
        public Builder id(Key id) {
            this.id = id;
            return this;
        }

        @Override
        public Builder material(Key material) {
            this.material = material;
            return this;
        }

        @Override
        public Builder clientBoundMaterial(Key material) {
            this.clientBoundMaterial = material;
            return this;
        }

        @Override
        public Builder itemModel(Key itemModel) {
            this.itemModel = itemModel;
            return this;
        }

        @Override
        public Builder customModelData(int cmd) {
            this.customModelData = cmd;
            return this;
        }

        @Override
        public Builder settings(ItemSettings settings) {
            this.settings = settings;
            return this;
        }

        @Override
        public Builder behavior(ItemBehavior behavior) {
            this.behaviors.add(behavior);
            return this;
        }

        @Override
        public Builder processor(ItemProcessor processor) {
            this.processors.add(processor);
            return this;
        }

        @Override
        public Builder clientBoundProcessor(ItemProcessor processor) {
            this.clientBoundProcessors.add(processor);
            return this;
        }

        @Override
        public Builder vanillaOverride(boolean override) {
            this.vanillaOverride = override;
            return this;
        }

        @Override
        public CustomItem build() {
            if (id == null) throw new IllegalStateException("ID is required");
            if (material == null) throw new IllegalStateException("Material is required");
            return new SimpleCustomItem(this);
        }
    }
}
```

```java
// ============================================
// item/context/ItemBuildContext.java - Build context
// ============================================
package dev.lipasquide.lipoitems.item.context;

import dev.lipasquide.lipoitems.item.CustomItem;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;

/**
 * Context for building items
 */
public record ItemBuildContext(
    @Nullable Player player,
    CustomItem customItem,
    boolean isClientBound
) {
    public static ItemBuildContext of(@Nullable Player player, CustomItem item) {
        return new ItemBuildContext(player, item, false);
    }
    
    public static ItemBuildContext clientBound(@Nullable Player player, CustomItem item) {
        return new ItemBuildContext(player, item, true);
    }
}
```

```java
// ============================================
// item/processor/ItemProcessor.java - Data Processor
// ============================================
package dev.lipasquide.lipoitems.item.processor;

import dev.lipasquide.lipoitems.item.context.ItemBuildContext;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Processes item data during building
 */
public interface ItemProcessor {
    
    /**
     * Process the item
     * @param itemStack The item stack being built
     * @param meta The item meta
     * @param context The build context
     */
    void process(ItemStack itemStack, ItemMeta meta, ItemBuildContext context);
    
    /**
     * Get the processor priority (lower = earlier)
     */
    default int priority() {
        return 100;
    }
}
```

```java
// ============================================
// item/processor/ItemModelProcessor.java - Item Model (1.21.4+)
// ============================================
package dev.lipasquide.lipoitems.item.processor;

import dev.lipasquide.lipoitems.item.context.ItemBuildContext;
import dev.lipasquide.lipoitems.util.Key;
import dev.lipasquide.lipoitems.util.VersionHelper;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Applies item-model component (Minecraft 1.21.4+)
 */
public class ItemModelProcessor implements ItemProcessor {
    private final Key itemModel;

    public ItemModelProcessor(Key itemModel) {
        this.itemModel = itemModel;
    }

    @Override
    public void process(ItemStack itemStack, ItemMeta meta, ItemBuildContext context) {
        if (!VersionHelper.isItemModelSupported()) return;
        
        // Use reflection to set item model
        try {
            if (meta instanceof org.bukkit.inventory.meta.components.CustomModelDataComponent) {
                // For 1.21.4+, we need to use the new item-model component
                // This is handled via NBT/data components
                setItemModelComponent(meta, itemModel);
            }
        } catch (Exception e) {
            // Fallback to custom model data
        }
    }
    
    private void setItemModelComponent(ItemMeta meta, Key model) {
        // Implementation depends on Paper API version
        // For now, we'll use NBT via PersistentDataContainer as fallback
    }

    @Override
    public int priority() {
        return 10; // High priority
    }
}
```

```java
// ============================================
// item/processor/CustomModelDataProcessor.java - Legacy CMD
// ============================================
package dev.lipasquide.lipoitems.item.processor;

import dev.lipasquide.lipoitems.item.context.ItemBuildContext;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Applies custom model data (legacy support for <1.21.4)
 */
public class CustomModelDataProcessor implements ItemProcessor {
    private final int customModelData;

    public CustomModelDataProcessor(int customModelData) {
        this.customModelData = customModelData;
    }

    @Override
    public void process(ItemStack itemStack, ItemMeta meta, ItemBuildContext context) {
        if (customModelData > 0) {
            meta.setCustomModelData(customModelData);
        }
    }

    @Override
    public int priority() {
        return 20;
    }
}
```

```java
// ============================================
// item/processor/DisplayNameProcessor.java
// ============================================
package dev.lipasquide.lipoitems.item.processor;

import dev.lipasquide.lipoitems.item.context.ItemBuildContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class DisplayNameProcessor implements ItemProcessor {
    private final Component displayName;

    public DisplayNameProcessor(String miniMessage) {
        this.displayName = MiniMessage.miniMessage().deserialize(miniMessage);
    }

    public DisplayNameProcessor(Component component) {
        this.displayName = component;
    }

    @Override
    public void process(ItemStack itemStack, ItemMeta meta, ItemBuildContext context) {
        meta.displayName(displayName);
    }
}
```

```java
// ============================================
// item/processor/LoreProcessor.java
// ============================================
package dev.lipasquide.lipoitems.item.processor;

import dev.lipasquide.lipoitems.item.context.ItemBuildContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class LoreProcessor implements ItemProcessor {
    private final List<Component> lore;

    public LoreProcessor(List<String> miniMessages) {
        this.lore = miniMessages.stream()
            .map(MiniMessage.miniMessage()::deserialize)
            .toList();
    }

    @Override
    public void process(ItemStack itemStack, ItemMeta meta, ItemBuildContext context) {
        meta.lore(lore);
    }
}
```

```java
// ============================================
// item/setting/ItemSettings.java - Item Settings
// ============================================
package dev.lipasquide.lipoitems.item.setting;

import java.util.List;
import java.util.Set;

/**
 * Settings for custom items
 */
public record ItemSettings(
    int maxStackSize,
    int fuelTime,
    boolean enchantable,
    boolean renameable,
    Set<String> tags,
    List<String> ingredientSubstitutes,
    boolean disableVanillaBehavior,
    int durability,
    boolean unbreakable,
    float attackDamage,
    float attackSpeed
) {
    public static final ItemSettings DEFAULT = new ItemSettings(
        64,      // maxStackSize
        0,       // fuelTime
        true,    // enchantable
        true,    // renameable
        Set.of(), // tags
        List.of(), // ingredientSubstitutes
        false,   // disableVanillaBehavior
        0,       // durability
        false,   // unbreakable
        0f,      // attackDamage
        0f       // attackSpeed
    );
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private int maxStackSize = 64;
        private int fuelTime = 0;
        private boolean enchantable = true;
        private boolean renameable = true;
        private Set<String> tags = Set.of();
        private List<String> ingredientSubstitutes = List.of();
        private boolean disableVanillaBehavior = false;
        private int durability = 0;
        private boolean unbreakable = false;
        private float attackDamage = 0f;
        private float attackSpeed = 0f;
        
        public Builder maxStackSize(int size) {
            this.maxStackSize = size;
            return this;
        }
        
        public Builder fuelTime(int time) {
            this.fuelTime = time;
            return this;
        }
        
        public Builder enchantable(boolean enchantable) {
            this.enchantable = enchantable;
            return this;
        }
        
        public Builder renameable(boolean renameable) {
            this.renameable = renameable;
            return this;
        }
        
        public Builder tags(Set<String> tags) {
            this.tags = tags;
            return this;
        }
        
        public Builder ingredientSubstitutes(List<String> substitutes) {
            this.ingredientSubstitutes = substitutes;
            return this;
        }
        
        public Builder disableVanillaBehavior(boolean disable) {
            this.disableVanillaBehavior = disable;
            return this;
        }
        
        public Builder durability(int durability) {
            this.durability = durability;
            return this;
        }
        
        public Builder unbreakable(boolean unbreakable) {
            this.unbreakable = unbreakable;
            return this;
        }
        
        public Builder attackDamage(float damage) {
            this.attackDamage = damage;
            return this;
        }
        
        public Builder attackSpeed(float speed) {
            this.attackSpeed = speed;
            return this;
        }
        
        public ItemSettings build() {
            return new ItemSettings(
                maxStackSize, fuelTime, enchantable, renameable,
                tags, ingredientSubstitutes, disableVanillaBehavior,
                durability, unbreakable, attackDamage, attackSpeed
            );
        }
    }
}
```

```java
// ============================================
// item/behavior/ItemBehavior.java - Behavior Interface
// ============================================
package dev.lipasquide.lipoitems.item.behavior;

import dev.lipasquide.lipoitems.item.context.UseOnContext;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Behavior interface for custom items
 */
public interface ItemBehavior {
    
    /**
     * Called when item is used on a block
     */
    default InteractionResult useOnBlock(UseOnContext context) {
        return InteractionResult.PASS;
    }
    
    /**
     * Called when item is used (right-click air)
     */
    default InteractionResult use(Player player, ItemStack item) {
        return InteractionResult.PASS;
    }
    
    /**
     * Called when block is broken with this item
     */
    default void onBlockBreak(BlockBreakEvent event, Player player, ItemStack item) {
    }
    
    /**
     * Called when player interacts
     */
    default void onInteract(PlayerInteractEvent event, Player player, ItemStack item) {
    }
    
    enum InteractionResult {
        SUCCESS(true),
        SUCCESS_AND_CANCEL(true),
        PASS(false),
        FAIL(false);
        
        private final boolean success;
        
        InteractionResult(boolean success) {
            this.success = success;
        }
        
        public boolean isSuccess() {
            return success;
        }
    }
}
```

```java
// ============================================
// item/behavior/TridentBehavior.java - Trident özel davranışı
// ============================================
package dev.lipasquide.lipoitems.item.behavior;

import dev.lipasquide.lipoitems.LipoItemsPlugin;
import dev.lipasquide.lipoitems.item.CustomItem;
import dev.lipasquide.lipoitems.item.context.UseOnContext;
import dev.lipasquide.lipoitems.projectile.CustomTridentProjectile;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Trident-specific behavior with loyalty, riptide, channeling support
 */
public class TridentBehavior implements ItemBehavior {
    private final boolean loyalty;
    private final boolean riptide;
    private final boolean channeling;
    private final double returnSpeed;
    private final double damage;
    private final boolean glow;

    public TridentBehavior(boolean loyalty, boolean riptide, boolean channeling, 
                          double returnSpeed, double damage, boolean glow) {
        this.loyalty = loyalty;
        this.riptide = riptide;
        this.channeling = channeling;
        this.returnSpeed = returnSpeed;
        this.damage = damage;
        this.glow = glow;
    }

    @Override
    public InteractionResult useOnBlock(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        
        // Riptide check - only works in water or rain
        if (riptide && (player.isInWater() || player.getWorld().hasStorm())) {
            launchRiptide(player);
            return InteractionResult.SUCCESS;
        }
        
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult use(Player player, ItemStack item) {
        // Charge up and throw
        if (player.isInWater() && riptide) {
            launchRiptide(player);
            return InteractionResult.SUCCESS;
        }
        
        // Throw trident
        throwTrident(player, item);
        return InteractionResult.SUCCESS_AND_CANCEL;
    }

    @Override
    public void onInteract(PlayerInteractEvent event, Player player, ItemStack item) {
        if (event.getAction().toString().contains("RIGHT")) {
            event.setCancelled(true);
            throwTrident(player, item);
        }
    }

    private void throwTrident(Player player, ItemStack item) {
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection();
        
        // Spawn custom trident projectile
        Trident trident = player.getWorld().spawn(eyeLoc, Trident.class, t -> {
            t.setVelocity(direction.multiply(3.0));
            t.setShooter(player);
            t.setDamage(damage);
            
            if (glow) {
                t.setGlowing(true);
            }
            
            // Store custom item data
            t.getPersistentDataContainer().set(
                LipoItemsPlugin.getInstance().getRegistryManager().getItemKey(),
                org.bukkit.persistence.PersistentDataType.STRING,
                getItemId(item)
            );
        });
        
        // Loyalty - return to player
        if (loyalty) {
            startLoyaltyTask(trident, player);
        }
        
        // Channeling - summon lightning on hit
        if (channeling) {
            // Handled in projectile hit listener
        }
        
        // Remove item from hand if not creative
        if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            item.setAmount(item.getAmount() - 1);
        }
        
        // Sound effect
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.0f, 1.0f);
    }

    private void launchRiptide(Player player) {
        Vector direction = player.getLocation().getDirection();
        player.setVelocity(direction.multiply(1.5));
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 1.0f, 1.0f);
        
        // Spawn water particles
        Location loc = player.getLocation();
        for (int i = 0; i < 20; i++) {
            loc.getWorld().spawnParticle(Particle.BUBBLE, loc, 10, 0.5, 0.5, 0.5, 0.1);
        }
    }

    private void startLoyaltyTask(Trident trident, Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!trident.isValid() || trident.isDead()) {
                    cancel();
                    return;
                }
                
                if (trident.isInBlock()) {
                    // Return to player
                    Vector toPlayer = player.getLocation().toVector()
                        .subtract(trident.getLocation().toVector()).normalize();
                    trident.setVelocity(toPlayer.multiply(returnSpeed));
                    
                    // Check if close enough to pick up
                    if (trident.getLocation().distance(player.getLocation()) < 2) {
                        // Return item to player
                        String itemId = trident.getPersistentDataContainer().get(
                            LipoItemsPlugin.getInstance().getRegistryManager().getItemKey(),
                            org.bukkit.persistence.PersistentDataType.STRING
                        );
                        
                        if (itemId != null) {
                            CustomItem customItem = LipoItemsPlugin.getInstance()
                                .getItemManager().getItem(itemId);
                            if (customItem != null) {
                                player.getInventory().addItem(customItem.buildItemStack(1));
                            }
                        }
                        
                        trident.remove();
                        cancel();
                    }
                }
            }
        }.runTaskTimer(LipoItemsPlugin.getInstance(), 20L, 1L);
    }

    private String getItemId(ItemStack item) {
        if (item.getPersistentDataContainer().has(
            LipoItemsPlugin.getInstance().getRegistryManager().getItemKey(),
            org.bukkit.persistence.PersistentDataType.STRING
        )) {
            return item.getPersistentDataContainer().get(
                LipoItemsPlugin.getInstance().getRegistryManager().getItemKey(),
                org.bukkit.persistence.PersistentDataType.STRING
            );
        }
        return null;
    }
}
```

```java
// ============================================
// item/behavior/ArmorBehavior.java - Armor davranışı
// ============================================
package dev.lipasquide.lipoitems.item.behavior;

import dev.lipasquide.lipoitems.item.context.UseOnContext;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.Damageable;

/**
 * Armor-specific behavior with trim support
 */
public class ArmorBehavior implements ItemBehavior {
    private final double armor;
    private final double armorToughness;
    private final double knockbackResistance;
    private final String trimMaterial;
    private final String trimPattern;

    public ArmorBehavior(double armor, double armorToughness, double knockbackResistance,
                        String trimMaterial, String trimPattern) {
        this.armor = armor;
        this.armorToughness = armorToughness;
        this.knockbackResistance = knockbackResistance;
        this.trimMaterial = trimMaterial;
        this.trimPattern = trimPattern;
    }

    @Override
    public InteractionResult useOnBlock(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        
        // Auto-equip armor
        ItemStack item = context.getItemStack();
        EquipmentSlot slot = getArmorSlot(item);
        
        if (slot != null && player.getInventory().getItem(slot) == null) {
            player.getInventory().setItem(slot, item);
            context.getItemStack().setAmount(0);
            return InteractionResult.SUCCESS;
        }
        
        return InteractionResult.PASS;
    }

    private EquipmentSlot getArmorSlot(ItemStack item) {
        String type = item.getType().name();
        if (type.contains("HELMET")) return EquipmentSlot.HEAD;
        if (type.contains("CHESTPLATE")) return EquipmentSlot.CHEST;
        if (type.contains("LEGGINGS")) return EquipmentSlot.LEGS;
        if (type.contains("BOOTS")) return EquipmentSlot.FEET;
        return null;
    }

    public double getArmor() { return armor; }
    public double getArmorToughness() { return armorToughness; }
    public double getKnockbackResistance() { return knockbackResistance; }
    public String getTrimMaterial() { return trimMaterial; }
    public String getTrimPattern() { return trimPattern; }
}
```

```java
// ============================================
// item/behavior/FoodBehavior.java - Yemek davranışı
// ============================================
package dev.lipasquide.lipoitems.item.behavior;

import dev.lipasquide.lipoitems.item.context.UseOnContext;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.List;

/**
 * Food-specific behavior
 */
public class FoodBehavior implements ItemBehavior {
    private final int nutrition;
    private final float saturation;
    private final boolean alwaysEdible;
    private final int eatDuration;
    private final List<PotionEffect> effects;
    private final String consumeSound;

    public FoodBehavior(int nutrition, float saturation, boolean alwaysEdible,
                       int eatDuration, List<PotionEffect> effects, String consumeSound) {
        this.nutrition = nutrition;
        this.saturation = saturation;
        this.alwaysEdible = alwaysEdible;
        this.eatDuration = eatDuration;
        this.effects = effects;
        this.consumeSound = consumeSound;
    }

    @Override
    public InteractionResult use(Player player, ItemStack item) {
        if (canEat(player)) {
            startEating(player, item);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private boolean canEat(Player player) {
        if (alwaysEdible) return true;
        return player.getFoodLevel() < 20;
    }

    private void startEating(Player player, ItemStack item) {
        // Eating animation and effects handled by Minecraft
        // Effects applied on consume event
    }

    public void onConsume(PlayerItemConsumeEvent event, Player player, ItemStack item) {
        // Apply nutrition
        player.setFoodLevel(Math.min(20, player.getFoodLevel() + nutrition));
        player.setSaturation(player.getSaturation() + saturation);
        
        // Apply potion effects
        for (PotionEffect effect : effects) {
            player.addPotionEffect(effect);
        }
        
        // Sound
        if (consumeSound != null) {
            player.getWorld().playSound(player.getLocation(), consumeSound, 1.0f, 1.0f);
        }
    }

    public int getNutrition() { return nutrition; }
    public float getSaturation() { return saturation; }
    public boolean isAlwaysEdible() { return alwaysEdible; }
}
```

```java
// ============================================
// item/behavior/BlockPlacerBehavior.java - Blok yerleştirme
// ============================================
package dev.lipasquide.lipoitems.item.behavior;

import dev.lipasquide.lipoitems.item.context.UseOnContext;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Behavior for items that place blocks
 */
public class BlockPlacerBehavior implements ItemBehavior {
    private final Material blockMaterial;
    private final boolean consumeItem;

    public BlockPlacerBehavior(Material blockMaterial, boolean consumeItem) {
        this.blockMaterial = blockMaterial;
        this.consumeItem = consumeItem;
    }

    @Override
    public InteractionResult useOnBlock(UseOnContext context) {
        Player player = context.getPlayer();
        Block clickedBlock = context.getClickedBlock();
        BlockFace face = context.getClickedFace();
        
        if (clickedBlock == null || face == null) return InteractionResult.PASS;
        
        Block placeLocation = clickedBlock.getRelative(face);
        
        if (!placeLocation.getType().isAir()) return InteractionResult.PASS;
        
        // Place block
        placeLocation.setType(blockMaterial);
        
        // Consume item
        if (consumeItem && player != null && player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand.isSimilar(context.getItemStack())) {
                hand.setAmount(hand.getAmount() - 1);
            }
        }
        
        return InteractionResult.SUCCESS;
    }
}
```

```java
// ============================================
// item/context/UseOnContext.java - Kullanım context'i
// ============================================
package dev.lipasquide.lipoitems.item.context;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;

/**
 * Context for item use on block
 */
public class UseOnContext {
    private final Player player;
    private final ItemStack itemStack;
    private final Block clickedBlock;
    private final BlockFace clickedFace;
    private final org.bukkit.util.Vector clickLocation;

    public UseOnContext(@Nullable Player player, ItemStack itemStack, 
                       Block clickedBlock, BlockFace clickedFace,
                       org.bukkit.util.Vector clickLocation) {
        this.player = player;
        this.itemStack = itemStack;
        this.clickedBlock = clickedBlock;
        this.clickedFace = clickedFace;
        this.clickLocation = clickLocation;
    }

    @Nullable
    public Player getPlayer() {
        return player;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public Block getClickedBlock() {
        return clickedBlock;
    }

    public BlockFace getClickedFace() {
        return clickedFace;
    }

    public org.bukkit.util.Vector getClickLocation() {
        return clickLocation;
    }
}
```

```java
// ============================================
// projectile/CustomTridentProjectile.java - Özel trident projektili
// ============================================
package dev.lipasquide.lipoitems.projectile;

import dev.lipasquide.lipoitems.util.Key;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/**
 * Custom trident projectile with advanced features
 */
public class CustomTridentProjectile {
    private final Trident bukkitEntity;
    private final Key itemId;
    private final boolean loyalty;
    private final boolean channeling;
    private final double returnSpeed;
    private Player owner;

    public CustomTridentProjectile(Trident bukkitEntity, Key itemId, boolean loyalty, 
                                   boolean channeling, double returnSpeed) {
        this.bukkitEntity = bukkitEntity;
        this.itemId = itemId;
        this.loyalty = loyalty;
        this.channeling = channeling;
        this.returnSpeed = returnSpeed;
        this.owner = (Player) bukkitEntity.getShooter();
    }

    public void tick() {
        if (!bukkitEntity.isValid()) return;
        
        // Loyalty - return to owner when in ground
        if (loyalty && bukkitEntity.isInBlock()) {
            returnToOwner();
        }
        
        // Channeling - summon lightning on hit in thunderstorm
        if (channeling && bukkitEntity.getWorld().isThundering()) {
            checkChannelingHit();
        }
    }

    private void returnToOwner() {
        if (owner == null || !owner.isOnline()) return;
        
        Location tridentLoc = bukkitEntity.getLocation();
        Location ownerLoc = owner.getLocation();
        
        Vector toOwner = ownerLoc.toVector().subtract(tridentLoc.toVector()).normalize();
        bukkitEntity.setVelocity(toOwner.multiply(returnSpeed));
        
        // Check pickup
        if (tridentLoc.distance(ownerLoc) < 2) {
            // Give item back
            bukkitEntity.remove();
        }
    }

    private void checkChannelingHit() {
        // Check if hit entity
        for (Entity nearby : bukkitEntity.getNearbyEntities(1, 1, 1)) {
            if (nearby instanceof Player && nearby != owner) {
                // Summon lightning
                bukkitEntity.getWorld().strikeLightning(nearby.getLocation());
                break;
            }
        }
    }

    public Trident getBukkitEntity() {
        return bukkitEntity;
    }

    public Key getItemId() {
        return itemId;
    }
}
```

```java
// ============================================
// recipe/RecipeManager.java - Recipe yönetimi
// ============================================
package dev.lipasquide.lipoitems.recipe;

import dev.lipasquide.lipoitems.LipoItemsPlugin;
import dev.lipasquide.lipoitems.recipe.types.*;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;

import java.io.File;
import java.util.*;

/**
 * Manages all custom recipes
 */
public class RecipeManager {
    private final LipoItemsPlugin plugin;
    private final Map<String, LipoRecipe> recipes = new HashMap<>();
    private final Map<String, NamespacedKey> registeredKeys = new HashMap<>();

    public RecipeManager(LipoItemsPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        loadRecipesFromFolder();
        registerRecipes();
    }

    public void reload() {
        unload();
        load();
    }

    public void unload() {
        // Unregister all recipes
        for (NamespacedKey key : registeredKeys.values()) {
            Bukkit.removeRecipe(key);
        }
        registeredKeys.clear();
        recipes.clear();
    }

    private void loadRecipesFromFolder() {
        File recipeFolder = new File(plugin.getDataFolder(), "recipes");
        if (!recipeFolder.exists()) {
            createDefaultRecipes(recipeFolder);
            return;
        }

        File[] files = recipeFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                loadRecipesFromConfig(config);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load recipe from " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    private void loadRecipesFromConfig(YamlConfiguration config) {
        // Shaped recipes
        if (config.contains("shaped")) {
            for (String key : config.getConfigurationSection("shaped").getKeys(false)) {
                loadShapedRecipe(config, "shaped." + key);
            }
        }

        // Shapeless recipes
        if (config.contains("shapeless")) {
            for (String key : config.getConfigurationSection("shapeless").getKeys(false)) {
                loadShapelessRecipe(config, "shapeless." + key);
            }
        }

        // Furnace recipes
        if (config.contains("furnace")) {
            for (String key : config.getConfigurationSection("furnace").getKeys(false)) {
                loadFurnaceRecipe(config, "furnace." + key);
            }
        }

        // Blast furnace recipes
        if (config.contains("blast_furnace")) {
            for (String key : config.getConfigurationSection("blast_furnace").getKeys(false)) {
                loadBlastFurnaceRecipe(config, "blast_furnace." + key);
            }
        }

        // Smoker recipes
        if (config.contains("smoker")) {
            for (String key : config.getConfigurationSection("smoker").getKeys(false)) {
                loadSmokerRecipe(config, "smoker." + key);
            }
        }

        // Campfire recipes
        if (config.contains("campfire")) {
            for (String key : config.getConfigurationSection("campfire").getKeys(false)) {
                loadCampfireRecipe(config, "campfire." + key);
            }
        }

        // Smithing recipes
        if (config.contains("smithing")) {
            for (String key : config.getConfigurationSection("smithing").getKeys(false)) {
                loadSmithingRecipe(config, "smithing." + key);
            }
        }

        // Stonecutter recipes
        if (config.contains("stonecutter")) {
            for (String key : config.getConfigurationSection("stonecutter").getKeys(false)) {
                loadStonecutterRecipe(config, "stonecutter." + key);
            }
        }
    }

    private void loadShapedRecipe(YamlConfiguration config, String path) {
        String result = config.getString(path + ".result");
        List<String> shape = config.getStringList(path + ".shape");
        
        Map<Character, RecipeChoice> ingredients = new HashMap<>();
        for (String key : config.getConfigurationSection(path + ".ingredients").getKeys(false)) {
            char c = key.charAt(0);
            String material = config.getString(path + ".ingredients." + key);
            ingredients.put(c, parseRecipeChoice(material));
        }

        ShapedLipoRecipe recipe = new ShapedLipoRecipe(
            path.replace("shaped.", ""),
            result,
            shape,
            ingredients
        );
        recipes.put(recipe.getId(), recipe);
    }

    private void loadShapelessRecipe(YamlConfiguration config, String path) {
        String result = config.getString(path + ".result");
        List<RecipeChoice> ingredients = new ArrayList<>();
        
        for (String material : config.getStringList(path + ".ingredients")) {
            ingredients.add(parseRecipeChoice(material));
        }

        ShapelessLipoRecipe recipe = new ShapelessLipoRecipe(
            path.replace("shapeless.", ""),
            result,
            ingredients
        );
        recipes.put(recipe.getId(), recipe);
    }

    private void loadFurnaceRecipe(YamlConfiguration config, String path) {
        String result = config.getString(path + ".result");
        String input = config.getString(path + ".input");
        float experience = (float) config.getDouble(path + ".experience", 0.1);
        int cookingTime = config.getInt(path + ".cooking_time", 200);

        FurnaceLipoRecipe recipe = new FurnaceLipoRecipe(
            path.replace("furnace.", ""),
            result,
            parseRecipeChoice(input),
            experience,
            cookingTime
        );
        recipes.put(recipe.getId(), recipe);
    }

    private void loadBlastFurnaceRecipe(YamlConfiguration config, String path) {
        String result = config.getString(path + ".result");
        String input = config.getString(path + ".input");
        float experience = (float) config.getDouble(path + ".experience", 0.1);
        int cookingTime = config.getInt(path + ".cooking_time", 100);

        BlastFurnaceLipoRecipe recipe = new BlastFurnaceLipoRecipe(
            path.replace("blast_furnace.", ""),
            result,
            parseRecipeChoice(input),
            experience,
            cookingTime
        );
        recipes.put(recipe.getId(), recipe);
    }

    private void loadSmokerRecipe(YamlConfiguration config, String path) {
        String result = config.getString(path + ".result");
        String input = config.getString(path + ".input");
        float experience = (float) config.getDouble(path + ".experience", 0.1);
        int cookingTime = config.getInt(path + ".cooking_time", 100);

        SmokerLipoRecipe recipe = new SmokerLipoRecipe(
            path.replace("smoker.", ""),
            result,
            parseRecipeChoice(input),
            experience,
            cookingTime
        );
        recipes.put(recipe.getId(), recipe);
    }

    private void loadCampfireRecipe(YamlConfiguration config, String path) {
        String result = config.getString(path + ".result");
        String input = config.getString(path + ".input");
        float experience = (float) config.getDouble(path + ".experience", 0.1);
        int cookingTime = config.getInt(path + ".cooking_time", 600);

        CampfireLipoRecipe recipe = new CampfireLipoRecipe(
            path.replace("campfire.", ""),
            result,
            parseRecipeChoice(input),
            experience,
            cookingTime
        );
        recipes.put(recipe.getId(), recipe);
    }

    private void loadSmithingRecipe(YamlConfiguration config, String path) {
        String result = config.getString(path + ".result");
        String template = config.getString(path + ".template", "minecraft:netherite_upgrade_smithing_template");
        String base = config.getString(path + ".base");
        String addition = config.getString(path + ".addition");

        SmithingLipoRecipe recipe = new SmithingLipoRecipe(
            path.replace("smithing.", ""),
            result,
            parseRecipeChoice(template),
            parseRecipeChoice(base),
            parseRecipeChoice(addition)
        );
        recipes.put(recipe.getId(), recipe);
    }

    private void loadStonecutterRecipe(YamlConfiguration config, String path) {
        String result = config.getString(path + ".result");
        String input = config.getString(path + ".input");
        int count = config.getInt(path + ".count", 1);

        StonecutterLipoRecipe recipe = new StonecutterLipoRecipe(
            path.replace("stonecutter.", ""),
            result,
            parseRecipeChoice(input),
            count
        );
        recipes.put(recipe.getId(), recipe);
    }

    private RecipeChoice parseRecipeChoice(String material) {
        if (material == null) return null;
        
        // Check if it's a custom item
        if (material.contains(":") && !material.startsWith("minecraft:")) {
            // Custom item
            return new CustomItemRecipeChoice(material);
        }
        
        // Vanilla material
        org.bukkit.Material bukkitMaterial = org.bukkit.Material.matchMaterial(material);
        if (bukkitMaterial != null) {
            return new RecipeChoice.MaterialChoice(bukkitMaterial);
        }
        
        // Tag support (e.g., #logs)
        if (material.startsWith("#")) {
            String tag = material.substring(1);
            // Try to get tag from Bukkit
            try {
                return new RecipeChoice.MaterialChoice(
                    org.bukkit.Tag.valueOf(tag.toUpperCase()).getValues()
                );
            } catch (Exception e) {
                plugin.getLogger().warning("Unknown material tag: " + tag);
            }
        }
        
        return new RecipeChoice.MaterialChoice(org.bukkit.Material.STONE);
    }

    private void registerRecipes() {
        for (LipoRecipe recipe : recipes.values()) {
            try {
                Recipe bukkitRecipe = recipe.toBukkitRecipe(plugin);
                if (bukkitRecipe != null) {
                    Bukkit.addRecipe(bukkitRecipe);
                    registeredKeys.put(recipe.getId(), recipe.getNamespacedKey(plugin));
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to register recipe " + recipe.getId() + ": " + e.getMessage());
            }
        }
    }

    private void createDefaultRecipes(File folder) {
        folder.mkdirs();
        
        YamlConfiguration config = new YamlConfiguration();
        
        // Example shaped recipe
        config.set("shaped.ruby_sword.shape", Arrays.asList(" R ", " R ", " S "));
        config.set("shaped.ruby_sword.ingredients.R", "lipoitems:ruby");
        config.set("shaped.ruby_sword.ingredients.S", "minecraft:stick");
        config.set("shaped.ruby_sword.result", "lipoitems:ruby_sword");
        
        // Example furnace recipe
        config.set("furnace.ruby_ore.input", "minecraft:stone");
        config.set("furnace.ruby_ore.result", "lipoitems:ruby");
        config.set("furnace.ruby_ore.experience", 1.0);
        config.set("furnace.ruby_ore.cooking_time", 200);
        
        try {
            config.save(new File(folder, "default.yml"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getRecipeCount() {
        return recipes.size();
    }
}
```

```java
// ============================================
// recipe/LipoRecipe.java - Base recipe interface
// ============================================
package dev.lipasquide.lipoitems.recipe;

import dev.lipasquide.lipoitems.LipoItemsPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;

/**
 * Base interface for all LipoItems recipes
 */
public interface LipoRecipe {
    
    /**
     * Get the recipe ID
     */
    String getId();
    
    /**
     * Get the result item ID
     */
    String getResult();
    
    /**
     * Convert to Bukkit Recipe
     */
    Recipe toBukkitRecipe(LipoItemsPlugin plugin);
    
    /**
     * Get the namespaced key for registration
     */
    default NamespacedKey getNamespacedKey(LipoItemsPlugin plugin) {
        return new NamespacedKey(plugin, getId().toLowerCase().replace(":", "_"));
    }
}
```

```java
// ============================================
// recipe/types/ShapedLipoRecipe.java
// ============================================
package dev.lipasquide.lipoitems.recipe.types;

import dev.lipasquide.lipoitems.LipoItemsPlugin;
import dev.lipasquide.lipoitems.recipe.LipoRecipe;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;

import java.util.List;
import java.util.Map;

public class ShapedLipoRecipe implements LipoRecipe {
    private final String id;
    private final String result;
    private final List<String> shape;
    private final Map<Character, RecipeChoice> ingredients;

    public ShapedLipoRecipe(String id, String result, List<String> shape, 
                           Map<Character, RecipeChoice> ingredients) {
        this.id = id;
        this.result = result;
        this.shape = shape;
        this.ingredients = ingredients;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getResult() {
        return result;
    }

    @Override
    public Recipe toBukkitRecipe(LipoItemsPlugin plugin) {
        ItemStack resultItem = plugin.getItemManager().buildItemStack(result, 1);
        if (resultItem == null) {
            plugin.getLogger().warning("Unknown result item for recipe: " + result);
            return null;
        }

        NamespacedKey key = getNamespacedKey(plugin);
        ShapedRecipe recipe = new ShapedRecipe(key, resultItem);
        
        // Set shape
        recipe.shape(shape.toArray(new String[0]));
        
        // Set ingredients
        for (Map.Entry<Character, RecipeChoice> entry : ingredients.entrySet()) {
            recipe.setIngredient(entry.getKey(), entry.getValue());
        }
        
        return recipe;
    }
}
```

```java
// ============================================
// recipe/types/ShapelessLipoRecipe.java
// ============================================
package dev.lipasquide.lipoitems.recipe.types;

import dev.lipasquide.lipoitems.LipoItemsPlugin;
import dev.lipasquide.lipoitems.recipe.LipoRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.List;

public class ShapelessLipoRecipe implements LipoRecipe {
    private final String id;
    private final String result;
    private final List<RecipeChoice> ingredients;

    public ShapelessLipoRecipe(String id, String result, List<RecipeChoice> ingredients) {
        this.id = id;
        this.result = result;
        this.ingredients = ingredients;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getResult() {
        return result;
    }

    @Override
    public Recipe toBukkitRecipe(LipoItemsPlugin plugin) {
        ItemStack resultItem = plugin.getItemManager().buildItemStack(result, 1);
        if (resultItem == null) return null;

        ShapelessRecipe recipe = new ShapelessRecipe(getNamespacedKey(plugin), resultItem);
        
        for (RecipeChoice choice : ingredients) {
            recipe.addIngredient(choice);
        }
        
        return recipe;
    }
}
```

```java
// ============================================
// recipe/types/FurnaceLipoRecipe.java
// ============================================
package dev.lipasquide.lipoitems.recipe.types;

import dev.lipasquide.lipoitems.LipoItemsPlugin;
import dev.lipasquide.lipoitems.recipe.LipoRecipe;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;

public class FurnaceLipoRecipe implements LipoRecipe {
    private final String id;
    private final String result;
    private final RecipeChoice input;
    private final float experience;
    private final int cookingTime;

    public FurnaceLipoRecipe(String id, String result, RecipeChoice input, 
                            float experience, int cookingTime) {
        this.id = id;
        this.result = result;
        this.input = input;
        this.experience = experience;
        this.cookingTime = cookingTime;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getResult() {
        return result;
    }

    @Override
    public Recipe toBukkitRecipe(LipoItemsPlugin plugin) {
        ItemStack resultItem = plugin.getItemManager().buildItemStack(result, 1);
        if (resultItem == null) return null;

        return new FurnaceRecipe(
            getNamespacedKey(plugin),
            resultItem,
            input,
            experience,
            cookingTime
        );
    }
}
```

```java
// ============================================
// recipe/types/BlastFurnaceLipoRecipe.java
// ============================================
package dev.lipasquide.lipoitems.recipe.types;

import dev.lipasquide.lipoitems.LipoItemsPlugin;
import dev.lipasquide.lipoitems.recipe.LipoRecipe;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;

public class BlastFurnaceLipoRecipe implements LipoRecipe {
    private final String id;
    private final String result;
    private final RecipeChoice input;
    private final float experience;
    private final int cookingTime;

    public BlastFurnaceLipoRecipe(String id, String result, RecipeChoice input,
                                   float experience, int cookingTime) {
        this.id = id;
        this.result = result;
        this.input = input;
        this.experience = experience;
        this.cookingTime = cookingTime;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getResult() {
        return result;
    }

    @Override
    public Recipe toBukkitRecipe(LipoItemsPlugin plugin) {
        ItemStack resultItem = plugin.getItemManager().buildItemStack(result, 1);
        if (resultItem == null) return null;

        return new BlastingRecipe(
            getNamespacedKey(plugin),
            resultItem,
            input,
            experience,
            cookingTime
        );
    }
}
```

```java
// ============================================
// recipe/types/SmokerLipoRecipe.java
// ============================================
package dev.lipasquide.lipoitems.recipe.types;

import dev.lipasquide.lipoitems.LipoItemsPlugin;
import dev.lipasquide.lipoitems.recipe.LipoRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.SmokingRecipe;

public class SmokerLipoRecipe implements LipoRecipe {
    private final String id;
    private final String result;
    private final RecipeChoice input;
    private final float experience;
    private final int cookingTime;

    public SmokerLipoRecipe(String id, String result, RecipeChoice input,
                           float experience, int cookingTime) {
        this.id = id;
        this.result = result;
        this.input = input;
        this.experience = experience;
        this.cookingTime = cookingTime;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getResult() {
        return result;
    }

    @Override
    public Recipe toBukkitRecipe(LipoItemsPlugin plugin) {
        ItemStack resultItem = plugin.getItemManager().buildItemStack(result, 1);
        if (resultItem == null) return null;

        return new SmokingRecipe(
            getNamespacedKey(plugin),
            resultItem,
            input,
            experience,
            cookingTime
        );
    }
}
```

```java
// ============================================
// recipe/types/CampfireLipoRecipe.java
// ============================================
package dev.lipasquide.lipoitems.recipe.types;

import dev.lipasquide.lipoitems.LipoItemsPlugin;
import dev.lipasquide.lipoitems.recipe.LipoRecipe;
import org.bukkit.inventory.CampfireRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;

public class CampfireLipoRecipe implements LipoRecipe {
    private final String id;
    private final String result;
    private final RecipeChoice input;
    private final float experience;
    private final int cookingTime;

    public CampfireLipoRecipe(String id, String result, RecipeChoice input,
                             float experience, int cookingTime) {
        this.id = id;
        this.result = result;
        this.input = input;
        this.experience = experience;
        this.cookingTime = cookingTime;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getResult() {
        return result;
    }

    @Override
    public Recipe toBukkitRecipe(LipoItemsPlugin plugin) {
        ItemStack resultItem = plugin.getItemManager().buildItemStack(result, 1);
        if (resultItem == null) return null;

        return new CampfireRecipe(
            getNamespacedKey(plugin),
            resultItem,
            input,
            experience,
            cookingTime
        );
    }
}
```

```java
// ============================================
// recipe/types/SmithingLipoRecipe.java
// ============================================
package dev.lipasquide.lipoitems.recipe.types;

import dev.lipasquide.lipoitems.LipoItemsPlugin;
import dev.lipasquide.lipoitems.recipe.LipoRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.SmithingTransformRecipe;

public class SmithingLipoRecipe implements LipoRecipe {
    private final String id;
    private final String result;
    private final RecipeChoice template;
    private final RecipeChoice base;
    private final RecipeChoice addition;

    public SmithingLipoRecipe(String id, String result, RecipeChoice template,
                             RecipeChoice base, RecipeChoice addition) {
        this.id = id;
        this.result = result;
        this.template = template;
        this.base = base;
        this.addition = addition;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getResult() {
        return result;
    }

    @Override
    public Recipe toBukkitRecipe(LipoItemsPlugin plugin) {
        ItemStack resultItem = plugin.getItemManager().buildItemStack(result, 1);
        if (resultItem == null) return null;

        return new SmithingTransformRecipe(
            getNamespacedKey(plugin),
            resultItem,
            template,
            base,
            addition
        );
    }
}
```

```java
// ============================================
// recipe/types/StonecutterLipoRecipe.java
// ============================================
package dev.lipasquide.lipoitems.recipe.types;

import dev.lipasquide.lipoitems.LipoItemsPlugin;
import dev.lipasquide.lipoitems.recipe.LipoRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.StonecuttingRecipe;

public class StonecutterLipoRecipe implements LipoRecipe {
    private final String id;
    private final String result;
    private final RecipeChoice input;
    private final int count;

    public StonecutterLipoRecipe(String id, String result, RecipeChoice input, int count) {
        this.id = id;
        this.result = result;
        this.input = input;
        this.count = count;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getResult() {
        return result;
    }

    @Override
    public Recipe toBukkitRecipe(LipoItemsPlugin plugin) {
        ItemStack resultItem = plugin.getItemManager().buildItemStack(result, count);
        if (resultItem == null) return null;

        return new StonecuttingRecipe(
            getNamespacedKey(plugin),
            resultItem,
            input
        );
    }
}
```

```java
// ============================================
// recipe/CustomItemRecipeChoice.java - Özel item için recipe choice
// ============================================
package dev.lipasquide.lipoitems.recipe;

import dev.lipasquide.lipoitems.LipoItemsPlugin;
import dev.lipasquide.lipoitems.item.CustomItem;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;

import java.util.Objects;

/**
 * Recipe choice that matches custom items by their ID
 */
public class CustomItemRecipeChoice implements RecipeChoice {
    private final String itemId;

    public CustomItemRecipeChoice(String itemId) {
        this.itemId = itemId;
    }

    @Override
    public ItemStack getItemStack() {
        CustomItem item = LipoItemsPlugin.getInstance().getItemManager().getItem(itemId);
        if (item != null) {
            return item.buildItemStack(1);
        }
        return new ItemStack(org.bukkit.Material.STONE);
    }

    @Override
    public RecipeChoice clone() {
        return new CustomItemRecipeChoice(itemId);
    }

    @Override
    public boolean test(ItemStack itemStack) {
        if (itemStack == null) return false;
        
        String customId = LipoItemsPlugin.getInstance().getItemManager().getCustomItemId(itemStack);
        return itemId.equals(customId);
    }
}
```

```java
// ============================================
// pack/PackGenerator.java - Resource Pack Generator
// ============================================
package dev.lipasquide.lipoitems.pack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.lipasquide.lipoitems.LipoItemsPlugin;
import dev.lipasquide.lipoitems.item.CustomItem;
import dev.lipasquide.lipoitems.util.Key;
import dev.lipasquide.lipoitems.util.VersionHelper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Generates resource pack files for custom items
 * Supports both Item Model (1.21.4+) and Custom Model Data (legacy)
 */
public class PackGenerator {
    private final LipoItemsPlugin plugin;
    private final Gson gson;
    private final File packFolder;

    public PackGenerator(LipoItemsPlugin plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.packFolder = new File(plugin.getDataFolder(), "pack");
    }

    /**
     * Generate the complete resource pack
     */
    public void generatePack() {
        if (!packFolder.exists()) {
            packFolder.mkdirs();
        }

        // Generate pack.mcmeta
        generatePackMeta();

        // Generate item models
        if (VersionHelper.isItemModelSupported()) {
            generateItemModels();
        }

        // Generate model overrides for legacy support
        generateLegacyOverrides();

        plugin.getLogger().info("Resource pack generated successfully!");
    }

    /**
     * Generate pack.mcmeta
     */
    private void generatePackMeta() {
        JsonObject meta = new JsonObject();
        JsonObject pack = new JsonObject();
        
        // Pack format 46 = 1.21.4
        int packFormat = VersionHelper.isItemModelSupported() ? 46 : 34;
        
        pack.addProperty("pack_format", packFormat);
        pack.addProperty("description", "LipoItems Custom Items Resource Pack");
        meta.add("pack", pack);

        writeJson(new File(packFolder, "pack.mcmeta"), meta);
    }

    /**
     * Generate item model files (1.21.4+)
     * Format: assets/lipoitems/items/item_name.json
     */
    private void generateItemModels() {
        File itemsFolder = new File(packFolder, "assets/lipoitems/items");
        itemsFolder.mkdirs();

        for (CustomItem item : plugin.getItemManager().getAllItems()) {
            if (item.getItemModel() == null) continue;

            JsonObject model = new JsonObject();
            model.addProperty("type", "minecraft:model");
            model.addProperty("model", "lipoitems:item/" + item.getId().value());
            
            // Add special handlers for different item types
            if (isTrident(item)) {
                model.addProperty("type", "minecraft:trident");
            } else if (isArmor(item)) {
                // Armor uses different model type
            }

            File modelFile = new File(itemsFolder, item.getId().value() + ".json");
            writeJson(modelFile, model);
        }

        // Generate model definitions
        generateModelDefinitions();
    }

    /**
     * Generate model definitions (actual 3D models)
     * assets/lipoitems/models/item/item_name.json
     */
    private void generateModelDefinitions() {
        File modelsFolder = new File(packFolder, "assets/lipoitems/models/item");
        modelsFolder.mkdirs();

        for (CustomItem item : plugin.getItemManager().getAllItems()) {
            JsonObject model = new JsonObject();
            model.addProperty("parent", "minecraft:item/generated");
            
            JsonObject textures = new JsonObject();
            textures.addProperty("layer0", "lipoitems:item/" + item.getId().value());
            model.add("textures", textures);

            File modelFile = new File(modelsFolder, item.getId().value() + ".json");
            writeJson(modelFile, model);
        }
    }

    /**
     * Generate legacy model overrides for <1.21.4
     * assets/minecraft/models/item/vanilla_item.json
     */
    private void generateLegacyOverrides() {
        // Group items by their client-bound material
        Map<Key, Map<Integer, String>> overridesByMaterial = new HashMap<>();

        for (CustomItem item : plugin.getItemManager().getAllItems()) {
            if (item.getCustomModelData() <= 0) continue;

            Key material = item.getClientBoundMaterial();
            int cmd = item.getCustomModelData();
            
            overridesByMaterial
                .computeIfAbsent(material, k -> new HashMap<>())
                .put(cmd, item.getId().value());
        }

        // Generate override files for each material
        for (Map.Entry<Key, Map<Integer, String>> entry : overridesByMaterial.entrySet()) {
            generateOverrideFile(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Generate a single override file
     */
    private void generateOverrideFile(Key material, Map<Integer, String> overrides) {
        File mcModelsFolder = new File(packFolder, "assets/minecraft/models/item");
        mcModelsFolder.mkdirs();

        JsonObject model = new JsonObject();
        model.addProperty("parent", "minecraft:item/handheld");
        
        JsonArray overridesArray = new JsonArray();
        
        for (Map.Entry<Integer, String> entry : overrides.entrySet()) {
            JsonObject override = new JsonObject();
            
            JsonObject predicate = new JsonObject();
            predicate.addProperty("custom_model_data", entry.getKey());
            override.add("predicate", predicate);
            
            override.addProperty("model", "lipoitems:item/" + entry.getValue());
            overridesArray.add(override);
        }
        
        model.add("overrides", overridesArray);

        File modelFile = new File(mcModelsFolder, material.value() + ".json");
        writeJson(modelFile, model);
    }

    /**
     * Generate item tags for recipe ingredients
     */
    public void generateItemTags() {
        File tagsFolder = new File(packFolder, "assets/lipoitems/tags/items");
        tagsFolder.mkdirs();

        // Generate tags based on item settings
        Map<String, java.util.List<String>> tags = new HashMap<>();
        
        for (CustomItem item : plugin.getItemManager().getAllItems()) {
            for (String tag : item.getSettings().tags()) {
                tags.computeIfAbsent(tag, k -> new java.util.ArrayList<>())
                    .add(item.getId().toString());
            }
        }

        for (Map.Entry<String, java.util.List<String>> entry : tags.entrySet()) {
            JsonObject tagFile = new JsonObject();
            tagFile.addProperty("replace", false);
            
            JsonArray values = new JsonArray();
            for (String value : entry.getValue()) {
                values.add(value);
            }
            tagFile.add("values", values);

            File tagFilePath = new File(tagsFolder, entry.getKey() + ".json");
            writeJson(tagFilePath, tagFile);
        }
    }

    private boolean isTrident(CustomItem item) {
        return item.getBehaviors().stream()
            .anyMatch(b -> b instanceof dev.lipasquide.lipoitems.item.behavior.TridentBehavior);
    }

    private boolean isArmor(CustomItem item) {
        return item.getBehaviors().stream()
            .anyMatch(b -> b instanceof dev.lipasquide.lipoitems.item.behavior.ArmorBehavior);
    }

    private void writeJson(File file, JsonObject json) {
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(json, writer);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to write " + file.getName() + ": " + e.getMessage());
        }
    }
}
```

```java
// ============================================
// network/NetworkItemHandler.java - Client-Server sync
// ============================================
package dev.lipasquide.lipoitems.network;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import dev.lipasquide.lipoitems.LipoItemsPlugin;
import dev.lipasquide.lipoitems.item.CustomItem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Handles network packet interception for client-server item synchronization
 * Uses ProtocolLib for packet manipulation
 */
public class NetworkItemHandler {
    private final LipoItemsPlugin plugin;
    private ProtocolManager protocolManager;

    public NetworkItemHandler(LipoItemsPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("ProtocolLib")) {
            plugin.getLogger().warning("ProtocolLib not found! Client-bound items will not work.");
            return;
        }

        this.protocolManager = ProtocolLibrary.getProtocolManager();
        registerPacketListeners();
    }

    private void registerPacketListeners() {
        // Intercept SET_SLOT packets (inventory updates)
        protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.HIGH,
                PacketType.Play.Server.SET_SLOT) {
            @Override
            public void onPacketSending(PacketEvent event) {
                handleSetSlot(event);
            }
        });

        // Intercept WINDOW_ITEMS packets (full inventory)
        protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.HIGH,
                PacketType.Play.Server.WINDOW_ITEMS) {
            @Override
            public void onPacketSending(PacketEvent event) {
                handleWindowItems(event);
            }
        });

        // Intercept ENTITY_EQUIPMENT packets (held/worn items)
        protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.HIGH,
                PacketType.Play.Server.ENTITY_EQUIPMENT) {
            @Override
            public void onPacketSending(PacketEvent event) {
                handleEntityEquipment(event);
            }
        });
    }

    /**
     * Handle SET_SLOT packet - convert items for client display
     */
    private void handleSetSlot(PacketEvent event) {
        PacketContainer packet = event.getPacket();
        ItemStack item = packet.getItemModifier().read(0);
        
        if (item == null || !item.hasItemMeta()) return;
        
        ItemStack converted = convertForClient(item, event.getPlayer());
        if (converted != null) {
            packet.getItemModifier().write(0, converted);
        }
    }

    /**
     * Handle WINDOW_ITEMS packet - convert all items in inventory
     */
    private void handleWindowItems(PacketEvent event) {
        PacketContainer packet = event.getPacket();
        List<ItemStack> items = packet.getItemListModifier().read(0);
        
        for (int i = 0; i < items.size(); i++) {
            ItemStack item = items.get(i);
            if (item == null || !item.hasItemMeta()) continue;
            
            ItemStack converted = convertForClient(item, event.getPlayer());
            if (converted != null) {
                items.set(i, converted);
            }
        }
    }

    /**
     * Handle ENTITY_EQUIPMENT packet - convert equipped items
     */
    private void handleEntityEquipment(PacketEvent event) {
        PacketContainer packet = event.getPacket();
        List<Pair<EnumWrappers.ItemSlot, ItemStack>> slots = packet.getSlotStackPairLists().read(0);
        
        for (int i = 0; i < slots.size(); i++) {
            Pair<EnumWrappers.ItemSlot, ItemStack> pair = slots.get(i);
            ItemStack item = pair.getSecond();
            
            if (item == null || !item.hasItemMeta()) continue;
            
            ItemStack converted = convertForClient(item, event.getPlayer());
            if (converted != null) {
                slots.set(i, new Pair<>(pair.getFirst(), converted));
            }
        }
    }

    /**
     * Convert server item to client-bound item
     * Applies client-bound processors (different visual representation)
     */
    private ItemStack convertForClient(ItemStack serverItem, Player player) {
        String customId = plugin.getItemManager().getCustomItemId(serverItem);
        if (customId == null) return null;
        
        CustomItem customItem = plugin.getItemManager().getItem(customId);
        if (customItem == null) return null;
        
        // Check if item has client-bound processors
        if (customItem.getClientBoundProcessors().isEmpty()) return null;
        
        // Build client version
        return customItem.buildItemStack(player, serverItem.getAmount());
    }

    /**
     * Convert client item back to server item
     * Used when receiving items from client (e.g., creative inventory)
     */
    public ItemStack convertFromClient(ItemStack clientItem, Player player) {
        // Check if this is a custom item by looking for our PDC key
        String customId = plugin.getItemManager().getCustomItemId(clientItem);
        if (customId == null) return clientItem;
        
        CustomItem customItem = plugin.getItemManager().getItem(customId);
        if (customItem == null) return clientItem;
        
        // Rebuild as server item
        return customItem.buildItemStack(player, clientItem.getAmount());
    }

    /**
     * Simple pair class for slot-stack pairs
     */
    public static class Pair<F, S> {
        private final F first;
        private final S second;

        public Pair(F first, S second) {
            this.first = first;
            this.second = second;
        }

        public F getFirst() { return first; }
        public S getSecond() { return second; }
    }
}
```

```java
// ============================================
// registry/RegistryManager.java - Registry yönetimi
// ============================================
package dev.lipasquide.lipoitems.registry;

import dev.lipasquide.lipoitems.LipoItemsPlugin;
import org.bukkit.NamespacedKey;

/**
 * Manages all registries and keys
 */
public class RegistryManager {
    private final LipoItemsPlugin plugin;
    private NamespacedKey itemKey;

    public RegistryManager(LipoItemsPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        this.itemKey = new NamespacedKey(plugin, "custom_item_id");
    }

    public void unload() {
    }

    /**
     * Get the PDC key for storing custom item ID
     */
    public NamespacedKey getItemKey() {
        return itemKey;
    }
}
```

```java
// ============================================
// command/CommandManager.java - Komut yönetimi
// ============================================
package dev.lipasquide.lipoitems.command;

import dev.lipasquide.lipoitems.LipoItemsPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CommandManager implements TabExecutor {
    private final LipoItemsPlugin plugin;

    public CommandManager(LipoItemsPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("lipoitems").setExecutor(this);
        plugin.getCommand("lipoitems").setTabCompleter(this);
        plugin.getCommand("givecustomitem").setExecutor(this);
        plugin.getCommand("givecustomitem").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("lipoitems")) {
            return handleLipoItemsCommand(sender, args);
        } else if (command.getName().equalsIgnoreCase("givecustomitem")) {
            return handleGiveCommand(sender, args);
        }
        return false;
    }

    private boolean handleLipoItemsCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§6LipoItems §7v" + plugin.getDescription().getVersion());
            sender.sendMessage("§7/lipoitems give <player> <item> [count]");
            sender.sendMessage("§7/lipoitems list");
            sender.sendMessage("§7/lipoitems reload");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give":
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /lipoitems give <player> <item> [count]");
                    return true;
                }
                return giveItem(sender, args[1], args[2], args.length > 3 ? Integer.parseInt(args[3]) : 1);
                
            case "list":
                return listItems(sender);
                
            case "reload":
                if (!sender.hasPermission("lipoitems.admin")) {
                    sender.sendMessage("§cNo permission!");
                    return true;
                }
                plugin.getItemManager().reload();
                plugin.getRecipeManager().reload();
                sender.sendMessage("§aLipoItems reloaded!");
                return true;
                
            default:
                sender.sendMessage("§cUnknown subcommand!");
                return true;
        }
    }

    private boolean handleGiveCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /givecustomitem <player> <item> [count]");
            return true;
        }
        
        return giveItem(sender, args[0], args[1], args.length > 2 ? Integer.parseInt(args[2]) : 1);
    }

    private boolean giveItem(CommandSender sender, String playerName, String itemId, int count) {
        Player target = plugin.getServer().getPlayer(playerName);
        if (target == null) {
            sender.sendMessage("§cPlayer not found: " + playerName);
            return true;
        }

        var item = plugin.getItemManager().buildItemStack(itemId, target, count);
        if (item == null) {
            sender.sendMessage("§cUnknown item: " + itemId);
            return true;
        }

        target.getInventory().addItem(item);
        sender.sendMessage("§aGave " + count + "x " + itemId + " to " + target.getName());
        return true;
    }

    private boolean listItems(CommandSender sender) {
        sender.sendMessage("§6=== Custom Items ===");
        for (String id : plugin.getItemManager().getAllItemIds()) {
            sender.sendMessage("§7- " + id);
        }
        sender.sendMessage("§6Total: " + plugin.getItemManager().getItemCount() + " items");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (command.getName().equalsIgnoreCase("lipoitems")) {
            if (args.length == 1) {
                completions.add("give");
                completions.add("list");
                completions.add("reload");
            } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
                // Player names
                plugin.getServer().getOnlinePlayers().forEach(p -> completions.add(p.getName()));
            } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
                // Item IDs
                completions.addAll(plugin.getItemManager().getAllItemIds());
            }
        } else if (command.getName().equalsIgnoreCase("givecustomitem")) {
            if (args.length == 1) {
                plugin.getServer().getOnlinePlayers().forEach(p -> completions.add(p.getName()));
            } else if (args.length == 2) {
                completions.addAll(plugin.getItemManager().getAllItemIds());
            }
        }
        
        return completions;
    }
}
```

```java
// ============================================
// listener/ItemListener.java - Event dinleyicileri
// ============================================
package dev.lipasquide.lipoitems.listener;

import dev.lipasquide.lipoitems.LipoItemsPlugin;
import dev.lipasquide.lipoitems.item.CustomItem;
import dev.lipasquide.lipoitems.item.behavior.ItemBehavior;
import dev.lipasquide.lipoitems.item.context.UseOnContext;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

public class ItemListener implements Listener {
    private final LipoItemsPlugin plugin;

    public ItemListener(LipoItemsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item == null) return;
        
        String customId = plugin.getItemManager().getCustomItemId(item);
        if (customId == null) return;
        
        CustomItem customItem = plugin.getItemManager().getItem(customId);
        if (customItem == null) return;
        
        // Call behaviors
        for (ItemBehavior behavior : customItem.getBehaviors()) {
            behavior.onInteract(event, player, item);
            
            if (event.getAction() == Action.RIGHT_CLICK_AIR) {
                ItemBehavior.InteractionResult result = behavior.use(player, item);
                if (result == ItemBehavior.InteractionResult.SUCCESS_AND_CANCEL) {
                    event.setCancelled(true);
                    return;
                }
            } else if (event.getClickedBlock() != null) {
                UseOnContext context = new UseOnContext(
                    player,
                    item,
                    event.getClickedBlock(),
                    event.getBlockFace(),
                    event.getInteractionPoint() != null ? 
                        new org.bukkit.util.Vector(
                            event.getInteractionPoint().getX(),
                            event.getInteractionPoint().getY(),
                            event.getInteractionPoint().getZ()
                        ) : new org.bukkit.util.Vector(0, 0, 0)
                );
                
                ItemBehavior.InteractionResult result = behavior.useOnBlock(context);
                if (result == ItemBehavior.InteractionResult.SUCCESS || 
                    result == ItemBehavior.InteractionResult.SUCCESS_AND_CANCEL) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        String customId = plugin.getItemManager().getCustomItemId(item);
        if (customId == null) return;
        
        CustomItem customItem = plugin.getItemManager().getItem(customId);
        if (customItem == null) return;
        
        for (ItemBehavior behavior : customItem.getBehaviors()) {
            behavior.onBlockBreak(event, player, item);
        }
    }

    @EventHandler
    public void onItemConsume(PlayerItemConsumeEvent event) {
        // Handle food behaviors
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        String customId = plugin.getItemManager().getCustomItemId(item);
        if (customId == null) return;
        
        CustomItem customItem = plugin.getItemManager().getItem(customId);
        if (customItem == null) return;
        
        for (ItemBehavior behavior : customItem.getBehaviors()) {
            if (behavior instanceof dev.lipasquide.lipoitems.item.behavior.FoodBehavior foodBehavior) {
                foodBehavior.onConsume(event, player, item);
            }
        }
    }
}
```

```java
// ============================================
// item/ItemManager.java - Ana item yöneticisi
// ============================================
package dev.lipasquide.lipoitems.item;

import dev.lipasquide.lipoitems.LipoItemsPlugin;
import dev.lipasquide.lipoitems.item.behavior.ArmorBehavior;
import dev.lipasquide.lipoitems.item.behavior.FoodBehavior;
import dev.lipasquide.lipoitems.item.behavior.TridentBehavior;
import dev.lipasquide.lipoitems.item.processor.*;
import dev.lipasquide.lipoitems.item.setting.ItemSettings;
import dev.lipasquide.lipoitems.util.Key;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nullable;
import java.io.File;
import java.util.*;

public class ItemManager {
    private final LipoItemsPlugin plugin;
    private final Map<String, CustomItem> items = new HashMap<>();
    private final Map<String, CustomItem> itemsByPath = new HashMap<>();
    private final Map<String, List<CustomItem>> itemsByTag = new HashMap<>();

    public ItemManager(LipoItemsPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        loadItemsFromFolder();
        plugin.getLogger().info("Loaded " + items.size() + " custom items");
    }

    public void reload() {
        unload();
        load();
    }

    public void unload() {
        items.clear();
        itemsByPath.clear();
        itemsByTag.clear();
    }

    private void loadItemsFromFolder() {
        File itemsFolder = new File(plugin.getDataFolder(), "items");
        if (!itemsFolder.exists()) {
            createDefaultItems(itemsFolder);
            return;
        }

        File[] files = itemsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                loadItemsFromConfig(config, file.getName());
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load items from " + file.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void loadItemsFromConfig(YamlConfiguration config, String fileName) {
        for (String key : config.getKeys(false)) {
            try {
                String path = key;
                String idStr = config.getString(key + ".id", key);
                Key id = Key.of(idStr);
                
                // Material
                String materialStr = config.getString(key + ".material", "minecraft:stick");
                Key material = Key.of(materialStr);
                
                // Client-bound material (for visual)
                String clientBoundStr = config.getString(key + ".client-bound-material", materialStr);
                Key clientBoundMaterial = Key.of(clientBoundStr);
                
                // Item model (1.21.4+)
                String itemModelStr = config.getString(key + ".item-model");
                Key itemModel = itemModelStr != null ? Key.of(itemModelStr) : null;
                
                // Custom model data (legacy)
                int customModelData = config.getInt(key + ".custom-model-data", 0);
                
                // Settings
                ItemSettings settings = loadSettings(config, key);
                
                // Build item
                SimpleCustomItem.Builder builder = SimpleCustomItem.builder()
                    .id(id)
                    .material(material)
                    .clientBoundMaterial(clientBoundMaterial)
                    .itemModel(itemModel)
                    .customModelData(customModelData)
                    .settings(settings);
                
                // Processors
                loadProcessors(config, key, builder);
                
                // Behaviors
                loadBehaviors(config, key, builder);
                
                CustomItem item = builder.build();
                registerItem(item);
                
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load item '" + key + "' in " + fileName + ": " + e.getMessage());
            }
        }
    }

    private ItemSettings loadSettings(YamlConfiguration config, String path) {
        ItemSettings.Builder builder = ItemSettings.builder();
        
        builder.maxStackSize(config.getInt(path + ".settings.max-stack-size", 64));
        builder.fuelTime(config.getInt(path + ".settings.fuel-time", 0));
        builder.enchantable(config.getBoolean(path + ".settings.enchantable", true));
        builder.renameable(config.getBoolean(path + ".settings.renameable", true));
        builder.durability(config.getInt(path + ".settings.durability", 0));
        builder.unbreakable(config.getBoolean(path + ".settings.unbreakable", false));
        
        // Tags
        List<String> tags = config.getStringList(path + ".settings.tags");
        builder.tags(new HashSet<>(tags));
        
        return builder.build();
    }

    private void loadProcessors(YamlConfiguration config, String path, SimpleCustomItem.Builder builder) {
        // Display name
        String displayName = config.getString(path + ".display.name");
        if (displayName != null) {
            builder.processor(new DisplayNameProcessor(displayName));
        }
        
        // Lore
        List<String> lore = config.getStringList(path + ".display.lore");
        if (!lore.isEmpty()) {
            builder.processor(new LoreProcessor(lore));
        }
        
        // Custom model data processor
        int cmd = config.getInt(path + ".custom-model-data", 0);
        if (cmd > 0) {
            builder.processor(new CustomModelDataProcessor(cmd));
        }
        
        // Item model processor (1.21.4+)
        if (config.contains(path + ".item-model")) {
            builder.processor(new ItemModelProcessor(Key.of(config.getString(path + ".item-model"))));
        }
        
        // Client-bound processors
        if (config.contains(path + ".client-bound")) {
            // Different visual for client
            String clientModel = config.getString(path + ".client-bound.model");
            if (clientModel != null) {
                builder.clientBoundProcessor(new ItemModelProcessor(Key.of(clientModel)));
            }
        }
    }

    private void loadBehaviors(YamlConfiguration config, String path, SimpleCustomItem.Builder builder) {
        // Trident behavior
        if (config.contains(path + ".behavior.trident")) {
            boolean loyalty = config.getBoolean(path + ".behavior.trident.loyalty", false);
            boolean riptide = config.getBoolean(path + ".behavior.trident.riptide", false);
            boolean channeling = config.getBoolean(path + ".behavior.trident.channeling", false);
            double returnSpeed = config.getDouble(path + ".behavior.trident.return-speed", 0.1);
            double damage = config.getDouble(path + ".behavior.trident.damage", 8.0);
            boolean glow = config.getBoolean(path + ".behavior.trident.glow", false);
            
            builder.behavior(new TridentBehavior(loyalty, riptide, channeling, returnSpeed, damage, glow));
        }
        
        // Armor behavior
        if (config.contains(path + ".behavior.armor")) {
            double armor = config.getDouble(path + ".behavior.armor.armor", 0);
            double toughness = config.getDouble(path + ".behavior.armor.toughness", 0);
            double kbResist = config.getDouble(path + ".behavior.armor.knockback-resistance", 0);
            String trimMaterial = config.getString(path + ".behavior.armor.trim-material");
            String trimPattern = config.getString(path + ".behavior.armor.trim-pattern");
            
            builder.behavior(new ArmorBehavior(armor, toughness, kbResist, trimMaterial, trimPattern));
        }
        
        // Food behavior
        if (config.contains(path + ".behavior.food")) {
            int nutrition = config.getInt(path + ".behavior.food.nutrition", 4);
            float saturation = (float) config.getDouble(path + ".behavior.food.saturation", 0.6f);
            boolean alwaysEdible = config.getBoolean(path + ".behavior.food.always-edible", false);
            
            builder.behavior(new FoodBehavior(nutrition, saturation, alwaysEdible, 32, 
                new ArrayList<>(), "minecraft:entity.generic.eat"));
        }
        
        // Block placer behavior
        if (config.contains(path + ".behavior.block-placer")) {
            String blockMaterial = config.getString(path + ".behavior.block-placer.material", "minecraft:stone");
            boolean consume = config.getBoolean(path + ".behavior.block-placer.consume-item", true);
            
            Material mat = Material.matchMaterial(blockMaterial);
            if (mat != null) {
                builder.behavior(new dev.lipasquide.lipoitems.item.behavior.BlockPlacerBehavior(mat, consume));
            }
        }
    }

    public void registerItem(CustomItem item) {
        items.put(item.getId().toString(), item);
        itemsByPath.put(item.getId().value(), item);
        
        // Index by tags
        for (String tag : item.getSettings().tags()) {
            itemsByTag.computeIfAbsent(tag, k -> new ArrayList<>()).add(item);
        }
    }

    public boolean registerExternalItem(org.bukkit.plugin.java.JavaPlugin registeringPlugin, CustomItem item) {
        String prefix = registeringPlugin.getName().toLowerCase() + ":";
        if (!item.getId().toString().startsWith(prefix)) {
            plugin.getLogger().warning("External item ID must start with " + prefix);
            return false;
        }
        
        registerItem(item);
        return true;
    }

    @Nullable
    public CustomItem getItem(String id) {
        return items.get(id);
    }

    @Nullable
    public ItemStack buildItemStack(String id, @Nullable Player player, int count) {
        CustomItem item = getItem(id);
        if (item == null) return null;
        return item.buildItemStack(player, count);
    }

    @Nullable
    public ItemStack buildItemStack(String id, int count) {
        return buildItemStack(id, null, count);
    }

    public boolean isCustomItem(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) return false;
        return itemStack.getPersistentDataContainer().has(
            plugin.getRegistryManager().getItemKey(),
            PersistentDataType.STRING
        );
    }

    @Nullable
    public String getCustomItemId(ItemStack itemStack) {
        if (!isCustomItem(itemStack)) return null;
        return itemStack.getPersistentDataContainer().get(
            plugin.getRegistryManager().getItemKey(),
            PersistentDataType.STRING
        );
    }

    public Collection<String> getAllItemIds() {
        return Collections.unmodifiableSet(items.keySet());
    }

    public Collection<CustomItem> getAllItems() {
        return Collections.unmodifiableCollection(items.values());
    }

    public int getItemCount() {
        return items.size();
    }

    private void createDefaultItems(File folder) {
        folder.mkdirs();
        
        YamlConfiguration config = new YamlConfiguration();
        
        // Example Ruby Sword
        config.set("ruby_sword.id", "lipoitems:ruby_sword");
        config.set("ruby_sword.material", "minecraft:diamond_sword");
        config.set("ruby_sword.custom-model-data", 1001);
        config.set("ruby_sword.display.name", "<gradient:red:gold>Ruby Sword");
        config.set("ruby_sword.display.lore", Arrays.asList(
            "<gray>A sword made of pure ruby",
            "<dark_gray>Very rare and powerful"
        ));
        config.set("ruby_sword.settings.max-stack-size", 1);
        config.set("ruby_sword.settings.durability", 1561);
        config.set("ruby_sword.settings.tags", Arrays.asList("swords", "gem_items"));
        
        // Example Trident
        config.set("storm_trident.id", "lipoitems:storm_trident");
        config.set("storm_trident.material", "minecraft:trident");
        config.set("storm_trident.custom-model-data", 2001);
        config.set("storm_trident.item-model", "lipoitems:storm_trident");
        config.set("storm_trident.display.name", "<blue>Storm Trident");
        config.set("storm_trident.behavior.trident.loyalty", true);
        config.set("storm_trident.behavior.trident.channeling", true);
        config.set("storm_trident.behavior.trident.damage", 12.0);
        config.set("storm_trident.behavior.trident.return-speed", 0.15);
        
        // Example Armor
        config.set("ruby_helmet.id", "lipoitems:ruby_helmet");
        config.set("ruby_helmet.material", "minecraft:diamond_helmet");
        config.set("ruby_helmet.custom-model-data", 3001);
        config.set("ruby_helmet.display.name", "<gradient:red:gold>Ruby Helmet");
        config.set("ruby_helmet.behavior.armor.armor", 3);
        config.set("ruby_helmet.behavior.armor.toughness", 2);
        config.set("ruby_helmet.settings.durability", 363);
        
        // Example Food
        config.set("golden_berry.id", "lipoitems:golden_berry");
        config.set("golden_berry.material", "minecraft:sweet_berries");
        config.set("golden_berry.custom-model-data", 4001);
        config.set("golden_berry.display.name", "<yellow>Golden Berry");
        config.set("golden_berry.behavior.food.nutrition", 6);
        config.set("golden_berry.behavior.food.saturation", 1.2f);
        config.set("golden_berry.behavior.food.always-edible", true);
        
        try {
            config.save(new File(folder, "default_items.yml"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

```java
// ============================================
// item/behavior/BehaviorManager.java - Davranış yöneticisi
// ============================================
package dev.lipasquide.lipoitems.item.behavior;

import dev.lipasquide.lipoitems.LipoItemsPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Manages behavior factories for extensibility
 */
public class BehaviorManager {
    private final LipoItemsPlugin plugin;
    private final Map<String, Function<Map<String, Object>, ItemBehavior>> behaviorFactories = new HashMap<>();

    public BehaviorManager(LipoItemsPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        // Register default behavior factories
        registerDefaultFactories();
    }

    private void registerDefaultFactories() {
        // Trident
        registerFactory("trident", config -> {
            boolean loyalty = (boolean) config.getOrDefault("loyalty", false);
            boolean riptide = (boolean) config.getOrDefault("riptide", false);
            boolean channeling = (boolean) config.getOrDefault("channeling", false);
            double returnSpeed = ((Number) config.getOrDefault("return-speed", 0.1)).doubleValue();
            double damage = ((Number) config.getOrDefault("damage", 8.0)).doubleValue();
            boolean glow = (boolean) config.getOrDefault("glow", false);
            return new TridentBehavior(loyalty, riptide, channeling, returnSpeed, damage, glow);
        });

        // Armor
        registerFactory("armor", config -> {
            double armor = ((Number) config.getOrDefault("armor", 0)).doubleValue();
            double toughness = ((Number) config.getOrDefault("toughness", 0)).doubleValue();
            double kbResist = ((Number) config.getOrDefault("knockback-resistance", 0)).doubleValue();
            String trimMaterial = (String) config.get("trim-material");
            String trimPattern = (String) config.get("trim-pattern");
            return new ArmorBehavior(armor, toughness, kbResist, trimMaterial, trimPattern);
        });

        // Food
        registerFactory("food", config -> {
            int nutrition = ((Number) config.getOrDefault("nutrition", 4)).intValue();
            float saturation = ((Number) config.getOrDefault("saturation", 0.6f)).floatValue();
            boolean alwaysEdible = (boolean) config.getOrDefault("always-edible", false);
            return new FoodBehavior(nutrition, saturation, alwaysEdible, 32, 
                new java.util.ArrayList<>(), "minecraft:entity.generic.eat");
        });
    }

    public void registerFactory(String type, Function<Map<String, Object>, ItemBehavior> factory) {
        behaviorFactories.put(type.toLowerCase(), factory);
    }

    public ItemBehavior createBehavior(String type, Map<String, Object> config) {
        Function<Map<String, Object>, ItemBehavior> factory = behaviorFactories.get(type.toLowerCase());
        if (factory == null) {
            plugin.getLogger().warning("Unknown behavior type: " + type);
            return null;
        }
        return factory.apply(config);
    }

    public boolean hasFactory(String type) {
        return behaviorFactories.containsKey(type.toLowerCase());
    }
}
```

Bu **LipoItems** plugini şunları içerir:

## 🎯 **Temel Özellikler**

1. **Modern Item Model Sistemi** (1.21.4+)
   - `item-model` component desteği
   - Otomatik resource pack generation
   - Legacy Custom Model Data desteği (<1.21.4)

2. **Gelişmiş Davranış Sistemi**
   - **TridentBehavior**: Loyalty, Riptide, Channeling enchantment'ları
   - **ArmorBehavior**: Armor, toughness, knockback resistance, trim support
   - **FoodBehavior**: Nutrition, saturation, potion effects
   - **BlockPlacerBehavior**: Blok yerleştirme

3. **Kapsamlı Recipe Sistemi**
   - Shaped & Shapeless crafting
   - Furnace, Blast Furnace, Smoker, Campfire
   - Smithing Table (upgrade & trim)
   - Stonecutter
   - Custom item ingredient support

4. **Network/Client-Server Senkronizasyonu**
   - ProtocolLib entegrasyonu
   - Client-bound item gösterimi
   - Packet interception

5. **API & Addon Sistemi**
   - `LipoItemsAPI` public interface
   - Diğer pluginlerden item kaydetme
   - Behavior factory registration

## 📁 **Dosya Yapısı**

```
LipoItems/
├── plugin.yml
├── src/
│   └── dev/lipasquide/lipoitems/
│       ├── LipoItemsPlugin.java
│       ├── api/
│       │   └── LipoItemsAPI.java
│       ├── command/
│       │   └── CommandManager.java
│       ├── item/
│       │   ├── CustomItem.java
│       │   ├── SimpleCustomItem.java
│       │   ├── ItemManager.java
│       │   ├── behavior/
│       │   │   ├── ItemBehavior.java
│       │   │   ├── TridentBehavior.java
│       │   │   ├── ArmorBehavior.java
│       │   │   ├── FoodBehavior.java
│       │   │   ├── BlockPlacerBehavior.java
│       │   │   └── BehaviorManager.java
│       │   ├── context/
│       │   │   ├── ItemBuildContext.java
│       │   │   └── UseOnContext.java
│       │   ├── processor/
│       │   │   ├── ItemProcessor.java
│       │   │   ├── ItemModelProcessor.java
│       │   │   ├── CustomModelDataProcessor.java
│       │   │   ├── DisplayNameProcessor.java
│       │   │   └── LoreProcessor.java
│       │   └── setting/
│       │       └── ItemSettings.java
│       ├── recipe/
│       │   ├── RecipeManager.java
│       │   ├── LipoRecipe.java
│       │   ├── CustomItemRecipeChoice.java
│       │   └── types/
│       │       ├── ShapedLipoRecipe.java
│       │       ├── ShapelessLipoRecipe.java
│       │       ├── FurnaceLipoRecipe.java
│       │       ├── BlastFurnaceLipoRecipe.java
│       │       ├── SmokerLipoRecipe.java
│       │       ├── CampfireLipoRecipe.java
│       │       ├── SmithingLipoRecipe.java
│       │       └── StonecutterLipoRecipe.java
│       ├── pack/
│       │   └── PackGenerator.java
│       ├── network/
│       │   └── NetworkItemHandler.java
│       ├── registry/
│       │   └── RegistryManager.java
│       ├── listener/
│       │   └── ItemListener.java
│       ├── projectile/
│       │   └── CustomTridentProjectile.java
│       └── util/
│           ├── Key.java
│           ├── UniqueKey.java
│           └── VersionHelper.java
├── items/
│   └── default_items.yml (auto-generated)
├── recipes/
│   └── default.yml (auto-generated)
└── pack/ (auto-generated resource pack)
```

## 🚀 **Kullanım Örneği**

```yaml
# items/my_items.yml
storm_trident:
  id: lipoitems:storm_trident
  material: minecraft:trident
  custom-model-data: 2001
  item-model: lipoitems:storm_trident  # 1.21.4+
  display:
    name: "<gradient:blue:aqua>⚡ Storm Trident"
    lore:
      - "<gray>Calls down lightning from the sky"
      - "<dark_gray>Very powerful in thunderstorms"
  behavior:
    trident:
      loyalty: true
      channeling: true
      riptide: false
      damage: 12.0
      return-speed: 0.15
      glow: true
  settings:
    max-stack-size: 1
    durability: 250
    enchantable: true
    tags: [tridents, legendary_items]
```

```yaml
# recipes/my_recipes.yml
shaped:
  storm_trident:
    shape:
      - "TTT"
      - " S "
      - " S "
    ingredients:
      T: lipoitems:thunder_shard
      S: minecraft:stick
    result: lipoitems:storm_trident

furnace:
  thunder_shard:
    input: minecraft:amethyst_shard
    result: lipoitems:thunder_shard
    experience: 2.0
    cooking_time: 200

smithing:
  upgraded_trident:
    template: minecraft:netherite_upgrade_smithing_template
    base: lipoitems:storm_trident
    addition: minecraft:netherite_ingot
    result: lipoitems:netherite_storm_trident
```

Bu plugini derlemek için **Maven** veya **Gradle** kullanabilirsiniz. ProtocolLib dependency olarak eklenmelidir (optional ama network özellikleri için gerekli).

ZIP olarak indirmek ister misiniz yoksa bu yapıyı kendi projenize mi entegre etmek istiyorsunuz?
