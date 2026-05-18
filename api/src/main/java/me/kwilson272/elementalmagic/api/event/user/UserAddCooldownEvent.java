package me.kwilson272.elementalmagic.api.event.user;

import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

import me.kwilson272.elementalmagic.api.user.AbilityUser;

/**
 * Called when an AbilityUser has a cooldown added.
 */
public class UserAddCooldownEvent extends UserEvent implements Cancellable {
  
    private static final HandlerList HANDLER_LIST = new HandlerList();
    
    private final String cooldownId;
    private long durationMillis;
    private boolean isCancelled;

    public UserAddCooldownEvent(AbilityUser user, String cooldownId, long durationMillis) {
		super(user);
        this.cooldownId = cooldownId;
        this.durationMillis = durationMillis;
        this.isCancelled = false;
	}
   
    /**
     * @return the String id being put on cooldown. 
     */
    public String getCooldownId() {
        return cooldownId;
    }
    
    /**
     * @return the duraiton in milliseconds the cooldown will persist for.
     */
    public long getDurationMillis() {
        return durationMillis;
    }
    
    /**
     * Sets the duration the cooldown will persist for.
     *
     * @param newDuration the new duration in milliseconds.
     */
    public void setDurationMillis(long newDuration) {
        durationMillis = newDuration;
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
