package me.kwilson272.elementalmagic.core.gameplay.air.airstream;

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

public class AirStreamController extends CoreAbilityController implements SequenceController {

    protected static final String CONFIG_PATH = "Abilities.Air.AirStream.";

    @Configure(path = CONFIG_PATH + "Description", config = Config.LANGUAGE)
    private String description = "Create a current of air that traps enemies and drags them along.";
    @Configure(path = CONFIG_PATH + "Instructions", config = Config.LANGUAGE)
    private String instructions = "AirShield (Sneak-Down) > AirSuction (Left-Click) > AirBlast (Left-Click)";

    public AirStreamController() {
        ElementalMagicApi.configManager().configure(this);
        ElementalMagicApi.configManager().configure(AirStream.CONFIG);
    }

	@Override
	public String name() {
        return "AirStream";
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
            new ActionRecord("AirShield", Action.SNEAK_DOWN),
            new ActionRecord("AirSuction", Action.LEFT_CLICK),
            new ActionRecord("AirBlast", Action.LEFT_CLICK)
        );
	}

	@Override
	public Collection<Ability> createAbilities(AbilityUser user) {
        if (user.canUse(this, false, true)) {
            return List.of(new AirStream(user, this));
        }
        return List.of();
	}
}
