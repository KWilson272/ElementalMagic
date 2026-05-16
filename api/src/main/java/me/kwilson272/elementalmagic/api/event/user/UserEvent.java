package me.kwilson272.elementalmagic.api.event.user;

import org.bukkit.event.Event;

import me.kwilson272.elementalmagic.api.user.AbilityUser;

/**
 * Generic base class for an {@link AbilityUser} related event.
 */
public abstract class UserEvent extends Event {
    
    private final AbilityUser user;

    public UserEvent(AbilityUser user) {
        this.user = user;
    }

    /**
     * @return the {@link AbilityUser} related to this event.
     */
    public AbilityUser getUser() {
        return user;
    }
}
