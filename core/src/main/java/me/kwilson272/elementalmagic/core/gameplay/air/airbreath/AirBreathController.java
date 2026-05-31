package me.kwilson272.elementalmagic.core.gameplay.air.airbreath;

import java.util.Collection;
import java.util.List;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.Ability;
import me.kwilson272.elementalmagic.api.ability.Element;
import me.kwilson272.elementalmagic.api.activation.Action;
import me.kwilson272.elementalmagic.api.activation.Activator;
import me.kwilson272.elementalmagic.api.activation.activations.ActionActivation;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.ability.CoreAbilityController;
import me.kwilson272.elementalmagic.core.ability.CoreElement;

public class AirBreathController extends CoreAbilityController {

    protected static final String CONFIG_PATH = "Abilities.Air.AirBreath.";

    @Configure(path = CONFIG_PATH + "Description", config = Config.LANGUAGE)
    private String description = "Air users can augment their breath with high moving air currents.";
    @Configure(path = CONFIG_PATH + "Instructions", config = Config.LANGUAGE)
    private String instructions = "Hold-Sneak.";

    public AirBreathController() {
        ElementalMagicApi.configManager().configure(this);   
        ElementalMagicApi.configManager().configure(AirBreath.CONFIG);
    }

    @Activator
    private Collection<Ability> handleActions(AbilityUser user,
                                              ActionActivation activation) {
        if (canActivateBy(activation.action())
                && user.canUse(this, true, true)) {
            return List.of(new AirBreath(user, this));
        }
        return List.of();
    }

	@Override
	public String name() {
        return "AirBreath";
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
        return true;
	}

	@Override
	public boolean isPassive() {
        return false;
	}

	@Override
	public boolean canActivateBy(Action action) {
        return action == Action.SNEAK_DOWN;
	}
}


