package me.kwilson272.elementalmagic.core.gameplay.water.torrent;

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

public class TorrentController extends CoreAbilityController {

    protected static final String CONFIG_PATH = "Abilities.Water.Torrent.";

    @Configure(path = CONFIG_PATH + "Description", config = Config.LANGUAGE)
    private String description = "Torrent is another fundamental ability in the water-user's toolkit. Users can " +
            "launch heavy streams of water to freeze their enemies, or create outwardly expanding shields that blow away opponents!";
    @Configure(path = CONFIG_PATH + "Instructions", config = Config.LANGUAGE)
    private String instructions = """
        Left-Click while looking at a water source, and hold sneak to draw it near. once it rotates around you:
        (Stream) - Left-Click again to fire, and Left-Click once more to activate freeze
        (Wave) - Release-Sneak to create an expanding ring of water
        """;

    public TorrentController() {
        ElementalMagicApi.configManager().configure(this);
        ElementalMagicApi.configManager().configure(Torrent.CONFIG);
        ElementalMagicApi.configManager().configure(TorrentWave.CONFIG);
    }

    @Activator
    public Collection<Ability> onAction(AbilityUser user, ActionActivation activation) {
        if (!canActivateBy(activation.action())
                || activation.action() == Action.SNEAK_DOWN) {
            return List.of();
        }
    
        return activation.action() == Action.SNEAK_UP ? 
            handleSneak(user) : handleLeftClick(user);
    }

    private Collection<Ability> handleSneak(AbilityUser user) {
        if (!user.canUse(this, true, false) || user.isOnCooldown("TorrentWave")) {
            return List.of();
        }

        AbilityManager manager = ElementalMagicApi.abilityManager();
        boolean makeWave = !manager.destroyIf(user, Torrent.class, 
                Torrent::isCharged).isEmpty();
        return makeWave ? List.of(new TorrentWave(user, this)) : List.of();
    }

    private Collection<Ability> handleLeftClick(AbilityUser user) {
        if (!user.canUse(this, true, false)) {
            return List.of();
        }

        AbilityManager manager = ElementalMagicApi.abilityManager();
        boolean fired = manager.getUserAbilities(user, Torrent.class)
            .anyMatch(Torrent::handleLeftClick);

        if (!fired && !user.isOnCooldown("Torrent")) {
            return List.of(new Torrent(user, this));
        }
        return List.of();
    }

    @Override
    public String name() {
        return "Torrent";
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

    @Override
    public boolean canActivateBy(Action action) {
        return action == Action.LEFT_CLICK 
            || action == Action.LEFT_CLICK_BLOCK
            || action == Action.HIT_ENTITY 
            || action == Action.SNEAK_UP
            || action == Action.SNEAK_DOWN;
    }
}
