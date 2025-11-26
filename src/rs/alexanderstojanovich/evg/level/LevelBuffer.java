/*
 * Copyright (C) 2025 Aleksandar Stojanovic <coas91@rocketmail.com>
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
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.audio.AudioFile;
import rs.alexanderstojanovich.evg.cache.CacheModule;
import rs.alexanderstojanovich.evg.core.Camera;
import static rs.alexanderstojanovich.evg.level.LevelContainer.AllBlockMap;
import rs.alexanderstojanovich.evg.location.TexByte;
import rs.alexanderstojanovich.evg.models.Block;
import rs.alexanderstojanovich.evg.resources.Assets;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.util.VectorFloatUtils;

/**
 * Class responsible for load/save world operations.
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class LevelBuffer {

    public static final int TEX_LEN = 5; // 5 B
    public static final int VEC3_LEN = 12; // 12 B
    public static final int VEC4_LEN = 16; // 16 B
    public static final int BOOL_LEN = 1; // 1 B

    public static final int BUFFER_SIZE = 0x1000000;
    public final ByteBuffer mainBuffer = MemoryUtil.memCalloc(BUFFER_SIZE); // 16 MB Buffer Little Endian
    public final ByteBuffer uploadBuffer = MemoryUtil.memCalloc(BUFFER_SIZE); // 16 MB BAK Buffer Little Endian

    public final LevelContainer levelContainer;

    /**
     * Ensure correct level extension
     *
     * @param filename filename extension
     *
     * @return
     */
    private static String ensureCorrectExtension(String filename) {
        Pattern pattern = Pattern.compile("\\.(dat|ndat)$");
        if (!pattern.matcher(filename).find()) {
            filename += ".dat"; // Default to .dat if no valid extension is found
        }

        return filename;
    }

    /**
     * Create level buffer for storing
     *
     * @param levelContainer where world is stored
     */
    public LevelBuffer(LevelContainer levelContainer) {
        this.levelContainer = levelContainer;
    }

    /**
     * Clear both buffers
     */
    public void clear() {
        mainBuffer.clear();
        uploadBuffer.clear();
    }

    /**
     * Store level in binary DAT (old) format to internal level container
     * mainBuffer.
     *
     * @return on success
     */
    public boolean storeLevelToBufferOldFormat() {
        levelContainer.working = true;
        boolean success = false;
//        if (levelContainer.progress > 0.0f) {
//            return false;
//        }
        levelContainer.progress = 0.0f;
        levelContainer.levelActors.freeze();
        levelContainer.gameObject.getMusicPlayer().play(AudioFile.INTERMISSION, true, true);

        mainBuffer.put((byte) 'D');
        mainBuffer.put((byte) 'S');

        Camera camera = levelContainer.levelActors.mainCamera();
        if (camera == null) {
            return false;
        }

        byte[] campos = VectorFloatUtils.vec3fToByteArray(camera.getPos());
        mainBuffer.put(campos);

        byte[] camfront = VectorFloatUtils.vec3fToByteArray(camera.getFront());
        mainBuffer.put(camfront);

        byte[] camup = VectorFloatUtils.vec3fToByteArray(camera.getUp());
        mainBuffer.put(camup);

        byte[] camright = VectorFloatUtils.vec3fToByteArray(camera.getRight());
        mainBuffer.put(camright);

        IList<Vector3f> solidPos = AllBlockMap.getPopulatedLocations(tb -> tb.solid);
        IList<Vector3f> fluidPos = AllBlockMap.getPopulatedLocations(tb -> !tb.solid);

        mainBuffer.put((byte) 'S');
        mainBuffer.put((byte) 'O');
        mainBuffer.put((byte) 'L');
        mainBuffer.put((byte) 'I');
        mainBuffer.put((byte) 'D');

        int solidNum = solidPos.size();
        mainBuffer.putShort((short) solidNum);
        //----------------------------------------------------------------------
        for (Vector3f sp : solidPos) {
            if (levelContainer.gameObject.gameWindow.shouldClose()) {
                break;
            }
            byte[] byteArraySolid = Block.toByteArray(sp, AllBlockMap.getLocation(sp));
            mainBuffer.put(byteArraySolid);
            levelContainer.progress += 50.0f / (float) solidPos.size();
        }

        mainBuffer.put((byte) 'F');
        mainBuffer.put((byte) 'L');
        mainBuffer.put((byte) 'U');
        mainBuffer.put((byte) 'I');
        mainBuffer.put((byte) 'D');

        int fluidNum = fluidPos.size();
        mainBuffer.putShort((short) fluidNum);
        for (Vector3f fp : fluidPos) {
            if (levelContainer.gameObject.gameWindow.shouldClose()) {
                break;
            }
            byte[] byteArrayFluid = Block.toByteArray(fp, AllBlockMap.getLocation(fp));
            mainBuffer.put(byteArrayFluid);
            levelContainer.progress += 50.0f / (float) fluidPos.size();
        }

        mainBuffer.put((byte) 'E');
        mainBuffer.put((byte) 'N');
        mainBuffer.put((byte) 'D');
        mainBuffer.flip(); // write finished, flip so it can be read

        levelContainer.levelActors.unfreeze();
        levelContainer.progress = 100.0f;

        if (levelContainer.progress == 100.0f && !levelContainer.gameObject.gameWindow.shouldClose()) {
            success = true;
        }
        levelContainer.working = false;
        levelContainer.gameObject.getMusicPlayer().stop();
        return success;
    }

    /**
     * Store level in binary NDAT (new) format to internal level container
     * buffer. Used in Multiplayer.
     *
     * @return on success
     */
    public boolean storeLevelToBufferNewFormat() {
        levelContainer.working = true;
        boolean success = false;
//        if (levelContainer.progress > 0.0f) {
//            return false;
//        }
        levelContainer.progress = 0.0f;
        levelContainer.levelActors.freeze();
        levelContainer.gameObject.getMusicPlayer().play(AudioFile.INTERMISSION, true, true);

        mainBuffer.put((byte) 'D');
        mainBuffer.put((byte) 'S');
        mainBuffer.put((byte) '2');

        Camera camera = levelContainer.levelActors.mainCamera();
        if (camera == null) {
            return false;
        }

        byte[] campos = VectorFloatUtils.vec3fToByteArray(camera.getPos());
        mainBuffer.put(campos);

        byte[] camfront = VectorFloatUtils.vec3fToByteArray(camera.getFront());
        mainBuffer.put(camfront);

        byte[] camup = VectorFloatUtils.vec3fToByteArray(camera.getUp());
        mainBuffer.put(camup);

        byte[] camright = VectorFloatUtils.vec3fToByteArray(camera.getRight());
        mainBuffer.put(camright);

        int allBlkSize = AllBlockMap.locationProperties.size();

        mainBuffer.put((byte) 'B');
        mainBuffer.put((byte) 'L');
        mainBuffer.put((byte) 'K');
        mainBuffer.put((byte) 'S');

        // Store the total number of blocks
        mainBuffer.putInt(allBlkSize);

        for (String texName : Assets.TEX_WORLD) {
            IList<Vector3f> blkPos = AllBlockMap.getPopulatedLocations(tb -> tb.texName.equals(texName));
            int count = blkPos.size();
            byte[] texNameBytes = texName.getBytes(StandardCharsets.US_ASCII);
            for (int i = 0; i < 5; i++) {
                mainBuffer.put(texNameBytes[i]);
            }
            mainBuffer.putInt(count);

            for (Vector3f p : blkPos) {
                if (levelContainer.gameObject.gameWindow.shouldClose()) {
                    break;
                }
                TexByte texByte = AllBlockMap.getLocation(p);
                byte[] vec3fPosBytes = VectorFloatUtils.vec3fToByteArray(p);
                mainBuffer.put(vec3fPosBytes);
                Vector4f primCol = texByte.getColor();
                byte[] vec4fColByte = VectorFloatUtils.vec4fToByteArray(primCol);
                mainBuffer.put(vec4fColByte);

                boolean solid = texByte.solid;
                mainBuffer.put(solid ? (byte) 0xFF : (byte) 0x00);

                levelContainer.progress += 100.0f / (float) allBlkSize;
            }
        }

        mainBuffer.put((byte) 'E');
        mainBuffer.put((byte) 'N');
        mainBuffer.put((byte) 'D');

        levelContainer.levelActors.unfreeze();
        levelContainer.progress = 100.0f;

        if (!levelContainer.gameObject.gameWindow.shouldClose()) {
            success = true;
        }
        levelContainer.working = false;
        levelContainer.gameObject.getMusicPlayer().stop();
        return success;
    }

    /**
     * Load level in binary format DAT (old) to internal level container
     * mainBuffer.
     *
     * @return on success
     */
    public boolean loadLevelFromBufferOldFormat() {
        levelContainer.working = true;
        boolean success = false;
//        if (levelContainer.progress > 0.0f) {
//            return false;
//        }
        levelContainer.progress = 0.0f;
        levelContainer.levelActors.freeze();
        levelContainer.gameObject.getMusicPlayer().play(AudioFile.INTERMISSION, true, true);
        if (mainBuffer.get() == 'D' && mainBuffer.get() == 'S') {
            levelContainer.chunks.clear();

            AllBlockMap.init();

            levelContainer.lightSources.retainLights(2);

            CacheModule.deleteCache();

            byte[] posArr = new byte[12];
            mainBuffer.get(posArr);
            Vector3f campos = VectorFloatUtils.vec3fFromByteArray(posArr);

            byte[] frontArr = new byte[12];
            mainBuffer.get(frontArr);
            Vector3f camfront = VectorFloatUtils.vec3fFromByteArray(frontArr);

            byte[] upArr = new byte[12];
            mainBuffer.get(upArr);
            Vector3f camup = VectorFloatUtils.vec3fFromByteArray(upArr);

            byte[] rightArr = new byte[12];
            mainBuffer.get(rightArr);
            Vector3f camright = VectorFloatUtils.vec3fFromByteArray(rightArr);

            levelContainer.levelActors.configureMainObserver(campos, camfront, camup, camright);

            char[] solid = new char[5];
            for (int i = 0; i < solid.length; i++) {
                solid[i] = (char) mainBuffer.get();
            }
            String strSolid = String.valueOf(solid);

            if (strSolid.equals("SOLID")) {
                int solidNum = mainBuffer.getShort() & 0xFFFF; // Convert to unsigned
                for (int i = 0; i < solidNum && !levelContainer.gameObject.gameWindow.shouldClose(); i++) {
                    byte[] byteArraySolid = new byte[29];
                    mainBuffer.get(byteArraySolid);
                    Block solidBlock = Block.fromByteArray(byteArraySolid, true);
                    levelContainer.chunks.addBlock(solidBlock);
                    levelContainer.progress += 50.0f / solidNum;
                }

//                solidChunks.updateSolids();
                char[] fluid = new char[5];
                for (int i = 0; i < fluid.length; i++) {
                    fluid[i] = (char) mainBuffer.get();
                }
                String strFluid = String.valueOf(fluid);

                if (strFluid.equals("FLUID")) {
                    int fluidNum = mainBuffer.getShort() & 0xFFFF; // Convert to unsigned
                    for (int i = 0; i < fluidNum && !levelContainer.gameObject.gameWindow.shouldClose(); i++) {
                        byte[] byteArrayFluid = new byte[29];
                        mainBuffer.get(byteArrayFluid);
                        Block fluidBlock = Block.fromByteArray(byteArrayFluid, false);
                        levelContainer.chunks.addBlock(fluidBlock);
                        levelContainer.progress += 50.0f / fluidNum;
                    }

//                    fluidChunks.updateFluids();
                    char[] end = new char[3];
                    for (int i = 0; i < end.length; i++) {
                        end[i] = (char) mainBuffer.get();
                    }
                    String strEnd = String.valueOf(end);
                    if (strEnd.equals("END")) {
                        success = true;
                    }
                }

            }

        }
        levelContainer.levelActors.unfreeze();
        levelContainer.blockEnvironment.clear();

        levelContainer.progress = 100.0f;
        levelContainer.working = false;
        levelContainer.gameObject.getMusicPlayer().stop();

        return success;
    }

    /**
     * Load level in binary NDAT (new) format from internal level container
     * buffer. Used in Multiplayer.
     *
     * @return on success
     * @throws java.io.UnsupportedEncodingException
     */
    public boolean loadLevelFromBufferNewFormat() throws UnsupportedEncodingException, Exception {
        levelContainer.working = true;
        boolean success = false;
//        if (levelContainer.progress > 0.0f) {
//            return false;
//        }
        levelContainer.progress = 0.0f;
        levelContainer.levelActors.freeze();
        levelContainer.gameObject.getMusicPlayer().play(AudioFile.INTERMISSION, true, true);

        // Check the initial format identifiers
        if (mainBuffer.get() == 'D' && mainBuffer.get() == 'S' && mainBuffer.get() == '2') {

            AllBlockMap.init();
            levelContainer.lightSources.retainLights(2);
            CacheModule.deleteCache();

            byte[] posArr = new byte[12];
            mainBuffer.get(posArr);
            Vector3f campos = VectorFloatUtils.vec3fFromByteArray(posArr);

            byte[] frontArr = new byte[12];
            mainBuffer.get(frontArr);
            Vector3f camfront = VectorFloatUtils.vec3fFromByteArray(frontArr);

            byte[] upArr = new byte[12];
            mainBuffer.get(upArr);
            Vector3f camup = VectorFloatUtils.vec3fFromByteArray(upArr);

            byte[] rightArr = new byte[12];
            mainBuffer.get(rightArr);
            Vector3f camright = VectorFloatUtils.vec3fFromByteArray(rightArr);

            levelContainer.levelActors.configureMainObserver(campos, camfront, camup, camright);

            if (mainBuffer.get() == 'B' && mainBuffer.get() == 'L' && mainBuffer.get() == 'K' && mainBuffer.get() == 'S') {
                // Read the total number of blocks
                final int totalBlocks = mainBuffer.getInt();
                if (totalBlocks <= 0) { // 'Empty world'
                    levelContainer.levelActors.unfreeze();
                    levelContainer.blockEnvironment.clear();

                    levelContainer.progress = 100.0f;
                    levelContainer.working = false;
                    levelContainer.gameObject.getMusicPlayer().stop();
                    throw new Exception("No blocks to process!");
                }

                int totalCount = totalBlocks;
                while (totalCount != 0) {
                    char[] texNameChars = new char[5];
                    for (int i = 0; i < texNameChars.length; i++) {
                        texNameChars[i] = (char) mainBuffer.get();
                    }
                    String texName = new String(texNameChars);

                    // Block count for that texture type
                    int count = mainBuffer.getInt();
                    if (count <= 0 || count > totalBlocks) {
                        throw new Exception("Error in level ndat file. File could be corrupted!");
                    }

                    for (int i = 0; i < count && !levelContainer.gameObject.gameWindow.shouldClose(); i++) {
                        byte[] vec3fPosBytes = new byte[VEC3_LEN];
                        mainBuffer.get(vec3fPosBytes);
                        Vector3f blockPos = VectorFloatUtils.vec3fFromByteArray(vec3fPosBytes);

                        byte[] vec4fColBytes = new byte[VEC4_LEN];
                        mainBuffer.get(vec4fColBytes);
                        Vector4f blockCol = VectorFloatUtils.vec4fFromByteArray(vec4fColBytes);

                        boolean solid = mainBuffer.get() != (byte) 0x00;
                        Block block = new Block(texName, blockPos, blockCol, solid);
                        levelContainer.chunks.addBlock(block);

                        levelContainer.progress += 100.0f / (float) totalBlocks;
                        if (totalCount-- < 0) {
                            throw new Exception("Error in level ndat file. File could be corrupted!");
                        }
                    }

                    if (totalCount < 0) {
                        throw new Exception("Error in level ndat file. File could be corrupted!");
                    }
                }
            }
        }

        if (mainBuffer.get() == 'E' && mainBuffer.get() == 'N' && mainBuffer.get() == 'D') {
            success = true;
        }

        levelContainer.levelActors.unfreeze();
        levelContainer.blockEnvironment.clear();

        levelContainer.progress = 100.0f;
        levelContainer.working = false;
        levelContainer.gameObject.getMusicPlayer().stop();

        return success;
    }

    /**
     * Save level to exported file. Extension is chosen (dat|ndat) in filename
     * arg.
     *
     * @param filename filename to export on drive (dat|ndat).
     * @return on success
     */
    public boolean saveLevelToFile(String filename) {
        if (levelContainer.working) {
            return false;
        }
        boolean success = false;
        ensureCorrectExtension(filename);

        BufferedOutputStream bos = null;
        File file = new File(filename);
        if (file.exists()) {
            file.delete();
        }

        mainBuffer.clear();
        if (filename.endsWith(".dat")) {
            success |= storeLevelToBufferOldFormat(); // saves level to mainBuffer first
        } else if (filename.endsWith(".ndat")) {
            success |= storeLevelToBufferNewFormat(); // saves level to mainBuffer first
        }
        mainBuffer.flip(); // write finished, flip so it can be read
        try {
            bos = new BufferedOutputStream(new FileOutputStream(file));
            byte[] buff = new byte[mainBuffer.limit()];
            mainBuffer.get(buff);
            bos.write(buff); // save mainBuffer to file at pos to limit mark
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

    /**
     * Load level from imported file. Extension is chosen (dat|ndat) in filename
     * arg.
     *
     * @param filename filename to export on drive (dat|ndat).
     * @return on success
     */
    public boolean loadLevelFromFile(String filename) {
        if (levelContainer.working) {
            return false;
        }
        boolean success = false;
        if (filename.isEmpty()) {
            return false;
        }
        ensureCorrectExtension(filename);

        File file = new File(filename);
        BufferedInputStream bis = null;
        if (!file.exists()) {
            return false; // this prevents further fail
        }
        try {
            bis = new BufferedInputStream(new FileInputStream(file));
            byte[] buff = new byte[BUFFER_SIZE];
            int length = bis.read(buff);
            mainBuffer.clear();
            mainBuffer.put(buff, 0, length);
            mainBuffer.flip();
            if (filename.endsWith(".dat")) {
                success |= loadLevelFromBufferOldFormat();
            } else if (filename.endsWith(".ndat")) {
                success |= loadLevelFromBufferNewFormat();
            }
        } catch (FileNotFoundException ex) {
            DSLogger.reportFatalError(ex.getMessage(), ex);
        } catch (IOException ex) {
            DSLogger.reportFatalError(ex.getMessage(), ex);
        } catch (Exception ex) {
            DSLogger.reportError(ex.getMessage(), ex); // zero blocks
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

    /**
     * Used in storing world for multiplayer. Upload buffer is needed for server
     * operations of download and get fragment. Used in server.
     */
    public void copyMain2UploadBuffer() {
        uploadBuffer.clear();
        // Assuming sourceBuffer and destinationBuffer are already initialized
        if (mainBuffer.position() != 0 && mainBuffer.limit() == mainBuffer.position()) {
            mainBuffer.flip(); // Prepare sourceBuffer for reading
        }
        uploadBuffer.put(mainBuffer); // Copy remaining bytes from source to destination
        mainBuffer.compact(); // If you need to retain unread data in sourceBuffer
        uploadBuffer.flip(); // Make upload buffer readable
    }

    /**
     * After level download it is copied to main buffer to allow loading (world)
     * level. Used in client.
     */
    public void copyUpload2MainBuffer() {
        mainBuffer.clear();
        // Assuming sourceBuffer and destinationBuffer are already initialized
        if (uploadBuffer.position() != 0 && uploadBuffer.limit() == uploadBuffer.position()) {
            uploadBuffer.flip(); // Prepare sourceBuffer for reading
        }
        mainBuffer.put(uploadBuffer); // Copy remaining bytes from source to destination
        uploadBuffer.compact(); // If you need to retain unread data in sourceBuffer
        mainBuffer.flip(); // Make upload buffer readable
    }

    /**
     * Clean up used resources (BUFFER(S))
     */
    public void release() {
        MemoryUtil.memFree(mainBuffer);
        MemoryUtil.memFree(uploadBuffer);
    }

    public ByteBuffer getMainBuffer() {
        return mainBuffer;
    }

    public ByteBuffer getUploadBuffer() {
        return uploadBuffer;
    }

    public LevelContainer getLevelContainer() {
        return levelContainer;
    }

    public float getProgress() {
        return levelContainer.progress;
    }

}
