package me.kwilson272.elementalmagic.api.revertible;

/**
 * Manages the tracking and reversion of Revertible objects.
 */
public interface RevertibleManager {

    void enable();

    void disable(boolean shutDown);

    /**
     * Registers a Revertible object with this manager.
     *
     * @param revertible the Revertible to be tracked.
     */
    void register(Revertible revertible);

    /**
     * Reverts a Revertible object. This method can be used to revert an object
     * before its expiration time.
     *
     * @param revertible the Revertible to be reverted.
     */
    void revert(Revertible revertible);

    /**
     * Reverts all registered Revertible instances.
     */
    void revertAll();

    /**
     * Reverts {@link Revertible} objects whose revert time has been passed.
     */
    void revertExpired();
}

