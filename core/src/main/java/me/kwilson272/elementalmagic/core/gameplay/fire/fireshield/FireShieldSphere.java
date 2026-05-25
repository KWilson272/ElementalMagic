package me.kwilson272.elementalmagic.core.gameplay.fire.fireshield;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.collision.BoundingVolume;
import me.kwilson272.elementalmagic.api.collision.Sphere;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.api.util.BlockUtil;
import me.kwilson272.elementalmagic.core.gameplay.fire.FireAbility;
import me.kwilson272.elementalmagic.core.gameplay.util.EntityUtil;

// TODO: Fix the particle spam 
public class FireShieldSphere extends FireAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private double radius;
    private long duration;
    private long burnDuration;
    private long animInterval;

    private boolean isInfinite;
    private long endTime;
    private long animTime;
    private List<Vector> sphereVecs;

	public FireShieldSphere(AbilityUser user, AbilityController controller) {
		super(user, controller);
        cooldown = CONFIG.cooldown;
        radius = CONFIG.radius;
        duration = CONFIG.duration;
        burnDuration = CONFIG.burnDuration;
        animInterval = CONFIG.animInterval;
        
        animTime = 0;
        sphereVecs = new ArrayList<>();
	}

	@Override
	public boolean start() {
        initVecs();
        isInfinite = duration < 0;
        endTime = System.currentTimeMillis() + duration;
        return true;
	}

    private void initVecs() {
        double spacing = 0.8;
        double step = Math.asin(spacing / (2 * radius));
        int countVert = (int) Math.ceil(Math.PI * 2 / step);
        for (int i = 0; i < countVert; ++i) {
            double angleVert = i * step;
            double y = Math.sin(angleVert) * radius;
            double xzMag = Math.cos(angleVert) * radius;
            
            double horSpacing = 2.0;
            double stepHoriz = Math.asin(horSpacing / (2 * xzMag));
            double countHoriz = (int) Math.ceil(Math.PI * 2 / stepHoriz);
            countHoriz = Math.max(1, countHoriz);

            for (int j = 0; j < countHoriz; ++j) {
                double angleHoriz = j * stepHoriz;
                double x = Math.cos(angleHoriz) * xzMag;
                double z = Math.sin(angleHoriz) * xzMag;
                sphereVecs.add(new Vector(x, y, z));
            }
        }
    }

	@Override
	public boolean progress() {
        if (!user().canUse(controller(), true, false)
                || (!isInfinite && System.currentTimeMillis() > endTime)
                || !user().player().isSneaking()) {
            return false;
        }
   
        Location loc = user().player().getLocation();
        loc.add(0, 1, 0); // Render with the center of the player
                          
        if (System.currentTimeMillis() > animTime) {
            animTime = System.currentTimeMillis() + animInterval;
            animateShield(loc);
        }
        burnEntities(loc);
        return true;
	}

    private void animateShield(Location loc) {
        World world = loc.getWorld();
        Particle particle = getFireParticle();
        
        boolean printed = false;
        for (Vector v : sphereVecs) {
            Location display = loc.clone().add(v);
            if (!printed) {
                printed = true;
            }

            Block block = display.getBlock();
            if (!BlockUtil.isSolid(block)) {
                world.spawnParticle(particle, display, 1, 0.2, 0.2, 0.2, 0.0125);
            }
        }
    }

    private void burnEntities(Location loc) {
        World world = loc.getWorld();
        BoundingVolume bv = Sphere.at(loc, radius);
        for (Entity e : EntityUtil.getNearbyEntities(world, bv)) {
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

    @Override
    public String name() {
        return "FireShieldSphere";
    };
    
    protected static class ConfigValues {
       
        private static final String CONFIG_PATH = FireShieldController.CONFIG_PATH + "Sphere.";
        
        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 0;
        @Configure(path = CONFIG_PATH + "Radius", config = Config.ABILITIES)
        private double radius = 3.0;
        @Configure(path = CONFIG_PATH + "Duration", config = Config.ABILITIES)
        private long duration = -1;
        @Configure(path = CONFIG_PATH + "BurnDuration", config = Config.ABILITIES)
        private long burnDuration = 2000;
        @Configure(path = CONFIG_PATH + "AnimationInterval", config = Config.ABILITIES)
        private long animInterval = 200;
    }
}
