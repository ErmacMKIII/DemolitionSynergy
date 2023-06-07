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
import rs.alexanderstojanovich.evg.texture.Texture;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class MenuItem {

    protected final DynamicText keyText;

    protected final MenuValue menuValue;
    protected final Menu.EditType editType;

    public MenuItem(String string, Menu.EditType editType, MenuValue menuValue) {
        this.keyText = new DynamicText(Texture.FONT, string);
        this.editType = editType;
        this.menuValue = menuValue;
    }

    public MenuValue getMenuValue() {
        return menuValue;
    }

    public Menu.EditType getEditType() {
        return editType;
    }

    public void render(ShaderProgram shaderProgram) {
        if (!keyText.isBuffered()) {
            keyText.bufferAll();
        }
        keyText.render(shaderProgram);

        if (menuValue != null) {
            if (!menuValue.getValueText().isBuffered()) {
                menuValue.getValueText().bufferAll();
            }
            menuValue.getValueText().render(shaderProgram);
        }
    }

}
