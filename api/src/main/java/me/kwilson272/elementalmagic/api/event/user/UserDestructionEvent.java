package me.kwilson272.elementalmagic.api.event.user;

import me.kwilson272.elementalmagic.api.user.AbilityUser;

import org.bukkit.event.HandlerList;

/**
 * Called whenever an AbilityUser object is destroyed.
 */
public class UserDestructionEvent extends UserEvent {

    private static HandlerList HANDLER_LIST = new HandlerList();

    public UserDestructionEvent(AbilityUser user) {
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
