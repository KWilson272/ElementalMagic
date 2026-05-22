package me.kwilson272.elementalmagic.core.gameplay.water.waterflow;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.collision.AABB;
import me.kwilson272.elementalmagic.api.collision.BoundingVolume;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.revertible.TempBlock;
import me.kwilson272.elementalmagic.api.revertible.TempBlock.TempBlockBuilder;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.api.util.BlockUtil;
import me.kwilson272.elementalmagic.core.ability.CoreAbility;
import me.kwilson272.elementalmagic.core.gameplay.util.EntityUtil;
import me.kwilson272.elementalmagic.core.gameplay.util.VectorUtil;
import me.kwilson272.elementalmagic.core.gameplay.util.WaterSourceOptions;
import me.kwilson272.elementalmagic.core.gameplay.util.WaterUtil;

public class WaterFlow extends CoreAbility {
    
    protected static final ConfigValues CONFIG = new ConfigValues();
    
    private long cooldown;
    private long duration;
    private double selectRange;
    private double minHoldRange;
    private double maxHoldRange;
    private double radius;
    private int maxBlocks;
    private double hitboxSize;
    private long freezeDuration;

    private boolean isInifinte;
    private long endTime;

    private double health;

    private Location location;
    private Deque<TempBlock> blocks;
    private Map<Block, Vector> directions;

	public WaterFlow(AbilityUser user, AbilityController controller) {
		super(user, controller);
    
        cooldown = CONFIG.cooldown;
        duration = CONFIG.duration;
        selectRange = CONFIG.selectRange;
        minHoldRange = CONFIG.minHoldRange;
        maxHoldRange = CONFIG.maxHoldRange;
        radius = CONFIG.radius;
        maxBlocks = CONFIG.maxBlocks;
        hitboxSize = CONFIG.hitboxSize;
        freezeDuration = CONFIG.freezeDuration;
        
        health = user().player().getHealth();

        blocks = new ArrayDeque<>();
        directions = new HashMap<>();
	}

	@Override
	public boolean start() {
        var opts = new WaterSourceOptions(user()).noPlant().noIce().noSnow();
        Block source = WaterUtil.getSourceBlock(user(), selectRange, opts); 
        if (source == null) {
            return false;
        }

        isInifinte = duration < 0;
        endTime = System.currentTimeMillis() + duration;
        location = source.getLocation().add(0.5, 0.5, 0.5);
        return true;
	}   

	@Override
	public boolean progress() {
        if (!user().canUse(controller(), false, false)
                || !user().getSelectedBindName().equals("WaterManipulation")
                || (!isInifinte && System.currentTimeMillis() > endTime)) {
            return false;
        }

        Player player = user().player();
        if (player.getHealth() < health) {
            return false;
        } else {
            health = player.getHealth();
        }

        double holdRange = player.isSneaking() ? minHoldRange : maxHoldRange;
        Location targetLoc = EntityUtil.getTarget(player, holdRange);

        moveTo(targetLoc);
        revertBlocks(blocks.size() - maxBlocks);
        affectEntities();
        WaterUtil.playWaterSound(location);

        return true;
    }

    private void moveTo(Location target) {
        Block block = location.getBlock();
        Block targBlock = target.getBlock();
        if (block.equals(targBlock)) {
            return;
        }

        Vector dir = VectorUtil.getDirection(location, target);
        location.add(dir.normalize());

        BlockData data = Material.WATER.createBlockData();
        TempBlockBuilder blockBuilder = TempBlock.builder(this, data);
        
        for (Block b : BlockUtil.collectSphere(location, radius)) {
            blockBuilder.buildAt(b).ifPresent(tb -> {
                blocks.offerFirst(tb);
                directions.put(b, dir);
            });
        }
    }

    private void revertBlocks(int amount) {
        for (int i = 0; i < amount; ++i) {
            TempBlock tb = blocks.pollLast();
            if (TempBlock.isActive(tb)) {
                directions.remove(tb.block());
            }

            ElementalMagicApi.revertibleManager().revert(tb);
        }
    }

    private void affectEntities() {
        for (Block block : directions.keySet()) {
            Location loc = block.getLocation().add(0.5, 0.5, 0.5);
            // Math taken from JedCore to preserve 'feel'. All credit due to
            // JedK1 and the JedCore maintainers.
            Vector dir = directions.get(block).clone().multiply(1.5);
            Location projection = loc.add(dir);

            World world = block.getWorld();
            BoundingVolume bv = AABB.fromBlock(block, hitboxSize);
            for (Entity e : EntityUtil.getNearbyEntities(world, bv)) {
                Location eLoc = e.getLocation();
                Vector vec = VectorUtil.getDirection(eLoc, projection).normalize();
                ElementalMagicApi.effectHandler().setVelocity(e, this, vec, 1); 
            }
        }
    }

    protected void freeze() {
        // The randomized breaking will prevent server owners from removing
        // freeze entirely unless we do this
        if (freezeDuration <= 0) {
            return;
        }

        BlockData data = Material.ICE.createBlockData();
        TempBlockBuilder iceBuilder = TempBlock.builder(this, data)
            .addRevertTask(this::playBreakEffect);
        for (TempBlock tb : blocks) {
            long time = ThreadLocalRandom.current().nextLong(500) + freezeDuration;
            iceBuilder.setDuration(time).buildAt(tb.block());
        }
    }

    private void playBreakEffect(TempBlock tb) {
        World world = tb.block().getWorld();
        Location loc = tb.block().getLocation().add(0.5, 0.5, 0.5);
        BlockData data = Material.ICE.createBlockData();

        world.spawnParticle(Particle.BLOCK, loc, 10, 0, 0, 0, data);
        world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1, 1.3f);
    }

	@Override
	public void onDestruction() {
        revertBlocks(blocks.size());
        user().addCooldown("WaterFlow", cooldown);
	}
    
    protected static class ConfigValues {
    
        private static final String CONFIG_PATH = WaterFlowController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 6000;
        @Configure(path = CONFIG_PATH + "Duration", config = Config.ABILITIES)
        private long duration = 3000;
        @Configure(path = CONFIG_PATH + "SelectRange", config = Config.ABILITIES)
        private double selectRange = 80;
        @Configure(path = CONFIG_PATH + "MinHoldRange", config = Config.ABILITIES)
        private double minHoldRange = 6;
        @Configure(path = CONFIG_PATH + "MaxHoldRange", config = Config.ABILITIES)
        private double maxHoldRange = 10;
        @Configure(path = CONFIG_PATH + "Radius", config = Config.ABILITIES)
        private double radius = 2.0;
        @Configure(path = CONFIG_PATH + "MaxBlocks", config = Config.ABILITIES)
        private int maxBlocks = 21;
        @Configure(path = CONFIG_PATH + "HitboxSize", config = Config.ABILITIES)
        private double hitboxSize = 2.5;
        @Configure(path = CONFIG_PATH + "FreezeDuration", config = Config.ABILITIES) 
        private long freezeDuration = 6500;
    }
}
