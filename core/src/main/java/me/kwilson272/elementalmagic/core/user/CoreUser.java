package me.kwilson272.elementalmagic.core.user;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.ability.Element;
import me.kwilson272.elementalmagic.api.event.user.UserAddElementEvent;
import me.kwilson272.elementalmagic.api.event.user.UserPostChangeBindEvent;
import me.kwilson272.elementalmagic.api.event.user.UserPreChangeBindEvent;
import me.kwilson272.elementalmagic.api.event.user.UserRemoveElementEvent;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.api.user.UserProfile;

public class CoreUser implements AbilityUser {
   
    private static final int BINDS_SIZE = 9;

    private final Player player;
    private final AbilityController[] binds;
    /** True if toggled on, false otherwise **/
    private final Map<Element, Boolean> elements; 

    public CoreUser(Player player) {
        this.player = player;
        this.binds = new AbilityController[BINDS_SIZE];
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
	public AbilityController[] getBinds() {
        return Arrays.copyOf(binds, BINDS_SIZE);
	}

	@Override
	public boolean hasBound(AbilityController controller) {
        for (AbilityController bind : binds) {
            if (controller == bind) {
                return true;
            }
        }
        return false;
	}

	@Override
	public Optional<AbilityController> getBindAt(int slotNumber) {
        return Optional.ofNullable(binds[slotNumber - 1]);
	}

	@Override
	public Optional<AbilityController> getSelectedBind() {
        int slotNumber = player.getInventory().getHeldItemSlot();
        return getBindAt(slotNumber + 1);
	}

	@Override
	public String getSelectedBindName() {
        return getSelectedBind().map(AbilityController::name).orElse("null");
	}

	@Override
	public boolean hasSelected(AbilityController controller) {
        return controller == getSelectedBind().orElse(null);
	}

	@Override
	public boolean canUse(AbilityController controller, boolean checkSelected, 
                                                        boolean checkCooldowns) {
        return canGenerallyUse(controller)
            && (!checkSelected || hasSelected(controller))
            && (!checkCooldowns /* || !isOnCooldown(controller.name()) */);
	}

	@Override
	public boolean canGenerallyUse(AbilityController controller) {
        return controller != null
            && !player.isDead()
            && canUseElement(controller.element())
            && player.hasPermission(controller.permission());
	}

	@Override
	public boolean canBind(AbilityController controller) {
        return controller != null
            && controller.isBindable()
            && hasElement(controller.element())
            && player.hasPermission(controller.permission());
	}

	@Override
	public boolean bindController(AbilityController toBind, int slotNumber) {
        var preChange = new UserPreChangeBindEvent(this, slotNumber, toBind);
        Bukkit.getPluginManager().callEvent(preChange);
        if (preChange.isCancelled()) {
            return false;
        }
    
        bindControllerForceful(toBind, slotNumber);
        return true;
	}

    @Override
    public void bindControllerForceful(AbilityController toBind, int slotNumber) {
        binds[slotNumber-1] = toBind;
        var postChange = new UserPostChangeBindEvent(this, slotNumber);
        Bukkit.getPluginManager().callEvent(postChange);
    }

	@Override
	public List<AbilityController> removeInvalidBinds() {
        List<AbilityController> removedBinds = new ArrayList<>();
        for (int i = 0; i < BINDS_SIZE; ++i) {
            AbilityController controller = binds[i];
            if (controller != null && !canBind(controller)) {
                removedBinds.add(controller);
                bindControllerForceful(null, i+1);
            }
        }

        return removedBinds;
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
