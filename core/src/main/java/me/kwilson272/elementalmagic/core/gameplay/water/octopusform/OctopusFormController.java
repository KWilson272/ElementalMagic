package me.kwilson272.elementalmagic.core.gameplay.water.octopusform;

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
import me.kwilson272.elementalmagic.core.gameplay.water.watergimbal.WaterGimbal;

public class OctopusFormController extends CoreAbilityController {

    protected static final String CONFIG_PATH = "Abilities.Water.OctopusForm.";

    @Configure(path = CONFIG_PATH + "Description", config = Config.LANGUAGE) 
    private String description = "A fundamental ability to the element of water; users can create highly defensive tentacles to deal damage and keep enemies at bay!";
    @Configure(path = CONFIG_PATH + "Instructions", config = Config.LANGUAGE)
    private String instructions = "Left-click at a water block to source, hold-sneak to activate, and left-click to deal damage.";

    public OctopusFormController() {
        ElementalMagicApi.configManager().configure(this);
        ElementalMagicApi.configManager().configure(OctopusForm.CONFIG);
    }

    @Activator
    private Collection<Ability> handleAction(AbilityUser user,
                                             ActionActivation activation) {
        if (!canActivateBy(activation.action()) 
                || activation.action() == Action.SNEAK_DOWN 
                || !user.canUse(this, true, true)) {
            return List.of();
        }
        
        AbilityManager manager = ElementalMagicApi.abilityManager();
        if (manager.hasAbility(user, WaterGimbal.class)) {
            return List.of();
        }

        boolean create = !manager.hasAbility(user, OctopusForm.class);
        create |= !manager.destroyIf(user, OctopusForm.class,
                OctopusForm::isSourced).isEmpty();
        if (create) {
            return List.of(new OctopusForm(user, this));
        }

        manager.getUserAbilities(user, OctopusForm.class)
            .forEach(OctopusForm::attack);
        return List.of();
    }

	@Override
	public String name() {
        return "OctopusForm";
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
