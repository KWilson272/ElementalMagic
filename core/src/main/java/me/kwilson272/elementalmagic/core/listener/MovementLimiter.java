package me.kwilson272.elementalmagic.core.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;

public class MovementLimiter implements Listener {
    
    @EventHandler
    private void onMove(PlayerMoveEvent event) {
         Player player = event.getPlayer();
         if (ElementalMagicApi.effectHandler().isImmobilized(player)) {
            event.setCancelled(true);
         }
    }

    @EventHandler
    private void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (ElementalMagicApi.effectHandler().isImmobilized(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (ElementalMagicApi.effectHandler().isImmobilized(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onSprint(PlayerToggleSprintEvent event) {
        Player player = event.getPlayer();
        if (ElementalMagicApi.effectHandler().isImmobilized(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (ElementalMagicApi.effectHandler().isImmobilized(player)) {
            event.setCancelled(true);
        }
    }
}
