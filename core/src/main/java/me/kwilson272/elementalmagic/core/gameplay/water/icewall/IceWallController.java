package me.kwilson272.elementalmagic.core.gameplay.water.icewall;

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

public class IceWallController extends CoreAbilityController {

    protected static final String CONFIG_PATH = "Abilities.Water.IceWall.";

    @Configure(path = CONFIG_PATH + "Description", config = Config.LANGUAGE)
    private String description = "Water users can freeze solid walls of ice in front of them for quick defense!";
    @Configure(path = CONFIG_PATH + "Instructions", config = Config.LANGUAGE)
    private String instructions = "Tap-Sneak at a water source to create a wall. Tap-Sneak at an IceWall to shatter it.";

    public IceWallController() {
        ElementalMagicApi.configManager().configure(this);
        ElementalMagicApi.configManager().configure(IceWall.CONFIG);
    }

    @Activator
    private Collection<Ability> handleSneak(AbilityUser user, 
                                            ActionActivation activation) {
        if (!canActivateBy(activation.action())) {
            return List.of();
        }
        // Just instantiate a new icewall because creation is so complex
        return List.of(new IceWall(user, this));
    }

	@Override
	public String name() {
        return "IceWall";
	}

	@Override
	public Element element() {
        return CoreElement.ICE;
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
        return action == Action.SNEAK_DOWN;
	}
}

