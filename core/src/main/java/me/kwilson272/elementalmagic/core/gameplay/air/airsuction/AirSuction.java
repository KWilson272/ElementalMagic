package me.kwilson272.elementalmagic.core.gameplay.air.airsuction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.effect.EffectHandler;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.gameplay.air.AirAbility;
import me.kwilson272.elementalmagic.core.gameplay.components.Ray;
import me.kwilson272.elementalmagic.core.util.Blocks;
import me.kwilson272.elementalmagic.core.util.Entities;
import me.kwilson272.elementalmagic.core.util.Vectors;

/**
 * AirSuction push is sensitive, so a lot of the math and functionality from PK
 * is directly used here without much recoding. All credit goes to the developers
 */
public class AirSuction extends AirAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private double selectRange;
    private double range;
    private double speed;
    private double affectRadius;
    private double pushSelf;
    private double pushOthers;
    private double slidingFactor;

    private boolean isSourced;
    private boolean affectPlayer;
    private Location sourceLoc;
    private Ray ray;

	public AirSuction(AbilityUser user, AbilityController controller, boolean isSneak) {
		super(user, controller);

        cooldown = CONFIG.cooldown;
        selectRange = CONFIG.selectRange;
        range = CONFIG.range;
        speed = CONFIG.speed;
        affectRadius = CONFIG.affectRadius;
        pushSelf = CONFIG.pushSelf;
        pushOthers = CONFIG.pushOthers;
        slidingFactor = CONFIG.slidingFactor;

        isSourced = isSneak;
        affectPlayer = isSourced;
	}

	@Override
	public boolean start() {
        if (isSourced) {
            sourceLoc = getTargetLoc(selectRange);
        } else {
            fire();
        }

        return true;
	}

    protected void fire() {
        Location target = getTargetLoc(range);
        if (!isSourced) {
            sourceLoc = user().player().getEyeLocation();
        }

        isSourced = false;
        Vector vec = Vectors.getDirection(target, sourceLoc).normalize();
        ray = new AirSuctionRay(target, vec);
        user().addCooldown(name(), cooldown);
    }

    private Location getTargetLoc(double range) {
        Player player = user().player();
        Block block = Entities.getTargetBlock(player, range, Blocks::isSolid);

        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection();
        double dist = eyeLoc.distance(block.getLocation()) - 1.5;

        return eyeLoc.add(direction.multiply(dist));
    }

	@Override
	public boolean progress() {
        if (!user().canUse(controller(), isSourced, false)) {
            return false;
        }

        if (isSourced) {
            playAirParticles(sourceLoc, 3, 0.4, 0.4, 0.4);
            return isSourceViable();
        }
        
        affectEntities(ray.getLocation());
        return ray.progress();
	}

    private boolean isSourceViable() {
        Location eyeLoc = user().player().getEyeLocation();
        double maxDist = Math.pow(selectRange + 2, 2);
        return eyeLoc.getWorld().equals(sourceLoc.getWorld())
            && eyeLoc.distanceSquared(sourceLoc) <= maxDist;
    }

    private void affectEntities(Location loc) {
        EffectHandler effectHandler = ElementalMagicApi.effectHandler();
        double dist = loc.distance(sourceLoc);

        // This is basically exactly PK's math
        for (Entity e : getEntitiesAroundPoint(loc)) {
            boolean isPlayer = e.equals(user().player()); 
            if (isPlayer && !affectPlayer) {
                continue;
             }

            double factor = isPlayer ? pushSelf : pushOthers;
            double maxSpeed = 1.0 / pushOthers;
            Vector velocity = e.getVelocity();
        
            Vector push = ray.getDirection();
            if (Math.abs(push.getY()) > maxSpeed && !affectPlayer) {
                double y = Math.copySign(maxSpeed, push.getY());
                push.setY(y);
            }

            factor *= 1 - (dist / (2 * range));
        
            Location feet = user().player().getLocation();
            Block block = feet.add(0, -0.5, 0).getBlock();
            if (isPlayer && block.getType().isSolid()) {
                factor *= slidingFactor;
            }
        
            Vector pushDir = push.clone().normalize();
            double comp = velocity.dot(pushDir);
            if (comp > factor) {
                velocity.multiply(0.5);
                Vector v = pushDir.clone();
                double d = velocity.dot(v);
                velocity.add(v.multiply(d));
            
            } else if (comp + (factor * 0.5) > factor) {
                velocity.add(push.clone().multiply(factor - comp));
            
            } else {
                velocity.add(push.clone().multiply(factor * 0.5));
            }
        
            if (Double.isNaN(velocity.length())) {
                continue;
            }

            effectHandler.setVelocity(e, this, velocity);
            if (e.getFireTicks() > 0 && effectHandler.setFireDuration(e, this, 0)) {
                World world = e.getWorld();
                world.playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 1, 1);
            }
        }
    }

    // Simulates the Beta9 hitbox method
    private Collection<Entity> getEntitiesAroundPoint(Location loc) {
        List<Entity> entities = new ArrayList<>();
        double maxDist = affectRadius * affectRadius;
        for (Entity e : Entities.getNearbyEntities(loc, maxDist)) {
            if (e.getLocation().distanceSquared(loc) <= maxDist) {
                entities.add(e);
            }
        }

        return entities;
    }

	@Override
	public void onDestruction() {
	}

	@Override
	public String name() {
        return "AirSuction";
	}

    protected boolean isSourced() {
        return isSourced;
    }

    private class AirSuctionRay extends Ray {
    
        private final Vector direction;

        public AirSuctionRay(Location location, Vector direction) {
			super(location, speed, range);
            this.direction = direction;
		}

		@Override
		public boolean collides(Block block) {
            return Blocks.isSolid(block);
		}

		@Override
		public boolean moveTo(Location loc) {
            if (ThreadLocalRandom.current().nextInt(4) == 0) {
                playAirSound(loc);
            }
            playAirParticles(loc, 1, 0.275, 0.275, 0.275);
            return true;
		}

		@Override
		public Vector getDirection() {
            return direction.clone();
		}
    }

    protected static class ConfigValues {
    
        private static final String CONFIG_PATH = AirSuctionController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 500;
        @Configure(path = CONFIG_PATH + "SelectRange", config = Config.ABILITIES)
        private double selectRange = 10.0;
        @Configure(path = CONFIG_PATH + "Range", config = Config.ABILITIES)
        private double range = 15.0;
        @Configure(path = CONFIG_PATH + "Speed", config = Config.ABILITIES)
        private double speed = 1.1;
        @Configure(path = CONFIG_PATH + "AffectRadius", config = Config.ABILITIES)
        private double affectRadius = 2.0;
        @Configure(path = CONFIG_PATH + "PushSelf", config = Config.ABILITIES)
        private double pushSelf = 2.1;
        @Configure(path = CONFIG_PATH + "PushOthers", config = Config.ABILITIES)
        private double pushOthers = 1.5;
        @Configure(path = CONFIG_PATH + "SlidingFactor", config = Config.ABILITIES)
        private double slidingFactor = 0.5;
    }
}
