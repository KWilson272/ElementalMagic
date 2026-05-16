package me.kwilson272.elementalmagic.api.event.user;

import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

import me.kwilson272.elementalmagic.api.ability.Element;
import me.kwilson272.elementalmagic.api.user.AbilityUser;

public class UserAddElementEvent extends UserEvent implements Cancellable {
   
    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Element element;
    private boolean isCancelled;

	public UserAddElementEvent(AbilityUser user, Element element) {
		super(user);
        this.element = element;
        this.isCancelled = false;
	}
    
    /**
     * @return the {@link Element} being added to the {@link AbilityUser}.
     */
    public Element getElement() {
        return element;
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
