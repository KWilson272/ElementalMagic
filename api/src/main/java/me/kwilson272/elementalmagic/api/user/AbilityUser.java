package me.kwilson272.elementalmagic.api.user;

import org.bukkit.entity.Player;

public interface AbilityUser {
    
    /**
     * @return the underlying {@link Player} object for an AbilityUser.   
     */
    Player player();

    /**
     * Loads the provided {@link UserProfile}'s data into an AbilityUser.
     * 
     * @param profile the UserProfile to be loaded.
     */
    void loadProfile(UserProfile profile); 

    /**
     * @return an AbilityUser's data as a {@link UserProfile}.
     */
    UserProfile exportProfile();
}    

