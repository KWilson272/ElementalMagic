package me.kwilson272.elementalmagic.core.gameplay.fire.fireburst;

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

public class FireBurstController extends CoreAbilityController {

    protected static final String CONFIG_PATH = "Abilities.Fire.FireBurst.";

    @Configure(path = CONFIG_PATH + "Description", config = Config.LANGUAGE)
    private String description = "Destroy everything around you with a massive burst of flames!";
    @Configure(path = CONFIG_PATH + "Instructions", config = Config.LANGUAGE)
    private String instructions = "Hold-Sneak until you see a ring of fire, then left-click for a cone, or right-click for a burst.";

    public FireBurstController() {
        ElementalMagicApi.configManager().configure(this);
        ElementalMagicApi.configManager().configure(FireBurst.CONFIG);
    }

    @Activator
    private Collection<Ability> handleActions(AbilityUser user,
                                              ActionActivation activation) {
        if (!canActivateBy(activation.action())
                || !user.canUse(this, true, true)) {
            return List.of();        
        }

        if (activation.action() == Action.SNEAK_DOWN) {
            return List.of(new FireBurst(user, this));
        }

        AbilityManager manager = ElementalMagicApi.abilityManager();
        manager.getUserAbilities(user, FireBurst.class)
            .forEach(FireBurst::initConeBurst);
        return List.of();
    }



	@Override
	public String name() {
        return "FireBurst";
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
            || action == Action.HIT_ENTITY
            || action == Action.SNEAK_DOWN;
	}
}
