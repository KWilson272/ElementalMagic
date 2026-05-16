package me.kwilson272.elementalmagic.api.user;

import org.bukkit.entity.Player;

public interface AbilityUser {
    
    /**
     * @return the underlying {@link Player} object for an AbilityUser.   
     */
    Player player(); 
}    

