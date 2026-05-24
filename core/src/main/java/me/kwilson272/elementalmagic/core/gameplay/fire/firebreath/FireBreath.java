package me.kwilson272.elementalmagic.core.gameplay.fire.firebreath;

import java.awt.FontFormatException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.effect.EffectHandler;
import me.kwilson272.elementalmagic.api.revertible.TempBlock;
import me.kwilson272.elementalmagic.api.revertible.TempBlock.TempBlockBuilder;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.api.util.BlockUtil;
import me.kwilson272.elementalmagic.core.gameplay.fire.FireAbility;
import me.kwilson272.elementalmagic.core.gameplay.util.AbilityUtil;
import me.kwilson272.elementalmagic.core.gameplay.util.EntityUtil;
import me.kwilson272.elementalmagic.core.gameplay.water.icewall.IceWall;
import me.kwilson272.elementalmagic.core.gameplay.water.icewave.IceWave;
import me.kwilson272.elementalmagic.core.gameplay.water.surge.SurgeWave;
import me.kwilson272.elementalmagic.core.gameplay.water.torrent.Torrent;

public class FireBreath extends FireAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();
    
    private long cooldown;
    private long duration;
    private double range;
    private double spread;
    private double damage;
    private long damageInterval;
    private long burnDuration;
    private double meltRadius;
    private boolean meltUnusable;
    private long meltDuration;
    
    private boolean isInfinite;
    private long endTime;

    private Map<Entity, Long> damageTimes;

	public FireBreath(AbilityUser user, AbilityController controller) {
		super(user, controller);

        cooldown = CONFIG.cooldown;
        duration = CONFIG.duration;
        range = CONFIG.range;
        spread = CONFIG.spread;
        damage = CONFIG.damage;
        damageInterval = CONFIG.damageInterval;
        burnDuration = CONFIG.burnDuration;
        meltRadius = CONFIG.meltRadius;
        meltUnusable = CONFIG.meltUnusable;

        damageTimes = new HashMap<>();
	}

	@Override
	public boolean start() {
        isInfinite = duration < 0;
        endTime = System.currentTimeMillis() + duration;
        return true;
	}

	@Override
	public boolean progress() {
        if (!user().canUse(controller(), true, false)
                || !user().player().isSneaking()
                || (!isInfinite && System.currentTimeMillis() > endTime)) {
            return false;       
        }

        createBreath();
        return true;
	}

    private void createBreath() {
        double radius = 0.5;
        double spacing = 0.5;
        Location loc = user().player().getEyeLocation();
        Vector dir = loc.getDirection().multiply(spacing);

        for (double i = 0; i <= range; i += spacing) {
            meltIce(loc);
            if (BlockUtil.isSolid(loc.getBlock())) {
                break;
            }

            displayParticles(loc, radius);
            affectEntities(loc, radius);
            radius += spread;

            loc.add(dir);
            if (ThreadLocalRandom.current().nextInt(8) == 0) {
                playFireSound(loc);
            }
        }
    }

    private void meltIce(Location loc) {
        for (Block b : BlockUtil.collectSphere(loc, meltRadius)) {
            if (canMelt(b)) {
                melt(b);
            }
        }
    }

    private boolean canMelt(Block block) {
        if (!AbilityUtil.isSnow(block) && !AbilityUtil.isIce(block)) {
            return false;
        }

        TempBlock tb = TempBlock.get(block).orElse(null);
        return tb == null || tb.isUsable() || (meltUnusable 
                && (tb.ability() instanceof IceWall
                || tb.ability() instanceof Torrent
                || tb.ability() instanceof SurgeWave
                || tb.ability() instanceof IceWave));
    }

    private void melt(Block block) {
        if (AbilityUtil.isSnow(block)) {
            BlockData data = Material.AIR.createBlockData();
            TempBlock.builder(this, data).setDuration(meltDuration).buildAt(block);
            return;
        }

        TempBlock tb = TempBlock.get(block).orElse(null);
        if (tb != null) {
            ElementalMagicApi.revertibleManager().revert(tb);    
        } else {
            BlockData data = Material.WATER.createBlockData();
            TempBlock.builder(this, data).setDuration(duration)
                .setUsable(true).buildAt(block);
        }
    }

    private void displayParticles(Location loc, double radius) {
        World world = user().player().getWorld();
        Particle particle = getFireParticle();
        double offset = radius * 0.75;

        Vector dir = user().player().getEyeLocation().getDirection();
        double dirX = dir.getX();
        double dirY = dir.getY();
        double dirZ = dir.getZ();

        Location disp = loc.clone().add(
            ThreadLocalRandom.current().nextDouble(-offset, offset),
            ThreadLocalRandom.current().nextDouble(-offset, offset),
            ThreadLocalRandom.current().nextDouble(-offset, offset)
        ); 
        world.spawnParticle(particle, disp, 0, dirX, dirY, dirZ, 0.5); 
        world.spawnParticle(Particle.SMOKE, loc, 1, offset, offset, offset, 0.02);
    }

    private void affectEntities(Location loc, double radius) {
        EffectHandler effectHandler = ElementalMagicApi.effectHandler();
        for (Entity e : EntityUtil.getNearbyEntities(loc, radius)) {
            if (canAffect(e)) {
                effectHandler.damageEntity(e, this, damage);
                effectHandler.setFireDuration(e, this, burnDuration);
                damageTimes.put(e, System.currentTimeMillis() + damageInterval);
            }
        }
    }
    
    private boolean canAffect(Entity entity) {
        if (entity.equals(user().player()) 
                || !(entity instanceof LivingEntity)) {
            return false;
        }    
        
        long time = damageTimes.getOrDefault(entity, 0L);
        return System.currentTimeMillis() > time;
    }

	@Override
	public void onDestruction() {
        user().addCooldown("FireBreath", cooldown);
	}

    protected static class ConfigValues {
    
        private static final String CONFIG_PATH = FireBreathController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 5600;
        @Configure(path = CONFIG_PATH + "Duration", config = Config.ABILITIES)       
        private long duration = 4500;
        @Configure(path = CONFIG_PATH + "Range", config = Config.ABILITIES)
        private double range = 15.0;
        @Configure(path = CONFIG_PATH + "Spread", config = Config.ABILITIES)       
        private double spread = 0.2;
        @Configure(path = CONFIG_PATH + "Damage", config = Config.ABILITIES) 
        private double damage = 0.5;
        @Configure(path = CONFIG_PATH + "DamageInterval", config = Config.ABILITIES)
        private long damageInterval = 500;
        @Configure(path = CONFIG_PATH + "BurnDuration", config = Config.ABILITIES) 
        private int burnDuration = 750;
        @Configure(path = CONFIG_PATH + "IceMeltRadus", config = Config.ABILITIES)
        private double meltRadius = 2.0;
        @Configure(path = CONFIG_PATH + "MeltUnusableIce", config = Config.ABILITIES)
        private boolean meltUnusable = true;
        @Configure(path = CONFIG_PATH + "MeltDuration", config = Config.ABILITIES)
        private long meltDuration = 10000;
    }
}
