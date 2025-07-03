/*
 * Copyright (C) 2024 Parisi Alessandro - alessandro.parisi406@gmail.com
 * This file is part of VirtualizedFX (https://github.com/palexdev/VirtualizedFX)
 *
 * VirtualizedFX is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 *
 * VirtualizedFX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with VirtualizedFX. If not, see <http://www.gnu.org/licenses/>.
 */

package utils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.utils.fx.FXCollectors;
import io.github.palexdev.virtualizedfx.base.VFXContainer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Window;
import org.scenicview.ScenicView;
import org.testfx.api.FxRobot;

public class Utils {

    //================================================================================
    // Constructors
    //================================================================================
    private Utils() {}

    //================================================================================
    // Static Methods
    //================================================================================
    public static void debugView(FxRobot robot, Node node) {
        if (robot != null) {
            robot.interact(() -> ScenicView.show(node.getScene()));
            return;
        }
        ScenicView.show(node.getScene());
    }

    public static void debugView(FxRobot robot, Node node, long sleepMillis) {
        debugView(robot, node);
        sleep(sleepMillis);
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (Exception ignored) {}
    }

    public static void setWindowSize(Node node, double size) {
        setWindowSize(node, size, size);
    }

    public static void setWindowSize(Node node, double w, double h) {
        Scene scene = node.getScene();
        if (scene == null) throw new NullPointerException("Node is not in a Scene");
        Window window = scene.getWindow();
        if (window == null) throw new NullPointerException("Scene is not in a Window");
        if (w >= 0) window.setWidth(w);
        if (h >= 0) window.setHeight(h);
    }

    public static void setWindowPos(Node node, double pos) {
        setWindowPos(node, pos, pos);
    }

    public static void setWindowPos(Node node, double x, double y) {
        Scene scene = node.getScene();
        if (scene == null) throw new NullPointerException("Node is not in a Scene");
        Window window = scene.getWindow();
        if (window == null) throw new NullPointerException("Scene is not in a Window");
        if (!Double.isNaN(x)) window.setX(x);
        if (!Double.isNaN(y)) window.setY(y);
    }

    public static <T> void removeAll(VFXContainer<T> container, int... indexes) {
        removeAll(container.getItems(), indexes);
    }

    public static <T> void removeAll(VFXContainer<T> container, IntegerRange range) {
        removeAll(container.getItems(), range);
    }

    public static <T> void removeAll(ObservableList<T> list, int... indexes) {
        List<T> rem = Arrays.stream(indexes)
            .mapToObj(list::get)
            .toList();
        list.removeAll(rem);
    }

    public static <T> void removeAll(ObservableList<T> list, IntegerRange range) {
        List<T> rem = IntStream.rangeClosed(range.getMin(), range.getMax())
            .mapToObj(list::get)
            .toList();
        list.removeAll(rem);
    }

    public static ObservableList<Integer> items(int cnt) {
        return FXCollections.observableArrayList(IntegerRange.expandRangeToArray(0, cnt - 1));
    }

    public static ObservableList<Integer> items(int start, int cnt) {
        return IntStream.range(0, cnt)
            .mapToObj(i -> start + i)
            .collect(FXCollectors.toList());
    }

    public static boolean isTouchSupported() {
        String env = System.getenv().getOrDefault("TOUCH_SUPPORTED", "false");
        return Boolean.parseBoolean(env);
    }
}
