package rs.alexanderstojanovich.evg.texture;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import rs.alexanderstojanovich.evg.main.Configuration;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Common interface for Texture and TextureArray.
 * Defines the contract for buffering, binding, unbinding and releasing OpenGL textures.
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public interface TextureIfc {

    /** Texture atlas grid size (number of textures per row/column). */
    static final int TEX_SIZE = Configuration.getInstance().getTextureSize();
    /** Empty texture placeholder. */
    static final Map<String, TexValue> TEX_STORE = new LinkedHashMap<>();

    /** Texture format (colorRGBA/depth) flag. */
    public static enum Format {
        NONE, RGB5_A1, RGBA8, DEPTH24
    }

    /** Texture data type for loading. */
    public static enum Type {
        UNSINGED_BYTE, FLOAT
    }

    // -----------------------------------------------------------------------
    // Static lookups
    // -----------------------------------------------------------------------

    /** Gets texture by name or returns the empty texture if not found. */
    static TextureIfc getOrDefault(String texName) {
        return TEX_STORE.getOrDefault(texName, Texture.EMPTY_VALUE).texture;
    }

    /** Gets texture index in atlas by name or returns -1 if not found. */
    static int getOrDefaultIndex(String texName) {
        return TEX_STORE.getOrDefault(texName, Texture.EMPTY_VALUE).value;
    }

    /** Gets grid size of atlas by name or returns 1 if not found. */
    static int getOrDefaultGridSize(String texName) {
        return TEX_STORE.getOrDefault(texName, Texture.EMPTY_VALUE).gridSize;
    }
    // -----------------------------------------------------------------------
    // Buffering
    // -----------------------------------------------------------------------

    /**
     * Buffers texture data to OpenGL.
     * If image data is null texture is generated empty.
     */
    void bufferAll();

    // -----------------------------------------------------------------------
    // Binding
    // -----------------------------------------------------------------------

    /**
     * Binds this texture as active for use (unit 0).
     *
     * @param shaderProgram      provided shader program
     * @param textureUniformName texture uniform name in the fragment shader
     */
    void bind(ShaderProgram shaderProgram, String textureUniformName);

    /**
     * Binds this texture to a specific texture unit (0–7).
     *
     * @param textureUnitNum     texture unit number
     * @param shaderProgram      provided shader program
     * @param textureUniformName texture uniform name in the fragment shader
     */
    void bind(int textureUnitNum, ShaderProgram shaderProgram, String textureUniformName);

    // -----------------------------------------------------------------------
    // Release
    // -----------------------------------------------------------------------

    /**
     * Releases the OpenGL texture.
     */
    void release();

    // -----------------------------------------------------------------------
    // Getters
    // -----------------------------------------------------------------------

    /**
     * @return OpenGL texture ID
     */
    int getTextureID();

    /**
     * @return true if texture has been buffered to OpenGL
     */
    boolean isBuffered();

    /**
     * @return texture name (alias)
     */
    String getTexName();

    /**
     * @return texture format
     */
    Texture.Format getTexFmt();

}
