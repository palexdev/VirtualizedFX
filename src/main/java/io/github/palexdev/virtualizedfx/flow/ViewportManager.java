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

package io.github.palexdev.virtualizedfx.flow;

import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.base.beans.range.NumberRange;
import io.github.palexdev.mfxcore.base.properties.range.IntegerRangeProperty;
import io.github.palexdev.mfxcore.utils.fx.ListChangeHelper;
import io.github.palexdev.virtualizedfx.beans.FlowStateProperty;
import io.github.palexdev.virtualizedfx.cell.Cell;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.ListChangeListener;

import java.util.List;
import java.util.Map;

/**
 * The {@code ViewportManager} is responsible for managing the flow's viewport, the cells.
 * <p>
 * It stores the current state of the viewport at any time with two properties:
 * <p> - The state property which holds an object of type {@link FlowState}
 * <p> - The last range property which holds the last range of displayed items, as a {@link IntegerRange}
 * <p></p>
 * This is responsible for initializing, update on scroll, update on any change on the items list,
 * clearing or resetting the viewport when needed.
 *
 * @param <T> the type of items
 * @param <C> the type of cell used
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class ViewportManager<T, C extends Cell<T>> {
	//================================================================================
	// Properties
	//================================================================================
	private final VirtualFlow<T, C> virtualFlow;
	private final FlowStateProperty<T, C> state = new FlowStateProperty<>(FlowState.EMPTY);
	private final IntegerRangeProperty lastRange = new IntegerRangeProperty();

	//================================================================================
	// Constructors
	//================================================================================
	ViewportManager(VirtualFlow<T, C> virtualFlow) {
		this.virtualFlow = virtualFlow;
	}

	//================================================================================
	// Methods
	//================================================================================

	/**
	 * This method is responsible for filling the viewport with the necessary number of cells.
	 * So, it is a bit more than just an initializing method, since this is also called for example
	 * when the viewport size changes and cells need to be added or removed.
	 * <p></p>
	 * For this reason this method works in a general way:
	 * <p>
	 * First we compute the range of items we want to display by using the {@link OrientationHelper}.
	 * Specifically we get:
	 * <p> the number of needed cells with {@link OrientationHelper#maxCells()}
	 * <p> the end index with {@code Math.max(start + num - 1, itemsNum - 1)}
	 * <p> the start index with {@link OrientationHelper#firstVisible()}.
	 * It may happen that the difference between the end and start indexes doesn't equal to the number of
	 * needed cells, for this reason we prevent any error from occurring by "correcting" the start index as follows:
	 * {@code Math.max(end - num + 1, 0)}
	 * <p></p>
	 * The next step is to instantiate a new {@link FlowState} which will be the new state of the viewport,
	 * and the current "old" state.
	 * <p>
	 * If the old state is {@link FlowState#EMPTY} then from start to end index
	 * new cells are created and added to the new state. Then both the state property and
	 * last range property are updated and finally {@link VirtualFlow#requestViewportLayout()} is called.
	 * <p>
	 * In any other case it means we are in a situation where cells need to either be supplied or removed.
	 * <p>
	 * From start to end index we check if the old state contains the i-th cell, if that's the case the cell
	 * is removed from the old state and added to the new one. Otherwise, a new cell is created and added to the
	 * new state.
	 * <p>
	 * So, how the supply/remove works:
	 * <p>
	 * The new state carries all the needed cells, a listener in the {@link VirtualFlowSkin} will then update
	 * the viewport children if needed.
	 * All the cells which remain in the old state are the one to be removed, but since only the new state determines
	 * the viewport's content, the only operation left is to dispose these unneeded ones with {@link Cell#dispose()}.
	 * <p>
	 * Finally, both the state property and last range property are updated
	 * and {@link VirtualFlow#requestViewportLayout()} is called.
	 */
	public void init() {
		if (virtualFlow.getCellFactory() == null) return;

		// Compute the range of items to display
		OrientationHelper helper = virtualFlow.getOrientationHelper();
		int num = helper.maxCells();
		int first = helper.firstVisible();
		int last = Math.min(first + num - 1, itemsNum() - 1);
		first = Math.max(last - num + 1, 0); // Fix the start index if last - first != num
		IntegerRange range = IntegerRange.of(first, last);

		// Compute the new state
		FlowState<T, C> newState = new FlowState<>(virtualFlow, range);
		FlowState<T, C> oldState = getState();
		if (oldState == FlowState.EMPTY) {
			for (int i = range.getMin(); i <= range.getMax(); i++) {
				T item = indexToItem(i);
				C cell = itemToCell(item);
				cell.updateIndex(i);
				newState.addCell(i, cell);
			}
			newState.setCellsChanged(true);
			setState(newState);
			setLastRange(range);
			virtualFlow.requestViewportLayout();
			return;
		}

		Map<Integer, C> cells = oldState.getCells();
		for (int i = range.getMin(); i <= range.getMax(); i++) {
			if (cells.containsKey(i)) {
				newState.addCell(i, cells.remove(i));
				continue;
			}

			T item = indexToItem(i);
			C cell = itemToCell(item);
			cell.updateIndex(i);
			newState.addCell(i, cell);
		}

		cells.values().forEach(C::dispose);
		newState.setCellsChanged(oldState.cellsNum() != newState.cellsNum());
		setState(newState);
		setLastRange(range);
		virtualFlow.requestViewportLayout();
	}

	/**
	 * This is responsible for updating the viewport state on scroll.
	 * <p></p>
	 * If the current state is {@link FlowState#EMPTY} or the items list is empty, exits immediately.
	 * <p>
	 * The first step is to compute the new range of items to display. Then the new state is updated only
	 * if the current range and the computed one are different, the new state is built with
	 * {@link FlowState#transition(IntegerRange)}.
	 * <p>
	 * The second step is to call {@link VirtualFlow#requestViewportLayout()}, but this happens only if the last range and the
	 * new range are not equal.
	 */
	public void onScroll() {
		FlowState<T, C> state = getState();
		if (state == FlowState.EMPTY || itemsEmpty()) return;

		OrientationHelper helper = virtualFlow.getOrientationHelper();
		int num = helper.maxCells();

		// State Computation
		int sFirst = helper.firstVisible();
		int sLast = helper.lastVisible();
		int sTrueFirst = Math.max(sLast - num + 1, 0); // Fix the start index if last - first != num
		IntegerRange sRange = IntegerRange.of(sTrueFirst, sLast);
		if (!sRange.equals(state.getRange())) {
			setState(state.transition(sRange));
		}

		// Layout Computation
		IntegerRange lRange = IntegerRange.of(sFirst, sLast); // The layout range and the state range not always are equal
		if (!lRange.equals(getLastRange())) {
			virtualFlow.requestViewportLayout();
		}
		setLastRange(lRange);
	}

	/**
	 * This is responsible for updating the viewport state whenever a change occurs in the items list.
	 * <p></p>
	 * There are three separate situations:
	 * <p> 1) The items list is empty, calls {@link #clear()} and exits
	 * <p> 2) The current state is {@link FlowState#EMPTY}, calls {@link #init()} and exits
	 * <p> 3) The given change is processed by using the {@link ListChangeHelper} utility class,
	 * then the new state is computed by using {@link FlowState#transition(List)}, finally
	 * {@link VirtualFlow#requestViewportLayout()} is called and the last range property is updated.
	 */
	public void onListChange(ListChangeListener.Change<? extends T> c) {
		if (itemsEmpty()) {
			clear();
			return;
		}

		if (getState() == FlowState.EMPTY) {
			init();
			return;
		}

		List<ListChangeHelper.Change> changes = ListChangeHelper.instance().processChange(c);
		setState(getState().transition(changes));
		virtualFlow.requestViewportLayout();
		setLastRange(getState().getRange());
	}

	/**
	 * Clears the viewport. Sets the state to {@link FlowState#EMPTY}.
	 */
	public void clear() {
		getState().disposeAndClear();
		setState(FlowState.EMPTY);
		OrientationHelper helper = virtualFlow.getOrientationHelper();
		helper.computeEstimatedLength();
		virtualFlow.requestViewportLayout(); // To invalidate the positions
	}

	/**
	 * Resets the viewport by first calling {@link #clear()}, then {@link #init()}.
	 */
	public void reset() {
		clear();
		init();
	}

	/**
	 * Converts the given index to an item from the list.
	 */
	protected T indexToItem(int index) {
		return virtualFlow.getItems().get(index);
	}

	/**
	 * Converts the given item to a cell using {@link VirtualFlow#cellFactoryProperty()}.
	 */
	protected C itemToCell(T item) {
		return virtualFlow.getCellFactory().apply(item);
	}

	/**
	 * @return the number of items in the list
	 */
	public final int itemsNum() {
		return virtualFlow.getItems().size();
	}

	/**
	 * @return whether the list is empty
	 */
	public final boolean itemsEmpty() {
		return virtualFlow.getItems().isEmpty();
	}

	/**
	 * @return the virtual flow instance associated to this manager
	 */
	protected VirtualFlow<T, C> getVirtualFlow() {
		return virtualFlow;
	}

	public FlowState<T, C> getState() {
		return state.get();
	}

	/**
	 * Specifies the current state of the viewport as a {@link FlowState} object.
	 */
	public ReadOnlyObjectProperty<FlowState<T, C>> stateProperty() {
		return state.getReadOnlyProperty();
	}

	protected void setState(FlowState<T, C> state) {
		this.state.set(state);
	}

	public NumberRange<Integer> getLastRange() {
		return lastRange.get();
	}

	/**
	 * Specifies the current range of displayed items as a {@link IntegerRange} object.
	 */
	public ReadOnlyObjectProperty<NumberRange<Integer>> lastRangeProperty() {
		return lastRange.getReadOnlyProperty();
	}

	protected void setLastRange(IntegerRange lastRange) {
		this.lastRange.set(lastRange);
	}
}
