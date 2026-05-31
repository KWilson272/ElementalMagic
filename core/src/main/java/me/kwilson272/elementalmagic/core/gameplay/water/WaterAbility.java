package me.kwilson272.elementalmagic.core.gameplay.water;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Waterlogged;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.revertible.RevertibleManager;
import me.kwilson272.elementalmagic.api.revertible.TempBlock;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.ability.CoreAbility;
import me.kwilson272.elementalmagic.core.util.Blocks;
import me.kwilson272.elementalmagic.core.util.Entities;

public abstract class WaterAbility extends CoreAbility {

    public WaterAbility(AbilityUser user, AbilityController controller) {
		super(user, controller);
	}

	/**
     * Checks if an {@link AbilityUser} can use the provided {@link Block}
     * for water abilities. This function checks usability based on all 
     * water-related elements the user can currently use.
     *
     * @param block the {@code Block}. 
     * @param user the {@code AbilityUser}.
     * return true if the block can be used, false otherwise.
     */
    public static boolean canUse(Block block, AbilityUser user) {
        return canUse(block, WaterUsePolicy.from(user));
    }

    /**
     * Checks if a {@link Block} can be used given a {@link WaterUsePolicy}.
     *
     * @param block the {@code Block}. 
     * @param opts the {@code WaterUsePolicy}.
     * return true if the block can be used, false otherwise.
     */
    public static boolean canUse(Block block, WaterUsePolicy opts) {
        if (!Blocks.canAbilityUse(block)) {
            return false;
        }

        return (opts.useWater() && Blocks.isWater(block)) 
            || (opts.useIce() && Blocks.isIce(block))
            || (opts.useSnow() && Blocks.isSnow(block))
            || (opts.usePlant() && Blocks.isPlant(block));
    }

    /**
     * Checks if the provided {@link block} is considered usable water by
     * abilities.
     *
     * @param block the {@code Block}.
     * @return true if the block is both usable and water, false otherwise.
     */
    public static boolean isUsableWater(Block block) {
        return Blocks.isWater(block) && Blocks.canAbilityUse(block);
    }

    /**
     * Checks if the provided {@link block} is considered usable ice by
     * abilities.
     *
     * @param block the {@code Block}.
     * @return true if the block is both usable and ice, false otherwise.
     */
    public static boolean isUsableIce(Block block) {
        return Blocks.isIce(block) && Blocks.canAbilityUse(block);
    }

    /**
     * Checks if the provided {@link block} is considered usable snow by
     * abilities.
     *
     * @param block the {@code Block}.
     * @return true if the block is both usable and snow false otherwise.
     */
    public static boolean isUsableSnow(Block block) {
        return Blocks.isSnow(block) && Blocks.canAbilityUse(block);
    }

    /**
     * Checks if the provided {@link block} is considered usable plant by
     * abilities.
     *
     * @param block the {@code Block}.
     * @return true if the block is both usable and plant, false otherwise.
     */
    public static boolean isUsablePlant(Block block) {
        return Blocks.isPlant(block) && Blocks.canAbilityUse(block);
    }

    /**
     * Gets the water-filled {@link BlockData} equivalent for the provided 
     * {@link Block}. In the case the block is non-fillable, this method will 
     * just return {@code Material.Water}.
     *
     * @param block the {@code Block} being filled.
     * @param level the Integer fill level.
     * @return the water-filled {@code BlockData}.
     */
    public static BlockData getFilledData(Block block, int level) {
        Material type = block.getType();
        BlockData data = block.getBlockData();

        if (data instanceof Waterlogged wl) {
            wl.setWaterlogged(true);
            return wl;
        }

        // These types are essentially water but their level isn't controllable
        if (type == Material.BUBBLE_COLUMN
            || type == Material.KELP 
            || type == Material.KELP_PLANT 
            ||type == Material.SEAGRASS 
            || type == Material.TALL_SEAGRASS) {
            // Already 'full'
            return data;
        }

        if (type == Material.CAULDRON) {
            data = Material.WATER_CAULDRON.createBlockData();
        } else if (!(data instanceof Levelled) 
                || type == Material.COMPOSTER 
                || type == Material.LAVA 
                || type == Material.LAVA_CAULDRON 
                || type == Material.POWDER_SNOW_CAULDRON) {
            data = Material.WATER.createBlockData();
        }

        ((Levelled) data).setLevel(level);
        return data;
    }
    
    /**
     * Gets the drained {@link BlockData} equivalent for the provided
     * {@link Block}. In the case the block cannot be drained, this method
     * will return {@code Material.Water}.
     *
     * @param block the {@code Block} being drained.
     * @return the drained {@code BlockData}.
     */
    public static BlockData getDrainedData(Block block) {
        Material type = block.getType();
        BlockData data = block.getBlockData();

        if (type == Material.WATER_CAULDRON 
                || type == Material.POWDER_SNOW_CAULDRON) {
            return Material.CAULDRON.createBlockData();
        }

        if (data instanceof Waterlogged wl) {
            wl.setWaterlogged(false);
            return wl;
        }

        if (Blocks.isWater(block)) {
            // Don't drain if there are sources around, it looks weird.
            // 3 because PK used it and it worked well.
            return getAdjacentSources(block) >= 3 ? 
                Material.WATER.createBlockData() : Material.AIR.createBlockData();
        }
    
        return Blocks.isIce(block) ?
            Material.WATER.createBlockData() : Material.AIR.createBlockData();
    }
    
    /**
     * Gets the 'solid' {@link BlockData} for a {@link Block}.
     *
     * @param block the {@code Block}.
     * @return the solidified {@code BlockData}, or the block's data
     * if the block was already solid.
     */
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

    /**
     * Consumes the provided source and replaces it with a drained
     * {@link TempBlock}. This function does not check if the provided 
     * {@link Block} is usable.
     *
     * @param block the {@link Block} being consumed.
     * @param revertTime the Long revert time of the TempBlock in milliseconds.
     */
    public void consumeSource(Block block, long revertTime) {
        RevertibleManager revertManager = ElementalMagicApi.revertibleManager();
        // We only revert TempBlocks if they're ice because
        // 1. If a water block reverts from liquid -> solid, it looks bad
        // 2. A lot of the ice blocks created in this ability set are created over
        // air. If we make a new water TempBlock over an ice block, and the chunk
        // of user-made ice gets reverted, we're left with floating water (bad!)
        if (Blocks.isIce(block) && TempBlock.isTempBlock(block)) {
            TempBlock.get(block).ifPresent(revertManager::revert);
            return;
        }

        BlockData data = getDrainedData(block);
        TempBlock.builder(this, data).setUsable(true)
                .setDuration(revertTime).buildAt(block);
    }
    
    /**
     * @return the number of adjacent water blocks to the provided.
     */
    public static int getAdjacentSources(Block block) {
        int count = 0;
        Location center = block.getLocation().add(0.5, 0.5, 0.5);
        for (Block check : Blocks.collectCube(center, 1)) {
            if (Blocks.isWater(check) && !check.equals(block)) {
                ++count;
            }
        }
        return count;
    }

    public static void playSourceSelectedEffect(Block block) {
        World world = block.getWorld();
        // Center and raise to make the particle visible in opaque blocks
        Location loc = block.getLocation().add(0.5, 1, 0.5);
        world.spawnParticle(Particle.SMOKE, loc, 1, 0, 0, 0, 0);
    }

    public static void playWaterSound(Location location) {
        World world = location.getWorld();
        if (world != null) {
            world.playSound(location, Sound.BLOCK_WATER_AMBIENT, 1, 1);
        }
    }

    public static void playIceSound(Location location) {
        World world = location.getWorld();
        if (world != null) {
            world.playSound(location, Sound.ITEM_FLINTANDSTEEL_USE, 1, 2);
        }
    }

    public static void playSnowSound(Location location) {
        World world = location.getWorld();
        if (world != null) {
            world.playSound(location, Sound.BLOCK_POWDER_SNOW_BREAK, 1, 1);
        }
    }

    public static void playPlantSound(Location location) {
        World world = location.getWorld();
        if (world != null) {
            world.playSound(location, Sound.BLOCK_GRASS_BREAK, 1, 1);
        }
    }

    /**
     * Gets the {@link Block} the user of this ability is targeting, and 
     * returns it. If no usable block could be found, or no water-related
     * blocks could be found, this method will return null.
     *
     * <p> Usability is determined by all water-related elements
     * the user can use.
     * @param range the Double target range.
     * @return a {@code Block} if one was found, null otherwise.
     */
    public Block selectSourceBlock(double range) {
        return selectSourceBlock(range, WaterUsePolicy.from(user()));
    }

    /**
     * Gets the {@link Block} the user of this ability is targeting, and 
     * returns it. If no usable block could be found, or no water-related
     * blocks could be found, this method will return null. 
     * 
     * <p> Usability is determined by the passed {@link WaterUsePolicy} object.
     *
     * @param range the Double select range.
     * @param opts the {@code WaterUsePolicy} determining usability.
     * @return a {@code Block} if one was found, null otherwise.
     */
    public Block selectSourceBlock(double range, WaterUsePolicy opts) {
        Block block = Entities.getTargetBlock(user().player(), range,
                b -> Blocks.isSolid(b) || canUse(b, opts));
        return canUse(block, opts) ? block : null;
    }
}

