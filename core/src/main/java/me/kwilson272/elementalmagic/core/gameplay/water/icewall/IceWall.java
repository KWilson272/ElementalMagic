package me.kwilson272.elementalmagic.core.gameplay.water.icewall;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.ability.AbilityManager;
import me.kwilson272.elementalmagic.api.collision.AABB;
import me.kwilson272.elementalmagic.api.collision.BoundingVolume;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.effect.EffectHandler;
import me.kwilson272.elementalmagic.api.revertible.RevertibleManager;
import me.kwilson272.elementalmagic.api.revertible.TempBlock;
import me.kwilson272.elementalmagic.api.revertible.TempBlock.TempBlockBuilder;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.api.util.BlockUtil;
import me.kwilson272.elementalmagic.core.ability.CoreAbility;
import me.kwilson272.elementalmagic.core.gameplay.util.EntityUtil;
import me.kwilson272.elementalmagic.core.gameplay.util.WaterSourceOptions;
import me.kwilson272.elementalmagic.core.gameplay.util.WaterUtil;

public class IceWall extends CoreAbility {
    
    protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private long duration;
    private double selectRange;
    private double damage;
    private double hitboxSize;
    private int width;
    private int minHeight;
    private int maxHeight;

    private boolean isInfinite;
    private long endTime;
    
    private boolean isRising;
    private boolean isShattered;
    private Map<Block, Integer> riseBlocks;
    private Map<Block, TempBlock> wallBlocks;

	public IceWall(AbilityUser user, AbilityController controller) {
		super(user, controller);

        cooldown = CONFIG.cooldown;
        duration = CONFIG.duration;
        selectRange = CONFIG.selectRange;
        damage = CONFIG.damage;
        hitboxSize = CONFIG.hitboxSize;
        width = CONFIG.width;
        minHeight = CONFIG.minHeight;
        maxHeight = CONFIG.maxHeight;

        isRising = true;
        isShattered = false;
        riseBlocks = new HashMap<>();
        wallBlocks = new HashMap<>();
	}

	@Override
	public boolean start() {
        if (!user().canUse(controller(), true, false) || popOtherIcewall()) {
            return false;
        }
        
        if (user().isOnCooldown("IceWall")) {
            return false;
        }

        var opts = new WaterSourceOptions(user()).noPlant();
        Block source = WaterUtil.getSourceBlock(user(), selectRange, opts);
        if (source == null) {
            return false;
        }

        setupWallBlocks(source);
        if (riseBlocks != null) {
            user().addCooldown("IceWall", cooldown);
            return true;
        }
        return false;
	}

    private boolean popOtherIcewall() {
        Block target = BlockUtil.getTargetBlock(user().player(),
                selectRange, BlockUtil::isSolid);
        
        AbilityManager manager = ElementalMagicApi.abilityManager();
        IceWall toPop = manager.getAllOf(IceWall.class)
            .filter(iw -> iw.wallBlocks.containsKey(target) && iw.canShatter())
            .findFirst()
            .orElse(null);

        if (toPop != null) {
            toPop.shatter(user());
            return true;
        }
        return false;
    }

    private boolean canShatter() {
        return !isRising && !isShattered;
    }

    private void shatter(AbilityUser user) {
        damageEntities(user);
        playShatterEffect();
        revertWall();
        isShattered = true;
    }

    private void damageEntities(AbilityUser user) {
        Set<LivingEntity> noAffect = new HashSet<>();
        if (user != null) {
            noAffect.add(user.player());
        }

        EffectHandler effectHandler = ElementalMagicApi.effectHandler();
        for (LivingEntity le : getAffectedEntities()) {
            if (!noAffect.contains(le)) {
                 effectHandler.damageEntity(le, this, damage);
                 le.setNoDamageTicks(0);
                 noAffect.add(le);
            }
        }
    }

    private Collection<LivingEntity> getAffectedEntities() {
        Set<LivingEntity> entities = new HashSet<>();
        for (Block block : wallBlocks.keySet()) {
            BoundingVolume bv = AABB.fromBlock(block, hitboxSize);
            for (Entity e : EntityUtil.getNearbyEntities(block.getWorld(), bv)) {
                if (e instanceof LivingEntity le) {
                    entities.add(le);
                }
            }
        }

        return entities;
    }

    private void playShatterEffect() {
        for (Block block : wallBlocks.keySet()) {
            World world = block.getWorld();
            BlockData data = block.getBlockData();
            Particle particle = Particle.BLOCK;
           
            Location loc = block.getLocation().add(0.5, 0.5, 0.5);
            world.spawnParticle(particle, loc, 3, data);
            if (ThreadLocalRandom.current().nextInt(5) == 0) {
                world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.3f);
            }
        }
    }

    private void revertWall() {
        RevertibleManager manager = ElementalMagicApi.revertibleManager();
        wallBlocks.values().forEach(manager::revert);
        wallBlocks.clear(); 
    }

    private void setupWallBlocks(Block source) {
        Vector vec = getWallPlane();
        Location loc = source.getLocation().add(0.5, 0.5, 0.5);
        loc.subtract(vec.clone().multiply(width/2));

        int inc = 1;
        int height = minHeight;
        for (int i = 0; i < width; ++i) {
            Block block = loc.getBlock();
            loc.add(vec);

            Block base = getWallBase(block);
            if (base != null) {
                riseBlocks.put(block, height);
            }

            height += inc;
            if (height == maxHeight) {
                inc *= -1;
            }
        }
    }

    private Block getWallBase(Block block) {
        var opts = new WaterSourceOptions(user()).noPlant();
        int limit = 15;
        
        for (int i = 0; i < limit; ++i) {
            Block above = block.getRelative(BlockFace.UP); 
            if (WaterUtil.canUse(above, opts)) {
                block = above;
            } else if (WaterUtil.canUse(block, opts)) {
                return block;
            } else {
                block = block.getRelative(BlockFace.DOWN);
            }
        }

        return null;
    }

    private Vector getWallPlane() {
        Location eyeLoc = user().player().getEyeLocation();
        double yaw = Math.toRadians(eyeLoc.getYaw() + 90);
        double x = -Math.sin(yaw);
        double z = Math.cos(yaw);
        return new Vector(x, 0, z);
    }

	@Override
	public boolean progress() {
        if (!user().canUse(controller(), false, false) || isShattered) {
            return false;
        }

        if (isRising) {
            manageRise();
            if (riseBlocks.isEmpty()) {
                isInfinite = duration < 0;
                endTime = System.currentTimeMillis() + duration;
                isRising = false;
            }

        } else if (!isInfinite && System.currentTimeMillis() > endTime) {
            return false;
        
        } else {
            // Prevents invisible icewalls 
            removeExpiredBlocks();
            return !wallBlocks.isEmpty();
        }
        return true;
	}

    private void manageRise() {
        BlockData iceData = Material.ICE.createBlockData();
        TempBlockBuilder iceBuilder = TempBlock.builder(this, iceData);

        // Only the highest pillars so the wall looks like it is rising 
        int highest = 0;
        for (Integer height : riseBlocks.values()) {
            if (height > highest) {
                highest = height;
            }
        }

        Map<Block, Integer> newRise = new HashMap<>();
        for (Block block : riseBlocks.keySet()) {
            if (riseBlocks.get(block) < highest) {
                newRise.put(block, riseBlocks.get(block));
                continue;
            }

            if (BlockUtil.isSolid(block) && !WaterUtil.canUse(block, user())) {
                continue;
            }

            iceBuilder.buildAt(block).ifPresent(tb -> {
                wallBlocks.put(tb.block(), tb);
                if (ThreadLocalRandom.current().nextInt(5) == 0) {
                    WaterUtil.playIceSound(tb.block().getLocation());
                }
            });
                
            int height = riseBlocks.get(block) - 1;
            if (height > 0) {
                newRise.put(block.getRelative(BlockFace.UP), height);
            }
        }

        riseBlocks = newRise;
    }

    private void removeExpiredBlocks() {
        Iterator<TempBlock> iter = wallBlocks.values().iterator();
        while (iter.hasNext()) {
            TempBlock tb = iter.next();
            if (tb.isReverted()) {
                iter.remove();
            }
        }
    }

	@Override
	public void onDestruction() {
        revertWall();
	}

    protected static class ConfigValues {
        
        private static final String CONFIG_PATH = IceWallController.CONFIG_PATH; 

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 3250;
        @Configure(path = CONFIG_PATH + "Duration", config = Config.ABILITIES)
        private long duration = -1;
        @Configure(path = CONFIG_PATH + "SelectRange", config = Config.ABILITIES)
        private double selectRange = 22.0;
        @Configure(path = CONFIG_PATH + "Damage", config = Config.ABILITIES)
        private double damage = 3.0;
        @Configure(path = CONFIG_PATH + "HitboxSize", config = Config.ABILITIES)
        private double hitboxSize = 1.5;
        @Configure(path = CONFIG_PATH + "Width", config = Config.ABILITIES)
        private int width = 5;
        @Configure(path = CONFIG_PATH + "MinHeight", config = Config.ABILITIES)
        private int minHeight = 5;
        @Configure(path = CONFIG_PATH + "MaxHeight", config = Config.ABILITIES)
        private int maxHeight = 8;
    }
}
