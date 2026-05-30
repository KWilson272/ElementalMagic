package me.kwilson272.elementalmagic.core.gameplay.air.airsweep;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

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
import me.kwilson272.elementalmagic.core.gameplay.util.VectorUtil;

public class AirSweep extends AirAbility {
    
    protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private int formDelay;
    private int formSpeed;
    private int streamCount;
    private double range;
    private double speed;
    private double hitboxSize;
    private double damage;
    private double knockback;

    private boolean isFired;
    private int ticksLived;
    private Location headPos1;
    private Location headPos2;

    private Deque<SweepStream> queuedStreams;
    private List<SweepStream> activeStreams;

	public AirSweep(AbilityUser user, AbilityController controller) {
		super(user, controller);
    
        cooldown = CONFIG.cooldown;
        formDelay = CONFIG.formDelay;
        formSpeed = CONFIG.formSpeed;
        streamCount = CONFIG.streamCount;
        range = CONFIG.range;
        speed = CONFIG.speed;
        hitboxSize = CONFIG.hitboxSize;
        damage = CONFIG.damage;
        knockback = CONFIG.knockback;

        isFired = false;
        ticksLived = 0;
        queuedStreams = new ArrayDeque<>();
        activeStreams = new ArrayList<>();
	}

	@Override
	public boolean start() {
        headPos1 = user().player().getEyeLocation();

        Block head = headPos1.getBlock();
        if (BlockUtil.isSolid(head) || BlockUtil.isLiquid(head)) {
            return false;
        }

        user().addCooldown(name(), cooldown);
        return true;
	}

	@Override
	public boolean progress() {
        if (!user().canUse(controller(), false, false)) {
            return false;
        }

        if (!isFired) {
            ++ticksLived;
            if (ticksLived < formDelay) {
                return true;
            }
            
            headPos2 = user().player().getEyeLocation();
            initStreams();
            isFired = true;
        }
       
        for (int i = 0; i < formSpeed; ++i) {
            if (queuedStreams.isEmpty()) {
                break;
            }
            activeStreams.add(queuedStreams.pollFirst());
        }
        activeStreams.removeIf(stream -> !stream.progress());
        return !queuedStreams.isEmpty() || !activeStreams.isEmpty();
	}

    private void initStreams() {
        double dist = 10.0;
        Vector startDir = headPos1.getDirection().multiply(dist);
        Vector endDir = headPos2.getDirection().multiply(dist);
        Location start = user().player().getEyeLocation().add(startDir);
        Location end = user().player().getEyeLocation().add(endDir);
        
        Location feet = user().player().getLocation();
        Vector vec = VectorUtil.getDirection(start, end);
        for (int i = 0; i < streamCount; ++i) {
            double t = (i + 1.0) / streamCount;
            Location loc = start.clone().add(vec.clone().multiply(t));
            Vector dir = VectorUtil.getDirection(feet, loc).normalize();
            queuedStreams.add(new SweepStream(feet.clone(), dir));
        }
    }

	@Override
	public void onDestruction() {
	}

	@Override
	public String name() {
        return "AirSweep";
	}

    private class SweepStream extends Ray {

        private final Vector direction;

		public SweepStream(Location location, Vector direction) { 
			super(location, speed, range);
            this.direction = direction;
		}

		@Override
		public boolean collides(Block block) {
            return BlockUtil.isSolid(block);
		}

		@Override
		public boolean moveTo(Location loc) {
            playAirParticles(loc, 1, 0, 0, 0);
		    
            Vector knock = direction.clone().multiply(knockback);
            EffectHandler effectHandler = ElementalMagicApi.effectHandler();
            for (Entity e : EntityUtil.getNearbyEntities(loc, hitboxSize)) {
                if (!e.equals(user().player())) {
                    effectHandler.setVelocity(e, AirSweep.this, knock);
                    effectHandler.damageEntity(e, AirSweep.this, damage);
                }
            }

            return true;
        }

		@Override
		public Vector getDirection() {
            return direction.clone();
		}
    }

    protected static class ConfigValues {
        
        private static final String CONFIG_PATH = AirSweepController.CONFIG_PATH;
        
        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 6500;
        @Configure(path = CONFIG_PATH + "FormDelay", config = Config.ABILITIES)
        private int formDelay = 8;
        @Configure(path = CONFIG_PATH + "FormSpeed", config = Config.ABILITIES)
        private int formSpeed = 3;
        @Configure(path = CONFIG_PATH + "StreamCount", config = Config.ABILITIES)
        private int streamCount = 30;
        @Configure(path = CONFIG_PATH + "Range", config = Config.ABILITIES)
        private double range = 12.0;
        @Configure(path = CONFIG_PATH + "Speed", config = Config.ABILITIES)
        private double speed = 1.8;
        @Configure(path = CONFIG_PATH + "HitboxSize", config = Config.ABILITIES)
        private double hitboxSize = 2.0;
        @Configure(path = CONFIG_PATH + "Damage", config = Config.ABILITIES)
        private double damage = 3.0;
        @Configure(path = CONFIG_PATH + "Knockback", config = Config.ABILITIES)
        private double knockback = 2.0;
    }
}
