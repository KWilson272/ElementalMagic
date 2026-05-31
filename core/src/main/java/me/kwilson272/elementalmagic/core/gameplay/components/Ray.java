package me.kwilson272.elementalmagic.core.gameplay.components;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.core.util.Blocks;

public abstract class Ray {

    private Location location;
    private double speed;
    private double range;
    private double iterSpeed;

    public Ray(Location location, double speed, double range) {
        this.location = location;
        this.speed = speed;
        this.range = range;
        this.iterSpeed = calculateIterSpeed();
    }

    // Should help keep visuals decently similar across speeds
    private double calculateIterSpeed() {
        if (speed <= 1.0) {
            return speed;
        } else {
            int toRound = ((int) speed) + 5;
            double tenMod = Math.round(toRound / 10.0);
            return speed / (Math.floor(speed) + tenMod + 1);
        }
    }

    public boolean progress() {
        for (double i = 0; i < speed; i += iterSpeed) {
            Vector dir = getDirection().multiply(iterSpeed);
            Location prev = location.clone();
            location.add(dir);

            if (collides(location.getBlock()) 
                    || Blocks.collidesDiagonally(prev, location, this::collides)) {
                return false; 
            }

            if (!moveTo(location)) {
                return false;
            }

            range -= iterSpeed;
            if (range <= 0) {
                return false;
            }
        }
        return range > 0;
    }

    public Location getLocation() {
        return location;
    }
    
    public abstract boolean collides(Block block);

    public abstract boolean moveTo(Location loc);

    public abstract Vector getDirection();
}
