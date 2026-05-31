package me.kwilson272.elementalmagic.core.gameplay.air.airsuction;

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
import me.kwilson272.elementalmagic.core.gameplay.air.airspout.AirSpout;

public class AirSuctionController extends CoreAbilityController {

    protected static final String CONFIG_PATH = "Abilities.Air.AirSuction.";

    @Configure(path = CONFIG_PATH + "Description", config = Config.LANGUAGE)
    private String description = "Air users can utilize wind to draw items or enemies towards them.";
    @Configure(path = CONFIG_PATH + "Instructions", config = Config.LANGUAGE)
    private String instructions = "Left-Click to fire. Additionally, tap-sneak to change where the suction originates from.";

    public AirSuctionController() {
        ElementalMagicApi.configManager().configure(this);
        ElementalMagicApi.configManager().configure(AirSuction.CONFIG);
    }
   
    @Activator
    private Collection<Ability> handleActions(AbilityUser user,
                                              ActionActivation activation) {
        if (!canActivateBy(activation.action())
                || !user.canUse(this, true, false)) {
            return List.of();
        }

        if (ElementalMagicApi.abilityManager().hasAbility(user, AirSpout.class)) {
            return List.of();
        }

        return activation.action() == Action.SNEAK_DOWN ? 
            handleSneak(user) : handleLeftClick(user);
    }

    private Collection<Ability> handleSneak(AbilityUser user) {
        AbilityManager manager = ElementalMagicApi.abilityManager();
        manager.destroyIf(user, AirSuction.class, AirSuction::isSourced);
        return List.of(new AirSuction(user, this, true));
    }

    private Collection<Ability> handleLeftClick(AbilityUser user) {
        if (user.isOnCooldown("AirSuction")) {
            return List.of();
        }

        AbilityManager manager = ElementalMagicApi.abilityManager();
        AirSuction sourced = manager.getUserAbilities(user, AirSuction.class)
            .filter(AirSuction::isSourced).findFirst().orElse(null);
        if (sourced != null) {
            sourced.fire();
            return List.of();
        } 

        return List.of(new AirSuction(user, this, false));
    }

	@Override
	public String name() {
        return "AirSuction";
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
