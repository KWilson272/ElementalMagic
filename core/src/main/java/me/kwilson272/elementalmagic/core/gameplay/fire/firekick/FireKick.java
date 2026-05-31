package me.kwilson272.elementalmagic.core.gameplay.fire.firekick;

import java.util.ArrayList;
import java.util.List;
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
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.gameplay.components.Ray;
import me.kwilson272.elementalmagic.core.gameplay.fire.FireAbility;
import me.kwilson272.elementalmagic.core.util.Blocks;
import me.kwilson272.elementalmagic.core.util.Entities;
import me.kwilson272.elementalmagic.core.util.Vectors;

public class FireKick extends FireAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private double range;
    private double speed;
    private double angle;
    private double hitboxSize;
    private double damage;

    private List<KickStream> streams;

	public FireKick(AbilityUser user, AbilityController controller) {
		super(user, controller);

        cooldown = CONFIG.cooldown;
        range = CONFIG.range;
        speed = CONFIG.speed;
        angle = CONFIG.angle;
        hitboxSize = CONFIG.hitboxSize;
        damage = CONFIG.damage;

        streams = new ArrayList<>();
	}

	@Override
	public boolean start() {
        Block feetBlock = user().player().getLocation().getBlock();
        if (Blocks.isLiquid(feetBlock)) {
            return false;
        }

        World world = user().player().getWorld();
        world.playSound(feetBlock.getLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 1, 1);
        playFireSound(feetBlock.getLocation());

        initStreams();
        user().addCooldown("FireKick", cooldown);
        return true;
	}

    private void initStreams() {
        Location eyeLoc = user().player().getEyeLocation();
        Location footLoc = user().player().getLocation();
        // Offset so we spawn rays further from collidable ground
        footLoc.add(0, 0.3, 0); 

        Vector eyeDir = eyeLoc.getDirection();
        Location target = eyeLoc.add(eyeDir.multiply(range));
        Vector dir = Vectors.getDirection(footLoc, target).normalize();

        double pitch = Math.toRadians(eyeLoc.getPitch() - 90);
        double yaw = Math.toRadians(eyeLoc.getYaw());
        Vector axis = new Vector(
            Math.cos(-pitch) * -Math.sin(yaw),
            Math.sin(-pitch),
            Math.cos(-pitch) * Math.cos(yaw)
        );

        double spacing = 1.0;
        double step = Math.asin(spacing / (2 * range));
        int count = (int) Math.ceil(Math.toRadians(angle) / step);
        for (int i = -count/2; i <= count/2; ++i) {
            double rad = i * step;
            Vector vec = Vectors.rotateAroundVector(axis, dir, rad);
            streams.add(new KickStream(footLoc.clone(), vec));
        }
    }

	@Override
	public boolean progress() {
        if (!user().canUse(controller(), false, false)) {
            return false;
        }
        
        streams.removeIf(stream -> !stream.progress());
        if (streams.isEmpty()) {
            return false;
        }
        return true;
	}

	@Override
	public void onDestruction() {
	}

    @Override
    public String name() {
        return "FireKick";
    }

    private void affectEntities(Location loc) {
        for (Entity e : Entities.getNearbyEntities(loc, hitboxSize)) {
            if (!e.equals(user().player())) {
                ElementalMagicApi.effectHandler().damageEntity(e, this, damage);
            }
        }
    }

    private class KickStream extends Ray {

        private final Vector direction;

		public KickStream(Location location, Vector direction) {
			super(location, speed, range);
            this.direction = direction;
		}

		@Override
		public boolean collides(Block block) {
            return Blocks.isSolid(block);
		}

		@Override
		public boolean moveTo(Location loc) {
            World world = loc.getWorld();
            Particle fire = getFireParticle();
            world.spawnParticle(fire, loc, 1, 0.3, 0.3, 0.3, 0.02);
            if (ThreadLocalRandom.current().nextInt(5) == 0) {
                world.spawnParticle(Particle.SMOKE, loc, 1, 0.3, 0.3, 0.3, 0.01);
            }

            affectEntities(loc);
            return true;
		}

		@Override
		public Vector getDirection() {
            return direction.clone();
		}
    }

    protected static class ConfigValues {

        private static final String CONFIG_PATH = FireKickController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 5600;
        @Configure(path = CONFIG_PATH + "Range", config = Config.ABILITIES)
        private double range = 8.0;
        @Configure(path = CONFIG_PATH + "Speed", config = Config.ABILITIES)       
        private double speed = 2.0;
        @Configure(path = CONFIG_PATH + "Angle", config = Config.ABILITIES)       
        private double angle = 60;
        @Configure(path = CONFIG_PATH + "HitboxSize", config = Config.ABILITIES)       
        private double hitboxSize = 1.2;
        @Configure(path = CONFIG_PATH + "Damage", config = Config.ABILITIES)
        private double damage = 2.0;

    }
}

