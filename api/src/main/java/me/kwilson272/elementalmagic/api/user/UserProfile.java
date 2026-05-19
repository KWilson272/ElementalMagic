package me.kwilson272.elementalmagic.api.user;

import java.util.Map;

import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.ability.Element;

/**
 * Holds a snapshot of the AbilityUser's non-player related data.
 */
public record UserProfile(AbilityController[] binds, 
                          Map<Element, Boolean> elements,
                          Map<String, AbilityController[]> presets) {}
