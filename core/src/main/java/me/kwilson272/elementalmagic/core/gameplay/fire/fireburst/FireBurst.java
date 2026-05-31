package me.kwilson272.elementalmagic.core.gameplay.fire.fireburst;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.effect.EffectHandler;
import me.kwilson272.elementalmagic.api.revertible.TempBlock;
import me.kwilson272.elementalmagic.api.revertible.TempBlock.TempBlockBuilder;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.gameplay.components.Ray;
import me.kwilson272.elementalmagic.core.gameplay.fire.FireAbility;
import me.kwilson272.elementalmagic.core.util.Blocks;
import me.kwilson272.elementalmagic.core.util.Entities;
import me.kwilson272.elementalmagic.core.util.Vectors;

public class FireBurst extends FireAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private long chargeTime;
    private double coneAngle;
    private double coneSpacing;
    private double sphereSpacing;
    private int particleInterval;
    private double speed;
    private double range;
    private double damage;
    private double hitboxSize;
    private double fireRadius;
    private long fireDuration;
    private double fireDamage;
    
    private boolean isCharging;
    private long animTime;
    private long chargedTime;

    private Set<Block> safeBlocks;
    private List<BurstStream> streams;

	public FireBurst(AbilityUser user, AbilityController controller) {
		super(user, controller);

        cooldown = CONFIG.cooldown;
        chargeTime = CONFIG.chargeTime;
        coneAngle = CONFIG.coneAngle;
        coneSpacing = CONFIG.coneSpacing;
        sphereSpacing = CONFIG.sphereSpacing;
        particleInterval = CONFIG.particleInterval;
        speed = CONFIG.speed;
        range = CONFIG.range;
        damage = CONFIG.damage;
        hitboxSize = CONFIG.hitboxSize;
        fireRadius = CONFIG.fireRadius;
        fireDuration = CONFIG.fireDuration;
        fireDamage = CONFIG.fireDamage;
        
        isCharging = true;
        animTime = 0;

        safeBlocks = new HashSet<>(); 
        streams = new ArrayList<>();
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
                playChargeRing();
                
                if (!user().player().isSneaking()) {
                    initSphereBurst();
                    isCharging = false;
                }
            } else if (!user().player().isSneaking()) {
                return false;
            }
            return true;

        } else {
            streams.removeIf(stream -> !stream.progress());
            return !streams.isEmpty();
        }
	}

    // Uses the same function as CFB so it becomes a guessing game
    // as to which ability a player is charging.
    private void playChargeRing() {
        if (animTime > System.currentTimeMillis()) {
            return;
        }
        animTime = System.currentTimeMillis() + 400;    
    
        Location loc = user().player().getEyeLocation();
        Vector dir = loc.getDirection();
        loc.add(dir.clone().multiply(1.5));

        Vector ortho = Vectors.getOrthogonal(dir);
        Particle particle = getFireParticle();
        World world = loc.getWorld();

        int count = 15;
        double step = 2 * Math.PI / count;
        for (int i = 0; i < count; ++i) {
            double angle = step * i;
            Vector vec = Vectors.rotateAroundVector(dir, ortho, angle);
            Location spawnLoc = loc.clone().add(vec.clone().multiply(0.5));
            world.spawnParticle(particle, spawnLoc, 0, vec.getX(), vec.getY(), vec.getZ(), 0.2);    
        }
    }

    private void initSphereBurst() {
        initSafeBlocks();
        Location loc = user().player().getEyeLocation();

        for (double i = -90; i <= 90; i += sphereSpacing) {
            double theta = Math.toRadians(i);
            double y = Math.sin(theta);
            double xzMag = Math.cos(theta);
            
            // Div by 0 at the poles
            if (xzMag < 0.001) {
                Vector vec = new Vector(0, y, 0);
                streams.add(new BurstStream(loc.clone(), vec));
                continue;
            }
            
            double step = sphereSpacing / xzMag;
            for (double j = 0; j < 360; j += step) {
                double phi = Math.toRadians(j);
                double x = Math.sin(phi) * xzMag;
                double z = Math.cos(phi) * xzMag;

                Vector vec = new Vector(x, y, z);
                streams.add(new BurstStream(loc.clone(), vec));
            }
        }

        user().addCooldown(name(), cooldown);
    }
    
    protected void initConeBurst() {
        if (!isCharging || System.currentTimeMillis() < chargedTime) {
            return;
        }

        initSafeBlocks();
        Location loc = user().player().getEyeLocation();
       
        Vector dir = user().player().getEyeLocation().getDirection();
        Vector ortho = Vectors.getOrthogonal(dir);

        double spacing = Math.toRadians(coneSpacing);
        double angle = Math.toRadians(coneAngle / 2);
        for (double i = 0; i <= angle; i += spacing) {
            double magDir = Math.cos(i);
            double magRot = Math.sin(i);
            
            // Center of the cone will cause div by 0
            if (magRot < 0.001) {
                streams.add(new BurstStream(loc.clone(), dir.clone()));
                continue;
            }

            double step = spacing / magRot; 
            for (double j = 0; j <= 2 * Math.PI; j += step) {
                Vector rot = Vectors.rotateAroundVector(dir, ortho, j);
                Vector vec = dir.clone().multiply(magDir);
                vec.add(rot.multiply(magRot));

                streams.add(new BurstStream(loc.clone(), vec));
            }
        }
        
        isCharging = false;
        user().addCooldown(name(), cooldown);
    }

    private void initSafeBlocks() {
        Location loc = user().player().getLocation();
        for (Block block : Blocks.collectSphere(loc, 3.0)) {
            safeBlocks.add(block);
        }
    }

	@Override
	public void onDestruction() {
	}

	@Override
	public String name() {
        return "FireBurst";
	}

    private class BurstStream extends Ray {

        private final Vector direction;
        private int particleCounter;

		public BurstStream(Location location, Vector direction) {
			super(location, speed, range);
            this.direction = direction;
            this.particleCounter = 0;
		}

		@Override
		public boolean collides(Block block) {
            if (Blocks.isSolid(block)) {
                Location loc = block.getLocation().add(0.5, 0.5, 0.5);
                igniteAroundPoint(loc);
                return false;
            }

            return Blocks.isLiquid(block);
		}

        private void igniteAroundPoint(Location loc) {
            BlockData data = getFireData();
            TempBlockBuilder builder = TempBlock.builder(FireBurst.this, data)
                .setDuration(fireDuration)
                .setDamage(fireDamage);

            for (Block b : Blocks.collectSphere(loc, fireRadius)) {
                if (canIgnite(b) && !safeBlocks.contains(b)) {
                    builder.buildAt(b);
                }
            }
        }

		@Override
		public boolean moveTo(Location loc) {
            playParticles(loc);
            affectEntities(loc);
            if (ThreadLocalRandom.current().nextInt(10) == 0) {
                playFireSound(loc);
            }

            return true;
		}

        private void playParticles(Location loc) {
            if (particleCounter % particleInterval == 0) { 
                World world = loc.getWorld();
                world.spawnParticle(getFireParticle(), loc, 1, 1, 1, 1, 0.01);
                world.spawnParticle(Particle.SMOKE, loc, 1, 1, 1, 1, 0);
            }
            ++particleCounter;
        }

        private void affectEntities(Location loc) {
            EffectHandler effectHandler = ElementalMagicApi.effectHandler();
            for (Entity e : Entities.getNearbyEntities(loc, hitboxSize)) {
                if (!e.equals(user().player())) {
                    effectHandler.damageEntity(e, FireBurst.this, damage);
                }
            }
        }

		@Override
		public Vector getDirection() {
            return direction.clone();
		}
    }

    protected static class ConfigValues {

        private static final String CONFIG_PATH = FireBurstController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 9800;
        @Configure(path = CONFIG_PATH + "ChargeTime", config = Config.ABILITIES)
        private long chargeTime = 1500;
        @Configure(path = CONFIG_PATH + "ConeAngle", config = Config.ABILITIES)
        private double coneAngle = 45.0;
        @Configure(path = CONFIG_PATH + "ConeSpacing", config = Config.ABILITIES)
        private double coneSpacing = 15.0;
        @Configure(path = CONFIG_PATH + "SphereSpacing", config = Config.ABILITIES)
        private double sphereSpacing = 20.0;
        @Configure(path = CONFIG_PATH + "ParticleInterval", config = Config.ABILITIES)
        private int particleInterval = 2;
        @Configure(path = CONFIG_PATH + "Speed", config = Config.ABILITIES)
        private double speed = 1.25;
        @Configure(path = CONFIG_PATH + "Range", config = Config.ABILITIES)
        private double range = 15.0;
        @Configure(path = CONFIG_PATH + "Damage", config = Config.ABILITIES)
        private double damage = 3.0;
        @Configure(path = CONFIG_PATH + "HitboxSize", config = Config.ABILITIES)
        private double hitboxSize = 1.5;
        @Configure(path = CONFIG_PATH + "FireRadius", config = Config.ABILITIES)
        private double fireRadius = 2.0;
        @Configure(path = CONFIG_PATH + "FireDuration", config = Config.ABILITIES)
        private long fireDuration = 2000;
        @Configure(path = CONFIG_PATH + "FireDamage", config = Config.ABILITIES)
        private double fireDamage = 1.0;
    }
}
