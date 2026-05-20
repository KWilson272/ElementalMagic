package me.kwilson272.elementalmagic.core.gameplay.util;

import java.util.EnumSet;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.boat.JungleChestBoat;

public final class AbilityUtil {

    // TODO: this is horrifically ugly and should be made configurable
    public static final Set<Material> PLANT_MATERIALS = EnumSet.of(
            Material.SHORT_GRASS, Material.TALL_GRASS, Material.FERN, Material.LARGE_FERN,
            Material.DEAD_BUSH, Material.SEA_PICKLE, Material.SEAGRASS, Material.TALL_SEAGRASS,
            Material.KELP, Material.KELP_PLANT, Material.BAMBOO, Material.SUGAR_CANE,
            Material.CACTUS, Material.MOSS_CARPET, Material.PINK_PETALS, Material.VINE,
            Material.BIG_DRIPLEAF, Material.SMALL_DRIPLEAF, Material.GLOW_LICHEN, Material.CAVE_VINES,
            Material.SPORE_BLOSSOM, Material.TORCHFLOWER, Material.MUSHROOM_STEM, Material.BROWN_MUSHROOM,
            Material.RED_MUSHROOM, Material.BROWN_MUSHROOM_BLOCK, Material.RED_MUSHROOM_BLOCK, Material.WARPED_ROOTS,
            Material.CRIMSON_ROOTS, Material.NETHER_SPROUTS, Material.PUMPKIN, Material.MELON,
            Material.PUMPKIN_STEM, Material.ATTACHED_PUMPKIN_STEM, Material.MELON_STEM, Material.ATTACHED_MELON_STEM,
            Material.TWISTING_VINES, Material.WEEPING_VINES, Material.PITCHER_PLANT, Material.SWEET_BERRY_BUSH,
            Material.MOSS_BLOCK, Material.AZALEA, Material.FLOWERING_AZALEA, Material.MANGROVE_ROOTS,
            Material.MUDDY_MANGROVE_ROOTS, Material.LILY_PAD, Material.MANGROVE_PROPAGULE, Material.COCOA,
            Material.NETHER_WART, Material.NETHER_WART_BLOCK, Material.CAVE_VINES_PLANT
    );

    static {
        PLANT_MATERIALS.addAll(Tag.SAPLINGS.getValues());
        PLANT_MATERIALS.addAll(Tag.LEAVES.getValues());
        PLANT_MATERIALS.addAll(Tag.FLOWERS.getValues());
        PLANT_MATERIALS.addAll(Tag.CORAL_PLANTS.getValues());
        PLANT_MATERIALS.addAll(Tag.CROPS.getValues());
    }

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

    public static boolean isIce(Block block) {
        return Tag.ICE.isTagged(block.getType());
    }

    public static boolean isSnow(Block block) {
        Material type = block.getType();
        return type == Material.SNOW 
            || type == Material.POWDER_SNOW_CAULDRON 
            || type == Material.SNOW_BLOCK 
            || type == Material.POWDER_SNOW;
    }

    public static boolean isPlant(Block block) {
        return PLANT_MATERIALS.contains(block.getType());
    }

    public static BlockData getSolidPlant(Block block) {
        Material material = block.getType();
        if (material.isSolid()) {
            return block.getBlockData();
        }

        Material mat = switch(material) {
            case PALE_HANGING_MOSS: 
                yield Material.PALE_MOSS_BLOCK;
            case OAK_SAPLING:
                yield Material.OAK_LEAVES;
            case SPRUCE_SAPLING:
                yield Material.SPRUCE_LEAVES;
            case BIRCH_SAPLING:
                yield Material.BIRCH_LEAVES;
            case JUNGLE_SAPLING:
                yield Material.JUNGLE_LEAVES;
            case ACACIA_SAPLING:
                yield Material.ACACIA_LEAVES;
            case DARK_OAK_SAPLING:
                yield Material.DARK_OAK_LEAVES;
            case MANGROVE_PROPAGULE:
                yield Material.MANGROVE_LEAVES;
            case CHERRY_SAPLING:
                yield Material.CHERRY_LEAVES;
            case PALE_OAK_SAPLING:
                yield Material.PALE_OAK_LEAVES;
            case BROWN_MUSHROOM:
                yield Material.BROWN_MUSHROOM_BLOCK;
            case RED_MUSHROOM:
                yield Material.RED_MUSHROOM_BLOCK;
            case CRIMSON_FUNGUS:
            case CRIMSON_ROOTS:
            case NETHER_WART:
            case WEEPING_VINES:
                yield Material.NETHER_WART_BLOCK;
            case WARPED_FUNGUS:
            case TWISTING_VINES:
            case WARPED_ROOTS:
            case NETHER_SPROUTS:
                yield Material.WARPED_WART_BLOCK;
            case MELON_STEM:
                yield Material.MELON;
            case PUMPKIN_STEM:
                yield Material.PUMPKIN;
            case WHEAT:
                yield Material.HAY_BLOCK;
            case KELP:
                yield Material.DRIED_KELP_BLOCK;
            default:
                yield Material.OAK_LEAVES;
        };

        return mat.createBlockData(); 
    }
}
