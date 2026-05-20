package me.kwilson272.elementalmagic.core.gameplay.water.icicle;

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

public class IcicleController extends CoreAbilityController {
    
    protected static final String CONFIG_PATH = "Abilities.Water.Icicle.";

    @Configure(path = CONFIG_PATH + "Description", config = Config.LANGUAGE)
    private String description = "Create giant spikes of ice to impale opponents and shatter ice.";
    @Configure(path = CONFIG_PATH + "instructions", config = Config.LANGUAGE)
    private String instructions = "Left-Click while looking at ice to source, tap-sneak multiple times to fire.";

    public IcicleController() {
        ElementalMagicApi.configManager().configure(this);
        ElementalMagicApi.configManager().configure(Icicle.CONFIG);
    }

    @Activator
    private Collection<Ability> handleAction(AbilityUser user,
                                             ActionActivation activation) {
        if (!canActivateBy(activation.action()) 
                || !user.canUse(this, true, true)) {
            return List.of();
        }
        
        if (activation.action() == Action.SNEAK_DOWN) {
            handleSneak(user);
            return List.of();
        }

        return handleLeftClick(user);
    }

    private void handleSneak(AbilityUser user) {
        AbilityManager manager = ElementalMagicApi.abilityManager();
        manager.getUserAbilities(user, Icicle.class)
            .forEach(Icicle::fire);
    }

    private Collection<Ability> handleLeftClick(AbilityUser user) {
        AbilityManager manager = ElementalMagicApi.abilityManager();

        // Create only if we don't have an icicle or we need to re source
        boolean create = !manager.hasAbility(user, Icicle.class);
        create |= !manager.destroyIf(user, Icicle.class, 
                icicle -> !icicle.hasFired()).isEmpty();

        return create ? List.of(new Icicle(user, this)) : List.of();
    }

	@Override
	public String name() {
        return "Icicle";
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
        return action == Action.LEFT_CLICK_BLOCK
            || action == Action.LEFT_CLICK
            || action == Action.HIT_ENTITY 
            || action == Action.SNEAK_DOWN;
	}
}
