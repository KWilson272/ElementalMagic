package me.kwilson272.elementalmagic.core.gameplay.air.airscooter;

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

public class AirScooterController extends CoreAbilityController {

    protected static final String CONFIG_PATH = "Abilities.Air.AirScooter.";

    @Configure(path = CONFIG_PATH + "Description", config = Config.LANGUAGE)
    private String description = "Create and ride a ball of air around the world!";
    @Configure(path = CONFIG_PATH + "Instructions", config = Config.LANGUAGE)
    private String instructions = "Sprint, jump, and left-click."; 

    public AirScooterController() {
        ElementalMagicApi.configManager().configure(this);
        ElementalMagicApi.configManager().configure(AirScooter.CONFIG);
    }

    @Activator(requireSelected = false)
    private Collection<Ability> handleActions(AbilityUser user,
                                              ActionActivation activation) {
        if (!canActivateBy(activation.action())) {
            return List.of();
        }

        AbilityManager manager = ElementalMagicApi.abilityManager();
        boolean removed = !manager.destroyAbilities(user, AirScooter.class).isEmpty();

        if (!removed && user.canUse(this, true, true)
                && activation.action() != Action.SNEAK_DOWN) {
            return List.of(new AirScooter(user, this));
        }

        return List.of();
    }

	@Override
	public String name() {
        return "AirScooter";
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
        return action == Action.LEFT_CLICK
            || action == Action.LEFT_CLICK_BLOCK
            || action == Action.HIT_ENTITY
            || action == Action.SNEAK_DOWN;
	}
}
