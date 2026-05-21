package me.kwilson272.elementalmagic.core.gameplay.components;

import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.api.util.BlockUtil;
import me.kwilson272.elementalmagic.core.gameplay.util.VectorUtil;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public abstract class BlockBlast {

    public static enum State {
        SOURCED,
        SETTING_UP,
        TRAVELLING
    }

    private State state;
    private Location location;
    private Vector direction;
    private Block setUpDest;
    private Block finalDest;
    private double range;

    public BlockBlast(Location location, double range) {
        this.state = State.SOURCED;
        this.range = range;
        this.location = location;
    }

    public boolean progress() {
        if (state == State.SOURCED) {
            return true;
        }

        // We check this condition the tick AFTER we advance so the block can
        // render at the final destination; it looks cleaner/less buggy
        if (location.getBlock().equals(finalDest)) {
            return false;
        }

        // We have to do this line here so we can support EarthBlast redirection
        // before its set up, while also preventing manip from moving
        Block target = state == State.SETTING_UP ? setUpDest : finalDest;
        Location targetLoc = target.getLocation();
        Location blockLoc = location.getBlock().getLocation();
        direction = VectorUtil.getDirection(blockLoc, targetLoc).normalize();

        Block block = getNextBlock();
        Location dest = block.getLocation();
        if (isCollidable(block)
                || BlockUtil.collidesDiagonally(location, dest, this::isCollidable)) {
            return false;
        }

        moveTo(block);
        if (state == State.SETTING_UP && block.equals(setUpDest)) {
            state = State.TRAVELLING;
        }
        return true;
    }

    private Block getNextBlock() {
        Block oldBlock = location.getBlock();
        Block newBlock = oldBlock;
        while (newBlock.equals(oldBlock)) {
            newBlock = location.add(direction).getBlock();
        }
        return newBlock;
    }

    public abstract boolean isCollidable(Block block);

    public abstract void moveTo(Block block);

    public boolean isTargetedBy(AbilityUser user, double targetRange) {
        Location eye = user.player().getEyeLocation();
        Vector dir = eye.getDirection();
        Block block = location.getBlock();
        Location center = block.getLocation().add(0.5, 0.5, 0.5);
        Vector toCenter = VectorUtil.getDirection(eye, center);

        double distance = toCenter.length();
        if (distance == 0 || !Double.isFinite(distance)) {
            return false;
        }

        double bound = Math.atan(targetRange / distance);
        double angle = dir.angle(toCenter);
        return angle <= bound;
    }

    public void redirect(AbilityUser user, Block targetBlock, Entity targetEntity) {
        Location targetLoc = targetEntity != null ?
                targetEntity.getLocation() : targetBlock.getLocation();

        if (targetEntity == null) {
            // If we go with the exact target block, players will send blasts into
            // the ground when really they wanted it to glide across the floor.
            // We will try to check if the player was looking at the top of the block
            // and raise the target location if so, to make it feel better to use
            Player player = user.player();
            BlockFace face = BlockUtil.getTargetFace(player, targetBlock);
            if (face == BlockFace.UP) {
                targetLoc.add(0, 1, 0);
            }
        }

        finalDest = targetLoc.getBlock();

        if (state == State.SOURCED) {
            setUpDest = setUpFromSource(targetLoc);
            state = State.SETTING_UP;
        } else {
            // Using a block location so that the vector from the location to the
            // destination is somewhat consistent
            Location start = location.getBlock().getLocation();
            Location end = finalDest.getLocation();
            direction = VectorUtil.getDirection(start, end).normalize();
        }

        // If the final dest is an entity location and the entity moves away, the
        // blast will still remove even if the entity isn't there AND the ability
        // is not out of range. Attempt to mitigate this as it feels 'buggy'
        if (targetEntity != null) {
            Location loc = state == State.SETTING_UP ?
                setUpDest.getLocation() : location.getBlock().getLocation();
            Vector toDest = VectorUtil.getDirection(loc, finalDest.getLocation());
            toDest.normalize().multiply(range);
            finalDest = loc.clone().add(toDest).getBlock();
        }
    }

    public abstract Block setUpFromSource(Location target);

    public State getState() {
        return state;
    }

    public Location getLocation() {
        return location.clone();
    }

    public Vector getDirection() {
        return direction.clone();
    }
}

