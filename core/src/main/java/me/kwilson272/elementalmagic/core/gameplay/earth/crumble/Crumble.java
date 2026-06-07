package me.kwilson272.elementalmagic.core.gameplay.earth.crumble;

import java.util.PriorityQueue;
import java.util.Queue;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.revertible.TempBlock;
import me.kwilson272.elementalmagic.api.revertible.TempBlock.TempBlockBuilder;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.gameplay.earth.EarthAbility;
import me.kwilson272.elementalmagic.core.util.Blocks;
import me.kwilson272.elementalmagic.core.util.Entities;

public class Crumble extends EarthAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private static final BlockFace[] TOUCH_FACES = {
        BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, 
        BlockFace.SOUTH, BlockFace.WEST, BlockFace.EAST
    };

    private long cooldown;
    private long duration;
    private double selectRange;
    private double radius;

    private int curRadius;
    private Location origin;
    private Queue<Block> conversionQueue;

    public Crumble(AbilityUser user, AbilityController controller) {
        super(user, controller);
        cooldown = CONFIG.cooldown;
        duration = CONFIG.duration;
        selectRange = CONFIG.selectRange;
        radius = CONFIG.radius;
        curRadius = 0;
    }

	@Override
	public boolean start() {
        Player player = user().player();
        Block block = Entities.getTargetBlock(player, selectRange, Blocks::isSolid);
        if (!Blocks.isSolid(block)) {
            return false;
        }

        origin = block.getLocation().add(0.5, 0.5, 0.5);
        conversionQueue = new PriorityQueue<>(this::compareDistance);
        collectBlocks(origin);
        if (conversionQueue.isEmpty()) {
            return false;
        }

        user().addCooldown(name(), cooldown);
        return true;
	}

    private int compareDistance(Block a, Block b) {
        double distA = a.getLocation().add(0.5, 0.5, 0.5).distanceSquared(origin);
        double distB = b.getLocation().add(0.5, 0.5, 0.5).distanceSquared(origin);
        // Should be fine to not return 0 for ==
        return distA <= distB ? -1 : 1;
    }

    private void collectBlocks(Location loc) {
        for (Block b : Blocks.collectSphere(loc, radius)) {
            if (Blocks.isEarth(b) && Blocks.canAbilityUse(b) && isUncovered(b)) {
                conversionQueue.add(b);
            }
        }
    }

    private boolean isUncovered(Block block) {
        for (BlockFace face : TOUCH_FACES) {
            if (!Blocks.isSolid(block.getRelative(face))) {
                return true;
            }
        }
        return false;
    }

	@Override
	public boolean progress() {
        if (!user().canUse(controller(), false, false)) {
            return false;
        }
       
        BlockData data = Material.SAND.createBlockData();
        TempBlockBuilder builder = TempBlock.builder(this, data)
            .setDuration(duration)
            .setUsable(true);

        curRadius++;
        double maxDist = curRadius * curRadius;
        while (!conversionQueue.isEmpty()) {
            Location loc = conversionQueue.peek().getLocation().add(0.5, 0.5, 0.5);
            double distSqrd = loc.distanceSquared(origin);
            if (distSqrd > maxDist) {
                break;
            }
            
            builder.buildAt(conversionQueue.remove());
        }

        playSandSound(origin);
        return curRadius < radius;
	}

	@Override
	public void onDestruction() {
	}

	@Override
	public String name() {
        return "Crumble";
	}

    protected static class ConfigValues {
        
        private static final String CONFIG_PATH = CrumbleController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 6200;
        @Configure(path = CONFIG_PATH + "Duration", config = Config.ABILITIES)
        private long duration = 8000;
        @Configure(path = CONFIG_PATH + "SelectRange", config = Config.ABILITIES)
        private double selectRange = 12;
        @Configure(path = CONFIG_PATH + "Radius", config = Config.ABILITIES)
        private double radius = 5.5;
    }
}
