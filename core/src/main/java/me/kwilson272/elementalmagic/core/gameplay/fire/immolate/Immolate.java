package me.kwilson272.elementalmagic.core.gameplay.fire.immolate;

import java.util.Collection;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;
import org.joml.Math;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.collision.BoundingVolume;
import me.kwilson272.elementalmagic.api.collision.Sphere;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.revertible.TempBlock;
import me.kwilson272.elementalmagic.api.revertible.TempBlock.TempBlockBuilder;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.api.util.BlockUtil;
import me.kwilson272.elementalmagic.core.gameplay.components.Ray;
import me.kwilson272.elementalmagic.core.gameplay.fire.FireAbility;
import me.kwilson272.elementalmagic.core.gameplay.util.AbilityUtil;
import me.kwilson272.elementalmagic.core.gameplay.util.EntityUtil;
import me.kwilson272.elementalmagic.core.gameplay.util.VectorUtil;

public class Immolate extends FireAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private long chargeTime;
    private double speed;
    private double range;
    private double sensitivity;
    private double hitboxSize;
    private double damage;
    private double explosionAffectRadius;
    private double explosionDestroyRadius;
    private boolean explodeOnHit;
    private boolean destroyBlocks;
    private boolean placeFire;
    private long revertTime;

    private boolean appliedCooldown;
    private boolean isCharging;
    private long chargedTime;
    private double animAngle;
    private double health;

    private ImmolateRay ray;

    public Immolate(AbilityUser user, AbilityController controller) {
		super(user, controller);

        cooldown = CONFIG.cooldown;
        chargeTime = CONFIG.chargeTime;
        speed = CONFIG.speed;
        range = CONFIG.range;
        sensitivity = CONFIG.sensitivity;
        hitboxSize = CONFIG.hitboxSize;
        damage = CONFIG.damage;
        explosionAffectRadius = CONFIG.explosionAffectRadius;
        explosionDestroyRadius = CONFIG.explosionDestroyRadius;
        explodeOnHit = CONFIG.explodeOnHit;
        destroyBlocks = CONFIG.destroyBlocks;
        placeFire = CONFIG.placeFire;
        revertTime = CONFIG.revertTime;

        isCharging = true;
        animAngle = 0;
        health = user().player().getHealth();
	}


	@Override
	public boolean start() {
        chargedTime = System.currentTimeMillis() + chargeTime;
        return true;
	}

	@Override
	public boolean progress() {
        if (!user().canUse(controller(), true, false)) {
            return false;
        }

        
        if (isCharging) {
            playChargingAnimation();
            if (System.currentTimeMillis() > chargedTime) {
                playChargedAnimation();
                if (!user().player().isSneaking()) {
                    isCharging = false;
                    appliedCooldown = true;
                    user().addCooldown(name(), cooldown);
                    ray = new ImmolateRay(user().player().getEyeLocation());
                    return true;
                }
            }
            
            if (explodeOnHit && !checkHealth()) {
                explode(user().player().getLocation());
                return false;
            }

            return user().player().isSneaking();
        
        } else {
            return ray.progress();
        }
	}

    private void playChargingAnimation() {
        World world = user().player().getWorld();
        Location center = user().player().getLocation().add(0, 0.65, 0);

        double radius = 1.5;
        double step = Math.toRadians(8);
        for (int i = 0; i < 4; ++i) {
            animAngle += step;
            double x = Math.cos(animAngle) * radius;
            double z = Math.sin(animAngle) * radius;
            Location loc = center.clone().add(x, 0, z);

            world.spawnParticle(getFireParticle(), loc, 1, 0, 0, 0, 0.01);
            world.spawnParticle(Particle.SMOKE, loc, 1, 0, 0, 0, 0.01);
        }
    }

    private void playChargedAnimation() {
        Location loc = user().player().getEyeLocation();
        Vector dir = loc.getDirection();
        loc.add(dir.multiply(1.5)).add(0, 0.5, 0);

        World world = user().player().getWorld();
        world.spawnParticle(Particle.LARGE_SMOKE, loc, 1, 0, 0, 0, 0);
    }

    private boolean checkHealth() {
        double curHealth = user().player().getHealth();
        if (curHealth < health) {
            return false;
        }
        health = user().player().getHealth();
        return true;
    }

    private void explode(Location loc) {
        if (!appliedCooldown) {
            appliedCooldown = true;
            user().addCooldown(name(), cooldown);
        }
        
        World world = loc.getWorld();
        double off = explosionAffectRadius / 2;
        world.spawnParticle(Particle.EXPLOSION_EMITTER, loc, 5, off, off, off);
        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2, 0);
        
        affectEntities(loc);
        affectBlocks(loc);
    }

    private void affectEntities(Location loc) {
        World world = loc.getWorld();
        BoundingVolume bv = Sphere.at(loc, explosionAffectRadius);
        for (Entity e : EntityUtil.getNearbyEntities(world, bv)) {
            ElementalMagicApi.effectHandler().damageEntity(e, this, damage);
        }
    }

    private void affectBlocks(Location loc) {
        Collection<Block> affected = 
            BlockUtil.collectSphere(loc, explosionDestroyRadius);
        // Two passes in case we destroy a hole, so we can properly place fire
        if (destroyBlocks) {
            BlockData data = Material.AIR.createBlockData();
            TempBlockBuilder builder = TempBlock.builder(this, data)
                .setDuration(revertTime);

            for (Block block : affected) {
                if (!block.getType().isAir() && !BlockUtil.isLiquid(block)) {
                    builder.buildAt(block);
                }
            }
        }

        if (placeFire) {
            TempBlockBuilder builder = TempBlock.builder(this, getFireData())
                .setDuration(revertTime);

            for (Block block : affected) {
                Block below = block.getRelative(BlockFace.DOWN);
                if (!BlockUtil.isSolid(block) && !AbilityUtil.isWater(block)
                        && BlockUtil.isSolid(below)) {
                    builder.buildAt(block);
                }
            }
        }
    }

	@Override
	public void onDestruction() {
	}

    protected boolean detonate() {
        if (!isCharging) {
            explode(ray.lastLoc);
            return true;
        }
        return false;
    }

	@Override
	public String name() {
        return "Immolate";
	}
    
    private class ImmolateRay extends Ray {
        
        private Location lastLoc;
        private Vector direction;
        private double angle;
        private int animCounter;

        public ImmolateRay(Location location) {
			super(location, speed, range);
            lastLoc = location;
            direction = user().player().getEyeLocation().getDirection();
            animCounter = 0;
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
            lastLoc = loc;

            World world = loc.getWorld();
            world.spawnParticle(Particle.LARGE_SMOKE, loc, 1, 0, 0, 0, 0.01);
            world.spawnParticle(Particle.FIREWORK, loc, 1, 0, 0, 0, 0.01);
            rotateFireParticles(loc);

            if (++animCounter % 15 == 0) {
                playParticleRing(loc);
                world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST_FAR, 2, 0);
            }
            
            if (hitsEntity(loc)) {
                explode(loc);
                return false;
            }
            return true;
		}

        private void rotateFireParticles(Location loc) {
            World world = loc.getWorld(); 
            double spacing = Math.toRadians(5);
            for (int i = 0; i < 3; ++i) {
                angle += spacing;
                Vector vec = VectorUtil.getOrthogonal(direction);
                vec = VectorUtil.rotateAroundVector(direction, vec, angle); 
                double x = vec.getX();
                double y = vec.getY();
                double z = vec.getZ();

                // Makes a 2 colored helix when the user has blue fire
                world.spawnParticle(getFireParticle(), loc, 0, x, y, z, 0.1);
                world.spawnParticle(Particle.FLAME, loc, 0, -x, -y, -z, 0.1);
            }
        }

        private void playParticleRing(Location loc) {
            World world = loc.getWorld();
            Vector rotate = VectorUtil.getOrthogonal(direction);

            int count = 15;
            for (int i = 0; i < count; ++i) {
                double angle = 2 * Math.PI * ((double) i / count);
                Vector dir = VectorUtil.rotateAroundVector(direction, rotate, angle);
                double x = dir.getX();
                double y = dir.getY();
                double z = dir.getZ();
        
                world.spawnParticle(Particle.FIREWORK, loc, 0, x, y, z, 0.2);
            }
        }

        private boolean hitsEntity(Location loc) {
            for (Entity e : EntityUtil.getNearbyEntities(loc, hitboxSize)) {
                if (!e.equals(user().player()) 
                        && ElementalMagicApi.effectHandler().canAffect(e)) {
                    return true;        
                }
            }
            return false;
        }

		@Override
		public Vector getDirection() {
            double weightNew = sensitivity / 100;
            Vector newDir = user().player().getEyeLocation().getDirection();

            direction.multiply(1 - weightNew);
            newDir.multiply(weightNew);
            direction = direction.add(newDir);

            return direction.normalize().clone();
		}
    }

    protected static class ConfigValues {
        
        private static final String CONFIG_PATH = ImmolateController.CONFIG_PATH;
    
        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 11000;
        @Configure(path = CONFIG_PATH + "ChargeTime", config = Config.ABILITIES)
        private long chargeTime  = 1600;
        @Configure(path = CONFIG_PATH + "Speed", config = Config.ABILITIES)       
        private double speed = 1.1;
        @Configure(path = CONFIG_PATH + "Range", config = Config.ABILITIES)       
        private double range = 37;
        @Configure(path = CONFIG_PATH + "sensitivity", config = Config.ABILITIES)       
        private double sensitivity = 30;
        @Configure(path = CONFIG_PATH + "HitboxSize", config = Config.ABILITIES)
        private double hitboxSize = 1.2;
        @Configure(path = CONFIG_PATH + "Damage", config = Config.ABILITIES)       
        private double damage = 2.0;
        @Configure(path = CONFIG_PATH + "ExplosionAffectRadius", config = Config.ABILITIES)       
        private double explosionAffectRadius = 2.5;
        @Configure(path = CONFIG_PATH + "ExplosionDestroyRadius", config = Config.ABILITIES)
        private double explosionDestroyRadius = 2.8;
        @Configure(path = CONFIG_PATH + "ExplodeOnHit", config = Config.ABILITIES)
        private boolean explodeOnHit = true;
        @Configure(path = CONFIG_PATH + "DestroyBlocks", config = Config.ABILITIES)        
        private boolean destroyBlocks = true;
        @Configure(path = CONFIG_PATH + "PlaceFire", config = Config.ABILITIES)       
        private boolean placeFire = true;
        @Configure(path = CONFIG_PATH + "RevertTime", config = Config.ABILITIES)       
        private long revertTime = 20000;

    }
}
