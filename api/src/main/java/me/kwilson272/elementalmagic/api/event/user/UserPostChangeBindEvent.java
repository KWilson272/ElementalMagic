package me.kwilson272.elementalmagic.api.event.user;

import org.bukkit.event.HandlerList;

import me.kwilson272.elementalmagic.api.user.AbilityUser;

public class UserPostChangeBindEvent extends UserEvent {
  
    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final int slotNumber;

    public UserPostChangeBindEvent(AbilityUser user, int slotNumber) {
		super(user);
        this.slotNumber = slotNumber;
	}
    
    /**
     * @return the slot number 1-9 (inclusive) bound to.
     */
    public int getSlotNumber() {
        return slotNumber;
    }

	@Override
	public HandlerList getHandlers() {
        return HANDLER_LIST;
	}

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
