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
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class HistoryItem {

    protected Command cmd;
    protected final DynamicText cmdText = new DynamicText(Texture.FONT, "", new Vector2f(), 18, 18);
    protected Quad quad;

    public HistoryItem(Command command, Quad quad) {
        this.cmd = command;
        this.quad = quad;

        cmdText.pos.y += (0.5f - cmdText.getRelativeCharHeight()) * Text.LINE_SPACING;

        cmdText.setAlignment(Text.ALIGNMENT_LEFT);
        cmdText.alignToNextChar();
    }

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

    public void render(Vector2f pos, ShaderProgram shaderProgram) {
        buildCmdText();
        cmdText.pos.x = pos.x;
        cmdText.pos.y = pos.y;

        if (!cmdText.isBuffered()) {
            cmdText.bufferAll();
        }
        cmdText.render(shaderProgram);
        // ------------------------------------------------------------------------------------------------------
        quad.getPos().x = cmdText.pos.x + cmdText.getRelativeCharWidth() * (cmdText.content.length() + 1);
        quad.getPos().y = cmdText.pos.y;

        if (!quad.isBuffered()) {
            quad.bufferAll();
        }
        quad.color = Console.StatusColor(cmd.status);
        quad.render(shaderProgram);
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

}
