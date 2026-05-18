package me.kwilson272.elementalmagic.api.user;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.bukkit.entity.Player;

import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.ability.Element;

public interface AbilityUser {
    
    /**
     * @return the underlying {@link Player} object for an AbilityUser.   
     */
    Player player();

    /**
     * Loads the provided {@link UserProfile}'s data into an AbilityUser.
     * 
     * @param profile the UserProfile to be loaded.
     */
    void loadProfile(UserProfile profile); 

    /**
     * @return an AbilityUser's data as a {@link UserProfile}.
     */
    UserProfile exportProfile();

    /**
     * Gets the AbilityUser's currently bound controllers.
     *
     * @return an array of 9 bound AbilityControllers
     */
    AbilityController[] getBinds();

    /**
     * Checks if the AbilityUser has the provided controller bound to any slot
     *
     * @param controller the AbilityController being checked
     * @return true if the AbilityUser has the provided AbilityController
     * bound, false otherwise
     */
    boolean hasBound(AbilityController controller);

    /**
     * Gets the controller the user has bound to the provided slot
     *
     * @param slotNumber an Integer between 1-9 (inclusive)
     * @return an Optional containing the AbilityController bound to the
     * provided slot
     */
    Optional<AbilityController> getBindAt(int slotNumber);

    /**
     * Gets the controller bound to the user's currently selected slot
     *
     * @return an Optional of the selected AbilityController
     */
    Optional<AbilityController> getSelectedBind();

    /**
     * @return the String name of the user's selected AbilityController
     */
    String getSelectedBindName();

    /**
     * Checks if the AbilityUser has the provided controller selected.
     *
     * @param controller the AbilityController being checked
     * @return true if the controller is selected, false otherwise
     */
    boolean hasSelected(AbilityController controller);

    /**
     * Checks if the AbilityUser can use the provided AbilityController, given
     * additional checks.
     *
     * @param controller the AbilityController being checked
     * @param checkSelected if true, checks if the user has the controller selected
     * @param checkCooldowns if true, checks if the user has the controller on cooldown
     * @return true if the AbilityUser can use the controller, AND if the
     * checks prescribed via the parameters pass.
     */
    boolean canUse(AbilityController controller, boolean checkSelected,
                                                boolean checkCooldowns);

    /**
     * Checks if the AbilityUser can use the provided AbilityController without
     * regard to cooldowns, binds, or selection status.
     *
     * @param controller the AbilityController being checked
     * @return true if the user can use the controller, false otherwise
     */
    boolean canGenerallyUse(AbilityController controller);

    /**
     * Checks if the AbilityUser can bind the provided AbilityController
     *
     * @param controller the AbilityController being checked
     * @return true if the controller can be bound, false otherwise
     */
    boolean canBind(AbilityController controller);

    /**
     * Binds the provided controller to the selected slot. This will overwrite
     * whatever is currently in the slot.<p>
     *
     * Note: this method can fail to bind the controller. For guaranteed binds,
     * see {@link #bindControllerForceful(AbilityController, int)}.
     *
     * @param toBind     the AbilityController to be bound
     * @param slotNumber an Integer between 1-9 (inclusive) for the hotbar slot
     * @return boolean true if the controller was bound, false otherwise
     */
    boolean bindController(AbilityController toBind, int slotNumber);

    /**
     * Binds the provided controller to the selected slot. This will overwrite
     * whatever is currently in the slot.<p>
     *
     * Note: this method cannot fail. Improper usage may result in the plugin
     * functioning incorrectly or crashing.
     *
     * @param toBind     the AbilityController to be bound
     * @param slotNumber an Integer between 1-9 (inclusive) for the hotbar slot
     */
    void bindControllerForceful(AbilityController toBind, int slotNumber);

     /**
     * Removes and returns all binds the user does not satisfy the requirements
     * to have bound.
     *
     * @return a List of removed AbilityControllers
     */
    List<AbilityController> removeInvalidBinds();

    /** 
     * Adds an {@link Element} to an {@code AbilityUser} for use. If the user
     * already has the provided element, this method will return false.
     *
     * @param element the Element being added.
     * @return true if the addition was successful, false otherwise.
     */
    boolean addElement(Element element);
    
    /**
     * Removes an {@link Element} from an {@code AbilityUser}. If the user
     * does not have the provided element, this method will return false.
     *
     * @param element the Element being removed.
     * @return true if the removal was successful, false otherwise.
     */
    boolean removeElement(Element element);
    
    /**
     * @return a Collection of the user's added elements.
     */
    Collection<Element> getElements();
    
    /**
     * Checks if an {@code AbilityUser} has the provided {@link Element} added.
     * This method does not check if the user can use the element, see
     * {@link #canUseElement(Element)}.
     *
     * @param element the Element being checked.
     * @return true if the user has the element added, false otherwise.
     */
    boolean hasElement(Element element);
    
    /**
     * Checks if an {@code AbilityUser} can use an element for the creation or 
     * management of abilities.
     *
     * @param element the Element being checked.
     * @return true if the user can use the element, false otherwise.
     */
    boolean canUseElement(Element element);
    
    /**
     * Checks if the {@link Element} is toggled on. Elements that are toggled
     * off cannot be used.
     *
     * @param element the Element being checked.
     * @return true if the element is toggled on, false otherwise.
     */
    boolean isElementToggledOn(Element element);
    
    /**
     * Toggles an {@link Element} on/off for an {@code AbilityUser}. This method
     * will do nothing if the user does not have the element added.
     *
     * @param element the Element being toggled.
     * @param toggleOn if the element should be toggled on (true) or off (false).
     */
    void toggleElement(Element element, boolean toggleOn);
    
    /**
     * Puts an object or action on cooldown for this AbilityUser.
     *
     * @param cooldownId the String identifier of the cooldown
     * @param durationMillis how long in milliseconds the cooldown lasts
     * @return true if the cooldown was applied successfully, false otherwise
     */
    boolean addCooldown(String cooldownId, long durationMillis);

    /**
     * Removes a cooldown from this AbilityUser.
     *
     * @param cooldownId the String identifier of the cooldown
     * @return true if the cooldown was removed successfully, false otherwise
     */
    boolean removeCooldown(String cooldownId);
    
    /**
     * Checks if this AbilityUser has a specific action on cooldown.
     *
     * @param cooldownId the String identifier of the cooldown
     * @return true if the user has a cooldown with the provided identifier,
     * false otherwise
     */
    boolean isOnCooldown(String cooldownId);
}    

