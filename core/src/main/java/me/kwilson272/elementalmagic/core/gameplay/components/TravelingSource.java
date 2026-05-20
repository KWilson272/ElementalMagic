package me.kwilson272.elementalmagic.core.gameplay.components;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.revertible.RevertibleManager;
import me.kwilson272.elementalmagic.api.revertible.TempBlock;
import me.kwilson272.elementalmagic.api.revertible.TempBlock.TempBlockBuilder;
import me.kwilson272.elementalmagic.api.util.BlockUtil;
import me.kwilson272.elementalmagic.core.gameplay.util.VectorUtil;

public class TravelingSource {

    private static final Vector[] MOVE_DIRS = {
        BlockFace.UP.getDirection(), BlockFace.DOWN.getDirection(),
        BlockFace.NORTH.getDirection(), BlockFace.SOUTH.getDirection(),
        BlockFace.EAST.getDirection(), BlockFace.WEST.getDirection()
    };

    public static enum TravelState {
        MOVING,
        BLOCKED,
        ARRIVED
    }
        
    private double speed;
    private int maxLength;
    private boolean prioritizeY;
    private TempBlockBuilder blockBuilder;
    
    private Location location;
    private Block source;
    private Deque<TempBlock> tempBlocks;
    private Set<Block> affectedBlocks;

    public TravelingSource(Location location, double speed, boolean prioritizeY,
                                                    TempBlockBuilder builder) {
        this.speed = speed;
        this.maxLength = (int) Math.ceil(speed);
        this.prioritizeY = prioritizeY;
        this.blockBuilder = builder;

        this.location = location;
        this.source = location.getBlock();
        this.tempBlocks = new ArrayDeque<>();
        this.affectedBlocks = new HashSet<>();
    }

    public TravelState moveTowards(Location dest, double successDist) {
        double remainder = speed;
        while (remainder > 0) {
            double curSpeed = Math.min(1, remainder);
            --remainder;

            Block block = location.getBlock();
            if (isCollidable(block)) {
                return TravelState.BLOCKED;
            }

            if (!affectedBlocks.contains(block)) {
                blockBuilder.buildAt(block).ifPresent(tb -> {
                    tempBlocks.offerFirst(tb);
                    affectedBlocks.add(block);
                });

                if (location.distanceSquared(dest) <= successDist * successDist) {
                    return TravelState.ARRIVED;
                }
            }

            Vector dir = getSafeDir(dest).multiply(curSpeed);
            location.add(dir);
        }

        cleanOldBlocks();
        return TravelState.MOVING;
    }

    private void cleanOldBlocks() {
        while (tempBlocks.size() > maxLength) {
            TempBlock tb = tempBlocks.pollLast();
            affectedBlocks.remove(tb.block());
            ElementalMagicApi.revertibleManager().revert(tb);
        }   
    }

    private Vector getSafeDir(Location dest) {
        Vector toTarget = getToTarget(dest);
        Block check = location.clone().add(toTarget).getBlock();
        if (!isCollidable(check)) {
            return toTarget;
        }
        
        // Very naive pathfinding, but it works fine enough
        Vector minDir = toTarget;
        double minDist = Double.MAX_VALUE;
        for (Vector dir : MOVE_DIRS) {
            Location testLoc = location.clone().add(dir);
            Location nextLoc = testLoc.clone().add(toTarget);
            Block checkNow = testLoc.getBlock();
            // Don't move to a block that would cause a collision next tick
            Block checkNext = nextLoc.getBlock();
            if (isCollidable(checkNow) || isCollidable(checkNext)) {
                continue;
            }

            double dist = testLoc.distanceSquared(dest);
            if (dist < minDist) {
                minDist = dist;
                minDir = dir;
            }
        }

        return minDir;
    }

    private Vector getToTarget(Location target) {
        double yDiff = target.getBlockY() - location.getBlockY();
        if (yDiff != 0 && prioritizeY) {
            return new Vector(0, yDiff, 0).normalize();
        }

        return VectorUtil.getDirection(location, target).normalize();
    }

    private boolean isCollidable(Block block) {
        return BlockUtil.isSolid(block) 
            && !affectedBlocks.contains(block)
            && !block.equals(source); 
    }

    public void revertBlocks() {
        RevertibleManager revertManager = ElementalMagicApi.revertibleManager();
        tempBlocks.forEach(revertManager::revert);
    }

    public Location getLocation() {
        return location;
    }
}
