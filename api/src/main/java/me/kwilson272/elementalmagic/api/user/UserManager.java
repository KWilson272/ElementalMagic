package me.kwilson272.elementalmagic.api.user;

import java.util.Collection;
import java.util.Optional;

import org.bukkit.entity.Player;

/**
 * Handles the creation, removal, and distribution of AbilityUsers.
 */
public interface UserManager {
    
    void enable();
    void disable();

    /**
     * Creates an {@link AbilityUser} object for the provided Player.
     *
     * @return the created AbilityUser object.
     */
    AbilityUser create(Player player);
   
    /**
     * Removes the provided {@link AbilityUser} from gameplay.
     *
     * @param user the AbilityUser to be removed.
     */
    void destroy(AbilityUser user);

    /**
     * Retrieves the AbilityUser object for the specified Player.
     *
     * @param player the Player whose user-counterpart is being retrieved.
     * @return an Optional of type AbilityUser.
     */
    Optional<AbilityUser> get(Player player);
    
    /**
     * @return a Collection of all registered Abilityusers.
     */
    Collection<AbilityUser> getAll();
}
