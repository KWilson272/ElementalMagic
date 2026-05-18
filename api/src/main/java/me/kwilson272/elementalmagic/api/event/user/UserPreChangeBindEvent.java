package me.kwilson272.elementalmagic.api.event.user;

import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.user.AbilityUser;

public class UserPreChangeBindEvent extends UserEvent implements Cancellable {
  
    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final int slotNumber;
    private final AbilityController controller;
    private boolean isCancelled;

    public UserPreChangeBindEvent(AbilityUser user, int slotNumber, AbilityController toBind) {
		super(user);
        this.slotNumber = slotNumber;
        this.controller = toBind;
	}
    
    /**
     * @return the slot number 1-9 (inclusive) being bound to.
     */
    public int getSlotNumber() {
        return slotNumber;
    }

    /**
     * @return the {@link AbilityController} being bound.
     */
    public AbilityController getController() {
        return controller;
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
