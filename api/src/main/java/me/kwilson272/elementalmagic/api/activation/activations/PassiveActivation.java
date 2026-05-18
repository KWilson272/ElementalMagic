package me.kwilson272.elementalmagic.api.activation.activations;

import me.kwilson272.elementalmagic.api.activation.Activation;

/**
 * {@link Activation} run in the background and called once per tick per player.
 * This activation should be observed when a controller needs to be activated
 * regardless of if the user is acting or not.
 */
public record PassiveActivation() implements Activation { }
