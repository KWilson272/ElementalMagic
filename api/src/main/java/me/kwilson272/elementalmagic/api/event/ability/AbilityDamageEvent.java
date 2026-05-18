package me.kwilson272.elementalmagic.api.event.ability;

import me.kwilson272.elementalmagic.api.ability.Ability;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.HandlerList;

/**
 * Event to be called any time an Ability causes damage to a LivingEntity
 */
public class AbilityDamageEvent extends AbilityAffectEntityEvent {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private double damage;

    public AbilityDamageEvent(Ability cause, LivingEntity affectedEntity, double damage) {
        super(cause, affectedEntity);
        this.damage = damage;
    }

    /**
     * Returns the amount of damage being applied to the Entity.
     *
     * @return the amount of damage being applied to the Entity
     */
    public double getDamage() {
        return damage;
    }

    /**
     * Sets the amount of damage applied to the LivingEntity.
     *
     * @param newDamage the Double amount of damage to be applied
     */
    public void setDamage(double newDamage) {
        damage = newDamage;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
