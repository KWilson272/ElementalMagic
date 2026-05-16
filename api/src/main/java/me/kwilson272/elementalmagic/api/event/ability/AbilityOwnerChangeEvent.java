package me.kwilson272.elementalmagic.api.event.ability;

import me.kwilson272.elementalmagic.api.ability.Ability;
import me.kwilson272.elementalmagic.api.user.AbilityUser;

import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * Event to be called when an {@link Ability} is trying to change ownership.
 */
public class AbilityOwnerChangeEvent extends AbilityEvent implements Cancellable {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final AbilityUser curOwner;
    private final AbilityUser newOwner;
    private boolean isCancelled;

    public AbilityOwnerChangeEvent(Ability ability, 
                                   AbilityUser curOwner,
                                   AbilityUser newOwner) {
        super(ability);
        this.curOwner = curOwner;
        this.newOwner = newOwner;
        this.isCancelled = false;
    }

    public AbilityUser getCurrentOwner() {
        return curOwner;
    }

    public AbilityUser getNewOwner() {
        return newOwner;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        isCancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
