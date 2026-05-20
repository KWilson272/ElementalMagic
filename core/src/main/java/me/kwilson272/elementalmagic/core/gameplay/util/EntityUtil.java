package me.kwilson272.elementalmagic.core.gameplay.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.collision.AABB;
import me.kwilson272.elementalmagic.api.collision.BoundingVolume;

public final class EntityUtil {

    private EntityUtil() {
        // Don't allow instantiation
    }

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
        List<Entity> entities = new ArrayList<>();
        BoundingBox box = bv.getEnclosingBox().toBukkit();
        for (Entity entity : world.getNearbyEntities(box)) {
            AABB eBox = AABB.fromBukkit(entity.getBoundingBox());
            if (eBox.intersects(bv)) {
                entities.add(entity);
            }
        }

        return entities;
    }
    
    /**
     * Gets the targted {@link Location} of a {@link LivingEntity}. This 
     * may be just outside of a target block, or the location of a targeted
     * entity.
     *
     * @param entity the {@code LivingEntity} targeting.
     * @param range the Double range of target selection.
     * @return the targeted {@code Location} 
     */
    public static Location getTarget(LivingEntity entity, double range) {
        return getTarget(entity, range, 1.25);
    }

    /**
     * Gets the targted {@link Location} of a {@link LivingEntity}. This 
     * may be just outside of a target block, or the location of a targeted
     * entity.
     *
     * @param entity the {@code LivingEntity} targeting.
     * @param range the Double range of target selection.
     * @param raySize the Double thickness of the ray used for collisions.
     * @return the targeted {@code Location} 
     */
    public static Location getTarget(LivingEntity entity, double range, 
                                                        double raySize) {
        World world = entity.getWorld();
        Location start = entity.getEyeLocation();
        Vector direction = start.getDirection();

        RayTraceResult result = world.rayTrace(
                start,
                direction,
                range,
                FluidCollisionMode.NEVER,
                true,
                raySize,
                e -> !e.equals(entity) && e instanceof LivingEntity 
                    && ElementalMagicApi.effectHandler().canAffect(entity)
        );

        if (result == null) {
            return start.add(direction.multiply(range));
        }

        Vector hitPosition = result.getHitPosition();
        if (result.getHitBlock() != null) {
            // Ensure we do not return a location inside a block
            hitPosition.add(direction.multiply(-0.2));
        }

        return hitPosition.toLocation(world);
    }
}

