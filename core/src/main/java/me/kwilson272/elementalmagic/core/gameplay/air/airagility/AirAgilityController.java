package me.kwilson272.elementalmagic.core.gameplay.air.airagility;

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

public class AirAgilityController extends CoreAbilityController{

    protected static final String CONFIG_PATH = "Abilities.Air.AirAgility.";

    @Configure(path = CONFIG_PATH + "Description", config = Config.LANGUAGE)
    private String description = "Naturally being light on their feet, air users get a boost to both speed and jump power when sprinting.";
    @Configure(path = CONFIG_PATH + "Instructions", config = Config.LANGUAGE)
    private String instructions = "Sprint to activate.";

    public AirAgilityController() {
        ElementalMagicApi.configManager().configure(this);
        ElementalMagicApi.configManager().configure(AirAgility.CONFIG);
    }

    @Activator(requireSelected = false)
    private Collection<Ability> handleAction(AbilityUser user,
                                             ActionActivation activation) {
        if (!canActivateBy(activation.action())
                || !user.canUse(this, false, false)) {
            return List.of();
        }

        AbilityManager manager = ElementalMagicApi.abilityManager();
        if (manager.hasAbility(user, AirAgility.class)) {
            return List.of();
        }

        return List.of(new AirAgility(user, this));
    }

	@Override
	public String name() {
        return "AirAgility";
	}

	@Override
	public Element element() {
        return CoreElement.AIR;
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
        return false;
	}

	@Override
	public boolean isPassive() {
        return true;
	}

	@Override
	public boolean canActivateBy(Action action) {
        return action == Action.TOGGLE_SPRINT_ON;
	}
}
