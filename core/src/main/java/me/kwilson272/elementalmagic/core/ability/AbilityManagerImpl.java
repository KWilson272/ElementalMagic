package me.kwilson272.elementalmagic.core.ability;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.kwilson272.elementalmagic.api.ability.Ability;
import me.kwilson272.elementalmagic.api.ability.AbilityManager;
import me.kwilson272.elementalmagic.api.event.ability.AbilityOwnerChangeEvent;
import me.kwilson272.elementalmagic.api.user.AbilityUser;

/**
 * Core implementation of {@link AbilityManager}
 *
 * <strong>Implementation Note:</strong> This implementation does not support
 * registering abilities during {@link #progressAll()}. Any calls to 
 * {@link #addAbility(Ability)} or {@link #destroyAbility(Ability)} during
 * the progress loop either directly or indirectly are likely to cause a
 * {@link java.util.ConcurrentModificationException} to be thrown.
 */
public class AbilityManagerImpl implements AbilityManager {
    
    private final Map<AbilityUser, List<Ability>> instancesByUser;

    public AbilityManagerImpl() {
        this.instancesByUser = new HashMap<>();
    }

	@Override
	public void enable() {
	}

	@Override
	public void disable(boolean shutDown) {
        destroyAll();
        instancesByUser.clear();
	}

	@Override
	public void registerUser(AbilityUser user) {
        getInstances(user);
	}

    private List<Ability> getInstances(AbilityUser user) {
        return instancesByUser.computeIfAbsent(user, k -> new ArrayList<>());
    }

	@Override
	public void unregisterUser(AbilityUser user) {
        if (instancesByUser.containsKey(user)) {
            destroyAbilities(user, Ability.class);
            instancesByUser.remove(user);
        }
	}

	@Override
	public void addAbility(Ability ability) {
        getInstances(ability.user()).add(ability);
	}

	@Override
	public void removeAbility(Ability ability) {
        getInstances(ability.user()).remove(ability);
	}

	@Override
	public boolean changeOwner(Ability ability, AbilityUser newOwner) {
        if (!ability.canChangeOwners(newOwner)) {
            return false;
        }
        
        AbilityOwnerChangeEvent event = 
            new AbilityOwnerChangeEvent(ability, ability.user(), newOwner);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        removeAbility(ability);
        ability.changeOwner(newOwner);
        addAbility(ability);
        return true;
	}

	@Override
	public void progressAll() {
        for (AbilityUser user : instancesByUser.keySet()) {
            Iterator<Ability> iter = getInstances(user).iterator();
            while (iter.hasNext()) {
                Ability ability = iter.next();
                try {
                    if (!ability.progress()) {
                        iter.remove();
                        destroySafely(ability);
                    }
                    // TODO: insert collision structure
                } catch (Exception e) {
                    e.printStackTrace();
                    // TODO: we need better logging
                    Player owner = user.player();
                    owner.sendMessage("There was a error running " + ability);
                    destroySafely(ability);
                    iter.remove();
                }
            }
        }
	}
    
    // Extraction since we can remove from different points of execution
    private void destroySafely(Ability ability) {
        try {
            ability.onDestruction(); 
        } catch (Exception e) {
            e.printStackTrace();
            // TODO: we need better logging
            Player owner = ability.user().player();
            owner.sendMessage("There was an error removing: " + ability);
        }
    }

	@Override
	public void destroyAbility(Ability ability) {
        getInstances(ability.user()).remove(ability);
        destroySafely(ability);
	}

	@Override
	public void destroyAll() {
        for (AbilityUser user : instancesByUser.keySet()) {
            destroyAbilities(user, Ability.class);
        }
	}

	@Override
	public <T extends Ability> Collection<T> destroyAbilities(AbilityUser user, Class<T> abilityClass) {
        return destroyIf(user, abilityClass, a -> true);
	}

	@Override
	public <T extends Ability> Collection<T> destroyIf(AbilityUser user, 
                                                       Class<T> abilityClass,
			                                           Predicate<T> destroyCondition) {
        List<T> removed = new ArrayList<>();
        Iterator<Ability> iter = getInstances(user).iterator();
        while (iter.hasNext()) {
            Ability ability = iter.next();
            if (!abilityClass.isInstance(ability)) {
                continue;
            }

            T genericAbil = abilityClass.cast(ability);
            if (destroyCondition.test(genericAbil)) {
                iter.remove();
                destroySafely(ability);
                removed.add(genericAbil);
            }
        }
        
        return removed;
	}

	@Override
	public <T extends Ability> boolean hasAbility(AbilityUser user, Class<T> abilityClass) {
        return !getAbility(user, abilityClass).isEmpty();
	}

	@Override
	public <T extends Ability> Optional<T> getAbility(AbilityUser user, Class<T> abilityClass) {
        return getInstances(user).stream()
            .filter(abilityClass::isInstance)
            .map(abilityClass::cast)
            .findFirst();
	}

	@Override
	public <T extends Ability> Stream<T> getUserAbilities(AbilityUser user, Class<T> abilityClass) {
        return getInstances(user).stream()
            .filter(abilityClass::isInstance)
            .map(abilityClass::cast);
	}

	@Override
	public <T extends Ability> Stream<T> getAllOf(Class<T> abilityClass) {
        // stream.concat can cause stackoverflow so we do this
        List<Stream<T>> streams = new ArrayList<>();
        for (AbilityUser user : instancesByUser.keySet()) {
            streams.add(getUserAbilities(user, abilityClass));
        }
        return streams.stream().flatMap(stream -> stream);
	}
}
