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
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

@State(Scope.Benchmark)
@SuppressWarnings("NewClassNamingConvention")
public class JMHTestColumnsChange {
    private static final double MIN_WIDTH = 100.0;

    @Param({"500", "5000"})
    public int size;

    private List<MockColumn> master;
    private List<MockColumn> columns;
    private MockCache cache;

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

    @Setup(Level.Trial)
    public void generate() {
        Random random = new Random(42);
        master = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            master.add(new MockColumn(random.nextInt(4) == 0 ? MIN_WIDTH + random.nextInt(200) : -1.0));
        }
    }

    @Setup(Level.Invocation)
    public void setup() {
        columns = new ArrayList<>(master);
        cache = new MockCache(columns);
        cache.rebuild();
        cache.posAt(size); // the position prefix is fully valid, as after a layout
    }

    @Benchmark
    public void rebuild(Blackhole blackhole) {
        cache.rebuild();
        blackhole.consume(cache.overridesWidth);
    }

    @Benchmark
    public void permutation(Blackhole blackhole) {
        cache.permutation(0, size, cache.movePermutation(0, size - 1));
        blackhole.consume(cache.overridesWidth);
    }

    @Benchmark
    public void append(Blackhole blackhole) {
        columns.add(new MockColumn(-1.0));
        cache.splice(size, 0, 1);
        blackhole.consume(cache.overridesWidth);
    }

    @Benchmark
    public void insertMiddle(Blackhole blackhole) {
        columns.add(size / 2, new MockColumn(-1.0));
        cache.splice(size / 2, 0, 1);
        blackhole.consume(cache.overridesWidth);
    }

    @Benchmark
    public void removeMiddle(Blackhole blackhole) {
        columns.remove(size / 2);
        cache.splice(size / 2, 1, 0);
        blackhole.consume(cache.overridesWidth);
    }

    @Benchmark
    public void appendRebuild(Blackhole blackhole) {
        columns.add(new MockColumn(-1.0));
        cache.rebuild();
        blackhole.consume(cache.overridesWidth);
    }

    @Benchmark
    public void insertMiddleRebuild(Blackhole blackhole) {
        columns.add(size / 2, new MockColumn(-1.0));
        cache.rebuild();
        blackhole.consume(cache.overridesWidth);
    }

    @Benchmark
    public void removeMiddleRebuild(Blackhole blackhole) {
        columns.remove(size / 2);
        cache.rebuild();
        blackhole.consume(cache.overridesWidth);
    }

    //================================================================================
    // Internal Classes
    //================================================================================

    /// Mimics [io.github.palexdev.virtualizedfx.table.VFXTableColumn]'s pref width: a boxed value behind a getter.
    static class MockColumn {
        private final Double userPrefWidth;

        MockColumn(double userPrefWidth) {this.userPrefWidth = userPrefWidth;}

        double getUserPrefWidth() {return userPrefWidth;}
    }

    /// The parts of ColumnsLayoutCache a columns change touches: rebuild as it is today, permutation and splice as
    /// proposed.
    static class MockCache {
        private final List<MockColumn> columns;
        private int columnsCount;
        private double[] userPrefWidths;
        private int overridesCount;
        private double overridesWidth;
        private double[] naturalPositions;
        private int posValidUpTo;
        private int[] cumulativeWeights;

        MockCache(List<MockColumn> columns) {this.columns = columns;}

        void rebuild() {
            columnsCount = columns.size();
            userPrefWidths = new double[columnsCount];
            overridesCount = 0;
            overridesWidth = 0.0;
            for (int i = 0; i < columnsCount; i++) {
                double pref = columns.get(i).getUserPrefWidth();
                userPrefWidths[i] = pref;
                if (pref > MIN_WIDTH) {
                    overridesCount++;
                    overridesWidth += pref;
                }
            }
            naturalPositions = new double[columnsCount + 1];
            posValidUpTo = 0;
            cumulativeWeights = new int[columnsCount + 1];
        }

        void permutation(int from, int to, int[] perm) {
            double[] tmp = new double[to - from];
            for (int i = from; i < to; i++) tmp[perm[i - from] - from] = userPrefWidths[i];
            System.arraycopy(tmp, 0, userPrefWidths, from, tmp.length);
            posValidUpTo = Math.min(posValidUpTo, from);
        }

        void splice(int from, int removedSize, int addedSize) {
            for (int i = from; i < from + removedSize; i++) {
                double pref = userPrefWidths[i];
                if (pref > MIN_WIDTH) {
                    overridesCount--;
                    overridesWidth -= pref;
                }
            }

            int newCount = columnsCount - removedSize + addedSize;
            double[] prefs = new double[newCount];
            System.arraycopy(userPrefWidths, 0, prefs, 0, from);
            for (int i = 0; i < addedSize; i++) {
                double pref = columns.get(from + i).getUserPrefWidth();
                prefs[from + i] = pref;
                if (pref > MIN_WIDTH) {
                    overridesCount++;
                    overridesWidth += pref;
                }
            }
            System.arraycopy(
                userPrefWidths, from + removedSize,
                prefs, from + addedSize,
                columnsCount - from - removedSize
            );
            userPrefWidths = prefs;

            int valid = Math.min(posValidUpTo, from);
            double[] positions = new double[newCount + 1];
            System.arraycopy(naturalPositions, 0, positions, 0, valid + 1);
            naturalPositions = positions;
            posValidUpTo = valid;
            cumulativeWeights = new int[newCount + 1];
            columnsCount = newCount;
        }

        double posAt(int index) {
            if (index > posValidUpTo) {
                for (int i = posValidUpTo + 1; i <= index; i++)
                    naturalPositions[i] = naturalPositions[i - 1] + Math.max(userPrefWidths[i - 1], MIN_WIDTH);
                posValidUpTo = index;
            }
            return naturalPositions[index];
        }

        int[] movePermutation(int from, int to) {
            int lo = Math.min(from, to);
            int hi = Math.max(from, to);
            int[] perm = new int[hi - lo + 1];
            if (from < to) {
                perm[0] = to;
                for (int k = 1; k < perm.length; k++) perm[k] = lo + k - 1;
            } else {
                for (int k = 0; k < perm.length - 1; k++) perm[k] = lo + k + 1;
                perm[perm.length - 1] = to;
            }
            return perm;
        }
    }
}
