package me.kwilson272.elementalmagic.core.gameplay.fire.firespin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
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

public class FireSpin extends FireAbility {

	protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private double range;
    private double speed;
    private double damage;
    private double knockback;
    private double hitboxSize;

    private List<SpinStream> streams;
    private Set<Entity> noAffect;

    public FireSpin(AbilityUser user, AbilityController controller) {
        super(user, controller);

        cooldown = CONFIG.cooldown;
        range = CONFIG.range;
        speed = CONFIG.speed;
        damage = CONFIG.damage;
        knockback = CONFIG.knockback;
        hitboxSize = CONFIG.hitboxSize;

        streams = new ArrayList<>();
        noAffect = new HashSet<>();
    }

    @Override
	public boolean start() {
        initStreams();

        World world = user().player().getWorld();
        Location loc = user().player().getLocation();
        world.playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 2, 0.6f);

        user().addCooldown("FireSpin", cooldown);
        return true;
	}

    private void initStreams() {
        int count = 30;
        double spacing = Math.PI * 2 / count;
        Location loc = user().player().getLocation().add(0, 0.3, 0);
        for (int i = 0; i < count; ++i) {
            double angle = i * spacing;
            Vector dir = new Vector(Math.cos(angle), 0, Math.sin(angle));
            streams.add(new SpinStream(loc.clone(), dir));
        }
    }

	@Override
	public boolean progress() {
        if (!user().canUse(controller(), false, false)) {
            return false;
        }

        streams.removeIf(stream -> !stream.progress());
        return !streams.isEmpty();
	}

	@Override
	public void onDestruction() {
	}

    @Override
    public String name() {
        return "FireSpin";
    }

    private void playParticles(Location loc) {
        World world = loc.getWorld();
        Particle particle;
        if (ThreadLocalRandom.current().nextInt(5) == 0) {
            particle = Particle.SMOKE;
        } else {
            particle = getFireParticle();
        }
        world.spawnParticle(particle, loc, 2, 0.2, 0.1, 0.2, 0.02);
    }

    private void affectEntities(Location loc, Vector dir) {
        EffectHandler effectHandler = ElementalMagicApi.effectHandler();
        Vector knock = dir.clone().multiply(knockback);

        for (Entity e : EntityUtil.getNearbyEntities(loc, hitboxSize)) {
            if (e.equals(user().player())) {
                continue;
            }
            
            playFireSound(e.getLocation());
            effectHandler.setVelocity(e, this, knock, 2);
            if (!noAffect.contains(e)) {
                effectHandler.damageEntity(e, this, damage);
                noAffect.add(e);
            }
        }
    }

    private class SpinStream extends Ray {

        private final Vector direction;

        public SpinStream(Location location, Vector direction) {
			super(location, speed, range);
            this.direction = direction;
		}

		@Override
		public boolean collides(Block block) {
            return BlockUtil.isSolid(block);
		}

		@Override
		public boolean moveTo(Location loc) {
            playParticles(loc);
            affectEntities(loc, direction);
            return true;
		}

		@Override
		public Vector getDirection() {
            return direction.clone();
		}
    }

    protected static class ConfigValues {

        private static final String CONFIG_PATH = FireSpinController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 8500;
        @Configure(path = CONFIG_PATH + "Range", config = Config.ABILITIES)
        private double range = 8.0;
        @Configure(path = CONFIG_PATH + "Speed", config = Config.ABILITIES)
        private double speed = 0.3;
        @Configure(path = CONFIG_PATH + "Damage", config = Config.ABILITIES)
        private double damage = 2.0;
        @Configure(path = CONFIG_PATH + "knockback", config = Config.ABILITIES) 
        private double knockback = 2.0;
        @Configure(path = CONFIG_PATH + "HitboxSize", config = Config.ABILITIES) 
        private double hitboxSize = 1.2;

    }
}
