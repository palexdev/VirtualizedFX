package jmh;

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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("NewClassNamingConvention")
public class JMHTestUpdateAlgorithms {
	private static final List<Integer> items = Utils.items(100);

	// Common old state
	private static final IntegerRange lastRange = IntegerRange.of(0, 49);
	private static final MockState oldState = MockState.generate(lastRange);

	// No intersect
	private static final IntegerRange newRangeNI = IntegerRange.of(50, 99);

	// Intersect
	private static final IntegerRange newRangeI = IntegerRange.of(24, 74);

	@Test
	void runBenchmarks() throws Exception {
		Options opt = new OptionsBuilder()
			.include(this.getClass().getName() + ".*")
			.mode(Mode.All)
			.warmupTime(TimeValue.seconds(1))
			.warmupIterations(5)
			.threads(1)
			.measurementIterations(10)
			.forks(1)
			.shouldFailOnError(true)
			.shouldDoGC(true)
			.build();
		new Runner(opt).run();
	}

	@Benchmark
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void moveReuseCreateAlgorithmNoIntersect() {
		MockState newState = MockState.generate(newRangeNI);
		moveReuseCreateAlgorithm(oldState, newState);
	}

	@Benchmark
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void moveReuseCreateAlgorithmIntersect() {
		MockState newState = MockState.generate(newRangeI);
		moveReuseCreateAlgorithm(oldState, newState);
	}

	@Benchmark
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void intersectionAlgorithmNoIntersect() {
		MockState newState = MockState.generate(newRangeNI);
		intersectionAlgorithm(oldState, newState);
	}

	@Benchmark
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void intersectionAlgorithmIntersect() {
		MockState newState = MockState.generate(newRangeI);
		intersectionAlgorithm(oldState, newState);
	}

	//================================================================================
	// Algorithms
	//================================================================================
	private void moveReuseCreateAlgorithm(MockState oldState, MockState newState) {
		Set<Integer> remaining = new LinkedHashSet<>();
		for (Integer index : newState.range) {
			MockCell c = oldState.removeCell(index);
			if (c == null) {
				remaining.add(index);
				continue;
			}
			newState.addCell(index, c);
		}
		remainingAlgorithm(remaining, oldState, newState);
	}

	private void intersectionAlgorithm(MockState oldState, MockState newState) {
		Set<Integer> expanded = IntegerRange.expandRangeToSet(newState.range);
		IntegerRange intersection = io.github.palexdev.virtualizedfx.utils.Utils.intersection(oldState.range, newState.range);
		if (!io.github.palexdev.virtualizedfx.utils.Utils.INVALID_RANGE.equals(intersection)) {
			for (Integer common : intersection) {
				newState.addCell(common, oldState.removeCell(common));
				expanded.remove(common);
			}
		}

		remainingAlgorithm(expanded, oldState, newState);
	}

	private void remainingAlgorithm(Set<Integer> remaining, MockState oldState, MockState newState) {
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
