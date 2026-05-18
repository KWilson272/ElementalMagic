package me.kwilson272.elementalmagic.api.ability;

import java.util.Collection;
import java.util.Optional;

/**
 * Stores and provides search functionality for all {@link AbilityController} and 
 * {@link Element} usable in the current game session.
 */
public interface AbilityStorage {

    void enable();

    void disable(boolean shutDown);

    /**
     * Registers an {@link AbilityController} for use in the plugin.
     *
     * @param controller the AbilityController being registered.
     */
    void registerController(AbilityController controller);

    /**
     * @return a {@link Collection} of all registered {@link AbilityController}
     */
    Collection<AbilityController> getControllers();

    /**
     * Gets all registered {@link AbilityController}s that return the provided
     * {@link Element} in {@link AbilityController#element()}.
     *
     * @param element the {@code Element} search key
     * @return a {@link Collection} of found controllers
     */
    Collection<AbilityController> getControllersByElement(Element element);

    /**
     * Searches for an {@link AbilityController} using the provided {@link String}.
     * This method must provide case-insensitive searching.
     *
     * @param search the {@code String} controller search key.
     * @return an {@link Optional} with the found {@code AbilityController},
     * empty if none were present.
     */
    Optional<AbilityController> getController(String search);
    
    /** 
     * Registers a {@link} element for use in gameplay.
     */
    void registerElement(Element element);

    /**
     * @return a {@link Collection} of all registered {@link Element}s
     */
    Collection<Element> getElements();

    /**
     * Gets all registered {@link Element}s that return the provided parent
     * element in {@link Element#parents()}.
     *
     * @param parent the {@code Element} search key
     * @return a {@link Collection} of found elements
     */
    Collection<Element> getElementsByParent(Element parent);

    /**
     * Searches for an {@link Element} using the provided {@link String}. This
     * method must provide case-insensitive searching.
     *
     * @param search the {@code String} element name/alias search key
     * @return an {@link Optional} with the found {@code Element}, empty if none
     * were present
     */
    Optional<Element> getElement(String search);
}
