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

import io.github.palexdev.virtualizedfx.cell.Cell;

/**
 * Utility API used by the {@link FlowState} to map indexes from an old state to a new one.
 * <p>
 * There are three concrete implementations which extend the common abstract class {@code AbstractMapping}:
 * <p> {@link ValidMapping}: a valid mapping means that a cell can be removed from the old state and copied
 * as is to the new one
 * <p> {@link PartialMapping}: a partial mapping means that a cell should be removed from the old state
 * and copied to the new state after updating its index
 * <p> {@link FullMapping}: a full mapping means that a cell should be removed from the old state and
 * copied to the new state only after both its index and item have been updated.
 * Often a {@code FullMapping} can also receive a negative index as the "oldIndex" parameter of
 * {@link #manage(FlowState, FlowState, int)}, this means that no cells are left in the old state and a new one
 * must be created to reach {@link FlowState#getTargetSize()}
 */
public interface FlowMapping<T, C extends Cell<T>> {
	void manage(FlowState<T, C> oldState, FlowState<T, C> newState, int oldIndex);

	int getNewIndex();

	abstract class AbstractMapping<T, C extends Cell<T>> implements FlowMapping<T, C> {
		protected final int newIndex;

		public AbstractMapping(int newIndex) {
			this.newIndex = newIndex;
		}

		@Override
		public int getNewIndex() {
			return newIndex;
		}
	}

	class ValidMapping<T, C extends Cell<T>> extends AbstractMapping<T, C> {

		public ValidMapping(int newIndex) {
			super(newIndex);
		}

		@Override
		public void manage(FlowState<T, C> oldState, FlowState<T, C> newState, int oldIndex) {
			newState.addCell(newIndex, oldState.getCells().remove(newIndex));
		}
	}

	class PartialMapping<T, C extends Cell<T>> extends AbstractMapping<T, C> {

		public PartialMapping(int newIndex) {
			super(newIndex);
		}

		@Override
		public void manage(FlowState<T, C> oldState, FlowState<T, C> newState, int oldIndex) {
			C cell = oldState.getCells().remove(oldIndex);
			cell.updateIndex(newIndex);
			newState.addCell(newIndex, cell);
		}
	}

	class FullMapping<T, C extends Cell<T>> extends AbstractMapping<T, C> {

		public FullMapping(int newIndex) {
			super(newIndex);
		}

		@Override
		public void manage(FlowState<T, C> oldState, FlowState<T, C> newState, int oldIndex) {
			VirtualFlow<T, C> virtualFlow = oldState.getVirtualFlow();
			T item = virtualFlow.getItems().get(newIndex);
			C cell = oldState.getCells().remove(oldIndex);
			if (cell != null) {
				cell.updateItem(item);
			} else {
				cell = virtualFlow.getCellFactory().apply(item);
			}
			cell.updateIndex(newIndex);
			newState.addCell(newIndex, cell);
		}
	}
}
