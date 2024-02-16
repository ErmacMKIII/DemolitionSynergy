/*
 * Copyright (C) 2024 Alexander Stojanovich <coas91@rocketmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package rs.alexanderstojanovich.evg.core;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.critter.Observer;

/**
 * "Represents the 3D cuboid area of the world in which objects will cast
 * shadows (basically represents the orthographic projection area for the shadow
 * render pass). It is updated each frame to optimize the area, making it as
 * small as possible (to allow for optimal shadow map resolution) while not
 * being too small to avoid objects not having shadows when they should.
 * Everything inside the cuboid area represented by this object will be rendered
 * to the shadow map in the shadow render pass. Everything outside the area
 * won't be." -Karl
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class ShadowBox {

    public static final ShadowBox ZERO = new ShadowBox().zero();

    public float minX, maxX;
    public float minY, maxY;
    public float minZ, maxZ;

    public ShadowBox(float minX, float maxX, float minY, float maxY, float minZ, float maxZ) {
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        this.minZ = minZ;
        this.maxZ = maxZ;
    }

    /**
     * Default constructor sets vertex points to min/max bounds.
     *
     * Creates a new shadow box and calculates some initial values relating to
     * the camera's view frustum, namely the width and height of the near plane
     * and (possibly adjusted) far plane.
     */
    public ShadowBox() {
        minX = Float.POSITIVE_INFINITY;
        maxX = Float.NEGATIVE_INFINITY;

        minY = Float.POSITIVE_INFINITY;
        maxY = Float.NEGATIVE_INFINITY;

        minZ = Float.POSITIVE_INFINITY;
        maxZ = Float.NEGATIVE_INFINITY;
    }

    /**
     * Set all bounding box vertex points to zero
     *
     * @return this pointer
     */
    public ShadowBox zero() {
        this.minX = 0.0f;
        this.maxX = 0.0f;
        this.minY = 0.0f;
        this.maxY = 0.0f;
        this.minZ = 0.0f;
        this.maxZ = 0.0f;

        return this;
    }

    /**
     * Transforms shadow box to the light space.
     *
     * Light Space matrix is light projection matrix x light view matrix
     *
     * @param lightSpaceMat4 light space matrix
     * @return
     */
    public ShadowBox toLightSpace(Matrix4f lightSpaceMat4) {
        Vector3f[] points = {
            new Vector3f(minX, minY, minZ),
            new Vector3f(minX, minY, maxZ),
            new Vector3f(minX, maxY, minZ),
            new Vector3f(minX, maxY, maxZ),
            new Vector3f(maxX, minY, minZ),
            new Vector3f(maxX, minY, maxZ),
            new Vector3f(maxX, maxY, minZ),
            new Vector3f(maxX, maxY, maxZ)
        };

        float minLightX = Float.POSITIVE_INFINITY;
        float minLightY = Float.POSITIVE_INFINITY;
        float minLightZ = Float.POSITIVE_INFINITY;
        float maxLightX = Float.NEGATIVE_INFINITY;
        float maxLightY = Float.NEGATIVE_INFINITY;
        float maxLightZ = Float.NEGATIVE_INFINITY;

        for (Vector3f point : points) {
            Vector4f temp = new Vector4f();
            Vector4f vec4f = new Vector4f(point, 1.0f);
            Vector4f pos = lightSpaceMat4.transform(vec4f, temp);

            // Update min and max coordinates in light space
            minLightX = Math.min(minLightX, pos.x);
            minLightY = Math.min(minLightY, pos.y);
            minLightZ = Math.min(minLightZ, pos.z);
            maxLightX = Math.max(maxLightX, pos.x);
            maxLightY = Math.max(maxLightY, pos.y);
            maxLightZ = Math.max(maxLightZ, pos.z);
        }

        // Construct and return a new shadow box in light space
        return new ShadowBox(minLightX, maxLightX, minLightY, maxLightY, minLightZ, maxLightZ);
    }

    /**
     *
     * Updates the bounds of the shadow box based on the light direction and the
     * camera's view frustum, to make sure that the box covers the smallest area
     * possible while still ensuring that everything inside the camera's view
     * (within a certain range) will cast shadows.
     *
     * Light matrix is basically the "view matrix" of the light. Can be used to
     * transform a point from world space into "light" space (i.e. changes a
     * point's coordinates from being in relation to the world's axis to being
     * in terms of the light's local axis).
     *
     *
     * @param shadowDistance shadow distance
     * @param aspectRatio screen aspect ratio
     * @param obs observer (viewing the light space). Camera from light point of
     * view - perpective.
     * @param outShadowBox (can be null)
     * @return
     */
    public static ShadowBox createOrUpdate(float shadowDistance, float aspectRatio, Observer obs, ShadowBox outShadowBox) {
        final float nearDistance = PerspectiveRenderer.NEAR_PLANE;
        final float farDistance = shadowDistance;
        Vector4f whVec4 = widthAndHeight(nearDistance, farDistance, aspectRatio);
        IList<Vector3f> points = frustumVertices(
                nearDistance,
                farDistance,
                whVec4,
                obs.getFront(), obs.getUp(), obs.getRight() // front, right & up vectors from main observer (observer or player)
        );

        ShadowBox newShadowBox = new ShadowBox();

        for (Vector3f point : points) {

            if (point.x > newShadowBox.maxX) {
                newShadowBox.maxX = point.x;
            } else if (point.x < newShadowBox.minX) {
                newShadowBox.minX = point.x;
            }

            if (point.y > newShadowBox.maxY) {
                newShadowBox.maxY = point.y;
            } else if (point.y < newShadowBox.minY) {
                newShadowBox.minY = point.y;
            }

            if (point.z > newShadowBox.maxZ) {
                newShadowBox.maxZ = point.z;
            } else if (point.z < newShadowBox.minZ) {
                newShadowBox.minZ = point.z;
            }

        }

        outShadowBox = newShadowBox;

        return outShadowBox;
    }

    /**
     * Calculates the center of the "view cuboid" in light space first, and then
     * converts this to world space using the inverse light's view matrix.
     *
     * @return The center of the "view cuboid" in world space.
     */
    public Vector3f getCenter() {
        float centerX = (minX + maxX) / 2.0f;
        float centerY = (minY + maxY) / 2.0f;
        float centerZ = (minZ + maxZ) / 2.0f;

        return new Vector3f(centerX, centerY, centerZ);
    }

    /**
     * @return The width of the "view cuboid" (orthographic projection area).
     */
    public float getWidth() {
        return maxX - minX;
    }

    /**
     * @return The height of the "view cuboid" (orthographic projection area).
     */
    public float getHeight() {
        return maxY - minY;
    }

    /**
     * @return The length of the "view cuboid" (orthographic projection area).
     */
    public float getDepth() {
        return maxZ - minZ;
    }

    /**
     * Calculates the positions of cuboid frustum (nearWidth x nearHeight),
     * (farWidth x farHeight),
     *
     * @param nearDistance near Z
     * @param farDistance far Z
     * @param widthHeightVec4 (nearWidth x nearHeight), (farWidth x farHeight)
     * as xyzw
     *
     * @param frontVec3 camera front vec3
     * @param upVec3 camera up vec3
     * @param rightVec3 camera right vec3
     *
     * @return The positions of the vertices of the frustum in light space.
     */
    protected static IList<Vector3f> frustumVertices(float nearDistance, float farDistance, Vector4f widthHeightVec4, Vector3f frontVec3, Vector3f upVec3, Vector3f rightVec3) {
        float halfWidthNear = widthHeightVec4.x / 2.0f;
        float halfWidthFar = widthHeightVec4.y / 2.0f;

        float halfHeightNear = widthHeightVec4.z / 2.0f;
        float halfHeightFar = widthHeightVec4.w / 2.0f;

        Vector3f temp1 = new Vector3f();
        Vector3f toNear = frontVec3.mul(nearDistance, temp1);
        Vector3f temp2 = new Vector3f();
        Vector3f toFar = frontVec3.mul(farDistance, temp2);

        Vector3f tempA = new Vector3f();
        Vector3f tempB = new Vector3f();
        Vector3f tempC = new Vector3f();
        Vector3f tempD = new Vector3f();

        Vector3f nearXPosYPos = toNear.add(rightVec3.mul(halfWidthNear, tempA)).add(upVec3.mul(halfHeightNear, tempA), tempA);
        Vector3f nearXNegYPos = toNear.add(rightVec3.mul(-halfWidthNear, tempB)).add(upVec3.mul(halfHeightNear, tempB), tempB);
        Vector3f nearXPosYNeg = toNear.add(rightVec3.mul(halfWidthNear, tempC)).add(upVec3.mul(-halfHeightNear, tempC), tempC);
        Vector3f nearXNegYNeg = toNear.add(rightVec3.mul(-halfWidthNear, tempD)).add(upVec3.mul(-halfHeightNear, tempD), tempD);

        Vector3f farXPosYPos = toFar.add(rightVec3.mul(halfWidthFar, tempA)).add(upVec3.mul(halfHeightFar, tempA), tempA);
        Vector3f farXNegYPos = toFar.add(rightVec3.mul(-halfWidthFar, tempB)).add(upVec3.mul(halfHeightFar, tempB), tempB);
        Vector3f farXPosYNeg = toFar.add(rightVec3.mul(halfWidthFar, tempC)).add(upVec3.mul(-halfHeightFar, tempC), tempC);
        Vector3f farXNegYNeg = toFar.add(rightVec3.mul(-halfWidthFar, tempD)).add(upVec3.mul(-halfHeightFar, tempD), tempD);

        Vector3f[] points = new Vector3f[]{
            nearXPosYPos, nearXNegYPos, nearXPosYNeg, nearXNegYNeg,
            farXPosYPos, farXNegYPos, farXPosYNeg, farXNegYNeg
        };
        IList<Vector3f> result = new GapList<>();

        return result;
    }

    /**
     * Calculates the width and height of the near and far planes of the
     * camera's view frustum. However, this doesn't have to use the "actual" far
     * plane of the view frustum. It can use a shortened view frustum if desired
     * by bringing the far-plane closer, which would increase shadow resolution
     * but means that distant objects wouldn't cast shadows.
     *
     * @param nearDistance (shadow) distance internal config
     * @param aspectRatio screen aspect ratio
     * @return width x height of near and far as vec4
     */
    private static Vector4f widthAndHeight(float nearDistance, float farDistance, float aspectRatio) {
        float tangent = org.joml.Math.tan(PerspectiveRenderer.FOV / 2.0f);

        // Calculate height of the near plane
        float heightNear = 2.0f * tangent * nearDistance;

        // Calculate width of the near plane based on aspect ratio
        float widthNear = heightNear * aspectRatio;

        // Calculate height of the far plane
        float heightFar = 2.0f * tangent * farDistance;

        // Calculate width of the far plane based on aspect ratio
        float widthFar = heightFar * aspectRatio;

        return new Vector4f(widthNear, widthFar, heightNear, heightFar);
    }

    public float getMinX() {
        return minX;
    }

    public float getMaxX() {
        return maxX;
    }

    public float getMinY() {
        return minY;
    }

    public float getMaxY() {
        return maxY;
    }

    public float getMinZ() {
        return minZ;
    }

    public float getMaxZ() {
        return maxZ;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ShadowBox{");
        sb.append("minX=").append(minX);
        sb.append(", maxX=").append(maxX);
        sb.append(", minY=").append(minY);
        sb.append(", maxY=").append(maxY);
        sb.append(", minZ=").append(minZ);
        sb.append(", maxZ=").append(maxZ);
        sb.append('}');
        return sb.toString();
    }

}
