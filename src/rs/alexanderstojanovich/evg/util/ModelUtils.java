/*
 * Copyright (C) 2023 coas9
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
package rs.alexanderstojanovich.evg.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIColor4D;
import org.lwjgl.assimp.AIFace;
import org.lwjgl.assimp.AIMaterial;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AIVector3D;
import org.lwjgl.assimp.Assimp;
import org.lwjgl.system.MemoryUtil;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.chunk.Chunk;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.models.Material;
import rs.alexanderstojanovich.evg.models.Mesh;
import rs.alexanderstojanovich.evg.models.Model;
import rs.alexanderstojanovich.evg.models.Vertex;
import rs.alexanderstojanovich.evg.texture.Texture;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class ModelUtils {

    /**
     * Read the obj and constructs the mode. Texture is assigned from the cache
     * by tex name.
     *
     * @param dirEntry directory entry for the model & texture
     * @param fileName obj filename
     * @param texName texName (in the cache)
     * @return new Model
     */
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

        Model result = new Model(fileName, texName);
        Mesh mesh = new Mesh();
        BufferedReader br = new BufferedReader(new InputStreamReader(objInput));
        IList<Vector2f> uvs = new GapList<>();
        IList<Vector3f> normals = new GapList<>();
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
//                            if (texIndex != -1) {
//                                vertex.getUv().x = (vertex.getUv().x + row) * oneOver;
//                                vertex.getUv().y = (vertex.getUv().y + col) * oneOver;
//                            }
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

        Material material = new Material(Texture.getOrDefault(texName));
        material.setColor(new Vector4f(GlobalColors.WHITE_RGBA));
        result.materials.add(material);

        result.meshes.add(mesh);
        result.calcDimsPub();

        return result;
    }

    /**
     * Read the obj and constructs the mode.Texture is assigned from the cache
     * by tex name.
     *
     * @param dirEntry directory entry for the model & texture
     * @param fileName obj filename
     * @param texName texName (in the cache)
     * @param gridSize grid size of texture atlas (non-zero, positive number)
     * @param reverseFaces reverse face (for face culling)
     * @return new Model
     */
    public static Model readFromObjFile(String dirEntry, String fileName, String texName, int gridSize, boolean reverseFaces) {
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

        int texIndex = Texture.getOrDefaultIndex(texName);
        int row = texIndex / gridSize;
        int col = texIndex % gridSize;
        final float oneOver = 1.0f / (float) gridSize;

        Model result = new Model(fileName, texName);
        Mesh mesh = new Mesh();
        BufferedReader br = new BufferedReader(new InputStreamReader(objInput));
        IList<Vector2f> uvs = new GapList<>();
        IList<Vector3f> normals = new GapList<>();
        String line;
        try {
            while ((line = br.readLine()) != null) {
                String[] things = line.split(" ");
                if (things[0].equals("v")) {
                    Vector3f pos = new Vector3f(Float.parseFloat(things[1]), Float.parseFloat(things[2]), Float.parseFloat(things[3]));
                    Vertex vertex = new Vertex(pos);
                    mesh.vertices.addLast(vertex);
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

        if (reverseFaces) {
            for (int index = 0; index < mesh.indices.size() - 3; index += 3) {
                mesh.indices.reverse(index, 3);
            }
        }

        Material material = new Material(Texture.getOrDefault(texName));
        material.setColor(new Vector4f(GlobalColors.WHITE_RGBA));
        result.materials.add(material);

        result.meshes.add(mesh);
        result.calcDimsPub();

        return result;
    }

    @Deprecated
    public static Model readFromModelFile(String dirEntry, String fileName, String texName) {
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

        Model result = new Model(fileName, texName);

        try {
            byte[] buffer = objInput.readAllBytes();
            objInput.close();

            ByteBuffer byteBuffer = MemoryUtil.memCalloc(buffer.length);
            byteBuffer.clear();
            byteBuffer.put(buffer);
            byteBuffer.flip();
            AIScene aiScene = Assimp.aiImportFileFromMemory(
                    byteBuffer,
                    Assimp.aiProcess_Triangulate
                    | Assimp.aiProcess_ConvertToLeftHanded
                    | Assimp.aiProcess_Debone
                    | Assimp.aiProcess_FlipWindingOrder
                    | Assimp.aiProcess_JoinIdenticalVertices
                    | Assimp.aiProcess_FlipUVs
                    | Assimp.aiProcess_OptimizeMeshes,
                    ""
            );
            if (aiScene == null) {
                DSLogger.reportError("Error loading model " + dirEntry + fileName + "!", null);
                return null;
            }

            int numMaterials = aiScene.mNumMaterials();
            PointerBuffer aiMaterials = aiScene.mMaterials();
            if (aiMaterials == null) {
                DSLogger.reportError("Error loading model " + dirEntry + fileName + "!", null);
                return null;
            }
            for (int i = 0; i < numMaterials; i++) {
                AIMaterial aiMaterial = AIMaterial.create(aiMaterials.get(i));
                result.materials.add(processMaterial(aiMaterial, texName));
            }

            int numMeshes = aiScene.mNumMeshes();
            PointerBuffer aiMeshes = aiScene.mMeshes();
            if (aiMeshes == null) {
                DSLogger.reportError("Error loading model " + dirEntry + fileName + "!", null);
                return null;
            }

            for (int i = 0; i < numMeshes; i++) {
                AIMesh aiMesh = AIMesh.create(aiMeshes.get(i));
                result.meshes.add(processMesh(aiMesh));
            }

            if (byteBuffer.capacity() != 0) {
                MemoryUtil.memFree(byteBuffer);
            }
        } catch (IOException ex) {
            DSLogger.reportError(ex.getMessage(), ex);
        } finally {
            try {
                objInput.close();
            } catch (IOException ex) {
                DSLogger.reportError(ex.getMessage(), ex);
            }
        }

        result.calcDimsPub();

        return result;
    }

    private static Material processMaterial(AIMaterial aiMaterial, String texName) {
        AIColor4D colour = AIColor4D.create();

//        AIString path = AIString.calloc();
//        String textPath = path.dataString();
//        Texture texture = null;
//        if (textPath != null && textPath.length() > 0) {
//            texture = new Texture(dirEntry, textPath);
//        }
        Vector4f ambient = new Vector4f(GlobalColors.WHITE, 1.0f);
        int result = Assimp.aiGetMaterialColor(aiMaterial, Assimp.AI_MATKEY_COLOR_AMBIENT, Assimp.aiTextureType_NONE, 0, colour);
        if (result == 0) {
            ambient = new Vector4f(colour.r(), colour.g(), colour.b(), colour.a());
        }

        Vector4f diffuse = new Vector4f(GlobalColors.WHITE, 1.0f);
        result = Assimp.aiGetMaterialColor(aiMaterial, Assimp.AI_MATKEY_COLOR_DIFFUSE, Assimp.aiTextureType_NONE, 0, colour);
        if (result == 0) {
            diffuse = new Vector4f(colour.r(), colour.g(), colour.b(), colour.a());
        }

        Vector4f specular = new Vector4f(GlobalColors.WHITE, 1.0f);
        result = Assimp.aiGetMaterialColor(aiMaterial, Assimp.AI_MATKEY_COLOR_SPECULAR, Assimp.aiTextureType_NONE, 0, colour);
        if (result == 0) {
            specular = new Vector4f(colour.r(), colour.g(), colour.b(), colour.a());
        }

        Material material = new Material(ambient, diffuse, specular, Texture.getOrDefault(texName));

        DSLogger.reportDebug(material.getTexture().toString(), null);

        return material;
    }

    private static Mesh processMesh(AIMesh aiMesh) {
        Mesh mesh = new Mesh();

        int verticesNum = aiMesh.mNumVertices();
        for (int i = 0; i < verticesNum; i++) {
            AIVector3D.Buffer aiVertices = aiMesh.mVertices();
            AIVector3D aiVertex = aiVertices.get(i);
            Vertex v = new Vertex(aiVertex.x(), aiVertex.y(), aiVertex.z());
            mesh.vertices.add(v);
        }

        AIVector3D.Buffer aiNormals = aiMesh.mNormals();
        if (aiNormals != null) {
            int index = 0;
            while (aiNormals.hasRemaining()) {
                AIVector3D aiNormal = aiNormals.get();
                mesh.vertices.get(index).setNormal(new Vector3f(aiNormal.x(), aiNormal.y(), aiNormal.z()));
                index++;
            }
        }

        AIVector3D.Buffer aiTexCoords = aiMesh.mTextureCoords(0);
        if (aiTexCoords != null) {
            int index = 0;
            while (aiTexCoords.hasRemaining()) {
                AIVector3D aiUV = aiTexCoords.get();
                mesh.vertices.get(index).setUv(new Vector2f(aiUV.x(), aiUV.y()));
                index++;
            }
        }

        int numFaces = aiMesh.mNumFaces();
        AIFace.Buffer aiFaces = aiMesh.mFaces();
        if (aiFaces != null) {
            for (int i = 0; i < numFaces; i++) {
                AIFace aiFace = aiFaces.get(i);
                IntBuffer buffer = aiFace.mIndices();
                while (buffer.hasRemaining()) {
                    mesh.indices.add(buffer.get());
                }
            }
        }

        return mesh;
    }

    /**
     * Convert block specs {solid, texName, VEC3} to unique int (hashcode).
     *
     * @param solid is block solid
     * @param texName texName[5] string,
     * @param facebits
     * @param pos float3(x,y,z) vector
     *
     * @return unique int
     */
    public static int blockSpecsToUniqueInt(boolean solid, String texName, int facebits, Vector3f pos) {
        // Convert boolean solid to integer based on ASCII values of 'S' and 'F'
        int a = solid ? 'S' : 'F';

        // Get texture index
        int b = Texture.getOrDefaultIndex(texName);
        b++;

        // Get chunk function
        int c = Chunk.chunkFunc(pos);
        c++;

        // Calculate indices for the position
        float iFloat = (pos.x + Chunk.BOUND) / 2.0f;
        float jFloat = (pos.z + Chunk.BOUND) / 2.0f;
        float kFloat = (pos.y + Chunk.BOUND) / 2.0f;

        // Calculate unique integer using the position indices using FMA
        int d = (int) Math.fma(Math.round(kFloat), Math.fma(Chunk.BOUND, Chunk.BOUND, Math.fma(Math.round(jFloat), Chunk.BOUND, Math.round(iFloat))), 0);
        d++;

        // Combine all components to generate the final ID using FMA
        int result = Math.round(Math.fma(b ^ c, d, a));

        return result;
    }
}
