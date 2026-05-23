package me.kwilson272.elementalmagic.core.gameplay.water.icewave;

import java.util.Collection;
import java.util.List;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.Ability;
import me.kwilson272.elementalmagic.api.ability.AbilityManager;
import me.kwilson272.elementalmagic.api.ability.Element;
import me.kwilson272.elementalmagic.api.ability.SequenceController;
import me.kwilson272.elementalmagic.api.activation.Action;
import me.kwilson272.elementalmagic.api.activation.ActionRecord;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.core.ability.CoreAbilityController;
import me.kwilson272.elementalmagic.core.ability.CoreElement;
import me.kwilson272.elementalmagic.core.gameplay.water.waterspout.WaterWave;

public class IceWaveController extends CoreAbilityController implements SequenceController {

    protected static final String CONFIG_PATH = "Abilities.Water.IceWave.";
    
    @Configure(path = CONFIG_PATH + "Description", config = Config.LANGUAGE)
    private String description = "Freeze your WaterWave and any enemies that happen to get caught in it!";
    @Configure(path = CONFIG_PATH + "Instructions", config = Config.LANGUAGE)
    private String instructions = "After charging waterwave, release sneak and left click phasechange.";

    public IceWaveController() {
        ElementalMagicApi.configManager().configure(this);
        ElementalMagicApi.configManager().configure(IceWave.CONFIG);
    }

	@Override
	public String name() {
        return "IceWave";
	}

	@Override
	public Element element() {
        return CoreElement.ICE;
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
            new ActionRecord("WaterSpout", Action.SNEAK_UP),
            new ActionRecord("PhaseChange", Action.LEFT_CLICK)
        );
	}

	@Override
	public Collection<Ability> createAbilities(AbilityUser user) {
        if (!user.canUse(this, false, true)) {
            return List.of();
        }

        AbilityManager manager = ElementalMagicApi.abilityManager();
        WaterWave wave = manager.getAbility(user, WaterWave.class).orElse(null);
        if (wave != null && wave.canIceWave()) {
            return List.of(new IceWave(user, this, wave));
        }

        return List.of();
	}
}
