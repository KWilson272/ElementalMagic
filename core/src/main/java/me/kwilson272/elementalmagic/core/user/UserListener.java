package me.kwilson272.elementalmagic.core.user;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import me.kwilson272.elementalmagic.api.user.UserManager;

public class UserListener implements Listener {

    private final UserManager manager;

    public UserListener(UserManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        manager.create(event.getPlayer());
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        manager.get(event.getPlayer()).ifPresent(manager::destroy);
    }
}
