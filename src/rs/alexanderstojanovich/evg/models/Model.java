/* 
 * Copyright (C) 2020 Alexander Stojanovich <coas91@rocketmail.com>
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.level.LightSources;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.util.Vector3fColors;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Model implements Comparable<Model> {

    protected String modelFileName;

    protected String texName;
    protected Texture waterTexture;

    protected float width; // X axis dimension
    protected float height; // Y axis dimension
    protected float depth; // Z axis dimension

    public Vector3f pos = new Vector3f();
    protected float scale = 1.0f; // changing scale also changes width, height and depth

    protected float rX = 0.0f;
    protected float rY = 0.0f;
    protected float rZ = 0.0f;

    protected IList<Material> materials = new GapList<>();
    protected IList<Mesh> meshes = new GapList<>();

    protected boolean solid = true; // is movement through this model possible
    // fluid models are solid whilst solid ones aren't               

    protected Matrix4f modelMatrix = new Matrix4f();

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

    public static Model readFromObjFile(String dirEntry, String fileName, String texName) {
        File extern = new File(dirEntry + fileName);
        File archive = new File(Game.DATA_ZIP);
        ZipFile zipFile = null;
        InputStream objInput = null;
        if (extern.exists()) {
            try {
                objInput = new FileInputStream(extern);
            } catch (FileNotFoundException ex) {
                DSLogger.reportFatalError(ex.getMessage(), ex);
            }
        } else if (archive.exists()) {
            try {
                zipFile = new ZipFile(archive);
                for (ZipEntry zipEntry : Collections.list(zipFile.entries())) {
                    if (zipEntry.getName().equals(dirEntry + fileName)) {
                        objInput = zipFile.getInputStream(zipEntry);
                        break;
                    }
                }
            } catch (IOException ex) {
                DSLogger.reportFatalError(ex.getMessage(), ex);
            }
        } else {
            DSLogger.reportError("Cannot find zip archive " + Game.DATA_ZIP + " or relevant ingame files!", null);
        }
        //----------------------------------------------------------------------
        if (objInput == null) {
            DSLogger.reportError("Cannot find resource " + dirEntry + fileName + "!", null);
            return null;
        }

        int texIndex = Texture.TEX_MAP.get(texName).getValue();
        int row = texIndex / Texture.GRID_SIZE_WORLD;
        int col = texIndex % Texture.GRID_SIZE_WORLD;
        final float oneOver = 1.0f / (float) Texture.GRID_SIZE_WORLD;

        Model result = new Model(fileName, texName);
        Mesh mesh = new Mesh();
        BufferedReader br = new BufferedReader(new InputStreamReader(objInput));
        List<Vector2f> uvs = new ArrayList<>();
        List<Vector3f> normals = new ArrayList<>();
        String line;
        try {
            while ((line = br.readLine()) != null) {
                String[] things = line.split(" ");
                if (things[0].equals("v")) {
                    Vector3f pos = new Vector3f(Float.parseFloat(things[1]), Float.parseFloat(things[2]), Float.parseFloat(things[3]));
                    Vertex vertex = new Vertex(pos);
                    mesh.vertices.add(vertex);
                } else if (things[0].equals("vt")) {
                    Vector2f uv = new Vector2f(Float.parseFloat(things[1]), 1.0f - Float.parseFloat(things[2]));
                    uvs.add(uv);
                } else if (things[0].equals("vn")) {
                    Vector3f normal = new Vector3f(Float.parseFloat(things[1]), Float.parseFloat(things[2]), Float.parseFloat(things[3]));
                    normals.add(normal);
                } else if (things[0].equals("f")) {
                    String[] subThings = {things[1], things[2], things[3]};
                    for (String subThing : subThings) {
                        String[] data = subThing.split("/");
                        int index = Integer.parseInt(data[0]) - 1;
                        mesh.indices.add(index);
                        Vertex vertex = mesh.vertices.get(index);
                        if (data.length >= 2 && !data[1].isEmpty()) {
                            vertex.setUv(uvs.get(Integer.parseInt(data[1]) - 1));
                            if (texIndex != -1) {
                                vertex.getUv().x = (vertex.getUv().x + row) * oneOver;
                                vertex.getUv().y = (vertex.getUv().y + col) * oneOver;
                            }
                        }
                        if (data.length >= 3 && !data[2].isEmpty()) {
                            mesh.vertices.get(index).setNormal(normals.get(Integer.parseInt(data[2]) - 1));
                        }
                    }
                }
            }
        } catch (IOException ex) {
            DSLogger.reportFatalError(ex.getMessage(), ex);
        } finally {
            try {
                objInput.close();
            } catch (IOException ex) {
                DSLogger.reportFatalError(ex.getMessage(), ex);
            }
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException ex) {
                    DSLogger.reportFatalError(ex.getMessage(), ex);
                }
            }
        }

        Material material = new Material(Texture.TEX_MAP.get(texName).getTexture());
        material.color = new Vector3f(Vector3fColors.WHITE);
        result.materials.add(material);

        result.meshes.add(mesh);
        result.calcDims();

        return result;
    }

    public void render(LightSources lightSources, ShaderProgram shaderProgram) {
        if (meshes.isEmpty() || !meshes.getFirst().buffered || materials.isEmpty() || !materials.getFirst().texture.isBuffered()) {
            return; // this is very critical!!
        }

        final Mesh mesh = meshes.getFirst();

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mesh.vbo);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, mesh.ibo);

        GL20.glEnableVertexAttribArray(0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glEnableVertexAttribArray(2);

        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, Vertex.SIZE * 4, 0); // this is for pos
        GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, Vertex.SIZE * 4, 12); // this is for normal
        GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, Vertex.SIZE * 4, 24); // this is for uv

        if (shaderProgram != null) {
            shaderProgram.bind();
            transform(shaderProgram);
            setAlpha(shaderProgram);

            lightSources.updateLightsInShader(shaderProgram);

            Texture primaryTexture = Texture.TEX_MAP.get(texName).getTexture();
            if (primaryTexture != null) { // this is primary texture
                primaryColor(shaderProgram);
                primaryTexture.bind(0, shaderProgram, "modelTexture0");
            }

            if (waterTexture != null) { // this is reflective texture
                secondaryColor(shaderProgram);
                waterTexture.bind(1, shaderProgram, "modelTexture1");
            }
        }
        GL11.glDrawElements(GL11.GL_TRIANGLES, meshes.getFirst().indices.size(), GL11.GL_UNSIGNED_INT, 0);
        Texture.unbind(0);
        Texture.unbind(1);
        ShaderProgram.unbind();

        GL20.glDisableVertexAttribArray(0);
        GL20.glDisableVertexAttribArray(1);
        GL20.glDisableVertexAttribArray(2);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    /**
     * Render multiple models old fashion way.
     *
     * @param models models to render
     * @param vbo common vbo
     * @param ibo common ibo
     * @param lightSources light sources {SUN, PLAYER, OTHER LIGHT BLOCKS etc}
     * @param shaderProgram shaderProgram for the models
     */
    public static void render(List<Model> models, int vbo, int ibo, LightSources lightSources, ShaderProgram shaderProgram) {
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);

        GL20.glEnableVertexAttribArray(0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glEnableVertexAttribArray(2);

        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, Vertex.SIZE * 4, 0); // this is for pos
        GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, Vertex.SIZE * 4, 12); // this is for normal
        GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, Vertex.SIZE * 4, 24); // this is for uv

        if (shaderProgram != null) {
            shaderProgram.bind();

            lightSources.updateLightsInShader(shaderProgram);

            for (Model model : models) {
                model.transform(shaderProgram);
                model.setAlpha(shaderProgram);

                final Mesh mesh = model.meshes.getFirst();

                Texture primaryTexture = Texture.TEX_MAP.get(model.texName).getTexture();
                if (primaryTexture != null) { // this is primary texture
                    model.primaryColor(shaderProgram);
                    primaryTexture.bind(0, shaderProgram, "modelTexture0");
                }
                GL11.glDrawElements(GL11.GL_TRIANGLES, mesh.indices.size(), GL11.GL_UNSIGNED_INT, 0);
                Texture.unbind(0);
            }
        }
        ShaderProgram.unbind();

        GL20.glDisableVertexAttribArray(0);
        GL20.glDisableVertexAttribArray(1);
        GL20.glDisableVertexAttribArray(2);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
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

    public void transform(ShaderProgram shaderProgram) {
        calcModelMatrix();
        shaderProgram.updateUniform(modelMatrix, "modelMatrix");
    }

    public void primaryColor(ShaderProgram shaderProgram) {
        shaderProgram.updateUniform(materials.getFirst().color, "modelColor0");
    }

    public void secondaryColor(ShaderProgram shaderProgram) {
        shaderProgram.updateUniform(new Vector3f(1.0f, 1.0f, 1.0f), "modelColor2");
    }

    protected void setAlpha(ShaderProgram shaderProgram) {
        shaderProgram.updateUniform(materials.getFirst().alpha, "modelAlpha");
    }

    private void calcDims() {
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
        this.materials.getFirst().alpha = (solid) ? 1.0f : 0.5f;
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

    public Vector3f getPrimaryColor() {
        return this.getMaterials().getFirst().color;
    }

    public void setPrimaryColor(Vector3f color) {
        this.materials.getFirst().color = color;
    }

    public float getPrimaryColorAlpha() {
        return this.materials.getFirst().alpha;
    }

    public void setPrimaryColorAlpha(float alpha) {
        this.materials.getFirst().alpha = alpha;
    }

    public boolean isBuffered() {
        return meshes.getFirst().buffered;
    }

    public void bufferAll() {
        meshes.forEach(m -> m.bufferAll());
    }

    public IList<Vertex> getVertices() {
        return meshes.getFirst().vertices;
    }

}
