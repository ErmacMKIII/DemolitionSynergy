/* 
 * Copyright (C) 2020 Alexander Stojanovich <coas91@rocketmail.com>
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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.joml.Vector2f;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.Vector3fColors;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public abstract class ConcurrentDialog extends Dialog { // execution is done in another thread                

    private Runnable command;
    public static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private Thread dialogThread; // thread which executes command     

    public ConcurrentDialog(Texture texture, Vector2f pos, String question, String success, String fail) {
        super(texture, pos, question, success, fail);
    }

    @Override
    protected abstract boolean execute(String command); // we need to override this upon creation of the dialog     

    // what is happening internally on command
    @Override
    protected void onCommand() {
        command = () -> {
            if (!input.toString().equals("")) {
                boolean execStatus = execute(input.toString());
                if (execStatus) {
                    dialog.setContent(success);
                    dialog.color = Vector3fColors.GREEN;
                } else {
                    dialog.setContent(fail);
                    dialog.color = Vector3fColors.RED;
                }
            } else {
                dialog.setContent("");
                enabled = false;
            }
            input.setLength(0);
            done = true;
        };
        EXECUTOR.execute(command);
    }

    public Runnable getCommand() {
        return command;
    }

    public Thread getDialogThread() {
        return dialogThread;
    }

}
