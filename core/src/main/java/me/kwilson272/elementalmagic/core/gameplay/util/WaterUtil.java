package me.kwilson272.elementalmagic.core.gameplay.util;

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
import me.kwilson272.elementalmagic.api.ability.Ability;
import me.kwilson272.elementalmagic.api.revertible.RevertibleManager;
import me.kwilson272.elementalmagic.api.revertible.TempBlock;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.api.util.BlockUtil;

public final class WaterUtil {

    /**
     * Checks if the AbilityUser can use the provided block for water 
     * abilities.
     *
     * @param block the {@link Block} being checked.
     * @param user the {@link AbilityUser} using the block.
     * return true if the block is usable, false otherwise.
     */
    public static boolean canUse(Block block, AbilityUser user) {
        return canUse(block, new WaterSourceOptions(user));
    }

    /**
     * Checks if the provided block can be used given the source options.
     *
     * @param block the {@link Block} being checked.
     * @param opts the {@link WaterSourceOptions}.
     * return true if the block is usable, false otherwise.
     */
    public static boolean canUse(Block block, WaterSourceOptions opts) {
        if (!TempBlock.isUsableTempBlock(block)) {
            return false;
        }

        return (opts.useWater() && AbilityUtil.isWater(block)) 
            || (opts.useIce() && AbilityUtil.isIce(block))
            || (opts.useSnow() && AbilityUtil.isSnow(block))
            || (opts.usePlant() && AbilityUtil.isPlant(block));
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

        if (AbilityUtil.isWater(block)) {
            // Don't drain if there are sources around, it looks weird.
            // 3 because PK used it and it worked well.
            return getAdjacentSources(block) >= 3 ? 
                Material.WATER.createBlockData() : Material.AIR.createBlockData();
        }
    
        return AbilityUtil.isIce(block) ?
            Material.WATER.createBlockData() : Material.AIR.createBlockData();
    }
    
    /**
     * Consumes the provided source and replaces it with a drained
     * {@link TempBlock}. This function does not check if the provided 
     * {@link Block} is usable.
     *
     * @param ability the responsible {@link Ability}.
     * @param block the {@link Block} being consumed.
     * @param revertTime the Long revert time of the TempBlock in milliseconds.
     */
    public static void consumeSource(Ability ability, Block block, long revertTime) {
        RevertibleManager revertManager = ElementalMagicApi.revertibleManager();
        // We only revert TempBlocks if they're ice because
        // 1. If a water block reverts from liquid -> solid, it looks bad
        // 2. A lot of the ice blocks created in this ability set are created over
        // air. If we make a new water TempBlock over an ice block, and the chunk
        // of user-made ice gets reverted, we're left with floating water (bad!)
        if (AbilityUtil.isIce(block) && TempBlock.isTempBlock(block)) {
            TempBlock.get(block).ifPresent(revertManager::revert);
            return;
        }

        BlockData data = getDrainedData(block);
        TempBlock.builder(ability, data).setUsable(true)
                .setDuration(revertTime).buildAt(block);
    }
    
    /**
     * @return the number of adjacent water blocks to the provided.
     */
    public static int getAdjacentSources(Block block) {
        int count = 0;
        Location center = block.getLocation().add(0.5, 0.5, 0.5);
        for (Block check : BlockUtil.collectCube(center, 1)) {
            if (AbilityUtil.isWater(check) && !check.equals(block)) {
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

    public static Block getSourceBlock(AbilityUser user, double range) {
        return getSourceBlock(user, range, new WaterSourceOptions(user));
    }

    public static Block getSourceBlock(AbilityUser user, double range, WaterSourceOptions opts) {
        Block block = BlockUtil.getTargetBlock(user.player(), range,
                b -> BlockUtil.isSolid(b) || canUse(b, opts));
        return canUse(block, opts) ? block : null;
    }
}
