package me.kwilson272.elementalmagic.core.display;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.scoreboard.Scoreboard;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.ability.AbilityStorage;
import me.kwilson272.elementalmagic.api.ability.Element;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.event.user.UserAddCooldownEvent;
import me.kwilson272.elementalmagic.api.event.user.UserCreationEvent;
import me.kwilson272.elementalmagic.api.event.user.UserDestructionEvent;
import me.kwilson272.elementalmagic.api.event.user.UserPostChangeBindEvent;
import me.kwilson272.elementalmagic.api.event.user.UserRemoveCooldownEvent;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.api.user.UserManager;
import me.kwilson272.elementalmagic.core.ability.CoreElement;
import net.md_5.bungee.api.ChatColor;

public class BoardManager implements Listener {
    
    @Configure(path = "Board.MiscCooldowns", config = Config.LANGUAGE)
    private List<String> configCooldowns;
    private Map<String, String> miscCooldowns;
    private final Map<AbilityUser, AbilityBoard> boardsByUser;

    public BoardManager() {
        configCooldowns = List.of(
            "WallRun:" + CoreElement.CHI.name(),
            "ChargedFireBlast:" + CoreElement.FIRE.name(),
            "IceSpikeBlast:" + CoreElement.ICE.name(),
            "IceSpikeField:" + CoreElement.ICE.name(),
            "IceSpikePillar:" + CoreElement.ICE.name(),
            "PhaseChangeFreeze:" + CoreElement.ICE.name(),
            "PhaseChangeMelt:" + CoreElement.ICE.name(),
            "SurgeWall:" + CoreElement.ICE.name(),
            "SurgeWave:" + CoreElement.WATER.name(),
            "TorrentWave:" + CoreElement.WATER.name(),
            "WaterWave:" + CoreElement.WATER.name()
        );
            
        boardsByUser = new HashMap<>();
        miscCooldowns = new HashMap<>();
        ElementalMagicApi.configManager().configure(this);

        parseMiscCooldowns();
    }

    private void parseMiscCooldowns() {
        Logger logger = ElementalMagicApi.logger();
        for (String s : configCooldowns) {
            if (!s.contains(":")) {
                logger.log(Level.WARNING, "Malformed misc cooldown: '" + s + "'");
            } else {
                String[] parsed = s.split(":", 2);
                miscCooldowns.put(parsed[0], parsed[1]);
            }
        }
    }
    
    @EventHandler
    private void onUserCreation(UserCreationEvent event) {
        Player player = event.getUser().player();
        AbilityBoard board = new AbilityBoard();
        boardsByUser.put(event.getUser(), board);

        board.setSelected(player.getInventory().getHeldItemSlot());
        player.setScoreboard(board.getScoreboard());
    }

    @EventHandler
    private void onUserDestruction(UserDestructionEvent event) {
        if (boardsByUser.remove(event.getUser()) != null) {
            Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
            event.getUser().player().setScoreboard(board);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onSlotChange(PlayerItemHeldEvent event) {
        UserManager userManager = ElementalMagicApi.userManager();
        userManager.get(event.getPlayer()).ifPresent(user -> {
            AbilityBoard board = boardsByUser.get(user);
            if (board != null) {
                board.setSelected(event.getNewSlot());
            }
        }); 
    }

    @EventHandler
    private void onBindChange(UserPostChangeBindEvent event) {
        displayBind(event.getUser(), event.getSlotNumber()-1, false); 
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onCooldownAdd(UserAddCooldownEvent event) {
        displayCooldowns(event.getUser(), event.getCooldownId(), true); 
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onCooldownRemove(UserRemoveCooldownEvent event) {
        displayCooldowns(event.getUser(), event.getCooldownId(), false);
    }
    
    private void displayBind(AbilityUser user, int slotNumber, boolean onCooldown) {
        AbilityBoard board = boardsByUser.get(user);
        if (board == null) {
            return;
        }

        AbilityController bind = user.getBindAt(slotNumber+1).orElse(null);
        if (bind == null) {
            board.clearSlot(slotNumber);
        
        } else {
            String color = bind.element().color().toString();
            if (onCooldown) {
                color += ChatColor.STRIKETHROUGH;
            }
            board.display(slotNumber, color + bind.name());
        }
    }

    private void displayCooldowns(AbilityUser user, String cooldownId, boolean onCooldown) {
        AbilityStorage storage = ElementalMagicApi.abilityStorage();
        AbilityController controller = storage.getController(cooldownId)
            .orElse(null);

        if (controller != null && controller.isBindable()) {
            AbilityController[] binds = user.getBinds();
            for (int i = 0; i < binds.length; ++i) {
                AbilityController bind = binds[i];
                if (bind != null && bind.name().equals(cooldownId)) {
                    displayBind(user, i, onCooldown);
                }
            }
            return;
        }
        
        AbilityBoard board = boardsByUser.get(user);
        if (board == null) {
            return;
        }

        if (!onCooldown) {
            board.removeMiscCooldown(cooldownId);
            return;
        }
        
        Element e;
        if (controller != null) {
            e = controller.element();
        } else {
            e = storage.getElement(miscCooldowns.get(cooldownId)).orElse(null);
        }

        if (e != null) {
            String display = "" + e.color() + ChatColor.STRIKETHROUGH + cooldownId;
            board.addMiscCooldown(cooldownId, display);
        }
    }
}
