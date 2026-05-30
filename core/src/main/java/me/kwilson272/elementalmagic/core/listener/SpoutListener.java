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
import me.kwilson272.elementalmagic.core.gameplay.air.airspout.AirSpout;
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

        double x = event.getTo().getX() - event.getFrom().getX();
        double y = event.getTo().getY() - event.getFrom().getY();
        double z = event.getTo().getZ() - event.getFrom().getZ();
        Vector movement = new Vector(x, y, z);
    
        WaterSpout wSpout = abilityManager.getAbility(user, WaterSpout.class).orElse(null);
        if (wSpout != null) {
            // Up max speed to prevent feedback loop of movement
            double maxSpeed = Math.pow(wSpout.getSpeed() + 0.01, 2);
            if (movement.lengthSquared() > maxSpeed) {
                movement.normalize().multiply(wSpout.getSpeed());
                event.getPlayer().setVelocity(movement);
            }
            return;
        }

        AirSpout aSpout = abilityManager.getAbility(user, AirSpout.class).orElse(null);
        if (aSpout != null) {
            // Up max speed to prevent feedback loop of movement
            double maxSpeed = Math.pow(aSpout.getSpeed() + 0.01, 2);
            if (movement.lengthSquared() > maxSpeed) {
                movement.normalize().multiply(aSpout.getSpeed());
                event.getPlayer().setVelocity(movement);
            }
            return;
        }

    }
}

