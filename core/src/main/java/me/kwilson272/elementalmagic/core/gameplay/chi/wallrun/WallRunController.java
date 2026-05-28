package me.kwilson272.elementalmagic.core.gameplay.chi.wallrun;

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

public class WallRunController extends CoreAbilityController {

    protected static final String CONFIG_PATH = "Abilities.Chi.WallRun.";

    @Configure(path = CONFIG_PATH + "Description", config = Config.LANGUAGE)
    private String description = "The most agile players are able to run on walls for extra mobility.";
    @Configure(path = CONFIG_PATH + "Instructions", config = Config.LANGUAGE)
    private String instructions = "Left-Click while sprinting, not on the ground, and directly next to a wall.";

    public WallRunController() {
        ElementalMagicApi.configManager().configure(this);
        ElementalMagicApi.configManager().configure(WallRun.CONFIG);
    }

    @Activator(requireSelected = false, requireElement = false) 
    private Collection<Ability> handleAction(AbilityUser user,
                                             ActionActivation activation) {
        if (!canActivateBy(activation.action())
                || user.isOnCooldown("WallRun")) {
            return List.of();        
        }

        AbilityManager manager = ElementalMagicApi.abilityManager();
        if (manager.hasAbility(user, WallRun.class)) {
            return List.of();
        } 

        return List.of(new WallRun(user, this));
    }

	@Override
	public String name() {
        return "WallRun";
	}

	@Override
	public Element element() {
        return CoreElement.CHI;
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
        return action == Action.LEFT_CLICK
            || action == Action.LEFT_CLICK_BLOCK
            || action == Action.HIT_ENTITY;
	}
}
