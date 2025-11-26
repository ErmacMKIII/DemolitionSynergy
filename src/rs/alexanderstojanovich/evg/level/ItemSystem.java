package rs.alexanderstojanovich.evg.level;

import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import org.magicwerk.brownies.collections.Key1List;
import rs.alexanderstojanovich.evg.chunk.Chunk;
import rs.alexanderstojanovich.evg.core.Camera;
import rs.alexanderstojanovich.evg.light.LightSources;
import rs.alexanderstojanovich.evg.models.Model;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;

/**
 * Item system. Contains rendered items on the ground (on blocks).
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class ItemSystem {

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
     * Preprocesses (weapon) items from visible chunks to meet specific conditions.
     *
     * @param vqueue visible (chunk) queue
     * @param camera game camera
     */
    public void preprocessItems(IList<Integer> vqueue, Camera camera) {
        // Clear items that cannot be rendered
        selectedWeaponItems.removeIf((x -> !camera.doesSeeEff(x, 7.5f)));

        // For all weapon items
        for (int chunkId : vqueue) {
            // if visible queue contains chunk (determined from chunk function)
            // and camera does see the model (eff means better method, more effective)
            for (Model weapon : allWeaponItems.getAllByKey1(chunkId)) {
                synchronized (selectedWeaponItems) {
                    selectedWeaponItems.addIfAbsent(weapon);
                }
            }
        }
    }

    /**
     * Render selected items.
     *
     * @param lightSources light sources (sunlight, player light, block light etc.)
     * @param shaderProgram shader program used for rendering (should be main shader)
     */
    public void render(LightSources lightSources, ShaderProgram shaderProgram) {
        synchronized (selectedWeaponItems) {
            // Render loop
            for (Model model : selectedWeaponItems) {
                if (!model.isBuffered()) {
                    // Buffer all to GPU
                    model.bufferAll();
                }

                // Draw method
                model.render(lightSources, shaderProgram);
            }
        }
    }

    /**
     * Render selected items. (Contour, animated)
     *
     * @param lightSources light sources (sunlight, player light, block light etc.)
     * @param shaderProgram shader program used for rendering (should be main shader)
     */
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

}
