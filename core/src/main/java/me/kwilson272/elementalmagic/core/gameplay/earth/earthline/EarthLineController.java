package me.kwilson272.elementalmagic.core.gameplay.earth.earthline;

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

public class EarthLineController extends CoreAbilityController {

    protected static final String CONFIG_PATH = "Abilities.Earth.EarthLine.";

    @Configure(path = CONFIG_PATH + "Description", config = Config.LANGUAGE)
    private String description = "Create ripples that race through the earth towards your targets.";
    @Configure(path = CONFIG_PATH + "Instructions", config = Config.LANGUAGE)
    private String instructions = "Tap-Sneak at an earth block to source, left-click to fire, and hold-sneak to aim";

    public EarthLineController() {
        ElementalMagicApi.configManager().configure(this);
        ElementalMagicApi.configManager().configure(EarthLine.CONFIG);
    }

    @Activator
    private Collection<Ability> handleAction(AbilityUser user,  
                                             ActionActivation activation) {
        if (!canActivateBy(activation.action()) 
                || !user.canUse(this, true, true)) {
            return List.of();
        }
    
        AbilityManager manager = ElementalMagicApi.abilityManager();
        if (activation.action() != Action.SNEAK_DOWN) {
            manager.getUserAbilities(user, EarthLine.class)
                .forEach(EarthLine::fire);
            return List.of();
        }
        
        manager.destroyIf(user, EarthLine.class, EarthLine::isSourced);
        return List.of(new EarthLine(user, this));
    }

	@Override
	public String name() {
        return "EarthLine";
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
