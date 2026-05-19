package me.kwilson272.elementalmagic.core.gameplay.water.waterspout;

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

public class WaterSpoutController extends CoreAbilityController {

    protected static final String CONFIG_PATH = "Abilities.Water.WaterSpout.";

    @Configure(path = CONFIG_PATH + "Description", config = Config.ABILITIES)
    private String description = "Lift yourself high above your enemies on towers of water, or soar over terrain with the ocean itself beneath your feet!";
    @Configure(path = CONFIG_PATH + "Instructions", config = Config.ABILITIES)
    private String instructions = """
        (Spout) - Left-Click while not looking at a source to toggle on/off.
        (Wave) - Left-Click while looking at a source, then hold sneak. Release sneak after charging to begin flying.
        """;

    public WaterSpoutController() {
        ElementalMagicApi.configManager().configure(WaterSpout.CONFIG);
    }

    @Activator
    public Collection<Ability> onAction(AbilityUser user, ActionActivation activation) {
        if (!canActivateBy(activation)) {
            return List.of();
        }

        AbilityManager manager = ElementalMagicApi.abilityManager();
        WaterSpout spout = manager.getAbility(user, WaterSpout.class).orElse(null);
        if (spout != null) {
            // Destroy the ability first or spout listener will dampen the hop
            manager.destroyAbility(spout);
            spout.hop();
            return List.of();
        } else if (user.canUse(this, true, true)) {
            return List.of(new WaterSpout(user, this));
        }

        return List.of();
    }

    private boolean canActivateBy(ActionActivation activation) {
        Action action = activation.action();
        return action == Action.LEFT_CLICK 
            || action == Action.LEFT_CLICK_BLOCK
            || action == Action.HIT_ENTITY;
    }

    @Override
    public String name() {
        return "WaterSpout";
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
}
