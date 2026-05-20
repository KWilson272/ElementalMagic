package me.kwilson272.elementalmagic.core.gameplay.water.surge;

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

public class SurgeController extends CoreAbilityController {

    protected static final String CONFIG_PATH = "Abilities.Water.Surge.";

    @Configure(path = CONFIG_PATH + "Description", config = Config.LANGUAGE)
    private String description = "Create discs of water to propel entities through the air, or freeze and use them as shields to block attacks!";
    @Configure(path = CONFIG_PATH + "Instructions", config = Config.LANGUAGE)
    private String instructions = """
            (Shield) - Left-Click to source, Sneak-Down while looking in the air to create a shield.
            (Wave) - Tap-Sneak to source, left click to fire.
            """;

    public SurgeController() {
        ElementalMagicApi.configManager().configure(SurgeWave.CONFIG);
        ElementalMagicApi.configManager().configure(SurgeWall.CONFIG);
    }

    @Activator
    public Collection<Ability> onAction(AbilityUser user, ActionActivation activation) {
        if (!canActivateBy(activation.action())
                || !user.canUse(this, true, false)) {
            return List.of();
        }
    
        return activation.action() == Action.SNEAK_DOWN ?
            handleSneak(user) : handleLeftClick(user);
    }

    private Collection<Ability> handleSneak(AbilityUser user) {
        AbilityManager manager = ElementalMagicApi.abilityManager();
        SurgeWave wave = manager.getAbility(user, SurgeWave.class).orElse(null);
        boolean create = true;
        
        if (wave != null) {
            if (wave.destroyOnSneak()) {
                manager.destroyAbility(wave);
            } else {
                create = false;
            }
        }

        if (create && !user.isOnCooldown("SurgeWave")) {
            return List.of(new SurgeWave(user, this));
        }
        return List.of();
    }

    private Collection<Ability> handleLeftClick(AbilityUser user) {
        AbilityManager manager = ElementalMagicApi.abilityManager();
        // Prioritize wave over shield as it is used more often.
        boolean fired = manager.getUserAbilities(user, SurgeWave.class)
            .anyMatch(SurgeWave::handleLeftClick);
        
        if (fired || user.isOnCooldown("SurgeWall")) {
            return List.of();
        }
        
        SurgeWall wall = manager.getAbility(user, SurgeWall.class).orElse(null);
        boolean create = true;
        
        if (wall != null) {
            if (wall.isSourced()) {
                manager.destroyAbility(wall);
            } else {
                create = false;
            }
        }
    
        return create ? List.of(new SurgeWall(user, this)) : List.of();
    }

    @Override
    public String name() {
        return "Surge";
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
    public boolean canActivateBy(Action action) {
        return action == Action.LEFT_CLICK 
            || action == Action.LEFT_CLICK_BLOCK 
            || action == Action.HIT_ENTITY 
            || action == Action.SNEAK_DOWN;
    }

	@Override
	public boolean isPassive() {
        return false;
	}
}
