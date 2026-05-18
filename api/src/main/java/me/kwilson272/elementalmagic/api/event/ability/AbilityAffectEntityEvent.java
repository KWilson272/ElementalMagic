package me.kwilson272.elementalmagic.api.event.ability;

import me.kwilson272.elementalmagic.api.ability.Ability;

import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * Generic class for marking when an Ability is affecting an Entity.
 */
public abstract class AbilityAffectEntityEvent extends AbilityEvent implements Cancellable {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Entity affected;
    private boolean isCancelled;

    public AbilityAffectEntityEvent(Ability ability, Entity affected) {
        super(ability);
        this.affected = affected;
        this.isCancelled = false;
    }

    /**
     * Gets the Entity the Ability is affecting.
     *
     * @return the Entity the Ability is affecting
     */
    public Entity getAffected() {
        return  affected;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean isCancelled) {
        this.isCancelled = isCancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
