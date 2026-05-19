package me.kwilson272.elementalmagic.api.ability;

import me.kwilson272.elementalmagic.api.activation.Action;

/**
 * Handles the static aspects of an ability, including display related code,
 * unchanging attributes, and the activation and creation of {@link Ability}
 * instances.<p>
 *
 * Activations are to be handled by a {@code AbilityController}, however there
 * are no specific interface methods to implement to do so for most
 * {@link Activation}. Activation method should instead be defined according
 * to the {@link Activator} annotation.
 */
public interface AbilityController {

    /**
     * Gets the name used by an {@code AbilityController}. This is used by the
     * plugin to provide display and search functionality.
     *
     * @return the {@link String} name of this controller
     */
    String name();

    /**
     * Gets the {@link Element} an {@code AbilityController} is classified under.
     * This is what element an {@link AbilityUser} must be able to use in order
     * to create {@link Ability} instances using this controller.
     *
     * @return the {@code Element} this controller is classified under
     */
    Element element();

    /**
     * @return a {@link String} description of the functionalities of an
     * {@code AbilityController} or managed {@link Ability}
     */
    String description();

    /**
     * @return a {@link String} of instructions users must follow to activate
     * an {@code AbilityController}
     */
    String instructions();

    /**
     * @return the {@link String} permission needed to use a controller
     */
    String permission();

    /**
     * Gets if an {@code AbilityController} should be hidden from in-game users.
     * Hidden controllers should not show up in plugin-related display, and users
     * will be unable to search for them.
     *
     * @return true if this controller is hidden, false otherwise
     */
    boolean isHidden();

    /**
     * Gets if this {@code AbilityController} can be manually bound by users
     * in-game. If a controller is not intended to be activated via an action
     * while selected, this should generally return false.<p>
     *
     * <strong>Implementation Note:</strong> developers should be aware that
     * returning false may not prevent the controller from being bound by force
     * in-code; however, there is no expectation that non-bindable controllers
     * should support forced binding.
     *
     * @return true if an {@link AbilityUser} can bind this controller, false
     * otherwise.
     */
    boolean isBindable();

    /**
     * If this controller should be displayed as a passive.
     *
     * @return true if this controller is passive, false otherwise.
     */
    boolean isPassive();

    /**
     * Checks if the provided {@link Action} can be used to activate an 
     * {@code AbilityController}.
     *
     * @param action the {@code Action} being checked
     * @return true if the action can be used to activate this, false otherwise
     */
    boolean canActivateBy(Action action);
}
