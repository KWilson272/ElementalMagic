package me.kwilson272.elementalmagic.api.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.revertible.TempBlock;

public final class BlockUtil {
    
    private BlockUtil() { }
    
    /**
     * @param block the checked {@link Block}
     * @return true if the block is a collidable solid false otherwise.
     */
    public static boolean isSolid(Block block) {
        return block.getType().isSolid()
            && TempBlock.get(block).map(TempBlock::isCollidable).orElse(true);
    }

    /**
     * @param block the checked {@link Block}
     * @return true if the block is a collidable liquid, false otherwise.
     */
    public static boolean isLiquid(Block block) {
        return block.isLiquid()
            && TempBlock.get(block).map(TempBlock::isCollidable).orElse(true);
    }

    /**
     * Checks if there is a collision with diagonally connected blocks between
     * the start and end locations. This function is designed to work with
     * locations that are <= 1 block away from each other.
     *
     * @param start the first {@link Location}
     * @param end the second {@link Location}
     * @param collisionCheck the {@link Predicate} that must return true if a 
     * block is collidable, false otherwise.
     * @return true if there is a diagonal block collision, false otherwise.
     */
    public static boolean collidesDiagonally(Location start, Location end, 
                                             Predicate<Block> collisionCheck) {
        Block startBlock = start.getBlock();
        Block endBlock = end.getBlock();
        if (startBlock.equals(endBlock)) {
            return false;
        }
        
        int x = endBlock.getX() - startBlock.getX();
        int y = endBlock.getY() - startBlock.getY();
        int z = endBlock.getZ() - startBlock.getZ();
        
        // Diagonal collisions can only occur where the components of movement
        // are non-zero on two or more axes:
        if (x != 0 && z != 0) {
            Block checkX = startBlock.getRelative(x, 0, 0);
            Block checkZ = startBlock.getRelative(0, 0, z);
            if (collisionCheck.test(checkX) || collisionCheck.test(checkZ)) {
                return true;
            }
        }

        if (y != 0 && (x != 0 || z != 0)) {
            Block checkXZ = startBlock.getRelative(x, 0, z);
            Block checkY = startBlock.getRelative(0, y, 0);
            return collisionCheck.test(checkXZ) || collisionCheck.test(checkY);
        }
    
        return false;
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

    /**
     * Collects all of the blocks in a cuboid and returns them.
     *
     * @param center the {@link Location} center of the cuboid.
     * @param lenX the Double length of the cuboid on the x axis.
     * @param lenY the Double length of the cuboid on the y axis.
     * @param lenZ the Double length of the cuboid on the z axis.
     * @return a {@link List} of collected blocks.
     */
    public static List<Block> collectCuboid(Location center, 
                                     double lenX, double lenY, double lenZ) {
        World world = center.getWorld();
        if (world == null) {
            return List.of();
        }

        // Start at the lowest corner so we can use plain addition to iterate
        int halfX = (int) Math.round(lenX / 2);
        int halfY = (int) Math.round(lenY / 2);
        int halfZ = (int) Math.round(lenZ / 2);
    
        int startX = center.getBlockX() - halfX;
        int endX = center.getBlockX() + halfX;
        int startY = center.getBlockY() - halfY;
        int endY = center.getBlockY() + halfY;
        int startZ = center.getBlockZ() - halfZ;
        int endZ = center.getBlockZ() + halfZ;
        
        List<Block> blocks = new ArrayList<>();
        for (int x = startX; x <= endX; ++x) {
            for (int y = startY; y <= endY; ++y) {
                for (int z = startZ; z <= endZ; ++z) {
                    blocks.add(world.getBlockAt(x, y, z));
                }
            }
        }

        return blocks;
    }

    /**
     * Collects all of the blocks in a cube and returns them.
     *
     * @param center the {@link Location} center of the cube.
     * @param len the Double side length of the cube on all axes.
     * @return a {@link List} of collected blocks.
     */
    public static List<Block> collectCube(Location center, double len) {
        return collectCuboid(center, len, len, len);
    }

    /**
     * Collects all of the blocks in a sphere and returns them.
     *
     * @param center the {@link Location} center of the sphere.
     * @param radius the Double radius of the sphere.
     * @return a {@link List} of collected blocks.
     */
    public static List<Block> collectSphere(Location center, double radius) {
        World world = center.getWorld();
        if (world == null) {
            return List.of();
        }
        
        int rad = (int) (Math.ceil(radius));
        // Keep this a double to preserve the effect of decimal values on shape
        double radSqrd =  radius * radius;

        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();

        int minX = centerX - rad;
        int maxX = centerX + rad;
        int minY = centerY - rad;
        int maxY = centerY + rad;
        int minZ = centerZ - rad;
        int maxZ = centerZ + rad;
        
        List<Block> blocks = new ArrayList<>();
        for (int x = minX; x <= maxX; ++x) {
            for (int y = minY; y <= maxY; ++y) {
                for (int z = minZ; z <= maxZ; ++z) {
                    int dx = centerX - x;
                    int dy = centerY - y;
                    int dz = centerZ - z;
                    if (dx*dx + dy*dy + dz*dz <= radSqrd) {
                        blocks.add(world.getBlockAt(x, y, z));
                    }
                }
            }
        }

        return blocks;
    }
   
    /**
     * Collects all blocks in a flat circle and returns them.
     *
     * @param center the {@link Location} center of the circle.
     * @param radius the Double radius of the circle.
     */
    public static Collection<Block> collectCircle(Location center, double radius) {
        World world = center.getWorld();
        if (world == null) {
            return List.of();
        }

        int rad = (int) (Math.round(radius));
        double radSqrd =  radius * radius;

        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();
        
        List<Block> blocks = new ArrayList<>();
        for (int x = -(rad+1); x <= rad+1; ++x) {
            for (int z = -(rad+1); z <= rad+1; ++z) {
                double distSqrd = x*x + z*z;
                if (distSqrd <= radSqrd) {
                    int bx = centerX + x;
                    int bz = centerZ + z;
                    blocks.add(world.getBlockAt(bx, centerY, bz));
                }
            }
        }

        return blocks;
    }
}
