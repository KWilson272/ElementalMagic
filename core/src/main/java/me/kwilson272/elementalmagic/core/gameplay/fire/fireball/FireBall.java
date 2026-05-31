package me.kwilson272.elementalmagic.core.gameplay.fire.fireball;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.effect.EffectHandler;
import me.kwilson272.elementalmagic.api.revertible.TempBlock;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.gameplay.components.Ray;
import me.kwilson272.elementalmagic.core.gameplay.fire.FireAbility;
import me.kwilson272.elementalmagic.core.util.Blocks;
import me.kwilson272.elementalmagic.core.util.Entities;

public class FireBall extends FireAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private double range;
    private double speed;
    private double sensitivity;
    private double hitboxSize;
    private double damage;
    private long burnDuration;
    private long fireRevertTime;
    private double fireDamage;

    private FireBallRay ray;

	public FireBall(AbilityUser user, AbilityController controller) {
		super(user, controller);
        
        cooldown = CONFIG.cooldown;
        range = CONFIG.range;
        speed = CONFIG.speed;
        sensitivity = CONFIG.sensitivity;
        hitboxSize = CONFIG.hitboxSize;
        damage = CONFIG.damage;
        burnDuration = CONFIG.burnDuration;
        fireRevertTime = CONFIG.fireRevertTime;
        fireDamage = CONFIG.fireDamage;
	}

	@Override
	public boolean start() {
        Location loc = user().player().getEyeLocation();
        Vector direction = loc.getDirection();
        loc.add(direction); // So we don't create fire at our feet
        ray = new FireBallRay(loc, direction);

        user().addCooldown(name(), cooldown);
        return true;
	}

	@Override
	public boolean progress() {
	    return user().canUse(controller(), false, false) && ray.progress();
    }

	@Override
	public void onDestruction() {
	}

	@Override
	public String name() {
        return "FireBall";
	}
    
    private boolean affectEntities(Location loc) {
        boolean affected = false;
        EffectHandler effectHandler = ElementalMagicApi.effectHandler();
        for (Entity e : Entities.getNearbyEntities(loc, hitboxSize)) {
            if (!e.equals(user().player())) {
                affected |= effectHandler.damageEntity(e, this, damage);
                if (e.getFireTicks() * 50 < burnDuration) {
                    affected |= effectHandler.setFireDuration(e, this, burnDuration);
                }
            }
        }

        return affected;
    }

    private class FireBallRay extends Ray {
    
        private Vector direction;

		public FireBallRay(Location location, Vector direction) {
			super(location, speed, range);
            this.direction = direction;
		}

		@Override
		public boolean collides(Block block) {
            return Blocks.isSolid(block) || Blocks.isLiquid(block);
		}

		@Override
		public boolean moveTo(Location loc) {
            Block below = loc.getBlock().getRelative(BlockFace.DOWN);
            if (canIgnite(below)) {
                TempBlock.builder(FireBall.this, getFireData())
                    .setDuration(fireRevertTime)
                    .setDamage(fireDamage)
                    .buildAt(below);
            }

            playEffects(loc);
            return !affectEntities(loc);
		}

        private void playEffects(Location loc) {
            World world = loc.getWorld();
            world.spawnParticle(Particle.LARGE_SMOKE, loc, 1, 0, 0, 0, 0);
            world.spawnParticle(getFireParticle(), loc, 1, 0, 0, 0, 0);

            if (ThreadLocalRandom.current().nextInt(5) == 0) {
                playFireSound(loc);
            }
        }

		@Override
		public Vector getDirection() {
            Vector newDir = user().player().getEyeLocation().getDirection();
            double weight = sensitivity / 100;
            newDir.multiply(weight);
            newDir.add(direction.clone().multiply(1 - weight));
            direction = newDir.normalize();
            return newDir.clone();
		}
    
    }

    protected static class ConfigValues {
    
        private static final String CONFIG_PATH = FireBallController.CONFIG_PATH;
        
        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 5000;
        @Configure(path = CONFIG_PATH + "Range", config = Config.ABILITIES)
        private double range = 12.0;
        @Configure(path = CONFIG_PATH + "Speed", config = Config.ABILITIES)
        private double speed = 2.0;
        @Configure(path = CONFIG_PATH + "Sensitivity", config = Config.ABILITIES)
        private double sensitivity = 0.0;
        @Configure(path = CONFIG_PATH + "HitboxSize", config = Config.ABILITIES)
        private double hitboxSize = 1.2;
        @Configure(path = CONFIG_PATH + "Damage", config = Config.ABILITIES)
        private double damage = 2.0;
        @Configure(path = CONFIG_PATH + "BurnDuration", config = Config.ABILITIES)
        private long burnDuration = 750;
        @Configure(path = CONFIG_PATH + "FireRevertTime", config = Config.ABILITIES)
        private long fireRevertTime = 500;
        @Configure(path = CONFIG_PATH + "FireDamage", config = Config.ABILITIES)
        private double fireDamage = 1.0;

    }
}
