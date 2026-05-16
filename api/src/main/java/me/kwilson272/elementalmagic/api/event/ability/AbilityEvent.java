package me.kwilson272.elementalmagic.api.event.ability;

import org.bukkit.event.Event;

import me.kwilson272.elementalmagic.api.ability.Ability;

public abstract class AbilityEvent extends Event {
    
    private final Ability ability;

    public AbilityEvent(Ability ability) {
        this.ability = ability;
    }

    /**
      @return the Ability related to this event.
     */
    public Ability getAbility() {
        return ability;
    }
}
