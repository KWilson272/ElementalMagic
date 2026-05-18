package me.kwilson272.elementalmagic.api.event.ability;

import me.kwilson272.elementalmagic.api.ability.Ability;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.HandlerList;

/**
 * Event to be called whenever an Ability heals a LivingEntity.
 */
public class AbilityHealEvent extends AbilityAffectEntityEvent {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private double healAmount;

    public AbilityHealEvent(Ability cause, LivingEntity affected, double healAmount) {
        super(cause, affected);
        this.healAmount = healAmount;
    }

    /**
     * Returns the amount of health to be restored.
     *
     * @return the amount of health to be restored
     */
    public double getHealAmount() {
        return healAmount;
    }
    /**
     * Sets the amount of health to be restored.
     *
     * @param newHealAmount the Double amount of health to be restored
     */
    public void setHealAmount(double newHealAmount) {
        healAmount = newHealAmount;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
