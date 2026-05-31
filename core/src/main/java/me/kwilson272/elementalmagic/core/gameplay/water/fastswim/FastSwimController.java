package me.kwilson272.elementalmagic.core.gameplay.water.fastswim;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.Ability;
import me.kwilson272.elementalmagic.api.ability.Element;
import me.kwilson272.elementalmagic.api.activation.Action;
import me.kwilson272.elementalmagic.api.activation.Activator;
import me.kwilson272.elementalmagic.api.activation.activations.ActionActivation;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.ability.CoreAbilityController;
import me.kwilson272.elementalmagic.core.ability.CoreElement;

import java.util.Collection;
import java.util.List;

public class FastSwimController extends CoreAbilityController {

    protected static final String CONFIG_PATH = "Abilities.Water.FastSwim.";

    @Configure(path = CONFIG_PATH + "Description", config = Config.LANGUAGE)
    private String description = "Water users are able to manipulate surrounding water to propel themselves while swimming.";
    @Configure(path = CONFIG_PATH + "Instructions", config = Config.LANGUAGE)
    private String instructions = "Hold-Sneak while in water to swim quickly through it.";

    public FastSwimController() {
        ElementalMagicApi.configManager().configure(FastSwim.CONFIG);
    }

    @Activator(requireSelected = false)
    public Collection<Ability> onSneak(AbilityUser user, 
                                       ActionActivation activation) {
        Action action = activation.action();
        if (canActivateBy(action) && user.canUse(this, false, true)) {
            return List.of(new FastSwim(user, this));
        }
        return List.of();
    }

    @Override
    public String name() {
        return "FastSwim";
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
        return true;
	}

    @Override
    public boolean canActivateBy(Action action) {
        return action == Action.SNEAK_DOWN;
    }
}

