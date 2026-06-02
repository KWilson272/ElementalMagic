package me.kwilson272.elementalmagic.core.gameplay.earth.lavadisc;

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

public class LavaDiscController extends CoreAbilityController {

    protected static final String CONFIG_PATH = "Abilities.Earth.LavaDisc.";

    @Configure(path = CONFIG_PATH + "Description", config = Config.LANGUAGE)
    private String description = "Create spinning discs of molten earth to lob at enemies.";
    @Configure(path = CONFIG_PATH + "Instructions", config = Config.LANGUAGE)
    private String instructions = "Hold-Sneak while looking at a earth to source, release-sneak to fire.";

    public LavaDiscController() {
        ElementalMagicApi.configManager().configure(this);
        ElementalMagicApi.configManager().configure(LavaDisc.CONFIG);
    }

    @Activator
    private Collection<Ability> handleAction(AbilityUser user,
                                             ActionActivation activation) {
        AbilityManager abilityManager = ElementalMagicApi.abilityManager();
        if (!canActivateBy(activation.action())
                || !user.canUse(this, true, true)
                || abilityManager.hasAbility(user, LavaDisc.class)) {
            return List.of();
        }

        return List.of(new LavaDisc(user, this));
    }

	@Override
	public String name() {
        return "LavaDisc";
	}

	@Override
	public Element element() {
        return CoreElement.LAVA;
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
