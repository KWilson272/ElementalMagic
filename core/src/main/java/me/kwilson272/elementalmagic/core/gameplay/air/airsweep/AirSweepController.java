package me.kwilson272.elementalmagic.core.gameplay.air.airsweep;

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

public class AirSweepController extends CoreAbilityController implements SequenceController {

    protected static final String CONFIG_PATH = "Abilities.Air.AirSweep.";

    @Configure(path = CONFIG_PATH + "Description", config = Config.LANGUAGE)
    private String description = "Create rapidly expanding currents of air to slice through anything in its path.";
    @Configure(path = CONFIG_PATH + "Instructions", config = Config.LANGUAGE)
    private String instructions = "AirSwipe (Left-Click Twice) > AirBurst (Hold-Sneak) > AirBurst (Left-Click and drag your mouse).";

    public AirSweepController() {
        ElementalMagicApi.configManager().configure(this);
        ElementalMagicApi.configManager().configure(AirSweep.CONFIG);
    }

	@Override
	public String name() {
        return "AirSweep";
	}

	@Override
	public Element element() {
        return CoreElement.AIR;
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
            new ActionRecord("AirSwipe", Action.LEFT_CLICK),
            new ActionRecord("AirSwipe", Action.LEFT_CLICK),
            new ActionRecord("AirBurst", Action.SNEAK_DOWN),
            new ActionRecord("AirBurst", Action.LEFT_CLICK)
        );
	}

	@Override
	public Collection<Ability> createAbilities(AbilityUser user) {
        if (user.canUse(this, false, true)) {
            return List.of(new AirSweep(user, this)); 
        }
        return List.of();
	}
}
