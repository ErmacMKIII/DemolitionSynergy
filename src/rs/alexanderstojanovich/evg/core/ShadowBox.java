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

    protected static float FOV_Factor = 0.0f;

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
     * Set all bounding box vertex points to +/- infinity
     *
     * @return this pointer
     */
    public ShadowBox infinity() {
        minX = Float.POSITIVE_INFINITY;
        maxX = Float.NEGATIVE_INFINITY;

        minY = Float.POSITIVE_INFINITY;
        maxY = Float.NEGATIVE_INFINITY;

        minZ = Float.POSITIVE_INFINITY;
        maxZ = Float.NEGATIVE_INFINITY;

        return this;
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
     * Calculate projection matrix so this shadow box is used when rendering to
     * the texture
     *
     * @return projection matrix
     */
    public Matrix4f projectionMatrix() {
        // Calculate the aspect ratio of the shadow map texture
        float aspectRatio = (float) 1.0f;

        // Determine the center of the shadow box
        float centerX = (minX + maxX) / 2.0f;
        float centerY = (minY + maxY) / 2.0f;
        float centerZ = (minZ + maxZ) / 2.0f;

        // Calculate the extents of the shadow box
        float halfWidth = (maxX - minX) / 2.0f;
        float halfHeight = (maxY - minY) / 2.0f;
        float halfDepth = (maxZ - minZ) / 2.0f;

        // Construct the orthographic projection matrix
        Matrix4f projectionMatrix = new Matrix4f().orthoLH(
                (float) (centerX - halfWidth * aspectRatio), (float) (centerX + halfWidth * aspectRatio),
                (float) (centerY - halfHeight), (float) (centerY + halfHeight),
                (float) (centerZ - halfDepth), (float) (centerZ + halfDepth)
        );

        return projectionMatrix;
    }

    /**
     * Transforms shadow box to the world space (assuming is from light space)
     *
     * Light Space matrix is light view matrix
     *
     * @param lightViewMatrix light space matrix
     * @return
     */
    public ShadowBox toWorldSpace(Matrix4f lightViewMatrix) {
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

        Matrix4f lightInverseMatrix = new Matrix4f(lightViewMatrix).invert();

        for (Vector3f point : points) {
            Vector4f temp = new Vector4f();
            Vector4f vec4f = new Vector4f(point, 1.0f);
            Vector4f pos = lightInverseMatrix.transform(vec4f, temp);

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
     * Returned shadow box is in the light space
     *
     * @param shadowDistance shadow distance
     * @param aspectRatio screen aspect ratio
     * @param lightPov light (point of view) camera
     * @param outShadowBox (nullable)
     * @return new or updated shadow box
     */
    public static ShadowBox createOrUpdate(float shadowDistance, float aspectRatio, Observer lightPov, ShadowBox outShadowBox) {
        final float nearDistance = PerspectiveRenderer.NEAR_PLANE;
        final float farDistance = shadowDistance;
        Vector4f whVec4 = widthAndHeight(nearDistance, farDistance, aspectRatio);
        IList<Vector3f> points = frustumVertices(
                nearDistance,
                farDistance,
                whVec4,
                lightPov.getFront(), lightPov.getUp(), lightPov.getRight(), // front, right & up vectors from the light (and not the main actor)
                lightPov.getCamera().viewMatrix // way to free this shadow box to not being tied to light-space
        );

        if (outShadowBox == null) {
            outShadowBox = new ShadowBox();
        }
        outShadowBox.infinity();

        for (Vector3f point : points) {
            if (point.x > outShadowBox.maxX) {
                outShadowBox.maxX = point.x;
            } else if (point.x < outShadowBox.minX) {
                outShadowBox.minX = point.x;
            }

            if (point.y > outShadowBox.maxY) {
                outShadowBox.maxY = point.y;
            } else if (point.y < outShadowBox.minY) {
                outShadowBox.minY = point.y;
            }

            if (point.z > outShadowBox.maxZ) {
                outShadowBox.maxZ = point.z;
            } else if (point.z < outShadowBox.minZ) {
                outShadowBox.minZ = point.z;
            }
        }

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
     * (farWidth x farHeight) in space provided with view vectors front, up,
     * right. From light-point-of-view.
     *
     * @param nearDistance near Z
     * @param farDistance far Z
     * @param widthHeightVec4 (nearWidth x nearHeight), (farWidth x farHeight)
     * as xyzw
     *
     * @param frontVec3 front vec3
     * @param upVec3 up vec3
     * @param rightVec3 right vec3
     * @param viewMat4 view mat4
     *
     * @return The positions of the vertices of the frustum in light space.
     */
    protected static IList<Vector3f> frustumVertices(float nearDistance, float farDistance, Vector4f widthHeightVec4, Vector3f frontVec3, Vector3f upVec3, Vector3f rightVec3, Matrix4f viewMat4) {
        final IList<Vector3f> result = new GapList<>();

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

        Vector3f nearXPosYPos = toNear.fma(halfWidthNear, rightVec3, tempA).fma(halfHeightNear, upVec3, tempA);
        Vector3f nearXNegYPos = toNear.fma(-halfWidthNear, rightVec3, tempB).fma(halfHeightNear, upVec3, tempB);
        Vector3f nearXPosYNeg = toNear.fma(halfWidthNear, rightVec3, tempC).fma(-halfHeightNear, upVec3, tempC);
        Vector3f nearXNegYNeg = toNear.fma(-halfWidthNear, rightVec3, tempD).fma(-halfHeightNear, upVec3, tempD);

        Vector3f farXPosYPos = toFar.fma(halfWidthFar, rightVec3, tempA).fma(halfHeightFar, upVec3, tempA);
        Vector3f farXNegYPos = toFar.fma(-halfWidthFar, rightVec3, tempB).fma(halfHeightFar, upVec3, tempB);
        Vector3f farXPosYNeg = toFar.fma(halfWidthFar, rightVec3, tempC).fma(-halfHeightFar, upVec3, tempC);
        Vector3f farXNegYNeg = toFar.fma(-halfWidthFar, rightVec3, tempD).fma(-halfHeightFar, upVec3, tempD);

        // temp array of points
        Vector3f[] points = new Vector3f[]{
            nearXPosYPos, nearXNegYPos, nearXPosYNeg, nearXNegYNeg,
            farXPosYPos, farXNegYPos, farXPosYNeg, farXNegYNeg
        };

        Vector4f tempV4 = new Vector4f();
        for (Vector3f point : points) {
            Vector4f vec4f = new Vector4f(point, 1.0f);
            vec4f = viewMat4.transform(vec4f, tempV4); // convert to light space
            Vector3f vec3f = new Vector3f(vec4f.x, vec4f.y, vec4f.z);
            result.add(vec3f);
        }

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
        float tangent = org.joml.Math.tan(FOV_Factor * PerspectiveRenderer.FOV / 2.0f);

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

    public static float getFOV_Factor() {
        return FOV_Factor;
    }

}
