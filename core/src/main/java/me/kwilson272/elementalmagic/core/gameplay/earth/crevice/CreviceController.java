package me.kwilson272.elementalmagic.core.gameplay.earth.crevice;

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

public class CreviceController extends CoreAbilityController implements SequenceController {

    protected static final String CONFIG_PATH = "Abilities.Earth.Crevice.";

    @Configure(path = CONFIG_PATH + "Description", config = Config.LANGUAGE)
    private String description = "Create monstrous cracks in the earth and swallow enemies whole.";
    @Configure(path = CONFIG_PATH + "Instructions", config = Config.LANGUAGE)
    private String instructions = "Collapse (Right-Click a Block) > Shockwave (Tap-Sneak Twice)";

    public CreviceController() {
        ElementalMagicApi.configManager().configure(this);
        ElementalMagicApi.configManager().configure(Crevice.CONFIG);
    }

    @Override
    public String name() {
        return "Crevice";
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
        return false;
    }

    @Override
    public boolean isPassive() {
        return false;
    }

    @Override
    public boolean canActivateBy(Action action) {
        return action == Action.SNEAK_DOWN;
    }

    @Override
    public List<ActionRecord> steps() {
        return List.of(
            new ActionRecord("Collapse", Action.RIGHT_CLICK_BLOCK),
            new ActionRecord("Shockwave", Action.SNEAK_DOWN),
            new ActionRecord("Shockwave", Action.SNEAK_UP),
            new ActionRecord("Shockwave", Action.SNEAK_DOWN)
        );
    }

    @Override
    public Collection<Ability> createAbilities(AbilityUser user) {
        if (!user.canUse(this, false, true)) {
            return List.of();
        }

        AbilityManager manager = ElementalMagicApi.abilityManager();
        if (manager.getUserAbilities(user, Crevice.class)
                .noneMatch(Crevice::isOpening)) {
            return List.of(new Crevice(user, this));
        }

        return List.of();
    }

    @Activator(requireSelected = false)
    private Collection<Ability> handleAction(AbilityUser user,
                                             ActionActivation activation) {
        if (canActivateBy(activation.action())
                && user.canUse(this, false, false)
                && user.getSelectedBindName().equals("Shockwave")) {
            AbilityManager manager = ElementalMagicApi.abilityManager();
            manager.getAllOf(Crevice.class).forEach(c -> c.close(user));
        }

        return List.of();
    }
}
