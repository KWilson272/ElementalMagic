package me.kwilson272.elementalmagic.api.ability;

import java.util.Collection;
import java.util.List;

import me.kwilson272.elementalmagic.api.activation.ActionRecord;
import me.kwilson272.elementalmagic.api.user.AbilityUser;

/**
 * Defines controller functionality for creating {@link Ability} instances in
 * response to the performance of a sequence of actions.<p>
 *
 * <strong>Implementation Note:</strong> this interface is intended to be
 * implemented in tandem with the {@link AbilityController} interface.
 */
public interface SequenceController {

    /**
     * Gets the ordered sequence of actions that need to be performed for
     * {@link #createAbilities(AbilityUser)} to be called. The order of the
     * returned {@link List} should be such that the first element is the first
     * step the {@link AbilityUser} should perform, the second element being the
     * next step, and so on, with the final step being the last element. The
     * returned {@link List} may be empty but should never be null.
     *
     * @return the sequence of actions needed to activate this controller
     */
    List<ActionRecord> steps();

    /**
     * Creates {@link Ability} instances in response to the actions returned
     * in {@link #sequence()} being performed. The returned {@link Collection}
     * may be empty but should never be null.
     *
     * @param user the {@link AbilityUser} creating the abilities
     * @return a {@code Collection} of created instances
     */
    Collection<Ability> createAbilities(AbilityUser user);
}
