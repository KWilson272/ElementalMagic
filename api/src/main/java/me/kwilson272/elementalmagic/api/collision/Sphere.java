package me.kwilson272.elementalmagic.api.collision;

import org.bukkit.Location;

public final class Sphere implements BoundingVolume {

    public final double centerX;
    public final double centerY;
    public final double centerZ;
    public final double radius;

    public Sphere(double centerX, double centerY, double centerZ, double radius) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.radius = radius;
    }

    @Override
    public BoundingVolume grow(double factor) {
        return new Sphere(
                centerX,
                centerY,
                centerZ,
                radius * factor
        );
    }

    @Override
    public AABB getEnclosingBox() {
        return new AABB(
                centerX + radius,
                centerY + radius,
                centerZ + radius,
                centerX - radius,
                centerY - radius,
                centerZ - radius
        );
    }

    @Override
    public boolean intersects(BoundingVolume other) {
        return other.testSphere(this);
    }

    @Override
    public boolean testAABB(AABB aabb) {
        return BoundingVolume.aabbIntersectsSphere(aabb, this);
    }

    @Override
    public boolean testSphere(Sphere sphere) {
        return BoundingVolume.sphereIntersectsSphere(this, sphere);
    }

    /**
     * Creates a Sphere around the given Location with the provided radius.
     *
     * @param center the center of the Sphere
     * @param radius the radius of the Sphere
     * @return a new Sphere object
     */
    public static Sphere at(Location center, double radius) {
        return new Sphere(center.getX(), center.getY(), center.getZ(), radius);
    }
}
