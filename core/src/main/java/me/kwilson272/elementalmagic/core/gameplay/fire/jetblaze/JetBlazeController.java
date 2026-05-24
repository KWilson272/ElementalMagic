package me.kwilson272.elementalmagic.core.gameplay.fire.jetblaze;

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

public class JetBlazeController extends CoreAbilityController implements SequenceController {

    protected static final String CONFIG_PATH = "Abilities.Fire.JetBlaze.";

    @Configure(path = CONFIG_PATH + "Description", config = Config.LANGUAGE)
    private String description = "Skilled fire users can superheat their FireJet to burn even the air around them.";
    @Configure(path = CONFIG_PATH + "Instructions", config = Config.LANGUAGE)
    private String instructions = "FireJet (Tap-Sneak Twice) > Blaze (Tap-Sneak) > FireJet (Left-Click)";

    public JetBlazeController() {
        ElementalMagicApi.configManager().configure(this);
        ElementalMagicApi.configManager().configure(JetBlaze.CONFIG);
    }

	@Override
	public String name() {
        return "JetBlaze";
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
            new ActionRecord("FireJet", Action.SNEAK_DOWN),
            new ActionRecord("FireJet", Action.SNEAK_UP),
            new ActionRecord("FireJet", Action.SNEAK_DOWN),
            new ActionRecord("FireJet", Action.SNEAK_UP),
            new ActionRecord("Blaze", Action.SNEAK_DOWN),
            new ActionRecord("Blaze", Action.SNEAK_UP),
            new ActionRecord("FireJet", Action.LEFT_CLICK)
        );
	}

	@Override
	public Collection<Ability> createAbilities(AbilityUser user) {
        if (user.canUse(this, false, true)) {
            return List.of(new JetBlaze(user, this));
        }
        return List.of();
	}
}
