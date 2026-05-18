package me.kwilson272.elementalmagic.api.activation.activations;

import me.kwilson272.elementalmagic.api.activation.Action;
import me.kwilson272.elementalmagic.api.activation.Activation;

/**
 * {@link Activation} based on the performance of a {@link Action}. This
 * activation should be used when a controller only cares about the action
 * performed and not associated data.<p>
 *
 * There are two things of note about listening to this activation. This activation
 * will be posted in conjunction with any other activations that might be spawned
 * from the underlying event trigger. Thus, developers should be aware when
 * listening to related activations in the same controller.
 *
 * @param action the Action performed by the AbilityUser
 */
public record ActionActivation(Action action) implements Activation { }
