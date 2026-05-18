package me.kwilson272.elementalmagic.api.event.user;

import org.bukkit.event.HandlerList;

import me.kwilson272.elementalmagic.api.user.AbilityUser;

/**
 * Called when an AbilityUser's cooldown expires or is manually removed.
 */
public class UserRemoveCooldownEvent extends UserEvent {
  
    private static final HandlerList HANDLER_LIST = new HandlerList();
    
    private final String cooldownId;

    public UserRemoveCooldownEvent(AbilityUser user, String cooldownId) {
		super(user);
        this.cooldownId = cooldownId;
	}
   
    /**
     * @return the String id being put on cooldown. 
     */
    public String getCooldownId() {
        return cooldownId;
    }

	@Override
	public HandlerList getHandlers() {
        return HANDLER_LIST;
	}

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
