package me.kwilson272.elementalmagic.core.gameplay.fire.walloffire;

import java.time.chrono.HijrahEra;
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

public class WallOfFireController extends CoreAbilityController {

    protected static final String CONFIG_PATH = "Abilities.Fire.WallOfFire.";

    @Configure(path = CONFIG_PATH + "Description", config = Config.LANGUAGE)
    private String description = "Create a wall of flames to keep your enemies at bay.";
    @Configure(path = CONFIG_PATH + "Instructions", config = Config.LANGUAGE)
    private String instructions = "Left-click to create the wall.";

    public WallOfFireController() {
        ElementalMagicApi.configManager().configure(this);
        ElementalMagicApi.configManager().configure(WallOfFire.CONFIG);
    }

    @Activator
    private Collection<Ability> handleAction(AbilityUser user,
                                             ActionActivation activation) {
        if (!canActivateBy(activation.action())
                || !user.canUse(this, true, true)) {
            return List.of();        
        }

        AbilityManager manager = ElementalMagicApi.abilityManager();
        if (!manager.hasAbility(user, WallOfFire.class)) {
            return List.of(new WallOfFire(user, this));
        }

        return List.of();
    }

	@Override
	public String name() {
        return "WallOfFire";
	}

	@Override
	public Element element() {
        return CoreElement.FIRE;
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
            || action == Action.HIT_ENTITY; 
    }
}
