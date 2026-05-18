package me.kwilson272.elementalmagic.api.event.ability;

import me.kwilson272.elementalmagic.api.ability.Ability;

import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;
import org.bukkit.util.Vector;

/**
 * Event to be called when an Ability changes an Entity's velocity
 */
public class AbilityAffectVelocityEvent extends AbilityAffectEntityEvent {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private Vector velocity;

    public AbilityAffectVelocityEvent(Ability cause, Entity affected, Vector velocity) {
        super(cause, affected);
        this.velocity = velocity;
    }

    /**
     * Gets the velocity to be applied to the Entity.
     *
     * @return the velocity to be applied to the Entity
     */
    public Vector getVelocity() {
        return velocity.clone();
    }

    /**
     * Sets the velocity that will be applied to the Entity.
     *
     * @param newVelocity the Vector to be applied
     */
    public void setVelocity(Vector newVelocity) {
        velocity = newVelocity;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
