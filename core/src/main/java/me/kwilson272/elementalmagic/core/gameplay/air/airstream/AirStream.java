package me.kwilson272.elementalmagic.core.gameplay.air.airstream;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
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
import me.kwilson272.elementalmagic.core.gameplay.util.EntityUtil;
import me.kwilson272.elementalmagic.core.gameplay.util.VectorUtil;

public class AirStream extends AirAbility {

    protected static final ConfigValues CONFIG = new ConfigValues();

    private long cooldown;
    private long duration;
    private double range;
    private double speed;
    private double hitboxSize;
    private double breakAwayRange;

    private Location location;
    private Deque<List<Location>> particleLocs;

    private boolean hasGrabbed;
    private long endTime;
    private List<Entity> affected;

	public AirStream(AbilityUser user, AbilityController controller) {
		super(user, controller);

        cooldown = CONFIG.cooldown;
        duration = CONFIG.duration;
        range = CONFIG.range;
        speed = CONFIG.speed;
        hitboxSize = CONFIG.hitboxSize;
        breakAwayRange = CONFIG.breakAwayRange;
        
        location = user.player().getEyeLocation();
        particleLocs = new ArrayDeque<>();

        hasGrabbed = false;
        affected = new ArrayList<>();
	}

	@Override
	public boolean start() {
        return true;
	}

	@Override
	public boolean progress() {
        if (!user().canUse(controller(), false, false)
                || !user().player().isSneaking()
                || (hasGrabbed && System.currentTimeMillis() > endTime)) {
            return false; 
        }

        Player player = user().player();
        Location target = EntityUtil.getTarget(player, range);
        Vector direction = VectorUtil.getDirection(location, target);
        direction.normalize().multiply(speed);
        location.add(direction);

        removeInvalidEntities();
        collectNearbyEntities();
        dragEntities(direction);
        if (!hasGrabbed && !affected.isEmpty()) {
            hasGrabbed = true;
            endTime = System.currentTimeMillis() + duration;
        }

        particleLocs.offerFirst(getRingLocs(direction));
        while (particleLocs.size() > 10) {
            particleLocs.pollLast();
        }
        renderStream();

        return true;
	}

    private void removeInvalidEntities() {
        World world = user().player().getWorld();
        Iterator<Entity> iter = affected.iterator();
        while (iter.hasNext()) {
            Entity e = iter.next();

            if (e.isDead() || !e.getWorld().equals(world) 
                    || (e instanceof Player p && !p.isOnline())) {
                iter.remove();
                continue;
            }

            double maxDist = breakAwayRange * breakAwayRange;
            if (e.getLocation().distanceSquared(location) >= maxDist) {
                iter.remove();
            }
        }
    }

    private void collectNearbyEntities() {
        EffectHandler effectHandler = ElementalMagicApi.effectHandler();
        for (Entity e : EntityUtil.getNearbyEntities(location, hitboxSize)) {
            if (!e.equals(user().player()) && effectHandler.canAffect(e)) {
                affected.add(e);
            }
        }
    }

    private void dragEntities(Vector direction) {
        for (Entity e : affected) {
            Vector drag = VectorUtil.getDirection(e.getLocation(), location);
            drag.multiply(speed);
            
            double force = drag.lengthSquared();
            // Entity is close enough to the head that we should just 
            // pre-emptively push it towards the next location instead
            if (Double.isNaN(force) || force <= speed * speed) {
                drag = direction;
            }
           
            ElementalMagicApi.effectHandler().setVelocity(e, this, drag); 
        }

    }

    private List<Location> getRingLocs(Vector direction) {
        int count = 10;
        List<Location> locs = new ArrayList<>(count);

        Vector axis = direction.normalize();
        Vector ortho = VectorUtil.getOrthogonal(axis);
        double step = (2 * Math.PI) / count;

        for (int i = 0; i < count; ++i) {
            double angle = step * i;
            Vector vec = VectorUtil.rotateAroundVector(axis, ortho, angle);
            locs.add(location.clone().add(vec.multiply(0.5)));
        }

        return locs;
    }

    private void renderStream() {
        for (List<Location> ring : particleLocs) {
            for (Location loc : ring) {
                playAirParticles(loc, 1, 0, 0, 0);
            }
        }
    }

	@Override
	public void onDestruction() {
        user().addCooldown(name(), cooldown);
	}

	@Override
	public String name() {
        return "AirStream";
	}
    
    protected static class ConfigValues {

        private static final String CONFIG_PATH = AirStreamController.CONFIG_PATH;

        @Configure(path = CONFIG_PATH + "Cooldown", config = Config.ABILITIES)
        private long cooldown = 15000;
        @Configure(path = CONFIG_PATH + "Duration", config = Config.ABILITIES)
        private long duration = 7000;
        @Configure(path = CONFIG_PATH + "Range", config = Config.ABILITIES)
        private double range = 40.0;
        @Configure(path = CONFIG_PATH + "Speed", config = Config.ABILITIES)
        private double speed = 0.8;
        @Configure(path = CONFIG_PATH + "HitboxSize", config = Config.ABILITIES)
        private double hitboxSize = 2.0;
        @Configure(path = CONFIG_PATH + "BreakAwayRange", config = Config.ABILITIES)
        private double breakAwayRange = 4.0;
    }
}
