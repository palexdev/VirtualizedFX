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

import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.utils.GridUtils;
import jmh.JMHCommons.MockCell;
import jmh.JMHCommons.MockState2D;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import utils.Utils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("NewClassNamingConvention")
public class JMHTestUpdateAlgorithm2D {
	private static final int nColumns = 5;
	private static final List<Integer> items = Utils.items(100);

	// Common old state
	private static final IntegerRange orRange = IntegerRange.of(0, 4);
	private static final IntegerRange ocRange = IntegerRange.of(0, 4);
	private static final MockState2D oldState = MockState2D.generate2D(nColumns, orRange, ocRange);

	// No intersect
	private static final IntegerRange nrRangeNI = IntegerRange.of(5, 9);
	private static final IntegerRange ncRangeNI = IntegerRange.of(5, 9);

	// Partial intersect
	private static final IntegerRange nrRangePI = IntegerRange.of(3, 7);

	// Intersect
	private static final IntegerRange nrRangeI = IntegerRange.of(3, 7);
	private static final IntegerRange ncRangeI = IntegerRange.of(3, 7);

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
	public void moveReuseCreateAlgorithmNoIntersect() {
		MockState2D newState = MockState2D.generate2D(nColumns, nrRangeNI, ncRangeNI);
		moveReuseCreateAlgorithm(oldState, newState);
	}

	@Benchmark
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void moveReuseCreateAlgorithmPartialIntersect() {
		MockState2D newState = MockState2D.generate2D(nColumns, nrRangePI, ncRangeNI);
		moveReuseCreateAlgorithm(oldState, newState);
	}

	@Benchmark
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void moveReuseCreateAlgorithmIntersect() {
		MockState2D newState = MockState2D.generate2D(nColumns, nrRangeI, ncRangeI);
		moveReuseCreateAlgorithm(oldState, newState);
	}

	@Benchmark
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void intersectionAlgorithmNoIntersect() {
		MockState2D newState = MockState2D.generate2D(nColumns, nrRangeNI, ncRangeNI);
		intersectionAlgorithm(oldState, newState);
	}

	@Benchmark
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void intersectionAlgorithmPartialIntersect() {
		MockState2D newState = MockState2D.generate2D(nColumns, nrRangePI, ncRangeNI);
		intersectionAlgorithm(oldState, newState);
	}

	@Benchmark
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void intersectionAlgorithmIntersect() {
		MockState2D newState = MockState2D.generate2D(nColumns, nrRangeI, ncRangeI);
		intersectionAlgorithm(oldState, newState);
	}

	//================================================================================
	// Algorithms
	//================================================================================
	private void moveReuseCreateAlgorithm(MockState2D oldState, MockState2D newState) {
		Set<Integer> remaining = new LinkedHashSet<>();
		outer_loop:
		for (Integer rIdx : newState.rRange) {
			for (Integer cIdx : newState.cRange) {
				int linear = GridUtils.subToInd(nColumns, rIdx, cIdx);
				if (linear >= items.size()) break outer_loop;
				MockCell c = oldState.removeCell(linear);
				if (c == null) {
					remaining.add(linear);
					continue;
				}
				newState.addCell(linear, c);
			}
		}
		remainingAlgorithm(remaining, oldState, newState);
	}

	private void intersectionAlgorithm(MockState2D oldState, MockState2D newState) {
		Set<Integer> remaining = new LinkedHashSet<>();
		for (Integer rIdx : newState.rRange) {
			for (Integer cIdx : newState.cRange) {
				int linear = GridUtils.subToInd(nColumns, rIdx, cIdx);
				if (!oldState.cells.containsKey(linear)) {
					remaining.add(linear);
					continue;
				}
				newState.addCell(linear, oldState.removeCell(linear));
			}
		}
		remainingAlgorithm(remaining, oldState, newState);
	}

	private void remainingAlgorithm(Set<Integer> remaining, MockState2D oldState, MockState2D newState) {
		for (Integer index : remaining) {
			Integer item = items.get(index);
			MockCell c;
			if (!oldState.isEmpty()) {
				c = oldState.cells.pollFirstEntry().getValue();
				c.updateIndex(index);
				c.updateItem(item);
			} else {
				c = new MockCell(item);
				c.updateIndex(index);
			}
			newState.addCell(index, c);
		}
	}
}
