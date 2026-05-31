package me.kwilson272.elementalmagic.core.gameplay.water.waterblade;

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

public class WaterBladeController extends CoreAbilityController {

    protected static final String CONFIG_PATH = "Abilities.Water.WaterBlade.";

    @Configure(path = CONFIG_PATH + "Description", config = Config.LANGUAGE)
    private String description = "Use water, ice, or plants to slice through your enemies.";
    @Configure(path = CONFIG_PATH + "Instructions", config = Config.LANGUAGE)
    private String instructions = "Left-click a block to source from it, hold sneak and then release once you see swirling particles to fire";

    public WaterBladeController() {
        ElementalMagicApi.configManager().configure(this);
        ElementalMagicApi.configManager().configure(WaterBlade.CONFIG);
    }

    @Activator
    private Collection<Ability> handleAction(AbilityUser user,
                                             ActionActivation activation) {
        Action action = activation.action();
        if (!user.canUse(this, true, true)
                || !canActivateBy(action) || action == Action.SNEAK_DOWN) {
            return List.of();   
        }

        AbilityManager manager = ElementalMagicApi.abilityManager();
        boolean create = !manager.hasAbility(user, WaterBlade.class);
        create |= !manager.destroyIf(user, WaterBlade.class, WaterBlade::isSourced)
            .isEmpty();

        if (create) {
            return List.of(new WaterBlade(user, this));
        }
        return List.of();
    }

	@Override
	public String name() {
        return "WaterBlade";
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
