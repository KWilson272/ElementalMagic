package me.kwilson272.elementalmagic.api.event.user;

import me.kwilson272.elementalmagic.api.user.AbilityUser;

import org.bukkit.event.HandlerList;

/**
* Event to be called when AbilityUser data is loaded from the database.
 */
public class UserLoadEvent extends UserEvent {

    private static HandlerList HANDLER_LIST = new HandlerList();

    public UserLoadEvent(AbilityUser user) {
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
