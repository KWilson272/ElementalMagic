package me.kwilson272.elementalmagic.core.gameplay.air.airswipe;

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

public class AirSwipeController extends CoreAbilityController {

    protected static final String CONFIG_PATH = "Abilities.Air.AirSwipe.";

    @Configure(path = CONFIG_PATH + "Description", config = Config.LANGUAGE)
    private String description = "The fundamental ability for any air user, create a rapid arc of air and blow enemies away!";
    @Configure(path = CONFIG_PATH + "Instructions", config = Config.LANGUAGE)
    private String instructions = "Left-Click, or hold-sneak to charge and then release to fire.";

    public AirSwipeController() {
        ElementalMagicApi.configManager().configure(this);
        ElementalMagicApi.configManager().configure(AirSwipe.CONFIG);
    }

    @Activator
    private Collection<Ability> handleActions(AbilityUser user,
                                              ActionActivation activation) {
        if (!canActivateBy(activation.action())
                || !user.canUse(this, true, true)) {
            return List.of();
        }

        AbilityManager manager = ElementalMagicApi.abilityManager();
        boolean isCharging = manager.getUserAbilities(user, AirSwipe.class)
            .anyMatch(AirSwipe::isCharging);
        boolean isSneak = activation.action() == Action.SNEAK_DOWN;
        return isCharging ? List.of() : List.of(new AirSwipe(user, this, isSneak));
    }

	@Override
	public String name() {
        return "AirSwipe";
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
