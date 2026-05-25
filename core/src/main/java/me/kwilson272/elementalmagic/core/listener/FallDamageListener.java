package me.kwilson272.elementalmagic.core.listener;

import java.awt.Dialog.ModalExclusionType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.Ability;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.Configure;
import me.kwilson272.elementalmagic.api.event.ability.AbilityAffectVelocityEvent;
import me.kwilson272.elementalmagic.api.event.ability.AbilityDestructionEvent;
import me.kwilson272.elementalmagic.api.revertible.Revertible;

public class FallDamageListener implements Listener {
    
    private static final String CONFIG_PATH = "Properties.NoFall.";

    @Configure(path = CONFIG_PATH + "ProtectionDuration", config = Config.PLUGIN_PROPERTIES)
    private long protectionDuration = 4000;
    @Configure(path = CONFIG_PATH + "OnRemoval", config = Config.PLUGIN_PROPERTIES)
    private List<String> configOnRemoval = List.of("WaterSpout");
    @Configure(path = CONFIG_PATH + "OnEffect", config = Config.PLUGIN_PROPERTIES)
    private List<String> configOnEffect = List.of("Icicle", "IceSpikeField", "IceSpikePillar");
    
    private final Set<String> onRemoval;
    private final Set<String> onEffect;
    private final Map<Entity, FallDamageTracker> noFall;

    public FallDamageListener() {
        ElementalMagicApi.configManager().configure(this);

        // Spigot doesnt allow us to store sets in yaml files
        onRemoval = new HashSet<>(configOnRemoval);
        onEffect = new HashSet<>(configOnEffect);
        noFall = new HashMap<>();
    }

    // Highest so we ensure the fall activation fires
    @EventHandler(priority = EventPriority.HIGHEST)
    private void onFall(EntityDamageEvent event) {
        if (event.getCause() != DamageCause.FALL 
                || !noFall.containsKey(event.getEntity())) {
            return;
        }
        
        FallDamageTracker tracker = noFall.get(event.getEntity());
        ElementalMagicApi.revertibleManager().revert(tracker);
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onDestroy(AbilityDestructionEvent event) {
        Ability ability = event.getAbility();
        if (onRemoval.contains(ability.name())) {
            trackFall(event.getAbility().user().player());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onVelocity(AbilityAffectVelocityEvent event) {
        Ability ability = event.getAbility();
        if (onEffect.contains(ability.name())) {
            trackFall(event.getAffected());
        }
    }

    private void trackFall(Entity entity) {
        FallDamageTracker tracker = noFall.get(entity);
        if (tracker != null) {
            ElementalMagicApi.revertibleManager().revert(tracker);
        }

        FallDamageTracker newTracker = new FallDamageTracker(entity);
        ElementalMagicApi.revertibleManager().register(newTracker);
        noFall.put(entity, newTracker);
    }

    @EventHandler
    private void onQuit(PlayerQuitEvent event) {
        FallDamageTracker tracker = noFall.get(event.getPlayer());
        if (tracker != null) {
            ElementalMagicApi.revertibleManager().revert(tracker);
        }
    }

    @EventHandler
    private void onDeath(EntityDeathEvent event) {
        FallDamageTracker tracker = noFall.get(event.getEntity());
        if (tracker != null) {
            ElementalMagicApi.revertibleManager().revert(tracker);
        }   
    }
    
    private class FallDamageTracker implements Revertible {

        private final Entity entity;
        private final long duration;
        private final long revertTime;
        private boolean isReverted;

        FallDamageTracker(Entity entity) {
            this.entity = entity;
            this.duration = protectionDuration;
            this.revertTime = System.currentTimeMillis() + duration;
            this.isReverted = false;
        }

		@Override
		public long getDurationMillis() {
            return duration;
		}

		@Override
		public long getRevertTimeMillis() {
            return revertTime;
		}

		@Override
		public void handleRevertTasks() {
            noFall.remove(entity);
            isReverted = true;
		}

		@Override
		public boolean isReverted() {
            return isReverted;
		}
    }
}
