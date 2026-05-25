package me.kwilson272.elementalmagic.core.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.user.UserManager;

public class HeatControlHelper implements Listener {
    
    @EventHandler
    private void onBurn(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)
                || (event.getCause() != DamageCause.FIRE 
                && event.getCause() != DamageCause.FIRE_TICK)) {
            return;
        }

        UserManager userManager = ElementalMagicApi.userManager();
        userManager.get((Player) event.getEntity()).ifPresent(user -> {
            if (user.getSelectedBindName().equals("HeatControl")) {
                event.getEntity().setFireTicks(0);
                event.setCancelled(true);
            }
        });
    }
}
