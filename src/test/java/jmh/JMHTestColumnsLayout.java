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

package jmh;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import io.github.palexdev.mfxcore.utils.NumberUtils;
import io.github.palexdev.virtualizedfx.cells.base.VFXTableCell;
import io.github.palexdev.virtualizedfx.enums.ColumnsFillPolicy;
import io.github.palexdev.virtualizedfx.table.ColumnsLayoutCache;
import io.github.palexdev.virtualizedfx.table.VFXTable;
import io.github.palexdev.virtualizedfx.table.VFXTableColumn;
import io.github.palexdev.virtualizedfx.table.defaults.VFXSimpleTableColumn;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

/// Columns-axis layout math, as a function of the column count. Columns carry varied
/// `userPrefWidth` values, so the prefix sums are non-uniform and the binary search is doing real
/// work; that is the whole point of the file and it is easy to break, see the setup comment.
///
/// **The scroll-path group**, which is what the columns range cost:
///
/// - [#legacyFullScan(Blackhole)] reconstructs the pre-range behaviour: every column queried on every
///   scroll event, visibility cache cleared first. None of that code exists any more, so it is
///   reproduced locally. A lower bound on the old cost, not a faithful replay.
/// - [#currentRangeScan(Blackhole)] is the shipped algorithm: binary search the prefix sums for the
///   range, then query only what is inside it.
/// - [#currentFastDraw(Blackhole)] adds the early exit taken when the range has not moved.
/// - [#coldPositionFillAscending(Blackhole)] and [#coldPositionFillDeep(Blackhole)] isolate
///   [ColumnsLayoutCache#posAt(int)]'s cold fill under the two access patterns that matter.
///
/// This drives its own [ColumnsLayoutCache] over a detached [VFXTable] instead of going through the
/// helper, so the range mirrors below are hand-copied from `VFXDefaultTableHelper` and nothing
/// enforces that they stay in sync.
///
/// **It cannot measure the thing that matters most.** A real range means not creating a cell per
/// column per row, and that is scene-graph cost. Read these numbers as "the math got cheaper", never
/// as "the change was worth this much".
@State(Scope.Benchmark)
@SuppressWarnings("NewClassNamingConvention")
public class JMHTestColumnsLayout {
    //================================================================================
    // Properties
    //================================================================================
    private static final double TABLE_W = 1280.0;
    private static final double TABLE_H = 720.0;
    private static final double MIN_W = 80.0;
    private static final double MAX_W = 320.0;
    /// Fixed, so runs are comparable across machines and across changes to the code under test.
    private static final long SEED = 20260820L;
    /// Power of two, so the cursor can wrap with a mask.
    private static final int OFFSETS = 512;
    /// Small enough that consecutive steps usually land in the same range, which is what makes the
    /// fast-draw exit worth measuring.
    private static final double SCROLL_STEP = 13.0;
    private static final double FILL_EXTRA_W = 400.0;
    private static final int WEIGHTS_BURST = 5;

    @Param({"10", "50", "200", "500"})
    public int nColumns;

    private VFXTable<Integer> table;
    private BenchCache cache;
    private ObservableList<VFXTableColumn<Integer, ? extends VFXTableCell<Integer>>> columns;
    /// Read from the table, not hardcoded, so it follows the real default if that changes.
    private int buffer;

    private VFXTable<Integer> fillTable;
    private ObservableList<VFXTableColumn<Integer, ? extends VFXTableCell<Integer>>> fillColumns;
    private double fillWidth;
    private boolean toggle;

    private double[] offsets;
    private int cursor;
    /// Stands in for [VFXTable#hPosProperty()], which is clamped through the helper's `maxHScroll`.
    private double scrollX;

    /// Stands in for the deleted visibility third of [ColumnsLayoutCache], used only by
    /// [#legacyFullScan(Blackhole)] to reproduce its O(n) per-event reset.
    private boolean[] legacyVisibility;

    private int rangeMin = -1;
    private int rangeMax = -1;
    private int lastMin = -1;
    private int lastMax = -1;

    //================================================================================
    // Setup
    //================================================================================

    /// JMH forks once per benchmark/parameter combination, so each JVM runs one trial. That is what
    /// makes starting the toolkit here and exiting it in [#tearDown()] safe: FX cannot be restarted.
    @Setup(Level.Trial)
    public void setup() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already running
        }

        table = new VFXTable<>(FXCollections.observableArrayList(), buildColumns());
        table.setColumnsWidth(MIN_W);
        table.resize(TABLE_W, TABLE_H);
        columns = table.columns();
        buffer = table.getColumnsBufferSize().val();

        cache = new BenchCache(table);
        legacyVisibility = new boolean[nColumns];

        fillTable = new VFXTable<>(FXCollections.observableArrayList(), buildColumns());
        fillTable.setColumnsWidth(MIN_W);
        fillColumns = fillTable.columns();
        double naturalTotal = 0.0;
        for (VFXTableColumn<Integer, ? extends VFXTableCell<Integer>> column : fillColumns)
            naturalTotal += Math.max(column.getUserPrefWidth(), MIN_W);
        fillWidth = naturalTotal + FILL_EXTRA_W;
        fillTable.resize(fillWidth, TABLE_H);
        fillTable.setColumnsFillPolicy(ColumnsFillPolicy.WEIGHTED);
        for (int i = 0; i < nColumns; i += 4) VFXTable.setWeight(fillColumns.get(i), 1);
        fillTable.getHelper().getVirtualMaxX();
        fillTable.getHelper().columnsRange();

        // The position warm-up must ascend; a single call at the last index would recurse once per
        // column, which is what coldPositionFillDeep measures separately
        double totalW = cache.get();
        for (int i = 0; i < nColumns; i++) cache.posAt(i);

        double maxScroll = Math.max(0.0, totalW - TABLE_W);
        offsets = new double[OFFSETS];
        double x = 0.0;
        for (int i = 0; i < OFFSETS; i++) {
            offsets[i] = x;
            x += SCROLL_STEP;
            if (x > maxScroll) x = 0.0;
        }
        cursor = 0;
    }

    private List<VFXTableColumn<Integer, ? extends VFXTableCell<Integer>>> buildColumns() {
        Random random = new Random(SEED);
        List<VFXTableColumn<Integer, ? extends VFXTableCell<Integer>>> cols = new ArrayList<>(nColumns);
        for (int i = 0; i < nColumns; i++) {
            VFXSimpleTableColumn<Integer, VFXTableCell<Integer>> column = new VFXSimpleTableColumn<>("C" + i);
            // setUserPrefWidth, not setPrefWidth: the cache reads the former. v1's naturalWidth used
            // prefWidth(-1), so the port silently left every column at the baseline, making the whole
            // benchmark uniform
            column.setUserPrefWidth(MIN_W + random.nextDouble() * (MAX_W - MIN_W));
            cols.add(column);
        }
        return cols;
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        cache.dispose();
        cache = null;
        table = null;
        columns = null;
        fillTable = null;
        fillColumns = null;
        legacyVisibility = null;
        Platform.exit();
    }

    private double nextOffset() {
        return offsets[cursor++ & (OFFSETS - 1)];
    }

    //================================================================================
    // Mirrors of production, keep in sync
    //================================================================================

    /// Largest index whose x position is `<= x`. Valid because positions are a prefix sum.
    private int columnAt(double x) {
        int lo = 0;
        int hi = nColumns - 1;
        int res = 0;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (cache.posAt(mid) <= x) {
                res = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return res;
    }

    private int firstColumn() {
        if (nColumns == 0) return 0;
        return NumberUtils.clamp(columnAt(scrollX), 0, nColumns - 1);
    }

    private int visibleColumns() {
        if (nColumns == 0) return 0;
        return columnAt(scrollX + TABLE_W) - firstColumn() + 1;
    }

    private int totalColumns() {
        int visible = visibleColumns();
        return visible == 0 ? 0 : Math.min(visible + buffer * 2, nColumns);
    }

    /// Writes into [#rangeMin]/[#rangeMax] rather than allocating an `IntegerRange`, which is not
    /// what is being measured. Runs three binary searches, not two, because production does
    ///
    /// @return whether the range is valid
    private boolean computeColumnsRange() {
        int needed = totalColumns();
        if (needed == 0) {
            rangeMin = -1;
            rangeMax = -1;
            return false;
        }
        int start = Math.max(0, firstColumn() - buffer);
        int end = Math.min(nColumns - 1, start + needed - 1);
        if (end - start + 1 < needed) start = Math.max(0, end - needed + 1);
        rangeMin = start;
        rangeMax = end;
        return true;
    }

    /// Reconstruction of the deleted `computeVisibility`, minus the scene guards, using [#scrollX].
    private boolean legacyIsInViewport(int index, VFXTableColumn<Integer, ?> column) {
        double columnX = cache.posAt(index);
        double columnW = cache.widthAt(column.getIndex());
        return (columnX + columnW >= scrollX) && (columnX <= scrollX + TABLE_W);
    }

    //================================================================================
    // Benchmarks
    //================================================================================

    /// Before the columns range. A lower bound on the real old cost, see the notes doc.
    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void legacyFullScan(Blackhole bh) {
        scrollX = nextOffset();
        Arrays.fill(legacyVisibility, false); // The O(n) clearVisibilityCache() pass
        for (int i = 0; i < nColumns; i++) {
            VFXTableColumn<Integer, ? extends VFXTableCell<Integer>> column = columns.get(i);
            legacyVisibility[i] = legacyIsInViewport(i, column);
            if (!legacyVisibility[i]) continue;
            bh.consume(cache.posAt(i));
            bh.consume(cache.widthAt(column.getIndex()));
        }
    }

    /// The shipped algorithm: compute the range, query only what is in it.
    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void currentRangeScan(Blackhole bh) {
        scrollX = nextOffset();
        if (!computeColumnsRange()) return;
        for (int i = rangeMin; i <= rangeMax; i++) {
            bh.consume(cache.posAt(i));
            bh.consume(cache.widthAt(columns.get(i).getIndex()));
        }
    }

    /// Same, plus the fast-draw exit `VFXTableManager.onPositionChanged` takes when the range has
    /// not moved, which should be most scroll events.
    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void currentFastDraw(Blackhole bh) {
        scrollX = nextOffset();
        if (!computeColumnsRange()) return;
        bh.consume(rangeMin);
        bh.consume(rangeMax);
        if (rangeMin == lastMin && rangeMax == lastMax) return;
        lastMin = rangeMin;
        lastMax = rangeMax;
        for (int i = rangeMin; i <= rangeMax; i++) {
            bh.consume(cache.posAt(i));
            bh.consume(cache.widthAt(columns.get(i).getIndex()));
        }
    }

    /// Ascending, which is how the layout pass walks columns: recursion never goes past one frame.
    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void coldPositionFillAscending(Blackhole bh) {
        cache.reset();
        for (int i = 0; i < nColumns; i++) bh.consume(cache.posAt(i));
    }

    /// A single high probe, which is what binary search does: same entries filled, `nColumns` frames
    /// deep. Measures *faster* than ascending, not slower; the notes doc explains why, and why this
    /// is still the one to watch.
    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void coldPositionFillDeep(Blackhole bh) {
        cache.reset();
        bh.consume(cache.posAt(nColumns - 1));
    }

    /// Walks the current range reading positions.
    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void posScan(Blackhole bh) {
        scrollX = nextOffset();
        if (!computeColumnsRange()) return;
        for (int i = rangeMin; i <= rangeMax; i++) {
            bh.consume(cache.posAt(i));
            bh.consume(cache.widthAt(i));
        }
    }

    /// Two binary searches per pulse, the pattern [#visibleColumns()] uses.
    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void columnAtSearch(Blackhole bh) {
        scrollX = nextOffset();
        bh.consume(columnAt(scrollX));
        bh.consume(columnAt(scrollX + TABLE_W));
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void rebuildOnColumnsChanged(Blackhole bh) {
        cache.reset();
        bh.consume(cache.get());
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void columnsSizeChanged(Blackhole bh) {
        table.setColumnsWidth((toggle = !toggle) ? MIN_W + 20.0 : MIN_W);
        bh.consume(table.getHelper().getVirtualMaxX());
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void fillPolicyChanged(Blackhole bh) {
        fillTable.setColumnsFillPolicy((toggle = !toggle) ? ColumnsFillPolicy.LAST : ColumnsFillPolicy.WEIGHTED);
        bh.consume(fillTable.getHelper().getVirtualMaxX());
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void columnWeightChanged(Blackhole bh) {
        VFXTable.setWeight(fillColumns.get(1), (toggle = !toggle) ? 1 : 0);
        bh.consume(fillTable.getHelper().getVirtualMaxX());
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void columnWeightBurst(Blackhole bh) {
        int weight = (toggle = !toggle) ? 1 : 0;
        for (int i = 0, set = 0; i < nColumns && set < WEIGHTS_BURST; i++) {
            if (i % 4 == 0) continue;
            VFXTable.setWeight(fillColumns.get(i), weight);
            set++;
        }
        bh.consume(fillTable.getHelper().getVirtualMaxX());
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void tableWidthChanged(Blackhole bh) {
        fillTable.resize((toggle = !toggle) ? fillWidth + 50.0 : fillWidth, TABLE_H);
        bh.consume(fillTable.getHelper().getVirtualMaxX());
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void columnResized(Blackhole bh) {
        fillColumns.get(nColumns / 2).setUserPrefWidth((toggle = !toggle) ? MAX_W + 10.0 : MAX_W);
        bh.consume(fillTable.getHelper().getVirtualMaxX());
        bh.consume(fillTable.getHelper().columnsRange());
    }

    //================================================================================
    // Runner
    //================================================================================

    @Test
    void runBenchmarks() throws Exception {
        Options opt = new OptionsBuilder()
            .include(this.getClass().getName() + ".*")
            .mode(Mode.AverageTime)
            .timeUnit(TimeUnit.MICROSECONDS)
            .warmupTime(TimeValue.seconds(1))
            .warmupIterations(3)
            .measurementTime(TimeValue.seconds(2))
            .measurementIterations(5)
            .threads(1)
            .forks(1)
            .shouldFailOnError(true)
            .shouldDoGC(true)
            .build();
        new Runner(opt).run();
    }

    //================================================================================
    // Internal Classes
    //================================================================================

    /// The real [ColumnsLayoutCache] over a detached table.
    private static class BenchCache extends ColumnsLayoutCache<Integer> {

        public BenchCache(VFXTable<Integer> table) {
            super(table);
            reset();
        }

        /// The production "everything changed" path: rebuilds and re-reads every pref. The argument
        /// is unused by the implementation.
        void reset() {
            onColumnsChanged(null);
        }
    }
}
