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
import io.github.palexdev.virtualizedfx.cells.base.VFXCell;
import javafx.scene.Node;

import java.util.SequencedMap;
import java.util.TreeMap;

public class JMHCommons {

	//================================================================================
	// Internal Classes
	//================================================================================
	public static class MockState {
		final IntegerRange range;
		final SequencedMap<Integer, MockCell> cells = new TreeMap<>();

		public MockState(IntegerRange range) {this.range = range;}

		public static MockState generate(IntegerRange range) {
			MockState s = new MockState(range);
			range.forEach(i -> s.addCell(i, new MockCell(i)));
			return s;
		}

		public void addCell(Integer index, MockCell cell) {
			cells.put(index, cell);
		}

		public MockCell removeCell(Integer index) {
			return cells.remove(index);
		}

		public boolean isEmpty() {
			return cells.isEmpty();
		}
	}

	public static class MockState2D {
		final int nColumns;
		final IntegerRange rRange;
		final IntegerRange cRange;
		final SequencedMap<Integer, MockCell> cells = new TreeMap<>();

		public MockState2D(int nColumns, IntegerRange rRange, IntegerRange cRange) {
			this.nColumns = nColumns;
			this.rRange = rRange;
			this.cRange = cRange;
		}

		public static MockState2D generate2D(int nColumns, IntegerRange rRange, IntegerRange cRange) {
			MockState2D s = new MockState2D(nColumns, rRange, cRange);
			for (Integer rIdx : rRange) {
				for (Integer cIdx : cRange) {
					int linear = GridUtils.subToInd(nColumns, rIdx, cIdx);
					s.addCell(linear, new MockCell(linear));
				}
			}
			return s;
		}

		public void addCell(Integer index, MockCell cell) {
			cells.put(index, cell);
		}

		public MockCell removeCell(Integer index) {
			return cells.remove(index);
		}

		public boolean isEmpty() {
			return cells.isEmpty();
		}
	}

	public static class MockCell implements VFXCell<Integer> {
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
