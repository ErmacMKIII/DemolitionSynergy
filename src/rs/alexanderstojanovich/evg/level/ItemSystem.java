package rs.alexanderstojanovich.evg.level;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryUtil;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import org.magicwerk.brownies.collections.Key1List;
import rs.alexanderstojanovich.evg.chunk.Chunk;
import rs.alexanderstojanovich.evg.core.Camera;
import rs.alexanderstojanovich.evg.light.LightSources;
import rs.alexanderstojanovich.evg.models.Model;
import rs.alexanderstojanovich.evg.models.Vertex;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.util.DSLogger;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Item system. Contains rendered items on the ground (on blocks).
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class ItemSystem {

    /**
     * Some count used in buffers (Float Buffer & Integer Buffer). It references to max count of weapons.
     */
    public static final int SOME_COUNT = 256;

    /**
     * Some count for vertices. Max count of vertices for the model.
     */
    public static final int SOME_VERTEX_COUNT = 4096;

    /**
     * Some count for indices. Max count of indices for the model.
     */
    public static final int SOME_INDEX_COUNT = 16384;

    /**
     * Buffer to store (vec3f) vertices
     */
    protected static FloatBuffer fb;
    /**
     * Buffer to store (int) indices
     */
    protected static IntBuffer ib;

    /**
     * Vertex buffer object ID
     */
    private int vbo;
    /**
     * Index buffer object ID
     */
    private int ibo;

    /**
     * Is (item system) ready for rendering
     */
    private boolean buffered;

    /**
     * Weapons on the ground. Contains all the weapons.
     */
    public final Key1List<Model, Integer> allWeaponItems = new Key1List.Builder<Model, Integer>()
            .withKey1Duplicates(true)
            .withKey1Map(x -> Chunk.chunkFunc(x.pos))
            .build();
    /**
     * Selected (visible) weapons on the ground. Visible based on player position.
     */
    public final IList<Model> selectedWeaponItems = new GapList<>();

    /**
     * Buffer vertices of the items.
     *
     * @return success or failure of operation
     */
    public boolean bufferVertices() {
        int someSize = SOME_COUNT * SOME_VERTEX_COUNT * Vertex.SIZE;
        // Allocate memory for the vertex data buffer
        if (fb == null || fb.capacity() == 0) {
            fb = MemoryUtil.memCallocFloat(someSize);
        } else if (fb.capacity() < someSize) {
            fb = MemoryUtil.memRealloc(fb, someSize);
        }

        // Set buffer position and limit
        fb.position(0);
        fb.limit(someSize);

        if (MemoryUtil.memAddressSafe(fb) == MemoryUtil.NULL) {
            DSLogger.reportError("Could not allocate memory address!", null);
            throw new RuntimeException("Could not allocate memory address!");
        }

        // Fill the vertex buffer with data
        synchronized (selectedWeaponItems) {
            for (Model model : selectedWeaponItems) {
                for (Vertex vertex : model.getVertices()) {
                    if (vertex.isEnabled()) {
                        fb.put(vertex.getPos().x)
                                .put(vertex.getPos().y)
                                .put(vertex.getPos().z)
                                .put(vertex.getNormal().x)
                                .put(vertex.getNormal().y)
                                .put(vertex.getNormal().z)
                                .put(vertex.getUv().x)
                                .put(vertex.getUv().y);
                    }
                }
            }
        }
        if (fb.position() != 0) {
            fb.flip();
        }

        if (vbo == 0) {
            vbo = GL15.glGenBuffers();
        }

        // Bind the vertex array and vertex buffer
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

        // Transfer vertex data to GPU
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, fb, GL15.GL_STATIC_DRAW);

        // Enable vertex attributes
        GL20.glEnableVertexAttribArray(0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glEnableVertexAttribArray(2);

        // Specify vertex attribute pointers
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, Vertex.SIZE * 4, 0); // Position
        GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, Vertex.SIZE * 4, 12); // Normal
        GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, Vertex.SIZE * 4, 24); // UV

        GL20.glDisableVertexAttribArray(0);
        GL20.glDisableVertexAttribArray(1);
        GL20.glDisableVertexAttribArray(2);

        // Unbind vertex array and vertex buffer
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        return true;
    }

    /**
     * Buffer indices of the items.
     *
     * @return success or failure of operation
     */
    public boolean bufferIndices() {
        int someSize = SOME_COUNT * SOME_INDEX_COUNT;
        // Allocate memory for index data
        if (ib == null || ib.capacity() == 0) {
            ib = MemoryUtil.memCallocInt(someSize);
        } else if (ib.capacity() < someSize) {
            ib = MemoryUtil.memRealloc(ib, someSize);
        }
        ib.position(0);
        ib.limit(someSize);

        if (MemoryUtil.memAddressSafe(ib) == MemoryUtil.NULL) {
            DSLogger.reportError("Could not allocate memory address!", null);
            throw new RuntimeException("Could not allocate memory address!");
        }

        // Track vertex offset for each model
        int vertexOffset = 0;

        // Fill the index buffer with data
        synchronized (selectedWeaponItems) {
            for (Model model : selectedWeaponItems) {
                // Add indices with vertex offset applied
                for (Integer index : model.getIndices()) {
                    ib.put(index + vertexOffset);
                }

                // Update vertex offset for next model
                // Count only enabled vertices
                int enabledVertexCount = 0;
                for (Vertex vertex : model.getVertices()) {
                    if (vertex.isEnabled()) {
                        enabledVertexCount++;
                    }
                }
                vertexOffset += enabledVertexCount;
            }
        }
        ib.flip();

        // Generate index buffer object if not already generated
        if (ibo == 0) {
            ibo = GL15.glGenBuffers();
        }

        // Bind the index buffer
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);

        // Transfer index data to GPU
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, ib, GL15.GL_STATIC_DRAW);

        return true;
    }

    /**
     * Buffer everything. Buffer Vertices first and then indices if former succeeded.
     */
    public void bufferAll() { // explicit call to buffer unbuffered before the rendering
        buffered = bufferVertices() && bufferIndices();
    }

    /**
     * Preprocesses (weapon) items from visible chunks to meet specific conditions.
     *
     * @param vqueue visible (chunk) queue
     * @param camera game camera
     */
    public void preprocessItems(IList<Integer> vqueue, Camera camera) {
        // Clear items that cannot be rendered
        boolean modified = false;
        synchronized (selectedWeaponItems) {
            modified |= selectedWeaponItems.removeIf((x -> !camera.doesSeeEff(x, 7.5f)));
        }

        // For all weapon items
        for (int chunkId : vqueue) {
            // if visible queue contains chunk (determined from chunk function)
            // and camera does see the model (eff means better method, more effective)
            for (Model weapon : allWeaponItems.getAllByKey1(chunkId)) {
                synchronized (selectedWeaponItems) {
                    modified |= selectedWeaponItems.addIfAbsent(weapon);
                }
            }
        }

        // If anything modified reset buffered flag
        if (modified) {
            this.buffered = false;
        }
    }

    /**
     * Render selected items.
     *
     * @param lightSources light sources (sunlight, player light, block light etc.)
     * @param shaderProgram shader program used for rendering (should be main shader)
     */
    public void render(LightSources lightSources, ShaderProgram shaderProgram) {
        // If whole system is buffered
        if (!buffered) {
            // If not buffer it
            bufferAll();
        }

        synchronized (selectedWeaponItems) {
            // Draw method
            Model.render(selectedWeaponItems, vbo, ibo, lightSources, shaderProgram);
        }
    }

    /**
     * Render selected items. (Contour, animated)
     *
     * @param lightSources light sources (sunlight, player light, block light etc.)
     * @param shaderProgram shader program used for rendering (should be main shader)
     */
    @Deprecated
    public void renderContour(LightSources lightSources, ShaderProgram shaderProgram) {
        synchronized (selectedWeaponItems) {
            // Render loop
            for (Model model : selectedWeaponItems) {
                if (!model.isBuffered()) {
                    // Buffer all to GPU
                    model.bufferAll();
                }

                // Draw method
                model.renderContour(lightSources, shaderProgram);
            }
        }
    }

    /**
     * Clear both all weapon list and selected weapon list
     */
    public void clear() {
        allWeaponItems.clear();
        selectedWeaponItems.clear();
    }

    /**
     * Get list of all weapon item (on the ground)
     *
     * @return all weapon item list
     */
    public IList<Model> getAllWeaponItems() {
        return allWeaponItems;
    }

    /**
     * Get list of selected weapon items. Preprocessed.
     *
     * @return selected weapon item list.
     */
    public IList<Model> getSelectedWeaponItems() {
        return selectedWeaponItems;
    }

    /**
     * Delete the buffers. Requires OpenGL context.
     */
    public void release() {
        if (buffered) {
            GL15.glDeleteBuffers(vbo);
            GL15.glDeleteBuffers(ibo);
        }
        buffered = false;
    }

}
