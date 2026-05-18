package me.kwilson272.elementalmagic.api.activation;

/**
 * Activation interface used by the activation system and {@link AbilityController}
 * to manage or create {@link Ability} instances.<p>
 *
 * Like events, activations are posted and sent to all {@code AbilityController}
 * that observe them. See {@link Activator} for information on how to create
 * an activator method.
 */
public interface Activation { }
