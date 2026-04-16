package dev.lipasquide.lipoitems;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.Waterlogged;
// FIX A: org.bukkit.Axis was never imported — every Axis.X/Y/Z reference failed.
import org.bukkit.Axis;
// FIX B: Bisected.Half is the correct nested type; Stairs.Half / TrapDoor.Half don't exist.
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class LipoItems extends JavaPlugin implements Listener {

    private static final int    MIN_Y   = -64;
    private static final String NBT_KEY = "lipoitems_type";

    private static final ClientVersion PE_VERSION = ClientVersion.V_1_21;

    public enum ToolType { PICKAXE, AXE, SHOVEL, NONE }

    public enum CustomBlockType {
        RUBY_ORE        ("ruby_ore",        Material.DIAMOND_ORE,                         Material.STONE, ToolType.PICKAXE, 2, Material.DIAMOND,           1, false),
        SAPPHIRE_ORE    ("sapphire_ore",     Material.EMERALD_ORE,                         Material.STONE, ToolType.PICKAXE, 2, Material.EMERALD,            1, false),
        RUBY_BLOCK      ("ruby_block",       Material.NETHERITE_BLOCK,                     Material.STONE, ToolType.PICKAXE, 3, Material.DIAMOND,            9, false),
        SAPPHIRE_BLOCK  ("sapphire_block",   Material.AMETHYST_BLOCK,                      Material.STONE, ToolType.PICKAXE, 2, Material.EMERALD,            9, false),
        SOUL_SAND_CUSTOM("soul_custom",      Material.SOUL_SAND,                           Material.STONE, ToolType.SHOVEL,  0, Material.SOUL_SAND,          1, false),
        RUBY_STAIRS     ("ruby_stairs",      Material.NETHER_BRICK_STAIRS,                 Material.STONE, ToolType.PICKAXE, 2, Material.DIAMOND,            1, true),
        SOUL_STAIRS     ("soul_stairs",      Material.BLACKSTONE_STAIRS,                   Material.STONE, ToolType.PICKAXE, 0, Material.SAND,               2, true),
        ANCIENT_STAIRS  ("ancient_stairs",   Material.DARK_OAK_STAIRS,                    Material.STONE, ToolType.AXE,     0, Material.STICK,              2, true),
        RUBY_SLAB       ("ruby_slab",        Material.NETHER_BRICK_SLAB,                   Material.STONE, ToolType.PICKAXE, 2, Material.DIAMOND,            1, true),
        SOUL_SLAB       ("soul_slab",        Material.BLACKSTONE_SLAB,                     Material.STONE, ToolType.PICKAXE, 0, Material.SAND,               1, true),
        RUBY_FENCE      ("ruby_fence",       Material.NETHER_BRICK_FENCE,                  Material.STONE, ToolType.PICKAXE, 2, Material.STICK,              2, true),
        ANCIENT_FENCE   ("ancient_fence",    Material.DARK_OAK_FENCE,                     Material.STONE, ToolType.AXE,     0, Material.STICK,              2, true),
        RUBY_GATE       ("ruby_gate",        Material.OAK_FENCE_GATE,                     Material.STONE, ToolType.PICKAXE, 2, Material.STICK,              2, true),
        RUBY_WALL       ("ruby_wall",        Material.NETHER_BRICK_WALL,                   Material.STONE, ToolType.PICKAXE, 2, Material.DIAMOND,            1, true),
        RUBY_PANE       ("ruby_pane",        Material.GLASS_PANE,                         Material.STONE, ToolType.PICKAXE, 0, Material.GLASS_PANE,         1, true),
        ANCIENT_DOOR    ("ancient_door",     Material.DARK_OAK_DOOR,                      Material.STONE, ToolType.AXE,     0, Material.STICK,              2, true),
        SOUL_TRAPDOOR   ("soul_trapdoor",    Material.IRON_TRAPDOOR,                      Material.STONE, ToolType.PICKAXE, 1, Material.IRON_INGOT,         1, true),
        RUBY_BUTTON     ("ruby_button",      Material.STONE_BUTTON,                        Material.STONE, ToolType.PICKAXE, 0, Material.STONE_BUTTON,       1, true),
        SOUL_PRESSURE   ("soul_pressure",    Material.POLISHED_BLACKSTONE_PRESSURE_PLATE,  Material.STONE, ToolType.PICKAXE, 0, Material.STONE_BUTTON,       1, true),
        ANCIENT_LOG     ("ancient_log",      Material.CRIMSON_STEM,                        Material.STONE, ToolType.AXE,     0, Material.STICK,              4, true),
        RUBY_LOG        ("ruby_log",         Material.WARPED_STEM,                         Material.STONE, ToolType.AXE,     2, Material.DIAMOND,            2, true),
        SOUL_LANTERN    ("soul_lantern",     Material.SOUL_LANTERN,                        Material.STONE, ToolType.PICKAXE, 0, Material.SOUL_LANTERN,       1, true),
        RUBY_TORCH      ("ruby_torch",       Material.REDSTONE_TORCH,                      Material.STONE, ToolType.NONE,    0, Material.REDSTONE_TORCH,    1, true),
        ANCIENT_CHEST   ("ancient_chest",    Material.CHEST,                              Material.STONE, ToolType.AXE,     0, Material.CHEST,              1, true),
        SOUL_SIGN       ("soul_sign",        Material.OAK_WALL_SIGN,                      Material.STONE, ToolType.AXE,     0, Material.OAK_SIGN,           1, true),
        SOUL_BED        ("soul_bed",         Material.RED_BED,                            Material.STONE, ToolType.AXE,     0, Material.RED_BED,            1, true),
        ANCIENT_BANNER  ("ancient_banner",   Material.BLACK_BANNER,                        Material.STONE, ToolType.AXE,     0, Material.BLACK_BANNER,       1, true);

        public final String   id;
        public final Material fakeMaterial;
        public final Material serverMaterial;
        public final ToolType toolType;
        public final int      minTier;
        public final Material dropMaterial;
        public final int      dropCount;
        public final boolean  directional;

        CustomBlockType(String id, Material fakeMaterial, Material serverMaterial,
                        ToolType toolType, int minTier, Material dropMaterial,
                        int dropCount, boolean directional) {
            this.id             = id;
            this.fakeMaterial   = fakeMaterial;
            this.serverMaterial = serverMaterial;
            this.toolType       = toolType;
            this.minTier        = minTier;
            this.dropMaterial   = dropMaterial;
            this.dropCount      = dropCount;
            this.directional    = directional;
        }

        public static CustomBlockType byId(String id) {
            for (CustomBlockType t : values()) if (t.id.equals(id)) return t;
            return null;
        }

        public boolean isConnectable() {
            String name = fakeMaterial.name();
            return (name.contains("FENCE") && !name.contains("GATE"))
                    || name.contains("WALL")
                    || name.contains("PANE");
        }

        ConnectionType connectionType() {
            String name = fakeMaterial.name();
            if (name.contains("WALL"))  return ConnectionType.WALL;
            if (name.contains("PANE"))  return ConnectionType.PANE;
            return ConnectionType.FENCE;
        }
    }

    private enum ConnectionType { FENCE, WALL, PANE }

    private static final class PlacedBlock {
        final CustomBlockType type;
        final String stateString;
        final String stateStringNormalized;

        PlacedBlock(CustomBlockType type, String stateString) {
            this.type                  = type;
            this.stateString           = stateString;
            this.stateStringNormalized = normalizeStateStringStatic(stateString);
        }
    }

    // ── Coordinate encoding ──────────────────────────────────────────────────

    private static long blockKey(int x, int y, int z) {
        int shiftedY = Math.max(0, y - MIN_Y);
        return ((long) (x & 0x3FFFFFF) << 38)
             | ((long) (shiftedY & 0xFFF) << 26)
             | ((long) (z & 0x3FFFFFF));
    }

    private static int[] decodeBlockKey(long bk) {
        int x        = (int) ((bk >> 38) & 0x3FFFFFF);
        int shiftedY = (int) ((bk >> 26) & 0xFFF);
        int z        = (int)  (bk        & 0x3FFFFFF);
        if ((x & 0x2000000) != 0) x |= 0xFC000000;
        if ((z & 0x2000000) != 0) z |= 0xFC000000;
        return new int[]{x, shiftedY + MIN_Y, z};
    }

    private static long chunkKey(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }

    private static String normalizeStateStringStatic(String state) {
        if (state == null || state.isEmpty()) return "";
        int bi = state.indexOf('[');
        if (bi == -1) return state;
        String base  = state.substring(0, bi);
        String props = state.substring(bi + 1, state.length() - 1);
        if (props.isEmpty()) return base;
        String[] pairs = props.split(",");
        Arrays.sort(pairs);
        return base + "[" + String.join(",", pairs) + "]";
    }

    /**
     * FIX C: BlockFace has no getAxis() method in the Bukkit API.
     *        Map manually via switch. Return type is org.bukkit.Axis (now imported).
     */
    private static Axis blockFaceAxis(BlockFace face) {
        return switch (face) {
            case NORTH, SOUTH -> Axis.Z;
            case EAST,  WEST  -> Axis.X;
            default           -> Axis.Y;
        };
    }

    // ── Storage ──────────────────────────────────────────────────────────────

    private final HashMap<Long, HashMap<Long, PlacedBlock>> persistent = new HashMap<>();
    private final ConcurrentHashMap<Long, ConcurrentHashMap<Long, PlacedBlock>> hot
            = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> stateCache = new ConcurrentHashMap<>();

    private NamespacedKey nbtKey;
    private File          dataFile;
    private volatile boolean dirty = false;

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        nbtKey   = new NamespacedKey(this, NBT_KEY);
        dataFile = new File(getDataFolder(), "blocks.yml");
        if (!getDataFolder().exists()) getDataFolder().mkdirs();
        saveDefaultConfig();

        PacketEvents.getAPI().init();
        PacketEvents.getAPI().getEventManager().registerListener(new ChunkPacketListener());

        getServer().getPluginManager().registerEvents(this, this);
        loadBlocks();
        registerRecipes();

        new BukkitRunnable() {
            @Override public void run() { if (dirty) saveBlocksAsync(); }
        }.runTaskTimer(this, 6000L, 6000L);

        getLogger().info("LipoItems enabled! " + CustomBlockType.values().length + " custom blocks.");
    }

    @Override
    public void onDisable() {
        saveBlocksSync();
        PacketEvents.getAPI().terminate();
    }

    // ── Storage helpers ──────────────────────────────────────────────────────

    private void putBlock(int x, int y, int z, PlacedBlock block) {
        long ck = chunkKey(x >> 4, z >> 4);
        long bk = blockKey(x, y, z);

        persistent.computeIfAbsent(ck, k -> new HashMap<>()).put(bk, block);

        ConcurrentHashMap<Long, PlacedBlock> hotChunk = hot.get(ck);
        if (hotChunk != null) hotChunk.put(bk, block);

        dirty = true;
    }

    private PlacedBlock getBlock(int x, int y, int z) {
        HashMap<Long, PlacedBlock> map = persistent.get(chunkKey(x >> 4, z >> 4));
        return map != null ? map.get(blockKey(x, y, z)) : null;
    }

    private void removeBlock(int x, int y, int z) {
        long ck = chunkKey(x >> 4, z >> 4);
        long bk = blockKey(x, y, z);

        HashMap<Long, PlacedBlock> pMap = persistent.get(ck);
        if (pMap != null) {
            pMap.remove(bk);
            if (pMap.isEmpty()) persistent.remove(ck);
        }

        ConcurrentHashMap<Long, PlacedBlock> hMap = hot.get(ck);
        if (hMap != null) {
            hMap.remove(bk);
            if (hMap.isEmpty()) hot.remove(ck);
        }

        dirty = true;
    }

    // ── Chunk lifecycle ──────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        long ck = chunkKey(event.getChunk().getX(), event.getChunk().getZ());
        HashMap<Long, PlacedBlock> pChunk = persistent.get(ck);
        if (pChunk == null || pChunk.isEmpty()) return;
        hot.put(ck, new ConcurrentHashMap<>(pChunk));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        hot.remove(chunkKey(event.getChunk().getX(), event.getChunk().getZ()));
    }

    // ── Packet listener ──────────────────────────────────────────────────────

    private final class ChunkPacketListener extends PacketListenerAbstract {
        ChunkPacketListener() { super(PacketListenerPriority.NORMAL); }

        @Override
        public void onPacketSend(PacketSendEvent event) {
            if (event.getPacketType() != PacketType.Play.Server.CHUNK_DATA) return;

            WrapperPlayServerChunkData wrapper = new WrapperPlayServerChunkData(event);
            Column column  = wrapper.getColumn();
            long ck        = chunkKey(column.getX(), column.getZ());

            ConcurrentHashMap<Long, PlacedBlock> chunkBlocks = hot.get(ck);
            if (chunkBlocks == null || chunkBlocks.isEmpty()) return;

            BaseChunk[] sections = column.getChunks();
            boolean modified     = false;

            for (Map.Entry<Long, PlacedBlock> entry : chunkBlocks.entrySet()) {
                PlacedBlock pb = entry.getValue();

                Integer resolvedId = stateCache.get(pb.stateStringNormalized);
                if (resolvedId == null) {
                    try {
                        WrappedBlockState wrapped =
                                WrappedBlockState.getByString(PE_VERSION, pb.stateString);
                        if (wrapped == null) {
                            getLogger().warning("[LipoItems] Null state for: " + pb.stateString);
                            continue;
                        }
                        int computed = wrapped.getGlobalId();
                        Integer existing = stateCache.putIfAbsent(pb.stateStringNormalized, computed);
                        resolvedId = existing != null ? existing : computed;
                    } catch (Exception e) {
                        getLogger().warning("[LipoItems] State resolve failed '"
                                + pb.stateString + "': " + e.getMessage());
                        continue;
                    }
                }

                int[] coords      = decodeBlockKey(entry.getKey());
                int x = coords[0], y = coords[1], z = coords[2];
                int sectionIndex  = (y - MIN_Y) >> 4;
                if (sectionIndex < 0 || sectionIndex >= sections.length) continue;
                BaseChunk section = sections[sectionIndex];
                if (section == null) continue;

                try {
                    section.set(x & 15, y & 15, z & 15, resolvedId);
                    modified = true;
                } catch (Throwable t) {
                    getLogger().warning("[LipoItems] Patch failed at ("
                            + x + "," + y + "," + z + "): " + t.getMessage());
                }
            }

            if (modified) event.markForReEncode(true);
        }
    }

    // ── Block events ─────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        String typeId = getCustomTypeId(event.getItemInHand());
        if (typeId == null) return;

        CustomBlockType type = CustomBlockType.byId(typeId);
        if (type == null) return;

        Block    block  = event.getBlockPlaced();
        Location loc    = block.getLocation();
        Player   player = event.getPlayer();

        if (block.getType() != type.serverMaterial)
            block.setType(type.serverMaterial, false);

        PlacedBlock pb = new PlacedBlock(type, createDirectionalStateString(type, player));
        putBlock(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), pb);

        if (type.isConnectable()) {
            Map<Long, Block> cache = new HashMap<>();
            updateBlock(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), cache);
            updateNeighbors(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), cache);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block    block  = event.getBlock();
        Location loc    = block.getLocation();
        PlacedBlock pb  = getBlock(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        if (pb == null) return;

        CustomBlockType type   = pb.type;
        Player          player = event.getPlayer();

        if (!checkTool(player, type)) {
            event.setCancelled(true);
            player.sendMessage(Component.text(
                    "Bu bloğu " + toolName(type.toolType) + " ile kırman gerekiyor!",
                    NamedTextColor.RED));
            return;
        }

        event.setDropItems(false);
        event.setExpToDrop(0);

        if (hasSilkTouch(player)) {
            block.getWorld().dropItemNaturally(loc, createCustomItem(type, 1));
        } else {
            block.getWorld().dropItemNaturally(loc, new ItemStack(type.dropMaterial, type.dropCount));
        }

        removeBlock(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

        if (type.isConnectable()) {
            Map<Long, Block> cache = new HashMap<>();
            updateNeighbors(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), cache);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPhysics(BlockPhysicsEvent event) {
        Block block = event.getBlock();
        PlacedBlock pb = getBlock(block.getX(), block.getY(), block.getZ());
        if (pb != null && pb.type.isConnectable()) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) return;

        PlacedBlock pb = getBlock(block.getX(), block.getY(), block.getZ());
        if (pb == null) return;

        String name = pb.type.fakeMaterial.name();
        if (name.contains("DOOR") || name.contains("TRAPDOOR") || name.contains("BUTTON") ||
            name.contains("PRESSURE") || name.contains("CHEST") || name.contains("GATE") ||
            name.contains("SIGN") || name.contains("BED") || name.contains("BANNER") ||
            name.contains("LANTERN") || name.contains("TORCH")) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        String url  = getConfig().getString("resource-pack.url",  "");
        String hash = getConfig().getString("resource-pack.sha1", "");
        if (!url.isEmpty()) {
            event.getPlayer().setResourcePack(url, hash, true,
                    Component.text("LipoItems resource pack gerekli!", NamedTextColor.YELLOW));
        }
    }

    @EventHandler
    public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
        String url = getConfig().getString("resource-pack.url", "");
        if (url.isEmpty()) return;
        PlayerResourcePackStatusEvent.Status status = event.getStatus();
        if (status == PlayerResourcePackStatusEvent.Status.DECLINED ||
            status == PlayerResourcePackStatusEvent.Status.FAILED_DOWNLOAD) {
            event.getPlayer().kick(
                    Component.text("Resource pack olmadan sunucuya giremezsin!", NamedTextColor.RED));
        }
    }

    // ── Connection logic ──────────────────────────────────────────────────────

    private Block cachedGetBlock(World world, int x, int y, int z, Map<Long, Block> cache) {
        return cache.computeIfAbsent(blockKey(x, y, z), k -> world.getBlockAt(x, y, z));
    }

    private boolean canConnect(World world, int x, int y, int z,
                               CustomBlockType self, BlockFace face,
                               Map<Long, Block> cache) {
        int nx = x + face.getModX();
        int ny = y + face.getModY();
        int nz = z + face.getModZ();

        PlacedBlock neighbor = getBlock(nx, ny, nz);
        if (neighbor != null) return canCustomConnect(self, neighbor.type, face);

        Block    block = cachedGetBlock(world, nx, ny, nz, cache);
        Material mat   = block.getType();
        if (mat.isAir()) return false;

        return switch (self.connectionType()) {
            case FENCE -> canFenceConnect(block, face);
            case WALL  -> canWallConnect(block);
            case PANE  -> canPaneConnect(block);
        };
    }

    private boolean canCustomConnect(CustomBlockType self, CustomBlockType other, BlockFace face) {
        ConnectionType a = self.connectionType();
        ConnectionType b = other.connectionType();
        if (a == b) return true;
        if (a == ConnectionType.FENCE && other.fakeMaterial.name().contains("GATE")) {
            // Gate connects to fence only when the face axis is horizontal (X or Z).
            // FIX C applied: blockFaceAxis now compiles because Axis is imported.
            return blockFaceAxis(face) != Axis.Y;
        }
        return false;
    }

    private boolean canFenceConnect(Block block, BlockFace face) {
        Material mat = block.getType();
        if (Tag.FENCES.isTagged(mat)) return true;
        if (Tag.FENCE_GATES.isTagged(mat)) {
            BlockData data = block.getBlockData();
            if (data instanceof org.bukkit.block.data.type.Gate gate) {
                // Gate connects to a fence only when perpendicular: their axes must differ.
                return blockFaceAxis(gate.getFacing()) != blockFaceAxis(face);
            }
        }
        return isFullBlock(block);
    }

    private boolean canWallConnect(Block block) {
        return Tag.WALLS.isTagged(block.getType()) || isFullBlock(block);
    }

    private boolean canPaneConnect(Block block) {
        Material mat = block.getType();
        // Tag.GLASS_PANES doesn't exist in 1.21.1, it's GLASS_PANES in Tag but might be different.
        // Actually it's Tag.GLASS_PANES usually. Let me check name.
        if (mat.name().contains("PANE")) return true;
        if (mat.name().contains("IRON_BARS")) return true;
        return isFullBlock(block);
    }

    private boolean isFullBlock(Block block) {
        Material mat = block.getType();
        if (!mat.isSolid() || !mat.isOccluding()) return false;
        String name = mat.name();
        return !name.contains("LEAVES") && !name.contains("GLASS") && !name.contains("ICE") &&
               !name.contains("TNT")    && !name.contains("BEACON") && !name.contains("CAULDRON");
    }

    private void updateBlock(World world, int x, int y, int z, Map<Long, Block> cache) {
        PlacedBlock pb = getBlock(x, y, z);
        if (pb == null || !pb.type.isConnectable()) return;

        CustomBlockType type     = pb.type;
        String          newState = (type.connectionType() == ConnectionType.WALL)
                ? buildWallState(world, x, y, z, type, cache)
                : buildFencePaneState(world, x, y, z, type, cache);

        if (normalizeStateStringStatic(newState).equals(pb.stateStringNormalized)) return;

        PlacedBlock updated = new PlacedBlock(type, newState);
        long ck = chunkKey(x >> 4, z >> 4);
        long bk = blockKey(x, y, z);

        HashMap<Long, PlacedBlock> pMap = persistent.get(ck);
        if (pMap != null) pMap.put(bk, updated);

        ConcurrentHashMap<Long, PlacedBlock> hMap = hot.get(ck);
        if (hMap != null) hMap.put(bk, updated);

        dirty = true;
    }

    private void updateNeighbors(World world, int x, int y, int z, Map<Long, Block> cache) {
        updateBlock(world, x + 1, y, z, cache);
        updateBlock(world, x - 1, y, z, cache);
        updateBlock(world, x, y, z + 1, cache);
        updateBlock(world, x, y, z - 1, cache);
    }

    private String buildFencePaneState(World world, int x, int y, int z,
                                        CustomBlockType type, Map<Long, Block> cache) {
        boolean north = canConnect(world, x, y, z, type, BlockFace.NORTH, cache);
        boolean south = canConnect(world, x, y, z, type, BlockFace.SOUTH, cache);
        boolean east  = canConnect(world, x, y, z, type, BlockFace.EAST,  cache);
        boolean west  = canConnect(world, x, y, z, type, BlockFace.WEST,  cache);
        return String.format("%s[east=%b,north=%b,south=%b,waterlogged=false,west=%b]",
                type.fakeMaterial.getKey(), east, north, south, west);
    }

    private String buildWallState(World world, int x, int y, int z,
                                   CustomBlockType type, Map<Long, Block> cache) {
        boolean north = canConnect(world, x, y, z, type, BlockFace.NORTH, cache);
        boolean south = canConnect(world, x, y, z, type, BlockFace.SOUTH, cache);
        boolean east  = canConnect(world, x, y, z, type, BlockFace.EAST,  cache);
        boolean west  = canConnect(world, x, y, z, type, BlockFace.WEST,  cache);

        String ns = north ? "low" : "none";
        String ss = south ? "low" : "none";
        String es = east  ? "low" : "none";
        String ws = west  ? "low" : "none";

        boolean straightLine = (north && south && !east && !west)
                             || (!north && !south && east && west);
        Block   above        = cachedGetBlock(world, x, y + 1, z, cache);
        boolean up           = !straightLine || above.getType().isSolid();

        return String.format("%s[east=%s,north=%s,south=%s,up=%b,waterlogged=false,west=%s]",
                type.fakeMaterial.getKey(), es, ns, ss, up, ws);
    }

    // ── Directional block placement ──────────────────────────────────────────

    private String createDirectionalStateString(CustomBlockType type, Player player) {
        if (!type.directional) {
            return type.fakeMaterial.getKey().toString();
        }

        BlockFace facing = getPlayerFacing(player);
        try {
            BlockData data = Bukkit.createBlockData(type.fakeMaterial);

            if (data instanceof Stairs stairs) {
                stairs.setFacing(facing.getOppositeFace());
                // FIX D: Stairs.Half does not exist as a nested type.
                //        Bukkit defines Half inside the Bisected interface → Bisected.Half.
                stairs.setHalf(Bisected.Half.BOTTOM);
                stairs.setShape(Stairs.Shape.STRAIGHT);
                if (stairs instanceof Waterlogged w) w.setWaterlogged(false);

            } else if (data instanceof Slab slab) {
                slab.setType(player.getEyeLocation().getPitch() > 0
                        ? Slab.Type.TOP : Slab.Type.BOTTOM);
                if (slab instanceof Waterlogged w) w.setWaterlogged(false);

            } else if (data instanceof org.bukkit.block.data.type.Gate gate) {
                gate.setFacing(facing);
                gate.setOpen(false);
                gate.setPowered(false);
                gate.setInWall(false);

            } else if (data instanceof Wall wall) {
                if (wall instanceof Waterlogged w) w.setWaterlogged(false);

            } else if (data instanceof Door door) {
                door.setFacing(facing);
                door.setHinge(Door.Hinge.LEFT);
                door.setOpen(false);
                door.setPowered(false);

            } else if (data instanceof TrapDoor trap) {
                trap.setFacing(facing);
                // FIX E: TrapDoor.Half does not exist. TrapDoor implements Bisected → Bisected.Half.
                trap.setHalf(Bisected.Half.BOTTOM);
                trap.setOpen(false);
                trap.setPowered(false);
                if (trap instanceof Waterlogged w) w.setWaterlogged(false);

            } else if (data instanceof Orientable orient) {
                // FIX C applied: Axis.Y/X/Z now compile because org.bukkit.Axis is imported.
                orient.setAxis(switch (facing) {
                    case UP, DOWN   -> Axis.Y;
                    case EAST, WEST -> Axis.X;
                    default         -> Axis.Z;
                });

            } else if (data instanceof org.bukkit.block.data.type.Switch button) {
                button.setFacing(facing.getOppositeFace());
                button.setPowered(false);

            } else if (data instanceof org.bukkit.block.data.Powerable powerable) {
                powerable.setPowered(false);

            } else if (data instanceof Lantern lantern) {
                lantern.setHanging(facing == BlockFace.DOWN
                        || player.getEyeLocation().getPitch() > 45);
                if (lantern instanceof Waterlogged w) w.setWaterlogged(false);

            } else if (data instanceof Chest chest) {
                chest.setFacing(facing.getOppositeFace());
                chest.setType(Chest.Type.SINGLE);
                if (chest instanceof Waterlogged w) w.setWaterlogged(false);

            // FIX F: Sign.setRotation(BlockFace) does not exist on the Sign interface.
            //        The Sign block data branch is removed entirely.
            //        — Standing signs (OAK_SIGN) implement Rotatable → caught below.
            //        — Wall signs (OAK_WALL_SIGN) implement Directional → caught at the end.

            } else if (data instanceof Bed bed) {
                bed.setFacing(facing.getOppositeFace());
                bed.setPart(Bed.Part.FOOT);
                bed.setOccupied(false);

            // Standing banners implement Rotatable; wall banners implement Directional.
            // Rotatable check must come before the generic Directional fallback.
            } else if (data instanceof org.bukkit.block.data.Rotatable rotatable) {
                rotatable.setRotation(facing);

            } else if (data instanceof Directional dir) {
                dir.setFacing(facing.getOppositeFace());
            }

            return normalizeStateStringStatic(data.getAsString());

        } catch (Exception e) {
            getLogger().warning("[LipoItems] Directional state fallback for "
                    + type.id + ": " + e.getMessage());
            return type.fakeMaterial.getKey().toString();
        }
    }

    private BlockFace getPlayerFacing(Player player) {
        float yaw = player.getLocation().getYaw() % 360;
        if (yaw < 0) yaw += 360;
        if (yaw < 45 || yaw >= 315) return BlockFace.SOUTH;
        if (yaw < 135)              return BlockFace.WEST;
        if (yaw < 225)              return BlockFace.NORTH;
        return BlockFace.EAST;
    }

    // ── Utility ──────────────────────────────────────────────────────────────

    private boolean checkTool(Player player, CustomBlockType type) {
        if (type.toolType == ToolType.NONE) return true;
        ItemStack tool = player.getInventory().getItemInMainHand();
        return getToolTier(tool.getType(), type.toolType) >= type.minTier;
    }

    private int getToolTier(Material m, ToolType required) {
        String name = m.name();
        boolean match = switch (required) {
            case PICKAXE -> name.endsWith("_PICKAXE");
            case AXE     -> name.endsWith("_AXE");
            case SHOVEL  -> name.endsWith("_SHOVEL");
            default      -> true;
        };
        if (!match) return Integer.MIN_VALUE;
        if (name.startsWith("WOODEN") || name.startsWith("GOLDEN")) return 0;
        if (name.startsWith("STONE"))     return 1;
        if (name.startsWith("IRON"))      return 2;
        if (name.startsWith("DIAMOND"))   return 3;
        if (name.startsWith("NETHERITE")) return 4;
        return Integer.MIN_VALUE;
    }

    private boolean hasSilkTouch(Player player) {
        return player.getInventory().getItemInMainHand()
                     .containsEnchantment(Enchantment.SILK_TOUCH);
    }

    private String toolName(ToolType type) {
        return switch (type) {
            case PICKAXE -> "kazma";
            case AXE     -> "balta";
            case SHOVEL  -> "kürek";
            default      -> "uygun alet";
        };
    }

    private String getCustomTypeId(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(nbtKey, PersistentDataType.STRING);
    }

    public ItemStack createCustomItem(CustomBlockType type, int amount) {
        ItemStack item = new ItemStack(type.serverMaterial, amount);
        ItemMeta  meta = item.getItemMeta();
        meta.displayName(Component.text(formatName(type.id)));
        meta.getPersistentDataContainer().set(nbtKey, PersistentDataType.STRING, type.id);
        meta.setCustomModelData(type.ordinal() + 1);
        item.setItemMeta(meta);
        return item;
    }

    private String formatName(String id) {
        StringBuilder sb = new StringBuilder();
        for (String part : id.split("_")) {
            if (!sb.isEmpty()) sb.append(' ');
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                sb.append(part.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }

    private void registerRecipes() {
        int registered = 0;
        for (CustomBlockType type : CustomBlockType.values()) {
            if (type.dropCount > 0) {
                try {
                    ShapedRecipe recipe = new ShapedRecipe(
                            new NamespacedKey(this, type.id + "_recipe"),
                            createCustomItem(type, 1));
                    recipe.shape("DDD", "DDD", "DDD");
                    recipe.setIngredient('D', type.dropMaterial);
                    getServer().addRecipe(recipe);
                    registered++;
                } catch (Exception e) {
                    getLogger().log(Level.WARNING,
                            "[LipoItems] Recipe failed for " + type.id, e);
                }
            }
        }
        getLogger().info("[LipoItems] Registered " + registered
                + " / " + CustomBlockType.values().length + " recipes.");
    }

    // ── Persistence ──────────────────────────────────────────────────────────

    private synchronized void loadBlocks() {
        if (!dataFile.exists()) return;
        YamlConfiguration cfg  = YamlConfiguration.loadConfiguration(dataFile);
        List<Map<?, ?>>    list = cfg.getMapList("blocks");
        int count = 0;

        for (Map<?, ?> entry : list) {
            try {
                String id    = (String) entry.get("type");
                String state = (String) entry.get("state");
                int x = ((Number) entry.get("x")).intValue();
                int y = ((Number) entry.get("y")).intValue();
                int z = ((Number) entry.get("z")).intValue();

                CustomBlockType type = CustomBlockType.byId(id);
                if (type != null) {
                    String stateString = (state != null && !state.isEmpty())
                            ? state
                            : type.fakeMaterial.getKey().toString();
                    putBlock(x, y, z, new PlacedBlock(type, stateString));
                    count++;
                }
            } catch (Exception e) {
                getLogger().warning("[LipoItems] Load error: " + e.getMessage());
            }
        }
        dirty = false;
        getLogger().info("[LipoItems] Loaded " + count + " custom blocks.");
    }

    private void saveBlocksAsync() {
        List<BlockSnapshot> snapshots = buildSnapshots();
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> writeSnapshots(snapshots));
    }

    private void saveBlocksSync() {
        writeSnapshots(buildSnapshots());
    }

    private List<BlockSnapshot> buildSnapshots() {
        List<BlockSnapshot> snapshots = new ArrayList<>();
        for (var ce : persistent.entrySet()) {
            for (var be : ce.getValue().entrySet()) {
                int[]       coords = decodeBlockKey(be.getKey());
                PlacedBlock pb     = be.getValue();
                snapshots.add(new BlockSnapshot(pb.type.id, pb.stateString,
                        coords[0], coords[1], coords[2]));
            }
        }
        return snapshots;
    }

    private record BlockSnapshot(String typeId, String stateString, int x, int y, int z) {}

    private void writeSnapshots(List<BlockSnapshot> snapshots) {
        try {
            YamlConfiguration cfg  = new YamlConfiguration();
            List<Map<String, Object>> list = new ArrayList<>(snapshots.size());
            for (BlockSnapshot snap : snapshots) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("type",  snap.typeId());
                map.put("state", snap.stateString());
                map.put("x",     snap.x());
                map.put("y",     snap.y());
                map.put("z",     snap.z());
                list.add(map);
            }
            cfg.set("blocks", list);
            cfg.save(dataFile);
            dirty = false;
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "[LipoItems] Save failed", e);
        }
    }
}
