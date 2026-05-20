package me.kwilson272.elementalmagic.api.collision;

public sealed interface BoundingVolume permits AABB, Sphere {

    BoundingVolume grow(double factor);

    AABB getEnclosingBox();

    boolean intersects(BoundingVolume other);

    boolean testAABB(AABB aabb);

    boolean testSphere(Sphere sphere);

    // Methods are ordered in alphabetical order such that names that come 
    // first handle the collision method with objects with names that come
    // later. This way we don't have n*n methods! It is messy, but I don't 
    // see a better way to do this given all calculations are special
    
    public static boolean aabbIntersectsAABB(AABB a, AABB b) {
        return a.xMin <= b.xMax && a.xMax >= b.xMin
            && a.yMin <= b.yMax && a.yMax >= b.yMin
            && a.zMin <= b.zMax && a.zMax >= b.zMin;
    }

    public static boolean aabbIntersectsSphere(AABB a, Sphere b) {
        // Closest point on the box to the sphere
        double x = Math.max(a.xMin, Math.min(b.centerX, a.xMax));
        double y = Math.max(a.yMin, Math.min(b.centerY, a.yMax));
        double z = Math.max(a.zMin, Math.min(b.centerZ, a.zMin));

        double distSqrd = distanceSquared(x, y, z, b.centerX, b.centerY, b.centerZ);
        return distSqrd <= b.radius * b.radius;
    }

    public static boolean sphereIntersectsSphere(Sphere a, Sphere b) {
        double distSqrd = distanceSquared(
                a.centerX, a.centerY, a.centerZ, b.centerX, b.centerY, b.centerZ);
        double maxDistSqrd = (a.radius + b.radius) * (a.radius + b.radius);
        return distSqrd <= maxDistSqrd;
    }

    // We are using this a lot with raw coordinates
    private static double distanceSquared(double x1, double y1, double z1,
                                          double x2, double y2, double z2) {
        return (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2) + (z1 - z2) * (z1 - z2);
    }
}

