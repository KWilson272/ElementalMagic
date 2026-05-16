package me.kwilson272.elementalmagic.api.ability;

import net.md_5.bungee.api.ChatColor;

import java.util.Collection;
import java.util.Set;

/**
 * Holds the attributes for an {@link Element} used to classify abilities.
 */
public interface Element {

    /**
     * @return the {@link String} name of an {@code Element}
     */
    String name();

    /**
     * @return the {@link ChatColor} used when displaying an {@code Element}
     * or related features
     */
    ChatColor color();

    /**
     * Gets all aliases that can be used to refer to an {@code Element} in place
     * of its full name. The returned {@link Collection} may be empty, but should
     * never be null.
     *
     * @return the {@code Collection} of {@link String} aliases for this
     * {@code Element}
     */
    Collection<String> aliases();

    /**
     * Gets all {@code Element}s that are considered parents of an element.
     * These may be used in conjunction with {@link #parentMode()}
     * to determine element grouping and usability. The returned {@link Set}
     * may be empty but should never be null.
     *
     * @return a {@code Set} of {@code Element} parents to an Element
     */
    Set<Element> parents();

    /**
     * @return the {@link ParentMode} of this {@code Element}
     */
    ParentMode parentMode();

    /**
     * @return the {@link String} permission node needed to use this {@code Element}
     */
    String permission();

    /**
     * Enum to dictate how an {@link Element} can be used by an
     * {@link me.kwilson272.elementalmagic.api.user.AbilityUser} given the elements
     * returned in {@link #parents()}
     */
    public static enum ParentMode {
        /** To use an {@code Element}, a user only requires one of the parents */
        ANY,
        /** To use an {@code Element}, a user requires all parents */
        ALL,
        /** To use an {@code Element}, a user requires no parents */
        NONE
    }
}
