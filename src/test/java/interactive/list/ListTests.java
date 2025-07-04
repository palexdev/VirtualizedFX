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

package interactive.list;

import java.util.Comparator;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import cells.TestCell;
import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.controls.Label;
import io.github.palexdev.mfxcore.controls.SkinBase;
import io.github.palexdev.mfxcore.utils.RandomUtils;
import io.github.palexdev.virtualizedfx.cells.base.VFXCell;
import io.github.palexdev.virtualizedfx.enums.BufferSize;
import io.github.palexdev.virtualizedfx.list.VFXList;
import io.github.palexdev.virtualizedfx.list.VFXListHelper;
import io.github.palexdev.virtualizedfx.list.VFXListSkin;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import utils.Utils;

import static interactive.list.ListTestUtils.List;
import static interactive.list.ListTestUtils.assertState;
import static io.github.palexdev.virtualizedfx.utils.Utils.INVALID_RANGE;
import static model.User.faker;
import static model.User.users;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static utils.TestFXUtils.*;
import static utils.Utils.items;
import static utils.Utils.setWindowSize;

@ExtendWith(ApplicationExtension.class)
public class ListTests {

    @Start
    void start(Stage stage) {stage.show();}

    @BeforeEach
    void setup() {
        resetCounters();
    }

    @Test
    void testInitAndGeometry(FxRobot robot) {
        StackPane pane = setupStage();
        List list = new List(items(20));
        robot.interact(() -> pane.getChildren().add(list));

        assertState(list, IntegerRange.of(0, 16)); // ceil(400/32) + 4 (buffer) -> 17
        assertCounter(17, 1, 17, 17, 0, 0, 0);

        // Expand and test again
        robot.interact(() -> setWindowSize(pane, -1, 600));
        assertState(list, IntegerRange.of(0, 19)); // ceil(600/32) + 4 (buffer) -> 23 (Items num 20!!!)
        assertCounter(3, 1, 3, 3, 0, 0, 0);

        // Shrink and test again
        robot.interact(() -> setWindowSize(pane, -1, 300));
        assertState(list, IntegerRange.of(0, 13)); // ceil(300/32) + 4 (buffer) -> 14
        assertCounter(0, 1, 0, 0, 0, 6, 0);

        // Expand width and test again
        // Should be the same as before but with one extra layout
        robot.interact(() -> setWindowSize(pane, 500, -1));
        assertState(list, IntegerRange.of(0, 13));
        assertCounter(0, 1, 0, 0, 0, 0, 0);

        // Edge case set height to 0
        robot.interact(() -> {
            list.setMinHeight(0);
            list.setPrefHeight(0);
            list.setMaxHeight(0);
            setWindowSize(pane, -1, 0);
        });
        assertState(list, INVALID_RANGE);
        assertCounter(0, 0, 0, 0, 0, 14, 10); // 6 were already in cache, so 10 disposals
    }

    @Test
    void testInitAndGeometryAtBottom(FxRobot robot) {
        StackPane pane = setupStage();
        List list = new List(items(50));
        robot.interact(() -> {
            pane.getChildren().add(list);
            list.scrollToLast();
        });

        // Check vPos!!
        assertEquals(1200.0, list.getVPos());

        assertState(list, IntegerRange.of(33, 49)); // ceil(400/32) + 4 (buffer) -> 17
        assertCounter(17, 1, 17, 17, 0, 0, 0);

        // Expand and check counter, state and pos
        robot.interact(() -> {
            Window w = list.getScene().getWindow();
            w.setHeight(600);
        });
        assertEquals(1000.0, list.getVPos());
        assertState(list, IntegerRange.of(27, 49)); // ceil(600/32) + 4 (buffer) -> 23
        assertCounter(6, 1, 6, 6, 0, 0, 0);

        // Shrink and check again
        robot.interact(() -> {
            Window w = list.getScene().getWindow();
            w.setHeight(300);
        });
        assertEquals(1000.0, list.getVPos());
        assertState(list, IntegerRange.of(29, 42));
        assertCounter(0, 1, 0, 0, 0, 9, 0);
    }

    @Test
    void testPopulateCache(FxRobot robot) {
        StackPane pane = setupStage();
        List list = new List(items(20));
        list.populateCache(); // 10 by default
        counter.reset(); // Cells created by cache do not count
        robot.interact(() -> pane.getChildren().add(list));

        assertState(list, IntegerRange.of(0, 16));
        assertCounter(7, 1, 17, 17, 10, 0, 0);

        robot.interact(() -> setWindowSize(pane, -1, 600));
        assertState(list, IntegerRange.of(0, 19));
        assertCounter(3, 1, 3, 3, 0, 0, 0);

        robot.interact(() -> setWindowSize(pane, -1, 300));
        assertState(list, IntegerRange.of(0, 13));
        assertCounter(0, 1, 0, 0, 0, 6, 0);

        // Re-expand and cache should put in some work
        robot.interact(() -> setWindowSize(pane, -1, 600));
        assertState(list, IntegerRange.of(0, 19));
        assertCounter(0, 1, 0, 0, 6, 0, 0); // No index nor item updates!
    }

    @Test
    void testScroll(FxRobot robot) {
        StackPane pane = setupStage();
        List list = new List(items(100));
        list.populateCache(); // 10 by default
        robot.interact(() -> pane.getChildren().add(list));

        // Test init, why not
        assertState(list, IntegerRange.of(0, 16));
        assertCounter(17, 1, 17, 17, 10, 0, 0);

        robot.interact(() -> list.setVPos(100));
        assertState(list, IntegerRange.of(1, 17)); // [0, 16] -> [1, 17] = 1 update
        assertCounter(0, 1, 1, 1, 0, 0, 0);

        robot.interact(() -> list.setVPos(Double.MAX_VALUE));
        assertState(list, IntegerRange.of(83, 99)); // [1, 17] -> [83, 99] = big jump, all cells updated +17
        assertCounter(0, 1, 17, 17, 0, 0, 0);

        robot.interact(() -> {
            list.setItems(items(50));
            list.setVPos(0);
            counter.reset();
            list.setVPos(600.0);
        });
        assertState(list, IntegerRange.of(16, 32));
        assertCounter(0, 1, 16, 16, 0, 0, 0); // Only 16 updated because index 16 is common
    }

    @Test
    void testBufferChangeTop(FxRobot robot) {
        StackPane pane = setupStage();
        List list = new List(items(20));
        VFXListHelper<Integer, VFXCell<Integer>> helper = list.getHelper();
        robot.interact(() -> pane.getChildren().add(list));

        // Standard buffer -> 17 cells (4 buffer)
        assertState(list, IntegerRange.of(0, 16));
        assertEquals(13, helper.visibleNum());
        assertEquals(17, helper.totalNum());
        assertCounter(17, 1, 17, 17, 0, 0, 0);

        // Small buffer -> 15 cells (2 buffer)
        robot.interact(() -> list.setBufferSize(BufferSize.SMALL));
        assertState(list, IntegerRange.of(0, 14));
        assertEquals(13, helper.visibleNum());
        assertEquals(15, helper.totalNum());
        assertCounter(0, 1, 0, 0, 0, 2, 0);

        // Big buffer -> 19 cells (6 buffer)
        robot.interact(() -> list.setBufferSize(BufferSize.BIG));
        assertState(list, IntegerRange.of(0, 18));
        assertEquals(13, helper.visibleNum());
        assertEquals(19, helper.totalNum());
        assertCounter(2, 1, 2, 2, 2, 0, 0);
    }

    @Test
    void testBufferChangeBottom(FxRobot robot) {
        StackPane pane = setupStage();
        List list = new List(items(20));
        VFXListHelper<Integer, VFXCell<Integer>> helper = list.getHelper();
        robot.interact(() -> {
            pane.getChildren().add(list);
            list.setVPos(Double.MAX_VALUE);
        });

        // Check vPos!!!
        assertEquals(240, list.getVPos());

        // Standard buffer -> 17 cells (4 buffer)
        assertState(list, IntegerRange.of(3, 19));
        assertEquals(13, helper.visibleNum());
        assertEquals(17, helper.totalNum());
        assertCounter(17, 1, 17, 17, 0, 0, 0);

        // Small buffer -> 15 cells (2 buffer)
        robot.interact(() -> list.setBufferSize(BufferSize.SMALL));
        assertState(list, IntegerRange.of(5, 19));
        assertEquals(13, helper.visibleNum());
        assertEquals(15, helper.totalNum());
        assertCounter(0, 1, 0, 0, 0, 2, 0);

        // Big buffer -> 19 cells (6 buffer)
        robot.interact(() -> list.setBufferSize(BufferSize.BIG));
        assertState(list, IntegerRange.of(1, 19));
        assertEquals(13, helper.visibleNum());
        assertEquals(19, helper.totalNum());
        assertCounter(2, 1, 4, 4, 2, 0, 0);

        /*
         * Updates are 4 because the implemented cache is simple but quick. Cells are not stored by index!
         */
    }

    @Test
    void testBufferChangeMiddle(FxRobot robot) {
        StackPane pane = setupStage();
        List list = new List(items(50));
        VFXListHelper<Integer, VFXCell<Integer>> helper = list.getHelper();
        robot.interact(() -> {
            pane.getChildren().add(list);
            list.setVPos(600);
        });

        // Check vPos!!!
        assertEquals(600, list.getVPos());

        // Standard buffer -> 17 cells (4 buffer)
        assertState(list, IntegerRange.of(16, 32));
        assertEquals(13, helper.visibleNum());
        assertEquals(17, helper.totalNum());
        assertCounter(17, 1, 17, 17, 0, 0, 0);

        // Small buffer -> 15 cells (2 buffer)
        robot.interact(() -> list.setBufferSize(BufferSize.SMALL));
        assertState(list, IntegerRange.of(17, 31));
        assertEquals(13, helper.visibleNum());
        assertEquals(15, helper.totalNum());
        assertCounter(0, 1, 0, 0, 0, 2, 0);

        // Big buffer -> 19 cells (6 buffer)
        robot.interact(() -> list.setBufferSize(BufferSize.BIG));
        assertState(list, IntegerRange.of(15, 33));
        assertEquals(13, helper.visibleNum());
        assertEquals(19, helper.totalNum());
        assertCounter(2, 1, 4, 4, 2, 0, 0);
    }

    @Test
    void testChangeFactory(FxRobot robot) {
        StackPane pane = setupStage();
        List list = new List(items(50));
        VFXListHelper<Integer, VFXCell<Integer>> helper = list.getHelper();
        robot.interact(() -> pane.getChildren().add(list));

        // Test init
        assertState(list, IntegerRange.of(0, 16));
        assertCounter(17, 1, 17, 17, 0, 0, 0);

        robot.interact(() -> list.setVPos(600));
        // Check vPos!!!
        assertEquals(600, list.getVPos());

        // Test scroll
        assertState(list, IntegerRange.of(16, 32));
        assertCounter(0, 1, 16, 16, 0, 0, 0);

        // Change factory and test
        robot.interact(() ->
            list.setCellFactory(i -> new TestCell<>(i) {
                {
                    setConverter(t -> "New Factory! Index: %d Item: %s".formatted(
                        getIndex(),
                        t != null ? t.toString() : ""
                    ));
                }
            }));

        assertState(list, IntegerRange.of(16, 32));
        assertEquals(13, helper.visibleNum());
        assertEquals(17, helper.totalNum());
        assertCounter(17, 1, 17, 17, 0, 17, 17);
        // The cache comes from the old state disposal, but those cells are then immediately
        // disposed and dropped by the cache, in fact there is no de-cache afterward
    }

    @Test
    void testFitToViewport(FxRobot robot) {
        StackPane pane = setupStage();
        List list = new List(items(30), i -> new TestCell<>(i) {
            @Override
            protected double computePrefWidth(double height) {
                if (getItem() != null && getItem() == 0) return 800.0;
                int val;
                do {
                    val = ThreadLocalRandom.current().nextInt(350, 450);
                } while (val == 400.0);
                return val;
            }
        });
        robot.interact(() -> pane.getChildren().add(list));

        // Test init
        list.getState().getCellsByIndexUnmodifiable().values().stream()
            .map(VFXCell::toNode)
            .forEach(n -> assertEquals(400.0, n.getLayoutBounds().getWidth()));

        // Disable and test again
        robot.interact(() -> list.setFitToViewport(false));
        assertEquals(800.0, list.getHelper().getVirtualMaxX());
        list.getState().getCellsByIndexUnmodifiable().values().stream()
            .map(VFXCell::toNode)
            .forEach(n -> assertNotEquals(400.0, n.getLayoutBounds().getWidth()));

        // Scroll to max and then disable again
        robot.interact(() -> list.setHPos(Double.MAX_VALUE));
        assertEquals(400.0, list.getHPos()); // virtualMaxX(800) - viewportWidth(400)

        robot.interact(() -> list.setFitToViewport(true));
        assertEquals(0.0, list.getHPos());
        list.getState().getCellsByIndexUnmodifiable().values().stream()
            .map(VFXCell::toNode)
            .forEach(n -> assertEquals(400.0, n.getLayoutBounds().getWidth()));
    }

    @Test
    void testChangeCellSizeTop(FxRobot robot) {
        StackPane pane = setupStage();
        List list = new List(items(50));
        VFXListHelper<Integer, VFXCell<Integer>> helper = list.getHelper();
        robot.interact(() -> pane.getChildren().add(list));

        // Test init, why not
        assertState(list, IntegerRange.of(0, 16));
        assertCounter(17, 1, 17, 17, 0, 0, 0);
        assertEquals(32 * 50, helper.getVirtualMaxY());

        // Decrease and test again
        robot.interact(() -> list.setCellSize(20));
        assertState(list, IntegerRange.of(0, 23));
        assertCounter(7, 1, 7, 7, 0, 0, 0);
        assertEquals(20 * 50, helper.getVirtualMaxY());

        // Increase and test again
        robot.interact(() -> list.setCellSize(36));
        assertState(list, IntegerRange.of(0, 15));
        assertCounter(0, 1, 0, 0, 0, 8, 0);
        assertEquals(36 * 50, helper.getVirtualMaxY());
    }

    @Test
    void testChangeCellSizeMiddle(FxRobot robot) {
        StackPane pane = setupStage();
        List list = new List(items(50));
        VFXListHelper<Integer, VFXCell<Integer>> helper = list.getHelper();
        robot.interact(() -> {
            pane.getChildren().add(list);
            list.setVPos(600.0);
        });

        // Check vPos!!!
        assertEquals(600, list.getVPos());

        // Test init, why not
        assertState(list, IntegerRange.of(16, 32));
        assertCounter(17, 1, 17, 17, 0, 0, 0);
        assertEquals(32 * 50, helper.getVirtualMaxY());

        // Decrease and test again
        robot.interact(() -> list.setCellSize(10));
        assertState(list, IntegerRange.of(6, 49));
        assertCounter(27, 1, 27, 27, 0, 0, 0);
        assertEquals(10 * 50, helper.getVirtualMaxY());
        assertEquals(100, list.getVPos());

        // Increase and test again
        // vPos is now at 100!
        robot.interact(() -> list.setCellSize(50));
        assertState(list, IntegerRange.of(0, 11));
        assertCounter(0, 1, 6, 6, 0, 32, 22);
        assertEquals(50 * 50, helper.getVirtualMaxY());
        assertEquals(100, list.getVPos());
    }

    @Test
    void testChangeCellSizeBottom(FxRobot robot) {
        StackPane pane = setupStage();
        List list = new List(items(50));
        VFXListHelper<Integer, VFXCell<Integer>> helper = list.getHelper();
        robot.interact(() -> {
            pane.getChildren().add(list);
            list.setVPos(Double.MAX_VALUE);
        });

        // Check vPos!!!
        assertEquals(1200.0, list.getVPos());

        // Test init, why not
        assertState(list, IntegerRange.of(33, 49));
        assertCounter(17, 1, 17, 17, 0, 0, 0);
        assertEquals(32 * 50, helper.getVirtualMaxY());

        // Decrease and test again
        robot.interact(() -> list.setCellSize(24));
        assertState(list, IntegerRange.of(29, 49));
        assertCounter(4, 1, 4, 4, 0, 0, 0);
        assertEquals(24 * 50, helper.getVirtualMaxY());

        // Increase and test again
        // vPos is now at 800!
        robot.interact(() -> list.setCellSize(44));
        assertState(list, IntegerRange.of(16, 29));
        assertCounter(0, 1, 13, 13, 0, 7, 0);
        assertEquals(44 * 50, helper.getVirtualMaxY());
    }

    @Test
    void testChangeCellSizeTo0(FxRobot robot) {
        StackPane pane = setupStage();
        List list = new List(items(50));
        robot.interact(() -> pane.getChildren().add(list));

        assertState(list, IntegerRange.of(0, 16));
        assertCounter(17, 1, 17, 17, 0, 0, 0);

        robot.interact(() -> list.setCellSize(0.0));
        assertState(list, INVALID_RANGE);
        assertCounter(0, 0, 0, 0, 0, 17, 7);
    }

    @Test
    void testChangeList(FxRobot robot) {
        StackPane pane = setupStage();
        List list = new List(items(50));
        robot.interact(() -> pane.getChildren().add(list));

        assertState(list, IntegerRange.of(0, 16));
        assertCounter(17, 1, 17, 17, 0, 0, 0);

        // Change items property
        robot.interact(() -> list.setItems(items(50, 50)));
        assertState(list, IntegerRange.of(0, 16));
        assertCounter(0, 1, 0, 17, 0, 0, 0);

        // Change items property (fewer elements)
        robot.interact(() -> list.setItems(items(50, 10)));
        assertState(list, IntegerRange.of(0, 9));
        assertCounter(0, 1, 0, 0, 0, 7, 0);

        // Change items property (more elements)
        robot.interact(() -> list.setItems(items(100, 50)));
        assertState(list, IntegerRange.of(0, 16));
        assertCounter(0, 1, 0, 17, 7, 0, 0); // No index updates because of cached cells

        // Scroll(to bottom) and change items property (fewer elements)
        robot.interact(() -> {
            list.scrollToLast();
            list.setItems(items(50, 50));
        });
        assertState(list, IntegerRange.of(33, 49));
        assertCounter(0, 2, 17, 34, 0, 0, 0); // 2 layouts because of scroll

        // Change items property again (more elements)
        robot.interact(() -> list.setItems(items(50, 100)));
        assertState(list, IntegerRange.of(35, 51));
        assertCounter(0, 1, 2, 2, 0, 0, 0);

        // Change items to empty
        robot.interact(() -> list.setItems(null));
        assertState(list, INVALID_RANGE);
        assertCounter(0, 0, 0, 0, 0, 17, 7);
    }

    @Test
    void testPermutation(FxRobot robot) {
        StackPane pane = setupStage();
        List list = new List(items(50));
        robot.interact(() -> pane.getChildren().add(list));

        // Assert init
        assertState(list, IntegerRange.of(0, 16));
        assertCounter(17, 1, 17, 17, 0, 0, 0);

        // Permutation change
        robot.interact(() -> FXCollections.sort(list.getItems(), Comparator.reverseOrder()));
        assertState(list, IntegerRange.of(0, 16));
        assertCounter(0, 1, 0, 17, 0, 0, 0);
    }

    @Test
    void testSet(FxRobot robot) {
        StackPane pane = setupStage();
        List list = new List(items(50));
        robot.interact(() -> pane.getChildren().add(list));

        // Assert init
        assertState(list, IntegerRange.of(0, 16));
        assertCounter(17, 1, 17, 17, 0, 0, 0);

        // Set all (more)
        robot.interact(() -> list.getItems().setAll(items(100, 100)));
        assertState(list, IntegerRange.of(0, 16));
        assertCounter(0, 1, 0, 17, 0, 0, 0);

        // Set all (less)
        robot.interact(() -> list.getItems().setAll(items(100, 15)));
        assertState(list, IntegerRange.of(0, 14));
        assertCounter(0, 1, 0, 0, 0, 2, 0);

        // Restore old items count, scroll and set all (more)
        // Also check re-usability (by identity!!!)
        ObservableList<Integer> tmp = items(100, 100);
        robot.interact(() -> {
            list.getItems().setAll(tmp.subList(0, 50));
            list.setVPos(Double.MAX_VALUE);
        });
        counter.reset();
        robot.interact(() -> list.getItems().setAll(tmp));
        assertState(list, IntegerRange.of(35, 51));
        assertCounter(0, 1, 2, 2, 0, 0, 0);

        // Set random items
        robot.interact(() -> {
            for (int i = 0; i < 5; i++) {
                int index = RandomUtils.random.nextInt(35, 52);
                list.getItems().set(index, 999 - i);
            }
        });
        assertState(list, IntegerRange.of(35, 51));
        assertCounter(0, 5, 0, 5, 0, 0, 0);
    }

    @Test
    void testAddAt0(FxRobot robot) {
        StackPane pane = setupStage();
        List list = new List(items(50));
        robot.interact(() -> pane.getChildren().add(list));

        // Assert init
        assertState(list, IntegerRange.of(0, 16));
        assertCounter(17, 1, 17, 17, 0, 0, 0);

        // Add all at 0
        java.util.List<Integer> elements = IntStream.range(0, 4)
            .map(i -> 999 - i)
            .boxed()
            .toList();
        robot.interact(() -> list.getItems().addAll(0, elements));
        assertState(list, IntegerRange.of(0, 16));
        assertCounter(0, 1, 17, 4, 0, 0, 0); // 17 by index, 13 are shifted, 4 are updated

        // Add at 0 (for)
        robot.interact(() -> {
            for (int i = 0; i < 4; i++) list.getItems().addFirst(1999 - i);
        });
        assertState(list, IntegerRange.of(0, 16));
        assertCounter(0, 4, 68, 4, 0, 0, 0); // A lot of index updates because of for cycle
    }

    @Test
    void testAddMiddle(FxRobot robot) {
        StackPane pane = setupStage();
        List list = new List(items(50));
        robot.interact(() -> pane.getChildren().add(list));

        // Assert init
        assertState(list, IntegerRange.of(0, 16));
        assertCounter(17, 1, 17, 17, 0, 0, 0);

        // Scroll to middle
        robot.interact(() -> list.setVPos(600));
        counter.reset(); // Reset otherwise wrong statistics

        // Add before no intersect
        robot.interact(() -> list.getItems().addAll(0, java.util.List.of(-1, -2, -3)));
        assertState(list, IntegerRange.of(16, 32));
        assertCounter(0, 1, 17, 3, 0, 0, 0);

        // Add before intersect
        robot.interact(() -> list.getItems().addAll(14, java.util.List.of(-4, -5, -6, -7)));
        assertState(list, IntegerRange.of(16, 32));
        assertCounter(0, 1, 17, 4, 0, 0, 0);

        // Add after intersect
        robot.interact(() -> list.getItems().addAll(29, java.util.List.of(-8, -9, -10, -11, -12)));
        assertState(list, IntegerRange.of(16, 32));
        assertCounter(0, 1, 0, 4, 0, 0, 0);

        // Add after no intersect
        robot.interact(() -> list.getItems().addAll(40, java.util.List.of(483, 9822, 75486, 1240, 1151)));
        assertState(list, IntegerRange.of(16, 32));
        assertCounter(0, 1, 0, 0, 0, 0, 0);
    }

    @Test
    void testAddAtEnd(FxRobot robot) {
        StackPane pane = setupStage();
        List list = new List(items(50));
        robot.interact(() -> pane.getChildren().add(list));

        // Assert init
        assertState(list, IntegerRange.of(0, 16));
        assertCounter(17, 1, 17, 17, 0, 0, 0);

        robot.interact(() -> list.setVPos(Double.MAX_VALUE));
        counter.reset(); // Reset otherwise wrong statistics

        // Add all at end
        java.util.List<Integer> elements = IntStream.range(0, 4)
            .map(i -> 999 - i)
            .boxed()
            .toList();
        robot.interact(() -> list.getItems().addAll(elements));
        assertState(list, IntegerRange.of(35, 51));
        assertCounter(0, 1, 2, 2, 0, 0, 0); // These 2 updates are because the new elements allow the buffer to be at the bottom

        // Add at end (for)
        robot.interact(() -> {
            for (int i = 0; i < 4; i++) list.getItems().add(1999 - i);
        });
        assertState(list, IntegerRange.of(35, 51));
        assertCounter(0, 4, 0, 0, 0, 0, 0);

        // Add before no intersect
        robot.interact(() -> list.getItems().addAll(0, java.util.List.of(99, 98, 97, 96, 95)));
        assertState(list, IntegerRange.of(35, 51));
        assertCounter(0, 1, 17, 5, 0, 0, 0);

        // Add before intersect
        robot.interact(() -> list.getItems().addAll(32, java.util.List.of(-1, -2, -3, -4, -5)));
        assertState(list, IntegerRange.of(35, 51));
        assertCounter(0, 1, 17, 5, 0, 0, 0);
    }

    @Test
    void testRemoveAt0(FxRobot robot) {
        StackPane pane = setupStage();
        List list = new List(items(50));
        robot.interact(() -> pane.getChildren().add(list));

        // Assert init
        assertState(list, IntegerRange.of(0, 16));
        assertCounter(17, 1, 17, 17, 0, 0, 0);

        // Remove all at 0
        robot.interact(() -> Utils.removeAll(list, 0, 1, 2, 3));
        assertState(list, IntegerRange.of(0, 16));
        assertCounter(0, 1, 17, 4, 0, 0, 0);

        // Remove at 0 (for)
        robot.interact(() -> {
            for (int i = 0; i < 4; i++) list.getItems().removeFirst();
        });
        assertState(list, IntegerRange.of(0, 16));
        assertCounter(0, 4, 68, 4, 0, 0, 0); // A lot of index updates because of for cycle
    }

    @Test
    void testRemoveMiddle(FxRobot robot) {
        StackPane pane = setupStage();
        List list = new List(items(50));
        robot.interact(() -> pane.getChildren().add(list));

        // Assert init
        assertState(list, IntegerRange.of(0, 16));
        assertCounter(17, 1, 17, 17, 0, 0, 0);

        // Scroll to middle
        robot.interact(() -> list.setVPos(600));
        counter.reset(); // Reset otherwise wrong statistics

        // Remove before no intersect
        robot.interact(() -> Utils.removeAll(list, 0, 1, 2));
        assertState(list, IntegerRange.of(16, 32));
        assertCounter(0, 1, 17, 3, 0, 0, 0);

        // Remove before intersect
        robot.interact(() -> Utils.removeAll(list, 14, 15, 16, 17));
        assertState(list, IntegerRange.of(16, 32));
        assertCounter(0, 1, 17, 4, 0, 0, 0);

        // Remove after no intersect
        robot.interact(() -> Utils.removeAll(list, 34, 35));
        assertState(list, IntegerRange.of(16, 32));
        assertCounter(0, 1, 0, 0, 0, 0, 0);

        // Remove after intersect
        robot.interact(() -> Utils.removeAll(list, 30, 31, 32, 33, 34));
        assertState(list, IntegerRange.of(16, 32));
        assertCounter(0, 1, 0, 3, 0, 0, 0);

        // Remove enough to change vPos
        // Removed up until now: 14. Remaining -> 36. Max vPos -> 1152
        // Remove to 27 -> Max vPos -> 464
        robot.interact(() -> Utils.removeAll(list, 0, 1, 2, 3, 4, 5, 6, 7, 8));
        assertState(list, IntegerRange.of(10, 26));
        assertCounter(0, 1, 17, 3, 0, 0, 0);
        assertEquals(list.getCellSize() * list.size() - list.getHeight(), list.getVPos());

        // Do it again but this time from bottom
        robot.interact(() -> Utils.removeAll(list, 26, 25, 24));
        assertState(list, IntegerRange.of(7, 23));
        assertCounter(0, 1, 3, 3, 0, 0, 0);
        assertEquals(list.getCellSize() * list.size() - list.getHeight(), list.getVPos());
    }

    @Test
    void testRemoveAtEnd(FxRobot robot) {
        StackPane pane = setupStage();
        List list = new List(items(50));
        robot.interact(() -> pane.getChildren().add(list));

        // Assert init
        assertState(list, IntegerRange.of(0, 16));
        assertCounter(17, 1, 17, 17, 0, 0, 0);

        robot.interact(() -> list.setVPos(Double.MAX_VALUE));
        counter.reset(); // Reset otherwise wrong statistics

        // Remove all at end
        robot.interact(() -> Utils.removeAll(list, 49, 48, 47, 46));
        assertState(list, IntegerRange.of(29, 45));
        assertCounter(0, 1, 4, 4, 0, 0, 0);
        assertEquals(list.getCellSize() * list.size() - list.getHeight(), list.getVPos());

        // Remove at end (for)
        robot.interact(() -> {
            for (int i = 0; i < 4; i++) list.getItems().removeLast();
        });
        assertState(list, IntegerRange.of(25, 41));
        assertCounter(0, 4, 4, 4, 0, 0, 0);
        assertEquals(list.getCellSize() * list.size() - list.getHeight(), list.getVPos());

        // Remove before no intersect
        robot.interact(() -> Utils.removeAll(list, 0, 1, 2));
        assertState(list, IntegerRange.of(22, 38));
        assertCounter(0, 1, 17, 0, 0, 0, 0);
        assertEquals(list.getCellSize() * list.size() - list.getHeight(), list.getVPos());

        // Remove before intersect
        robot.interact(() -> Utils.removeAll(list, 20, 21, 22, 23));
        assertState(list, IntegerRange.of(18, 34));
        assertCounter(0, 1, 17, 2, 0, 0, 0);
        assertEquals(list.getCellSize() * list.size() - list.getHeight(), list.getVPos());

        // Remove enough to cache cells
        robot.interact(() -> Utils.removeAll(list, IntegerRange.of(15, 34)));
        assertState(list, IntegerRange.of(0, 14));
        assertCounter(0, 1, 15, 15, 0, 2, 0);
        assertEquals(list.getCellSize() * list.size() - list.getHeight(), list.getVPos());
    }

    @Test
    void testRemoveSparse(FxRobot robot) {
        StackPane pane = setupStage();
        List list = new List(items(50));
        robot.interact(() -> pane.getChildren().add(list));

        // Assert init
        assertState(list, IntegerRange.of(0, 16));
        assertCounter(17, 1, 17, 17, 0, 0, 0);

        robot.interact(() -> Utils.removeAll(list, 0, 3, 4, 8, 10, 11));
        assertState(list, IntegerRange.of(0, 16));
        assertCounter(0, 1, 17, 6, 0, 0, 0);
    }

    @Test
    void testSwitchOrientation(FxRobot robot) {
        StackPane pane = setupStage();
        List list = new List(items(50));
        robot.interact(() -> pane.getChildren().add(list));
        counter.reset();

        robot.interact(() -> list.setOrientation(Orientation.HORIZONTAL));
        assertState(list, IntegerRange.of(0, 16));
        assertCounter(0, 1, 0, 0, 0, 0, 0);

        robot.interact(() -> list.scrollToPixel(600.0));
        assertEquals(0.0, list.getVPos());
        assertEquals(600.0, list.getHPos());
        assertState(list, IntegerRange.of(16, 32));
        assertCounter(0, 1, 16, 16, 0, 0, 0);

        robot.interact(() -> list.scrollToPixel(Double.MAX_VALUE));
        assertEquals(0.0, list.getVPos());
        assertEquals(1200.0, list.getHPos());
        assertState(list, IntegerRange.of(33, 49));
        assertCounter(0, 1, 17, 17, 0, 0, 0);

        robot.interact(() -> list.setOrientation(Orientation.VERTICAL));
        assertEquals(0.0, list.getVPos());
        assertEquals(0.0, list.getHPos());
        assertState(list, IntegerRange.of(0, 16));
        assertCounter(0, 1, 17, 17, 0, 0, 0);
    }

    @Test
    void testSwitchOrientationFitToViewport(FxRobot robot) {
        StackPane pane = setupStage();
        List list = new List(items(50));
        robot.interact(() -> {
            list.setOrientation(Orientation.HORIZONTAL);
            pane.getChildren().add(list);
        });

        // Assert init
        assertState(list, IntegerRange.of(0, 16));
        assertCounter(17, 2, 17, 17, 0, 0, 0);

        // Change factory
        robot.interact(() -> list.setCellFactory(i -> new TestCell<>(i) {
            @Override
            protected double computePrefWidth(double height) {
                return 500.0;
            }

            @Override
            protected double computePrefHeight(double width) {
                return 500.0;
            }
        }));
        assertEquals(0.0, list.getVPos());
        assertEquals(0.0, list.getHPos());
        assertState(list, IntegerRange.of(0, 16));
        assertCounter(17, 1, 17, 17, 0, 17, 17);
        assertEquals(400.0, list.getHelper().getVirtualMaxY());

        // Allow variable height
        robot.interact(() -> {
            list.setFitToViewport(false);
            list.setVPos(Double.MAX_VALUE);
        });
        assertEquals(100.0, list.getVPos());
        assertEquals(0.0, list.getHPos());
        assertState(list, IntegerRange.of(0, 16));
        assertCounter(0, 1, 0, 0, 0, 0, 0);
        assertEquals(500.0, list.getHelper().getVirtualMaxY());

        robot.interact(() -> {
            list.setHPos(Double.MAX_VALUE);
            counter.reset();
            list.setOrientation(Orientation.VERTICAL);
        });
        assertEquals(0.0, list.getVPos());
        assertEquals(0.0, list.getHPos());
        assertState(list, IntegerRange.of(0, 16));
        assertCounter(0, 1, 17, 17, 0, 0, 0);
        assertEquals(500.0, list.getHelper().getVirtualMaxX());
    }

    @Test
    void testSpacing(FxRobot robot) {
        StackPane pane = setupStage();
        List list = new List(items(50));
        robot.interact(() -> pane.getChildren().add(list));

        // Assert init
        assertState(list, IntegerRange.of(0, 16));
        assertCounter(17, 1, 17, 17, 0, 0, 0);

        // Set spacing 10.0
        robot.interact(() -> list.setSpacing(10.0));
        assertState(list, IntegerRange.of(0, 13));
        assertCounter(0, 1, 0, 0, 0, 3, 0);

        // Set spacing 30.0
        robot.interact(() -> list.setSpacing(30.0));
        assertState(list, IntegerRange.of(0, 10));
        assertCounter(0, 1, 0, 0, 0, 3, 0);

        // Try scrolling
        robot.interact(() -> list.setVPos(100.0));
        assertState(list, IntegerRange.of(0, 10));
        assertCounter(0, 0, 0, 0, 0, 0, 0);

        // Scroll more
        robot.interact(() -> list.scrollToIndex(5));
        assertEquals(310.0, list.getVPos());
        assertState(list, IntegerRange.of(3, 13));
        assertCounter(0, 1, 3, 3, 0, 0, 0);

        // Scroll to end
        robot.interact(() -> list.setVPos(Double.MAX_VALUE));
        assertState(list, IntegerRange.of(39, 49));
        assertCounter(0, 1, 11, 11, 0, 0, 0);

        // Remove spacing
        robot.interact(() -> list.setSpacing(0.0));
        assertState(list, IntegerRange.of(33, 49));
        assertCounter(0, 1, 6, 6, 6, 0, 0);
    }

    @Test
    void testManualUpdate(FxRobot robot) {
        StackPane pane = setupStage();
        VFXList<User, VFXCell<User>> list = new VFXList<>(users(20), u -> {
            TestCell<User> cell = new TestCell<>(u);
            counter.created();
            return cell;
        }) {
            @Override
            public Supplier<SkinBase<?, ?>> defaultSkinProvider() {
                return () -> new VFXListSkin<>(this) {
                    @Override
                    protected void onLayoutCompleted(boolean done) {
                        super.onLayoutCompleted(done);
                        if (done) counter.layout();
                    }
                };
            }
        };
        robot.interact(() -> pane.getChildren().add(list));

        // Assert Init
        assertEquals(list.getRange(), IntegerRange.of(0, 16));
        assertCounter(17, 1, 17, 17, 0, 0, 0);

        // Get text before change cell 5
        VFXCell<User> cell5 = list.getState().getCellsByIndexUnmodifiable().get(5);
        Label label5 = (Label) cell5.toNode().lookup(".label");
        String text5 = label5.getText();

        // Change item 5
        robot.interact(() -> list.getItems().get(5).setFirstName(faker.name().firstName()));
        assertEquals(text5, label5.getText()); // No automatic update

        // Force update (all)
        robot.interact(() -> list.update());
        assertNotEquals(text5, label5.getText());

        // Get text before change cell 7
        VFXCell<User> cell7 = list.getState().getCellsByIndexUnmodifiable().get(7);
        Label label7 = (Label) cell7.toNode().lookup(".label");
        String text7 = label7.getText();

        // Change item 7
        robot.interact(() -> list.getItems().get(7).setFirstName(faker.name().firstName()));
        assertEquals(text7, label7.getText()); // No automatic update

        // Force update (single)
        robot.interact(() -> list.update(7));
        assertNotEquals(text7, label7.getText());
    }
}
