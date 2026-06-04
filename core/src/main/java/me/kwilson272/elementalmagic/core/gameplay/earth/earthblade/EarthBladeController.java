package me.kwilson272.elementalmagic.core.gameplay.earth.earthblade;

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

public class EarthBladeController extends CoreAbilityController {

    protected static final String CONFIG_PATH = "Abilities.Earth.EarthBlade.";

    @Configure(path = CONFIG_PATH + "Description", config = Config.LANGUAGE)
    private String description = "Turn the earth in front of you into a deadly blade and slice your enemies into pieces.";
    @Configure(path = CONFIG_PATH + "Instructions", config = Config.LANGUAGE)
    private String instructions = "Tap-Sneak.";

    public EarthBladeController() {
        ElementalMagicApi.configManager().configure(this);
        ElementalMagicApi.configManager().configure(EarthBlade.CONFIG);
    }

    @Activator
    private Collection<Ability> handleAction(AbilityUser user,
                                             ActionActivation activation) {
        if (canActivateBy(activation.action())
                && user.canUse(this, true, true)) {
            return List.of(new EarthBlade(user, this));
        }
        return List.of();
    }

	@Override
	public String name() {
        return "EarthBlade";
	}

	@Override
	public Element element() {
        return CoreElement.EARTH;
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
