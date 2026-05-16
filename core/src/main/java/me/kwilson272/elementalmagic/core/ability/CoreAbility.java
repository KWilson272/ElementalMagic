package me.kwilson272.elementalmagic.core.ability;

import me.kwilson272.elementalmagic.api.ability.Ability;
import me.kwilson272.elementalmagic.api.user.AbilityUser;

public abstract class CoreAbility implements Ability {
        
    public AbilityUser user;
   
    public CoreAbility(AbilityUser user) {
        this.user = user;
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
}
