package me.kwilson272.elementalmagic.api.event.ability;

import me.kwilson272.elementalmagic.api.ability.Ability;

import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;

/**
 * Event to be called whenever an Ability changes an Entity's fire ticks.
 */
public class AbilitySetFireTicksEvent extends AbilityAffectEntityEvent {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private long durationMilli;

    public AbilitySetFireTicksEvent(Ability cause, Entity affected, long durationMilli) {
        super(cause, affected);
        this.durationMilli = durationMilli;
    }

    /**
     * Returns the amount of milliseconds for which the Entity will burn.
     *
     * @return the long amount of milliseconds for which the Entity will burn
     */
    public long getDurationMilliseconds() {
        return durationMilli;
    }

    /**
     * Returns the amount of ticks for which the Entity will burn.
     *
     * @return an int amount of ticks for which the Entity will burn
     */
    public int getDurationTicks() {
        return (int) (durationMilli / 50);
    }

    /**
     * Sets the duration for which the Entity will burn in milliseconds.
     *
     * @param newDuration the int amount of milliseconds for which the Entity
     *                    will burn
     */
    public void setDurationMilliseconds(int newDuration) {
        durationMilli = newDuration;
    }

    /**
     * Sets the duration for which the Entity will burn in ticks.
     *
     * @param newDuration the int amount of ticks for which the Entity will
     *                    burn
     */
    public void setDurationTicks(int newDuration) {
        durationMilli = newDuration * 50L;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
