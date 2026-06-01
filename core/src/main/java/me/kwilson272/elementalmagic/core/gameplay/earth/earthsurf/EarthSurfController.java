package me.kwilson272.elementalmagic.core.gameplay.earth.earthsurf;

import java.util.Collection;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.FallingBlock;

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
import me.kwilson272.elementalmagic.core.util.Blocks;

public class EarthSurfController extends CoreAbilityController {

    protected static final String CONFIG_PATH = "Abilities.Earth.EarthSurf.";

    @Configure(path = CONFIG_PATH + "Description", config = Config.LANGUAGE)
    private String description = "Create waves in the earth and ride them to your destination.";
    @Configure(path = CONFIG_PATH + "Instructions", config = Config.LANGUAGE)
    private String instructions = "(Dash) - Tap-Sneak \n" + 
                                  "(Surf) - Left-Click ";
   
    public EarthSurfController() {
        ElementalMagicApi.configManager().configure(this);
        ElementalMagicApi.configManager().configure(EarthSurf.CONFIG);
    }

    @Activator
    private Collection<Ability> handleAction(AbilityUser user,
                                             ActionActivation activation) {
        AbilityManager abilityManager = ElementalMagicApi.abilityManager();
        if (!canActivateBy(activation.action())
                || !user.canUse(this, true, true)) {
            return List.of();        
        }

        // Toggle
        if (activation.action() != Action.SNEAK_DOWN
                && !abilityManager.destroyAbilities(user, EarthSurf.class).isEmpty()) {
            return List.of();
        }

        if (abilityManager.hasAbility(user, EarthSurf.class)) {
            return List.of();
        }
        
        // Ensure user is jumping
        Location loc = user.player().getLocation().add(0, -0.3, 0);
        if (Blocks.isSolid(loc.getBlock())) {
            return List.of();
        }

        boolean isSneak = activation.action() == Action.SNEAK_DOWN;
        return List.of(new EarthSurf(user, this, isSneak));
    }

	@Override
	public String name() {
        return "EarthSurf";
	}

	@Override
	public Element element() {
        return CoreElement.EARTH;
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
