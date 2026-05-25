package me.kwilson272.elementalmagic.core.gameplay.fire.heatcontrol;

import java.util.ArrayList;
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

public class HeatControlController extends CoreAbilityController {

    protected static final String CONFIG_PATH = "Abilities.Fire.HeatControl.";

    @Configure(path = CONFIG_PATH + "Description", config = Config.LANGUAGE)
    private String description = "Fire users can manipulate the heat in the air to extinguish fire or melt ice.";
    @Configure(path = CONFIG_PATH + "Instructions", config = Config.LANGUAGE)
    private String instructions = "Left-Click to extinguish/melt around the target, tap-sneak to extinguish/melt around yourself.";

    public HeatControlController() {
        ElementalMagicApi.configManager().configure(this);
        ElementalMagicApi.configManager().configure(HeatControlMelt.CONFIG);
        ElementalMagicApi.configManager().configure(HeatControlExtinguish.CONFIG);
    }

    @Activator
    private Collection<Ability> handleActions(AbilityUser user,
                                              ActionActivation activation) {
        if (!canActivateBy(activation.action()) 
                || !user.canUse(this, true, false)) {
            return List.of();
        }
        
        boolean isSneak = activation.action() == Action.SNEAK_DOWN;
        List<Ability> abilities = new ArrayList<>();
        if (!user.isOnCooldown("HeatControlExtinguish")) {
            abilities.add(new HeatControlExtinguish(user, this, isSneak));
        }
        if (!user.isOnCooldown("HeatControlMelt")) {
            abilities.add(new HeatControlMelt(user, this, isSneak));
        }
        return abilities;
    }

	@Override
	public String name() {
        return "HeatControl";
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
        return true;
	}

	@Override
	public boolean canActivateBy(Action action) {
        return action == Action.LEFT_CLICK
            || action == Action.LEFT_CLICK_BLOCK
            || action == Action.HIT_ENTITY
            || action == Action.SNEAK_DOWN;
	}
}
