package me.kwilson272.elementalmagic.core.gameplay.fire.fireshots;

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

public class FireShotsController extends CoreAbilityController {

    protected static final String CONFIG_PATH = "Abilities.Fire.FireShots.";

    @Configure(path = CONFIG_PATH + "Description", config = Config.LANGUAGE)
    private String description = "To help suplement the lack of projectiles, fire users split large blasts into small ones for rapid damage.";
    @Configure(path = CONFIG_PATH + "Instructions", config = Config.LANGUAGE)
    private String instructions = "Tap-sneak to create fireballs, left-click to fire them.";

    public FireShotsController() {
        ElementalMagicApi.configManager().configure(this);
        ElementalMagicApi.configManager().configure(FireShots.CONFIG);
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
            manager.getUserAbilities(user, FireShots.class)
                .forEach(FireShots::fire);
            return List.of();
        }

        boolean canFire = !manager.getUserAbilities(user, FireShots.class)
            .anyMatch(FireShots::isHolding);
        return canFire ? List.of(new FireShots(user, this)) : List.of();
    }

	@Override
	public String name() {
        return "FireShots";
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
