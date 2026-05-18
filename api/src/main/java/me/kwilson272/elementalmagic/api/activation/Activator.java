package me.kwilson272.elementalmagic.api.activation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an activator method to be called when a certain {@link Activation}
 * is posted.<p>
 *
 * The annotated method must:
 * <ul>
 *     <li>Be an instance method in an {@link AbilityController}</li>
 *     <li>Return a {@link Collection} of any created {@link Ability} instances</li>
 *     <li>Have its first parameter as AbilityUser} and</li>
 *     <li>Have its second parameter as a subclass of {@link Activation}</li>
 * </ul>
 *
 * An example of a valid activator method is as follows:
 * <pre>{@code
 * @Activator(requireSelected = true, requireElement = false)
 * private Collection<Ability> onActionActivation(AbilityUser user, ActionActivation activation) {
 *     if (activation.action() == Action.LEFT_CLICK) {
 *          return List.of(new AbilityImplementation(user));
 *     }
 *     return Collections.emptyList();
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Activator {

    /**
     * Indicates if the annotated method should be called only when the
     * responsible AbilityUser has the current AbilityController selected.
     *
     * @return true if the user must have the controller selected for the
     * activation, false otherwise
     */
    boolean requireSelected() default true;

    /**
     * Indicates if the annotated method should be called only when the
     * responsible AbilityUser is able to use the current AbilityController's
     * element.
     *
     * @return true if the user must have the controller's element for the
     * activation, false otherwise.
     */
    boolean requireElement() default true;
}
