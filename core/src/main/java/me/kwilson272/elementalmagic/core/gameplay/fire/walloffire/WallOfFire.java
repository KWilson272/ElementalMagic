package me.kwilson272.elementalmagic.core.gameplay.fire.walloffire;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.collision.AABB;
import me.kwilson272.elementalmagic.api.collision.BoundingVolume;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.api.util.BlockUtil;
import me.kwilson272.elementalmagic.core.gameplay.fire.FireAbility;
import me.kwilson272.elementalmagic.core.gameplay.util.EntityUtil;
import me.kwilson272.elementalmagic.core.gameplay.util.VectorUtil;

public class WallOfFire extends FireAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private long duration;
    private double maxAngle;
    private double placeRange;
    private double width;
    private double height;
    private double hitboxSize;
    private double damage;
    private long effectInterval;
    private long displayInterval;
   
    private long displayTime;
    private long endTime;

    private Location center;


    private Map<Location, BoundingVolume> wallLocs;
    private Map<Entity, Long> effectTimes;
    private Map<Entity, Location> prevEntityLocs;

	public WallOfFire(AbilityUser user, AbilityController controller) {
		super(user, controller);

        cooldown = CONFIG.cooldown;
        duration = CONFIG.duration;
        maxAngle = CONFIG.maxAngle;
        placeRange = CONFIG.placeRange;
        width = CONFIG.width;
        height = CONFIG.height;
        hitboxSize = CONFIG.hitboxSize;
        damage = CONFIG.damage;
        effectInterval = CONFIG.effectInterval;
        displayInterval = CONFIG.displayInterval;

        wallLocs = new HashMap<>();
        effectTimes = new HashMap<>();
        prevEntityLocs = new HashMap<>();
	}

	@Override
	public boolean start() {
        displayTime = 0;
        endTime = System.currentTimeMillis() + duration;

        initWall();
        return !wallLocs.isEmpty();
	}

    private void initWall() {
        Location loc = user().player().getEyeLocation();
        double pitch = Math.min(maxAngle, Math.max(-maxAngle, loc.getPitch()));
        pitch = Math.toRadians(pitch - 90);
        double yaw = Math.toRadians(loc.getYaw());
       
        Vector axis1 = new Vector(
            Math.cos(-pitch) * -Math.sin(yaw),
            Math.sin(-pitch),
            Math.cos(-pitch) * Math.cos(yaw)
        );
        yaw = Math.toRadians(loc.getYaw() + 90);
        Vector axis2 = new Vector(-Math.sin(yaw), 0, Math.cos(yaw));
        center = getPlaceLoc(); 

        double spacing = 0.5;
        for (double w = -width/2; w <= width/2; w += spacing) {
            Vector horiz = axis2.clone().multiply(w);
            for (double h = -height/2; h <= height/2; h += spacing) {
                Vector vert =  axis1.clone().multiply(h);
                Location l = center.clone().add(vert).add(horiz);
                
                if (!BlockUtil.isSolid(l.getBlock())) {
                    BoundingVolume bv = AABB.at(l, hitboxSize);
                    wallLocs.put(l, bv);    
                }
            }
        }
    }    

    private Location getPlaceLoc() {
        double spacing = 0.5;
        Location loc = user().player().getEyeLocation();
        Vector dir = loc.getDirection().multiply(spacing);        
        for (double i = 0; i <= placeRange; i += spacing) {
            Location newLoc = loc.clone().add(dir);
            if (BlockUtil.isSolid(newLoc.getBlock()) || 
                BlockUtil.collidesDiagonally(loc, newLoc, BlockUtil::isSolid)) {
                break;    
            }
            loc = newLoc;
        }
        return loc;
    }

	@Override
	public boolean progress() {
        if (!user().canUse(controller(), false, false)
                || System.currentTimeMillis() > endTime) {
            return false;        
        }

        if (System.currentTimeMillis() > displayTime) {
            displayTime = System.currentTimeMillis() + displayInterval;
            displayWall();
        }

        affectEntities();
        return true;
	}
        
    private void displayWall() {
        playFireSound(center);
        World world = center.getWorld();
        Particle particle = getFireParticle();
    
        for (Location loc : wallLocs.keySet()) {
            double off = 0.22;
            Location display = loc.clone().add(
                ThreadLocalRandom.current().nextDouble(-off, off),
                ThreadLocalRandom.current().nextDouble(-off, off),
                ThreadLocalRandom.current().nextDouble(-off, off)
            );
            
            // Make it appear the fire is traveling up the wall
            world.spawnParticle(particle, display, 0, 0, 1, 0, 0.2); 
        }
    }

    private void affectEntities() {
        Map<Entity, Location> newPrevLocs = new HashMap<>();

        Vector knock = new Vector();
        double boxSize = Math.max(width, height) + 2; // Account for entities just outside the wall
        for (Entity e : EntityUtil.getNearbyEntities(center, boxSize)) {
            if (e.equals(user().player())) {
                continue;
            }

            if (canAffectNow(e)) {
                // Set damage BEFORE the velocity to keep the wall feeling stickier
                ElementalMagicApi.effectHandler().damageEntity(e, this, damage);
                ElementalMagicApi.effectHandler().setVelocity(e, this, knock, 2);
                effectTimes.put(e, System.currentTimeMillis() + effectInterval);
            }
            newPrevLocs.put(e, e.getLocation());
        }

        prevEntityLocs = newPrevLocs;
    }

    private boolean canAffectNow(Entity e) {
        if (effectTimes.containsKey(e) 
                && System.currentTimeMillis() < effectTimes.get(e)) {
            return false;
        }

        // Sweep the entity's hitbox to account for high-speed phasing
        List<BoundingVolume> entityBoxes = sweepMotion(e);
        for (BoundingVolume entityBox : entityBoxes) {
            for (BoundingVolume wallBox : wallLocs.values()) {
                if (entityBox.intersects(wallBox)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<BoundingVolume> sweepMotion(Entity entity) {
        Location loc = prevEntityLocs.computeIfAbsent(entity,
                Entity::getLocation);
        Location newLoc = entity.getLocation();

        double eWidth = entity.getWidth();
        Vector dir = VectorUtil.getDirection(loc, newLoc);
        dir.normalize().multiply(-eWidth);

        List<BoundingVolume> entityBoxes = new ArrayList<>();
        BoundingBox bbox = entity.getBoundingBox();
        entityBoxes.add(AABB.fromBukkit(bbox));
        
        if (!Double.isFinite(dir.getX())
                || !Double.isFinite(dir.getY())
                || !Double.isFinite(dir.getZ())) {
            return entityBoxes;
        }

        while (loc.distanceSquared(newLoc) <= eWidth * eWidth) {
            newLoc.add(dir);
            entityBoxes.add(AABB.fromBukkit(bbox.shift(dir)));
        }

        return entityBoxes;
    }

	@Override
	public void onDestruction() {
        user().addCooldown("WallOffFire", cooldown);
	}
    
    protected static class ConfigValues {
        
        private static final String CONFIG_PATH = WallOfFireController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 11000;
        @Configure(path = CONFIG_PATH + "Duration", config = Config.ABILITIES)
        private long duration = 5000;
        @Configure(path = CONFIG_PATH + "MaxAngle", config = Config.ABILITIES)      
        private double maxAngle = 50.0;
        @Configure(path = CONFIG_PATH + "PlaceRange", config = Config.ABILITIES)
        private double placeRange = 5.0;
        @Configure(path = CONFIG_PATH + "Width", config = Config.ABILITIES)       
        private double width = 6.0;
        @Configure(path = CONFIG_PATH + "Height", config = Config.ABILITIES)       
        private double height = 11.0;
        @Configure(path = CONFIG_PATH + "HitBoxSize", config = Config.ABILITIES)       
        private double hitboxSize = 1.5;
        @Configure(path = CONFIG_PATH + "Damage", config = Config.ABILITIES)
        private double damage = 1.0;
        @Configure(path = CONFIG_PATH + "EffectInterval", config = Config.ABILITIES)       
        private long effectInterval = 500;
        @Configure(path = CONFIG_PATH + "DisplayInterval", config = Config.ABILITIES)
        private long displayInterval = 300;
    }
}
