package me.kwilson272.elementalmagic.core.effect;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ability.Ability;
import me.kwilson272.elementalmagic.api.effect.EffectHandler;
import me.kwilson272.elementalmagic.api.event.ability.AbilityAffectVelocityEvent;
import me.kwilson272.elementalmagic.api.event.ability.AbilityDamageEvent;
import me.kwilson272.elementalmagic.api.event.ability.AbilityPotionAddEvent;
import me.kwilson272.elementalmagic.api.event.ability.AbilityPotionRemoveEvent;
import me.kwilson272.elementalmagic.api.event.ability.AbilitySetFireTicksEvent;

public class EffectHandlerImpl implements EffectHandler {

    /**
     * prevent issues where an ability damaging an entity causes
     * EntityDamageEvent loops that other abilities respond to
    */
    private Entity currentDamager = null;
    private Entity currentVictim = null;
    private final Map<Entity, Integer> velocityPriorities;

    public EffectHandlerImpl() {
        velocityPriorities = new HashMap<>();
    }

    @Override
    public void enable() {
    }

    @Override
    public void disable(boolean shutDown) {
        currentDamager = null;
        currentVictim = null;
        velocityPriorities.clear();
    }

	@Override
	public boolean canAffect(Entity entity) {
        if (entity == null) {
            return false;
        }
        
        // TODO: Check region protection
        // TODO: Filter temporary entities out
        return !(entity instanceof Player p) 
            || p.getGameMode() != GameMode.SPECTATOR;
	}

	@Override
	public boolean damageEntity(Entity entity, Ability cause, double damage) {
        if (!(entity instanceof LivingEntity victim) 
                || !canAffect(entity) || damage <= 0) {
            return false;            
        }

        var event = new AbilityDamageEvent(cause, victim, damage);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        currentDamager = cause.user().player();
        currentVictim = victim;
        victim.damage(event.getDamage(), cause.user().player());
        currentDamager = null;
        currentVictim = null;

        return true;
	}

	@Override
	public boolean isDamaging(Entity entity) {
        return entity.equals(currentDamager);
	}

	@Override
	public boolean isBeingDamaged(Entity entity) {
        return entity.equals(currentVictim);
	}

	@Override
	public double convertDamage(Entity entity, Ability cause, double damage) {
        if (!(entity instanceof LivingEntity victim)
                || !canAffect(entity) || damage <= 0) {
            return damage;
        }

        var event = new AbilityDamageEvent(cause, victim, damage);
        Bukkit.getPluginManager().callEvent(event);
        return event.isCancelled() ? 0 : event.getDamage();
	}

	@Override
	public boolean setVelocity(Entity entity, Ability cause, Vector velocity) {
        return setVelocity(entity, cause, velocity, 0);
	}

	@Override
	public boolean setVelocity(Entity entity, Ability cause, Vector velocity, int priority) {
        if (!canAffect(entity)) {
            return false;
        }
    
        // TODO: Not sure if silently failing is the way to go
        int prevPriority = velocityPriorities.computeIfAbsent(entity, k -> 0);
        if (prevPriority > priority) {
            return true;
        }
        
        var event = new AbilityAffectVelocityEvent(cause, entity, velocity);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        velocityPriorities.put(entity, priority);
        entity.setVelocity(event.getVelocity());
        return true;
	}

    @Override
    public void clearVelocityPriorities() {
        velocityPriorities.clear();
    }

	@Override
	public boolean setFireDuration(Entity entity, Ability cause, long durationMillis) {
       if (!canAffect(entity)) {
            return false;
        }
        
        var event = new AbilitySetFireTicksEvent(cause, entity, durationMillis);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        entity.setFireTicks(event.getDurationTicks());
        return true;
	}

	@Override
	public long convertFire(Entity entity, Ability cause, long durationMillis) {
        if (!canAffect(entity)) {
            return 0;
        }

        var event = new AbilitySetFireTicksEvent(cause, entity, durationMillis);
        return event.isCancelled() ? 0 : event.getDurationMilliseconds();
	}

	@Override
	public boolean addPotionEffect(LivingEntity entity, Ability cause, PotionEffect effect) {
        if (!canAffect(entity)) {
            return false;
        }

        var event = new AbilityPotionAddEvent(cause, entity, effect);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        entity.addPotionEffect(event.getPotionEffect());
        return true;
	}

	@Override
	public boolean removePotionEffect(LivingEntity entity, Ability cause, PotionEffectType type) {
        if (!canAffect(entity) || !entity.hasPotionEffect(type)) {
            return false;
        }

        var event = new AbilityPotionRemoveEvent(cause, entity, type);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        entity.removePotionEffect(type);
        return true;
    }
}
