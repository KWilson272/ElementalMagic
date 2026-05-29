package me.kwilson272.elementalmagic.core.gameplay.air.airswipe;

import java.util.ArrayList;
import java.util.List;
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
import me.kwilson272.elementalmagic.core.gameplay.util.VectorUtil;

public class AirSwipe extends AirAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private long chargeTime;
    private double chargeModifier;
    private double angle;
    private double range;
    private double speed;
    private double damage;
    private double knockback;
    private double hitboxSize;
    
    private boolean isCharging;
    private long startTime;
    private long chargedTime;

    private List<SwipeStream> streams;

	public AirSwipe(AbilityUser user, AbilityController controller, boolean isSneak) {
		super(user, controller);
        
        cooldown = CONFIG.cooldown;
        chargeTime = CONFIG.chargeTime;
        chargeModifier = CONFIG.chargeModifier;
        angle = CONFIG.angle;
        range = CONFIG.range;
        speed = CONFIG.speed;
        damage = CONFIG.damage;
        knockback = CONFIG.knockback;
        hitboxSize = CONFIG.hitboxSize;
    
        isCharging = isSneak;
        streams = new ArrayList<>();
	}

	@Override
	public boolean start() {
        Block headBlock = user().player().getEyeLocation().getBlock();
        if (BlockUtil.isSolid(headBlock) || BlockUtil.isLiquid(headBlock)) {
            return false;
        }

        startTime = System.currentTimeMillis();
        chargedTime = System.currentTimeMillis() + chargeTime;
        
        if (!isCharging) {
            fire();
        }
        return true;
	}

    private void fire() {
        // Handle overcharging:
        long curTime = Math.min(chargedTime, System.currentTimeMillis());
        double timeMod = ((curTime - startTime) / chargeTime);
        chargeModifier = (1 - timeMod) + (timeMod * chargeModifier);
               
        isCharging = false;
        initStreams();
        user().addCooldown(name(), cooldown);
    }
    
    private void initStreams() {
        Location eyeLoc = user().player().getEyeLocation();
        double pitch = Math.toRadians(eyeLoc.getPitch() - 90);
        double yaw = Math.toRadians(eyeLoc.getYaw());
        
        double x = -Math.sin(yaw);
        double y = Math.sin(-pitch);
        double z = Math.cos(yaw);
        double xzMag = Math.cos(-pitch);

        Vector axis = new Vector(x * xzMag, y, z * xzMag);
        Vector dir = eyeLoc.getDirection();

        double spacing = 1.0;
        double step = Math.asin(spacing / (2 * range));
        int count = (int) Math.ceil(Math.toRadians(angle) / step);
        for (int i = -count/2; i <= count/2; ++i) {
            double rad = i * step;
            Vector vec = VectorUtil.rotateAroundVector(axis, dir, rad);
            streams.add(new SwipeStream(eyeLoc.clone(), vec));
        }
    }

	@Override
	public boolean progress() {
        if (!user().canUse(controller(), isCharging, false)) {
            return false;
        }

        if (isCharging) {
            if (!user().player().isSneaking()) {
                fire();
            }
            if (System.currentTimeMillis() > chargedTime) {
                Location loc = user().player().getEyeLocation();
                playAirParticles(loc, 3, 1, 1, 1);
            }
            return true;
        }

        streams.removeIf(stream -> !stream.progress()); 
        return !streams.isEmpty();
	}

	@Override
	public void onDestruction() {
	}

	@Override
	public String name() {
        return "AirSwipe";
	}

    protected boolean isCharging() {
        return isCharging;
    }

    private class SwipeStream extends Ray {

        private Vector direction;
        private int counter;

        public SwipeStream(Location location, Vector direction) {
			super(location, speed, range);
            this.direction = direction;
            this.counter = 0;
		}

		@Override
		public boolean collides(Block block) {
            return BlockUtil.isSolid(block) || BlockUtil.isLiquid(block);
		}

		@Override
		public boolean moveTo(Location loc) {
            if (++counter % 2 == 0) {
                playAirParticles(loc);
            }
            if (ThreadLocalRandom.current().nextInt(5) == 0) {
                playAirSound(loc);
            }
            affectEntities(loc);
            return true;
		}

        private void affectEntities(Location loc) {
            double modKnock = chargeModifier * knockback;
            double modDamage = chargeModifier * damage;
            Vector knock = direction.clone().multiply(modKnock);

            EffectHandler effectHandler = ElementalMagicApi.effectHandler();
            for (Entity e : EntityUtil.getNearbyEntities(loc, hitboxSize)) {
                if (!e.equals(user().player())) {
                    effectHandler.setVelocity(e, AirSwipe.this, knock);
                    effectHandler.damageEntity(e, AirSwipe.this, modDamage);
                }
            }
        }

		@Override
		public Vector getDirection() {
            return direction.clone();
		}
    }
    
    protected static class ConfigValues {
   
        private static final String CONFIG_PATH = AirSwipeController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 1500;
        @Configure(path = CONFIG_PATH + "ChargeTime", config = Config.ABILITIES)
        private long chargeTime = 2000;
        @Configure(path = CONFIG_PATH + "ChargeModifier", config = Config.ABILITIES)
        private double chargeModifier = 2.0;
        @Configure(path = CONFIG_PATH + "Angle", config = Config.ABILITIES)
        private double angle = 12.0;
        @Configure(path = CONFIG_PATH + "Speed", config = Config.ABILITIES)
        private double speed = 1.4;
        @Configure(path = CONFIG_PATH + "Range", config = Config.ABILITIES)
        private double range = 12.0;
        @Configure(path = CONFIG_PATH + "Damage", config = Config.ABILITIES)
        private double damage = 2.0;
        @Configure(path = CONFIG_PATH + "Knockback", config = Config.ABILITIES)
        private double knockback = 0.5;
        @Configure(path = CONFIG_PATH + "HitboxSize", config = Config.ABILITIES)
        private double hitboxSize = 1.5;
    }
}
