package me.kwilson272.elementalmagic.core.gameplay.earth.earthblast;

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

public class EarthBlastController extends CoreAbilityController {

    protected static final String CONFIG_PATH = "Abilities.Earth.EarthBlast.";

    @Configure(path = CONFIG_PATH + "Description", config = Config.ABILITIES)
    private String description = "A basic ability that allows the user to toss earth blocks around.";
    @Configure(path = CONFIG_PATH + "Instructions", config = Config.ABILITIES)
    private String instructions = "Tap-Sneak while looking at an earth block, left-click to fire.";

    public EarthBlastController() {
        ElementalMagicApi.configManager().configure(this);
        ElementalMagicApi.configManager().configure(EarthBlast.CONFIG);
    }

    @Activator
    private Collection<Ability> handleAction(AbilityUser user,
                                             ActionActivation activation) {
        boolean checkCooldowns = activation.action() == Action.SNEAK_DOWN;
        if (!canActivateBy(activation.action())
                || !user.canUse(this, true, checkCooldowns)) {
            return List.of(); 
        }

        AbilityManager manager = ElementalMagicApi.abilityManager();
        if (activation.action() == Action.SNEAK_DOWN) {
            manager.destroyIf(user, EarthBlast.class, EarthBlast::isSourced);
            return List.of(new EarthBlast(user, this));
        }

        manager.getAllOf(EarthBlast.class)
            .forEach(eb -> eb.handleLeftClick(user));
        return List.of();
    }

	@Override
	public String name() {
        return "EarthBlast";
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
            || action == Action.SNEAK_DOWN;
	}
}
