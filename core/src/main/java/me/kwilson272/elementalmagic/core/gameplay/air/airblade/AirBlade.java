package me.kwilson272.elementalmagic.core.gameplay.air.airblade;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
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
import me.kwilson272.elementalmagic.core.gameplay.air.AirAbility;
import me.kwilson272.elementalmagic.core.gameplay.components.Ray;
import me.kwilson272.elementalmagic.core.gameplay.util.EntityUtil;

public class AirBlade extends AirAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private double range;
    private double speed;
    private double growthFactor;
    private double angle;
    private double damage;
    private double hitboxSize;

    private Ray ray;

    public AirBlade(AbilityUser user, AbilityController controller) {
        super(user, controller);
       
        cooldown = CONFIG.cooldown;
        range = CONFIG.range;
        speed = CONFIG.speed;
        growthFactor = CONFIG.growthFactor;
        angle = CONFIG.angle;
        damage = CONFIG.damage;
        hitboxSize = CONFIG.hitboxSize;
    }

	@Override
	public boolean start() {
        Location loc = user().player().getEyeLocation();
        ray = new AirBladeRay(loc);
   
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
        return "AirBlade";
	}

    private class AirBladeRay extends Ray {
        
		private final Vector direction;
        private final double pitch;
        private final double yaw;
        private double radius;
        private int counter;

        public AirBladeRay(Location location) {
            super(location, speed, range);
            this.direction = location.getDirection();
            this.pitch = Math.toRadians(location.getPitch());
            this.yaw = Math.toRadians(location.getYaw());
            this.radius = 1.0;
            this.counter = 0;
        }

		@Override
		public boolean collides(Block block) {
		    return BlockUtil.isSolid(block);
        }

		@Override
		public boolean moveTo(Location loc) {
            boolean affected = false;
            boolean playParticles = ++counter % 2 == 0;

            Set<Block> checkedBlocks = new HashSet<>();
            for (Location l : getBladeLocs(loc)) {
                if (playParticles) {
                    playAirParticles(l, 1, 0, 0, 0);
                }

                Block block = l.getBlock();
                if (checkedBlocks.contains(block)) {
                    continue;
                }

                affected |= affectEntities(loc);
                checkedBlocks.add(block);
            }

            if (ThreadLocalRandom.current().nextInt(4) == 0) {
                playAirSound(loc);
            }

            radius += growthFactor;
            return !affected;
		}  

        private List<Location> getBladeLocs(Location loc) {
            List<Location> locs = new ArrayList<>();

            double x = -Math.sin(yaw);
            double z = Math.cos(yaw);
            double rad = Math.toRadians(angle) / 2;
            double spacing = 0.1;
            double step = 2 * Math.asin(spacing / (2 * radius));

            for (double i = pitch - rad; i <= pitch + rad; i += step) {
                double y = Math.sin(-i) * radius;
                double xzMag = Math.cos(-i) * radius;
                Vector vec = new Vector(x * xzMag, y, z * xzMag);
                locs.add(loc.clone().add(vec));
            }

            return locs;
        }

        private boolean affectEntities(Location loc) {
            boolean affected = false;
            EffectHandler effectHandler = ElementalMagicApi.effectHandler();
            for (Entity e : EntityUtil.getNearbyEntities(loc, hitboxSize)) {
                if (!e.equals(user().player())) {
                    affected |= effectHandler.damageEntity(e, AirBlade.this, damage);
                }
            }

            return affected;
        }


		@Override
		public Vector getDirection() {
            return direction.clone();
		}
    }

    protected static class ConfigValues {

        private static final String CONFIG_PATH = AirBladeController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 6200;
        @Configure(path = CONFIG_PATH + "Range", config = Config.ABILITIES)
        private double range = 19;
        @Configure(path = CONFIG_PATH + "Speed", config = Config.ABILITIES)
        private double speed = 2.0;
        @Configure(path = CONFIG_PATH + "GrowthFactor", config = Config.ABILITIES)
        private double growthFactor = 0.075;
        @Configure(path = CONFIG_PATH + "Angle", config = Config.ABILITIES)
        private double angle = 90.0;
        @Configure(path = CONFIG_PATH + "Damage", config = Config.ABILITIES)
        private double damage = 2.0;
        @Configure(path = CONFIG_PATH + "HitboxSize", config = Config.ABILITIES)
        private double hitboxSize = 1.5;
    }
}
