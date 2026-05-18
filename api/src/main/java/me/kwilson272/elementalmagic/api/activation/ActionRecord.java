package me.kwilson272.elementalmagic.api.activation;

/**
 * Records a performed {@link Action} with the name of the 
 * {@link AbilityController} the user had selected when performing it.
 *
 * @param controllerName the name of the selected {@code AbilityController}
 * @param action the {@code Action} performed
 */
public record ActionRecord(String controllerName, Action action) { }
