package me.kwilson272.elementalmagic.core.gameplay.water.phasechange;

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

public class PhaseChangeController extends CoreAbilityController {

    protected static final String CONFIG_PATH = "Abilities.Water.PhaseChange.";

    @Configure(path = CONFIG_PATH + "Description", config = Config.LANGUAGE)
    private String description = "PhaseChange users are able to command water " +
            "in any of its forms, changing state from solid to liquid in an instant!";
    @Configure(path = CONFIG_PATH + "Instructions", config = Config.LANGUAGE)
    private String instructions = """ 
            (Freeze) - Left-Click while looking at water blocks.
            (Melt) - Hold-Sneak while looking at ice or snow.
            """;

    public PhaseChangeController() {
        ElementalMagicApi.configManager().configure(PhaseChangeFreeze.CONFIG);
        ElementalMagicApi.configManager().configure(PhaseChangeMelt.CONFIG);
    }

    @Activator
    public Collection<Ability> onAction(AbilityUser user, ActionActivation activation) {
        if (!canActivateBy(activation.action()) || !user.canUse(this, true, false)) {
            return List.of();
        }

        if (!user.isOnCooldown("PhaseChangeMelt")
            && activation.action() == Action.SNEAK_DOWN) {
            return List.of(new PhaseChangeMelt(user, this));

        } else if (!user.isOnCooldown("PhaseChangeFreeze")
                && (activation.action() == Action.LEFT_CLICK
                || activation.action() == Action.LEFT_CLICK_BLOCK
                || activation.action() == Action.HIT_ENTITY)) {
            return List.of(new PhaseChangeFreeze(user, this));
        }
        return List.of();
    }

    @Override
    public String name() {
        return "PhaseChange";
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
        return action == Action.LEFT_CLICK 
            || action == Action.LEFT_CLICK_BLOCK
            || action == Action.HIT_ENTITY 
            || action == Action.SNEAK_DOWN;
    }
}
