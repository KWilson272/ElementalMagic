package me.kwilson272.elementalmagic.core.gameplay.fire.fireblast;

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

public class FireBlastController extends CoreAbilityController {

    protected static final String CONFIG_PATH = "Abilities.Fire.FireBlast.";

    @Configure(path = CONFIG_PATH + "Description", config = Config.LANGUAGE)
    private String description = "The most fundamental ability in a fire user's toolkit, fireblast allows players to shoot balls of fire at their enemies.";
    @Configure(path = CONFIG_PATH + "Instructions", config = Config.LANGUAGE)
    private String instructions = "Left-Click to fire a small blast, Hold-Sneak and release once you see a ring of fire to unleash a large blast.";
    
    public FireBlastController() {
        ElementalMagicApi.configManager().configure(this);
        ElementalMagicApi.configManager().configure(FireBlast.CONFIG);
        ElementalMagicApi.configManager().configure(ChargedFireBlast.CONFIG);
    }

    @Activator
    private Collection<Ability> handleAction(AbilityUser user,
                                             ActionActivation activation) {
        if (!canActivateBy(activation.action()) 
                || !user.canUse(this, true, false)) {
            return List.of();
        }
    
        if (activation.action() == Action.SNEAK_DOWN
                && !user.isOnCooldown("ChargedFireBlast")) {
            return List.of(new ChargedFireBlast(user, this));
        }
    
        AbilityManager manager = ElementalMagicApi.abilityManager();
        boolean isCharging = manager.getUserAbilities(user, ChargedFireBlast.class)
            .anyMatch(ChargedFireBlast::isCharging);
        if (!user.isOnCooldown("FireBlast") && !isCharging) {
            return List.of(new FireBlast(user, this));
        }
        
        return List.of();
    }

	@Override
	public String name() {
        return "FireBlast";
	}

	@Override
	public Element element() {
        return CoreElement.FIRE;
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
