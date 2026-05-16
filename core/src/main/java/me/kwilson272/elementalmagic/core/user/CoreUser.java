package me.kwilson272.elementalmagic.core.user;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.kwilson272.elementalmagic.api.ability.Element;
import me.kwilson272.elementalmagic.api.event.user.UserAddElementEvent;
import me.kwilson272.elementalmagic.api.event.user.UserRemoveElementEvent;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.api.user.UserProfile;

public class CoreUser implements AbilityUser {
    
    private final Player player;
    /** True if toggled on, false otherwise **/
    private final Map<Element, Boolean> elements; 

    public CoreUser(Player player) {
        this.player = player;
        this.elements = new HashMap<>();
    }

    @Override
    public Player player() {
        return player;
    }

    @Override
    public void loadProfile(UserProfile profile) {
    }
    
    @Override
    public UserProfile exportProfile() {
        return new UserProfile();
    }

	@Override
	public boolean addElement(Element element) {
        if (element == null || elements.containsKey(element)) {
            return false;
        }

        UserAddElementEvent event =
            new UserAddElementEvent(this, element);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        elements.put(element, true);
        return true;
	}

	@Override
	public boolean removeElement(Element element) {
        if (element == null || !elements.containsKey(element)) {
            return false;
        }

        UserRemoveElementEvent event = 
            new UserRemoveElementEvent(this, element);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        elements.remove(element);
        return true;
	}

	@Override
	public Collection<Element> getElements() {
        return Set.copyOf(elements.keySet());
	}

	@Override
	public boolean hasElement(Element element) {
        return element != null && elements.containsKey(element);
	}

	@Override
	public boolean canUseElement(Element element) {
        if (!isElementToggledOn(element) 
                || !player.hasPermission(element.permission())) {
            return false;
        }

        if (element.parentMode() == Element.ParentMode.NONE) {
            return true;
        }
        
        for (Element parent : element.parents()) {
            boolean usable = canUseElement(parent);
            if (usable && element.parentMode() == Element.ParentMode.ANY) {
                return true;
            } else if (!usable && element.parentMode() == Element.ParentMode.ALL) {
                return false;
            }
        }
        return true;
	}

	@Override
	public boolean isElementToggledOn(Element element) {
        return element != null 
            && elements.containsKey(element) 
            && elements.get(element);
	}

	@Override
	public void toggleElement(Element element, boolean toggleOn) {
        if (elements.containsKey(element)) {
            elements.put(element, toggleOn);
        }
	}
}
