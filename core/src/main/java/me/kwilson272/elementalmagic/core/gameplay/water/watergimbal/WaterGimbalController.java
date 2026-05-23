package me.kwilson272.elementalmagic.core.gameplay.water.watergimbal;

import java.util.Collection;
import java.util.List;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.Ability;
import me.kwilson272.elementalmagic.api.ability.AbilityManager;
import me.kwilson272.elementalmagic.api.ability.Element;
import me.kwilson272.elementalmagic.api.ability.SequenceController;
import me.kwilson272.elementalmagic.api.activation.Action;
import me.kwilson272.elementalmagic.api.activation.ActionRecord;
import me.kwilson272.elementalmagic.api.activation.Activator;
import me.kwilson272.elementalmagic.api.activation.activations.ActionActivation;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.ability.CoreAbilityController;
import me.kwilson272.elementalmagic.core.ability.CoreElement;

public class WaterGimbalController extends CoreAbilityController implements SequenceController {
    
    protected static final String CONFIG_PATH = "Abilities.Water.WaterGimbal.";

    @Configure(path = CONFIG_PATH + "Description", config = Config.LANGUAGE)
    private String description = "Command two powerful streams of water to quickly plow through enemies.";
    @Configure(path = CONFIG_PATH + "Instructions", config = Config.LANGUAGE)
    private String instructions = "Torrent (Tap-Sneak) > Torrent (Tap-Sneak) > OctopusForm (Hold-Sneak) > OctopusForm (Left-Click)";

    public WaterGimbalController() {
        ElementalMagicApi.configManager().configure(this);
        ElementalMagicApi.configManager().configure(WaterGimbal.CONFIG);
    }

	@Override
	public String name() {
        return "WaterGimbal";
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
        return false;
	}

	@Override
	public boolean isPassive() {
        return false;
	}

	@Override
	public boolean canActivateBy(Action action) {
        return action == Action.LEFT_CLICK
            || action == Action.LEFT_CLICK_BLOCK
            || action == Action.HIT_ENTITY;
	}

	@Override
	public List<ActionRecord> steps() {
        return List.of(
            new ActionRecord("Torrent", Action.SNEAK_DOWN),
            new ActionRecord("Torrent", Action.SNEAK_UP),
            new ActionRecord("Torrent", Action.SNEAK_DOWN),
            new ActionRecord("Torrent", Action.SNEAK_UP),
            new ActionRecord("OctopusForm", Action.SNEAK_DOWN)
        );
	}

	@Override
	public Collection<Ability> createAbilities(AbilityUser user) {
        if (user.canUse(this, false, true)) {
            return List.of(new WaterGimbal(user, this));
        }
        return List.of();
	}
    
    @Activator(requireSelected = false)
    public Collection<Ability> handleAction(AbilityUser user,
                                            ActionActivation activation) {
        if (canActivateBy(activation.action()) 
                && user.getSelectedBindName().equals("OctopusForm")) {
            AbilityManager manager = ElementalMagicApi.abilityManager();
            manager.getUserAbilities(user, WaterGimbal.class)
                .forEach(WaterGimbal::fire);
        }

        return List.of();
    }
}
