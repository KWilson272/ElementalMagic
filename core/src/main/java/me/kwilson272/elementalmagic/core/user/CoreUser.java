package me.kwilson272.elementalmagic.core.user;

import org.bukkit.entity.Player;

import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.api.user.UserProfile;

public class CoreUser implements AbilityUser {
    
    private final Player player;

    public CoreUser(Player player) {
        this.player = player;
    }

    @Override
    public Player player() {
        return player;
    }

    @Override
    public void loadProfile(UserProfile profile) {
    }
    
    @Override
    public UserProfile exportProfile() {
        return new UserProfile();
    }
}
