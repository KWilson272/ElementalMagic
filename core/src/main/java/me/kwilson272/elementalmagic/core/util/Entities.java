package me.kwilson272.elementalmagic.core.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.collision.AABB;
import me.kwilson272.elementalmagic.api.collision.BoundingVolume;

public final class Entities {

    private Entities() { }

    /**
     * Gets the nearby entities in the world within a cube.
     *
     * @param location the {@link Location} center of the cube.
     * @param hitboxSize the Double side length of the cube.
     * @return a {@link Collection} of found entities.
     */
    public static Collection<Entity> getNearbyEntities(Location location,
                                                       double hitboxSize) {
        if (location.getWorld() == null) {
            return List.of();
        }

        double len = hitboxSize / 2;
        return location.getWorld().getNearbyEntities(location, len, len, len);
    }

    /**
     * Gets the nearby entities in a world within the {@link BoundingVolume}.
     *
     * @param world the {@link World} the entities are in.
     * @param bv the {@code BoundingVolume}.
     * @return a {@link Collection} of found entities.
     */
    public static Collection<Entity> getNearbyEntities(World world,
                                                       BoundingVolume bv) {
        BoundingBox box = bv.getEnclosingBox().toBukkit();
        if (bv instanceof AABB) {
            return world.getNearbyEntities(box);
        }

        List<Entity> entities = new ArrayList<>();
        for (Entity entity : world.getNearbyEntities(box)) {
            AABB eBox = AABB.fromBukkit(entity.getBoundingBox());
            if (eBox.intersects(bv)) {
                entities.add(entity);
            }
        }

        return entities;
    }
    
    /**
     * Gets the targted {@link Location} of a {@link LivingEntity}.
     *
     * @param entity the {@code LivingEntity} targeting.
     * @param range the Double range of target selection.
     * @return the targeted {@code Location}.
     */
    public static Location getTargetLocation(LivingEntity entity, double range) {
        World world = entity.getWorld();
        Location start = entity.getEyeLocation();
        Vector direction = entity.getEyeLocation().getDirection();
    
        RayTraceResult result = world.rayTraceBlocks(
                start,
                direction,
                range,
                FluidCollisionMode.NEVER,
                true
        );

        if (result == null || result.getHitBlock() == null) {
            return start.add(direction.multiply(range));
        }

        Vector hitPosition = result.getHitPosition();
        // Avoid collisions by returning outside of solid blocks.
        hitPosition.subtract(direction.multiply(0.2));
        return hitPosition.toLocation(world);
    }

    /**
     * Gets the first collidable block the entity is looking at within range. 
     * This function will return the block at the end of the range regardless
     * of collisions if no earlier block was found.
     *
     * @param entity the {@link LivingEntity} targeting the block.
     * @param range the Double range of the check
     * @param collisionCheck the {@link Predicate} that returns true if a block
     * is collidable, false otherwise.
     * @return the targeted {@link Block}
     */
    public static Block getTargetBlock(LivingEntity entity, double range, 
                                       Predicate<Block> collisionCheck) {
        Vector start = entity.getEyeLocation().toVector();
        Vector dir = entity.getEyeLocation().getDirection();
        World world = entity.getWorld();
    
        Block block = entity.getEyeLocation().getBlock();
        BlockIterator iter = new BlockIterator(world, start, dir, 0, (int) range);
        while (iter.hasNext() && !collisionCheck.test(block)) {
            block = iter.next();
        }

        return block;
    }

    /**
     * Gets the {@link BlockFace} on the provided {@link Block} the entity is 
     * targeting. If the block is not targeted, this function will return 
     * {@code BlockFace.SELF}.
     *
     * @param entity the {@link LivingEntity} targeting the block.
     * @param block the {@code Block} being targeted.
     * @return the targeted BlockFace.
     */
    public static BlockFace getTargetFace(LivingEntity entity, Block block) {
        Location origin = entity.getEyeLocation();
        Vector dir = origin.getDirection();

        double xMin = block.getX();
        double xMax = xMin + 1.0;
        double yMin = block.getY();
        double yMax = yMin + 1.0;
        double zMin = block.getZ();
        double zMax = zMin + 1.0;

        // Essentially bounds for 'times to entry' for each axis 
        double tXMin = (xMin - origin.getX()) / dir.getX();
        double tXMax = (xMax - origin.getX()) / dir.getX();
        double tYMin = (yMin - origin.getY()) / dir.getY();
        double tYMax = (yMax - origin.getY()) / dir.getY();
        double tZMin = (zMin - origin.getZ()) / dir.getZ();
        double tZMax = (zMax - origin.getZ()) / dir.getZ();

        double xNear = Math.min(tXMin, tXMax);
        double xFar = Math.max(tXMin, tXMax);
        double yNear = Math.min(tYMin, tYMax);
        double yFar = Math.max(tYMin, tYMax);
        double zNear = Math.min(tZMin, tZMax);
        double zFar = Math.max(tZMin, tZMax);

        // We are only inside a block if we have entered all 3 axes
        double entry = Math.max(xNear, Math.max(zNear, yNear));
        double exit = Math.min(xFar, Math.min(yFar, zFar));
        if (exit < 0 || entry > exit) {
            return BlockFace.SELF;
        }

        if (entry == xNear) {
            return dir.getX() > 0 ? BlockFace.WEST : BlockFace.EAST;
        } else if (entry == yNear) {
            return dir.getY() > 0 ? BlockFace.DOWN : BlockFace.UP;
        } else if (entry == zNear) {
            return dir.getZ() > 0 ? BlockFace.NORTH : BlockFace.SOUTH;
        }

        return BlockFace.SELF;
    }
}

