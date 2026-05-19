package me.kwilson272.elementalmagic.core.activation;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.activation.Action;
import me.kwilson272.elementalmagic.api.activation.Activation;
import me.kwilson272.elementalmagic.api.activation.ActivationManager;
import me.kwilson272.elementalmagic.api.activation.activations.ActionActivation;
import me.kwilson272.elementalmagic.api.activation.activations.FallDamageActivation;
import me.kwilson272.elementalmagic.api.user.UserManager;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;

/**
 * Handles dispatch of various Activations to the ActivationManager.
 */
public class ActivationListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    private void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == org.bukkit.event.block.Action.PHYSICAL
            || event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR) {
            return;
        }

        UserManager userManager = ElementalMagicApi.userManager();
        ActivationManager activationManager = ElementalMagicApi.activationManager();

        userManager.get(event.getPlayer()).ifPresent(user -> {
            Action action = switch (event.getAction()) {
                case LEFT_CLICK_BLOCK -> Action.LEFT_CLICK_BLOCK;
                case RIGHT_CLICK_BLOCK -> Action.RIGHT_CLICK_BLOCK;
                case LEFT_CLICK_AIR -> Action.LEFT_CLICK;
                default -> null; // Should never happen given above check
            };

            Activation activation = new ActionActivation(action);
            activationManager.postActivation(user, activation);
            activationManager.handleAction(user, action);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSneak(PlayerToggleSneakEvent event) {
        UserManager userManager = ElementalMagicApi.userManager();
        ActivationManager activationManager = ElementalMagicApi.activationManager();

        userManager.get(event.getPlayer()).ifPresent(user -> {
            Action action = event.isSneaking() ?
                    Action.SNEAK_DOWN : Action.SNEAK_UP;
            Activation activation = new ActionActivation(action);
            activationManager.postActivation(user, activation);
            activationManager.handleAction(user, action);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onFlight(PlayerToggleFlightEvent event) {
        UserManager userManager = ElementalMagicApi.userManager();
        ActivationManager activationManager = ElementalMagicApi.activationManager();

        userManager.get(event.getPlayer()).ifPresent(user -> {
            Action action = event.isFlying() ?
                    Action.TOGGLE_FLIGHT_ON : Action.TOGGLE_FLIGHT_OFF;
            Activation activation = new ActionActivation(action);
            activationManager.postActivation(user, activation);
            activationManager.handleAction(user, action);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSprint(PlayerToggleSprintEvent event) {
        UserManager userManager = ElementalMagicApi.userManager();
        ActivationManager activationManager = ElementalMagicApi.activationManager();

        userManager.get(event.getPlayer()).ifPresent(user -> {
            Action action = event.isSprinting() ?
                    Action.TOGGLE_SPRINT_ON : Action.TOGGLE_SPRINT_OFF;
            Activation activation = new ActionActivation(action);
            activationManager.postActivation(user, activation);
            activationManager.handleAction(user, action);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFall(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL
                || !(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        UserManager userManager = ElementalMagicApi.userManager();
        ActivationManager activationManager = ElementalMagicApi.activationManager();

        userManager.get(player).ifPresent(user -> {
            FallDamageActivation fd = new FallDamageActivation(event.getDamage());
            activationManager.postActivation(user, fd);
            if (fd.getDamage() <= 0) {
                event.setCancelled(true);
            }
        });
    }
}
