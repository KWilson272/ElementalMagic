package me.kwilson272.elementalmagic.api.event.ability;

import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

import me.kwilson272.elementalmagic.api.ability.Ability;

/**
 * Event called before an {@link Ability} is started.
 */
public class AbilityStartEvent extends AbilityEvent implements Cancellable {

    private static final HandlerList HANDLER_LIST = new HandlerList();
        
    private boolean isCancelled;

    public AbilityStartEvent(Ability ability) {
		super(ability);
        isCancelled = false;
	}

	@Override
	public HandlerList getHandlers() {
        return HANDLER_LIST;
	}

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

	@Override
	public boolean isCancelled() {
        return isCancelled;
	}

	@Override
	public void setCancelled(boolean cancel) {
        isCancelled = cancel;
	}
}

