package me.kwilson272.elementalmagic.core;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import me.kwilson272.elementalmagic.api.user.UserManager;
import me.kwilson272.elementalmagic.core.user.UserManagerImpl;
import me.kwilson272.elementalmagic.core.user.UserListener;

public class ElementalMagicPlugin extends JavaPlugin {
    
    private UserManager userManager;
    private Listener userListener;

    @Override
    public void onEnable() {
        userManager = new UserManagerImpl(this);
        userListener = new UserListener(userManager);
        Bukkit.getPluginManager().registerEvents(userListener, this);
    }

    @Override
    public void onDisable() {
        userManager.disable(true); 
    }
}
