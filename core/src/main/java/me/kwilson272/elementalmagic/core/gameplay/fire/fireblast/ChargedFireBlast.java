package me.kwilson272.elementalmagic.core.gameplay.fire.fireblast;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.collision.BoundingVolume;
import me.kwilson272.elementalmagic.api.collision.Sphere;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.effect.EffectHandler;
import me.kwilson272.elementalmagic.api.revertible.TempBlock;
import me.kwilson272.elementalmagic.api.revertible.TempBlock.TempBlockBuilder;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.api.util.BlockUtil;
import me.kwilson272.elementalmagic.core.gameplay.components.Ray;
import me.kwilson272.elementalmagic.core.gameplay.fire.FireAbility;
import me.kwilson272.elementalmagic.core.gameplay.util.EntityUtil;
import me.kwilson272.elementalmagic.core.gameplay.util.VectorUtil;

public class ChargedFireBlast extends FireAbility {
  
    protected static final ConfigValues CONFIG = new ConfigValues();

    private enum State {
        CHARGING,
        CHARGED,
        FIRED
    }

    private long cooldown;
    private long chargeTime;
    private double speed;
    private double range;
    private double hitboxSize;
    private double damage;
    private double explosionRadius;
    private double fireRadius;
    private long fireDuration;
    private double fireDamage;
    private long burnDuration;
        
    private State state;
    private long chargedTime;
    private long animTime;

    private CFBRay ray;

	public ChargedFireBlast(AbilityUser user, AbilityController controller) {
		super(user, controller);

        cooldown = CONFIG.cooldown;
        chargeTime = CONFIG.chargeTime;
        speed = CONFIG.speed;
        range = CONFIG.range;
        hitboxSize = CONFIG.hitboxSize;
        damage = CONFIG.damage;
        explosionRadius = CONFIG.explosionRadius;
        fireRadius = CONFIG.fireRadius;
        fireDuration = CONFIG.fireDuration;
        burnDuration = CONFIG.burnDuration;

        state = State.CHARGING;
	}

	@Override
	public boolean start() {
        Block eyeBlock = user().player().getEyeLocation().getBlock();
        if (BlockUtil.isLiquid(eyeBlock)) {
            return false;
        }

        chargedTime = System.currentTimeMillis() + chargeTime;
        return true;
	}

	@Override
	public boolean progress() {
        if (!user().canUse(controller(), isCharging(), false)) {
            return false;
        }

        if (state == State.CHARGING) {
            if (System.currentTimeMillis() > chargedTime) {
                state = State.CHARGED;
            }
            return user().player().isSneaking();

        } else if (state == State.CHARGED) {
            if (System.currentTimeMillis() > animTime) {
                animTime = System.currentTimeMillis() + 400;
                playChargeRing();
            }

            if (!user().player().isSneaking()) {
                Location eyeLoc = user().player().getEyeLocation();
                ray = new CFBRay(eyeLoc, eyeLoc.getDirection());
                user().addCooldown("ChargedFireBlast", cooldown);
                state = State.FIRED;
            }
            return true;

        } else {
            return ray.progress();
        }
	}

    private void playChargeRing() {
        Location loc = user().player().getEyeLocation();
        Vector dir = loc.getDirection();
        loc.add(dir.clone().multiply(1.5));

        Vector ortho = VectorUtil.getOrthogonal(dir);
        Particle particle = getFireParticle();
        World world = loc.getWorld();

        int count = 15;
        double step = 2 * Math.PI / count;
        for (int i = 0; i < count; ++i) {
            double angle = step * i;
            Vector vec = VectorUtil.rotateAroundVector(dir, ortho, angle);
            Location spawnLoc = loc.clone().add(vec.clone().multiply(0.5));
            world.spawnParticle(particle, spawnLoc, 0, vec.getX(), vec.getY(), vec.getZ(), 0.2);    
        }
    }

	@Override
	public void onDestruction() {
	}

    @Override
    public String name() {
        return "ChargedFireBlast";
    }

    protected boolean isCharging() {
        return state != State.FIRED; 
    }

    private boolean hitsEntities(Location loc) {
        for (Entity e : EntityUtil.getNearbyEntities(loc, hitboxSize)) {
            if (!e.equals(user().player()) && e instanceof LivingEntity) {
                return true;
            }
        }
        return false;
    }

    private void explode(Location location) {
        damageEntities(location);
        igniteAround(location);
    
        World world = location.getWorld();
        world.spawnParticle(Particle.EXPLOSION, location, 2, 0.5, 0.5, 0.5); 
        world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 2, 0.8f);
    }
    
    private void damageEntities(Location location) {
        World world = location.getWorld();
        BoundingVolume bv = Sphere.at(location, explosionRadius);
        EffectHandler effectHandler = ElementalMagicApi.effectHandler();
        for (Entity e : EntityUtil.getNearbyEntities(world, bv)) {
            effectHandler.damageEntity(e, this, damage);
            if (e.getFireTicks() * 50 < fireDuration) {
                effectHandler.setFireDuration(e, this, burnDuration);
            }
        }
    }

    private void igniteAround(Location location) {
        TempBlockBuilder builder = TempBlock.builder(this, getFireData())
            .setDuration(fireDuration).setDamage(fireDamage);

        for (Block b : BlockUtil.collectSphere(location, fireRadius)) {
            if (!BlockUtil.isSolid(b) && !BlockUtil.isLiquid(b) 
                    && BlockUtil.isSolid(b.getRelative(BlockFace.DOWN))) {
                builder.buildAt(b);
            }
        }
    }
    
    private class CFBRay extends Ray {

        private final Vector direction;

		public CFBRay(Location location, Vector direction) {
			super(location, speed, range);
            this.direction = direction;
		}

		@Override
		public boolean collides(Block block) {
            if (BlockUtil.isSolid(block)) {
                Location loc = block.getLocation().add(0.5, 0.5, 0.5);
                explode(loc);
                return true;
            }
            return BlockUtil.isLiquid(block);
		}

		@Override
		public boolean moveTo(Location loc) {
            if (hitsEntities(loc)) {
                explode(loc);
                return false;
            } 

            playEffects(loc);
            return true;
		}

        private void playEffects(Location loc) {
            World world = loc.getWorld();
            Particle particle = getFireParticle();
            double radius = hitboxSize / 3;
            world.spawnParticle(particle, loc, 5, radius, radius, radius, 0.0125);
            world.spawnParticle(Particle.SMOKE, loc, 1, radius, radius, radius, 0.01);            

            if (ThreadLocalRandom.current().nextInt(10) == 0) {
                playFireSound(loc);
            }
        }

		@Override
		public Vector getDirection() {
            return direction.clone();
		}
    }

    protected static class ConfigValues {

        private static final String CONFIG_PATH = FireBlastController.CONFIG_PATH + "Charged.";

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 0;
        @Configure(path = CONFIG_PATH + "ChargeTime", config = Config.ABILITIES)
        private long chargeTime = 1800;
        @Configure(path = CONFIG_PATH + "Speed", config = Config.ABILITIES)
        private double speed = 1.6;
        @Configure(path = CONFIG_PATH + "Range", config = Config.ABILITIES)
        private double range = 32.0;
        @Configure(path = CONFIG_PATH + "HitboxSize", config = Config.ABILITIES)
        private double hitboxSize = 1.2;
        @Configure(path = CONFIG_PATH + "Damage", config = Config.ABILITIES)
        private double damage = 4.0;
        @Configure(path = CONFIG_PATH + "ExplosionRadius", config = Config.ABILITIES)
        private double explosionRadius = 2.0;
        @Configure(path = CONFIG_PATH + "FireRadius", config = Config.ABILITIES)
        private double fireRadius = 2.0;
        @Configure(path = CONFIG_PATH + "FireDuration", config = Config.ABILITIES)
        private long fireDuration = 2500;
        @Configure(path = CONFIG_PATH + "FireDamage", config = Config.ABILITIES)
        private double fireDamage = 1.0;
        @Configure(path = CONFIG_PATH + "BurnDuration", config = Config.ABILITIES)
        private long burnDuration = 500;
    }
}
