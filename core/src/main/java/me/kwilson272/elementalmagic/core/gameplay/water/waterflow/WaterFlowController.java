package me.kwilson272.elementalmagic.core.gameplay.water.waterflow;

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

public class WaterFlowController extends CoreAbilityController implements SequenceController {
    
    protected static final String CONFIG_PATH = "Abilities.Water.WaterFlow.";
    
    @Configure(path = CONFIG_PATH + "Description", config = Config.LANGUAGE)
    private String description = "Water users can create rushing streams of water to carry themselves or enemies far away.";
    @Configure(path = CONFIG_PATH + "Instructions", config = Config.LANGUAGE)
    private String instructions = "WaterManipulation (Tap-Sneak) > Torrent (Tap-Sneak) > Torrent (Hold-Sneak) > WaterManipulation (Release-Sneak)";
    
    public WaterFlowController() {
        ElementalMagicApi.configManager().configure(this);
        ElementalMagicApi.configManager().configure(WaterFlow.CONFIG);
    }

	@Override
	public String name() {
        return "WaterFlow";
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
            new ActionRecord("WaterManipulation", Action.SNEAK_DOWN),
            new ActionRecord("WaterManipulation", Action.SNEAK_UP),
            new ActionRecord("Torrent", Action.SNEAK_DOWN),
            new ActionRecord("Torrent", Action.SNEAK_UP),
            new ActionRecord("Torrent", Action.SNEAK_DOWN),
            new ActionRecord("WaterManipulation", Action.SNEAK_UP)
        );
	}

	@Override
	public Collection<Ability> createAbilities(AbilityUser user) {
        if (!user.canUse(this, false, true)) {
            return List.of();
        }

        return List.of(new WaterFlow(user, this));
	}

    @Activator(requireSelected = false)
    private Collection<Ability> handleClick(AbilityUser user, 
                                            ActionActivation activation) {
        if (canActivateBy(activation.action())
                && user.getSelectedBindName().equals("WaterManipulation")) {
            AbilityManager manager = ElementalMagicApi.abilityManager();
            manager.getUserAbilities(user, WaterFlow.class)
                .forEach(WaterFlow::freeze);
            manager.destroyAbilities(user, WaterFlow.class);
        }
        return List.of();
    }
}
