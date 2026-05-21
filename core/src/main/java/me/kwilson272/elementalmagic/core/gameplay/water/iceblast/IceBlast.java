package me.kwilson272.elementalmagic.core.gameplay.water.iceblast;

import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.collision.AABB;
import me.kwilson272.elementalmagic.api.collision.BoundingVolume;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.effect.EffectHandler;
import me.kwilson272.elementalmagic.api.revertible.TempBlock;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.api.util.BlockUtil;
import me.kwilson272.elementalmagic.core.ability.CoreAbility;
import me.kwilson272.elementalmagic.core.gameplay.util.EntityUtil;
import me.kwilson272.elementalmagic.core.gameplay.util.VectorUtil;
import me.kwilson272.elementalmagic.core.gameplay.util.WaterSourceOptions;
import me.kwilson272.elementalmagic.core.gameplay.util.WaterUtil;

public class IceBlast extends CoreAbility {
    
    protected static final ConfigValues CONFIG = new ConfigValues();
    
    private long cooldown;
    private double selectRange;
    private long sourceRevertTime;
    private double range;
    private double speed;
    private double damage;
    private double hitboxSize;
    private int slowPower;
    private int slowDuration;

    private boolean isSourced;
    private Block source;

    private boolean isRising;
    private Block riseDest;
    private Location location;
    private Vector finalDirection;

    private TempBlock blastBlock;

    public IceBlast(AbilityUser user, AbilityController controller) {
        super(user, controller);

        cooldown = CONFIG.cooldown;
        selectRange = CONFIG.selectRange;
        sourceRevertTime = CONFIG.sourceRevertTime;
        range = CONFIG.range;
        speed = CONFIG.speed;
        damage = CONFIG.damage;
        hitboxSize = CONFIG.hitboxSize;
        slowPower = CONFIG.slowPower;
        slowDuration = CONFIG.slowDuration;

        isSourced = true;
        isRising = true;
    }

	@Override
	public boolean start() {
        var opts = new WaterSourceOptions(user()).noWater().noPlant().noSnow();
        source = WaterUtil.getSourceBlock(user(), selectRange, opts);
        if (source == null) {
            return false;
        }

        return true;
	}

    protected boolean isSourced() {
        return isSourced;
    }

    protected void fire() {
        if (!isSourced) {
            return;        
        }
        
        Player player = user().player();
        Location target = EntityUtil.getTarget(player, range + selectRange); 
        riseDest = getRiseDestination(target);
        // Avoids vector math errors and disappearing blasts if target is in range
        finalDirection = VectorUtil.getDirection(riseDest.getLocation(), target);
        finalDirection.normalize();
        location = source.getLocation().add(0.5, 0.5, 0.5);
    
        WaterUtil.playIceSound(location);
        WaterUtil.consumeSource(this, source, sourceRevertTime);

        isSourced = false;
        user().addCooldown("IceBlast", cooldown);
    }

    private Block getRiseDestination(Location target) {
        Location loc = source.getLocation().add(0.5, 0.5, 0.5);
        double yDiff = target.getBlockY() - loc.getBlockY();
        double riseY = target.getBlockY();
        if (yDiff <= 2) {
            riseY = loc.getBlockY() + 2;
        }
        Location riseLoc = loc.clone();
        // Ensure we dont go out of range just rising
        riseLoc.setY(Math.min(riseY, loc.getBlockY() + range + 2));
        return riseLoc.getBlock();
    }

	@Override
	public boolean progress() {
        if (!user().canUse(controller(), true, isSourced)) {
            return false;
        }
        
        if (isSourced) {
            WaterUtil.playSourceSelectedEffect(source);
            return isSourceViable();
        } else {
            return advanceBlast();
        }
	}

    private boolean isSourceViable() {
        Location eyeLoc = user().player().getEyeLocation();
        Location sourceLoc = source.getLocation().add(0.5, 0.5, 0.5);
        double maxDist = Math.pow(selectRange + 1, 2);
        if (!eyeLoc.getWorld().equals(sourceLoc.getWorld())
                || eyeLoc.distanceSquared(sourceLoc) > maxDist) {
            return false;
        }

        var opts = new WaterSourceOptions(user()).noWater().noPlant().noSnow();
        return WaterUtil.canUse(source, opts);
    }

    private boolean advanceBlast() {
        double remainder = speed;
        while (remainder > 0) {
            double travel = Math.min(remainder, 1);
            remainder--;

            Vector dir = getDirection().clone().multiply(travel);
            Block oldBlock = location.getBlock();
            Block newBlock = location.add(dir).getBlock();
            if (!oldBlock.equals(newBlock)) {
                if (BlockUtil.isSolid(newBlock) && !newBlock.equals(source)) {
                    return false;
                }

                Location srcLoc = source.getLocation().add(0.5, 0.5, 0.5);
                double maxDist = range * range;
                if (location.distanceSquared(srcLoc) > maxDist) {
                    return false;
                } 

                moveIce(newBlock);
                playParticles(newBlock);
            }

            if (hitEntities(newBlock)) {
                return false;
            }

            if (newBlock.getY() == riseDest.getY()) {
                isRising = false;
            }
        }
        
        return true;
    }

    private Vector getDirection() {
        if (isRising) {
            double yDiff = riseDest.getY() - location.getBlockY();
            return new Vector(0, yDiff, 0).normalize();
        } else {
            return finalDirection;
        }
    }

    private void moveIce(Block block) {
        if (blastBlock != null) {
            ElementalMagicApi.revertibleManager().revert(blastBlock);
        }
       
        BlockData data = Material.PACKED_ICE.createBlockData();
        blastBlock = TempBlock.builder(this, data)
            .buildAt(block).orElse(null);        
    }

    private void playParticles(Block block) {
        World world = blastBlock.block().getWorld();
        Location loc = blastBlock.block().getLocation().add(0.5, 0.5, 0.5);
        BlockData data = Material.ICE.createBlockData();
        world.spawnParticle(Particle.BLOCK, loc, 20, 0.3, 0.3, 0.3, data);
        world.spawnParticle(Particle.SNOWFLAKE, loc, 3, 0, 0, 0, 0.1);
        world.spawnParticle(Particle.ITEM_SNOWBALL, loc, 5, 0.5, 0.5, 0.5);
    }

    private boolean hitEntities(Block block) {
        boolean hitAny = false;
        EffectHandler effectHandler = ElementalMagicApi.effectHandler();
        PotionEffect slow = new PotionEffect(
            PotionEffectType.SLOWNESS, slowDuration, slowPower);

        World world = block.getWorld();
        BoundingVolume bv = AABB.fromBlock(block, hitboxSize);
        for (Entity e : EntityUtil.getNearbyEntities(world, bv)) {
            if (e.equals(user().player()) || !(e instanceof LivingEntity le)) {
                continue;
            }
            
            hitAny |= effectHandler.damageEntity(le, this, damage);
            hitAny |= effectHandler.addPotionEffect(le, this, slow);
        }

        return hitAny;
    }

	@Override
	public void onDestruction() {
        if (blastBlock !=  null) {
            ElementalMagicApi.revertibleManager().revert(blastBlock);
            
            World world = blastBlock.block().getWorld();
            Location loc = blastBlock.block().getLocation().add(0.5, 0.5, 0.5);
            BlockData data = Material.PACKED_ICE.createBlockData();
            world.spawnParticle(Particle.BLOCK, loc, 30, data);
            world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 2, 1.1f);
        }
	}
    
    protected static class ConfigValues {
        
        private static final String CONFIG_PATH = IceBlastController.CONFIG_PATH;
        
        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 4500;
        @Configure(path = CONFIG_PATH + "SelectRange", config = Config.ABILITIES)
        private double selectRange = 10;
        @Configure(path = CONFIG_PATH + "SourceRevertTime", config = Config.ABILITIES)
        private long sourceRevertTime = 10000;
        @Configure(path = CONFIG_PATH + "Range", config = Config.ABILITIES)
        private double range = 20;
        @Configure(path = CONFIG_PATH + "Speed", config = Config.ABILITIES)
        private double speed = 1.75; 
        @Configure(path = CONFIG_PATH + "Damage", config = Config.ABILITIES)
        private double damage = 3.0;
        @Configure(path = CONFIG_PATH + "HitboxSize", config = Config.ABILITIES)
        private double hitboxSize = 1.5;
        @Configure(path = CONFIG_PATH + "SlowPower", config = Config.ABILITIES)
        private int slowPower = 3;
        @Configure(path = CONFIG_PATH + "SlowDuration", config = Config.ABILITIES)       
        private int slowDuration = 75;

    }
}
