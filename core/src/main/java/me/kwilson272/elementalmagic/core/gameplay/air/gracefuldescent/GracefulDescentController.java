package me.kwilson272.elementalmagic.core.gameplay.air.gracefuldescent;

import java.util.Collection;
import java.util.List;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.Ability;
import me.kwilson272.elementalmagic.api.ability.Element;
import me.kwilson272.elementalmagic.api.activation.Action;
import me.kwilson272.elementalmagic.api.activation.Activator;
import me.kwilson272.elementalmagic.api.activation.activations.FallDamageActivation;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.ability.CoreAbilityController;
import me.kwilson272.elementalmagic.core.ability.CoreElement;

public class GracefulDescentController extends CoreAbilityController {


    private static final String CONFIG_PATH = "Abilities.Air.GracefulDescent.";

    @Configure(path = CONFIG_PATH + "Description", config = Config.LANGUAGE)
    private String description = "Air users can negate fall damage passively.";
    @Configure(path = CONFIG_PATH  + "Instructions", config = Config.LANGUAGE)
    private String instructions = "Fall from a large height.";

    public GracefulDescentController() {
        ElementalMagicApi.configManager().configure(this);
    }

    @Activator(requireSelected = false)
    private Collection<Ability> handleFall(AbilityUser user,
                                           FallDamageActivation activation) {
        if (user.canUse(this, false, false)) {
            activation.setDamage(0);
        }
        return List.of();
    }

	@Override
	public String name() {
        return "GracefulDescent";
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
	    return false;
    }
}

