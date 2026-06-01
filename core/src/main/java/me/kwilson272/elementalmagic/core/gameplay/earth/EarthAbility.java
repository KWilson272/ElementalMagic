package me.kwilson272.elementalmagic.core.gameplay.earth;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.ability.CoreAbility;
import me.kwilson272.elementalmagic.core.util.Blocks;
import me.kwilson272.elementalmagic.core.util.Entities;

public abstract class EarthAbility extends CoreAbility {

	public EarthAbility(AbilityUser user, AbilityController controller) {
		super(user, controller);
	}

    /**
     * Checks if a {@link Block} is earth that abilities can use. This function
     * considers the blocks from {@link Blocks.isEarth}, {@link Blocks.isSand},
     * and {@link Blocks.isMetal} to all be earth.
     *
     * @param block the {@code Block}.
     * @return true if the block is usable earth, false otherwise.
     */
    public static boolean isUsableEarth(Block block) {
        return Blocks.canAbilityUse(block) 
            && (Blocks.isEarth(block)
            || Blocks.isSand(block)
            || Blocks.isMetal(block));
    }

    /**
     * Checks if a {@link Block} is sand that abilities can use.
     *
     * @param block the {@code Block}.
     * @return true if the block is usable sand, false otherwise.
     */
    public static boolean isUsableSand(Block block) {
        return Blocks.isSand(block) && Blocks.canAbilityUse(block);
    }

    /**
     * Checks if a {@link Block} is metal that abilities can use.
     *
     * @param block the {@code Block}.
     * @return true if the block is usable metal, false otherwise.
     */
    public static boolean isUsableMetal(Block block) {
        return Blocks.isMetal(block) && Blocks.canAbilityUse(block); 
    }

    /**
     * Checks if a {@link Block} is lava that abilities can use.
     *
     * @param block the {@code Block}.
     * @return true if the block is usable lava, false otherwise.
     */
    public static boolean isUsableLava(Block block) {
        return Blocks.isLava(block) && Blocks.canAbilityUse(block);
    }

    /**
     * Selects the first visible {@link Block} in the select range for which
     * {@link isUsableEarth} returns true.
     *
     * @param selectRange the Double max distance the chosen block can be.
     * @return the found {@code Block}, null if one could not be found.
     */
    public Block selectSource(double selectRange) {
        Player player = user().player();
        Block block = Entities.getTargetBlock(player, selectRange, 
                b -> Blocks.isSolid(b) || isUsableEarth(b)
        );
        return isUsableEarth(block) ? block : null;
    }

        /**
     * Selects the first visible {@link Block} in the select range for which
     * {@link isUsableSand} returns true.
     *
     * @param selectRange the Double max distance the chosen block can be.
     * @return the found {@code Block}, null if one could not be found.
     */
    public Block selectSandSource(double selectRange) {
        Player player = user().player();
        Block block = Entities.getTargetBlock(player, selectRange, 
                b -> Blocks.isSolid(b) || isUsableSand(b)
        );
        return isUsableSand(block) ? block : null;
    }

    /**
     * Selects the first visible {@link Block} in the select range for which
     * {@link isUsableMetal} returns true.
     *
     * @param selectRange the Double max distance the chosen block can be.
     * @return the found {@code Block}, null if one could not be found.
     */
    public Block selectMetalSource(double selectRange) {
        Player player = user().player();
        Block block = Entities.getTargetBlock(player, selectRange, 
                b -> Blocks.isSolid(b) || isUsableMetal(b)
        );
        return isUsableMetal(block) ? block : null;
    }

    /**
     * Selects the first visible {@link Block} in the select range for which
     * {@link isUsableLava} returns true.
     *
     * @param selectRange the Double max distance the chosen block can be.
     * @return the found {@code Block}, null if one could not be found.
     */
    public Block selectLavaSource(double selectRange) {
        Player player = user().player();
        Block block = Entities.getTargetBlock(player, selectRange, 
                b -> Blocks.isSolid(b) || isUsableLava(b)
        );
        return isUsableLava(block) ? block : null;
    }

    public static void playEarthSound(Location location) {
        World world = location.getWorld();
        world.playSound(location, Sound.ENTITY_GHAST_SHOOT, 1, 1);
    }

    public static void playSandSound(Location location) {
        World world = location.getWorld();
        world.playSound(location, Sound.BLOCK_SAND_BREAK, 1, 1);
    }

    public static void playMetalSound(Location location) {
        World world = location.getWorld();
        world.playSound(location, Sound.BLOCK_ANVIL_FALL, 1, 0.6f);
    }

    public static void playLavaSound(Location location) {
        World world = location.getWorld();
        world.playSound(location, Sound.BLOCK_LAVA_AMBIENT, 1, 0.7f);
    }

    /**
     * @return the {@link BlockData} to indicate the provided block is selected
     * as a source.
     */
    public static BlockData getSourceFocusData(Block block) {
        if (block.getType() == Material.STONE) {
            return Material.COBBLESTONE.createBlockData();
        } else {
            return Material.STONE.createBlockData();
        }
    }
}
