/* 
 * Copyright (C) 2020 Aleksandar Stojanovic <coas91@rocketmail.com>
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
package rs.alexanderstojanovich.evg.models;

import java.util.List;
import java.util.Objects;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL32;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.light.LightSources;
import rs.alexanderstojanovich.evg.main.GameTime;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;

/**
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class Model implements Renderable, Comparable<Model> {

    protected String modelFileName;

    public String texName;
    protected Texture waterTexture;

    protected float width; // X axis dimension
    protected float height; // Y axis dimension
    protected float depth; // Z axis dimension

    public Vector3f pos = new Vector3f();
    protected float scale = 1.0f; // changing scale also changes width, height and depth

    protected float rX = 0.0f;
    protected float rY = 0.0f;
    protected float rZ = 0.0f;

    public final IList<Material> materials = new GapList<>();
    public final IList<Mesh> meshes = new GapList<>();

    protected boolean solid = true; // is movement through this model possible
    // fluid models are solid whilst solid ones aren't               

    protected Matrix4f modelMatrix = new Matrix4f();

    /**
     * Placeholder for empty model. Use it instead of null.
     */
    public static final Model MODEL_NONE = new Model("NONE", "EMPTY");

    public Model(String modelFileName, String texName) {
        this.modelFileName = modelFileName;
        this.texName = texName;
    }

    public Model(String modelFileName, String texName, Vector3f pos, boolean solid) {
        this.modelFileName = modelFileName;
        this.texName = texName;
        this.pos = pos;
        this.solid = solid;
    }

    public Model(Model other) {
        this.modelFileName = other.modelFileName;
        this.texName = other.texName;
        this.pos = new Vector3f(other.pos);

        this.scale = other.scale;
        this.width = other.width;
        this.height = other.height;
        this.depth = other.depth;

        this.rX = other.rX;
        this.rY = other.rY;
        this.rZ = other.rZ;

        other.materials.forEach(mat -> {
            Material copy = new Material(mat.texture);
            copy.color = new Vector4f(mat.color);

            this.materials.add(copy);
        });

        other.meshes.forEach(mesh -> {
            Mesh copy = new Mesh();
            mesh.vertices.forEach(mv -> {
                copy.vertices.add(new Vertex(new Vector3f(mv.getPos()), new Vector3f(mv.getNormal()), new Vector2f(mv.getUv())));
            });
            copy.indices.addAll(mesh.indices);

            this.meshes.add(mesh);
        });

        this.solid = other.solid;

        this.modelMatrix = new Matrix4f(other.modelMatrix);
    }

    protected boolean safeCheck = false;

    /**
     * Render model. (Draw all meshes)
     *
     * @param lightSources light sources {SUN, PLAYER_WEAPONS, OTHER LIGHT,
     * BLOCKS etc}
     * @param shaderProgram shaderProgram for the models
     */
    @Override
    public void render(LightSources lightSources, ShaderProgram shaderProgram) {
        if (!isBuffered() || meshes.isEmpty() || materials.isEmpty() || !materials.getFirst().texture.isBuffered()) {
            return; // Skip rendering if not ready
        }

        // Enable vertex attribute arrays
        GL20.glEnableVertexAttribArray(0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glEnableVertexAttribArray(2);

        for (Mesh mesh : meshes) {
            if (shaderProgram != null) {
                shaderProgram.bind();

                // Bind attributes
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mesh.vbo);
                GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, Vertex.SIZE * 4, 0); // this is for pos
                GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, Vertex.SIZE * 4, 12); // this is for normal
                GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, Vertex.SIZE * 4, 24); // this is for uv
                shaderProgram.bindAttribute(0, "pos");
                shaderProgram.bindAttribute(1, "normal");
                shaderProgram.bindAttribute(2, "uv");

                // Update uniforms
                transform(shaderProgram);
                shaderProgram.updateUniform((float) GameTime.Now().getTime(), "gameTime");
                lightSources.updateLightsInShaderIfModified(shaderProgram);

                // Bind textures
                Texture primaryTexture = Texture.getOrDefault(texName);
                if (primaryTexture != null) {
                    primaryColor(shaderProgram);
                    primaryTexture.bind(0, shaderProgram, "modelTexture0");
                }

                if (waterTexture != null) {
                    secondaryColor(shaderProgram);
                    waterTexture.bind(1, shaderProgram, "modelTexture1");
                }

                // Draw the mesh
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, mesh.ibo);
                GL11.glDrawElements(GL11.GL_TRIANGLES, mesh.indices.size(), GL11.GL_UNSIGNED_INT, 0);
            }
            Texture.unbind(0);
            Texture.unbind(1);
            ShaderProgram.unbind();
        }

        // Clean up
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
        GL20.glDisableVertexAttribArray(0);
        GL20.glDisableVertexAttribArray(1);
        GL20.glDisableVertexAttribArray(2);
    }

    /**
     * Render model contour. (Draw all meshes)
     *
     * @param lightSources light sources {SUN, PLAYER_WEAPONS, OTHER LIGHT,
     * BLOCKS etc}
     * @param shaderProgram shaderProgram for the models
     */
    @Override
    public void renderContour(LightSources lightSources, ShaderProgram shaderProgram) {
        if (!isBuffered() || meshes.isEmpty() || materials.isEmpty() || !materials.getFirst().texture.isBuffered()) {
            return; // Skip rendering if not ready
        }

        // Enable vertex attribute arrays
        GL20.glEnableVertexAttribArray(0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glEnableVertexAttribArray(2);

        for (Mesh mesh : meshes) {
            if (shaderProgram != null) {
                shaderProgram.bind();

                // Bind attributes
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mesh.vbo);
                GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, Vertex.SIZE * 4, 0); // this is for pos
                GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, Vertex.SIZE * 4, 12); // this is for normal
                GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, Vertex.SIZE * 4, 24); // this is for uv
                shaderProgram.bindAttribute(0, "pos");
                shaderProgram.bindAttribute(1, "normal");
                shaderProgram.bindAttribute(2, "uv");

                // Update uniforms
                shaderProgram.updateUniform(1.0f / (float) Texture.TEX_SIZE, "unit");
                shaderProgram.updateUniform((float) GameTime.Now().getTime(), "gameTime");
                transform(shaderProgram);
                lightSources.updateLightsInShaderIfModified(shaderProgram);

                // Bind textures
                Texture primaryTexture = Texture.getOrDefault(texName);
                if (primaryTexture != null) {
                    primaryColor(shaderProgram);
                    primaryTexture.bind(0, shaderProgram, "modelTexture0");
                }

                if (waterTexture != null) {
                    secondaryColor(shaderProgram);
                    waterTexture.bind(1, shaderProgram, "modelTexture1");
                }

                // Draw the mesh
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, mesh.ibo);
                GL11.glDrawElements(GL11.GL_TRIANGLES, mesh.indices.size(), GL11.GL_UNSIGNED_INT, 0);
            }

            Texture.unbind(0);
            Texture.unbind(1);
            ShaderProgram.unbind();
        }

        // Clean up
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
        GL20.glDisableVertexAttribArray(0);
        GL20.glDisableVertexAttribArray(1);
        GL20.glDisableVertexAttribArray(2);
    }

    /**
     * Render multiple models old fashion way.
     *
     * @param models models to render
     * @param vbo common vbo
     * @param ibo common ibo
     * @param lightSources light sources {SUN, PLAYER_WEAPONS, OTHER LIGHT,
     * BLOCKS etc}
     * @param shaderProgram shaderProgram for the models
     */
    public static void render(List<Model> models, int vbo, int ibo, LightSources lightSources, ShaderProgram shaderProgram) {
        // Bind common buffers once
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);

        // Enable vertex attribute arrays
        GL20.glEnableVertexAttribArray(0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glEnableVertexAttribArray(2);

        // Setup vertex attribute pointers (these are the same for all models)
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, Vertex.SIZE * 4, 0); // pos
        GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, Vertex.SIZE * 4, 12); // normal
        GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, Vertex.SIZE * 4, 24); // uv

        if (shaderProgram != null) {
            shaderProgram.bind();

            // Bind attributes (only once, outside the loop)
            shaderProgram.bindAttribute(0, "pos");
            shaderProgram.bindAttribute(1, "normal");
            shaderProgram.bindAttribute(2, "uv");

            // Update game time uniform (only once, same for all models)
            shaderProgram.updateUniform((float) GameTime.Now().getTime(), "gameTime");

            // Update lights in shader (only once if not modified)
            lightSources.updateLightsInShaderIfModified(shaderProgram);

            // Track offset in the index buffer for each model
            int indexOffset = 0;

            for (Model model : models) {
                // Skip models that are not ready for rendering
                if (model.meshes.isEmpty() || model.safeCheck || model.materials.isEmpty()
                        || !model.materials.getFirst().texture.isBuffered()) {
                    continue;
                }

                final Mesh mesh = model.meshes.getFirst();

                // Update model-specific transformation matrix
                model.transform(shaderProgram);

                // Bind primary texture
                Texture primaryTexture = Texture.getOrDefault(model.texName);
                if (primaryTexture != null) {
                    model.primaryColor(shaderProgram);
                    primaryTexture.bind(0, shaderProgram, "modelTexture0");
                }

                // Bind secondary texture (water) if available
                if (model.waterTexture != null) {
                    model.secondaryColor(shaderProgram);
                    model.waterTexture.bind(1, shaderProgram, "modelTexture1");
                }

                // Draw the model with correct offset in index buffer
                GL11.glDrawElements(GL11.GL_TRIANGLES, mesh.indices.size(),
                        GL11.GL_UNSIGNED_INT, (long) indexOffset * Integer.BYTES);

                // Unbind textures after drawing
                Texture.unbind(0);
                Texture.unbind(1);

                // Advance offset for next model (assuming models are tightly packed in buffers)
                indexOffset += mesh.indices.size();
            }

            // Unbind element array buffer
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
        }

        // Cleanup: unbind shader and buffers
        ShaderProgram.unbind();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        // Disable vertex attribute arrays
        GL20.glDisableVertexAttribArray(0);
        GL20.glDisableVertexAttribArray(1);
        GL20.glDisableVertexAttribArray(2);
    }

    //    @Deprecated
//    public void release() {
//        if (buffered) {
//            GL15.glDeleteBuffers(vbo);
//            GL15.glDeleteBuffers(ibo);
//        }
//        buffered = false;
//    }
    public Matrix4f calcModelMatrix() {
        Matrix4f translationMatrix = new Matrix4f().setTranslation(pos);
        Matrix4f rotationMatrix = new Matrix4f().setRotationXYZ(rX, rY, rZ);
        Matrix4f scaleMatrix = new Matrix4f().scale(scale);

        Matrix4f temp = new Matrix4f();
        modelMatrix = translationMatrix.mul(rotationMatrix.mul(scaleMatrix, temp), temp);

        return modelMatrix;
    }

    public void setRotationXYZ(Vector3f viewDir) {
        float lightYaw = org.joml.Math.atan2(-viewDir.z, viewDir.x);
        float lightPitch = -org.joml.Math.atan2(viewDir.y, org.joml.Math.sqrt(viewDir.x * viewDir.x + viewDir.z * viewDir.z));
        this.calcModelMatrix();
    }

    public void transform(ShaderProgram shaderProgram) {
        calcModelMatrix();
        shaderProgram.updateUniform(modelMatrix, "modelMatrix");
    }

    public void primaryColor(ShaderProgram shaderProgram) {
        shaderProgram.updateUniform(getPrimaryRGBAColor(), "modelColor0");
    }

    public void secondaryColor(ShaderProgram shaderProgram) {
        shaderProgram.updateUniform(new Vector4f(1.0f, 1.0f, 1.0f, 1.0f), "modelColor1");
    }

//    public void lightColor(ShaderProgram shaderProgram) {
//        shaderProgram.updateUniform(this.getMapLightColor(), "lightColor");
//    }
    public void calcDims() {
        final Mesh mesh = meshes.getFirst();
        Vector3f vect = mesh.vertices.get(0).getPos();
        float xMin = vect.x;
        float yMin = vect.y;
        float zMin = vect.z;

        float xMax = vect.x;
        float yMax = vect.y;
        float zMax = vect.z;

        for (int i = 1; i < mesh.vertices.size(); i++) {
            vect = mesh.vertices.get(i).getPos();
            xMin = Math.min(xMin, vect.x);
            yMin = Math.min(yMin, vect.y);
            zMin = Math.min(zMin, vect.z);

            xMax = Math.max(xMax, vect.x);
            yMax = Math.max(yMax, vect.y);
            zMax = Math.max(zMax, vect.z);
        }

        width = Math.abs(xMax - xMin) * scale;
        height = Math.abs(yMax - yMin) * scale;
        depth = Math.abs(zMax - zMin) * scale;
    }

    public boolean containsInsideExactly(Vector3f x) {
        boolean ints = false;
        boolean boolX = x.x > pos.x - width / 2.0f && x.x < pos.x + width / 2.0f;
        boolean boolY = x.y > pos.y - height / 2.0f && x.y < pos.y + height / 2.0f;
        boolean boolZ = x.z > pos.z - depth / 2.0f && x.z < pos.z + depth / 2.0f;
        ints = boolX && boolY && boolZ;
        return ints;
    }

    public static boolean containsInsideExactly(Vector3f modelPos, float modelWidth, float modelHeight, float modelDepth, Vector3f x) {
        boolean ints = false;
        boolean boolX = x.x > modelPos.x - modelWidth / 2.0f && x.x < modelPos.x + modelWidth / 2.0f;
        boolean boolY = x.y > modelPos.y - modelHeight / 2.0f && x.y < modelPos.y + modelHeight / 2.0f;
        boolean boolZ = x.z > modelPos.z - modelDepth / 2.0f && x.z < modelPos.z + modelDepth / 2.0f;
        ints = boolX && boolY && boolZ;
        return ints;
    }

    public static boolean containsInsideEqually(Vector3f modelPos, float modelWidth, float modelHeight, float modelDepth, Vector3f x) {
        boolean ints = false;
        boolean boolX = x.x >= modelPos.x - modelWidth / 2.0f && x.x <= modelPos.x + modelWidth / 2.0f;
        boolean boolY = x.y >= modelPos.y - modelHeight / 2.0f && x.y <= modelPos.y + modelHeight / 2.0f;
        boolean boolZ = x.z >= modelPos.z - modelDepth / 2.0f && x.z <= modelPos.z + modelDepth / 2.0f;
        ints = boolX && boolY && boolZ;
        return ints;
    }

    public boolean containsInsideEqually(Vector3f x) {
        boolean ints = false;
        boolean boolX = x.x >= pos.x - width / 2.0f && x.x <= pos.x + width / 2.0f;
        boolean boolY = x.y >= pos.y - height / 2.0f && x.y <= pos.y + height / 2.0f;
        boolean boolZ = x.z >= pos.z - depth / 2.0f && x.z <= pos.z + depth / 2.0f;
        ints = boolX && boolY && boolZ;
        return ints;
    }

    public boolean containsOnXZEqually(Vector3f x) {
        boolean ints = false;
        boolean boolX = x.x >= pos.x - height / 2.0f && x.x <= pos.x + height / 2.0f;
        boolean boolY = x.y >= pos.y - height / 2.0f && x.y <= pos.y + height / 2.0f;
        boolean boolZ = x.z >= pos.z - height / 2.0f && x.z <= pos.z + height / 2.0f;
        ints = boolX && boolY && boolZ;
        return ints;
    }

    public boolean containsOnXZExactly(Vector3f x) {
        boolean ints = false;
        boolean boolX = x.x > pos.x - height / 2.0f && x.x < pos.x + height / 2.0f;
        boolean boolY = x.y > pos.y - height / 2.0f && x.y < pos.y + height / 2.0f;
        boolean boolZ = x.z > pos.z - height / 2.0f && x.z < pos.z + height / 2.0f;
        ints = boolX && boolY && boolZ;
        return ints;
    }

    public static boolean containsOnXZExactly(Vector3f modelPos, float modelHeight, Vector3f x) {
        boolean ints = false;
        boolean boolX = x.x > modelPos.x - modelHeight / 2.0f && x.x < modelPos.x + modelHeight / 2.0f;
        boolean boolY = x.y > modelPos.y - modelHeight / 2.0f && x.y < modelPos.y + modelHeight / 2.0f;
        boolean boolZ = x.z > modelPos.z - modelHeight / 2.0f && x.z < modelPos.z + modelHeight / 2.0f;
        ints = boolX && boolY && boolZ;
        return ints;
    }

    public static boolean containsOnXZEqually(Vector3f modelPos, float modelHeight, Vector3f x) {
        boolean ints = false;
        boolean boolX = x.x >= modelPos.x - modelHeight / 2.0f && x.x <= modelPos.x + modelHeight / 2.0f;
        boolean boolY = x.y >= modelPos.y - modelHeight / 2.0f && x.y <= modelPos.y + modelHeight / 2.0f;
        boolean boolZ = x.z >= modelPos.z - modelHeight / 2.0f && x.z <= modelPos.z + modelHeight / 2.0f;
        ints = boolX && boolY && boolZ;
        return ints;
    }

    public boolean intersectsExactly(Model model) {
        boolean coll = false;
        boolean boolX = this.pos.x - this.width / 2.0f < model.pos.x + model.width / 2.0f
                && this.pos.x + this.width / 2.0f > model.pos.x - model.width / 2.0f;
        boolean boolY = this.pos.y - this.height / 2.0f < model.pos.y + model.height / 2.0f
                && this.pos.y + this.height / 2.0f > model.pos.y - model.height / 2.0f;
        boolean boolZ = this.pos.z - this.depth / 2.0f < model.pos.z + model.depth / 2.0f
                && this.pos.z + this.depth / 2.0f > model.pos.z - model.depth / 2.0f;
        coll = boolX && boolY && boolZ;
        return coll;
    }

    public boolean intersectsExactly(Vector3f modelPos, float modelWidth, float modelHeight, float modelDepth) {
        boolean coll = false;
        boolean boolX = this.pos.x - this.width / 2.0f < modelPos.x + modelWidth / 2.0f
                && this.pos.x + this.width / 2.0f > modelPos.x - modelWidth / 2.0f;
        boolean boolY = this.pos.y - this.height / 2.0f < modelPos.y + modelHeight / 2.0f
                && this.pos.y + this.height / 2.0f > modelPos.y - modelHeight / 2.0f;
        boolean boolZ = this.pos.z - this.depth / 2.0f < modelPos.z + modelDepth / 2.0f
                && this.pos.z + this.depth / 2.0f > modelPos.z - modelDepth / 2.0f;
        coll = boolX && boolY && boolZ;
        return coll;
    }

    public static boolean intersectsExactly(Vector3f modelAPos, float modelAWidth, float modelAHeight, float modelADepth,
            Vector3f modelBPos, float modelBWidth, float modelBHeight, float modelBDepth) {
        boolean coll = false;
        boolean boolX = modelAPos.x - modelAWidth / 2.0f < modelBPos.x + modelBWidth / 2.0f
                && modelAPos.x + modelAWidth / 2.0f > modelBPos.x - modelBWidth / 2.0f;
        boolean boolY = modelAPos.y - modelAHeight / 2.0f < modelBPos.y + modelBHeight / 2.0f
                && modelAPos.y + modelAHeight / 2.0f > modelBPos.y - modelBHeight / 2.0f;
        boolean boolZ = modelAPos.z - modelBDepth / 2.0f < modelBPos.z + modelBDepth / 2.0f
                && modelAPos.z + modelBDepth / 2.0f > modelBPos.z - modelBDepth / 2.0f;
        coll = boolX && boolY && boolZ;
        return coll;
    }

    public boolean intersectsEqually(Model model) {
        boolean coll = false;
        boolean boolX = this.pos.x - this.width / 2.0f <= model.pos.x + model.width / 2.0f
                && this.pos.x + this.width / 2.0f >= model.pos.x - model.width / 2.0f;
        boolean boolY = this.pos.y - this.height / 2.0f <= model.pos.y + model.height / 2.0f
                && this.pos.y + this.height / 2.0f >= model.pos.y - model.height / 2.0f;
        boolean boolZ = this.pos.z - this.depth / 2.0f <= model.pos.z + model.depth / 2.0f
                && this.pos.z + this.depth / 2.0f >= model.pos.z - model.depth / 2.0f;
        coll = boolX && boolY && boolZ;
        return coll;
    }

    public boolean intersectsEqually(Vector3f modelPos, float modelWidth, float modelHeight, float modelDepth) {
        boolean coll = false;
        boolean boolX = this.pos.x - this.width / 2.0f <= modelPos.x + modelWidth / 2.0f
                && this.pos.x + this.width / 2.0f >= modelPos.x - modelWidth / 2.0f;
        boolean boolY = this.pos.y - this.height / 2.0f <= modelPos.y + modelHeight / 2.0f
                && this.pos.y + this.height / 2.0f >= modelPos.y - modelHeight / 2.0f;
        boolean boolZ = this.pos.z - this.depth / 2.0f <= modelPos.z + modelDepth / 2.0f
                && this.pos.z + this.depth / 2.0f >= modelPos.z - modelDepth / 2.0f;
        coll = boolX && boolY && boolZ;
        return coll;
    }

    public static boolean intersectsEqually(Vector3f modelAPos, float modelAWidth, float modelAHeight, float modelADepth,
            Vector3f modelBPos, float modelBWidth, float modelBHeight, float modelBDepth) {
        boolean coll = false;
        boolean boolX = modelAPos.x - modelAWidth / 2.0f <= modelBPos.x + modelBWidth / 2.0f
                && modelAPos.x + modelAWidth / 2.0f >= modelBPos.x - modelBWidth / 2.0f;
        boolean boolY = modelAPos.y - modelAHeight / 2.0f <= modelBPos.y + modelBHeight / 2.0f
                && modelAPos.y + modelAHeight / 2.0f >= modelBPos.y - modelBHeight / 2.0f;
        boolean boolZ = modelAPos.z - modelADepth / 2.0f <= modelBPos.z + modelBDepth / 2.0f
                && modelAPos.z + modelADepth / 2.0f >= modelBPos.z - modelBDepth / 2.0f;
        coll = boolX && boolY && boolZ;
        return coll;
    }

    public boolean intersectsRay(Vector3f l, Vector3f l0) {
        boolean ints = false; // l is direction and l0 is the point
        for (Vertex vertex : meshes.getFirst().vertices) {
            Vector3f temp = new Vector3f();
            Vector3f x0 = vertex.getPos().add(pos, temp); // point on the plane translated
            Vector3f n = vertex.getNormal(); // normal of the plane
            if (l.dot(n) != 0.0f) {
                float d = x0.sub(l0).dot(n) / l.dot(n);
                Vector3f x = l.mul(d, temp).add(l0, temp);
                if (containsInsideEqually(x)) {
                    ints = true;
                    break;
                }
            }
        }
        return ints;
    }

    @Override
    public int compareTo(Model model) {
        return Float.compare(this.getPos().y, model.getPos().y);
    }

    public void calcDimsPub() {
        calcDims();
        calcModelMatrix();
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 89 * hash + Objects.hashCode(this.modelFileName);
        hash = 89 * hash + Objects.hashCode(this.texName);
        hash = 89 * hash + Float.floatToIntBits(this.width);
        hash = 89 * hash + Float.floatToIntBits(this.height);
        hash = 89 * hash + Float.floatToIntBits(this.depth);
        hash = 89 * hash + Objects.hashCode(this.pos);
        hash = 89 * hash + Float.floatToIntBits(this.scale);
        hash = 89 * hash + Objects.hashCode(this.materials);
        hash = 89 * hash + Objects.hashCode(this.meshes);
        hash = 89 * hash + (this.solid ? 1 : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Model other = (Model) obj;
        if (Float.floatToIntBits(this.width) != Float.floatToIntBits(other.width)) {
            return false;
        }
        if (Float.floatToIntBits(this.height) != Float.floatToIntBits(other.height)) {
            return false;
        }
        if (Float.floatToIntBits(this.depth) != Float.floatToIntBits(other.depth)) {
            return false;
        }
        if (Float.floatToIntBits(this.scale) != Float.floatToIntBits(other.scale)) {
            return false;
        }
        if (this.solid != other.solid) {
            return false;
        }
        if (!Objects.equals(this.modelFileName, other.modelFileName)) {
            return false;
        }
        if (!Objects.equals(this.texName, other.texName)) {
            return false;
        }
        if (!Objects.equals(this.pos, other.pos)) {
            return false;
        }
        if (!Objects.equals(this.materials, other.materials)) {
            return false;
        }
        return Objects.equals(this.meshes, other.meshes);
    }

    @Override
    public String toString() {
        return "Model{" + "modelFileName=" + modelFileName + ", texName=" + texName + ", width=" + width + ", height=" + height + ", depth=" + depth + ", pos=" + pos + ", scale=" + scale + ", materials=" + materials + ", meshes=" + meshes + ", solid=" + solid + '}';
    }

    public float getSurfaceY() {
        return (this.pos.y + this.height / 2.0f);
    }

    public static float getSurfaceY(Vector3f modelPos, float modelHeight) {
        return (modelPos.y + modelHeight / 2.0f);
    }

    public float getBottomY() {
        return (this.pos.y - this.height / 2.0f);
    }

    public static float getBottomY(Vector3f modelPos, float modelHeight) {
        return (modelPos.y - modelHeight / 2.0f);
    }

    public String getModelFileName() {
        return modelFileName;
    }

    public Texture getWaterTexture() {
        return waterTexture;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public float getDepth() {
        return depth;
    }

    public Vector3f getPos() {
        return pos;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
        this.width *= scale;
        this.height *= scale;
        this.depth *= scale;
    }

    public float getrX() {
        return rX;
    }

    public void setrX(float rX) {
        this.rX = rX;
    }

    public float getrY() {
        return rY;
    }

    public void setrY(float rY) {
        this.rY = rY;
    }

    public float getrZ() {
        return rZ;
    }

    public void setrZ(float rZ) {
        this.rZ = rZ;
    }

    public boolean isSolid() {
        return solid;
    }

    public void setSolid(boolean solid) {
        this.solid = solid;
        this.materials.getFirst().color.w = (solid) ? 1.0f : 0.5f;
    }

    public void setPos(Vector3f pos) {
        this.pos = pos;
    }

    public String getTexName() {
        return texName;
    }

    public void setTexName(String texName) {
        this.texName = texName;
    }

    public void setTexNameWithDeepCopy(String texName) {
        this.texName = texName;
        // copy uvs from atlas
        Block.deepCopyTo(this.meshes.getFirst(), texName);
        unbuffer();
    }

    public void setWaterTexture(Texture waterTexture) {
        this.waterTexture = waterTexture;
    }

    public Matrix4f getModelMatrix() {
        return modelMatrix;
    }

    public IList<Material> getMaterials() {
        return materials;
    }

    public IList<Mesh> getMeshes() {
        return meshes;
    }

    public Vector3f getPrimaryRGBColor() {
        return new Vector3f(this.materials.getFirst().color.x, this.materials.getFirst().color.y, this.materials.getFirst().color.z);
    }

    public Vector4f getPrimaryRGBAColor() {
        return this.materials.getFirst().color;
    }

    /**
     * Get light color from material.
     *
     * @return
     */
//    public Vector4f getMapLightColor() {
//        return this.materials.getFirst().getLightColor();
//    }
//    public Vector4f getModelColor() {
//        Vector4f prim = this.materials.getFirst().color;
//        Vector4f light = this.materials.getFirst().getLightColor();
//        Vector4f temp = new Vector4f();
//
//        return prim.mul(light, temp);
//    }
    public void setPrimaryRGBColor(Vector3f color) {
        this.materials.getFirst().color = new Vector4f(color, 1.0f);
    }

    public void setPrimaryRGBAColor(Vector4f color) {
        this.materials.getFirst().color = color;
    }

    public void setPrimaryRGBAColor(Vector3f color, float alpha) {
        this.materials.getFirst().color = new Vector4f(color, alpha);
    }

    public float getPrimaryColorAlpha() {
        return this.materials.getFirst().color.w;
    }

    public void setPrimaryColorAlpha(float alpha) {
        this.materials.getFirst().color.w = alpha;
    }

    /**
     * Equivalent of safe check
     *
     * @return
     */
    public boolean isBuffered() {
        return safeCheck;
    }

    /**
     * Set to true if this model is allowed for rendering. Otherwise false.
     */
    protected void setSafeCheck() {
        safeCheck = true;
        meshes.forEach(m -> safeCheck &= m.buffered);
    }

    /*
    * Is this model allowed for rendering
     */
    public boolean getSafeCheck() {
        return safeCheck;
    }

    public void unbuffer() {
        meshes.forEach(m -> m.buffered = false);
        setSafeCheck();
    }

    public void bufferAll() {
        meshes.forEach(m -> m.bufferAll());
        setSafeCheck();
    }

    /**
     * Get vertices of the model (it's mesh).
     *
     * @return model vertices
     */
    public IList<Vertex> getVertices() {
        return meshes.getFirst().vertices;
    }

    /**
     * Get indices of the model (it's mesh index of vertices order).
     *
     * @return model vertices
     */
    public IList<Integer> getIndices() {
        return meshes.getFirst().indices;
    }

    public boolean isSafeCheck() {
        return safeCheck;
    }

}
