package me.kwilson272.elementalmagic.core.gameplay.fire.firewheel;

import java.util.Collection;
import java.util.List;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.Ability;
import me.kwilson272.elementalmagic.api.ability.Element;
import me.kwilson272.elementalmagic.api.ability.SequenceController;
import me.kwilson272.elementalmagic.api.activation.Action;
import me.kwilson272.elementalmagic.api.activation.ActionRecord;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.ability.CoreAbilityController;
import me.kwilson272.elementalmagic.core.ability.CoreElement;

public class FireWheelController extends CoreAbilityController implements SequenceController {

    protected static final String CONFIG_PATH = "Abilities.Fire.FireWheel.";

    @Configure(path = CONFIG_PATH + "Description", config = Config.LANGUAGE)
    private String description = "Create a rolling wheel of fire that barrels through enemies.";
    @Configure(path = CONFIG_PATH + "Instructions", config = Config.LANGUAGE)
    private String instructions = "FireShield (Hold-Sneak) > FireShield (Right-Click a Block Twice) > Blaze (Release-Sneak)";

    public FireWheelController() {
        ElementalMagicApi.configManager().configure(this);
        ElementalMagicApi.configManager().configure(FireWheel.CONFIG);
    }

	@Override
	public String name() {
        return "FireWheel";
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
        return false;
	}

	@Override
	public boolean isPassive() {
        return false;
	}

	@Override
	public boolean canActivateBy(Action action) {
        return false;
	}

	@Override
	public List<ActionRecord> steps() {
        return List.of(
            new ActionRecord("FireShield", Action.SNEAK_DOWN),
            new ActionRecord("FireShield", Action.RIGHT_CLICK_BLOCK),
            new ActionRecord("FireShield", Action.RIGHT_CLICK_BLOCK),
            new ActionRecord("Blaze", Action.SNEAK_UP)
        );
	}

	@Override
	public Collection<Ability> createAbilities(AbilityUser user) {
        if (user.canUse(this, false, true)) {
            return List.of(new FireWheel(user, this));
        }
        return List.of();
	}
}
