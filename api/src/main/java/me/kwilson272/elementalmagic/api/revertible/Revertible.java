package me.kwilson272.elementalmagic.api.revertible;

/**
 * Manages a temporary change to game state that should be reverted and
 * cleaned up by the time the plugin shuts down, if not before.
 */
public interface Revertible {

    /**
     * Gets the duration this Revertible should persist for before being
     * cleaned up by the RevertibleManager.
     *
     * <p> Durations less than zero should be used for Revertibles that
     * should not be reverted via time.
     *
     * @return the duration in milliseconds this Revertible should persist for
     */
    long getDurationMillis();

    /**
     * @return the point in time in milliseconds after which this Revertible
     * should be reverted.
     */
    long getRevertTimeMillis();

    /**
     * Handles any clean up logic necessary to revert the changes made by this
     * revertible. NOTE: This method does not handle removing a Revertible from
     * tracking, see {@link RevertibleManager#revert(Revertible)}
     */
    void handleRevertTasks();

    /**
     * @return true if a {@code Revertible} has been reverted, false otherwise
     */
    boolean isReverted();
}

