package me.kwilson272.elementalmagic.core.ability;

import me.kwilson272.elementalmagic.api.ability.AbilityController;

public abstract class CoreAbilityController implements AbilityController {
    
    @Override
    public String permission() {
        return element().permission() + "." + name();
    }

    @Override
    public boolean isHidden() {
        return false;
    }
}
