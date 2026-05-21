package me.kwilson272.elementalmagic.core.gameplay.water.plantwhip;

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

public class PlantWhipController extends CoreAbilityController {

    protected static final String CONFIG_PATH = "Abilities.Water.PlantWhip.";
    
    @Configure(path = CONFIG_PATH + "Description", config = Config.LANGUAGE)
    private String description = "Manipulate the water in plants and send the surrounding flora towards your enemies!";
    @Configure(path = CONFIG_PATH + "Instructions", config = Config.LANGUAGE)
    private String instructions = "Tap-Sneak while looking at a plant, left-click to shoot at a target.";

    public PlantWhipController() {
        ElementalMagicApi.configManager().configure(this);
        ElementalMagicApi.configManager().configure(PlantWhip.CONFIG);
    }

    @Activator
    private Collection<Ability> handleAction(AbilityUser user,
                                             ActionActivation activation) {
        if (!canActivateBy(activation.action()) 
                || !user.canUse(this, true, true)) {
            return List.of();
        }

        if (activation.action() == Action.SNEAK_DOWN) {
            return handleSneak(user);
        } 

        AbilityManager manager = ElementalMagicApi.abilityManager();
        manager.getUserAbilities(user, PlantWhip.class).forEach(PlantWhip::fire);
        return List.of();
    }

    private Collection<Ability> handleSneak(AbilityUser user) {
        AbilityManager manager = ElementalMagicApi.abilityManager();

        boolean create = !manager.hasAbility(user, PlantWhip.class);
        create |= !manager.destroyIf(user, PlantWhip.class, 
                plantWhip -> !plantWhip.isFired()).isEmpty();

        return create ? List.of(new PlantWhip(user, this)) : List.of();
    }

	@Override
	public String name() {
        return "PlantWhip";
	}

	@Override
	public Element element() {
        return CoreElement.PLANT;
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
            || action == Action.LEFT_CLICK
            || action == Action.SNEAK_DOWN;
	}
}
