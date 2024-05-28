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

package interactive.list;

import cells.TestCell;
import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.controls.SkinBase;
import io.github.palexdev.virtualizedfx.cells.VFXCellBase;
import io.github.palexdev.virtualizedfx.cells.base.VFXCell;
import io.github.palexdev.virtualizedfx.list.VFXList;
import io.github.palexdev.virtualizedfx.list.VFXListHelper;
import io.github.palexdev.virtualizedfx.list.VFXListSkin;
import io.github.palexdev.virtualizedfx.list.VFXListState;
import io.github.palexdev.virtualizedfx.list.paginated.VFXPaginatedList;
import io.github.palexdev.virtualizedfx.list.paginated.VFXPaginatedListSkin;
import io.github.palexdev.virtualizedfx.utils.Utils;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import org.opentest4j.AssertionFailedError;

import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static utils.TestFXUtils.counter;

public class ListTestUtils {
	//================================================================================
	// Constructors
	//================================================================================
	private ListTestUtils() {}

	//================================================================================
	// Methods
	//================================================================================
	static void assertState(VFXList<Integer, VFXCell<Integer>> list, IntegerRange range) {
		VFXListState<Integer, VFXCell<Integer>> state = list.getState();
		VFXListHelper<Integer, VFXCell<Integer>> helper = list.getHelper();
		if (Utils.INVALID_RANGE.equals(range)) {
			assertEquals(VFXListState.INVALID, state);
			return;
		}

		assertEquals(helper.range(), range);
		assertEquals(helper.totalNum(), state.size());

		Map<Integer, VFXCell<Integer>> cells = state.getCellsByIndexUnmodifiable();
		ObservableList<Integer> items = list.getItems();
		for (Integer index : range) {
			VFXCell<Integer> cell = cells.get(index);
			try {
				assertNotNull(cell);
			} catch (AssertionFailedError error) {
				// For debug purposes
				System.err.println("Null cell for index: " + index);
				throw error;
			}
			if (cell instanceof VFXCellBase<Integer> cb) {
				assertEquals(index, cb.getIndex());
				assertEquals(items.get(index), cb.getItem());
			}
			assertPosition(list, index - range.getMin(), cell);
		}
	}

	static void assertPosition(VFXList<Integer, VFXCell<Integer>> list, int iteration, VFXCell<Integer> cell) {
		Orientation o = list.getOrientation();
		Function<Bounds, Double> inParentPos = (o == Orientation.VERTICAL) ? Bounds::getMinY : Bounds::getMinX;
		double pos = iteration * list.getHelper().getTotalCellSize();
		assertEquals(pos, inParentPos.apply(cell.toNode().getBoundsInParent()));
	}

	//================================================================================
	// Internal Classes
	//================================================================================
	public static class List extends VFXList<Integer, VFXCell<Integer>> {
		public List(ObservableList<Integer> items) {
			this(items, TestCell::new);
		}

		public List(ObservableList<Integer> items, Function<Integer, VFXCell<Integer>> cellFactory) {
			super(items, cellFactory);
		}

		@Override
		public void setCellFactory(Function<Integer, VFXCell<Integer>> cellFactory) {
			super.setCellFactory(cellFactory.andThen(c -> {
				counter.created();
				return c;
			}));
		}

		@Override
		protected SkinBase<?, ?> buildSkin() {
			return new VFXListSkin<>(this) {
				@Override
				protected void onLayoutCompleted(boolean done) {
					super.onLayoutCompleted(done);
					if (done) counter.layout();
				}
			};
		}
	}

	public static class PList extends VFXPaginatedList<Integer, VFXCell<Integer>> {
		public PList(ObservableList<Integer> items) {
			this(items, TestCell::new);
		}

		public PList(ObservableList<Integer> items, Function<Integer, VFXCell<Integer>> cellFactory) {
			super(items, cellFactory);
		}

		@Override
		public void setCellFactory(Function<Integer, VFXCell<Integer>> cellFactory) {
			super.setCellFactory(cellFactory.andThen(c -> {
				counter.created();
				return c;
			}));
		}

		@Override
		protected SkinBase<?, ?> buildSkin() {
			return new VFXPaginatedListSkin<>(this) {
				@Override
				protected void onLayoutCompleted(boolean done) {
					super.onLayoutCompleted(done);
					if (done) counter.layout();
				}
			};
		}
	}

}

