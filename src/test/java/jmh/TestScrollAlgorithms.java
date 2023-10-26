package jmh;

import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.virtualizedfx.cells.Cell;
import javafx.scene.Node;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import utils.Utils;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static io.github.palexdev.virtualizedfx.utils.Utils.INVALID_RANGE;
import static io.github.palexdev.virtualizedfx.utils.Utils.intersection;

public class TestScrollAlgorithms {
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
		MockState newState = new MockState();
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
		MockState newState = new MockState();
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

	static class MockState {
		private final SequencedMap<Integer, MockCell> cells = new TreeMap<>();

		public static MockState generate(IntegerRange range) {
			MockState s = new MockState();
			range.forEach(i -> s.addCell(i, new MockCell(i)));
			return s;
		}

		public void addCell(Integer index, MockCell cell) {
			cells.put(index, cell);
		}

		public MockCell removeCell(Integer index) {
			return cells.remove(index);
		}
	}

	static class MockCell implements Cell<Integer> {
		private Integer index = -1;
		private Integer item;
		private String text = "";

		public MockCell(Integer item) {
			updateItem(item);
		}

		void render() {
			text = "Index/Item: %s/%s".formatted(index, item);
		}

		@Override
		public Node toNode() {
			return null;
		}

		@Override
		public void updateIndex(int index) {
			this.index = index;
			render();
		}

		@Override
		public void updateItem(Integer item) {
			this.item = item;
			render();
		}
	}
}
