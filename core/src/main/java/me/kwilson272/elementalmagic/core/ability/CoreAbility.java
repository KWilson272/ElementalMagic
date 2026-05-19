package me.kwilson272.elementalmagic.core.ability;

import me.kwilson272.elementalmagic.api.ability.Ability;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.user.AbilityUser;

public abstract class CoreAbility implements Ability {
        
    private AbilityUser user;
    private final AbilityController controller;
   
    public CoreAbility(AbilityUser user, AbilityController controller) {
        this.user = user;
        this.controller = controller;
    }

    @Override
    public boolean canChangeOwners(AbilityUser newOwner) {
        return false;
    }

    @Override
    public void changeOwner(AbilityUser newOwner) {
        user = newOwner;
    }

    @Override
    public AbilityUser user() {
        return user;
    }

    @Override
    public AbilityController controller() {
        return controller;
    }
}
