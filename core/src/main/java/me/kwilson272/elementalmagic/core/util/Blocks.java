package me.kwilson272.elementalmagic.core.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Waterlogged;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.revertible.TempBlock;

public final class Blocks {
    
    private static Set<Material> iceMaterials = Set.of();
    private static Set<Material> snowMaterials = Set.of();
    private static Set<Material> plantMaterials = Set.of();
    private static Set<Material> earthMaterials = Set.of();
    private static Set<Material> sandMaterials = Set.of();
    private static Set<Material> metalMaterials = Set.of();

    private Blocks() { }

    /**
     * Checks if a {@link Block} is considered solid to abilities.
     *
     * @param block the {@code Block}.
     * @return true if the block is solid, false otherwise.
     */
    public static boolean isSolid(Block block) {
        return block.getType().isSolid()
            && TempBlock.get(block).map(TempBlock::isCollidable).orElse(true);
    }

    /**
     * Checks if a {@link Block} is considered liquid to abilities.
     *
     * @param block the {@code Block}.
     * @return true if the block is a liquid, false otherwise.
     */
    public static boolean isLiquid(Block block) {
        return block.isLiquid()
            && TempBlock.get(block).map(TempBlock::isCollidable).orElse(true);
    }

    /**
     * Checks if a {@link Block} is considered water to abilities.
     *
     * @param block the {@code Block}.
     * @return true if the block is water, false otherwise.
     */
    public static boolean isWater(Block block) {
        Material type = block.getType();
        BlockData data = block.getBlockData();

        if (type == Material.WATER 
            || type == Material.BUBBLE_COLUMN 
            || type == Material.SEAGRASS 
            || type == Material.TALL_SEAGRASS 
            || type == Material.KELP 
            || type == Material.KELP_PLANT 
            || type == Material.WATER_CAULDRON) {
            return true;
        }

        if (data instanceof Waterlogged wl) {
            return wl.isWaterlogged();
        }

        if (block.getBlockData() instanceof Levelled) {
            return type != Material.LAVA 
                && type != Material.LAVA_CAULDRON 
                && type != Material.POWDER_SNOW_CAULDRON 
                && type != Material.COMPOSTER;
        }

        return false;
    }

    /**
     * Checks if a {@link Block} is considered ice to abilities.
     *
     * @param block the {@code Block}.
     * return true if the block is ice, false otherwise.
     */
    public static boolean isIce(Block block) {
        return iceMaterials.contains(block.getType());
    }

    /**
     * Checks if a {@link Block} is considered snow to abilities.
     *
     * @param block the {@code Block}.
     * return true if the block is snow, false otherwise.
     */
    public static boolean isSnow(Block block) {
        return snowMaterials.contains(block.getType());
    }

    /**
     * Checks if a {@link Block} is considered plant to abilities.
     *
     * @param block the {@code Block}.
     * return true if the block is plant, false otherwise.
     */
    public static boolean isPlant(Block block) {
        return plantMaterials.contains(block.getType());
    }

    /**
     * Checks if a {@link Block} is considered earth to abilities.
     *
     * @param block the {@code Block}.
     * return true if the block is earth, false otherwise.
     */
    public static boolean isEarth(Block block) {
        return earthMaterials.contains(block.getType());
    }

    /**
     * Checks if a {@link Block} is considered sand to abilities.
     *
     * @param block the {@code Block}.
     * return true if the block is sand, false otherwise.
     */
    public static boolean isSand(Block block) {
        return sandMaterials.contains(block.getType()); 
    }
    
    /**
     * Checks if a {@link Block} is considered metal to abilities.
     *
     * @param block the {@code Block}.
     * return true if the block is metal, false otherwise.
     */
    public static boolean isMetal(Block block) {
        return metalMaterials.contains(block.getType()); 
    }

    /**
     * Checks if a {@link Block} is considered lava to abilities.
     *
     * @param block the {@code Block}.
     * return true if the block is lava, false otherwise.
     */
    public static boolean isLava(Block block) {
        return block.getType() == Material.LAVA
            || block.getType() == Material.LAVA_CAULDRON;
    }

    /**
     * Checks if a {@link Block} is considered generally usable by abilities.
     *
     * @param Block the {@code block}
     * @return true if the block is usable, false otherwise.
     */
    public static boolean canAbilityUse(Block block) {
        return TempBlock.get(block).map(TempBlock::isUsable).orElse(true);
    }

    /**
     * Checks if there is a collision with diagonally connected blocks between
     * the start and end locations. This function is designed to work with
     * locations that are <= 1 block away from each other.
     *
     * @param start the first {@link Location}
     * @param end the second {@link Location}
     * @param collisionCheck the {@link Predicate} that must return true if a 
     * block is collidable, false otherwise.
     * @return true if there is a diagonal block collision, false otherwise.
     */
    public static boolean collidesDiagonally(Location start, Location end, 
                                             Predicate<Block> collisionCheck) {
        Block startBlock = start.getBlock();
        Block endBlock = end.getBlock();
        if (startBlock.equals(endBlock)) {
            return false;
        }
        
        int x = endBlock.getX() - startBlock.getX();
        int y = endBlock.getY() - startBlock.getY();
        int z = endBlock.getZ() - startBlock.getZ();
        
        // Diagonal collisions can only occur where the components of movement
        // are non-zero on two or more axes:
        if (x != 0 && z != 0) {
            Block checkX = startBlock.getRelative(x, 0, 0);
            Block checkZ = startBlock.getRelative(0, 0, z);
            if (collisionCheck.test(checkX) || collisionCheck.test(checkZ)) {
                return true;
            }
        }

        if (y != 0 && (x != 0 || z != 0)) {
            Block checkXZ = startBlock.getRelative(x, 0, z);
            Block checkY = startBlock.getRelative(0, y, 0);
            return collisionCheck.test(checkXZ) || collisionCheck.test(checkY);
        }
    
        return false;
    }

    /**
     * Collects all of the blocks in a cuboid and returns them.
     *
     * @param center the {@link Location} center of the cuboid.
     * @param lenX the Double length of the cuboid on the x axis.
     * @param lenY the Double length of the cuboid on the y axis.
     * @param lenZ the Double length of the cuboid on the z axis.
     * @return a {@link List} of collected blocks.
     */
    public static List<Block> collectCuboid(Location center, 
                                     double lenX, double lenY, double lenZ) {
        World world = center.getWorld();
        if (world == null) {
            return List.of();
        }

        // Start at the lowest corner so we can use plain addition to iterate
        int halfX = (int) Math.round(lenX / 2);
        int halfY = (int) Math.round(lenY / 2);
        int halfZ = (int) Math.round(lenZ / 2);
    
        int startX = center.getBlockX() - halfX;
        int endX = center.getBlockX() + halfX;
        int startY = center.getBlockY() - halfY;
        int endY = center.getBlockY() + halfY;
        int startZ = center.getBlockZ() - halfZ;
        int endZ = center.getBlockZ() + halfZ;
        
        List<Block> blocks = new ArrayList<>();
        for (int x = startX; x <= endX; ++x) {
            for (int y = startY; y <= endY; ++y) {
                for (int z = startZ; z <= endZ; ++z) {
                    blocks.add(world.getBlockAt(x, y, z));
                }
            }
        }

        return blocks;
    }

    /**
     * Collects all of the blocks in a cube and returns them.
     *
     * @param center the {@link Location} center of the cube.
     * @param len the Double side length of the cube on all axes.
     * @return a {@link List} of collected blocks.
     */
    public static List<Block> collectCube(Location center, double len) {
        return collectCuboid(center, len, len, len);
    }

    /**
     * Collects all of the blocks in a sphere and returns them.
     *
     * @param center the {@link Location} center of the sphere.
     * @param radius the Double radius of the sphere.
     * @return a {@link List} of collected blocks.
     */
    public static List<Block> collectSphere(Location center, double radius) {
        World world = center.getWorld();
        if (world == null) {
            return List.of();
        }
        
        int rad = (int) (Math.ceil(radius));
        // Keep this a double to preserve the effect of decimal values on shape
        double radSqrd =  radius * radius;

        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();

        int minX = centerX - rad;
        int maxX = centerX + rad;
        int minY = centerY - rad;
        int maxY = centerY + rad;
        int minZ = centerZ - rad;
        int maxZ = centerZ + rad;
        
        List<Block> blocks = new ArrayList<>();
        for (int x = minX; x <= maxX; ++x) {
            for (int y = minY; y <= maxY; ++y) {
                for (int z = minZ; z <= maxZ; ++z) {
                    int dx = centerX - x;
                    int dy = centerY - y;
                    int dz = centerZ - z;
                    if (dx*dx + dy*dy + dz*dz <= radSqrd) {
                        blocks.add(world.getBlockAt(x, y, z));
                    }
                }
            }
        }

        return blocks;
    }
   
    /**
     * Collects all blocks in a 1 block tall circle along the x-z axis and 
     * returns them.
     *
     * @param center the {@link Location} center of the circle.
     * @param radius the Double radius of the circle.
     */
    public static Collection<Block> collectCircle(Location center, double radius) {
        World world = center.getWorld();
        if (world == null) {
            return List.of();
        }

        int rad = (int) (Math.round(radius));
        double radSqrd =  radius * radius;

        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();
        
        List<Block> blocks = new ArrayList<>();
        for (int x = -(rad+1); x <= rad+1; ++x) {
            for (int z = -(rad+1); z <= rad+1; ++z) {
                double distSqrd = x*x + z*z;
                if (distSqrd <= radSqrd) {
                    int bx = centerX + x;
                    int bz = centerZ + z;
                    blocks.add(world.getBlockAt(bx, centerY, bz));
                }
            }
        }

        return blocks;
    }

/*********************************
 * CONFIGURATION
 *********************************/
    public static void configure() {
        BlockConfig config = new BlockConfig();
        ElementalMagicApi.configManager().configure(config);

        iceMaterials = parseMaterials(config.ice);
        snowMaterials = parseMaterials(config.snow);
        plantMaterials = parseMaterials(config.plant);
        earthMaterials = parseMaterials(config.earth);
        sandMaterials = parseMaterials(config.sand);
        metalMaterials = parseMaterials(config.metal);
    }

    private static Set<Material> parseMaterials(List<String> strings) {
        Set<Material> materials = EnumSet.noneOf(Material.class);
        for (String s : strings) {

            if (s.contains(":")) {
                String[] split = s.split(":", 2);
                String nameSpace = split[0];
                String key = split[1];
                var nsk = new NamespacedKey(nameSpace, key);
                Tag<Material> tag = Bukkit.getTag(Tag.REGISTRY_BLOCKS, nsk, Material.class);
                
                if (tag != null) {
                    materials.addAll(tag.getValues());
                    continue;
                }
            }

            Material mat = Material.getMaterial(s.toUpperCase());
            if (mat != null) {
                materials.add(mat);
                continue;
            }

            ElementalMagicApi.logger().log(Level.WARNING, 
                    "Unknown Material: '" + s + "' in blocks config.");
        }
        return materials;
    }

    protected static class BlockConfig {
        
        private static final String CONFIG_PATH = "Blocks.";

        @Configure(path = CONFIG_PATH + "Plant", config = Config.ABILITIES)
        private List<String> plant = List.of(
            "minecraft:saplings", "minecraft:leaves", "minecraft:flowers",
            "minecraft:small_flowers", "minecraft:crops", "moss_block",
            "moss_carpet", "pale_moss_block", "pale_moss_carpet", "fern",
            "pale_hanging_moss", "azalea", "flowering_azalea", "bush",
            "brown_mushroom", "red_mushroom", "crimson_fungus", "warped_fungus",
            "short_grass", "short_dry_grass", "dead_bush", "leaf_litter", 
            "bamboo", "sugar_cane", "cactus", "warped_roots", "warped_sprouts",
            "crimson_roots", "twisting_vines", "weeping_vines", "vine",
            "tall_grass", "tall_dry_grass", "large_fern", "big_dripleaf",
            "small_dripleaf", "glow_lichen", "hanging_roots", "seagrass"
        );

        @Configure(path = CONFIG_PATH + "Snow", config = Config.ABILITIES)
        private List<String> snow = List.of("minecraft:snow");
        
        @Configure(path = CONFIG_PATH + "Ice", config = Config.ABILITIES)
        private List<String> ice = List.of("minecraft:ice");

        @Configure(path = CONFIG_PATH + "Earth", config = Config.ABILITIES)
        private List<String> earth = List.of(
            "minecraft:stone_bricks", "minecraft:dirt", "minecraft:mud",
            "minecraft:terracotta", "minecraft:base_stone_overworld", 
            "minecraft:glazed_terracotta", "minecraft:base_stone_nether", 
            "stone_stairs", "stone_slab","cobblestone", "cobblestone_stairs",
            "cobblestone_slab","cobblestone_wall", "mossy_cobblestone", 
            "mossy_cobblestone_stairs","mossy_cobblestone_wall", "smooth_stone",
            "smooth_stone_slab","stone_bricks", "cracked_stone_bricks", 
            "stone_brick_stairs","stone_brick_slab", "stone_brick_wall", 
            "chiseled_stone_bricks", "mossy_stone_bricks", "mossy_stone_brick_stairs", 
            "mossy_stone_brick_wall", "granite", "granite_stairs", "granite_slab",
            "granite_wall", "polished_granite", "polished_granite_stairs", 
            "polished_granite_slab", "diorite", "diorite_stairs", "diorite_wall",
            "diorite_slab", "polished_diorite", "polished_diorite_stairs", 
            "polished_diorite_slab", "andesite", "andesite_stairs", "andesite_slab",
            "andesite_wall", "polished_andesite", "polished_andesite_stairs",
            "polished_andesite_slab", "deepslate", "cobbled_deepslate", 
            "cobbled_deepslate_stairs", "cobbled_deepslate_slab", 
            "cobbled_deepslate_wall", "chiseled_deepslate", "polished_deepslate",
            "polished_deepslate_slab", "polished_deepslate_stairs", 
            "polished_deepslate_wall", "deepslate_bricks", "deepslate_brick_stairs",
            "cracked_deepslate_bricks", "deepslate_brick_slab", 
            "deepslate_brick_wall", "deepslate_tiles", "cracked_deepslate_tiles",
            "deepslate_tile_stairs", "deepslate_tile_slab", "deepslate_tile_wall",
            "reinforced_deepslate", "tuff", "tuff_stairs", "tuff_slab", "tuff_wall",
            "chiseled_tuff", "polished_tuff", "polished_tuff_stairs", 
            "polished_tuff_slab", "polished_tuff_wall", "tuff_bricks",
            "tuff_brick_slab", "tuff_brick_stairs", "tuff_brick_wall", 
            "chiseled_tuff_bricks", "bricks", "brick_stairs", "brick_slab", 
            "brick_wall", "packed_mud", "mud_bricks", "mud_brick_stairs",
            "mud_brick_slab", "mud_brick_wall", "resin_bricks", 
            "resin_brick_slab", "resin_brick_stairs", "resin_brick_wall", 
            "chiseled_resin_bricks", "prismarine", "prismarine_stairs",
            "prismarine_slab", "prismarine_wall", "prismarine_bricks", 
            "prismarine_brick_stairs", "prismarine_brick_slab", 
            "dark_prismarine", "dark_prismarine_slab", "dark_prismarine_stairs",
            "sea_lantern", "nether_bricks", "nether_brick_stairs", 
            "cracked_nether_bricks", "nether_brick_slab", "nether_brick_wall",
            "chiseled_nether_bricks", "red_nether_bricks", 
            "red_nether_brick_stairs", "red_nether_brick_wall", 
            "red_nether_brick_slab", "basalt", "smooth_basalt", "polished_basalt",
            "blackstone", "blackstone_stairs", "blackstone_slab", "blackstone_wall",
            "chiseled_polished_blackstone", "polished_blackstone", 
            "polished_blackstone_stairs", "polished_blackstone_slab",
            "polished_blackstone_wall", "polished_blackstone_bricks", 
            "cracked_polished_blackstone_bricks", "polished_blackstone_brick_wall", 
            "polished_blackstone_brick_stairs", "polished_blackstone_brick_slab", 
            "end_stone", "end_stone_bricks", "end_stone_brick_stairs", 
            "end_stone_brick_slab", "end_stone_brick_wall", "purpur_block", 
            "purpur_pillar", "purpur_slab", "coal_block", "coal_ore", 
            "deepslate_coal_ore", "quartz_slab", "quartz_stairs", 
            "chiseled_quartz_block", "quartz_bricks", "quartz_block",
            "quartz_pillar", "smooth_quartz", "smooth_quartz_stairs", 
            "smooth_quartz_slab", "amethyst_block", "podzol", "grass_block", 
            "mycelium", "dirt_path", "dirt", "coarse_dirt", "farmland", 
            "magma_block", "obsidian", "crying_obsidian", "soul_soil", "bone_block",
            "lapis_ore", "lapis_block", "deepslate_lapis_ore", "redstone_ore",
            "redstone_block", "deepslate_redstone_ore", "nether_quartz_ore",
            "diamond_block", "diamond_ore", "deepslate_diamond_ore", "glowstone",
            "budding_amethyst", "resin_block", "sculk", "sculk_catalyst",
            "red_concrete", "black_concrete", "white_concrete", "blue_concrete",
            "light_blue_concrete", "cyan_concrete", "orange_concrete", 
            "lime_concrete", "green_concrete", "purple_concrete", "magenta_concrete",
            "pink_concrete", "gray_concrete", "light_gray_concrete",
            "emerald_ore", "deepslate_emerald_ore", "emerald_block"
        );

        @Configure(path = CONFIG_PATH + "Sand", config = Config.ABILITIES)
        private List<String> sand = List.of(
            "minecraft:sand", "minecraft:concrete_powder", "gravel", "sandstone",
            "sandstone_stairs", "sandstone_slab", "sandstone_wall",
            "chiseled_sandstone", "smooth_sandstone", "smooth_sandstone_stairs",
            "smooth_sandstone_slab", "cut_sandstone", "cut_sandstone_slab", 
            "red_sandstone", "red_sandstone_stairs", "red_sandstone_slab", 
            "red_sandstone_wall", "chiseled_red_sandstone", "smooth_red_sandstone",
            "smooth_red_sandstone_stairs", "smooth_red_sandstone_slab", 
            "cut_red_sandstone", "cut_red_sandstone_slab", "clay", "soul_sand"
        );

        @Configure(path = CONFIG_PATH + "Metal", config = Config.ABILITIES)
        private List<String> metal = List.of(
            "minecraft:copper", "gilded_blackstone", "iron_block", "iron_ore",
            "deepslate_iron_ore", "gold_block", "gold_ore", "deepslate_gold_ore",
            "netherite_block", "iron_bars", "iron_chain", "raw_iron_block",
            "raw_copper_block", "raw_gold_block"
        );
    }
}
