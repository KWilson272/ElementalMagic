package me.kwilson272.elementalmagic.core.util;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.util.Vector;

public class Vectors {

    public static Vector getDirection(Location from, Location to) {
        double x = to.getX() - from.getX();
        double y = to.getY() - from.getY();
        double z = to.getZ() - from.getZ();
        return new Vector(x, y, z);
    }

    /**
     * Creates a unit {@link Vector} from the provided pitch and yaw. This method 
     * returns vectors aligned with the way Spigot handles pitch and yaw.
     *
     * @param pitchDeg the Double pitch in degrees.
     * @param yawDeg the Double yaw in degrees.
     * @return a unit {@code Vector}
     */
    public static Vector fromRotations(double pitchDeg, double yawDeg) {
        double pitchRad = Math.toRadians(pitchDeg);
        double yawRad = Math.toRadians(yawDeg);

        double y = Math.sin(-pitchRad);
        double xzMag = Math.cos(-pitchRad);
        double x = -Math.sin(yawRad) * xzMag;
        double z = Math.cos(yawRad) * xzMag;

        return new Vector(x, y, z);
    }

    /**
     * Creates a unit {@link Vector} from the provided horizontal and vertical
     * angles in degrees.
     *
     * @param angleHoriz the Double angle for the x-z components in degrees.
     * @param angleVecrt the Double angle for the y component in degrees.
     * @return a unit {@code Vector}
     */
    public static Vector fromDegrees(Double angleHoriz, double angleVert) {
        return fromRadians(Math.toRadians(angleHoriz), Math.toRadians(angleVert));
    }

    /**
     * Creates a unit {@link Vector} from the provided horizontal and vertical
     * angles in radians.
     *
     * @param angleHoriz the Double angle for the x-z components in radians.
     * @param angleVecrt the Double angle for the y component in radians.
     * @return a unit {@code Vector}
     */
    public static Vector fromRadians(double angleHoriz, double angleVert) {
        double y = Math.sin(angleVert);
        double xzMag = Math.cos(angleVert);
        double x = Math.cos(angleHoriz) * xzMag;
        double z = Math.sin(angleHoriz) * xzMag;
        
        return new Vector(x, y, z);
    }

    /**
     * Returns a Vector 'V2' orthogonal to the provided Vector 'V1'. The
     * returned vector will be a unit vector.
     *
     * <p> If V1 were to be considered line-of-sight, V2 will always point
     * directly to the right of V1 assuming V1 does not draw near to being
     * completely vertical. In the case that V1 is vertical, the returned
     * V2 will just be the X axis.
     *
     * @param vector the base Vector
     * @return a Vector orthogonal to the provided Vector
     */
    public static Vector getOrthogonal(Vector vector) {
        // Up is necessary for us to return a 'right' facing vec
        Vector up = new Vector(0, 1, 0);
        // Don't alter the parameter vec - spigot mutability
        Vector mutVec = vector.clone().normalize();

        // If the two inputs are too close together, cross product will just
        // return floating point noise, and normalization will be somewhat
        // 'random'. Return the x-axis to be deterministic to a point.
        if (mutVec.equals(up)) {
            return new Vector(1, 0, 0);
        }

        // This order does matter!
        return mutVec.crossProduct(up).normalize();
    }

    /**
     * Rotates the provided 'rotate' Vector around the given axis by an angle
     * specified in radians.
     *
     * @param axis the axis of rotation
     * @param rotate the Vector being rotated
     * @param radians the angle the Vector is rotated by
     *
     * @return a new rotated Vector
     */
    public static Vector rotateAroundVector(Vector axis, Vector rotate, double radians) {
        // Cross product isn't reliable if these are too close
        if (axis.equals(rotate)) {
            return rotate;
        }

        // Rotating v around k via Rodrigues' rotation formula:
        // v_rot = vcos(theta) + (k x v)sin(theta) + k(k * v)(1 - cos(theta))
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);

        Vector term1 = rotate.clone().multiply(cos);
        Vector term2 = axis.getCrossProduct(rotate).multiply(sin);
        double dot = axis.clone().dot(rotate);
        Vector term3 = axis.clone().multiply(dot).multiply(1 - cos);

        return term1.add(term2).add(term3);
    }

    /**
     * Gets count vectors in a circle parallel to the x-z plane.
     *
     * @param count the Integer number of vectors returned.
     * @return a {@link List} of vectors.
     */
    public static List<Vector> getRing(int count) {
        Vector xAxis = new Vector(1, 0, 0);
        Vector zAxis = new Vector(0, 0, 1);
        return getRing(count, xAxis, zAxis);
    }

    /**
     * Gets count vectors in a circle parallel to the plane made up by the
     * provided axes.
     *
     * @param count the Integer number of vectors returned.
     * @param axis1 one {@link Vector} axis.
     * @param axis2 the other {@code Vector} axis.
     * @return a {@link List} of vectors.
     */
    public static List<Vector> getRing(int count, Vector axis1, Vector axis2) {
        List<Vector> vectors = new ArrayList<>(count);

        double step = (2 * Math.PI) / count;
        for (int i = 0; i < count; i++) {
            double theta = i * step;
            Vector comp1 = axis1.clone().multiply(Math.sin(theta));
            Vector comp2 = axis2.clone().multiply(Math.cos(theta));
            Vector result = comp1.add(comp2);
            vectors.add(result);
        }

        return vectors;
    }
}
