package me.kwilson272.elementalmagic.core.gameplay.earth.shockwave;

import java.util.Collection;
import java.util.List;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.Ability;
import me.kwilson272.elementalmagic.api.ability.AbilityManager;
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

public class ShockwaveController extends CoreAbilityController {

    protected static final String CONFIG_PATH = "Abilities.Earth.Shockwave.";

    @Configure(path = CONFIG_PATH + "Description", config = Config.LANGUAGE)
    private String description = "Create expanding waves of earth to knock enemies back!";
    @Configure(path = CONFIG_PATH + "Instructions", config = Config.LANGUAGE)
    private String instructions = "Hold Sneak until you see particles, and release to create an expanding ring. Or left click to create a cone.";

    public ShockwaveController() {
        ElementalMagicApi.configManager().configure(this);
        ElementalMagicApi.configManager().configure(Shockwave.CONFIG);
    }

    @Activator
    private Collection<Ability> handleAction(AbilityUser user,  
                                             ActionActivation activation) {
        if (!canActivateBy(activation.action())
                || !user.canUse(this, true, true)) {
            return List.of();        
        }

        if (activation.action() == Action.SNEAK_DOWN) {
            return List.of(new Shockwave(user, this, false));
        }

        AbilityManager manager = ElementalMagicApi.abilityManager();
        manager.getUserAbilities(user, Shockwave.class).forEach(Shockwave::fire);
        return List.of();
    }

    @Activator
    private Collection<Ability> handleFall(AbilityUser user,
                                           FallDamageActivation activation) {
        AbilityManager am = ElementalMagicApi.abilityManager();
        if (!user.canUse(this, true, true) || 
                am.getUserAbilities(user, Shockwave.class).anyMatch(Shockwave::isCharging)) {
            return List.of();        
        }

        return List.of(new Shockwave(user, this, true));
    }
    
	@Override
	public String name() {
        return "Shockwave";
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
