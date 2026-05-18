package me.kwilson272.elementalmagic.core.user;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.user.AbilityUser;

public class UserListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        AbilityUser user =
            ElementalMagicApi.userManager().create(event.getPlayer());
        ElementalMagicApi.abilityManager().registerUser(user);
        ElementalMagicApi.activationManager().registerUser(user);
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        ElementalMagicApi.userManager().get(player).ifPresent(user -> {
            ElementalMagicApi.abilityManager().unregisterUser(user);
            ElementalMagicApi.activationManager().unregisterUser(user);
            ElementalMagicApi.userManager().destroy(user);
        });
    }
}
