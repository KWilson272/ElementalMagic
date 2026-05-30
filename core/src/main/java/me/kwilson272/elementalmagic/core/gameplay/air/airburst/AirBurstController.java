package me.kwilson272.elementalmagic.core.gameplay.air.airburst;

import java.util.Collection;
import java.util.List;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.Ability;
import me.kwilson272.elementalmagic.api.ability.Element;
import me.kwilson272.elementalmagic.api.activation.Action;
import me.kwilson272.elementalmagic.api.activation.Activator;
import me.kwilson272.elementalmagic.api.activation.activations.ActionActivation;
import me.kwilson272.elementalmagic.api.activation.activations.FallDamageActivation;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.ability.CoreAbilityController;
import me.kwilson272.elementalmagic.core.ability.CoreElement;

public class AirBurstController extends CoreAbilityController {

    protected static final String CONFIG_PATH = "Abilities.Air.AirBurst.";

    @Configure(path = CONFIG_PATH + "Description", config = Config.LANGUAGE)
    private String description = "Create rapidly expanding bursts of air to blast opponents back.";
    @Configure(path = CONFIG_PATH + "Instructions", config = Config.LANGUAGE)
    private String instructions = "Hold-Sneak to charge, left click when particles appear. Additionally " +
                                  "fall from a great height while AirBurst is selected.";

    public AirBurstController() {
        ElementalMagicApi.configManager().configure(this);
        ElementalMagicApi.configManager().configure(AirBurst.CONFIG);
    }

    @Activator
    private Collection<Ability> handleAction(AbilityUser user,
                                             ActionActivation activation) {
        if (!canActivateBy(activation.action())
                || !user.canUse(this, true, true)) {
            return List.of();
        }

        if (activation.action() != Action.SNEAK_DOWN) {
            ElementalMagicApi.abilityManager().getUserAbilities(user, AirBurst.class)
                .forEach(AirBurst::fire);
            return List.of();
        }

        return List.of(new AirBurst(user, this, false));
    }

    @Activator
    private Collection<Ability> handleFall(AbilityUser user,
                                           FallDamageActivation activation) {
        return user.canUse(this, true, true) ? 
            List.of(new AirBurst(user, this, true)) : List.of();
    }

	@Override
	public String name() {
        return "AirBurst";
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
