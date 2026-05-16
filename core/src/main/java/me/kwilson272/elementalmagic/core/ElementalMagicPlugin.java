package me.kwilson272.elementalmagic.core;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import me.kwilson272.elementalmagic.api.ability.AbilityManager;
import me.kwilson272.elementalmagic.api.ability.AbilityStorage;
import me.kwilson272.elementalmagic.api.user.UserManager;
import me.kwilson272.elementalmagic.core.user.UserManagerImpl;
import me.kwilson272.elementalmagic.core.ability.AbilityManagerImpl;
import me.kwilson272.elementalmagic.core.ability.AbilityStorageImpl;
import me.kwilson272.elementalmagic.core.ability.CoreElement;
import me.kwilson272.elementalmagic.core.user.UserListener;

public class ElementalMagicPlugin extends JavaPlugin {
    
    private AbilityManager abilityManager;
    private AbilityStorage abilityStorage;
    private UserManager userManager;
    private Listener userListener;

    @Override
    public void onEnable() {
        abilityManager = new AbilityManagerImpl();
        abilityStorage = new AbilityStorageImpl();
        userManager = new UserManagerImpl(this);
        userListener = new UserListener(userManager);

        Bukkit.getPluginManager().registerEvents(userListener, this);
        Bukkit.getScheduler().runTaskTimer(this, abilityManager::progressAll, 1, 1);

        storeCoreElements();
        storeCoreAbilities();

        abilityManager.enable();
        abilityStorage.enable();
        userManager.enable();
    }


    private void storeCoreElements() {
        abilityStorage.registerElement(CoreElement.AIR);
        abilityStorage.registerElement(CoreElement.SOUND);
        abilityStorage.registerElement(CoreElement.FLIGHT);
        abilityStorage.registerElement(CoreElement.AVATAR);
        abilityStorage.registerElement(CoreElement.CHI);
        abilityStorage.registerElement(CoreElement.WEAPONRY);
        abilityStorage.registerElement(CoreElement.EARTH);
        abilityStorage.registerElement(CoreElement.SAND);
        abilityStorage.registerElement(CoreElement.MUD);
        abilityStorage.registerElement(CoreElement.METAL);
        abilityStorage.registerElement(CoreElement.LAVA);
        abilityStorage.registerElement(CoreElement.FIRE);
        abilityStorage.registerElement(CoreElement.LIGHTNING);
        abilityStorage.registerElement(CoreElement.COMBUSTION);
        abilityStorage.registerElement(CoreElement.WATER);
        abilityStorage.registerElement(CoreElement.ICE);
        abilityStorage.registerElement(CoreElement.PLANT);
        abilityStorage.registerElement(CoreElement.HEALING);
        abilityStorage.registerElement(CoreElement.BLOOD);
    }

    private void storeCoreAbilities() {
    
    }

    @Override
    public void onDisable() {
        abilityManager.disable(true);
        abilityStorage.disable(true);
        userManager.disable(true); 
    }
}
