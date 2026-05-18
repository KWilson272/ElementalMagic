package me.kwilson272.elementalmagic.api.event.ability;

import org.bukkit.event.HandlerList;

import me.kwilson272.elementalmagic.api.ability.Ability;

/**
 * Event called when an {@link Ability} is destroyed.
 */
public class AbilityDestructionEvent extends AbilityEvent {
    
    private static final HandlerList HANDLER_LIST = new HandlerList();

    public AbilityDestructionEvent(Ability ability) {
		super(ability);
	}

	@Override
	public HandlerList getHandlers() {
        return HANDLER_LIST;
	}

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}

