package me.kwilson272.elementalmagic.api.event.user;

import org.bukkit.event.HandlerList;

import me.kwilson272.elementalmagic.api.user.AbilityUser;

/**
 * Called whenever an {@link AbilityUser} is created. Created users may not 
 * have their stored data loaded at the time this event is posted. 
 * See the {@link UserLoadEvent}.
 */
public class UserCreationEvent extends UserEvent {
    
    private static HandlerList HANDLER_LIST = new HandlerList();

    public UserCreationEvent(AbilityUser user) {
        super(user);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST; 
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
