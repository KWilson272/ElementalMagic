package me.kwilson272.elementalmagic.api.activation;

import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.user.AbilityUser;

/**
 * Manages the activation system of the plugin. This class is responsible for
 * posting {@link Activation} instances, tracking the actions of users, and
 * managing passive and sequence-based activations. 
 *
 * The {@code ActivationManager} handles posting, or sending an {@code Activation}
 * instance to all {@link AbilityController}s that listen. In order for a
 * controller to receive activations through its activator method, it must be
 * registered using {@link #registerController(AbilityController)}. See the
 * {@link Activator} annotation for information on creating activator methods.<p>
 *
 * User tracking: the {@link ActivationManager} will only process activations of
 * {@code SequenceController} and {@code PassiveController} for {@link AbilityUser}s
 * that have been registered with the manager. Registration methods are provided in
 * {@link #registerUser(AbilityUser)} and {@link #unregisterUser(AbilityUser)}.
 */
public interface ActivationManager {

    void enable();

    void disable(boolean shutDown);

    /**
     * Registers an {@link AbilityUser} with an {@code ActivationManager},
     * enabling the user to activate sequences and passives.
     *
     * @param user the {@code AbilityUser} being registered
     */
    void registerUser(AbilityUser user);

    /**
     * Unregisters an {@link AbilityUser} with an {@code ActivationManager},
     * after which they will be unable to activate sequences and passives.
     *
     * @param user the {@code AbilityUser} being unregistered
     */
    void unregisterUser(AbilityUser user);

    /**
     * Registers an {@link AbilityController} with an {@code ActivationManager}
     * so its activator methods can be called.
     *
     * @param controller the {@code AbilityController} being registered
     */
    void registerController(AbilityController controller);

    /**
     * Posts the provided {@link Activation} and routes it to all
     * {@link AbilityController}s that listen to it.
     *
     * @param user the {@link AbilityUser} that caused the {@code Activation}
     * @param activation the {@code Activation} being posted
     */
    void postActivation(AbilityUser user, Activation activation);

    /**
     * Records an {@link Action} performed by the provided {@link AbilityUser}
     * and activates any valid sequences.
     *
     * @param user the {@code AbilityUser} that performed the {@code Action}
     * @param action the {@code Action} performed
     */
    void handleAction(AbilityUser user, Action action);

    /**
     * Clears all {@link Action}s the provided {@link AbilityUser} currently
     * has tracked.
     *
     * @param user the {@code AbilityUser} whose actions are being cleared
     */
    void clearTrackedActions(AbilityUser user);
    
    /**
     * Creates background passives for every user in the game.
     */
	void createPassives();

}
