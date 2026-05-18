package me.kwilson272.elementalmagic.api.activation;

import com.google.common.base.Objects;

/**
 * Records a performed {@link Action} with the name of the 
 * {@link AbilityController} the user had selected when performing it.
 *
 * @param controllerName the name of the selected {@code AbilityController}
 * @param action the {@code Action} performed
 */
public record ActionRecord(String controllerName, Action action) { 

    @Override
    public final boolean equals(Object arg0) {
        if (this == arg0) {
            return true;
        }

        return arg0 instanceof ActionRecord rec0
            && this.controllerName == rec0.controllerName
            && this.action == rec0.action;
    }

    @Override
    public final int hashCode() {
        return Objects.hashCode(controllerName, action);
    }
}
