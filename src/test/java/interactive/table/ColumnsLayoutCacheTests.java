/*
 * Copyright (C) 2026 Parisi Alessandro - alessandro.parisi406@gmail.com
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

package interactive.table;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.palexdev.virtualizedfx.cells.base.VFXTableCell;
import io.github.palexdev.virtualizedfx.enums.ColumnsFillPolicy;
import io.github.palexdev.virtualizedfx.table.ColumnsLayoutCache;
import io.github.palexdev.virtualizedfx.table.VFXTable;
import io.github.palexdev.virtualizedfx.table.VFXTableColumn;
import javafx.collections.ListChangeListener;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import src.model.User;

import static interactive.table.TableTestUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static src.model.User.users;

@ExtendWith(ApplicationExtension.class)
public class ColumnsLayoutCacheTests {
    private static final double DELTA = 1e-6;

    @Start
    void start(Stage stage) {
        stage.show();
    }

    //================================================================================
    // Init
    //================================================================================

    @Test
    void testInitOverflow(FxRobot robot) {
        Table table = new Table(users(50));
        robot.interact(() -> table.resize(400, 400));
        TestCache cache = new TestCache(table);

        assertCache(cache, table);
        assertEquals(180 * 7, cache.get(), DELTA);
        assertEquals(180 * 3, cache.posAt(3), DELTA);
        assertEquals(180, cache.widthAt(6), DELTA);
    }

    @Test
    void testInitLast(FxRobot robot) {
        Table table = new Table(users(50));
        robot.interact(() -> {
            table.setColumnsWidth(40);
            table.resize(400, 400);
        });
        TestCache cache = new TestCache(table);

        assertCache(cache, table);
        assertEquals(400, cache.get(), DELTA);
        assertEquals(240, cache.posAt(6), DELTA);
        assertEquals(160, cache.widthAt(6), DELTA);
    }

    @Test
    void testInitWeightedWithoutWeights(FxRobot robot) {
        Table table = new Table(users(50));
        robot.interact(() -> {
            table.setColumnsWidth(40);
            table.setColumnsFillPolicy(ColumnsFillPolicy.WEIGHTED);
            table.resize(400, 400);
        });
        TestCache cache = new TestCache(table);

        assertCache(cache, table);
        assertEquals(280, cache.get(), DELTA);
        assertEquals(40, cache.widthAt(6), DELTA);
    }

    @Test
    void testInitWeighted(FxRobot robot) {
        Table table = new Table(users(50));
        robot.interact(() -> {
            table.setColumnsWidth(40);
            table.setColumnsFillPolicy(ColumnsFillPolicy.WEIGHTED);
            VFXTable.setWeight(table.columns().get(1), 1);
            VFXTable.setWeight(table.columns().get(3), 1);
            table.resize(400, 400);
        });
        TestCache cache = new TestCache(table);

        assertCache(cache, table);
        assertEquals(400, cache.get(), DELTA);
        assertEquals(100, cache.widthAt(1), DELTA);
        assertEquals(100, cache.widthAt(3), DELTA);
        assertEquals(280, cache.posAt(4), DELTA);
    }

    @Test
    void testInitWithUserPrefs(FxRobot robot) {
        Table table = new Table(users(50));
        robot.interact(() -> {
            table.resize(400, 400);
            table.columns().get(2).setUserPrefWidth(150);
            table.columns().get(4).setUserPrefWidth(250);
        });
        TestCache cache = new TestCache(table);

        assertCache(cache, table);
    }

    //================================================================================
    // Table width
    //================================================================================

    @Test
    void testTableWidthChanged(FxRobot robot) {
        Table table = new Table(users(50));
        robot.interact(() -> {
            table.setColumnsWidth(40);
            table.resize(400, 400);
        });
        TestCache cache = new TestCache(table);
        assertCache(cache, table);

        robot.interact(() -> table.resize(500, 400));
        assertEquals(6, cache.tableWidthChanged());
        assertCache(cache, table);
        assertEquals(260, cache.widthAt(6), DELTA);

        robot.interact(() -> table.resize(280, 400));
        assertEquals(6, cache.tableWidthChanged());
        assertCache(cache, table);
        assertEquals(40, cache.widthAt(6), DELTA);

        robot.interact(() -> table.resize(250, 400));
        assertEquals(-1, cache.tableWidthChanged());
        assertCache(cache, table);
        assertEquals(280, cache.get(), DELTA);

        robot.interact(() -> table.resize(400, 400));
        assertEquals(6, cache.tableWidthChanged());
        assertCache(cache, table);
        assertEquals(400, cache.get(), DELTA);
    }

    @Test
    void testTableWidthChangedWeighted(FxRobot robot) {
        Table table = new Table(users(50));
        robot.interact(() -> {
            table.setColumnsWidth(40);
            table.setColumnsFillPolicy(ColumnsFillPolicy.WEIGHTED);
            VFXTable.setWeight(table.columns().get(2), 1);
            VFXTable.setWeight(table.columns().get(4), 1);
            table.resize(400, 400);
        });
        TestCache cache = new TestCache(table);
        assertCache(cache, table);

        robot.interact(() -> table.resize(500, 400));
        assertEquals(2, cache.tableWidthChanged());
        assertCache(cache, table);
        assertEquals(150, cache.widthAt(2), DELTA);

        robot.interact(() -> table.resize(250, 400));
        assertEquals(2, cache.tableWidthChanged());
        assertCache(cache, table);
        assertEquals(40, cache.widthAt(2), DELTA);

        robot.interact(() -> table.resize(200, 400));
        assertEquals(-1, cache.tableWidthChanged());
        assertCache(cache, table);
    }

    @Test
    void testTableWidthChangedWithoutAbsorber(FxRobot robot) {
        Table table = new Table(users(50));
        robot.interact(() -> {
            table.setColumnsWidth(40);
            table.setColumnsFillPolicy(ColumnsFillPolicy.WEIGHTED);
            table.resize(400, 400);
        });
        TestCache cache = new TestCache(table);
        assertCache(cache, table);

        robot.interact(() -> table.resize(500, 400));
        assertEquals(-1, cache.tableWidthChanged());
        assertCache(cache, table);
        assertEquals(280, cache.get(), DELTA);
    }

    //================================================================================
    // Columns size
    //================================================================================

    @Test
    void testColumnsSizeChanged(FxRobot robot) {
        Table table = new Table(users(50));
        robot.interact(() -> table.resize(400, 400));
        TestCache cache = new TestCache(table);
        robot.interact(() -> {
            table.columns().get(2).setUserPrefWidth(150);
            table.columns().get(4).setUserPrefWidth(250);
        });
        cache.columnResized(table.columns().get(2));
        cache.columnResized(table.columns().get(4));
        assertCache(cache, table);

        robot.interact(() -> table.setColumnsWidth(100));
        cache.columnsSizeChanged();
        assertCache(cache, table);
        assertEquals(150, cache.widthAt(2), DELTA);
        assertEquals(250, cache.widthAt(4), DELTA);

        robot.interact(() -> table.setColumnsWidth(200));
        cache.columnsSizeChanged();
        assertCache(cache, table);
        assertEquals(200, cache.widthAt(2), DELTA);
        assertEquals(250, cache.widthAt(4), DELTA);

        robot.interact(() -> table.setColumnsWidth(200));
        cache.columnsSizeChanged();
        assertCache(cache, table);

        robot.interact(() -> table.setColumnsWidth(20));
        cache.columnsSizeChanged();
        assertCache(cache, table);
        assertEquals(20 * 5 + 150 + 250, cache.get(), DELTA);
    }

    //================================================================================
    // Column resize
    //================================================================================

    @Test
    void testColumnResized(FxRobot robot) {
        Table table = new Table(users(50));
        robot.interact(() -> table.resize(400, 400));
        TestCache cache = new TestCache(table);
        assertCache(cache, table);

        robot.interact(() -> table.columns().get(1).setUserPrefWidth(250));
        assertEquals(1, cache.columnResized(table.columns().get(1)));
        assertCache(cache, table);

        robot.interact(() -> table.columns().get(1).setUserPrefWidth(260));
        assertEquals(1, cache.columnResized(table.columns().get(1)));
        assertEquals(180 + 260 + 180, cache.posAt(3), DELTA);
        robot.interact(() -> table.columns().get(5).setUserPrefWidth(300));
        assertEquals(5, cache.columnResized(table.columns().get(5)));
        assertCache(cache, table);

        robot.interact(() -> table.columns().get(2).setUserPrefWidth(150));
        assertEquals(-1, cache.columnResized(table.columns().get(2)));
        assertCache(cache, table);
        robot.interact(() -> table.columns().get(2).setUserPrefWidth(170));
        assertEquals(-1, cache.columnResized(table.columns().get(2)));
        assertCache(cache, table);

        robot.interact(() -> table.columns().get(1).setUserPrefWidth(-1));
        assertEquals(1, cache.columnResized(table.columns().get(1)));
        assertCache(cache, table);
    }

    @Test
    void testColumnResizedWhileFilling(FxRobot robot) {
        Table table = new Table(users(50));
        robot.interact(() -> {
            table.setColumnsWidth(40);
            table.resize(400, 400);
        });
        TestCache cache = new TestCache(table);
        assertCache(cache, table);

        robot.interact(() -> table.columns().getFirst().setUserPrefWidth(60));
        assertEquals(0, cache.columnResized(table.columns().getFirst()));
        assertCache(cache, table);
        assertEquals(140, cache.widthAt(6), DELTA);
        assertEquals(400, cache.get(), DELTA);

        robot.interact(() -> table.columns().getFirst().setUserPrefWidth(300));
        assertEquals(0, cache.columnResized(table.columns().getFirst()));
        assertCache(cache, table);
        assertEquals(540, cache.get(), DELTA);
    }

    @Test
    void testColumnResizedWeighted(FxRobot robot) {
        Table table = new Table(users(50));
        robot.interact(() -> {
            table.setColumnsWidth(40);
            table.setColumnsFillPolicy(ColumnsFillPolicy.WEIGHTED);
            VFXTable.setWeight(table.columns().get(3), 1);
            table.resize(400, 400);
        });
        TestCache cache = new TestCache(table);
        assertCache(cache, table);

        robot.interact(() -> table.columns().get(5).setUserPrefWidth(60));
        assertEquals(3, cache.columnResized(table.columns().get(5)));
        assertCache(cache, table);
        assertEquals(140, cache.widthAt(3), DELTA);

        robot.interact(() -> table.columns().get(1).setUserPrefWidth(60));
        assertEquals(1, cache.columnResized(table.columns().get(1)));
        assertCache(cache, table);
        assertEquals(120, cache.widthAt(3), DELTA);
    }

    //================================================================================
    // Fill policy and weights
    //================================================================================

    @Test
    void testFillPolicyChanged(FxRobot robot) {
        Table table = new Table(users(50));
        robot.interact(() -> {
            table.setColumnsWidth(40);
            VFXTable.setWeight(table.columns().get(1), 1);
            VFXTable.setWeight(table.columns().get(3), 2);
            table.resize(400, 400);
        });
        TestCache cache = new TestCache(table);
        assertCache(cache, table);
        assertEquals(160, cache.widthAt(6), DELTA);

        robot.interact(() -> table.setColumnsFillPolicy(ColumnsFillPolicy.WEIGHTED));
        cache.fillPolicyChanged();
        assertCache(cache, table);
        assertEquals(80, cache.widthAt(1), DELTA);
        assertEquals(120, cache.widthAt(3), DELTA);
        assertEquals(40, cache.widthAt(6), DELTA);

        robot.interact(() -> table.setColumnsFillPolicy(ColumnsFillPolicy.LAST));
        cache.fillPolicyChanged();
        assertCache(cache, table);
        assertEquals(40, cache.widthAt(1), DELTA);
        assertEquals(160, cache.widthAt(6), DELTA);
    }

    @Test
    void testNegativeWeightIsIgnored(FxRobot robot) {
        Table table = new Table(users(50));
        robot.interact(() -> {
            table.setColumnsWidth(40);
            table.setColumnsFillPolicy(ColumnsFillPolicy.WEIGHTED);
            VFXTable.setWeight(table.columns().getFirst(), -1);
            VFXTable.setWeight(table.columns().get(2), 1);
            table.resize(400, 400);
        });
        TestCache cache = new TestCache(table);

        assertCache(cache, table);
        assertEquals(40, cache.widthAt(0), DELTA);
        assertEquals(160, cache.widthAt(2), DELTA);
        assertEquals(400, cache.get(), DELTA);
    }

    @Test
    void testColumnWeightChangedUnderLast(FxRobot robot) {
        Table table = new Table(users(50));
        robot.interact(() -> {
            table.setColumnsWidth(40);
            table.resize(400, 400);
        });
        TestCache cache = new TestCache(table);
        assertCache(cache, table);

        robot.interact(() -> VFXTable.setWeight(table.columns().get(1), 1));
        assertEquals(-1, cache.columnWeightChanged(table.columns().get(1)));
        assertCache(cache, table);
        assertEquals(40, cache.widthAt(1), DELTA);
        assertEquals(160, cache.widthAt(6), DELTA);

        robot.interact(() -> table.setColumnsFillPolicy(ColumnsFillPolicy.WEIGHTED));
        cache.fillPolicyChanged();
        assertCache(cache, table);
        assertEquals(160, cache.widthAt(1), DELTA);
        assertEquals(40, cache.widthAt(6), DELTA);
    }

    @Test
    void testColumnWeightChanged(FxRobot robot) {
        Table table = new Table(users(50));
        robot.interact(() -> {
            table.setColumnsWidth(40);
            table.setColumnsFillPolicy(ColumnsFillPolicy.WEIGHTED);
            table.resize(400, 400);
        });
        TestCache cache = new TestCache(table);
        assertCache(cache, table);
        assertEquals(280, cache.get(), DELTA);

        robot.interact(() -> VFXTable.setWeight(table.columns().get(3), 1));
        assertEquals(3, cache.columnWeightChanged(table.columns().get(3)));
        assertCache(cache, table);
        assertEquals(160, cache.widthAt(3), DELTA);
        assertEquals(400, cache.get(), DELTA);

        robot.interact(() -> VFXTable.setWeight(table.columns().get(1), 1));
        assertEquals(1, cache.columnWeightChanged(table.columns().get(1)));
        assertCache(cache, table);
        assertEquals(100, cache.widthAt(1), DELTA);
        assertEquals(100, cache.widthAt(3), DELTA);

        robot.interact(() -> VFXTable.setWeight(table.columns().get(5), 2));
        assertEquals(1, cache.columnWeightChanged(table.columns().get(5)));
        assertCache(cache, table);
        assertEquals(70, cache.widthAt(1), DELTA);
        assertEquals(70, cache.widthAt(3), DELTA);
        assertEquals(100, cache.widthAt(5), DELTA);

        assertEquals(-1, cache.columnWeightChanged(table.columns().get(5)));
        assertCache(cache, table);

        robot.interact(() -> VFXTable.setWeight(table.columns().getFirst(), -1));
        assertEquals(-1, cache.columnWeightChanged(table.columns().getFirst()));
        assertCache(cache, table);

        robot.interact(() -> VFXTable.setWeight(table.columns().get(1), 0));
        assertEquals(1, cache.columnWeightChanged(table.columns().get(1)));
        assertCache(cache, table);
        assertEquals(80, cache.widthAt(3), DELTA);
        assertEquals(120, cache.widthAt(5), DELTA);

        robot.interact(() -> VFXTable.setWeight(table.columns().get(3), 0));
        assertEquals(3, cache.columnWeightChanged(table.columns().get(3)));
        assertCache(cache, table);
        assertEquals(160, cache.widthAt(5), DELTA);

        robot.interact(() -> VFXTable.setWeight(table.columns().get(5), 0));
        assertEquals(5, cache.columnWeightChanged(table.columns().get(5)));
        assertCache(cache, table);
        assertEquals(280, cache.get(), DELTA);
    }

    @Test
    void testColumnWeightChangedWhileOverflowing(FxRobot robot) {
        Table table = new Table(users(50));
        robot.interact(() -> {
            table.setColumnsFillPolicy(ColumnsFillPolicy.WEIGHTED);
            table.resize(400, 400);
        });
        TestCache cache = new TestCache(table);
        assertCache(cache, table);

        robot.interact(() -> VFXTable.setWeight(table.columns().get(2), 1));
        assertEquals(-1, cache.columnWeightChanged(table.columns().get(2)));
        assertCache(cache, table);
        assertEquals(180 * 7, cache.get(), DELTA);

        robot.interact(() -> table.resize(1500, 400));
        assertEquals(2, cache.tableWidthChanged());
        assertCache(cache, table);
        assertEquals(420, cache.widthAt(2), DELTA);
        assertEquals(1500, cache.get(), DELTA);
    }

    @Test
    void testColumnWeightChangedWithStaleVector(FxRobot robot) {
        Table table = new Table(users(50));
        robot.interact(() -> {
            table.setColumnsWidth(40);
            table.setColumnsFillPolicy(ColumnsFillPolicy.WEIGHTED);
            table.resize(400, 400);
        });
        TestCache cache = new TestCache(table);

        robot.interact(() -> VFXTable.setWeight(table.columns().get(2), 1));
        assertEquals(2, cache.columnWeightChanged(table.columns().get(2)));
        assertCache(cache, table);

        cache.fillPolicyChanged();
        assertEquals(2, cache.columnWeightChanged(table.columns().get(2)));
        assertCache(cache, table);
        assertEquals(160, cache.widthAt(2), DELTA);
    }

    @Test
    void testSharedAbsorberUnderLast(FxRobot robot) {
        Table table = new Table(users(50));
        robot.interact(() -> {
            table.setColumnsWidth(40);
            VFXTable.setWeight(table.columns().get(2), 1);
            table.resize(400, 400);
        });
        TestCache cache = new TestCache(table);

        assertSharedAbsorbers(cache, table);
    }

    @Test
    void testSharedAbsorberWeighted(FxRobot robot) {
        Table table = new Table(users(50));
        robot.interact(() -> {
            table.setColumnsWidth(40);
            table.setColumnsFillPolicy(ColumnsFillPolicy.WEIGHTED);
            VFXTable.setWeight(table.columns().getFirst(), -1);
            VFXTable.setWeight(table.columns().get(1), 1);
            VFXTable.setWeight(table.columns().get(3), 2);
            table.resize(400, 400);
        });
        TestCache cache = new TestCache(table);
        assertSharedAbsorbers(cache, table, 1, 3);

        robot.interact(() -> VFXTable.setWeight(table.columns().get(3), 0));
        cache.columnWeightChanged(table.columns().get(3));
        assertSharedAbsorbers(cache, table);

        robot.interact(() -> VFXTable.setWeight(table.columns().get(1), 0));
        cache.columnWeightChanged(table.columns().get(1));
        assertSharedAbsorbers(cache, table);
    }

    @Test
    void testSharedAbsorberWhileOverflowing(FxRobot robot) {
        Table table = new Table(users(50));
        robot.interact(() -> {
            table.setColumnsFillPolicy(ColumnsFillPolicy.WEIGHTED);
            VFXTable.setWeight(table.columns().get(2), 1);
            VFXTable.setWeight(table.columns().get(4), 1);
            table.resize(400, 400);
        });
        TestCache cache = new TestCache(table);

        assertSharedAbsorbers(cache, table, 2, 4);
    }

    //================================================================================
    // Columns list
    //================================================================================

    @Test
    void testColumnsChanged(FxRobot robot) {
        Table table = new Table(users(50));
        robot.interact(() -> table.resize(400, 400));
        TestCache cache = new TestCache(table);
        table.columns().addListener(cache::columnsChanged);
        robot.interact(() -> {
            table.columns().get(2).setUserPrefWidth(150);
            table.columns().get(4).setUserPrefWidth(250);
            table.columns().get(6).setUserPrefWidth(300);
        });
        cache.columnResized(table.columns().get(2));
        cache.columnResized(table.columns().get(4));
        cache.columnResized(table.columns().get(6));
        assertCache(cache, table);

        robot.interact(() -> table.columns().remove(4));
        assertCache(cache, table);

        robot.interact(() -> {
            VFXTableColumn<User, ? extends VFXTableCell<User>> column = new ArrayList<>(Table.defaultColumns()).getFirst();
            column.setUserPrefWidth(220);
            table.columns().addFirst(column);
        });
        assertCache(cache, table);

        robot.interact(() -> {
            List<VFXTableColumn<User, ? extends VFXTableCell<User>>> reversed = new ArrayList<>(table.columns()).reversed();
            table.columns().setAll(reversed);
        });
        assertCache(cache, table);

        robot.interact(() -> table.setColumnsWidth(100));
        cache.columnsSizeChanged();
        assertCache(cache, table);

        robot.interact(() -> table.columns().clear());
        assertCache(cache, table);
        assertEquals(0, cache.get(), DELTA);
    }

    //================================================================================
    // Misc
    //================================================================================

    static void assertSharedAbsorbers(TestCache cache, VFXTable<User> table, int... shared) {
        Set<Integer> expected = new HashSet<>();
        for (int index : shared) expected.add(index);
        for (int i = 0; i < table.columns().size(); i++) {
            assertEquals(expected.contains(i), cache.isSharedAbsorber(i), "Shared absorber at column " + i);
        }
    }

    static void assertCache(TestCache cache, VFXTable<User> table) {
        int size = table.columns().size();
        assertEquals(columnsWidth(table), cache.posAt(size), DELTA);
        for (int i = size - 1; i >= 0; i--) {
            assertEquals(columnX(table, i), cache.posAt(i), DELTA, "Position of column " + i);
        }
        for (int i = 0; i < size; i++) {
            assertEquals(columnWidth(table, i), cache.widthAt(i), DELTA, "Width of column " + i);
        }
        assertEquals(columnsWidth(table), cache.get(), DELTA);
    }

    //================================================================================
    // Inner Classes
    //================================================================================

    static class TestCache extends ColumnsLayoutCache<User> {
        TestCache(VFXTable<User> table) {
            super(table);
            init();
        }

        void columnsSizeChanged() {
            onColumnsSizeChanged();
        }

        int columnResized(VFXTableColumn<User, ?> column) {
            return onColumnResized(column);
        }

        int tableWidthChanged() {
            return onTableWidthChanged();
        }

        void fillPolicyChanged() {
            onFillPolicyChanged();
        }

        int columnWeightChanged(VFXTableColumn<User, ?> column) {
            return onColumnWeightChanged(column);
        }

        void columnsChanged(ListChangeListener.Change<? extends VFXTableColumn<User, ? extends VFXTableCell<User>>> change) {
            onColumnsChanged(change);
        }
    }
}
