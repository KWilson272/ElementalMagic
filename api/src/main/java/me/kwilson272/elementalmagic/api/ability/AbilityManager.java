package me.kwilson272.elementalmagic.api.ability;

import me.kwilson272.elementalmagic.api.user.AbilityUser;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * An {@code AbilityManager} provides functionality to track abilities, progress
 * and destroy instances, and query/retrieve active abilities.<p>
 *
 * {@code Ability} tracking is keyed by the {@link AbilityUser} that owns the
 * active instances, and users must be registered with an {@code AbilityManager}
 * for their abilities to be properly managed. In proceeding documentation,
 * tracked abilities may be referred to as 'active'.<p>
 *
 * <strong>Implementation Note:</strong> there should be no expectation that
 * {@code Ability} instances owned by unregistered users will be updated or
 * retrievable by implementations of {@code AbilityManager}.<p>
 *
 * The {@code AbilityUser} registration methods in this class are
 * {@link #registerUser(AbilityUser)} and {@link #unregisterUser(AbilityUser)}.
 * {@code Ability} instance tracking methods in this class are
 * {@link #addAbility(Ability)} and {@link #removeAbility(Ability)}.<p>
 *
 * The {@code Ability} lifecycle, consisting of progression and destruction is
 * managed by {@link #progressAll()}, where internal destructions (those initiated
 * by a false-returning {@link Ability#progress()}) will also be supported. The
 * {@code AbilityManager} provides manual instance destruction capability via the
 * {@link #destroyAbility(Ability)} method, alongside other convenience methods.<p>
 *
 * <strong>Implementation Note:</strong> implementations of {@code AbilityManager}
 * have no requirement to support the addition or removal of {@code Ability}
 * instances during the progress loop. Attempting to perform these operations
 * during {@code Ability#progress()} may lead to undefined behavior, if not
 * resulting in a thrown exception.<p>
 */
public interface AbilityManager {

    void enable();
    void disable(boolean shutDown);

    /**
     * Registers an {@link AbilityUser} with and enables the management of
     * their active {@link Ability} instances.
     *
     * @param user the {@code AbilityUser} being registered
     */
    void registerUser(AbilityUser user);

    /**
     * Unregisters an {@link AbilityUser} and handles the cleanup/destruction of
     * their active {@link Ability} instances.
     *
     * @param user the {@code AbilityUser} being unregistered
     */
    void unregisterUser(AbilityUser user);

    /**
     * Registers an {@link Ability} for tracking and progression via
     * {@link Ability#progress()}.
     *
     * @param ability the Ability being registered
     */
    void addAbility(Ability ability);

    /**
     * Removes an {@link Ability} from this {@code AbilityManager} without
     * destroying the instance. After this method is called, the manager will
     * not call {@link Ability#progress()}, and the ability instance will not
     * be returned in query-related methods.
     *
     * @param ability the {@code Ability} instance to be removed
     */
    void removeAbility(Ability ability);

    /**
     * Attempts to change the {@link AbilityUser} owner of the provided
     * {@link Ability}. Developers should also see both
     * {@link Ability#canChangeOwners(AbilityUser)} and
     * {@link Ability#changeOwner(AbilityUser)} <p>
     *
     * <strong>Implementation Note:</strong> if this method returns true and
     * is successful in changing the owners, changes MUST be reflected in all
     * following calls to the instance-querying methods in {@code AbilityManager}.
     * <p>
     *
     * <strong>Implementation Note:</strong> implementing methods must ensure
     * that successful calls to this method ensure the ownership changes are
     * reflected in the provided {@link Ability} instance. To achieve this, see
     * {@code Ability#changeOwner} and {@code Ability#canChangeOwners} <p>
     *
     * <strong> Usage/Implementation Note:</strong> like the rest of this class,
     * this method is not required to work directly during the progression of an
     * {@link Ability}. Calls to this method may be unsafe during {@link #progressAll()}
     * and {@link Ability#progress()}.
     *
     * @param ability the {@code Ability} whose owners are changing
     * @param newOwner the {@link AbilityUser} new owner of the ability
     * @return true if the owning user was changed, false otherwise
     */
    boolean changeOwner(Ability ability, AbilityUser newOwner);

    /**
     * Handles one tick of the progress loop for every active {@link Ability}
     * instance in the current game session. See {@link Ability#progress()} for
     * details on how to handle updates.
     */
    void progressAll();

    /**
     * Destroys the provided {@link Ability} instance, removing it from tracking.
     *
     * @param ability the {@code Ability} to be destroyed
     */
    void destroyAbility(Ability ability);

    /**
     * Destroys all active {@link Ability} instances in the current game session.
     */
    void destroyAll();

    /**
     * Destroys all instances of the provided {@link Ability} the {@link AbilityUser}
     * has active, and returns them in a {@link Collection}. This method should be
     * functionally equivalent to individually destroying instances via calls to
     * {@link #destroyAbility(Ability)}
     *
     * @param <T> the {@code Ability} type
     * @param user the {@code AbilityUser} owning the instances
     * @param abilityClass the class type of the instances to be destroyed
     * @return a {@link Collection} of destroyed abilities
     */
    <T extends Ability> Collection<T> destroyAbilities(AbilityUser user, Class<T> abilityClass);

    /**
     * Destroys all instances of the provided {@link Ability} the {@link AbilityUser}
     * has active if the provided destroy condition is met. Destroyed instances
     * are returned in a {@link Collection}. This method should be functionally
     * equivalent to destroying instances individually via calls to
     * {@link #destroyAbility(Ability)}
     *
     * @param <T> the {@code Ability} type
     * @param user the {@code AbilityUser} owning the instances
     * @param abilityClass the {@code Class} of the instances to be destroyed
     * @param destroyCondition the {@link Predicate} used to determine which
     *                         instances should be destroyed. Should return
     *                         true if an instance is to be destroyed, false
     *                         otherwise.
     * @return a {@code Collection} of destroyed abilities
     */
    <T extends Ability> Collection<T> destroyIf(AbilityUser user, Class<T> abilityClass,
                                                Predicate<T> destroyCondition);

    /**
     * Checks if the {@link AbilityUser} has any active instance of the specified
     * {@link Ability}.
     *
     * @param <T> the {@code Ability} type
     * @param user the {@link AbilityUser} that owns the Ability
     * @param abilityClass the {@code Class} of the Ability retrieved
     *
     * @return true if the AbilityUser has >= 1 active instance, false otherwise
     */
    <T extends Ability> boolean hasAbility(AbilityUser user, Class<T> abilityClass);

    /**
     * Gets a single active {@link Ability} instance that matches the supplied
     * type from the provided {@link AbilityUser} if one can be found.<p>
     *
     * <strong>Usage Note:</strong> this method makes no guarantee that the
     * {@code Ability} returned will be any specific instance given the user
     * has more than one matching instance active. Thus, developers should not
     * expect to find a specific instance using this method UNLESS the user is
     * known to only have one matching ability.
     *
     * @param <T> the type of {@code Ability}
     * @param user the {@code AbilityUser} owning the instance
     * @param abilityClass the {@link Class} of the ability to be found
     * @return an {@link Optional} containing the found instance, may be empty
     */
    <T extends Ability> Optional<T> getAbility(AbilityUser user, Class<T> abilityClass);

    /**
     * Returns all {@link Ability} instances that match the provided class
     * and are owned by the specifed {@link AbilityUser}, in a {@link Stream}
     *
     * @param <T> the {@code Ability} type
     * @param user the {@code AbilityUser} that owns the ability
     * @param abilityClass the {@code Class} of the ability retrieved
     *
     * @return a {@code Stream<T>} of found instances
     */
    <T extends Ability> Stream<T> getUserAbilities(AbilityUser user, Class<T> abilityClass);

    /**
     * Returns all {@link Ability} instances that match the provided type in a
     * {@link Stream}, regardless of owner.
     *
     * @param <T> the {@code Ability} type
     * @param abilityClass the {@code Class} of the ability retrieved
     *
     * @return a {@code Stream<T>} of found instances
     */
    <T extends Ability> Stream<T> getAllOf(Class<T> abilityClass);
}
