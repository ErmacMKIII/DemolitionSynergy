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

/**
 * Item used in a console. Of previous command inputs.
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class HistoryItem {

    protected Command cmd;
    protected final DynamicText cmdText;
    protected final Quad quad;
    public final Console console;

    public HistoryItem(Console console, Command command) throws Exception {
        this.console = console;
        this.cmd = command;
        this.cmdText = new DynamicText(console.intrface.gameObject.GameAssets.FONT, "", new Vector2f(), 18, 18, console.intrface);
        this.quad = new Quad(14, 14, console.intrface.gameObject.GameAssets.LIGHT_BULB, console.intrface);
    }

    /**
     * Build command text. Constructs text of this item.
     */
    protected void buildCmdText() {
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
                break;
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
            default:
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
     * @param intrface intrface
     * @param shaderProgram shader program to use
     */
    public void render(Intrface intrface, ShaderProgram shaderProgram) {
        buildCmdText();
        if (!cmdText.isBuffered()) {
            cmdText.bufferSmart(intrface);
        }
        cmdText.render(intrface, shaderProgram);
        // ------------------------------------------------------------------------------------------------------        
        if (!quad.isBuffered()) {
            quad.bufferSmart(intrface);
        }
        quad.color = Console.StatusColor(cmd.status);
        quad.render(intrface, shaderProgram);
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

}
