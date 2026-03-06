package rs.alexanderstojanovich.evg.texture;

import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;
import rs.alexanderstojanovich.evg.resources.Assets;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.util.ImageUtils;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

/**
 * TextureArray class responsible for loading and buffering GL_TEXTURE_2D_ARRAY textures.
 * Texture data is stored in BufferedImage array and can be buffered to OpenGL when needed.
 * World textures are loaded into a GL_TEXTURE_2D_ARRAY for efficient sampling in the shader.
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class TextureArray implements TextureIfc {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    /** Expected size (width and height) for regular textures in pixels. */
    public static final int TEX_SIZE = 128;

    /** Expected size (width and height) for each tile in the texture array. */
    public static final int ARRAY_TILE_SIZE = 64;

    /** Empty/blank texture used as a placeholder (e.g. for water). */
    public static final TextureArray EMPTY = new TextureArray("", new String[0], Texture.Format.RGBA8, TEX_SIZE);

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------

    /** Texture format flag. */
    protected Texture.Format texFmt = Texture.Format.NONE;

    /** Original image data array — one entry per layer. */
    private final BufferedImage[] images;

    /** Texture names (aliases) — one per layer, derived from fileNames. */
    private final String[] texNames;

    /** Original file names — one per layer. */
    private final String[] fileNames;

    /** GL texture ID after buffering to GPU. */
    private int textureID = 0;

    /** Whether texture has been buffered to GPU. */
    private boolean buffered = false;

    /** Expected size of the texture (width and height in pixels). */
    private final int texSize;

    /** Sub-directory used to locate texture files. */
    private String texDir = "";

    // -----------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------

    /**
     * Creates TextureArray from the given file names using default TEX_SIZE.
     *
     * @param subDir    directory or entry where files are located
     * @param fileNames array of image file names (empty array for EMPTY placeholder)
     * @param texFmt    colorRGBA/depth format flag
     */
    public TextureArray(String subDir, String[] fileNames, Texture.Format texFmt) {
        this(subDir, fileNames, texFmt, TEX_SIZE);
    }

    /**
     * Creates TextureArray from the given file names with explicit texSize.
     *
     * @param subDir    directory or entry where files are located
     * @param fileNames array of image file names (empty array for EMPTY placeholder)
     * @param texFmt    colorRGBA/depth format flag
     * @param texSize   expected size of each texture tile (width and height in pixels)
     */
    public TextureArray(String subDir, String[] fileNames, Texture.Format texFmt, int texSize) {
        this.texFmt = texFmt;
        this.texDir = subDir;
        this.texSize = texSize;
        this.fileNames = fileNames;

        if (fileNames == null || fileNames.length == 0) {
            this.images   = new BufferedImage[0];
            this.texNames = new String[0];
        } else {
            this.images   = new BufferedImage[fileNames.length];
            this.texNames = new String[fileNames.length];
            for (int i = 0; i < fileNames.length; i++) {
                final String fn = fileNames[i];
                this.images[i]   = (fn == null || fn.isEmpty())
                        ? null
                        : ImageUtils.loadImage(subDir, fn);
                this.texNames[i] = (fn == null || fn.isEmpty() || !fn.contains("."))
                        ? "EMPTY"
                        : fn.substring(0, fn.lastIndexOf('.'));
                Texture.TEX_STORE.put(this.texNames[i], new TexValue(this, -1, 1));
            }
        }
    }

    // -----------------------------------------------------------------------
    // Buffering — instance
    // -----------------------------------------------------------------------

    /**
     * Buffers texture data to OpenGL as a GL_TEXTURE_2D_ARRAY.
     * Uses the pre-loaded {@link #images} array for layer data.
     */
    @Override
    public void bufferAll() {
        loadTexture();
        buffered = true;
    }

    // -----------------------------------------------------------------------
    // Buffering — static helpers
    // -----------------------------------------------------------------------

    /**
     * Re-buffers a TextureArray using a pre-built ByteBuffer per layer.
     * The buffer must contain all layers packed sequentially
     * (each layer: {@code ARRAY_TILE_SIZE * ARRAY_TILE_SIZE * 4} bytes).
     *
     * @param textureArray target TextureArray
     * @param imgDatBuff   packed image data (nullable — falls back to stored images)
     */
    public static void bufferAll(TextureArray textureArray, ByteBuffer imgDatBuff) {
        if (imgDatBuff != null && imgDatBuff.capacity() != 0) {
            textureArray.loadTexture(imgDatBuff);
        } else {
            textureArray.loadTexture();
        }textureArray.buffered = true;
    }

    /** Buffers the EMPTY texture (used for water etc.). */
    public static void bufferAllTextures() {
        EMPTY.bufferAll();
    }

    // -----------------------------------------------------------------------
    // Internal OpenGL loader — from stored images
    // -----------------------------------------------------------------------

    /**
     * Loads all layers from the stored {@link #images} array into a
     * {@code GL_TEXTURE_2D_ARRAY}.
     */
    private void loadTexture() {
        if (textureID != 0) {
            GL11.glDeleteTextures(textureID);
            textureID = 0;
        }

        textureID = GL11.glGenTextures();

        final int tileSize   = ARRAY_TILE_SIZE;
        final int layerCount = (images != null) ? images.length : 0;

        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, textureID);

        // Allocate storage for all layers
        GL12.glTexImage3D(
                GL30.GL_TEXTURE_2D_ARRAY,
                0,
                GL11.GL_RGBA8,
                tileSize, tileSize,
                Math.max(layerCount, 1),   // at least 1 to avoid empty array
                0,
                GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE,
                (ByteBuffer) null
        );

        for (int layer = 0; layer < layerCount; layer++) {
            final BufferedImage src = images[layer];
            if (src == null) {
                DSLogger.reportWarning("TextureArray layer " + layer
                        + " ('" + (fileNames[layer]) + "') is null — layer will be blank.", null);
                continue;
            }

            ByteBuffer buf = null;
            try {
                buf = ImageUtils.getImageDataBuffer(src, tileSize);
                GL12.glTexSubImage3D(
                        GL30.GL_TEXTURE_2D_ARRAY,
                        0,
                        0, 0,
                        layer,
                        tileSize, tileSize, 1,
                        GL11.GL_RGBA,
                        GL11.GL_UNSIGNED_BYTE,
                        buf
                );
            } finally {
                if (buf != null) {
                    MemoryUtil.memFree(buf);
                }
            }
        }

        applyArrayTextureParams();

        DSLogger.reportInfo("World texture array built: "
                + layerCount + " layers @ " + tileSize + "px.", null);
    }

    // -----------------------------------------------------------------------
    // Internal OpenGL loader — from pre-built ByteBuffer
    // -----------------------------------------------------------------------

    /**
     * Loads all layers from a pre-packed {@link ByteBuffer} into a
     * {@code GL_TEXTURE_2D_ARRAY}.
     * Each layer occupies {@code ARRAY_TILE_SIZE * ARRAY_TILE_SIZE * 4} bytes.
     *
     * @param packedBuf packed RGBA byte buffer with all layers
     */
    private void loadTexture(ByteBuffer packedBuf) {
        if (textureID != 0) {
            GL11.glDeleteTextures(textureID);
            textureID = 0;
        }

        textureID = GL11.glGenTextures();

        final int tileSize   = ARRAY_TILE_SIZE;
        final int layerBytes = tileSize * tileSize * 4;
        final int layerCount = (packedBuf != null && packedBuf.capacity() > 0)
                ? packedBuf.capacity() / layerBytes
                : 0;

        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, textureID);

        GL12.glTexImage3D(
                GL30.GL_TEXTURE_2D_ARRAY,
                0,
                GL11.GL_RGBA8,
                tileSize, tileSize,
                Math.max(layerCount, 1),
                0,
                GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE,
                (ByteBuffer) null
        );

        if (packedBuf != null && layerCount > 0) {
            for (int layer = 0; layer < layerCount; layer++) {
                // Slice the packed buffer to isolate this layer's data
                packedBuf.position(layer * layerBytes).limit((layer + 1) * layerBytes);
                final ByteBuffer slice = packedBuf.slice();

                GL12.glTexSubImage3D(
                        GL30.GL_TEXTURE_2D_ARRAY,
                        0,
                        0, 0,
                        layer,
                        tileSize, tileSize, 1,
                        GL11.GL_RGBA,
                        GL11.GL_UNSIGNED_BYTE,
                        slice
                );
            }
            packedBuf.position(0).limit(packedBuf.capacity()); // reset
            MemoryUtil.memFree(packedBuf);
        }

        applyArrayTextureParams();

        DSLogger.reportInfo("World texture array built from buffer: "
                + layerCount + " layers @ " + tileSize + "px.", null);
    }

    // -----------------------------------------------------------------------
    // Shared texture parameter setup
    // -----------------------------------------------------------------------

    private void applyArrayTextureParams() {
        GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_WRAP_S,     GL11.GL_REPEAT);
        GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_WRAP_T,     GL11.GL_REPEAT);
        GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
        GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL30.glGenerateMipmap(GL30.GL_TEXTURE_2D_ARRAY);
        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, 0);
    }

    // -----------------------------------------------------------------------
    // TextureIfc — bind overloads
    // -----------------------------------------------------------------------

    @Override
    public void bind(ShaderProgram shaderProgram, String textureUniformName) {
        bind(0, shaderProgram, textureUniformName);
    }

    @Override
    public void bind(int textureUnitNum, ShaderProgram shaderProgram, String textureUniformName) {
        if (textureID == 0) {
            DSLogger.reportWarning("TextureArray not buffered — call bufferAll() first.", null);
            return;
        }
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + textureUnitNum);
        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, textureID);
        GL20.glUniform1i(
                GL20.glGetUniformLocation(shaderProgram.getProgram(), textureUniformName),
                textureUnitNum);
    }

    // -----------------------------------------------------------------------
    // Unbind (static)
    // -----------------------------------------------------------------------

    /** Unbinds the texture from the default texture unit (0). */
    public static void unbind() {
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, 0);
    }

    /** Unbinds the texture from the specified texture unit. */
    public static void unbind(int unit) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, 0);
    }

    // -----------------------------------------------------------------------
    // TextureIfc — release
    // -----------------------------------------------------------------------

    @Override
    public void release() {
        if (textureID != 0) {
            GL11.glDeleteTextures(textureID);
            textureID = 0;
            buffered = false;
        }
    }

    // -----------------------------------------------------------------------
    // TextureIfc — getters
    // -----------------------------------------------------------------------

    @Override
    public int getTextureID() { return textureID; }

    @Override
    public boolean isBuffered() { return buffered; }

    /**
     * Returns the first texture name, or "EMPTY" if none.
     * Use {@link #getTexNames()} for the full array.
     */
    @Override
    public String getTexName() {
        return (texNames != null && texNames.length > 0) ? texNames[0] : "EMPTY";
    }

    /** @return all texture names (one per layer) */
    public String[] getTexNames() { return texNames; }

    /** @return all file names (one per layer) */
    public String[] getFileNames() { return fileNames; }

    /** @return all loaded images (one per layer) */
    public BufferedImage[] getImages() { return images; }

    @Override
    public Texture.Format getTexFmt() { return texFmt; }
}
