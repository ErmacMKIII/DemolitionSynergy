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
package rs.alexanderstojanovich.evg.level;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.joml.Vector3f;
import rs.alexanderstojanovich.evg.audio.AudioFile;
import rs.alexanderstojanovich.evg.audio.AudioPlayer;
import rs.alexanderstojanovich.evg.core.Camera;
import rs.alexanderstojanovich.evg.core.Window;
import rs.alexanderstojanovich.evg.critter.Critter;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.main.GameObject;
import rs.alexanderstojanovich.evg.models.Block;
import rs.alexanderstojanovich.evg.models.Chunk;
import rs.alexanderstojanovich.evg.models.Chunks;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.util.Vector3fUtils;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class LevelContainer implements GravityEnviroment {

    private final GameObject gameObject;

    public static final Block SKYBOX = new Block("night");

    private final Chunks solidChunks = new Chunks();
    private final Chunks fluidChunks = new Chunks();

    private final Set<Integer> visibleChunks = new HashSet<>();

    private final byte[] buffer = new byte[0x1000000]; // 16 MB Buffer
    private int pos = 0;

    public static final float BASE = 7.0f;
    public static final float SKYBOX_SCALE = BASE * BASE * BASE;
    public static final float SKYBOX_WIDTH = 2.0f * SKYBOX_SCALE;
    public static final Vector3f SKYBOX_COLOR = new Vector3f(0.25f, 0.5f, 0.75f); // cool bluish color for SKYBOX

    public static final int MAX_NUM_OF_SOLID_BLOCKS = 65535;
    public static final int MAX_NUM_OF_FLUID_BLOCKS = 65535;

    private float progress = 0.0f;

    private boolean working = false;

    private final LevelActors levelActors = new LevelActors();

    // position of all the solid blocks to neighbors
    public static final Map<Integer, Byte> ALL_SOLID_MAP = new HashMap<>(MAX_NUM_OF_SOLID_BLOCKS);

    // position of all the fluid blocks to neighbors
    public static final Map<Integer, Byte> ALL_FLUID_MAP = new HashMap<>(MAX_NUM_OF_FLUID_BLOCKS);

    private static void determineSolid(Vector3f vector) {
        byte bits = 0;
        for (int j = 0; j <= 5; j++) {
            int mask = 1 << j;
            Vector3f adjPos = Block.getAdjacentPos(vector, j);
            if (ALL_SOLID_MAP.containsKey(Vector3fUtils.hashCode(adjPos))) {
                bits |= mask;
                byte adjBits = ALL_SOLID_MAP.getOrDefault(Vector3fUtils.hashCode(adjPos), (byte) 0);
                int k = (j % 2 == 0 ? j + 1 : j - 1);
                int maskAdj = 1 << k;
                adjBits |= maskAdj;
                ALL_SOLID_MAP.replace(Vector3fUtils.hashCode(adjPos), adjBits);
            }
        }
        ALL_SOLID_MAP.replace(Vector3fUtils.hashCode(vector), bits);
    }

    private static void determineFluid(Vector3f vector) {
        byte bits = 0;
        for (int j = 0; j <= 5; j++) {
            int mask = 1 << j;
            Vector3f adjPos = Block.getAdjacentPos(vector, j);
            if (ALL_FLUID_MAP.containsKey(Vector3fUtils.hashCode(adjPos))) {
                bits |= mask;
                byte adjBits = ALL_FLUID_MAP.getOrDefault(Vector3fUtils.hashCode(adjPos), (byte) 0);
                int k = (j % 2 == 0 ? j + 1 : j - 1);
                int maskAdj = 1 << k;
                adjBits |= maskAdj;
                ALL_FLUID_MAP.replace(Vector3fUtils.hashCode(adjPos), adjBits);
            }
        }
        ALL_FLUID_MAP.replace(Vector3fUtils.hashCode(vector), bits);
    }

    public static void putBlock(Block block) {
        Vector3f pos = block.getPos();
        int hashCode = Vector3fUtils.hashCode(pos);
        if (block.isSolid()) {
            int byteValue = ALL_SOLID_MAP.getOrDefault(hashCode, (byte) -1);
            if (byteValue == -1) {
                ALL_SOLID_MAP.put(hashCode, (byte) -1);
            }
            if (byteValue < 63) {
                determineSolid(pos);
            }
        } else {
            int byteValue = ALL_FLUID_MAP.getOrDefault(hashCode, (byte) -1);
            if (byteValue == -1) {
                ALL_FLUID_MAP.put(hashCode, (byte) -1);
            }
            if (byteValue < 63) {
                determineFluid(pos);
            }
        }
    }

    public static void removeBlock(Block block) {
        Vector3f pos = block.getPos();
        int hashCode = Vector3fUtils.hashCode(pos);
        if (block.isSolid()) {
            int byteValue = ALL_SOLID_MAP.getOrDefault(hashCode, (byte) -1);
            if (byteValue != -1) {
                ALL_SOLID_MAP.remove(hashCode, (byte) -1);
            }
            if (byteValue > 0) {
                determineSolid(pos);
            }
        } else {
            int byteValue = ALL_FLUID_MAP.getOrDefault(hashCode, (byte) -1);
            if (byteValue != -1) {
                ALL_FLUID_MAP.remove(hashCode, (byte) -1);
            }
            if (byteValue > 0) {
                determineFluid(pos);
            }
        }
    }

    static {
        // setting SKYBOX     
        SKYBOX.setPrimaryColor(SKYBOX_COLOR);
        SKYBOX.setUVsForSkybox();
        SKYBOX.setScale(SKYBOX_SCALE);
    }

    public LevelContainer(GameObject gameObject) {
        this.gameObject = gameObject;
    }

    public static void printPositionMaps() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("SOLID POSITION MAP");
        sb.append("(size = ").append(ALL_SOLID_MAP.size()).append(")\n");
        sb.append("---------------------------");
        sb.append("\n");
        sb.append("FLUID POSITION MAP");
        sb.append("(size = ").append(ALL_FLUID_MAP.size()).append(")\n");
        sb.append("---------------------------");
        DSLogger.reportInfo(sb.toString(), null);
    }

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------
    public boolean startNewLevel() {
        if (working) {
            return false;
        }
        boolean success = false;
        working = true;
        progress = 0.0f;
        levelActors.freeze();
        gameObject.getMusicPlayer().play(AudioFile.INTERMISSION, true);

        solidChunks.getChunkList().clear();
        fluidChunks.getChunkList().clear();

        ALL_SOLID_MAP.clear();
        ALL_FLUID_MAP.clear();
        Chunk.deleteCache();

        for (int i = 0; i <= 2; i++) {
            for (int j = 0; j <= 2; j++) {
                Block entity = new Block("doom0");

                entity.getPos().x = 4.0f * i;
                entity.getPos().y = 4.0f * j;
                entity.getPos().z = 3.0f;

                entity.getPrimaryColor().x = 0.5f * i + 0.25f;
                entity.getPrimaryColor().y = 0.5f * j + 0.25f;
                entity.getPrimaryColor().z = 0.0f;

                solidChunks.addBlock(entity, true);

                progress += 100.0f / 9.0f;
            }
        }

        levelActors.getPlayer().getCamera().setPos(new Vector3f(10.5f, 0.0f, -3.0f));
        levelActors.getPlayer().getCamera().setFront(Camera.Z_AXIS);
        levelActors.getPlayer().getCamera().setUp(Camera.Y_AXIS);
        levelActors.getPlayer().getCamera().setRight(Camera.X_AXIS);
        levelActors.getPlayer().getCamera().calcViewMatrixPub();
        levelActors.getPlayer().updateModelPos();

        levelActors.unfreeze();
        progress = 100.0f;
        working = false;
        success = true;
        gameObject.getMusicPlayer().play(AudioFile.AMBIENT, true);
        return success;
    }

    public boolean generateRandomLevel(RandomLevelGenerator randomLevelGenerator, int numberOfBlocks) {
        if (working) {
            return false;
        }
        working = true;
        levelActors.freeze();
        boolean success = false;
        progress = 0.0f;
        gameObject.getMusicPlayer().play(AudioFile.INTERMISSION, true);

        solidChunks.getChunkList().clear();
        fluidChunks.getChunkList().clear();

        ALL_SOLID_MAP.clear();
        ALL_FLUID_MAP.clear();
        Chunk.deleteCache();

        if (numberOfBlocks > 0 && numberOfBlocks <= MAX_NUM_OF_SOLID_BLOCKS + MAX_NUM_OF_FLUID_BLOCKS) {
            randomLevelGenerator.setNumberOfBlocks(numberOfBlocks);
            randomLevelGenerator.generate();
            success = true;
        }

        fluidChunks.updateFluids();

        progress = 100.0f;
        working = false;
        levelActors.unfreeze();
        gameObject.getMusicPlayer().play(AudioFile.AMBIENT, true);
        return success;
    }

    private boolean storeLevelToBuffer() {
        working = true;
        boolean success = false;
        if (progress > 0.0f) {
            return false;
        }
        progress = 0.0f;
        levelActors.freeze();
        gameObject.getMusicPlayer().play(AudioFile.INTERMISSION, true);
        pos = 0;
        buffer[0] = 'D';
        buffer[1] = 'S';
        pos += 2;
        Camera camera = levelActors.getPlayer().getCamera();
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

        solidChunks.loadInvisibleFromDisk(visibleChunks);
        List<Block> solidBlocks = solidChunks.getTotalList();

        fluidChunks.loadInvisibleFromDisk(visibleChunks);
        List<Block> fluidBlocks = fluidChunks.getTotalList();

        //----------------------------------------------------------------------
        for (Block solidBlock : solidBlocks) {
            if (GameObject.MY_WINDOW.shouldClose()) {
                break;
            }
            byte[] byteArraySolid = solidBlock.toByteArray();
            System.arraycopy(byteArraySolid, 0, buffer, pos, 29);
            pos += 29;
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
            if (GameObject.MY_WINDOW.shouldClose()) {
                break;
            }
            byte[] byteArrayFluid = fluidBlock.toByteArray();
            System.arraycopy(byteArrayFluid, 0, buffer, pos, 29);
            pos += 29;
            progress += 100.0f / (fluidBlocks.size() + fluidBlocks.size());
        }

        buffer[pos++] = 'E';
        buffer[pos++] = 'N';
        buffer[pos++] = 'D';

        levelActors.unfreeze();
        progress = 100.0f;

        if (progress == 100.0f && !GameObject.MY_WINDOW.shouldClose()) {
            success = true;
        }
        working = false;
        gameObject.getMusicPlayer().play(AudioFile.AMBIENT, true);
        return success;
    }

    private boolean loadLevelFromBuffer() {
        working = true;
        boolean success = false;
        if (progress > 0.0f) {
            return false;
        }
        progress = 0.0f;
        levelActors.freeze();
        gameObject.getMusicPlayer().play(AudioFile.INTERMISSION, true);
        pos = 0;
        if (buffer[0] == 'D' && buffer[1] == 'S') {
            solidChunks.getChunkList().clear();
            fluidChunks.getChunkList().clear();

            ALL_SOLID_MAP.clear();
            ALL_FLUID_MAP.clear();
            Chunk.deleteCache();

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

            levelActors.getPlayer().getCamera().setPos(campos);
            levelActors.getPlayer().getCamera().setFront(camfront);
            levelActors.getPlayer().getCamera().setUp(camup);
            levelActors.getPlayer().getCamera().setRight(camright);
            levelActors.getPlayer().getCamera().calcViewMatrixPub();
            levelActors.getPlayer().updateModelPos();

            char[] solid = new char[5];
            for (int i = 0; i < solid.length; i++) {
                solid[i] = (char) buffer[pos++];
            }
            String strSolid = String.valueOf(solid);

            if (strSolid.equals("SOLID")) {
                int solidNum = ((buffer[pos + 1] & 0xFF) << 8) | (buffer[pos] & 0xFF);
                pos += 2;
                for (int i = 0; i < solidNum && !GameObject.MY_WINDOW.shouldClose(); i++) {
                    byte[] byteArraySolid = new byte[29];
                    System.arraycopy(buffer, pos, byteArraySolid, 0, 29);
                    Block solidBlock = Block.fromByteArray(byteArraySolid, true);
                    solidChunks.addBlock(solidBlock, true);
                    pos += 29;
                    progress += 50.0f / solidNum;
                }

                solidChunks.saveInvisibleToDisk(visibleChunks);

                char[] fluid = new char[5];
                for (int i = 0; i < fluid.length; i++) {
                    fluid[i] = (char) buffer[pos++];
                }
                String strFluid = String.valueOf(fluid);

                if (strFluid.equals("FLUID")) {
                    int fluidNum = ((buffer[pos + 1] & 0xFF) << 8) | (buffer[pos] & 0xFF);
                    pos += 2;
                    for (int i = 0; i < fluidNum && !GameObject.MY_WINDOW.shouldClose(); i++) {
                        byte[] byteArrayFluid = new byte[29];
                        System.arraycopy(buffer, pos, byteArrayFluid, 0, 29);
                        Block fluidBlock = Block.fromByteArray(byteArrayFluid, false);
                        fluidChunks.addBlock(fluidBlock, true);
                        pos += 29;
                        progress += 50.0f / fluidNum;
                    }

                    fluidChunks.saveInvisibleToDisk(visibleChunks);
                    fluidChunks.updateFluids();
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
        levelActors.unfreeze();
        progress = 100.0f;
        working = false;
        gameObject.getMusicPlayer().play(AudioFile.AMBIENT, true);
        return success;
    }

    public boolean saveLevelToFile(String filename) {
        if (working) {
            return false;
        }
        boolean success = false;
        if (!filename.endsWith(".dat")) {
            filename += ".dat";
        }
        BufferedOutputStream bos = null;
        File file = new File(filename);
        if (file.exists()) {
            file.delete();
        }
        success = storeLevelToBuffer(); // saves level to bufferVertices first
        try {
            bos = new BufferedOutputStream(new FileOutputStream(file));
            bos.write(buffer, 0, pos); // save bufferVertices to file at pos mark
        } catch (FileNotFoundException ex) {
            DSLogger.reportFatalError(ex.getMessage(), ex);
        } catch (IOException ex) {
            DSLogger.reportFatalError(ex.getMessage(), ex);
        }
        if (bos != null) {
            try {
                bos.close();
            } catch (IOException ex) {
                DSLogger.reportFatalError(ex.getMessage(), ex);
            }
        }
        return success;
    }

    public boolean loadLevelFromFile(String filename) {
        if (working) {
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
        BufferedInputStream bis = null;
        if (!file.exists()) {
            return false; // this prevents further fail
        }
        try {
            bis = new BufferedInputStream(new FileInputStream(file));
            bis.read(buffer);
            success = loadLevelFromBuffer();
        } catch (FileNotFoundException ex) {
            DSLogger.reportFatalError(ex.getMessage(), ex);
        } catch (IOException ex) {
            DSLogger.reportFatalError(ex.getMessage(), ex);
        }
        if (bis != null) {
            try {
                bis.close();
            } catch (IOException ex) {
                DSLogger.reportFatalError(ex.getMessage(), ex);
            }
        }
        return success;
    }

    public void animate() {
        if (!working) {
            fluidChunks.animate();
        }
    }

    public boolean isCameraInFluid() {
        boolean yea = false;
        Vector3f obsCamPos = levelActors.getPlayer().getCamera().getPos();

        int currChunkId = Chunk.chunkFunc(obsCamPos);
        Chunk currFluidChunk = fluidChunks.getChunk(currChunkId);
        if (currFluidChunk != null) {
            for (Block fluidBLock : currFluidChunk.getList()) {
                if (fluidBLock.containsInsideEqually(obsCamPos)) {
                    yea = true;
                    break;
                }
            }
        }

        return yea;
    }

    public boolean hasCollisionWithCritter(Critter critter) {
        boolean coll;
        coll = (!SKYBOX.containsInsideExactly(critter.getPredictor())
                || !SKYBOX.intersectsExactly(critter.getPredictor(), critter.getModel().getWidth(),
                        critter.getModel().getHeight(), critter.getModel().getDepth()));
        if (!coll) {
//            OUTER:
//            for (Integer i : visibleChunks) {
//                Chunk solidChunk = solidChunks.getChunk(i);
//                if (solidChunk != null) {
//                    for (Block solidBlock : solidChunk.getList()) {
//                        if (solidBlock.containsInsideEqually(critter.getPredictor())
//                                || solidBlock.intersectsExactly(critter.getPredictor(), critter.getModel().getWidth(),
//                                        critter.getModel().getHeight(), critter.getModel().getDepth())) {
//                            coll = true;
//                            break OUTER;
//                        }
//                    }
//                }
//            }
        }
        return coll;
    }

    // thats what gravity does, object fells down if they don't have support below it (sky or other object)
    @Override
    public void gravityDo(float deltaTime) {
//        float value = (GRAVITY_CONSTANT * deltaTime * deltaTime) / 2.0f;
//        Map<Vector3f, Integer> solidMap = solidChunks.getPosMap();
//        for (Vector3f solidBlockPos : solidMap.keySet()) {
//            Vector3f bottom = new Vector3f(solidBlockPos);
//            bottom.y -= 1.0f;
//            boolean massBelow = false;
//            for (Vector3f otherSolidBlockPos : solidMap.keySet()) {
//                if (!solidBlockPos.equals(otherSolidBlockPos)
//                        && Block.containsOnXZEqually(otherSolidBlockPos, 2.0f, bottom)) {
//                    massBelow = true;
//                    break;
//                }
//            }
//            boolean inSkybox = LevelContainer.SKYBOX.containsInsideExactly(bottom);
//            if (!massBelow && inSkybox) {
//                solidBlockPos.y -= value;
//            }
//        }
//        solidChunks.setBuffered(false);
    }

    // method for determining visible chunks
    public void determineVisible() {
        Camera obsCamera = levelActors.getPlayer().getCamera();
        Chunk.determineVisible(visibleChunks, obsCamera.getPos(), obsCamera.getFront());
    }

    // method for loading visible chunks
    public void autoLoadVisibleChunks() {
        if (!working) {
            boolean singlFldChnkLd = false;
            //------------------------------------------------------------------
            for (int chunkId : visibleChunks) {
                Chunk solidChunk = solidChunks.getChunk(chunkId);
                if (solidChunk != null) {
                    if (solidChunk.isCached()) {
                        solidChunk.loadFromDisk();
                    }
                    if (!solidChunk.isAlive()) {
                        solidChunk.setTimeToLive(Game.TPS << 2);
                    }
                }

                Chunk fluidChunk = fluidChunks.getChunk(chunkId);
                if (fluidChunk != null) {
                    if (fluidChunk.isCached()) {
                        fluidChunk.loadFromDisk();
                        singlFldChnkLd = true;
                    }
                    if (!fluidChunk.isAlive()) {
                        fluidChunk.setTimeToLive(Game.TPS << 2);
                    }
                }
            }

            if (singlFldChnkLd) {
                fluidChunks.updateFluids();
            }
        }
    }

    // method for saving invisible chunks
    public void autoSaveInvisibleChunks() {
        if (!working) {
            for (Chunk solidChunk : solidChunks.getChunkList()) {
                if (solidChunk.isAlive()
                        && !visibleChunks.contains((Integer) solidChunk.getId())) {
                    solidChunk.decTimeToLive();
                } else if (!solidChunk.isAlive() && !solidChunk.isBuffered()) {
                    solidChunk.saveToDisk();
                }
            }

            for (Chunk fluidChunk : fluidChunks.getChunkList()) {
                if (fluidChunk.isAlive()
                        && !visibleChunks.contains((Integer) fluidChunk.getId())) {
                    fluidChunk.decTimeToLive();
                } else if (!fluidChunk.isAlive() && !fluidChunk.isBuffered()) {
                    fluidChunk.saveToDisk();
                }
            }
        }
    }

    public void update(float deltaTime) { // call it externally from the main thread 
        if (!working) { // don't update if working, it may screw up!
            SKYBOX.setrY(SKYBOX.getrY() + deltaTime / 64.0f);
            Camera obsCamera = levelActors.getPlayer().getCamera();
            // determine current chunks where player is
            int chunkId = Chunk.chunkFunc(obsCamera.getPos());
            Chunk fluidChunk = fluidChunks.getChunk(chunkId);
            if (fluidChunk != null) {
                fluidChunk.setCameraInFluid(isCameraInFluid());
            }
        }
    }

    public void render() { // render for regular level rendering
        if (working) {
            return;
        }
        Camera obsCamera = levelActors.getPlayer().getCamera();
        levelActors.render();
        if (!SKYBOX.isBuffered()) {
            SKYBOX.bufferAll();
        }
        SKYBOX.render(ShaderProgram.getMainShader());

        // copy uniforms from main shader to voxel shader
        ShaderProgram.getVoxelShader().bind();
        obsCamera.updateViewMatrix(ShaderProgram.getVoxelShader());
        obsCamera.updateCameraPosition(ShaderProgram.getVoxelShader());
        obsCamera.updateCameraFront(ShaderProgram.getVoxelShader());
        ShaderProgram.unbind();

        for (Chunk solidChunk : solidChunks.getChunkList()) {
            if (!solidChunk.isCached()) {
                if (solidChunk.isAlive() && !solidChunk.isBuffered()) {
                    solidChunk.bufferAll();
                } else if (!solidChunk.isAlive() && solidChunk.isBuffered()) {
                    solidChunk.unbuffer();
                } else if (solidChunk.isAlive() && solidChunk.isBuffered()) {
                    solidChunk.prepare();
                    solidChunk.render(ShaderProgram.getVoxelShader(), obsCamera.getPos());
                }
            }
        }

        for (Chunk fluidChunk : fluidChunks.getChunkList()) {
            if (!fluidChunk.isCached()) {
                if (fluidChunk.isAlive() && !fluidChunk.isBuffered()) {
                    fluidChunk.bufferAll();
                } else if (!fluidChunk.isAlive() && fluidChunk.isBuffered()) {
                    fluidChunk.unbuffer();
                } else if (fluidChunk.isAlive() && fluidChunk.isBuffered()) {
                    fluidChunk.prepare();
                    fluidChunk.render(ShaderProgram.getVoxelShader(), obsCamera.getPos());
                }
            }
        }

        Block editorNew = Editor.getSelectedNew();
        if (editorNew != null) {
            editorNew.setLight(obsCamera.getPos());
            if (!editorNew.isBuffered()) {
                editorNew.bufferAll();
            }
            editorNew.render(ShaderProgram.getMainShader());
        }

        Block selectedNewWireFrame = Editor.getSelectedNewWireFrame();
        if (selectedNewWireFrame != null) {
            selectedNewWireFrame.setLight(obsCamera.getPos());
            if (!selectedNewWireFrame.isBuffered()) {
                selectedNewWireFrame.bufferAll();
            }
            selectedNewWireFrame.render(ShaderProgram.getMainShader());
        }

        Block selectedCurrFrame = Editor.getSelectedCurrWireFrame();
        if (selectedCurrFrame != null) {
            selectedCurrFrame.setLight(obsCamera.getPos());
            if (!selectedCurrFrame.isBuffered()) {
                selectedCurrFrame.bufferAll();
            }
            selectedCurrFrame.render(ShaderProgram.getMainShader());
        }
    }

    public void render(Camera camera) { // render for both regular level rendering and framebuffer (water renderer)        
        if (working) {
            return;
        }
        // render SKYBOX
        camera.render(ShaderProgram.getWaterBaseShader());
        if (!SKYBOX.isBuffered()) {
            SKYBOX.bufferAll();
        }
        SKYBOX.render(ShaderProgram.getWaterBaseShader());
        levelActors.render();

        // copy uniforms from main shader to voxel shader
        ShaderProgram.getWaterVoxelShader().bind();
        camera.updateViewMatrix(ShaderProgram.getWaterVoxelShader());
        camera.updateCameraPosition(ShaderProgram.getWaterVoxelShader());
        camera.updateCameraFront(ShaderProgram.getWaterVoxelShader());
        ShaderProgram.unbind();

        // render blocks
        for (Chunk solidChunk : solidChunks.getChunkList()) {
            if (!solidChunk.isCached()) {
                if (solidChunk.isAlive() && !solidChunk.isBuffered()) {
                    solidChunk.bufferAll();
                } else if (!solidChunk.isAlive() && solidChunk.isBuffered()) {
                    solidChunk.unbuffer();
                } else if (solidChunk.isAlive() && solidChunk.isBuffered()) {
                    solidChunk.prepare();
                    solidChunk.render(ShaderProgram.getWaterVoxelShader(), camera.getPos());
                }
            }
        }

        for (Chunk fluidChunk : fluidChunks.getChunkList()) {
            if (!fluidChunk.isCached()) {
                if (fluidChunk.isAlive() && !fluidChunk.isBuffered()) {
                    fluidChunk.bufferAll();
                } else if (!fluidChunk.isAlive() && fluidChunk.isBuffered()) {
                    fluidChunk.unbuffer();
                } else if (fluidChunk.isAlive() && fluidChunk.isBuffered()) {
                    fluidChunk.prepare();
                    fluidChunk.render(ShaderProgram.getWaterVoxelShader(), camera.getPos());
                }
            }
        }

        Block editorNew = Editor.getSelectedNew();
        if (editorNew != null) {
            editorNew.setLight(camera.getPos());
            if (!editorNew.isBuffered()) {
                editorNew.bufferAll();
            }
            editorNew.render(ShaderProgram.getWaterBaseShader());
        }

        Block selectedNewWireFrame = Editor.getSelectedNewWireFrame();
        if (selectedNewWireFrame != null) {
            selectedNewWireFrame.setLight(camera.getPos());
            if (!selectedNewWireFrame.isBuffered()) {
                selectedNewWireFrame.bufferAll();
            }
            selectedNewWireFrame.render(ShaderProgram.getWaterBaseShader());
        }

        Block selectedCurrFrame = Editor.getSelectedCurrWireFrame();
        if (selectedCurrFrame != null) {
            selectedCurrFrame.setLight(camera.getPos());
            if (!selectedCurrFrame.isBuffered()) {
                selectedCurrFrame.bufferAll();
            }
            selectedCurrFrame.render(ShaderProgram.getWaterBaseShader());
        }
    }

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------
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
        return GameObject.MY_WINDOW;
    }

    public float getProgress() {
        return progress;
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

    public boolean isWorking() { // damn this one!
        return working || progress > 0.0f;
    }

    public Chunks getSolidChunks() {
        return solidChunks;
    }

    public Chunks getFluidChunks() {
        return fluidChunks;
    }

    public GameObject getGameObject() {
        return gameObject;
    }

    public Set<Integer> getVisibleChunks() {
        return visibleChunks;
    }

    public byte[] getBuffer() {
        return buffer;
    }

    public int getPos() {
        return pos;
    }

    public AudioPlayer getMusicPlayer() {
        return gameObject.getMusicPlayer();
    }

    public LevelActors getLevelActors() {
        return levelActors;
    }

}
