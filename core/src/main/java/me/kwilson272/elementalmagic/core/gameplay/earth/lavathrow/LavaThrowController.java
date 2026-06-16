package me.kwilson272.elementalmagic.core.gameplay.earth.lavathrow;

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

public class LavaThrowController extends CoreAbilityController {

    protected static final String CONFIG_PATH = "Abilities.Earth.LavaThrow.";

    @Configure(path = CONFIG_PATH + "Description", config = Config.LANGUAGE)
    private String description = "Throw streams of molten rock at your enemies.";
    @Configure(path = CONFIG_PATH + "Instructions", config = Config.LANGUAGE)
    private String instructions = "Tap-Sneak at a lava source, left-click to throw a stream towards a target.";

    public LavaThrowController() {
        ElementalMagicApi.configManager().configure(this);
        ElementalMagicApi.configManager().configure(LavaThrow.CONFIG);
    }

    @Activator
    private Collection<Ability> handleAction(AbilityUser user,
                                             ActionActivation activation) {
        if (!canActivateBy(activation.action())
            || !user.canUse(this, true, true)) {
            return List.of();
        }

        AbilityManager manager = ElementalMagicApi.abilityManager();
        if (activation.action() != Action.SNEAK_DOWN) {
            manager.getAbility(user, LavaThrow.class).ifPresent(LavaThrow::fire);
            return List.of();
        }

        return !manager.hasAbility(user, LavaThrow.class) ?
                List.of(new LavaThrow(user, this)) : List.of();
    }

    @Override
    public String name() {
        return "LavaThrow";
    }

    @Override
    public Element element() {
        return CoreElement.LAVA;
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
