package me.kwilson272.elementalmagic.core.gameplay.earth.earthsurf;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.revertible.TempBlock;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.gameplay.earth.EarthAbility;
import me.kwilson272.elementalmagic.core.revertible.TempFallingBlock;
import me.kwilson272.elementalmagic.core.util.Blocks;
import me.kwilson272.elementalmagic.core.util.Vectors;

/**
 * Many of the math concepts in this class come from the modern iteration of 
 * Jedcore. Credit is reserved for its developers and maintainters.
 */
public class EarthSurf extends EarthAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private long duration;
    private double height;
    private double springForce;
    private double dashSpeed;
    private double surfSpeed;
    private int filterSize;

    private boolean isSneak;
    private boolean isInfinite;
    private long endTime;
    private double speed;

    private Deque<Double> lowpassQueue;
    private Map<Block, TempBlock> activeBlocks;

	public EarthSurf(AbilityUser user, AbilityController controller, boolean isSneak) {
		super(user, controller);
        
        cooldown = CONFIG.cooldown;
        duration = CONFIG.duration;
        height = CONFIG.height;
        springForce = CONFIG.springForce;
        dashSpeed = CONFIG.dashSpeed;
        surfSpeed = CONFIG.surfSpeed;
        filterSize = CONFIG.filterSize;

        this.isSneak = isSneak;
        lowpassQueue = new ArrayDeque<>();
        activeBlocks = new HashMap<>();
	}

	@Override
	public boolean start() {
        Location loc = user().player().getLocation();
        Block base = getSafeBlock(loc.getBlock());
        if (base == null) {
            return false;
        }

        if (isSneak) {
            duration = 200; // Dash duration
            speed = dashSpeed;
        } else {
            speed = surfSpeed;
        }

        isInfinite = duration < 0;
        endTime = System.currentTimeMillis() + duration;
        return true;
	}

	@Override
	public boolean progress() {
        if (!user().canUse(controller(), true, false)
                || (!isInfinite && System.currentTimeMillis() > endTime)) {
            return false;
        }

        Location loc = user().player().getLocation();
        Block base = getSafeBlock(loc.getBlock());
        if (base == null) {
            return false;
        }
    
        double targY = base.getY() + height;
        double diffY = runLowPass(targY - loc.getY());
        if (Math.abs(diffY) > height + 2.0) {
            return false;
        }

        double yaw = user().player().getEyeLocation().getYaw();
        Vector dir = Vectors.fromRotations(0, yaw);
        applyVelocity(diffY, dir);
        animateSurf(dir);
        return !willCollide();
    }

    private double runLowPass(double height) {
        lowpassQueue.offerFirst(height);
        while (lowpassQueue.size() > filterSize) {
            lowpassQueue.pollLast();
        }

        double sum = 0;
        for (double d : lowpassQueue) {
            sum += d;
        }
        return sum / lowpassQueue.size();
    }

    private void applyVelocity(double heightDiff, Vector direction) {
        double yForce = heightDiff * springForce;
        Vector push = direction.clone().multiply(speed);

        Location loc = user().player().getLocation();
        Block inFront = loc.add(direction).getBlock();
        if (Blocks.isSolid(inFront)) {
            // Attempt to climb up the block if we weren't already
            yForce += 0.5;
        }

        if (Math.abs(yForce) > 0.5) {
            yForce = Math.copySign(0.5, yForce);
        }

        push.setY(yForce);
        Player player = user().player();
        ElementalMagicApi.effectHandler().setVelocity(player, this, push);
    }

    private boolean willCollide() {
        World world = user().player().getWorld();
        Vector start = user().player().getLocation().toVector();
        Vector dir = user().player().getVelocity();
        int len = (int) dir.length();
        dir.normalize();

        if (len <= 0) {
            return false;
        }

        BlockIterator iter = new BlockIterator(world, start, dir, 0, len);
        while (iter.hasNext()) {
            Block block = iter.next();
            if (Blocks.isSolid(block)) {
                return true;
            }
        }
        
        return false;
    }

    private void animateSurf(Vector dir) {
        Vector ortho = new Vector(dir.getZ(), 0, -dir.getX()); 
        Location center = user().player().getLocation().add(dir);

        for (int i = -1; i <= 1; ++i) {
            Location loc = center.clone().add(ortho.clone().multiply(i));
            Block block = getSafeBlock(loc.getBlock());
            if (block == null || activeBlocks.containsKey(block)) {
                continue;
            }

            Location spawn = block.getLocation().add(0.5, 0.4, 0.5);
            
            BlockData bData = block.getBlockData();
            BlockData airData = Material.AIR.createBlockData();
            TempBlock.builder(this, airData)
                .setDuration(700)
                .addRevertTask(tb -> activeBlocks.remove(tb.block()))
                .buildAt(block)
                .ifPresent(tb -> activeBlocks.put(tb.block(), tb));

            TempFallingBlock tfb = TempFallingBlock.builder(this, bData)
                .setCollidable(false)
                .setDuration(700)
                .buildAt(spawn);
            tfb.fallingBlock().setVelocity(new Vector(0, 0.3, 0));      
        }
    }

    private Block getSafeBlock(Block block) {
        int limit = (int) height + 2;
        for (int i = 0; i < limit; ++i) {
            Block above = block.getRelative(BlockFace.UP);
            if (activeBlocks.containsKey(block)) {
                return block;
            } if (Blocks.isSolid(block) && !Blocks.isSolid(above)) {
                break;
            } else if (Blocks.isSolid(above)) {
                block = above;
            } else {
                block = block.getRelative(BlockFace.DOWN);
            }
        }

        Block above = block.getRelative(BlockFace.UP);
        if (!Blocks.isSolid(block) || Blocks.isSolid(above) 
                || !isUsableEarth(block)) {
            return null;
        }

        return block;
    }

	@Override
	public void onDestruction() {
        user().addCooldown(name(), cooldown);
	}

	@Override
	public String name() {
        return "EarthSurf";
	}

    protected static class ConfigValues {
   
        private static final String CONFIG_PATH = EarthSurfController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 5600;
        @Configure(path = CONFIG_PATH + "Duration", config = Config.ABILITIES)
        private long duration = 3000;
        @Configure(path = CONFIG_PATH + "Height", config = Config.ABILITIES)
        private double height = 2.0;
        @Configure(path = CONFIG_PATH + "SpringForce", config = Config.ABILITIES)
        private double springForce = 0.4;
        @Configure(path = CONFIG_PATH + "DashSpeed", config = Config.ABILITIES)
        private double dashSpeed = 2.5;
        @Configure(path = CONFIG_PATH + "SurfSpeed", config = Config.ABILITIES)
        private double surfSpeed = 0.67;
        @Configure(path = CONFIG_PATH + "SmoothingFilterSize", config = Config.ABILITIES)
        private int filterSize = 5;
    }
}
