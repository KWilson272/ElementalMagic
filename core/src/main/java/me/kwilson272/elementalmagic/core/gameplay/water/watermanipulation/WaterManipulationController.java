package me.kwilson272.elementalmagic.core.gameplay.water.watermanipulation;

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

public class WaterManipulationController extends CoreAbilityController {

    protected static final String CONFIG_PATH = "Abilities.Water.WaterManipulation.";

    @Configure(path = CONFIG_PATH + "Description", config = Config.LANGUAGE)
    private String description = "The most fundamental ability in a water user's arsenal. Use WaterManipulation to send small streams of water into your enemies.";
    @Configure(path = CONFIG_PATH + "Instructions", config = Config.LANGUAGE)
    private String instructions = "Tap-Sneak while looking at a water source, Left-Click to shoot the stream towards your target.";

    public WaterManipulationController() {
        ElementalMagicApi.configManager().configure(this);
        ElementalMagicApi.configManager().configure(WaterManipulation.CONFIG);
    }

    @Activator
    public Collection<Ability> handleAction(AbilityUser user, 
                                            ActionActivation activation) {
        boolean checkCooldowns = activation.action() == Action.SNEAK_DOWN;
        if (!canActivateBy(activation.action()) ||
                !user.canUse(this, true, checkCooldowns)) {
            return List.of();
        }
        
        AbilityManager manager = ElementalMagicApi.abilityManager();
        if (activation.action() == Action.SNEAK_DOWN) {
            manager.destroyIf(user, WaterManipulation.class, WaterManipulation::isSourced);
            return List.of(new WaterManipulation(user, this));
        }

        manager.getAllOf(WaterManipulation.class)
            .forEach(wm -> wm.handleLeftClick(user));
        return List.of();
    }

	@Override
	public String name() {
        return "WaterManipulation";
	}

	@Override
	public Element element() {
        return CoreElement.WATER;
	}

	@Override
	public String description() {
        return description;
	}

	@Override
	public String instructions() {
        return instructions();
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
