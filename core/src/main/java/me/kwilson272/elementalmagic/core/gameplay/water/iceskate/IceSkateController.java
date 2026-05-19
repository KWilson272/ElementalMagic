package me.kwilson272.elementalmagic.core.gameplay.water.iceskate;

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

public class IceSkateController extends CoreAbilityController {

    protected static final String CONFIG_PATH = "Abilities.Water.IceSkate.";

    @Configure(path = CONFIG_PATH + "Description", config = Config.LANGUAGE)
    private String description = "Ice users are able to negate their fall on ice or snow!";
    @Configure(path = CONFIG_PATH + "Instructions", config = Config.LANGUAGE)
    private String instructions = "Activates automatically on fall damage.";

    public IceSkateController() {
        ElementalMagicApi.configManager().configure(IceSkate.CONFIG);
    }

    @Activator(requireSelected = false, requireElement = true)
    public Collection<Ability> onSprint(AbilityUser user, ActionActivation activation) {
        AbilityManager manager = ElementalMagicApi.abilityManager();
        if (manager.hasAbility(user, IceSkate.class)) {
            return List.of();
        }

        if (activation.action() == Action.TOGGLE_SPRINT_ON
                && user.canUse(this, false, false)) {
            return List.of(new IceSkate(user, this));
        }
        return List.of();
    }

    @Override
    public String name() {
        return "IceSkate";
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
        return false;
    }

    @Override
    public boolean isPassive() {
        return true;
    }

	@Override
	public boolean canActivateBy(Action action) {
        return action == Action.TOGGLE_SPRINT_ON;
	}
}

