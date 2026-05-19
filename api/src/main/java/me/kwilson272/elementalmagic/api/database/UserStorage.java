package me.kwilson272.elementalmagic.api.database;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.entity.Player;

import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.ability.Element;
import me.kwilson272.elementalmagic.api.user.UserProfile;

/**
 * Manages player data in persistent storage.
 */
public interface UserStorage {
   
    void enable();
    void disable(boolean shutDown);

    /**
     * Looks up the UUID of the player with the provided playerName. This
     * method will return an empty optional if not found, or if the player
     * has never logged on to the server.
     *
     * @param playerName the {@link String} name of the player.
     * @return an {@link Optional} of the player's {@link UUID}
     */
    public Optional<UUID> lookupUUID(String playerName);

    /* Initializes data for the provided player in the database if it doesn't
     * exist.
     *
     * @param player the {@link Player} being initialized.
     */
    void initPlayerData(Player player);

    /**
     * Loads the {@link UserProfile} for a player.
     *
     * @param uuid the {@link UUID} of the player
     * @return an {@link Optional} with the loaded profile. 
     */
    Optional<UserProfile> loadProfile(UUID uuid);
    
    /**
     * Stores the provided {@link UserProfile} for a player. 
     *
     * @param uuid the {@link UUID} of the player.
     * @param profile the {@code UserProfile} being stored.
     */
    void storeProfile(UUID uuid, UserProfile profile);
    
    /**
     * Loads the main bound {@link AbilityController} array for a player. If 
     * one could not be found, this function will return an empty array.
     *
     * @param uuid the {@link UUID} of the player.
     * @return the array of {@code AbilityController} binds
     */
    AbilityController[] loadBinds(UUID uuid);

    /**
     * Stores the provided binds for a player.
     *
     * @param uuid the {@link UUID} of the player. 
     * @param binds the array of {@link AbilityController} being stored.
     */
    void storeBinds(UUID uuid, AbilityController[] binds);
    
    /**
     * Loads the presets for a player. This will be empty in the case no entry
     * was found.
     *
     * @param uuid the {@link UUID} of the player. 
     * @return a map of preset names to {@link AbilityController} arrays.
     */
    Map<String, AbilityController[]> loadPresets(UUID uuid);

    /**
     * Stores the provided preset for a player. 
     *
     * @param uuid the {@link UUID} of the player.
     * @param binds the {@link AbilityController} array preset.
     * @param presetName the {@link String} name of the preset saved.
     */
    void storePreset(UUID uuid, AbilityController[] binds, String presetName);
    
    /**
     * Deletes the player's preset with the given name, if it exists.
     *
     * @param uuid the {@link UUID} of the player. 
     * @param presetName the {@link String} name of the preset.
     */
    void deletePreset(UUID uuid, String presetName);
    
    /**
     * Loads the saved elements for a player. This will be empty if no entry 
     * was found.
     *
     * @param uuid the {@link UUID} of the player. 
     * @return the map of {@link Element} and their toggle state.
     */
    Map<Element, Boolean> loadElements(UUID uuid);
    
    /**
     * Stores the provided elements for a player. 
     *
     * @param uuid the {@link UUID} of the player. 
     * @param elements the {@code Map} of elements and toggle states being saved.
     */
    void storeElements(UUID uuid, Map<Element, Boolean> elements);
}
