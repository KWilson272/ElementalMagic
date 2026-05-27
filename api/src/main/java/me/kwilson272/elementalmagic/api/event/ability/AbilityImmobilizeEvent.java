package me.kwilson272.elementalmagic.api.event.ability;

import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;

import me.kwilson272.elementalmagic.api.ability.Ability;

public class AbilityImmobilizeEvent extends AbilityAffectEntityEvent {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private long durationMillis;

	public AbilityImmobilizeEvent(Ability ability, Entity affected, long durationMillis) {
		super(ability, affected);
        this.durationMillis = durationMillis;
    }

    /**
     * @return the duration the entity will be immobilized for in milliseconds.
     */
    public long getDurationMillis() {
        return durationMillis;
    }

    public void setDuration(long durationMillis) {
        this.durationMillis = durationMillis;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
