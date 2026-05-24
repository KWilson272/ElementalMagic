package me.kwilson272.elementalmagic.core.gameplay.fire.fireshield;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.gameplay.fire.FireAbility;
import me.kwilson272.elementalmagic.core.gameplay.util.EntityUtil;
import me.kwilson272.elementalmagic.core.gameplay.util.VectorUtil;

public class FireShieldRing extends FireAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private double radius;
    private long duration;
    private double hitboxSize;
    private long burnDuration;
    
    private long endTime;
    private double animAngle;

	public FireShieldRing(AbilityUser user, AbilityController controller) {
		super(user, controller);
        cooldown = CONFIG.cooldown;
        radius = CONFIG.radius;
        duration = CONFIG.duration;
        hitboxSize = CONFIG.hitboxSize;
        burnDuration = CONFIG.burnDuration;
        animAngle = 0;
	}

	@Override
	public boolean start() {
        endTime = System.currentTimeMillis() + duration;
        return true;
	}

	@Override
	public boolean progress() {
        if (!user().canUse(controller(), true, false)
                || System.currentTimeMillis() > endTime) {
            return false;
        }
        
        Location loc = user().player().getEyeLocation();
        Vector dir = loc.getDirection().multiply(1.5);
        loc.add(dir);

        animateShield(loc);
        burnEntities(loc);
        return true;
	}

    private void animateShield(Location loc) {
        playFireSound(loc);
        
        World world = loc.getWorld();
        Particle particle = getFireParticle();
        Vector dir = user().player().getEyeLocation().getDirection();
        Vector ortho = VectorUtil.getOrthogonal(dir);

        double spacing = 0.3;
        double step = Math.asin(spacing / (2 * radius));
        int count = (int) Math.ceil(Math.toRadians(45) / step);
        for (int i = 0; i < count; ++i) {
            animAngle += i * step;
            Vector vec = VectorUtil.rotateAroundVector(dir, ortho, animAngle);
            // Want the fire to spray away from the rotation direction
            Vector move = vec.clone().crossProduct(dir).normalize();

            double dist = radius - (2 * spacing);
            Location disp = loc.clone().add(vec.clone().multiply(dist));
            for (int j = 0; j < 4; ++j) {
                double offset = 0.1;
                Location l = disp.add(
                    ThreadLocalRandom.current().nextDouble(-offset, offset),
                    ThreadLocalRandom.current().nextDouble(-offset, offset),
                    ThreadLocalRandom.current().nextDouble(-offset, offset)
                );

                double x = move.getX();
                double y = move.getY();
                double z = move.getZ();
                world.spawnParticle(particle, l, 0, x, y, z, 0.15); 
                disp.add(vec.clone().multiply(spacing/2));
                disp.subtract(dir.clone().multiply(spacing/2));
            }
        }
    }

    private void burnEntities(Location loc) {
        for (Entity e : EntityUtil.getNearbyEntities(loc, hitboxSize)) {
            if (!e.equals(user().player()) 
                    && e.getFireTicks() * 50 < burnDuration) {
                ElementalMagicApi.effectHandler().setFireDuration(e, this, burnDuration);        
            }
        }
    }

	@Override
	public void onDestruction() {
        user().addCooldown("FireShield", cooldown);
	}
    

    protected static class ConfigValues {
       
        private static final String CONFIG_PATH = FireShieldController.CONFIG_PATH + "Ring.";
        
        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 0;
        @Configure(path = CONFIG_PATH + "Radius", config = Config.ABILITIES)
        private double radius = 1.5;
        @Configure(path = CONFIG_PATH + "Duration", config = Config.ABILITIES)
        private long duration = 1500;
        @Configure(path = CONFIG_PATH + "HitboxSize", config = Config.ABILITIES)
        private double hitboxSize = 2.8;
        @Configure(path = CONFIG_PATH + "BurnDuration", config = Config.ABILITIES)
        private long burnDuration = 2000;
    }
}
