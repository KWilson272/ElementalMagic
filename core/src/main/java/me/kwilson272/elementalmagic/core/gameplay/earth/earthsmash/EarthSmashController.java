package me.kwilson272.elementalmagic.core.gameplay.earth.earthsmash;

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

public class EarthSmashController extends CoreAbilityController {

    protected static final String CONFIG_PATH = "Abilities.Earth.EarthSmash.";

    @Configure(path = CONFIG_PATH + "Description", config = Config.LANGUAGE)
    private String description = "Create a giant boulder to ride on, swing around, and smash your enemies flat.";
    @Configure(path = CONFIG_PATH + "Instructions", config = Config.LANGUAGE)
    private String instructions = "Hold Sneak to charge, release while looking at the ground to create a smash. \n" + 
        "(Grab) - Hold Sneak while looking at a stationary or fired EarthSmash \n" + 
        "(Shoot) - While grabbed, left-click to fire an EarthSmash. \n" + 
        "(Flight) - Right-Click any of the EarthSmash blocks to begin riding.";

    public EarthSmashController() {
        ElementalMagicApi.configManager().configure(this);
        ElementalMagicApi.configManager().configure(EarthSmash.CONFIG);
    }

    @Activator
    private Collection<Ability> handleAction(AbilityUser user,
                                             ActionActivation activation) {
        if (!canActivateBy(activation.action())
                || !user.canUse(this, true, false)) {
            return List.of();        
        }

        AbilityManager manager = ElementalMagicApi.abilityManager();
        if (activation.action() == Action.RIGHT_CLICK_BLOCK) {
            manager.getAllOf(EarthSmash.class).forEach(es -> es.ride(user));
       
        } else if (activation.action() != Action.SNEAK_DOWN) {
            manager.getUserAbilities(user, EarthSmash.class)
                .forEach(EarthSmash::fire);
        
        } else if (!manager.getAllOf(EarthSmash.class).anyMatch(es -> es.grab(user))
                && !user.isOnCooldown("EarthSmash")) {
            return List.of(new EarthSmash(user, this));
        }

        return List.of();
    }

    @Override
	public String name() {
        return "EarthSmash";
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
        return action == Action.LEFT_CLICK
            || action == Action.LEFT_CLICK_BLOCK
            || action == Action.HIT_ENTITY
            || action == Action.SNEAK_DOWN
            || action == Action.RIGHT_CLICK_BLOCK;
	}
}
