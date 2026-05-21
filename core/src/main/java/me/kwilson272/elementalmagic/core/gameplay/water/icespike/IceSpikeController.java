package me.kwilson272.elementalmagic.core.gameplay.water.icespike;

import java.util.ArrayList;
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

public class IceSpikeController extends CoreAbilityController {

    protected static final String CONFIG_PATH = "Abilities.Water.IceSpike.";

    @Configure(path = CONFIG_PATH + "Description", config = Config.LANGUAGE)
    private String description = "Ice users can take spikes of ice and lob them at enemies, or pierce opponents from below with sharp pillars!";
    @Configure(path = CONFIG_PATH + "Instructions", config = Config.LANGUAGE)
    private String instructions = """
            (Blast) - Tap-Sneak while looking at a source block, Left-Click at targets to fire.
            (Pillar) - Left-Click while looking at an ice source block.
            (Field) - Tap-Sneak while not looking at a source block.
            """;    

    public IceSpikeController() {
        ElementalMagicApi.configManager().configure(this);
        ElementalMagicApi.configManager().configure(IceSpikeBlast.CONFIG);
        ElementalMagicApi.configManager().configure(IceSpikeField.CONFIG);
        ElementalMagicApi.configManager().configure(IceSpikePillar.CONFIG);
    }

    @Activator
    public Collection<Ability> handleAction(AbilityUser user, 
                                            ActionActivation activation) {
        boolean checkCooldowns = activation.action() == Action.SNEAK_DOWN;
        if (!canActivateBy(activation.action()) 
                || !user.canUse(this, true, checkCooldowns)) {
            return List.of();        
        }
        
        return activation.action() == Action.SNEAK_DOWN ? 
            handleSneak(user) : handleLeftClick(user);
    }

    private Collection<Ability> handleSneak(AbilityUser user) {
        AbilityManager manager = ElementalMagicApi.abilityManager();
        manager.destroyIf(user, IceSpikeBlast.class, IceSpikeBlast::isSourced);
        // We NEED to return the blast first so field can check if
        // any have been sourced before starting.
        List<Ability> created = new ArrayList<>();
        if (!user.isOnCooldown("IceSpikeBlast")) {
            created.add(new IceSpikeBlast(user, this));
        }   
        if (!user.isOnCooldown("IceSpikeField")) {
            created.add(new IceSpikeField(user, this));
        }
        return created;
    }

    private Collection<Ability> handleLeftClick(AbilityUser user) {
        AbilityManager manager = ElementalMagicApi.abilityManager();
        manager.getAllOf(IceSpikeBlast.class).forEach(ib -> ib.handleLeftClick(user));

        if (!user.isOnCooldown("IceSpikePillar")
                && !manager.hasAbility(user, IceSpikePillar.class)) {
            return List.of(new IceSpikePillar(user, this)); 
        }
        return List.of();
    }

	@Override
	public String name() {
        return "IceSpike";
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
