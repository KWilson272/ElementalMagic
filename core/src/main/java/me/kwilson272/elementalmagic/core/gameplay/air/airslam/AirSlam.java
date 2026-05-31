package me.kwilson272.elementalmagic.core.gameplay.air.airslam;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.gameplay.air.AirAbility;
import me.kwilson272.elementalmagic.core.gameplay.util.EntityUtil;

public class AirSlam extends AirAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private long duration;
    private double targetRange;
    private double hitboxSize;
    private double verticalForce;
    private double horizontalForce;

    private long endTime;
    private LivingEntity entity;

	public AirSlam(AbilityUser user, AbilityController controller) {
        super(user, controller);

        cooldown = CONFIG.cooldown;
        duration = CONFIG.duration;
        targetRange = CONFIG.targetRange;
        hitboxSize = CONFIG.hitboxSize;
        verticalForce = CONFIG.verticalForce;
        horizontalForce = CONFIG.horizontalForce;
	}

	@Override
	public boolean start() {
	    World world = user().player().getWorld();
        Location start = user().player().getEyeLocation();
        Vector direction = start.getDirection();

        RayTraceResult result = world.rayTraceEntities(
                start,
                direction,
                targetRange,
                hitboxSize,
                this::canTarget
        );

        if (result == null || result.getHitEntity() == null) {
            return false;
        }

        entity = (LivingEntity) result.getHitEntity();
        Vector knockUp = new Vector(0, verticalForce, 0);
        ElementalMagicApi.effectHandler().setVelocity(entity, this, knockUp);
        playAirSound(entity.getLocation());
       
        endTime = System.currentTimeMillis() + duration;
        user().addCooldown(name(), cooldown);
        return true;
    }

    private boolean canTarget(Entity e) {
        return !e.equals(user().player())
            && e instanceof LivingEntity
            && ElementalMagicApi.effectHandler().canAffect(e);
    }


	@Override
	public boolean progress() {
        if (!user().canUse(controller(), false, false) 
                || System.currentTimeMillis() > endTime) {
            return false;
        }

        double yaw = Math.toRadians(user().player().getEyeLocation().getYaw());
        double x = -Math.sin(yaw) * horizontalForce;
        double y = 0.05 * horizontalForce;
        double z = Math.cos(yaw) * horizontalForce;
        Vector knock = new Vector(x, y, z);
        ElementalMagicApi.effectHandler().setVelocity(entity, this, knock); 
        
        playAirParticles(entity.getLocation(), 10, 1, 1, 1);
        return true;
	}

	@Override
	public void onDestruction() {
	}

	@Override
	public String name() {
        return "AirSlam";
	}
    
    protected static class ConfigValues {
    
        private static final String CONFIG_PATH = AirSlamController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 7000;
        @Configure(path = CONFIG_PATH + "Duration", config = Config.ABILITIES)
        private long duration = 400;
        @Configure(path = CONFIG_PATH + "TargetRange", config = Config.ABILITIES)
        private double targetRange = 12.0;
        @Configure(path = CONFIG_PATH + "HitboxSize", config = Config.ABILITIES)
        private double hitboxSize = 2.5;
        @Configure(path = CONFIG_PATH + "VerticalForce", config = Config.ABILITIES)
        private double verticalForce = 2.0;
        @Configure(path = CONFIG_PATH + "HorizontalForce", config = Config.ABILITIES)
        private double horizontalForce = 3.0;

    }
}
