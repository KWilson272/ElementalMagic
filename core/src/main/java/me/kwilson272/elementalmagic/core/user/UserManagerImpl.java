package me.kwilson272.elementalmagic.core.user;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.database.UserStorage;
import me.kwilson272.elementalmagic.api.event.user.UserCreationEvent;
import me.kwilson272.elementalmagic.api.event.user.UserDestructionEvent;
import me.kwilson272.elementalmagic.api.event.user.UserLoadEvent;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.api.user.UserManager;
import me.kwilson272.elementalmagic.api.user.UserProfile;

public class UserManagerImpl implements UserManager {
    
    private final Map<Player, AbilityUser> usersByPlayer;
    private boolean isShuttingDown;

    public UserManagerImpl() {
        this.usersByPlayer = new HashMap<>(); 
        this.isShuttingDown = false;
    }
    
    @Override 
    public void enable() {
    }

    @Override
    public void disable(boolean shutDown) {
        isShuttingDown = shutDown;
        getAll().forEach(this::destroy);
    }
    
    @Override
    public AbilityUser create(Player player) {
        if (usersByPlayer.containsKey(player)) {
            return usersByPlayer.get(player);
        }
        
        AbilityUser user = new CoreUser(player);
        usersByPlayer.put(player, user);

        Event event = new UserCreationEvent(user);
        Bukkit.getPluginManager().callEvent(event);

        Plugin plugin = ElementalMagicApi.plugin();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> loadData(user));

        return user;
    }

    @Override
    public void loadData(AbilityUser user) {
        Player player = user.player();
        UserStorage storage = ElementalMagicApi.userStorage();
        
        storage.initPlayerData(player);
        storage.loadProfile(player.getUniqueId())
            .ifPresent(p -> syncLoadProfile(user, p));
    }
    
    private void syncLoadProfile(AbilityUser user, UserProfile profile) {
        Plugin plugin = ElementalMagicApi.plugin();
        Bukkit.getScheduler().runTask(plugin, () -> {
            user.loadProfile(profile);
            Event event = new UserLoadEvent(user);
            Bukkit.getPluginManager().callEvent(event);
        });
    }

    @Override
    public void storeData(AbilityUser user) {
        UserProfile profile = user.exportProfile();
        UserStorage storage = ElementalMagicApi.userStorage();
        storage.storeProfile(user.player().getUniqueId(), profile);
    }

    @Override
    public void destroy(AbilityUser user) {
        if (!usersByPlayer.containsKey(user.player())) {
            return;
        }

        usersByPlayer.remove(user.player());
        Event event = new UserDestructionEvent(user);
        Bukkit.getPluginManager().callEvent(event);

        if (isShuttingDown) {
            storeData(user); 
        } else {
            Plugin plugin = ElementalMagicApi.plugin();
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> storeData(user));
        }
    }

    @Override
    public Optional<AbilityUser> get(Player player) {
        return Optional.ofNullable(usersByPlayer.get(player)); 
    }

    @Override
    public Collection<AbilityUser> getAll() {
        return new ArrayList<>(usersByPlayer.values());
    }
}
