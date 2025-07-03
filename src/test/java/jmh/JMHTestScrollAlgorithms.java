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

package jmh;

import java.util.*;
import java.util.concurrent.TimeUnit;

import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import jmh.JMHCommons.MockCell;
import jmh.JMHCommons.MockState;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import utils.Utils;

import static io.github.palexdev.virtualizedfx.utils.Utils.INVALID_RANGE;
import static io.github.palexdev.virtualizedfx.utils.Utils.intersection;

@SuppressWarnings("NewClassNamingConvention")
public class JMHTestScrollAlgorithms {
    private static final List<Integer> items = Utils.items(100);
    private static final IntegerRange lastRange = IntegerRange.of(20, 49);
    private static final IntegerRange newRange = new IntegerRange(25, 54);
    private static final MockState oldState = MockState.generate(lastRange);

    @Test
    void runBenchmarks() throws Exception {
        Options opt = new OptionsBuilder()
            .include(this.getClass().getName() + ".*")
            .mode(Mode.Throughput)
            .warmupTime(TimeValue.seconds(1))
            .warmupIterations(6)
            .threads(1)
            .measurementIterations(6)
            .forks(1)
            .shouldFailOnError(true)
            .shouldDoGC(true)
            .build();
        new Runner(opt).run();
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void classicAlgorithm() {
        MockState newState = new MockState(newRange);
        Deque<Integer> needed = new ArrayDeque<>();
        for (Integer index : newRange) {
            MockCell common = oldState.removeCell(index);
            if (common != null) {
                newState.addCell(index, common);
                continue;
            }
            needed.add(index);
        }

        Iterator<MockCell> it = oldState.cells.values().iterator();
        while (it.hasNext()) {
            int idx = needed.removeFirst();
            MockCell c = it.next();
            c.updateIndex(idx);
            c.updateItem(items.get(idx));
            newState.addCell(idx, c);
            it.remove();
        }
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void alternativeAlgorithm() {
        Set<Integer> expandedRange = IntegerRange.expandRangeToSet(newRange);
        IntegerRange intersection = intersection(lastRange, newRange);
        MockState newState = new MockState(newRange);
        if (!INVALID_RANGE.equals(intersection))
            for (Integer index : intersection) {
                newState.addCell(index, oldState.removeCell(index));
                expandedRange.remove(index);
            }

        for (Integer index : expandedRange) {
            Integer item = items.get(index);
            MockCell c;
            if (!oldState.cells.isEmpty()) {
                c = oldState.cells.pollFirstEntry().getValue();
                c.updateIndex(index);
                c.updateItem(item);
            } else {
                c = new MockCell(index);
                c.updateIndex(index);
            }
            newState.addCell(index, c);
        }
    }
}
