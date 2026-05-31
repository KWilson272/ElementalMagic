package me.kwilson272.elementalmagic.core.gameplay.air.airshield;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.collision.BoundingVolume;
import me.kwilson272.elementalmagic.api.collision.Sphere;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.effect.EffectHandler;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.gameplay.air.AirAbility;
import me.kwilson272.elementalmagic.core.util.Entities;

public class AirShield extends AirAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private long duration;
    private double sideForce;
    private double outForce;
    private double radius;

    private boolean isInfinite;
    private long endTime;
    
    private double animAngle;
    private double animInc;

	public AirShield(AbilityUser user, AbilityController controller) {
		super(user, controller);

        cooldown = CONFIG.cooldown;
        duration = CONFIG.duration;
        sideForce = CONFIG.sideForce;
        outForce = CONFIG.outForce;
        radius = CONFIG.radius;

        animAngle = 0;
        animInc = Math.toRadians(5);
	}

	@Override
	public boolean start() {
        isInfinite = duration < 0;
        endTime = System.currentTimeMillis() + duration;
        return true;
	}

	@Override
	public boolean progress() {
        if (!user().canUse(controller(), true, false)
                || !user().player().isSneaking()
                || (!isInfinite && System.currentTimeMillis() > endTime)) {
            return false;
        }
        
        animateShield();
        affectEntities();
        playAirSound(user().player().getLocation());
        return true;
	}

    private void animateShield() {
        animAngle += animInc;
        Location center = user().player().getLocation().add(0, 1, 0);
       
        double angle = animAngle;
        double spacing = 1.0;
        double step = 2 * Math.asin(spacing / (2 * radius));
        int count = (int) Math.ceil(Math.PI / step);
        
        for (int i = -count/2; i < count/2; ++i) {
            // So each level of the shield isn't aligned
            angle += Math.toRadians(71);
            double rad = i * step;
            double y = Math.sin(rad) * radius;
            double xzMag = Math.cos(rad) * radius;
            
            int horizCount = 3;
            double horizStep = (2 * Math.PI) / horizCount;
            for (int j = 0; j < horizCount; ++j) {
                double radHoriz = angle + (j * horizStep);
                double x = Math.cos(radHoriz) * xzMag;
                double z = Math.sin(radHoriz) * xzMag;
                Vector vec = new Vector(x, y, z);
                Location loc = center.clone().add(vec);
                playAirParticles(loc, 1, 0, 0, 0);
            }
        }
    }

    private void affectEntities() {
        Location center = user().player().getLocation().add(0, 1, 0);
        EffectHandler effectHandler = ElementalMagicApi.effectHandler();

        World world = user().player().getWorld();
        BoundingVolume bv = Sphere.at(center, radius);
        for (Entity e : Entities.getNearbyEntities(world, bv)) {
            if (e.equals(user().player())) {
                continue;
            }

            Location eLoc = e.getLocation();
            double dx = eLoc.getX() - center.getX();
            double dz = eLoc.getZ() - center.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            double mod = (dist / (2 * radius)); 

            Vector out = new Vector(dx, 0, dz).multiply(mod * outForce);
            Vector side = new Vector(-dz, 0, dx).multiply(mod * sideForce);
            Vector knock = e.getVelocity().add(out).add(side);
            effectHandler.setVelocity(e, this, knock); 
        }
    }

	@Override
	public void onDestruction() {
        user().addCooldown(name(), cooldown);
	}

	@Override
	public String name() {
        return "AirShield";
	}

    protected static class ConfigValues {

        private static final String CONFIG_PATH = AirShieldController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 0;
        @Configure(path = CONFIG_PATH + "Duration", config = Config.ABILITIES)
        private long duration = -1;
        @Configure(path = CONFIG_PATH + "SidewaysForce", config = Config.ABILITIES)
        private double sideForce = 0.5;
        @Configure(path = CONFIG_PATH + "OutwardForce", config = Config.ABILITIES)
        private double outForce = 0.3;
        @Configure(path = CONFIG_PATH + "Radius", config = Config.ABILITIES)
        private double radius = 3.0;   
    }
}
