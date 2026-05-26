package me.kwilson272.elementalmagic.core.gameplay.fire.combustion;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

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
import me.kwilson272.elementalmagic.core.gameplay.util.EntityUtil;
import me.kwilson272.elementalmagic.core.gameplay.util.VectorUtil;

public class Combustion extends FireAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private double speed;
    private double range;
    private double hitboxSize;
    private double damage;
    private double damageRadius;
    private double fireRadius;
    private long fireDuration;
    
    private CombustionRay ray;

    public Combustion(AbilityUser user, AbilityController controller) {
		super(user, controller);

        cooldown = CONFIG.cooldown;
        speed = CONFIG.speed;
        range = CONFIG.range;
        hitboxSize = CONFIG.hitboxSize;
        damage = CONFIG.damage;
        damageRadius = CONFIG.damageRadius;
        fireRadius = CONFIG.fireRadius;
        fireDuration = CONFIG.fireDuration;
    
        Location eyeLoc = user.player().getEyeLocation();
        Vector direction = eyeLoc.getDirection();
        ray = new CombustionRay(eyeLoc, direction);
	}

	@Override
	public boolean start() {
        user().addCooldown(name(), cooldown);
        return true;
	}

	@Override
	public boolean progress() {
        return user().canUse(controller(), true, false) && ray.progress();
	}

	@Override
	public void onDestruction() {
	}

	@Override
	public String name() {
        return "Combustion";
	}

    protected void detonate() {
        explode(ray.lastLoc);
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

    private void explode(Location loc) {
        World world = loc.getWorld();
        BoundingVolume bv = Sphere.at(loc, damageRadius);
        for (Entity e : EntityUtil.getNearbyEntities(world, bv)) {
            ElementalMagicApi.effectHandler().damageEntity(e, this, damage);
        }
       
        TempBlockBuilder builder = TempBlock.builder(this, getFireData())
            .setDuration(fireDuration)
            .setUsable(true);
        for (Block b : BlockUtil.collectSphere(loc, fireRadius)) {
            if (canIgnite(b)) {
                builder.buildAt(b);
            }
        }

        world.spawnParticle(Particle.EXPLOSION, loc, 1);
        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2, 0);
    }

    private class CombustionRay extends Ray {

        private Location lastLoc;
        private Vector direction;
        private int animCounter;
        private double animAngle;

		public CombustionRay(Location location, Vector direction) { 
			super(location, speed, range);
            this.lastLoc = location;
            this.direction = direction;
            this.animCounter = 0;
		}

		@Override
		public boolean collides(Block block) {
            if (BlockUtil.isSolid(block)) {
                Location loc = block.getLocation().add(0.5, 0.5, 0.5);
                explode(loc);
                return true;
            }
            return false;
		}

		@Override
		public boolean moveTo(Location loc) {
            World world = loc.getWorld();
            world.spawnParticle(Particle.FIREWORK, loc, 1, 0, 0, 0, 0.01);
            world.spawnParticle(getFireParticle(), loc, 1, 0, 0, 0, 0.01);
            world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.8f, 0.65f);

            rotateFireParticles(loc);
            if (++animCounter % 15 == 0) {
                playParticleRing(loc);
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
                animAngle += spacing;
                Vector vec = VectorUtil.getOrthogonal(direction);
                vec = VectorUtil.rotateAroundVector(direction, vec, animAngle); 
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

		@Override
		public Vector getDirection() {
            return direction.clone();
		}
    }

    protected static class ConfigValues {
        
        private static final String CONFIG_PATH = CombustionController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 11000;
        @Configure(path = CONFIG_PATH + "Speed", config = Config.ABILITIES)
        private double speed = 1.15;
        @Configure(path = CONFIG_PATH + "Range", config = Config.ABILITIES)
        private double range = 27;
        @Configure(path = CONFIG_PATH + "HitboxSize", config = Config.ABILITIES)
        private double hitboxSize = 1.2;
        @Configure(path = CONFIG_PATH + "Damage", config = Config.ABILITIES)
        private double damage = 2.0;
        @Configure(path = CONFIG_PATH + "DamageRadius", config = Config.ABILITIES)
        private double damageRadius = 2.75;
        @Configure(path = CONFIG_PATH + "FireRadius", config = Config.ABILITIES)
        private double fireRadius = 2.8;
        @Configure(path = CONFIG_PATH + "FireDuration", config = Config.ABILITIES)
        private long fireDuration = 5000;

    }
}
