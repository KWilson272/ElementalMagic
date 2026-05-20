package me.kwilson272.elementalmagic.api.collision;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public final class AABB implements BoundingVolume {

    public final double xMax;
    public final double yMax;
    public final double zMax;
    public final double xMin;
    public final double yMin;
    public final double zMin;

    public AABB(double xMax, double yMax, double zMax,
                double xMin, double yMin, double zMin) {
        this.xMax = xMax;
        this.yMax = yMax;
        this.zMax = zMax;
        this.xMin = xMin;
        this.yMin = yMin;
        this.zMin = zMin;
    }

    @Override
    public BoundingVolume grow(double factor) {
        return new AABB(
                xMax * factor,
                yMax * factor,
                zMax * factor,
                xMin * factor,
                yMin * factor,
                zMin * factor
        );
    }

    @Override
    public AABB getEnclosingBox() {
        return this;
    }

    @Override
    public boolean intersects(BoundingVolume other) {
        return other.testAABB(this);
    }

    @Override
    public boolean testAABB(AABB aabb) {
        return BoundingVolume.aabbIntersectsAABB(this, aabb);
    }
    @Override
    public boolean testSphere(Sphere sphere) {
        return BoundingVolume.aabbIntersectsSphere(this, sphere);
    }

    /**
     * Returns the smallest AABB containing this and the provided AABB.
     *
     * @param other the AABB to be enclosed.
     * @return a new AABB object enclosing both boxes
     */
    public AABB union(AABB other) {
        return new AABB(
                Math.max(xMax, other.xMax),
                Math.max(yMax, other.yMax),
                Math.max(zMax, other.zMax),
                Math.min(xMin, other.xMin),
                Math.min(yMin, other.yMin),
                Math.min(zMin, other.zMin)
        );
    }

    public BoundingBox toBukkit() {
        return new BoundingBox(xMax, yMax, zMax, xMin, yMin, zMin);
    }

    public static AABB at(Location center, double sideLength) {
        return at(center, sideLength, sideLength, sideLength);
    }

    public static AABB at(Location center, double xLen, double yLen, double zLen) {
        double halfX = xLen / 2;
        double halfY = yLen / 2;
        double halfZ = zLen / 2;
        return new AABB(
                center.getX() + halfX,
                center.getY() + halfY,
                center.getZ() + halfZ,
                center.getX() - halfX,
                center.getY() - halfY,
                center.getZ() - halfZ
        );
    }

    public static AABB from(Location loc1, Location loc2) {
        return from(loc1.toVector(), loc2.toVector());
    }

    public static AABB from(Vector vec1, Vector vec2) {
        return new AABB(
                Math.max(vec1.getX(), vec2.getX()),
                Math.max(vec1.getY(), vec2.getY()),
                Math.max(vec1.getZ(), vec2.getZ()),
                Math.min(vec1.getX(), vec2.getX()),
                Math.min(vec1.getY(), vec2.getY()),
                Math.min(vec1.getZ(), vec2.getZ())
        );
    }

    public static AABB fromBukkit(BoundingBox box) {
        return new AABB(
                box.getMaxX(),
                box.getMaxY(),
                box.getMaxZ(),
                box.getMinX(),
                box.getMinY(),
                box.getMinZ()
        );
    }

    public static AABB fromBlock(Block block) {
        return fromBlock(block, 1);
    }

    public static AABB fromBlock(Block block, double expansion) {
        Location loc = block.getLocation().add(0.5, 0.5, 0.5);
        return AABB.at(loc, expansion);
    }

    public static AABB empty() {
        return new AABB(0, 0, 0, 0, 0, 0);
    }
}

