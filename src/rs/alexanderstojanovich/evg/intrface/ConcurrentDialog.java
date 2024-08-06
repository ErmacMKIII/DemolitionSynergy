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

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import org.joml.Vector2f;
import rs.alexanderstojanovich.evg.main.GameObject;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.GlobalColors;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public abstract class ConcurrentDialog extends Dialog { // execution is done in another thread                

    public ConcurrentDialog(Texture texture, Vector2f pos, String question, String success, String fail) throws Exception {
        super(texture, pos, question, success, fail);
    }

    @Override
    protected abstract ExecStatus execute(String arg); // we need to override this upon creation of the dialog     

    // what is happening internally on command
    @Override
    protected void onCommand() {
        execStatus = ExecStatus.NOT_RUNNING;
        Runnable task = () -> {
            execStatus = ExecStatus.IN_PROGRESS;
            if (!input.toString().equals("")) {
                execStatus = execute(input.toString());
                if (execStatus == ExecStatus.SUCCESS) {
                    dialog.setContent(success);
                    dialog.color = GlobalColors.GREEN_RGBA;
                } else if (execStatus == ExecStatus.FAILURE) {
                    dialog.setContent(fail);
                    dialog.color = GlobalColors.RED_RGBA;
                }
            } else {
                dialog.setContent("");
                enabled = false;
            }
            input.setLength(0);
            done = true;
        };

        GameObject.TASK_EXECUTOR.execute(task);
    }

    /**
     * Execute async command and wait on result (blocking).
     *
     * @param arg string argument
     * @return execution status
     */
    public Future<ExecStatus> executeAsync(String arg) {
        Callable<ExecStatus> task = () -> {
            execStatus = ExecStatus.IN_PROGRESS;
            return execute(arg);
        };

        return GameObject.TASK_EXECUTOR.submit(task);
    }

}
