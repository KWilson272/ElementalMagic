package me.kwilson272.elementalmagic.core.gameplay.components;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

public abstract class BlockStream {
    
    private Location location;
    private double speed;
    private double range;
    private Block prevBlock;
    
    public BlockStream(Location location, double speed, double range) {
        this.location = location;
        this.speed = speed;
        this.range = range;
        this.prevBlock = null;
    }

    public boolean progress() {
        double remainder = speed;
        while (remainder > 0) {
            Block block = location.getBlock();
            if (!block.equals(prevBlock) && collidesWith(block)) {
                return false;
            }
            
            prevBlock = block;
            createBlock(block);

            double speed = Math.min(remainder, 1);
            remainder--;
            range -= speed;
            if (range <= 0) {
                return false;
            }

            location.add(getDirection().multiply(speed));
        }

        return true;
    }

    public abstract boolean collidesWith(Block block);

    public abstract void createBlock(Block block);
    
    public abstract Vector getDirection();
}
