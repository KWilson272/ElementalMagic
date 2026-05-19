package me.kwilson272.elementalmagic.core.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityManager;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.api.user.UserManager;
import me.kwilson272.elementalmagic.core.gameplay.water.waterspout.WaterSpout;

public class SpoutListener implements Listener {

    @EventHandler
    private void onMove(PlayerMoveEvent event) {
        UserManager userManager = ElementalMagicApi.userManager();
        AbilityManager abilityManager = ElementalMagicApi.abilityManager();
        
        Player player = event.getPlayer();
        AbilityUser user = userManager.get(player).orElse(null);
        if (user == null || !player.isFlying() || event.getTo() == null
                || event.getTo().equals(event.getFrom())) {
            return;
        }
    
        WaterSpout spout = abilityManager.getAbility(user, WaterSpout.class).orElse(null);
        if (spout == null) {
            return;
        }

        double x = event.getTo().getX() - event.getFrom().getX();
        double y = event.getTo().getY() - event.getFrom().getY();
        double z = event.getTo().getZ() - event.getFrom().getZ();
        Vector movement = new Vector(x, y, z);

        // Up max speed to prevent feedback loop of movement
        double maxSpeed = Math.pow(spout.getSpeed() + 0.01, 2);
        if (movement.lengthSquared() > maxSpeed) {
            movement.normalize().multiply(spout.getSpeed());
            event.getPlayer().setVelocity(movement);
        }
    }
}

