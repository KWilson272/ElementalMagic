package me.kwilson272.elementalmagic.api.ability;

import me.kwilson272.elementalmagic.api.user.AbilityUser;

/**
 * Handles the dynamic and instance-specific aspects of an {@code Ability},
 * managing per-tick updates and interactions with a world.<p>
 *
 * The {@code Ability} lifecycle is as follows:
 * <ol>
 *     <li> Creation: an {@link AbilityController} receives an activation and
 *     creates an instance of {@code Ability}.</li>
 *     <li> Startup: {@link #start()} is called for the instance, and any post-
 *     initialization code that must be run before {@link #progress()} is.</li>
 *     <li> Progress-loop: {@link #progress()} is called once per tick until the
 *     instance is destroyed. This is where world interactions should be done.</li>
 *     <li> Destruction: the instances is destroyed and final clean-up is done
 *     via {@link #onDestruction()}.</li>
 * </ol>
 */
public interface Ability {

    /**
     * Handles any logic that must be run before an {@code Ability} can enter
     * the progress loop. This method is only ever called once per instance.<p>
     *
     * Specifically, this method should be used for tasks that may prevent an
     * instance from progressing properly, or those that otherwise require runtime
     * data (like modified field values). This method must signal if the instance
     * is eligible to begin progression via its return value.<p>
     *
     * <strong>Implementation Note:</strong> this method must ensure that there
     * are no lasting side effects from calling it IF it returns false. For
     * example: if block modifications are made in this method, but the method
     * prevents the instance from progressing, the block modifications must be
     * undone. Similarly, {@link #onDestruction()} will not be called if this
     * method returns false.
     *
     * @return true if an {@link Ability} can enter its progress loop, false otherwise
     */
    boolean start();

    /**
     * Handles the per-tick update logic for an {@code Ability}, otherwise known
     * as the progress loop. This method will be called repeatedly until the
     * instance is destroyed. The return value of this method is used to signal
     * to the {@link AbilityManager} that the instance should exit progression
     * and be destroyed.
     *
     * @return true if this instance should remain in the progress loop (and
     * be called again on the next tick), false if it should exit and be
     * destroyed.
     */
    boolean progress();

    /**
     * Handles end of life logic and clean-up tasks necessary before an
     * instance is removed from the world.<p>
     *
     * <strong>Implementation Note:</strong> this method should ensure that it
     * cleans up any changes the {@link Ability} instance made to the world in
     * {@link #progress()} that will otherwise not be cleaned up. It should be
     * expected that {@code progress} will never be called again once this method
     * is run.<p>
     *
     * <strong>Implementation Note:</strong> this method should fully support
     * both automatic and manual destructions of an instance - where an automatic
     * destruction is initiated via returning false in {@code progress}, and
     * a manual destruction is initiated via a call to
     * {@link AbilityManager#destroyAbility(Ability)}.<p>
     *
     * <strong>Implementation Note:</strong> this method will be called during
     * plugin shutdown. Thus, developers should be cautious not to run any code
     * that will throw an exception during this state. An example includes
     * registering any bukkit tasks.
     */
    void onDestruction();

    /**
     * Checks if an {@code Ability} can change its owning user to the provided
     * {@link AbilityUser}.<p>
     *
     * @param newUser the new {@code AbilityUser}
     * @return true if the {@code Ability} allows ownership changes to the provded
     * user, false otherwise
     */
    boolean canChangeOwners(AbilityUser newUser);

    /**
     * Changes the {@link AbilityUser} that owns an {@code Ability} instance.
     * This method should handle both changing the owner and any logic needed
     * to ensure the instance runs the same for the new user as it did the
     * previous owner.<p>
     *
     * <strong>Implementation Note:</strong> if {@link #canChangeOwners(AbilityUser)}
     * returns true for the provided user, then {@link #user()} must return the
     * new {@code AbilityUser} after this method resolves.<p>
     *
     * <strong>Usage Note:</strong> developers should ensure that
     * before this method is called, {@link #canChangeOwners(AbilityUser)}
     * returns true for the provided user. Abilities are not required to support
     * ownership changes; thus, calling this method when the above condition is
     * not met may result in a no-op, errors, or otherwise dysfunctional abilities.
     * <p>
     *
     * <strong>Usage Note:</strong> developers should rarely call this method on
     * its own. Implementations of this method are not required to ensure the
     * changes made in this method are reflected in any of the ability querying
     * methods in {@link AbilityManager}. Thus, developers should instead call
     * {@link AbilityManager#changeOwner(Ability, AbilityUser)}.
     *
     * @param newUser the new {@code AbilityUser} owner of an instance
     */
    void changeOwner(AbilityUser newUser);

    /**
     * @return the {@link AbilityUser} that owns this instance
     */
    AbilityUser user();

    /**
     * @return the {@link AbilityController} that holds this instance's
     * static data.
     */
    AbilityController controller();

    /**
     * @return the String name of this {@code Ability}.
     */
    String name();
}
