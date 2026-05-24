package me.kwilson272.elementalmagic.core.gameplay.fire.fireshield;

import java.util.Collection;
import java.util.List;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.Ability;
import me.kwilson272.elementalmagic.api.ability.AbilityManager;
import me.kwilson272.elementalmagic.api.ability.Element;
import me.kwilson272.elementalmagic.api.activation.Action;
import me.kwilson272.elementalmagic.api.activation.Activator;
import me.kwilson272.elementalmagic.api.activation.activations.ActionActivation;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.ability.CoreAbilityController;
import me.kwilson272.elementalmagic.core.ability.CoreElement;

public class FireShieldController extends CoreAbilityController {

    protected static final String CONFIG_PATH = "Abilities.Fire.FireShield.";

    @Configure(path = CONFIG_PATH + "Description", config = Config.LANGUAGE)
    private String description = "Advanced fire users are able to shape fire in defensive ways to block incoming attacks.";
    @Configure(path = CONFIG_PATH + "Instructions", config = Config.LANGUAGE)
    private String instructions = "Left-Click to create a rotating shield, hold-sneak to create a sphere.";


    public FireShieldController() {
        ElementalMagicApi.configManager().configure(this);
        ElementalMagicApi.configManager().configure(FireShieldRing.CONFIG);
        ElementalMagicApi.configManager().configure(FireShieldSphere.CONFIG);
    }

    @Activator
    private Collection<Ability> handleActions(AbilityUser user,
                                              ActionActivation activation) {
        if (!canActivateBy(activation.action()) 
                || !user.canUse(this, true, true)) {
            return List.of();
        }

        AbilityManager manager = ElementalMagicApi.abilityManager();
        if (manager.hasAbility(user, FireShieldRing.class)) {
            return List.of();
        }

        if (activation.action() == Action.SNEAK_DOWN) { 
            return List.of(new FireShieldSphere(user, this));
        } else if (!manager.hasAbility(user, FireShieldSphere.class)) {
            return List.of(new FireShieldRing(user, this));        
        }

        return List.of();
    }

	@Override
	public String name() {
        return "FireShield";
	}

	@Override
	public Element element() {
        return CoreElement.FIRE;
	}

	@Override
	public String description() {
        return description;
	}

	@Override
	public String instructions() {
        return instructions;
	}

	@Override
	public boolean isBindable() {
        return true;
	}

	@Override
	public boolean isPassive() {
        return false;
	}

	@Override
	public boolean canActivateBy(Action action) {
        return action == Action.LEFT_CLICK
            || action == Action.LEFT_CLICK_BLOCK
            || action == Action.HIT_ENTITY
            || action == Action.SNEAK_DOWN;
	}
}
