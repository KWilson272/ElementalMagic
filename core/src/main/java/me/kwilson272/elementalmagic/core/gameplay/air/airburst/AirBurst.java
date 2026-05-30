package me.kwilson272.elementalmagic.core.gameplay.air.airburst;

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

public class AirBurst extends AirAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private long chargeTime;
    private double range;
    private double speed;
    private double damage;
    private double knockback;
    private double hitboxSize;
    private double coneAngle;
    private double coneSpacing;
    private double sphereSpacing;
    private double ringSpacing;
    private double fallHeight;

    private boolean isFall;
    private boolean isCharging;
    private long chargedTime;

    private List<Ray> rays;

	public AirBurst(AbilityUser user, AbilityController controller, boolean isFall) {
		super(user, controller);
	
        cooldown = CONFIG.cooldown;
        chargeTime = CONFIG.chargeTime;
        range = CONFIG.range;
        speed = CONFIG.speed;
        damage = CONFIG.damage;
        knockback = CONFIG.knockback;
        hitboxSize = CONFIG.hitboxSize;
        coneAngle = CONFIG.coneAngle;
        coneSpacing = CONFIG.coneSpacing;
        sphereSpacing = CONFIG.sphereSpacing;
        ringSpacing = CONFIG.ringSpacing;
        fallHeight = CONFIG.fallHeight;

        this.isFall = isFall;
        isCharging = !isFall;
            
        rays = new ArrayList<>();
    }

	@Override
	public boolean start() {
        if (isFall && user().player().getFallDistance() > fallHeight) {
            initRingBurst();
            user().addCooldown(name(), cooldown);
            isCharging = false;
        }
        
        chargedTime = System.currentTimeMillis() + chargeTime;
        return true;
    }

	@Override
	public boolean progress() {
        if (!user().canUse(controller(), isCharging, false)) {
            return false;
        }

        if (isCharging) {
            if (System.currentTimeMillis() >  chargedTime) {
                Location loc = user().player().getEyeLocation();
                playAirParticles(loc, 5, 1, 1, 1);
                if (!user().player().isSneaking()) {
                    initSphereBurst();
                    user().addCooldown(name(), cooldown);
                    isCharging = false;
                    return true;
                }
            }

            return user().player().isSneaking();
        }
    
        rays.removeIf(ray -> !ray.progress());
        return !rays.isEmpty();
	}

	@Override
	public void onDestruction() {
	}

	@Override
	public String name() {
        return "AirBurst";
	}

    protected void fire() {
        if (isCharging && System.currentTimeMillis() > chargedTime) {
            initConeBurst();
            user().addCooldown(name(), cooldown);
            isCharging = false;
        }
    }

    private void initRingBurst() {
        Location loc = user().player().getLocation(); 

        double angleVert = Math.toRadians(5);
        double y = Math.sin(angleVert);
        double xzMag = Math.cos(angleVert);

        double step = Math.toRadians(ringSpacing);
        for (double i = 0; i < Math.PI * 2; i += step) {
            double x = Math.cos(i) * xzMag;
            double z = Math.sin(i) * xzMag;
            Vector dir = new Vector(x, y, z);
            rays.add(new AirBurstRay(loc.clone(), dir));
        }
    }

    private void initSphereBurst() {
        Location loc = user().player().getEyeLocation();

        for (double i = -90; i <= 90; i += sphereSpacing) {
            double theta = Math.toRadians(i);
            double y = Math.sin(theta);
            double xzMag = Math.cos(theta);
            
            // Div by 0 at the poles
            if (xzMag < 0.001) {
                Vector vec = new Vector(0, y, 0);
                rays.add(new AirBurstRay(loc.clone(), vec));
                continue;
            }
            
            double step = sphereSpacing / xzMag;
            for (double j = 0; j < 360; j += step) {
                double phi = Math.toRadians(j);
                double x = Math.sin(phi) * xzMag;
                double z = Math.cos(phi) * xzMag;

                Vector vec = new Vector(x, y, z);
                rays.add(new AirBurstRay(loc.clone(), vec));
            }
        }
    }
    
    protected void initConeBurst() {
        Location loc = user().player().getEyeLocation();
       
        Vector dir = user().player().getEyeLocation().getDirection();
        Vector ortho = VectorUtil.getOrthogonal(dir);

        double spacing = Math.toRadians(coneSpacing);
        double angle = Math.toRadians(coneAngle / 2);
        for (double i = 0; i <= angle; i += spacing) {
            double magDir = Math.cos(i);
            double magRot = Math.sin(i);
            
            // Center of the cone will cause div by 0
            if (magRot < 0.001) {
                rays.add(new AirBurstRay(loc.clone(), dir.clone()));
                continue;
            }

            double step = spacing / magRot; 
            for (double j = 0; j <= 2 * Math.PI; j += step) {
                Vector rot = VectorUtil.rotateAroundVector(dir, ortho, j);
                Vector vec = dir.clone().multiply(magDir);
                vec.add(rot.multiply(magRot));

                rays.add(new AirBurstRay(loc.clone(), vec));
            }
        }
    }

    private class AirBurstRay extends Ray {

        private final Vector direction;

        public AirBurstRay(Location location, Vector direction) {
			super(location, speed, range);
            this.direction = direction;
		}

		@Override
		public boolean collides(Block block) {
            return BlockUtil.isSolid(block);
		}

		@Override
		public boolean moveTo(Location loc) {
            playAirParticles(loc, 1, 0.6, 0.6, 0.6);
            if (ThreadLocalRandom.current().nextInt(10) == 0) {
                playAirSound(loc);
            }

            affectEntities(loc);
            return true;
		}

        private void affectEntities(Location loc) {
            Vector baseKnock = direction.clone().multiply(knockback);
            EffectHandler effectHandler = ElementalMagicApi.effectHandler();

            for (Entity e : EntityUtil.getNearbyEntities(loc, hitboxSize)) {
                if (!e.equals(user().player())) {
                    Vector velocity = e.getVelocity().multiply(0.4);
                    Vector knock = baseKnock.clone().add(velocity);
                    effectHandler.setVelocity(e, AirBurst.this, knock);
                    effectHandler.damageEntity(e, AirBurst.this, damage);
                }
            }
        }

		@Override
		public Vector getDirection() {
            return direction.clone();
		}
    }

    protected static class ConfigValues {
    
        private static final String CONFIG_PATH = AirBurstController.CONFIG_PATH;
        
        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 10000; 
        @Configure(path = CONFIG_PATH + "ChargeTime", config = Config.ABILITIES)
        private long chargeTime = 1000;
        @Configure(path = CONFIG_PATH + "Range", config = Config.ABILITIES)
        private double range = 20.0;
        @Configure(path = CONFIG_PATH + "Speed", config = Config.ABILITIES)
        private double speed = 1.2;
        @Configure(path = CONFIG_PATH + "Damage", config = Config.ABILITIES)
        private double damage = 1.0;
        @Configure(path = CONFIG_PATH + "Knockback", config = Config.ABILITIES)
        private double knockback = 2.0;
        @Configure(path = CONFIG_PATH + "HitboxSize", config = Config.ABILITIES)
        private double hitboxSize = 2.0;
        @Configure(path = CONFIG_PATH + "ConeAngle", config = Config.ABILITIES)
        private double coneAngle = 45.0;
        @Configure(path = CONFIG_PATH + "ConeSpacing", config = Config.ABILITIES)
        private double coneSpacing = 15;
        @Configure(path = CONFIG_PATH + "SphereSpacing", config = Config.ABILITIES)
        private double sphereSpacing = 20;
        @Configure(path = CONFIG_PATH + "RingSpacing", config = Config.ABILITIES)
        private double ringSpacing = 15;
        @Configure(path = CONFIG_PATH + "FallHeight", config = Config.ABILITIES)
        private double fallHeight = 18.0;

    }
}
