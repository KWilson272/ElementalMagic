package me.kwilson272.elementalmagic.core.gameplay.fire.lightningburst;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Particle.DustTransition;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.effect.EffectHandler;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.api.util.BlockUtil;
import me.kwilson272.elementalmagic.core.ability.CoreAbility;
import me.kwilson272.elementalmagic.core.gameplay.components.Ray;
import me.kwilson272.elementalmagic.core.gameplay.util.EntityUtil;
import me.kwilson272.elementalmagic.core.gameplay.util.VectorUtil;

public class LightningBurst extends CoreAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private static final DustTransition DUST_OPTS = new DustTransition(
        Color.fromRGB(120, 240, 255),
        Color.fromRGB(60, 100, 255),
        0.8f
    );

    private long cooldown;
    private long chargeTime;
    private double range;
    private double speed;
    private double damage;
    private double hitboxSize;
    private double angleSpacing;
    private double randomOffset;

    private boolean isCharging;
    private long chargedTime;
    private double animAngle;

    private List<Bolt> bolts;

	public LightningBurst(AbilityUser user, AbilityController controller) {
		super(user, controller);

        cooldown = CONFIG.cooldown;
        chargeTime = CONFIG.chargeTime;
        range = CONFIG.range;
        speed = CONFIG.speed;
        damage = CONFIG.damage;
        hitboxSize = CONFIG.hitboxSize;
        angleSpacing = CONFIG.angleSpacing;
        randomOffset = CONFIG.randomOffset;

        isCharging = true;
        animAngle = 0;
        bolts = new ArrayList<>();
	}

	@Override
	public boolean start() {
        chargedTime = System.currentTimeMillis() + chargeTime;
        return true;
	}

	@Override
	public boolean progress() {
        if (!user().canUse(controller(), isCharging, false)) {
            return false;
        }

        if (isCharging) {
            if (System.currentTimeMillis() > chargedTime) {
                playChargeAnimation();
                if (!user().player().isSneaking()) {
                    user().addCooldown(name(), cooldown);
                    isCharging = false;
                    initBurst();
                }
                return true;
            }

            return user().player().isSneaking();
        }

        bolts.removeIf(bolt -> !bolt.progress());
        return !bolts.isEmpty();
	}

    private void playChargeAnimation() {
        Location loc = user().player().getEyeLocation();
        Vector dir = loc.getDirection();
        Vector ortho = VectorUtil.getOrthogonal(dir);
        Location center = loc.add(dir);

        double spacing = Math.toRadians(15);
        for (int i = 0; i < 3; ++i) {
            animAngle += spacing;
            Vector v1 = VectorUtil.rotateAroundVector(dir, ortho, animAngle);
            Vector v2 = VectorUtil.rotateAroundVector(dir, ortho, animAngle + Math.PI);
            
            drawParticle(center.clone().add(v1));
            drawParticle(center.clone().add(v2));
        }

        World world = loc.getWorld();
        world.playSound(loc, Sound.BLOCK_BEEHIVE_WORK, 1, 0);
    }

    private void initBurst() {
        Location loc = user().player().getEyeLocation();

        for (double i = -90; i <= 90; i += angleSpacing) {
            double theta = Math.toRadians(i);
            double y = Math.sin(theta);
            double xzMag = Math.cos(theta);
            
            if (xzMag < 0.001) {
                Vector vec = new Vector(0, y, 0);
                bolts.add(new Bolt(loc.clone(), vec));
                continue;
            }
            
            double step = angleSpacing / xzMag;
            for (double j = 0; j < 360; j += step) {
                double phi = Math.toRadians(j);
                double x = Math.sin(phi) * xzMag;
                double z = Math.cos(phi) * xzMag;

                Vector vec = new Vector(x, y, z);
                bolts.add(new Bolt(loc.clone(), vec));
            }
        }
    }

	@Override
	public void onDestruction() {
	}

	@Override
	public String name() {
        return "LightningBurst";
	}

    private void drawParticle(Location loc) {
        World world = loc.getWorld();
        Particle particle = Particle.DUST_COLOR_TRANSITION;
        world.spawnParticle(particle, loc, 1, 0, 0, 0, DUST_OPTS);
        world.spawnParticle(Particle.ELECTRIC_SPARK, loc, 1, 0, 0, 0, 0);
    }

    private class Bolt extends Ray {
        
   
        private Location location;
        private Vector direction;

        Bolt(Location location, Vector direction) {
			super(location, speed, range);
            this.location = location;
            this.direction = direction;
		}

		@Override
		public boolean collides(Block block) {
            return BlockUtil.isSolid(block);
		}

		@Override
		public boolean moveTo(Location loc) {
            affectEntities(loc);
            renderElectricity(loc);
            location = loc;

            if (ThreadLocalRandom.current().nextInt(40) == 0) {
                World world = loc.getWorld();
                world.playSound(loc, Sound.ENTITY_CREEPER_PRIMED, 1, 0);
            }

            return true;
    	}

        private void renderElectricity(Location loc) {
            double dist = loc.distance(location);
            Location midPoint = location.clone();
            midPoint.add(direction.clone().multiply(dist));
        
            Random rand = ThreadLocalRandom.current();
            Vector offset = new Vector(
                rand.nextDouble(-randomOffset, randomOffset),
                rand.nextDouble(-randomOffset, randomOffset),
                rand.nextDouble(-randomOffset, randomOffset)
            );
            
            midPoint.add(offset);
            drawBetween(location, midPoint);
            drawBetween(midPoint, loc);
        }
    
        private void affectEntities(Location loc) {
            EffectHandler effectHandler = ElementalMagicApi.effectHandler();
            for (Entity e : EntityUtil.getNearbyEntities(loc, hitboxSize)) {
                if (e.equals(user().player()) || !(e instanceof LivingEntity)) {
                    continue;
                }
                effectHandler.damageEntity(e, LightningBurst.this, damage);
            }
        }

        private void drawBetween(Location start, Location end) {
            double spacing = 0.1;

            Location loc = start.clone();
            Vector dir = VectorUtil.getDirection(start, end);
            double dist = dir.lengthSquared();
            dir.multiply(spacing / dist);

            int count = (int) (dist / spacing);
            for (int i = 0; i < count; ++i) {
                drawParticle(loc);
                loc.add(dir);
            }
        }

		@Override
		public Vector getDirection() {
            return direction.clone();
		}
    } 

    protected static class ConfigValues {
        
        private static final String CONFIG_PATH = LightningBurstController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 7000;
        @Configure(path = CONFIG_PATH + "ChargeTime", config = Config.ABILITIES)
        private long chargeTime = 0;
        @Configure(path = CONFIG_PATH + "Range", config = Config.ABILITIES)
        private double range = 20.0;
        @Configure(path = CONFIG_PATH + "Speed", config = Config.ABILITIES)
        private double speed = 1.0;
        @Configure(path = CONFIG_PATH + "Damage", config = Config.ABILITIES)
        private double damage = 2.0;
        @Configure(path = CONFIG_PATH + "HitboxSize", config = Config.ABILITIES)
        private double hitboxSize = 2.0;
        @Configure(path = CONFIG_PATH + "AngleSpacing", config = Config.ABILITIES)
        private double angleSpacing = 30.0;
        @Configure(path = CONFIG_PATH + "RandomOffset", config = Config.ABILITIES)
        private double randomOffset = 1.0;

    }
}
