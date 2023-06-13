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
package rs.alexanderstojanovich.evg.intrface;

import org.joml.Vector2f;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;

/**
 * Item used in a console. Of previous command inputs.
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class HistoryItem {

    protected Command cmd;
    protected final DynamicText cmdText = new DynamicText(Texture.FONT, "", new Vector2f(), 18, 18);
    protected Quad quad = new Quad(14, 14, Texture.LIGHT_BULB);
    protected int index = 0;

    public HistoryItem(int index, Command command) {
        this.index = index;
        this.cmd = command;
        this.buildCmdText();
        cmdText.pos.x = -1.0f;
        // index + 1 is because it is rendered above "] " where in text is
        cmdText.pos.y = Text.LINE_SPACING * (cmdText.getRelativeCharHeight() + (this.index + this.cmdText.numberOfLines()) * cmdText.getRelativeCharHeight()) * cmdText.scale;
        cmdText.setAlignment(Text.ALIGNMENT_LEFT);
        cmdText.alignToNextChar();
    }

    /**
     * Build command text. Constructs text of this item.
     */
    private void buildCmdText() {
        StringBuilder sb = new StringBuilder();
        if (cmd.target == Command.Target.ERROR) {
            sb.append(cmd.input);
        } else {
            sb.append(cmd.target);
        }
        String connector = "";
        switch (cmd.mode) {
            case GET:
                connector = " IS ";
                break;
            case SET:
                connector = " => ";
                break;
            default:
        }
        sb.append(connector);
        switch (cmd.mode) {
            case GET:
                if (cmd.target == Command.Target.ERROR) {
                    sb.append("Invalid Command");
                } else {
                    sb.append(cmd.result);
                }
                break;
            case SET:
                if (cmd.target == Command.Target.ERROR) {
                    sb.append("Invalid Command");
                } else {
                    cmd.args.forEach(a -> sb.append(a.toString()).append(" "));
                }
                break;

        }
        cmdText.setContent(sb.toString());
    }

    /**
     * Renders this history item in the console (in interface).
     *
     * @param shaderProgram shader program to use
     */
    public void render(ShaderProgram shaderProgram) {
        buildCmdText();
        if (!cmdText.isBuffered()) {
            cmdText.bufferSmart();
        }
        cmdText.render(shaderProgram);
        // ------------------------------------------------------------------------------------------------------        
        if (!quad.isBuffered()) {
            quad.bufferSmart();
        }
        quad.pos.x = cmdText.pos.x + (cmdText.getRelativeWidth() + cmdText.getRelativeCharWidth()) * cmdText.scale;
        quad.pos.y = cmdText.pos.y;
        quad.color = Console.StatusColor(cmd.status);
        quad.render(shaderProgram);
    }

    /*
    *  Delete all GL Buffers from this component.
     */
    public void release() {
        this.cmdText.release();
        this.quad.release();
    }

    public DynamicText getCmdText() {
        return cmdText;
    }

    public Command getCmd() {
        return cmd;
    }

    public void setCmd(Command cmd) {
        this.cmd = cmd;
    }

    public DynamicText getDynamicText() {
        return cmdText;
    }

    public Quad getQuad() {
        return quad;
    }

    public void setQuad(Quad quad) {
        this.quad = quad;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
        this.buildCmdText();
        // index + 1 is because it is rendered above "] " where in text is
        cmdText.pos.y = Text.LINE_SPACING * (cmdText.getRelativeCharHeight() + (this.index + this.cmdText.numberOfLines()) * cmdText.getRelativeCharHeight()) * cmdText.scale;
    }

}
