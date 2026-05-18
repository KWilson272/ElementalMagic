package me.kwilson272.elementalmagic.api.event.ability;

import me.kwilson272.elementalmagic.api.ability.Ability;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.HandlerList;
import org.bukkit.potion.PotionEffectType;

public class AbilityPotionRemoveEvent extends AbilityAffectEntityEvent {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final PotionEffectType potionEffectType;

    public AbilityPotionRemoveEvent(Ability cause, LivingEntity affected, PotionEffectType potionEffectType) {
        super(cause, affected);
        this.potionEffectType = potionEffectType;
    }

    /**
     * Returns the PotionEffectType being removed from the LivingEntity.
     *
     * @return the PotionEffectType being removed from the LivingEntity.
     */
    public PotionEffectType getPotionEffect() {
        return potionEffectType;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

}
