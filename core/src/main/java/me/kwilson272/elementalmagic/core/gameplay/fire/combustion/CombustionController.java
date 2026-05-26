package me.kwilson272.elementalmagic.core.gameplay.fire.combustion;

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

public class CombustionController extends CoreAbilityController {

    protected static final String CONFIG_PATH = "Abilities.Fire.Combustion.";
    
    @Configure(path = CONFIG_PATH + "Description", config = Config.LANGUAGE)
    private String description = "Skilled fire users can infuse their projectiles with explosive power.";
    @Configure(path = CONFIG_PATH + "Instructions", config = Config.LANGUAGE)
    private String instructions = "Tap-Sneak to fire, left-click to detonate";

    public CombustionController() {
        ElementalMagicApi.configManager().configure(this);
        ElementalMagicApi.configManager().configure(Combustion.CONFIG);
    }

    @Activator
    private Collection<Ability> handleActions(AbilityUser user,
                                              ActionActivation activation) {
        if (!canActivateBy(activation.action())
                || !user.canUse(this, true, false)) {
            return List.of();        
        }

        if (activation.action() != Action.SNEAK_DOWN) {
            AbilityManager manager = ElementalMagicApi.abilityManager();
            Collection<Combustion> combustions =
                manager.getUserAbilities(user, Combustion.class).toList();
            for (Combustion combustion : combustions) {
                combustion.detonate();
                manager.destroyAbility(combustion);
            }
        
        } else if (!user.isOnCooldown("Combustion")) {
            return List.of(new Combustion(user, this));
        }

        return List.of();
    }

	@Override
	public String name() {
        return "Combustion";
	}

	@Override
	public Element element() {
        return CoreElement.COMBUSTION;
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
