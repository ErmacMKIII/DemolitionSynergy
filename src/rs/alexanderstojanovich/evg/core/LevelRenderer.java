/*
 * Copyright (C) 2019 Coa
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import rs.alexanderstojanovich.evg.audio.AudioFile;
import rs.alexanderstojanovich.evg.audio.AudioPlayer;
import rs.alexanderstojanovich.evg.models.Block;
import rs.alexanderstojanovich.evg.models.Blocks;
import rs.alexanderstojanovich.evg.models.Chunk;
import rs.alexanderstojanovich.evg.models.Chunks;
import rs.alexanderstojanovich.evg.models.Model;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.util.Tuple;
import rs.alexanderstojanovich.evg.util.Vector3fUtils;

/**
 *
 * @author Coa
 */
public class LevelRenderer {

    private final Window myWindow;
    private final Block skybox;

    private final Chunks solidChunks = new Chunks();
    private final Chunks fluidChunks = new Chunks();

    private Critter observer;

    private final byte[] buffer = new byte[0x1000000]; // 16 MB Buffer
    private int pos = 0;

    public static final float SKYBOX_SCALE = 256.0f * 256.0f * 256.0f; // default 16.7M
    public static final float SKYBOX_WIDTH = 256.f; // default 256
    public static final Vector4f SKYBOX_COLOR = new Vector4f(0.25f, 0.5f, 0.75f, 1.0f); // cool bluish color for skybox

    public static final int MAX_NUM_OF_SOLID_BLOCKS = 65535;
    public static final int MAX_NUM_OF_FLUID_BLOCKS = 65535;

    private float progress = 0.0f;

    private boolean working = false;

    private final RandomLevelGenerator randomLevelGenerator;

    private final AudioPlayer musicPlayer;
    private final AudioPlayer soundFXPlayer;

    //----------------Vector3f hash, Block hash---------------------------------
    private static final Map<Vector3f, Integer> POS_SOLID_MAP = new HashMap<>();
    private static final Map<Vector3f, Integer> POS_FLUID_MAP = new HashMap<>();

    public LevelRenderer(Window myWindow, AudioPlayer musicPlayer, AudioPlayer soundFXPlayer) {
        this.myWindow = myWindow;
        this.randomLevelGenerator = new RandomLevelGenerator(this);
        this.musicPlayer = musicPlayer;
        this.soundFXPlayer = soundFXPlayer;
        // setting skybox
        skybox = new Block(true, Texture.NIGHT);
        skybox.setPrimaryColor(SKYBOX_COLOR);
        skybox.setUVsForSkybox();
        skybox.setScale(SKYBOX_SCALE);
        // setting observer
        observer = new Critter("icosphere.obj", Texture.MARBLE, new Vector3f(10.5f, 0.0f, -3.0f), new Vector4f(1.0f, 1.0f, 1.0f, 1.0f), 0.25f);
        observer.setGivenControl(true);
    }

    public boolean startNewLevel() {
        if (working || progress > 0.0f) {
            return false;
        }
        boolean success = false;
        working = true;
        progress = 0.0f;
        musicPlayer.play(AudioFile.INTERMISSION, true);
        observer = new Critter("icosphere.obj", Texture.MARBLE, new Vector3f(10.5f, 0.0f, -3.0f), new Vector4f(1.0f, 1.0f, 1.0f, 1.0f), 0.25f);
        observer.setGivenControl(true);

        solidChunks.getChunkList().clear();
        fluidChunks.getChunkList().clear();

        POS_SOLID_MAP.clear();
        POS_FLUID_MAP.clear();
        for (int i = 0; i <= 2; i++) {
            for (int j = 0; j <= 2; j++) {
                Block entity = new Block(false, Texture.DOOM0);
                entity.setScale(1.0f);

                entity.getPos().x = 4.0f * i;
                entity.getPos().y = 4.0f * j;
                entity.getPos().z = 3.0f;

                POS_SOLID_MAP.put(entity.getPos(), entity.hashCode());

                entity.getPrimaryColor().x = 0.5f * i + 0.25f;
                entity.getPrimaryColor().y = 0.5f * j + 0.25f;
                entity.getPrimaryColor().z = 0.0f;
                entity.getPrimaryColor().w = 1.0f;

                entity.setLight(observer.getModel().getPos());

                solidChunks.addBlock(entity);

                progress += 100.0f / 9.0f;
            }
        }

        solidChunks.setBuffered(false);
        fluidChunks.setBuffered(false);

        progress = 100.0f;
        working = false;
        success = true;
        musicPlayer.play(AudioFile.AMBIENT, true);
        return success;
    }

    public boolean generateRandomLevel(int numberOfBlocks) {
        if (working || progress > 0.0f) {
            return false;
        }
        working = true;
        boolean success = false;
        progress = 0.0f;
        musicPlayer.play(AudioFile.INTERMISSION, true);
        observer = new Critter("icosphere.obj", Texture.MARBLE, new Vector3f(10.5f, 0.0f, -3.0f), new Vector4f(1.0f, 1.0f, 1.0f, 1.0f), 0.25f);
        observer.setGivenControl(false);

        solidChunks.getChunkList().clear();
        fluidChunks.getChunkList().clear();

        POS_SOLID_MAP.clear();
        POS_FLUID_MAP.clear();
        if (numberOfBlocks > 0 && numberOfBlocks <= MAX_NUM_OF_SOLID_BLOCKS + MAX_NUM_OF_FLUID_BLOCKS) {
            randomLevelGenerator.setNumberOfBlocks(numberOfBlocks);
            randomLevelGenerator.generate();
            success = true;
        }

        solidChunks.setBuffered(false);
        fluidChunks.updateFluids(true);
        fluidChunks.setBuffered(false);

        observer.setGivenControl(true);

        progress = 100.0f;
        working = false;
        musicPlayer.play(AudioFile.AMBIENT, true);
        return success;
    }

    private boolean storeLevelToBuffer() {
        working = true;
        boolean success = false;
        if (progress > 0.0f) {
            return false;
        }
        progress = 0.0f;
        musicPlayer.play(AudioFile.INTERMISSION, true);
        pos = 0;
        buffer[0] = 'D';
        buffer[1] = 'S';
        pos += 2;
        Camera camera = this.observer.getCamera();
        byte[] campos = Vector3fUtils.vec3fToByteArray(camera.getPos());
        System.arraycopy(campos, 0, buffer, pos, campos.length);
        pos += campos.length;

        byte[] camfront = Vector3fUtils.vec3fToByteArray(camera.getFront());
        System.arraycopy(camfront, 0, buffer, pos, camfront.length);
        pos += camfront.length;

        byte[] camup = Vector3fUtils.vec3fToByteArray(camera.getUp());
        System.arraycopy(camup, 0, buffer, pos, camup.length);
        pos += camup.length;

        byte[] camright = Vector3fUtils.vec3fToByteArray(camera.getRight());
        System.arraycopy(camup, 0, buffer, pos, camright.length);
        pos += camright.length;

        buffer[pos++] = 'S';
        buffer[pos++] = 'O';
        buffer[pos++] = 'L';
        buffer[pos++] = 'I';
        buffer[pos++] = 'D';

        int solidNum = solidChunks.totalSize();
        buffer[pos++] = (byte) (solidNum);
        buffer[pos++] = (byte) (solidNum >> 8);

        List<Block> solidBlocks = solidChunks.getTotalList();
        List<Block> fluidBlocks = fluidChunks.getTotalList();

        //----------------------------------------------------------------------
        for (Block solidBlock : solidBlocks) {
            if (myWindow.shouldClose()) {
                break;
            }

            byte[] texName = solidBlock.getPrimaryTexture().getImage().getFileName().getBytes();
            System.arraycopy(texName, 0, buffer, pos, 5);
            pos += 5;
            byte[] solidPos = Vector3fUtils.vec3fToByteArray(solidBlock.getPos());
            System.arraycopy(solidPos, 0, buffer, pos, solidPos.length);
            pos += solidPos.length;
            Vector4f primCol = solidBlock.getPrimaryColor();
            Vector3f col = new Vector3f(primCol.x, primCol.y, primCol.z);
            byte[] solidCol = Vector3fUtils.vec3fToByteArray(col);
            System.arraycopy(solidCol, 0, buffer, pos, solidCol.length);
            pos += solidCol.length;

            progress += 100.0f / (solidBlocks.size() + fluidBlocks.size());
        }

        buffer[pos++] = 'F';
        buffer[pos++] = 'L';
        buffer[pos++] = 'U';
        buffer[pos++] = 'I';
        buffer[pos++] = 'D';

        int fluidNum = fluidChunks.totalSize();
        buffer[pos++] = (byte) (fluidNum);
        buffer[pos++] = (byte) (fluidNum >> 8);

        for (Block fluidBlock : fluidBlocks) {
            if (myWindow.shouldClose()) {
                break;
            }

            byte[] texName = fluidBlock.getPrimaryTexture().getImage().getFileName().getBytes();
            System.arraycopy(texName, 0, buffer, pos, 5);
            pos += 5;
            byte[] solidPos = Vector3fUtils.vec3fToByteArray(fluidBlock.getPos());
            System.arraycopy(solidPos, 0, buffer, pos, solidPos.length);
            pos += solidPos.length;
            Vector4f primCol = fluidBlock.getPrimaryColor();
            Vector3f col = new Vector3f(primCol.x, primCol.y, primCol.z);
            byte[] solidCol = Vector3fUtils.vec3fToByteArray(col);
            System.arraycopy(solidCol, 0, buffer, pos, solidCol.length);
            pos += solidCol.length;

            progress += 100.0f / (solidBlocks.size() + fluidBlocks.size());
        }

        buffer[pos++] = 'E';
        buffer[pos++] = 'N';
        buffer[pos++] = 'D';

        progress = 100.0f;

        if (progress == 100.0f && !myWindow.shouldClose()) {
            success = true;
        }
        working = false;
        musicPlayer.play(AudioFile.AMBIENT, true);
        return success;
    }

    private boolean loadLevelFromBuffer() {
        working = true;
        boolean success = false;
        if (progress > 0.0f) {
            return false;
        }
        progress = 0.0f;
        musicPlayer.play(AudioFile.INTERMISSION, true);
        pos = 0;
        if (buffer[0] == 'D' && buffer[1] == 'S') {
            solidChunks.getChunkList().clear();
            fluidChunks.getChunkList().clear();

            POS_SOLID_MAP.clear();
            POS_FLUID_MAP.clear();

            pos += 2;
            byte[] posArr = new byte[12];
            System.arraycopy(buffer, pos, posArr, 0, posArr.length);
            Vector3f campos = Vector3fUtils.vec3fFromByteArray(posArr);
            pos += posArr.length;

            byte[] frontArr = new byte[12];
            System.arraycopy(buffer, pos, frontArr, 0, frontArr.length);
            Vector3f camfront = Vector3fUtils.vec3fFromByteArray(frontArr);
            pos += frontArr.length;

            byte[] upArr = new byte[12];
            System.arraycopy(buffer, pos, frontArr, 0, upArr.length);
            Vector3f camup = Vector3fUtils.vec3fFromByteArray(upArr);
            pos += upArr.length;

            byte[] rightArr = new byte[12];
            System.arraycopy(buffer, pos, rightArr, 0, rightArr.length);
            Vector3f camright = Vector3fUtils.vec3fFromByteArray(rightArr);
            pos += rightArr.length;

            Camera obsCamera = new Camera(campos, camfront, camup, camright);
            Model obsModel = new Model(false, "icosphere.obj", Texture.MARBLE);
            obsModel.setScale(0.01f);
            observer = new Critter(obsCamera, obsModel);
            observer.setGivenControl(true);
            char[] solid = new char[5];
            for (int i = 0; i < solid.length; i++) {
                solid[i] = (char) buffer[pos++];
            }
            String strSolid = String.valueOf(solid);

            if (strSolid.equals("SOLID")) {
                int solidNum = ((buffer[pos + 1] & 0xFF) << 8) | (buffer[pos] & 0xFF);
                pos += 2;
                for (int i = 0; i < solidNum && !myWindow.shouldClose(); i++) {
                    char[] texNameArr = new char[5];
                    for (int k = 0; k < texNameArr.length; k++) {
                        texNameArr[k] = (char) buffer[pos++];
                    }
                    String texName = String.valueOf(texNameArr);

                    byte[] blockPosArr = new byte[12];
                    System.arraycopy(buffer, pos, blockPosArr, 0, blockPosArr.length);
                    Vector3f blockPos = Vector3fUtils.vec3fFromByteArray(blockPosArr);
                    pos += blockPosArr.length;

                    byte[] blockPosCol = new byte[12];
                    System.arraycopy(buffer, pos, blockPosCol, 0, blockPosCol.length);
                    Vector3f blockCol = Vector3fUtils.vec3fFromByteArray(blockPosCol);
                    pos += blockPosCol.length;

                    Vector4f primaryColor = new Vector4f(blockCol, 1.0f);

                    Block block = new Block(false, Texture.TEX_MAP.get(texName), blockPos, primaryColor, true);
                    solidChunks.addBlock(block);

                    progress += 50.0f / solidNum;
                }
                solidChunks.setBuffered(false);
                char[] fluid = new char[5];
                for (int i = 0; i < fluid.length; i++) {
                    fluid[i] = (char) buffer[pos++];
                }
                String strFluid = String.valueOf(fluid);
                if (strFluid.equals("FLUID")) {
                    int fluidNum = ((buffer[pos + 1] & 0xFF) << 8) | (buffer[pos] & 0xFF);
                    pos += 2;
                    for (int i = 0; i < fluidNum && !myWindow.shouldClose(); i++) {
                        char[] texNameArr = new char[5];
                        for (int k = 0; k < texNameArr.length; k++) {
                            texNameArr[k] = (char) buffer[pos++];
                        }
                        String texName = String.valueOf(texNameArr);

                        byte[] blockPosArr = new byte[12];
                        System.arraycopy(buffer, pos, blockPosArr, 0, blockPosArr.length);
                        Vector3f blockPos = Vector3fUtils.vec3fFromByteArray(blockPosArr);
                        pos += blockPosArr.length;

                        byte[] blockPosCol = new byte[12];
                        System.arraycopy(buffer, pos, blockPosCol, 0, blockPosCol.length);
                        Vector3f blockCol = Vector3fUtils.vec3fFromByteArray(blockPosCol);
                        pos += blockPosCol.length;

                        Vector4f primaryColor = new Vector4f(blockCol, 0.5f);

                        Block block = new Block(false, Texture.TEX_MAP.get(texName), blockPos, primaryColor, false);
                        fluidChunks.addBlock(block);

                        progress += 50.0f / fluidNum;
                    }
                    fluidChunks.updateFluids(true);
                    fluidChunks.setBuffered(false);
                    char[] end = new char[3];
                    for (int i = 0; i < end.length; i++) {
                        end[i] = (char) buffer[pos++];
                    }
                    String strEnd = String.valueOf(end);
                    if (strEnd.equals("END")) {
                        success = true;
                    }
                }
            }
        }
        progress = 100.0f;
        working = false;
        musicPlayer.play(AudioFile.AMBIENT, true);
        return success;
    }

    public boolean saveLevelToFile(String filename) {
        if (working || progress > 0.0f) {
            return false;
        }
        boolean success = false;
        if (!filename.endsWith(".dat")) {
            filename += ".dat";
        }
        FileOutputStream fos = null;
        File file = new File(filename);
        if (file.exists()) {
            file.delete();
        }
        Arrays.fill(buffer, (byte) 0);
        success = storeLevelToBuffer(); // saves level to bufferVertices first
        try {
            fos = new FileOutputStream(file);
            fos.write(buffer, 0, pos); // save bufferVertices to file at pos mark
        } catch (FileNotFoundException ex) {
            DSLogger.reportFatalError(ex.getMessage(), ex);
        } catch (IOException ex) {
            DSLogger.reportFatalError(ex.getMessage(), ex);
        }
        if (fos != null) {
            try {
                fos.close();
            } catch (IOException ex) {
                DSLogger.reportFatalError(ex.getMessage(), ex);
            }
        }
        return success;
    }

    public boolean loadLevelFromFile(String filename) {
        if (working || progress > 0.0f) {
            return false;
        }
        boolean success = false;
        if (filename.isEmpty()) {
            return false;
        }
        if (!filename.endsWith(".dat")) {
            filename += ".dat";
        }
        File file = new File(filename);
        FileInputStream fis = null;
        if (!file.exists()) {
            return false; // this prevents further fail
        }
        Arrays.fill(buffer, (byte) 0);
        try {
            fis = new FileInputStream(file);
            fis.read(buffer);
            success = loadLevelFromBuffer();
        } catch (FileNotFoundException ex) {
            DSLogger.reportFatalError(ex.getMessage(), ex);
        } catch (IOException ex) {
            DSLogger.reportFatalError(ex.getMessage(), ex);
        }
        if (fis != null) {
            try {
                fis.close();
            } catch (IOException ex) {
                DSLogger.reportFatalError(ex.getMessage(), ex);
            }
        }
        return success;
    }

    public void animate() {
        fluidChunks.animate();
    }

    public boolean isCameraInFluid() {
        boolean yea = false;
        Vector3f obsCamPos = observer.getCamera().getPos();
        Chunk fluidChunk = fluidChunks.getChunk(Chunk.chunkFunc(obsCamPos));
        if (fluidChunk != null) {
            for (Tuple<Blocks, Integer, Integer, Texture, Integer> tuple : fluidChunk.getTupleList()) {
                for (Block fluidBlock : tuple.getA().getBlockList()) {
                    if (fluidBlock.contains(obsCamPos)) {
                        yea = true;
                        break;
                    }
                }
            }
        }
        return yea;
    }

    public boolean hasCollisionWithCritter(Critter critter) {
        boolean coll;
        coll = (!skybox.containsExactly(critter.getPredictor()) || !skybox.intersectsExactly(critter.getPredModel()));
        Vector3f obsPredPos = critter.getPredictor();
        List<Chunk> visibleChunks = solidChunks.getVisibleChunks();
        for (Chunk solidChunk : visibleChunks) {
            for (Tuple<Blocks, Integer, Integer, Texture, Integer> tuple : solidChunk.getTupleList()) {
                for (Block solidBlock : tuple.getA().getBlockList()) {
                    if (solidBlock.contains(obsPredPos) || solidBlock.intersects(critter.getPredModel())) {
                        coll = true;
                        break;
                    }
                }
            }
        }
        return coll;
    }

    public void update() { // call it externally from the main thread 
        if (working || progress > 0.0f) {
            return; // don't update if working, it may screw up!
        }

        Camera obsCamera = observer.getCamera();
        int currChunkId = Chunk.chunkFunc(obsCamera.getPos(), obsCamera.getFront());
        List<Integer> visibleList = Chunk.determineVisible(obsCamera.getPos(), obsCamera.getFront()); // is list of estimated visible chunks (by that chunk)
        Chunk currSolid = solidChunks.getChunk(currChunkId);
        if (currSolid != null) {
            currSolid.setVisible(true);
        }
        for (Chunk solidChunk : solidChunks.getChunkList()) {
            if (visibleList.contains(solidChunk.getId())) {
                solidChunk.setVisible(true);
            } else {
                solidChunk.setVisible(false);
            }
        }

        Chunk currFluid = fluidChunks.getChunk(currChunkId);
        if (currFluid != null) {
            currFluid.setVisible(true);
        }
        for (Chunk fluidChunk : fluidChunks.getChunkList()) {
            if (visibleList.contains(fluidChunk.getId())) {
                fluidChunk.setVisible(true);
            } else {
                fluidChunk.setVisible(false);
            }
        }

        fluidChunks.setCameraInFluid(isCameraInFluid());
    }

    public void render() { // render for regular level rendering
        Camera obsCamera = observer.getCamera();
        // render skybox
        obsCamera.render(ShaderProgram.getMainShader());
        float time = (float) GLFW.glfwGetTime();
        skybox.setrY(time / 64.0f);
        skybox.render(ShaderProgram.getMainShader());

        Block editorNew = Editor.getSelectedNew();
        if (editorNew != null) {
            editorNew.setLight(obsCamera.getPos());
            if (!editorNew.isBuffered()) {
                editorNew.bufferAll();
            }
            editorNew.render(ShaderProgram.getMainShader());
        }
        // copy uniforms from main shader to voxel shader
        ShaderProgram.getVoxelShader().bind();
        obsCamera.updateViewMatrix(ShaderProgram.getVoxelShader());
        obsCamera.updateCameraPosition(ShaderProgram.getVoxelShader());
        obsCamera.updateCameraFront(ShaderProgram.getVoxelShader());
        ShaderProgram.unbind();

        // render solid series         
        if (!solidChunks.isBuffered()) {
            solidChunks.bufferAll();
        }

        solidChunks.render(ShaderProgram.getVoxelShader(), obsCamera.getPos());

        // render fluid blocks      
        if (!fluidChunks.isBuffered()) {
            fluidChunks.bufferAll();
        }

        fluidChunks.prepare();
        fluidChunks.render(ShaderProgram.getVoxelShader(), obsCamera.getPos());
    }

    public void render(Camera camera) { // render for both regular level rendering and framebuffer (water renderer)        
        // render skybox
        camera.render(ShaderProgram.getWaterBaseShader());
        skybox.render(ShaderProgram.getWaterBaseShader());

        Block editorNew = Editor.getSelectedNew();
        if (editorNew != null) {
            editorNew.setLight(camera.getPos());
            if (!editorNew.isBuffered()) {
                editorNew.bufferAll();
            }
            editorNew.render(ShaderProgram.getWaterBaseShader());
        }
        // copy uniforms from main shader to voxel shader
        ShaderProgram.getWaterVoxelShader().bind();
        camera.updateViewMatrix(ShaderProgram.getWaterVoxelShader());
        camera.updateCameraPosition(ShaderProgram.getWaterVoxelShader());
        camera.updateCameraFront(ShaderProgram.getWaterVoxelShader());
        ShaderProgram.unbind();

        // render solid series     
        if (!solidChunks.isBuffered()) {
            solidChunks.bufferAll();
        }
        solidChunks.render(ShaderProgram.getWaterVoxelShader(), camera.getPos());

        // render fluid blocks      
        if (!fluidChunks.isBuffered()) {
            fluidChunks.bufferAll();
        }

        fluidChunks.prepare();
        fluidChunks.render(ShaderProgram.getWaterVoxelShader(), camera.getPos());
    }

    public boolean maxSolidReached() {
        return solidChunks.totalSize() == MAX_NUM_OF_SOLID_BLOCKS;
    }

    public boolean maxFluidReached() {
        return fluidChunks.totalSize() == MAX_NUM_OF_FLUID_BLOCKS;
    }

    public void incProgress(float increment) {
        if (progress < 100.0f) {
            progress += increment;
        }
    }

    public Window getMyWindow() {
        return myWindow;
    }

    public Block getSkybox() {
        return skybox;
    }

    public Critter getObserver() {
        return observer;
    }

    public void setObserver(Critter observer) {
        this.observer = observer;
    }

    public float getProgress() {
        return progress;
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

    public boolean isWorking() {
        return working;
    }

    public Chunks getSolidChunks() {
        return solidChunks;
    }

    public Chunks getFluidChunks() {
        return fluidChunks;
    }

    public RandomLevelGenerator getRandomLevelGenerator() {
        return randomLevelGenerator;
    }

    public byte[] getBuffer() {
        return buffer;
    }

    public int getPos() {
        return pos;
    }

    public static Map<Vector3f, Integer> getPOS_SOLID_MAP() {
        return POS_SOLID_MAP;
    }

    public static Map<Vector3f, Integer> getPOS_FLUID_MAP() {
        return POS_FLUID_MAP;
    }

    public AudioPlayer getMusicPlayer() {
        return musicPlayer;
    }

    public AudioPlayer getSoundFXPlayer() {
        return soundFXPlayer;
    }

}
