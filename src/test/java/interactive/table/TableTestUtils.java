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

package interactive.table;

import java.util.Collection;
import java.util.List;
import java.util.SequencedMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

import io.github.palexdev.mfxcore.base.beans.Size;
import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.controls.SkinBase;
import io.github.palexdev.mfxcore.utils.RandomUtils;
import io.github.palexdev.mfxcore.utils.fx.CSSFragment;
import io.github.palexdev.mfxresources.fonts.MFXFontIcon;
import io.github.palexdev.mfxresources.fonts.fontawesome.FontAwesomeSolid;
import io.github.palexdev.virtualizedfx.cells.VFXSimpleTableCell;
import io.github.palexdev.virtualizedfx.cells.base.VFXTableCell;
import io.github.palexdev.virtualizedfx.enums.BufferSize;
import io.github.palexdev.virtualizedfx.enums.ColumnsLayoutMode;
import io.github.palexdev.virtualizedfx.table.*;
import io.github.palexdev.virtualizedfx.table.VFXTableHelper.FixedTableHelper;
import io.github.palexdev.virtualizedfx.table.VFXTableHelper.VariableTableHelper;
import io.github.palexdev.virtualizedfx.table.defaults.VFXDefaultTableColumn;
import io.github.palexdev.virtualizedfx.table.defaults.VFXDefaultTableRow;
import io.github.palexdev.virtualizedfx.utils.Utils;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;
import model.User;
import org.opentest4j.AssertionFailedError;
import utils.TestFXUtils;
import utils.TestFXUtils.Counter;

import static org.junit.jupiter.api.Assertions.*;
import static utils.TestFXUtils.FP_ASSERTIONS_DELTA;
import static utils.TestFXUtils.counter;

public class TableTestUtils {
    //================================================================================
    // Static Properties
    //================================================================================
    public static final Counter rowsCounter = new TestFXUtils.Counter();

    //================================================================================
    // Constructors
    //================================================================================
    private TableTestUtils() {}

    //================================================================================
    // Methods
    //================================================================================
    static void assertState(VFXTable<User> table, IntegerRange rowsRange, IntegerRange columnsRange, int cellsNum) {
        VFXTableState<User> state = table.getState();
        VFXTableHelper<User> helper = table.getHelper();
        Region columnsPane = (Region) table.lookup(".columns");
        Region rowsPane = (Region) table.lookup(".rows");

        assertNotNull(table.rowFactoryProperty().getOwner());

        if (Utils.INVALID_RANGE.equals(columnsRange)) {
            assertEquals(VFXTableState.INVALID, state);
            // Also verify that there are no nodes in the viewport
            assertTrue(columnsPane.getChildrenUnmodifiable().isEmpty());
            assertTrue(rowsPane.getChildrenUnmodifiable().isEmpty());
            return;
        }

        boolean partial = cellsNum < helper.totalCells();

        // Columns check
        assertEquals(helper.columnsRange(), columnsRange);

        // Rows and cells checks
        if (Utils.INVALID_RANGE.equals(rowsRange)) {
            if (table.getRowFactory() != null &&
                !(table.getRowsHeight() <= 0) &&
                !table.isEmpty()) {
                fail("Invalid rows range, but why");
            }
            assertTrue(state.isEmpty());
            assertEquals(0, state.cellsNum());
            assertTrue(rowsPane.getChildrenUnmodifiable().isEmpty());
        } else {
            assertEquals(helper.rowsRange(), rowsRange);
            assertEquals(rowsRange.diff() + 1, state.size());
            assertEquals(cellsNum, state.cellsNum());
        }

        ObservableList<VFXTableColumn<User, ? extends VFXTableCell<User>>> columns = table.getColumns();
        int j = 0;
        for (Integer cIdx : columnsRange) {
            try {
                VFXTableColumn<User, ? extends VFXTableCell<User>> column = columns.get(cIdx);
                assertNotNull(column);
                assertNotNull(column.getTable());
                assertNotNull(column.getCellFactory().getOwner());
                assertEquals(cIdx, column.getIndex());
                assertNotNull(column.getParent()); // Assert that the column is actually in the viewport before checking the position
                boolean inViewport = helper.isInViewport(column);
                assertEquals(inViewport, column.isVisible());
                if (inViewport) {
                    assertLayout(table, j, column);
                } else {
                    assertFalse(column.isVisible());
                }
            } catch (Exception ex) {
                fail(ex);
            }
            j++;
        }

        ObservableList<User> items = table.getItems();
        SequencedMap<Integer, VFXTableRow<User>> rows = state.getRowsByIndexUnmodifiable();
        if (rows.isEmpty()) return;
        int i = 0;
        for (Integer rIdx : rowsRange) {
            VFXTableRow<User> row;
            try {
                row = rows.get(rIdx);
                assertNotNull(row);
            } catch (AssertionFailedError err) {
                System.err.printf("Null row for index %d%n".formatted(rIdx));
                throw err;
            }

            assertEquals(rIdx, row.getIndex());
            assertEquals(columnsRange, row.getColumnsRange());
            assertEquals(items.get(rIdx), row.getItem());
            assertLayout(table, i, row);

            SequencedMap<Integer, VFXTableCell<User>> cells = row.getCellsUnmodifiable();
            j = 0;
            for (Integer cIdx : columnsRange) {
                VFXTableCell<User> cell = null;
                try {
                    cell = cells.get(cIdx);
                    assertNotNull(cell);
                } catch (AssertionFailedError err) {
                    System.err.printf("Null cell in row %d for column %d%n".formatted(rIdx, cIdx));
                    if (!partial) throw err;
                }

                if (cell instanceof VFXSimpleTableCell<User, ?> sCell) {
                    assertEquals(columns.get(cIdx), sCell.getColumn());
                    assertEquals(row, sCell.getRow());
                    assertEquals(cIdx, sCell.getIndex());
                    if (!(sCell instanceof EmptyCell)) {
                        assertEquals(items.get(rIdx), sCell.getItem());
                    }
                    assertEquals(sCell.isVisible(), sCell.getColumn().isVisible());
                    if (helper.isInViewport(sCell.getColumn())) {
                        assertLayout(table, j, sCell);
                    } else {
                        assertFalse(sCell.isVisible());
                    }
                } else {
                    System.err.println("Cannot assert for cell of type: " + cell);
                }
                j++;
            }
            i++;
        }
    }

    static void assertState(VFXTable<User> table, IntegerRange rowsRange, IntegerRange columnsRange) {
        assertState(table, rowsRange, columnsRange, table.getHelper().totalCells());
    }

    static void assertRowsCounter(int created, int ixUpdates, int itUpdates, int deCached, int cached, int disposed) {
        assertEquals(created, rowsCounter.created);
        assertEquals(ixUpdates, rowsCounter.getUpdIndexCnt());
        assertEquals(itUpdates, rowsCounter.getUpdItemCnt());
        assertEquals(deCached, rowsCounter.getFromCache());
        assertEquals(cached, rowsCounter.getToCache());
        assertEquals(disposed, rowsCounter.getDisposed());
        rowsCounter.reset();
    }

    static void assertLayout(VFXTable<User> table, int cIdx, VFXTableColumn<User, ? extends VFXTableCell<User>> column) {
        VFXTableHelper<User> helper = table.getHelper();
        Bounds bounds = column.getBoundsInParent();
        double x = 0;
        double w = helper.getColumnWidth(column);
        double h = table.getColumnsSize().getHeight();
        if (table.getColumnsLayoutMode() == ColumnsLayoutMode.FIXED) {
            x = cIdx * table.getColumnsSize().getWidth();
        } else {
            for (VFXTableColumn<User, ? extends VFXTableCell<User>> c : table.getColumns()) {
                if (c == column) break;
                x += helper.getColumnWidth(c);
            }
        }
        try {
            assertEquals(x, bounds.getMinX(), FP_ASSERTIONS_DELTA); // Fucking scaling settings may make the tests fail for no real reason
            assertEquals(0, bounds.getMinY());
            assertEquals(w, bounds.getWidth(), FP_ASSERTIONS_DELTA); // Fucking scaling settings may make the tests fail for no real reason
            assertEquals(h, column.getLayoutBounds().getHeight()); // This would fail with `bounds` because there's the overlay node!
        } catch (AssertionFailedError err) {
            System.err.printf("Failed column layout assertion for column %s%n".formatted(column.getText()));
            throw err;
        }
    }

    static void assertLayout(VFXTable<User> table, int rIdx, VFXTableRow<User> row) {
        Bounds bounds = row.getBoundsInParent();
        double w = table.getVirtualMaxX();
        double h = table.getRowsHeight();
        double y = h * rIdx;
        try {
            assertEquals(0, bounds.getMinX());
            assertEquals(y, bounds.getMinY());
            assertEquals(w, bounds.getWidth(), FP_ASSERTIONS_DELTA); // Fucking scaling settings may make the tests fail for no real reason
            assertEquals(h, bounds.getHeight(), FP_ASSERTIONS_DELTA); // Fucking scaling settings may make the tests fail for no real reason
        } catch (AssertionFailedError err) {
            System.err.printf("Failed layout assertion for row at index %d%n".formatted(rIdx));
            throw err;
        }
    }

    static void assertLayout(VFXTable<User> table, int layoutIdx, VFXSimpleTableCell<User, ?> cell) {
        if (cell == null) return;
        VFXTableHelper<User> helper = table.getHelper();
        int cellIdx = cell.getIndex();
        ObservableList<VFXTableColumn<User, ? extends VFXTableCell<User>>> columns = table.getColumns();
        VFXTableColumn<User, ? extends VFXTableCell<User>> column = columns.get(cellIdx);
        Bounds bounds = cell.toNode().getBoundsInParent();

        double x = 0;
        double w = helper.getColumnWidth(column);
        double h = table.getRowsHeight();

        if (table.getColumnsLayoutMode() == ColumnsLayoutMode.FIXED) {
            x = table.getColumnsSize().getWidth() * layoutIdx;
        } else {
            for (int i = 0; i < cellIdx; i++) {
                x += helper.getColumnWidth(columns.get(i));
            }
        }

        try {
            assertEquals(x, bounds.getMinX(), FP_ASSERTIONS_DELTA); // Fucking scaling settings may make the tests fail for no real reason
            assertEquals(0, bounds.getMinY());
            assertEquals(w, bounds.getWidth(), FP_ASSERTIONS_DELTA); // Fucking scaling settings may make the tests fail for no real reason
            assertEquals(h, bounds.getHeight(), FP_ASSERTIONS_DELTA); // Fucking scaling settings may make the tests fail for no real reason
        } catch (AssertionError err) {
            System.err.printf("Failed cell layout assertion for column %s%n".formatted(column.getText()));
            throw err;
        }
    }

    static void assertLength(VFXTable<User> table, double vLength, double hLength) {
        VFXTableHelper<User> helper = table.getHelper();
        assertEquals(hLength, helper.getVirtualMaxX(), FP_ASSERTIONS_DELTA); // Fucking scaling settings may make the tests fail for no real reason
        assertEquals(vLength, helper.getVirtualMaxY());
    }

    static void assertScrollable(VFXTable<User> table, double maxVScroll, double maxHScroll) {
        assertEquals(maxVScroll, table.getMaxVScroll());
        assertEquals(maxHScroll, table.getMaxHScroll());
    }

    static void setRandomColumnWidth(VFXTable<User> table, double w) {
        setColumnWidth(table, RandomUtils.random.nextInt(0, table.getColumns().size()), w);
    }

    static void setColumnWidth(VFXTable<User> table, int index, double w) {
        table.getColumns().get(index).resize(w);
    }

    //================================================================================
    // Internal Classes
    //================================================================================
    public static class Table extends VFXTable<User> {
        private static int priority = 0;

        {
            setColumnsSize(Size.of(180, 32));

            CSSFragment.Builder.build()
                .addSelector(".vfx-table")
                .border("#353839")
                .closeSelector()
                .addSelector(".vfx-table > .viewport > .columns")
                .border("transparent transparent #353839 transparent")
                .closeSelector()
                .addSelector(".vfx-table > .viewport > .columns > .vfx-column")
                .padding("0px 10px 0px 10px")
                .border("transparent #353839 transparent transparent")
                .closeSelector()
                .addSelector(".vfx-table > .viewport > .columns > .vfx-column:hover > .overlay")
                .addSelector(".vfx-table > .viewport > .columns > .vfx-column:dragged > .overlay")
                .background("rgba(53, 56, 57, 0.1)")
                .closeSelector()
                .addSelector(".vfx-table > .viewport > .rows > .vfx-row")
                .border("#353839")
                .borderInsets("1.25px")
                .addStyle("-fx-border-width: 0.5px")
                .closeSelector()
                .addSelector(".vfx-table > .viewport > .rows > .vfx-row > .table-cell")
                .padding("0px 10px 0px 10px")
                .closeSelector()
                .applyOn(this);
        }

        public Table(ObservableList<User> items) {
            this(items, columns());
        }

        public Table(ObservableList<User> items, Collection<VFXTableColumn<User, ? extends VFXTableCell<User>>> columns) {
            super(items, columns);
        }

        static Collection<VFXTableColumn<User, ? extends VFXTableCell<User>>> columns() {
            int ICON_SIZE = 18;
            Color ICON_COLOR = Color.rgb(53, 57, 53);

            TestColumn<String> firstNameColumn = new TestColumn<>("First name", priority());
            firstNameColumn.setCellFactory(u -> factory(u, User::firstName));
            firstNameColumn.setGraphic(new MFXFontIcon(FontAwesomeSolid.USER, ICON_SIZE, ICON_COLOR));

            TestColumn<String> lastNameColumn = new TestColumn<>("Last name", priority());
            lastNameColumn.setCellFactory(u -> factory(u, User::lastName));
            lastNameColumn.setGraphic(new MFXFontIcon(FontAwesomeSolid.USER, ICON_SIZE, ICON_COLOR));

            TestColumn<Integer> birthColumn = new TestColumn<>("Birth year", priority());
            birthColumn.setCellFactory(u -> factory(u, User::birthYear));
            birthColumn.setGraphic(new MFXFontIcon(FontAwesomeSolid.CAKE_CANDLES, ICON_SIZE, ICON_COLOR));

            TestColumn<String> zodiacColumn = new TestColumn<>("Zodiac Sign", priority());
            zodiacColumn.setCellFactory(u -> factory(u, User::zodiac));
            zodiacColumn.setGraphic(new MFXFontIcon(FontAwesomeSolid.STAR, ICON_SIZE, ICON_COLOR));

            TestColumn<String> countryColumn = new TestColumn<>("Country", priority());
            countryColumn.setCellFactory(u -> factory(u, User::country));
            countryColumn.setGraphic(new MFXFontIcon(FontAwesomeSolid.GLOBE, ICON_SIZE, ICON_COLOR));

            TestColumn<String> bloodColumn = new TestColumn<>("Blood", priority());
            bloodColumn.setCellFactory(u -> factory(u, User::blood));
            bloodColumn.setGraphic(new MFXFontIcon(FontAwesomeSolid.DROPLET, ICON_SIZE, ICON_COLOR));

            TestColumn<String> animalColumn = new TestColumn<>("Pet", priority());
            animalColumn.setCellFactory(u -> factory(u, User::pet));
            animalColumn.setGraphic(new MFXFontIcon(FontAwesomeSolid.PAW, ICON_SIZE, ICON_COLOR));

            return List.of(firstNameColumn, lastNameColumn, birthColumn, zodiacColumn, countryColumn, bloodColumn, animalColumn);
        }

        static <E> UserCell<E> factory(User user, Function<User, E> extractor) {
            return factory(user, extractor, c -> {});
        }

        static <E> UserCell<E> factory(User user, Function<User, E> extractor, Consumer<UserCell<E>> config) {
            Function<User, UserCell<E>> f = u -> new UserCell<>(u, extractor);
            f = f.andThen(c -> {
                config.accept(c);
                counter.created();
                return c;
            });
            return f.apply(user);
        }

        static int priority() {
            return priority++;
        }

        public static List<EmptyColumn> emptyColumns(String baseText, int cnt) {
            return IntStream.range(0, cnt)
                .mapToObj(i -> new EmptyColumn(baseText + " " + (char) ('A' + i), priority()))
                .toList();
        }

        public static List<EmptyColumn> emptyColumns(int cnt) {
            return emptyColumns("Empty", cnt);
        }

        public Table addEmptyColumns(int cnt) {
            getColumns().addAll(emptyColumns(cnt));
            return this;
        }

        @Override
        public void setBufferSize(BufferSize bufferSize) {
            setRowsBufferSize(bufferSize);
            setColumnsBufferSize(bufferSize);
        }

        @Override
        protected Function<User, VFXTableRow<User>> defaultRowFactory() {
            return TestRow::new;
        }

        @Override
        protected Function<ColumnsLayoutMode, VFXTableHelper<User>> defaultHelperFactory() {
            return mode -> mode == ColumnsLayoutMode.FIXED ? new FixedTableHelper<>(this) : new VariableTableHelper<>(this) {
                @Override
                public boolean layoutCell(int layoutIdx, VFXTableCell<User> node) {
                    if (super.layoutCell(layoutIdx, node)) {
                        counter.layout();
                        return true;
                    }
                    return false;
                }
            };
        }

        @Override
        protected SkinBase<?, ?> buildSkin() {
            return new VFXTableSkin<>(this) {
                @Override
                protected void onLayoutCompleted(boolean done) {
                    super.onLayoutCompleted(done);
                    if (getColumnsLayoutMode() == ColumnsLayoutMode.VARIABLE) return;
                    if (done) counter.layout();
                }
            };
        }
    }

    public static class TestColumn<E> extends VFXDefaultTableColumn<User, UserCell<E>> implements Comparable<TestColumn<E>> {
        private final int priority;

        public TestColumn(String text, int priority) {
            super(text);
            this.priority = priority;
        }

        @Override
        public int compareTo(TestColumn<E> o) {
            return Integer.compare(priority, o.priority);
        }
    }

    public static class TestRow extends VFXDefaultTableRow<User> {
        public TestRow(User item) {
            super(item);
            rowsCounter.created();
        }

        @Override
        public void updateIndex(int index) {
            super.updateIndex(index);
            rowsCounter.index();
        }

        @Override
        public void updateItem(User item) {
            super.updateItem(item);
            rowsCounter.item();
        }

        @Override
        public void onDeCache() {
            rowsCounter.fCache();
        }

        @Override
        public void onCache() {
            rowsCounter.tCache();
        }

        @Override
        public void dispose() {
            super.dispose();
            rowsCounter.disposed();
        }
    }

    public static class UserCell<E> extends VFXSimpleTableCell<User, E> {

        public UserCell(User item, Function<User, E> extractor) {
            super(item, extractor);
        }

        public UserCell(User item, Function<User, E> extractor, StringConverter<E> converter) {
            super(item, extractor, converter);
        }

        @Override
        public void updateIndex(int index) {
            super.updateIndex(index);
            counter.index();
        }

        @Override
        public void updateItem(User item) {
            super.updateItem(item);
            counter.item();
        }

        @Override
        public void onDeCache() {
            counter.fCache();
        }

        @Override
        public void onCache() {
            counter.tCache();
        }

        @Override
        public void dispose() {
            counter.disposed();
            super.dispose();
        }
    }

    public static class EmptyColumn extends TestColumn<String> {
        {
            setCellFactory(item -> {
                EmptyCell c = new EmptyCell(item, this);
                counter.created();
                return c;
            });
        }

        public EmptyColumn(String text, int priority) {
            super(text, priority);
        }
    }

    public static class EmptyCell extends UserCell<String> {
        public EmptyCell(User item, EmptyColumn column) {
            super(item, u -> "");
            setExtractor(u -> data(column));
        }

        String data(EmptyColumn column) {
            return "Column %s and Row %d".formatted(column.getText(), getIndex());
        }
    }
}
