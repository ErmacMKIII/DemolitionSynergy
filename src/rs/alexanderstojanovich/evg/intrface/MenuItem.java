/*
 * Copyright (C) 2022 coas91@rocketmail.com
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
package rs.alexanderstojanovich.evg.intrface;

import rs.alexanderstojanovich.evg.shaders.ShaderProgram;

/**
 * Menu item. Used widely in menus.
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class MenuItem {

    public final Intrface intrface;
    protected final DynamicText keyText;

    protected final MenuValue menuValue;
    protected final Menu.EditType editType;

    /**
     * Game menu item
     *
     * @param intrface intrface with game object (to access assets)
     * @param string display text
     * @param editType editable type {NoValue, SingleValue, MultiValue}
     * @param menuValue actual menu value
     */
    public MenuItem(Intrface intrface, String string, Menu.EditType editType, MenuValue menuValue) {
        this.intrface = intrface;
        this.keyText = new DynamicText(this.intrface.gameObject.GameAssets.FONT, string, intrface);
        this.editType = editType;
        this.menuValue = menuValue;
    }

    public MenuValue getMenuValue() {
        return menuValue;
    }

    public Menu.EditType getEditType() {
        return editType;
    }

    /**
     * Render this menu item in the menu.
     *
     * @param intrface intrface
     * @param shaderProgram shader program to use.
     */
    public void render(Intrface intrface, ShaderProgram shaderProgram) {
        if (!keyText.isBuffered()) {
            keyText.bufferSmart(intrface);
        }
        keyText.render(intrface, shaderProgram);

        if (menuValue != null) {
            if (!menuValue.getValueText().isBuffered()) {
                menuValue.getValueText().bufferSmart(intrface);
            }
            menuValue.getValueText().render(intrface, shaderProgram);
        }
    }

    /**
     * Deletes GL buffers used by this menu item. Call from the Renderer.
     */
    public void release() {
        this.keyText.release();
    }

}
