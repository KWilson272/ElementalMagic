package me.kwilson272.elementalmagic.api.user;

import java.util.Collection;

import org.bukkit.entity.Player;

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
}    

