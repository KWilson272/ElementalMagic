package me.kwilson272.elementalmagic.core.gameplay.fire.fireshots;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.effect.EffectHandler;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.api.util.BlockUtil;
import me.kwilson272.elementalmagic.core.gameplay.components.Ray;
import me.kwilson272.elementalmagic.core.gameplay.fire.FireAbility;
import me.kwilson272.elementalmagic.core.gameplay.util.EntityUtil;

public class FireShots extends FireAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private int shotCount;
    private double speed;
    private double range;
    private double hitboxSize;
    private double damage;
    private double sensitivity;

    private int shotsLeft;
    private List<Shot> shots;

	public FireShots(AbilityUser user, AbilityController controller) {
		super(user, controller);

        cooldown = CONFIG.cooldown;
        shotCount = CONFIG.shotCount;
        speed = CONFIG.speed;
        range = CONFIG.range;
        hitboxSize = CONFIG.hitboxSize;
        damage = CONFIG.damage;
        sensitivity = CONFIG.sensitivity;
        shots = new ArrayList<>();
	}

	@Override
	public boolean start() {
        shotsLeft = shotCount;
        return true;
	}

	@Override
	public boolean progress() {
        if (!user().canUse(controller(), false, false)) {
            return false;
        }

        if (!user().getSelectedBindName().equals("FireShots")) {
            shotsLeft = 0;
        }
        
        if (shotsLeft > 0) {
            Location loc = getHandLoc();
            playParticles(loc);
        }
        
        shots.removeIf(shot -> !shot.progress());
        return shotsLeft > 0 || !shots.isEmpty();
    }

    private Location getHandLoc() {
        Location eyeLoc = user().player().getEyeLocation();
        Vector dir = eyeLoc.getDirection();

        double yaw = Math.toRadians(eyeLoc.getYaw() + 90);
        Vector toHand = new Vector(-Math.sin(yaw), 0, Math.cos(yaw));
        Location loc = user().player().getLocation();
        loc.add(0, 1.35, 0); // Move to about hand level
        loc.add(toHand.multiply(0.4));
        loc.add(dir.multiply(0.6)); // Ensure it renders in front of the player
        return loc;
    }

    private void playParticles(Location loc) {
        World world = loc.getWorld();
        Particle fire = getFireParticle();
        world.spawnParticle(fire, loc, 2, 0, 0, 0, 0.01);
        world.spawnParticle(Particle.SMOKE, loc, 1, 0, 0, 0, 0.01);
    }
    
	@Override
	public void onDestruction() {
        user().addCooldown("FireShots", cooldown);
	}
    
    protected boolean isHolding() {
        return shotsLeft > 0;
    }

    protected void fire() {
        if (shotsLeft <= 0) {
            return;
        }

        Location loc = getHandLoc();
        Vector dir = user().player().getEyeLocation().getDirection();
        shots.add(new Shot(loc, dir));
        shotsLeft--;
    }

    private class Shot extends Ray {
        
        private Vector prevDirection;

        private Shot(Location location, Vector direction) {
            super(location, speed, range);
            this.prevDirection = direction;
        }

		@Override
		public boolean collides(Block block) {
            return BlockUtil.isSolid(block) 
                || BlockUtil.isLiquid(block);
		}

		@Override
		public boolean moveTo(Location loc) {
            playParticles(loc);
            playFireSound(loc);

            boolean damaged = false;
            EffectHandler effectHandler = ElementalMagicApi.effectHandler();
            for (Entity e : EntityUtil.getNearbyEntities(loc, hitboxSize)) {
                if (!e.equals(user().player())) {
                    damaged |= effectHandler.damageEntity(e, FireShots.this, damage);
                }
            }

            return !damaged;
		}

		@Override
		public Vector getDirection() {
            double curFactor = sensitivity / 100;
            Vector curDirection = user().player().getEyeLocation().getDirection();
            Vector direction = prevDirection.clone().multiply(1 - curFactor);
            return direction.add(curDirection.multiply(curFactor));
		}   
    }

    protected static class ConfigValues {
    
        private static final String CONFIG_PATH = FireShotsController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 6800;
        @Configure(path = CONFIG_PATH + "ShotCount", config = Config.ABILITIES)
        private int shotCount = 3;
        @Configure(path = CONFIG_PATH + "Speed", config = Config.ABILITIES)
        private double speed = 2.0;
        @Configure(path = CONFIG_PATH + "Range", config = Config.ABILITIES)
        private double range = 27.0;
        @Configure(path = CONFIG_PATH + "HitboxSize", config = Config.ABILITIES)
        private double hitboxSize = 0.9;
        @Configure(path = CONFIG_PATH + "Damage", config = Config.ABILITIES)
        private double damage = 1.0;
        @Configure(path = CONFIG_PATH + "sensitivity", config = Config.ABILITIES)
        private double sensitivity = 100;
    }
}
