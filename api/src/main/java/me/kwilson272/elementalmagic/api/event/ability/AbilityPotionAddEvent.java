package me.kwilson272.elementalmagic.api.event.ability;

import me.kwilson272.elementalmagic.api.ability.Ability;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.HandlerList;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Event to be called whenever an Ability adds a potion effect to a LivingEntity.
 */
public class AbilityPotionAddEvent extends AbilityAffectEntityEvent {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final PotionEffectType potionEffectType;
    private int durationMilli;
    private int amplifier;

    public AbilityPotionAddEvent(Ability cause, LivingEntity affected, PotionEffect potionEffect) {
        super(cause, affected);
        this.potionEffectType = potionEffect.getType();
        this.durationMilli = potionEffect.getDuration() * 50;
        this.amplifier = potionEffect.getAmplifier();
    }

    /**
     * returns the Type of potion being applied.
     *
     * @return the PotionEffectType being applied
     */
    public PotionEffectType getPotionEffectType() {
        return potionEffectType;
    }

    /**
     * Returns the duration of the potion's effect in milliseconds.
     *
     * @return the duration of the potion's effect in milliseconds.
     */
    public int getDurationMilliseconds() {
        return durationMilli;
    }

    /**
     * Returns the duration of the potion's effect in ticks.
     *
     * @return the duration of the potion's effect in ticks.
     */
    public int getDurationTicks() {
        return durationMilli / 50;
    }

    /**
     * Returns the PotionEffect's amplifier.
     *
     * @return the int PotionEffect amplifier
     */
    public int getAmplifier() {
        return amplifier;
    }

    /**
     * Sets the duration of the potion's effect in milliseconds.
     *
     * @param newDuration the int duration of the potion's effect
     */
    public void setDurationMilliseconds(int newDuration) {
        durationMilli = newDuration;
    }

    /**
     * Sets the duration of the potion's effect in ticks.
     *
     * @param newDuration the int duration of the potion's effect
     */
    public void setDurationTicks(int newDuration) {
        durationMilli = newDuration * 50;
    }

    /**
     * Sets the amplifier value for the potion.
     *
     * @param newAmplifier the amplifier value for the potion
     */
    public void setAmplifier(int newAmplifier) {
        amplifier = newAmplifier;
    }

    /**
     * Returns a PotionEffect built using the set values.
     *
     * @return a PotionEffect built using the set values
     */
    public PotionEffect getPotionEffect() {
        return new PotionEffect(potionEffectType, getDurationTicks(), amplifier, true, false);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
