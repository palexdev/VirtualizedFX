/*
 * Copyright (C) 2022 Parisi Alessandro
 * This file is part of VirtualizedFX (https://github.com/palexdev/VirtualizedFX).
 *
 * VirtualizedFX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * VirtualizedFX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with VirtualizedFX.  If not, see <http://www.gnu.org/licenses/>.
 */

package unit;

import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class CellsManagerTest {
	private List<String> strings;

	@BeforeEach
	public void setup() {
		strings = IntStream.rangeClosed(0, 100)
				.mapToObj(i -> "String " + i)
				.collect(Collectors.toList());
	}

	@Test
	public void forwardTest1() {
		State initState = getInitState(IntegerRange.of(0, 15), 16);
		List<Cell> toCheck = IntStream.range(0, 5)
				.mapToObj(initState.cells::get)
				.collect(Collectors.toList());

		IntegerRange newRange = IntegerRange.of(5, 20);
		State newState = initState.transition(newRange);

		int checkStart = 16;
		for (Cell cell : toCheck) {
			assertEquals(checkStart, cell.index);
			checkStart++;
		}
	}

	@Test
	public void backwardTest1() {
		State initState = getInitState(IntegerRange.of(2, 13));
		List<Cell> toCheck = IntStream.rangeClosed(12, 13)
				.mapToObj(initState.cells::get)
				.collect(Collectors.toList());

		IntegerRange newRange = IntegerRange.of(0, 11);
		State newState = initState.transition(newRange);

		int checkStart = 0;
		for (Cell cell : toCheck) {
			assertEquals(checkStart, cell.index);
			checkStart++;
		}
	}

	@Test
	public void testJump1() {
		State initState = getInitState(IntegerRange.of(5, 20));
		List<Cell> toCheck = IntStream.rangeClosed(5, 20)
				.mapToObj(initState.cells::get)
				.collect(Collectors.toList());

		IntegerRange newRange = IntegerRange.of(30, 45);
		State newState = initState.transition(newRange);

		int checkStart = 30;
		for (Cell cell : toCheck) {
			assertEquals(checkStart, cell.index);
			checkStart++;
		}
	}

	@Test
	public void testCellsExceed1() {
		State initState = getInitState(IntegerRange.of(0, 10));

		IntegerRange newRange = IntegerRange.of(2, 10);
		State newState = initState.transition(newRange);
		checkState(newState, 2, IntegerRange.of(2, 10));
		assertNotNull(newState.cells.get(11));
		assertNotNull(newState.cells.get(12));
	}

	@Test
	public void testCellsExceed2() {
		State initState = getInitState(IntegerRange.of(0, 10));

		IntegerRange newRange = IntegerRange.of(2, 10);
		State newState = initState.transition(newRange);
		checkState(newState, 2, IntegerRange.of(2, 10));
		assertNotNull(newState.cells.get(11));
		assertNotNull(newState.cells.get(12));

		IntegerRange newRange2 = IntegerRange.of(0, 10);
		State newState2 = newState.transition(newRange2);
		checkState(newState2, 0, IntegerRange.of(0, 10));
	}

	@Test
	public void testAdd1() {
		State initState = getInitState(IntegerRange.of(0, 15));

		strings.add(0, "String Add 0");
		Change change = new Change(Change.ChangeType.ADD, IntegerRange.of(0));
		change.indexes.add(0);

		State newState = initState.transition(change);
		checkState(newState, 0, initState.range);
	}

	@Test
	public void testAdd2() {
		State initState = getInitState(IntegerRange.of(0, 15));

		strings.addAll(2, List.of("Add 2.A", "Add 2.B", "Add 2.C"));
		Change change = new Change(Change.ChangeType.ADD, IntegerRange.of(2, 4));
		change.indexes.addAll(List.of(2, 3, 4));

		State newState = initState.transition(change);
		checkState(newState, 0, initState.range);
	}

	@Test
	public void testAdd3() {
		State initState = getInitState(IntegerRange.of(0, 10));

		IntegerRange newRange = IntegerRange.of(1, 10);
		State rangeTransition = initState.transition(newRange);
		checkState(rangeTransition, 1, IntegerRange.of(1, 10));
		assertNotNull(rangeTransition.cells.get(11));

		strings.addAll(List.of("Add 2.A", "Add 2.B", "Add 2.C"));
		Change change = new Change(Change.ChangeType.ADD, IntegerRange.of(11, 13));
		change.indexes.addAll(List.of(11, 12, 13));
		State changeTransition = rangeTransition.transition(change);
		assertEquals(rangeTransition, changeTransition);

		IntegerRange newRange2 = IntegerRange.of(1, 11);
		State rangeTransition2 = changeTransition.transition(newRange2);
		assertEquals(11, rangeTransition2.cells.size());
		checkState(rangeTransition2, 1, newRange2);
	}

	@Test
	public void testAdd4() {
		State initState = getInitState(IntegerRange.of(0, 10));

		IntegerRange newRange = IntegerRange.of(1, 10);
		State rangeTransition = initState.transition(newRange);
		checkState(rangeTransition, 1, IntegerRange.of(1, 10));
		assertNotNull(rangeTransition.cells.get(11));

		strings.addAll(2, List.of("Add 2.A", "Add 2.B", "Add 2.C"));
		Change change = new Change(Change.ChangeType.ADD, IntegerRange.of(2, 4));
		change.indexes.addAll(List.of(2, 3, 4));
		State changeTransition = rangeTransition.transition(change);
		assertNotEquals(rangeTransition.cells, changeTransition.cells);

		IntegerRange newRange2 = IntegerRange.of(1, 11);
		State rangeTransition2 = changeTransition.transition(newRange2);
		checkState(rangeTransition2, 1, newRange2);
	}

	@Test
	public void testAdd5() {
		strings = IntStream.rangeClosed(0, 5)
				.mapToObj(i -> "String " + i)
				.collect(Collectors.toList());
		State initState = getInitState(IntegerRange.of(0, 5), 11);

		strings.addAll(List.of("String 6", "String 7", "String 8", "String 9"));
		Change change = new Change(Change.ChangeType.ADD, IntegerRange.of(6, 9));
		change.indexes.addAll(List.of(6, 7, 8, 9));

		State newState = initState.transition(change);
		assertFalse(newState.isViewportFull());
		checkState(newState, 0, IntegerRange.of(0, 9));
	}

	@Test
	public void testAdd6() {
		State initState = getInitState(IntegerRange.of(0, 5), 11);

		Change change = new Change(Change.ChangeType.ADD, IntegerRange.of(6, 10));
		change.indexes.addAll(List.of(6, 7, 8, 9, 10));

		State newState = initState.transition(change);
		assertTrue(newState.isViewportFull());
		checkState(newState, 0, IntegerRange.of(0, 10));
	}

	@Test
	public void testAdd7() {
		strings = IntStream.rangeClosed(0, 5)
				.mapToObj(i -> "String " + i)
				.collect(Collectors.toList());
		State initState = getInitState(IntegerRange.of(0, 5), 11);

		strings.addAll(2, List.of("Add 2.A", "Add 2.B", "Add 2.C", "Add 2.D"));
		Change change = new Change(Change.ChangeType.ADD, IntegerRange.of(2, 5));
		change.indexes.addAll(List.of(2, 3, 4, 5));

		State newState = initState.transition(change);
		assertFalse(newState.isViewportFull());
		checkState(newState, 0, IntegerRange.of(0, 9));
	}

	@Test
	public void testAdd8() {
		State initState = getInitState(IntegerRange.of(0, 10), 11);

		IntegerRange newRange = IntegerRange.of(1, 10);
		State newState = initState.transition(newRange);
		assertEquals(1, newState.extra.size());

		strings.addAll(2, List.of("Add 2.A", "Add 2.B", "Add 2.C", "Add 2.D"));
		Change change = new Change(Change.ChangeType.ADD, IntegerRange.of(2, 5));
		change.indexes.addAll(List.of(2, 3, 4, 5));

		State newState2 = newState.transition(change);
		checkState(newState2, 1, IntegerRange.of(1, 11));
	}

	@Test
	public void testAdd9() {
		State initState = getInitState(IntegerRange.of(18, 31));

		strings.add(0, "New");
		Change change = new Change(Change.ChangeType.ADD, IntegerRange.of(0));
		change.indexes.add(0);

		State newState = initState.transition(change);
		checkState(newState, 18, IntegerRange.of(18, 31));
	}

	@Test
	public void testAdd10() {
		State initState = getInitState(IntegerRange.of(18, 31));

		List<String> newStrings = IntStream.rangeClosed(0, 14)
				.mapToObj(i -> "New " + i)
				.collect(Collectors.toList());
		strings.addAll(18, newStrings);
		Change change = new Change(Change.ChangeType.ADD, IntegerRange.of(18, 31));
		change.indexes.addAll(IntegerRange.expandRange(change.range));

		State newState = initState.transition(change);
		checkState(newState, 18, IntegerRange.of(18, 31));
	}

	@Test
	public void testRemove1() {
		State initState = getInitState(IntegerRange.of(0, 10));

		strings.remove(0);
		Change change = new Change(Change.ChangeType.REMOVE, IntegerRange.of(0));
		change.indexes.add(0);

		State newState = initState.transition(change);
		checkState(newState, 0, initState.range);
	}

	@Test
	public void testRemove2() {
		State initState = getInitState(IntegerRange.of(0, 10));

		removeAll(1, 3, 4, 6);
		Change change = new Change(Change.ChangeType.REMOVE, IntegerRange.of(1, 6));
		change.indexes.addAll(List.of(1, 3, 4, 6));

		State newState = initState.transition(change);
		checkState(newState, 0, initState.range);
	}

	@Test
	public void testRemove3() {
		State initState = getInitState(IntegerRange.of(90, 100));

		removeAll(98, 100);
		Change change = new Change(Change.ChangeType.REMOVE, IntegerRange.of(98, 100));
		change.indexes.addAll(List.of(98, 100));

		State newState = initState.transition(change);
		checkState(newState, 88, IntegerRange.of(88, 98));
	}

	@Test
	public void testRemove4() {
		State initState = getInitState(IntegerRange.of(90, 100));

		removeAll(90, 91);
		Change change = new Change(Change.ChangeType.REMOVE, IntegerRange.of(90, 91));
		change.indexes.addAll(List.of(90, 91));

		State newState = initState.transition(change);
		checkState(newState, 88, IntegerRange.of(88, 98));
	}

	@Test
	public void testRemove5() {
		strings = IntStream.rangeClosed(0, 10)
				.mapToObj(i -> "String " + i)
				.collect(Collectors.toList());
		State initState = getInitState(IntegerRange.of(1, 10));

		removeAll(1, 2);
		Change change = new Change(Change.ChangeType.REMOVE, IntegerRange.of(1, 2));
		change.indexes.addAll(List.of(1, 2));

		State newState = initState.transition(change);
		checkState(newState, 0, IntegerRange.of(0, 8));
	}

	@Test
	public void testRemove6() {
		strings = IntStream.rangeClosed(0, 5)
				.mapToObj(i -> "String " + i)
				.collect(Collectors.toList());
		State initState = getInitState(IntegerRange.of(0, 5));

		removeAll(0, 3);
		Change change = new Change(Change.ChangeType.REMOVE, IntegerRange.of(0, 3));
		change.indexes.addAll(List.of(0, 3));

		State newState = initState.transition(change);
		checkState(newState, 0, IntegerRange.of(0, 3));
	}

	@Test
	public void testRemove7() {
		strings = IntStream.rangeClosed(0, 5)
				.mapToObj(i -> "String " + i)
				.collect(Collectors.toList());
		State initState = getInitState(IntegerRange.of(0, 5));

		removeAll(4, 5);
		Change change = new Change(Change.ChangeType.REMOVE, IntegerRange.of(4, 5));
		change.indexes.addAll(List.of(4, 5));

		State newState = initState.transition(change);
		checkState(newState, 0, IntegerRange.of(0, 3));
	}

	@Test
	public void testRemove8() {
		strings = IntStream.rangeClosed(0, 12)
				.mapToObj(i -> "String " + i)
				.collect(Collectors.toList());
		State initState = getInitState(IntegerRange.of(8, 12));

		removeAll(5, 7, 8, 10);
		Change change = new Change(Change.ChangeType.REMOVE, IntegerRange.of(5, 10));
		change.indexes.addAll(List.of(5, 7, 8, 10));

		State newState = initState.transition(change);
		checkState(newState, 4, IntegerRange.of(4, 8));
	}

	@Test
	public void testRemove9() {
		strings = IntStream.rangeClosed(0, 12)
				.mapToObj(i -> "String " + i)
				.collect(Collectors.toList());
		State initState = getInitState(IntegerRange.of(8, 12));

		removeAll(0, 3, 4, 5, 7, 8, 9, 10, 11);
		Change change = new Change(Change.ChangeType.REMOVE, IntegerRange.of(0, 11));
		change.indexes.addAll(List.of(0, 3, 4, 5, 7, 8, 9, 10, 11));

		State newState = initState.transition(change);
		checkState(newState, 0, IntegerRange.of(0, 3));
	}

	private void checkState(State state, int start, IntegerRange expectedRange) {
		assertEquals(expectedRange, state.range);

		int i = start;
		for (Map.Entry<Integer, Cell> entry : state.cells.entrySet()) {
			Integer index = entry.getKey();
			String item = strings.get(i);
			Cell cell = entry.getValue();
			assertEquals(i, index);
			assertEquals(item, cell.item);
			i++;
		}
	}

	private State getInitState(IntegerRange range) {
		State state = new State(range);
		for (int i = range.getMin(); i <= range.getMax(); i++) {
			String item = strings.get(i);
			Cell cell = new Cell(item);
			cell.updateIndex(i);
			state.addCell(i, cell);
		}
		return state;
	}

	private State getInitState(IntegerRange range, int targetSize) {
		State state = new State(range, targetSize);
		for (int i = range.getMin(); i <= range.getMax(); i++) {
			String item = strings.get(i);
			Cell cell = new Cell(item);
			cell.updateIndex(i);
			state.addCell(i, cell);
		}
		return state;
	}

	private void removeAll(int... indexes) {
		List<String> items = Arrays.stream(indexes)
				.mapToObj(i -> strings.get(i))
				.collect(Collectors.toList());
		strings.removeAll(items);
	}

	private class State {
		private final IntegerRange range;
		private final Map<Integer, Cell> cells = new TreeMap<>();
		private final List<Integer> extra = new ArrayList<>();
		private final int targetSize;

		String caller;

		public State(IntegerRange range) {
			this.range = range;
			this.targetSize = range.getMax() - range.getMin() + 1;
		}

		private State(IntegerRange range, int targetSize) {
			this.range = range;
			this.targetSize = targetSize;
		}

		public void addCell(int index, Cell cell) {
			cells.put(index, cell);
		}

		public void addCells(Map<Integer, Cell> cells) {
			this.cells.putAll(cells);
		}

		public State transition(IntegerRange newRange) {
			if (range.equals(newRange)) return this;

			State newState = new State(newRange, targetSize);
			List<Integer> toUpdate = new ArrayList<>();
			for (int i = newRange.getMin(); i <= newRange.getMax(); i++) {
				Cell commonCell = cells.remove(i);
				if (commonCell != null) {
					newState.addCell(i, commonCell);
					continue;
				}
				toUpdate.add(i);
			}

			if (!toUpdate.isEmpty()) {
				int index = 0;
				Iterator<Map.Entry<Integer, Cell>> it = cells.entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry<Integer, Cell> next = it.next();
					int cIndex = toUpdate.get(index);
					String item = strings.get(cIndex);
					next.getValue().updateIndex(cIndex);
					next.getValue().updateItem(item);
					newState.addCell(cIndex, next.getValue());
					index++;
					it.remove();
				}
			}


			int head = newRange.getMin();
			int tail = newRange.getMax();
			int offset = 1;
			int newIndex = getNewIndex(head, tail, offset);
			List<Cell> remaining = new ArrayList<>(cells.values());
			while (!remaining.isEmpty() && newIndex != -1) {
				Cell cell = remaining.remove(0);
				String item = strings.get(newIndex);
				cell.updateIndex(newIndex);
				cell.updateItem(item);
				newState.addCell(newIndex, cell);
				newState.extra.add(newIndex);
				offset++;
				newIndex = getNewIndex(head, tail, offset);
			}
			return newState;
		}

		public State transition(Change change) {
			switch (change.type) {
				case PERMUTATION: {
					cells.forEach((index, cell) -> {
						String item = strings.get(index);
						cell.updateItem(item);
					});
					break;
				}
				case REPLACE: {
					for (Integer index : change.indexes) {
						if (IntegerRange.inRangeOf(index, range)) {
							String item = strings.get(index);
							cells.get(index).updateItem(item);
						}
					}
					break;
				}
				case ADD: {
					if (isViewportFull() && change.range.getMin() > range.getMax()) return this;

					int min = range.getMin();
					int max = Math.min(min + targetSize - 1, strings.size() - 1);
					IntegerRange newRange = IntegerRange.of(min, max);
					State newState = new State(newRange, targetSize);

					int tail = max;
					for (int i = min; i <= max; i++) {
						if (i < change.getFrom()) {
							newState.addCell(i, cells.remove(i));
							continue;
						}

						if (i > change.getTo()) {
							final int index = i;
							Optional.ofNullable(cells.remove(index - change.size()))
									.ifPresentOrElse(
											cell -> {
												cell.updateIndex(index);
												newState.addCell(index, cell);
											},
											() -> {
												String item = strings.get(index);
												Cell cell = cells.remove(index);
												cell.updateItem(item);
												newState.addCell(index, cell);
											}
									);
							continue;
						}

						Cell cell = getOrCreate(tail, i);
						newState.addCell(i, cell);
						tail--;
					}

					//updateExtra(newState);
					return newState;
				}
				case REMOVE: {
					int max = Math.min(range.getMin() + targetSize - 1, strings.size() - 1);
					int min = Math.max(0, max - targetSize + 1);
					IntegerRange newRange = IntegerRange.of(min, max);
					State newState = new State(newRange, targetSize);

					Set<Integer> toUpdate = IntegerRange.expandRangeToSet(range);
					toUpdate.removeAll(change.indexes);

					int[] changeIndexes = change.indexes.stream().mapToInt(Integer::intValue).toArray();
					for (Integer index : toUpdate) {
						int newIndex = index - findShift(changeIndexes, index);
						Cell cell = cells.remove(index);
						cell.updateIndex(newIndex);
						newState.addCell(newIndex, cell);
					}

					Set<Integer> toCreate = IntegerRange.expandRangeToSet(newRange);
					toCreate.removeAll(newState.cells.keySet());

					List<Integer> available = new ArrayList<>(cells.keySet());
					for (Integer index : toCreate) {
						String item = strings.get(index);
						int cellIndex = available.remove(0);
						Cell cell = cells.remove(cellIndex);
						cell.updateIndex(index);
						cell.updateItem(item);
						newState.addCell(index, cell);
					}

					Iterator<Map.Entry<Integer, Cell>> it = cells.entrySet().iterator();
					while (it.hasNext()) {
						Map.Entry<Integer, Cell> next = it.next();
						Cell cell = next.getValue();
						//cell.dispose();
						it.remove();
					}

					assertTrue(cells.isEmpty());
					return newState;
				}
			}
			return this;
		}

		private Cell getOrCreate(int getIndex, int newIndex) {
			String item = strings.get(newIndex);
			if (cells.containsKey(getIndex)) {
				Cell cell = cells.remove(getIndex);
				cell.updateIndex(newIndex);
				cell.updateItem(item);
				return cell;
			}

			Cell cell = new Cell(item);
			cell.updateIndex(newIndex);
			return cell;
		}

		private int getNewIndex(int head, int tail, int offset) {
			int nTail = tail + offset;
			if (nTail <= strings.size() - 1) return nTail;

			int nHead = head - offset;
			if (nHead >= 0) return nHead;

			return -1;
		}

		private void updateExtra(State newState) {
			if (newState.isViewportFull()) return;

			int head = newState.range.getMin();
			int tail = newState.range.getMax();
			int offset = 1;
			int newIndex = getNewIndex(head, tail, offset);

			List<Integer> remaining = new ArrayList<>(extra);
			while (!remaining.isEmpty() && newIndex != -1) {
				int cIndex = remaining.remove(0);
				Cell cell = cells.remove(cIndex);
				String item = strings.get(newIndex);
				cell.updateIndex(newIndex);
				cell.updateItem(item);
				newState.addCell(newIndex, cell);
				newState.extra.add(newIndex);
				offset++;
				newIndex = getNewIndex(head, tail, offset);
			}

			/*
			for (Integer rem : remaining) {
				Cell toDispose = cells.remove(rem);
				toDispose.dispose();
			}
			*/
		}

		private int getFirstValid(Change change, IntegerRange range, Set<Integer> used) {
			for (Integer index : change.indexes) {
				if (!used.contains(index) && IntegerRange.inRangeOf(index, range)) return index;
			}
			return -1;
		}

		private int findShift(int[] indexes, int index) {
			int shift = Arrays.binarySearch(indexes, index);
			return shift > -1 ? shift : -(shift + 1);
		}

		private boolean isViewportFull() {
			return targetSize == cells.size();
		}
	}

	private static class Cell {
		private String item;
		private int index;

		public Cell(String item) {
			updateItem(item);
		}

		public void updateIndex(int index) {
			this.index = index;
		}

		public void updateItem(String item) {
			this.item = item;
		}

		@Override
		public String toString() {
			return "Cell: [" + item + " - " + index + "]";
		}
	}

	private static class Change {
		public enum ChangeType {
			PERMUTATION, REPLACE, ADD, REMOVE
		}

		private final ChangeType type;
		private final IntegerRange range;
		private final Set<Integer> indexes = new HashSet<>();

		public Change(ChangeType type, IntegerRange range) {
			this.type = type;
			this.range = range;
		}

		public boolean hasChanged(int index) {
			return indexes.contains(index);
		}

		public int getFrom() {
			return range.getMin();
		}

		public int getTo() {
			return range.getMax();
		}

		public int size() {
			return indexes.size();
		}
	}
}
