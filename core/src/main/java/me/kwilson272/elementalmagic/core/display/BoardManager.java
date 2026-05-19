package me.kwilson272.elementalmagic.core.display;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.scoreboard.Scoreboard;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.event.user.UserAddCooldownEvent;
import me.kwilson272.elementalmagic.api.event.user.UserCreationEvent;
import me.kwilson272.elementalmagic.api.event.user.UserDestructionEvent;
import me.kwilson272.elementalmagic.api.event.user.UserPostChangeBindEvent;
import me.kwilson272.elementalmagic.api.event.user.UserRemoveCooldownEvent;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.api.user.UserManager;
import net.md_5.bungee.api.ChatColor;

public class BoardManager implements Listener {
    
    private final Map<AbilityUser, AbilityBoard> boardsByUser;

    public BoardManager() {
        boardsByUser = new HashMap<>();
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
        displayBind(event.getUser(), event.getSlotNumber()-1); 
    }

    @EventHandler
    private void onCooldownAdd(UserAddCooldownEvent event) {
        displayCooldowns(event.getUser(), event.getCooldownId()); 
    }

    @EventHandler
    private void onCooldownRemove(UserRemoveCooldownEvent event) {
        displayCooldowns(event.getUser(), event.getCooldownId());
    }
    
    private void displayBind(AbilityUser user, int slotNumber) {
        AbilityBoard board = boardsByUser.get(user);
        if (board == null) {
            return;
        }

        AbilityController bind = user.getBindAt(slotNumber+1).orElse(null);
        if (bind == null) {
            board.clearSlot(slotNumber);
        
        } else {
            String color = bind.element().color().toString();
            if (user.isOnCooldown(bind.name())) {
                color += ChatColor.ITALIC;
            }
            board.display(slotNumber, color + bind.name());
        }
    }

    private void displayCooldowns(AbilityUser user, String cooldownId) {
        AbilityController[] binds = user.getBinds();
        for (int i = 0; i < binds.length; ++i) {
            AbilityController bind = binds[i];
            if (bind != null && bind.name().equals(cooldownId)) {
                displayBind(user, i);
            }
        }
    }
}
