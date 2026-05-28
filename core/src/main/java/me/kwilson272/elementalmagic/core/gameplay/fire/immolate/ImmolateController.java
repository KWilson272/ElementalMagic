package me.kwilson272.elementalmagic.core.gameplay.fire.immolate;

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

public class ImmolateController extends CoreAbilityController {

    protected static final String CONFIG_PATH = "Abilities.Fire.Immolate.";

    @Configure(path = CONFIG_PATH + "Description", config = Config.LANGUAGE)
    private String description = "Fire users can harness explosive energy to create deadly ranged blasts!";
    @Configure(path = CONFIG_PATH + "Instructions", config = Config.LANGUAGE)
    private String instructions = "Hold-Sneak until you see smoke in front of your eyes, then release. Left-click to detonate.";

    public ImmolateController() {
        ElementalMagicApi.configManager().configure(this);
        ElementalMagicApi.configManager().configure(Immolate.CONFIG);
    }

    @Activator
    private Collection<Ability> handleActions(AbilityUser user,
                                              ActionActivation activation) {
        if (!canActivateBy(activation.action())
                || !user.canUse(this, true, false)) {
            return List.of();        
        }

        AbilityManager manager = ElementalMagicApi.abilityManager();
        Immolate immolate = manager.getAbility(user, Immolate.class).orElse(null);
        if (activation.action() == Action.SNEAK_DOWN) {
            if (!user.isOnCooldown("Immolate") && immolate == null) {
                return List.of(new Immolate(user, this));
            }

        } else if (immolate != null && immolate.detonate()) {
            manager.removeAbility(immolate);
        }

        return List.of();
    }

	@Override
	public String name() {
        return "Immolate";
	}

	@Override
	public Element element() {
        return CoreElement.COMBUSTION;
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
