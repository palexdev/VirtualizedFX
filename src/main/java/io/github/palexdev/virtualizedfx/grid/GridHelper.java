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

package io.github.palexdev.virtualizedfx.grid;

import io.github.palexdev.mfxcore.base.beans.Position;
import io.github.palexdev.mfxcore.base.beans.Size;
import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.collections.ObservableGrid;
import io.github.palexdev.mfxcore.utils.NumberUtils;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Orientation;
import javafx.scene.Node;

/**
 * The {@code GridHelper} is a utility interface which collects a series of common computations/operations used by
 * {@link VirtualGrid} and its subcomponents. Has one concrete implementation which is {@link DefaultGridHelper}
 */
public interface GridHelper {

	/**
	 * @return the first visible row index
	 */
	int firstRow();

	/**
	 * @return the first visible column index
	 */
	int firstColumn();

	/**
	 * @return the last visible row index
	 */
	int lastRow();

	/**
	 * @return the last visible column index
	 */
	int lastColumn();

	/**
	 * @return the range of visible rows
	 */
	IntegerRange rowsRange();

	/**
	 * @return the range of visible columns
	 */
	IntegerRange columnsRange();

	/**
	 * @return the number of rows the viewport can display at any time
	 */
	int maxRows();

	/**
	 * @return the number of columns the viewport can display at any time
	 */
	int maxColumns();

	/**
	 * @return the maximum amount of pixels the viewport can scroll vertically
	 */
	double maxVScroll();

	/**
	 * @return the maximum amount of pixels the viewport can scroll horizontally
	 */
	double maxHScroll();

	/**
	 * @return the total virtual size of the viewport
	 */
	Size computeEstimatedSize();

	/**
	 * Keeps the results of {@link #computeEstimatedSize()}.
	 */
	ReadOnlyObjectProperty<Size> estimatedSizeProperty();

	/**
	 * This binding holds the horizontal position of the viewport.
	 */
	DoubleBinding xPosBinding();

	/**
	 * This binding holds the vertical position of the viewport.
	 */
	DoubleBinding yPosBinding();

	/**
	 * Invalidates {@link VirtualGrid#positionProperty()} in case changes
	 * occur in the viewport and the current position is no longer valid.
	 *
	 * @return true or false if the old position was invalid or not
	 */
	boolean invalidatedPos();

	/**
	 * Scrolls the viewport to the given row index.
	 */
	void scrollToRow(int index);

	/**
	 * Scrolls the viewport to the given column index.
	 */
	void scrollToColumn(int index);

	/**
	 * Scrolls the viewport by the given amount of pixels.
	 * The direction is determined by the "orientation" parameter.
	 */
	void scrollBy(double pixels, Orientation orientation);

	/**
	 * Scrolls the viewport to the given pixel value.
	 * The direction is determined by the "orientation" parameter.
	 */
	void scrollTo(double pixel, Orientation orientation);

	/**
	 * Responsible for calling {@link Node#resizeRelocate(double, double, double, double)} with the needed parameters,
	 * this is used to position and resize cells.
	 */
	void layout(Node node, double x, double y);

	/**
	 * Disposes bindings/listeners that are not required anymore.
	 */
	void dispose();

	/**
	 * Abstract implementation of {@link GridHelper}, base class for {@link DefaultGridHelper}.
	 */
	abstract class AbstractHelper implements GridHelper {
		protected final VirtualGrid<?, ?> grid;
		protected final ViewportManager<?, ?> manager;

		protected final ObjectProperty<Size> estimatedSize = new SimpleObjectProperty<>(Size.of(0, 0));

		public AbstractHelper(VirtualGrid<?, ?> grid) {
			this.grid = grid;
			this.manager = grid.getViewportManager();
		}

		@Override
		public boolean invalidatedPos() {
			Size size = estimatedSize.get();
			double x = Math.min(size.getWidth(), grid.getHPos());
			double y = Math.min(size.getHeight(), grid.getVPos());
			Position pos = Position.of(x, y);
			boolean invalid = !grid.getPosition().equals(pos);
			grid.setPosition(Position.of(x, y));
			return invalid;
		}

		@Override
		public ReadOnlyObjectProperty<Size> estimatedSizeProperty() {
			return estimatedSize;
		}
	}

	/**
	 * Concrete implementation of {@link AbstractHelper} with default/expected behavior for a virtual grid.
	 * <p></p>
	 * This helper adds the following listeners:
	 * <p> - A listener to the virtual grid's width to re-initialize the viewport, {@link ViewportManager#init()}
	 * <p> - A listener to the virtual grid's height to re-initialize the viewport, {@link ViewportManager#init()}
	 * <p> - A listener on the virtual grid's position property to process the scroll
	 * <p></p>
	 * More info on the last listener:
	 * <p>
	 * The scroll computation is done only if no changes occurred in the grid's items data structure.
	 * <p> {@link ViewportManager#onHScroll()} is called only if the horizontal position has changed
	 * <p> {@link ViewportManager#onVScroll()} is called only if the vertical position has changed
	 * <p> If both positions have changed than both methods are called in the listed order.
	 */
	class DefaultGridHelper extends AbstractHelper {
		private ChangeListener<? super Number> widthListener;
		private ChangeListener<? super Number> heightListener;
		private ChangeListener<? super Position> positionListener;

		public DefaultGridHelper(VirtualGrid<?, ?> grid) {
			super(grid);

			widthListener = (observable, oldValue, newValue) -> {
				double val = newValue.doubleValue();
				if (val > 0 && grid.getHeight() > 0)
					manager.init();
			};
			heightListener = (observable, oldValue, newValue) -> {
				invalidatedPos();

				double val = newValue.doubleValue();
				if (val > 0 && grid.getWidth() > 0)
					manager.init();
			};
			positionListener = (observable, oldValue, newValue) -> {
				ObservableGrid.Change<?> change = grid.getItems().getChange();
				if (change != null && change != ObservableGrid.Change.EMPTY) {
					return;
				}

				if (oldValue.getX() != newValue.getX()) {
					manager.onHScroll();
				}
				if (oldValue.getY() != newValue.getY()) {
					manager.onVScroll();
				}
			};

			grid.widthProperty().addListener(widthListener);
			grid.heightProperty().addListener(heightListener);
			grid.positionProperty().addListener(positionListener);

			((ObjectProperty<Size>) grid.estimatedSizeProperty()).bind(estimatedSizeProperty());
		}

		@Override
		public int firstRow() {
			return NumberUtils.clamp(
					(int) Math.floor(grid.getVPos() / grid.getCellSize().getHeight()),
					0,
					grid.getRowsNum() - 1
			);
		}

		@Override
		public int firstColumn() {
			return NumberUtils.clamp(
					(int) Math.floor(grid.getHPos() / grid.getCellSize().getWidth()),
					0,
					grid.getColumnsNum() - 1
			);
		}

		@Override
		public int lastRow() {
			return NumberUtils.clamp(
					firstRow() + maxRows() - 1,
					0,
					grid.getRowsNum() - 1
			);
		}

		@Override
		public int lastColumn() {
			return NumberUtils.clamp(
					firstColumn() + maxColumns() - 1,
					0,
					grid.getColumnsNum() - 1
			);
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * Note that the result given by {@link #maxRows()} to compute the index of the first row,
		 * is clamped so that it will never be greater then the number of rows in the data structure.
		 */
		@Override
		public IntegerRange rowsRange() {
			int rNum = Math.min(maxRows(), grid.getRowsNum());
			int last = lastRow();
			int first = Math.max(last - rNum + 1, 0);
			return IntegerRange.of(first, last);
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * Note that the result given by {@link #maxColumns()} to compute the index of the first column,
		 * is clamped so that it will never be greater then the number of columns in the data structure.
		 */
		@Override
		public IntegerRange columnsRange() {
			int cNum = Math.min(maxColumns(), grid.getColumnsNum());
			int last = lastColumn();
			int first = Math.max(last - cNum + 1, 0);
			return IntegerRange.of(first, last);
		}

		@Override
		public int maxRows() {
			return (int) (Math.ceil(grid.getHeight() / grid.getCellSize().getHeight()) + 1);
		}

		@Override
		public int maxColumns() {
			return (int) (Math.ceil(grid.getWidth() / grid.getCellSize().getWidth()) + 1);
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * The value is given by: {@cde estimatedSize.getHeight() - virtualFlow.getHeight()}
		 */
		@Override
		public double maxVScroll() {
			return estimatedSize.get().getHeight() - grid.getHeight();
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * The value is given by: {@cde estimatedSize.getWidth() - virtualFlow.getWidth()}
		 */
		@Override
		public double maxHScroll() {
			return estimatedSize.get().getWidth() - grid.getWidth();
		}

		@Override
		public Size computeEstimatedSize() {
			Size cellSize = grid.getCellSize();
			double width = grid.getColumnsNum() * cellSize.getWidth();
			double height = grid.getRowsNum() * cellSize.getHeight();
			Size size = Size.of(width, height);
			estimatedSize.set(size);
			return size;
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * This is the direction along the estimated breath. However, the implementation
		 * makes it so that the position of the viewport is virtual. This binding which depends on both {@link VirtualGrid#positionProperty()}
		 * and {@link VirtualGrid#cellSizeProperty()} will always return a value that is greater or equal to 0 and lesser
		 * than the cell size.
		 * <p>
		 * This is the formula: {@code -virtualGrid.getHPos() % virtualGrid.getCellSize().getWidth()}.
		 * <p>
		 * Think about this. We have cells of width 64. and we scroll 15px on each gesture. When we reach 60px, we can still
		 * see the cell for 4px, but once we scroll again it makes to sense to go to 75px because the first cell won't be
		 * visible anymore, so we go back at 11px. Now the top cell will be visible for 53px. Keep in mind that cells
		 * are always positioned from the end to 0 (exceptions documented here {@link GridState#layoutRows()}).
		 * <p>
		 * Long story short, scrolling is just an illusion, the viewport just scroll by a little to give this illusion and
		 * when needed the cells are just repositioned from the end. This is important because the estimated length
		 * could, in theory, reach very high values, so we don't want the viewport to scroll by thousands of pixels.
		 */
		@Override
		public DoubleBinding xPosBinding() {
			return Bindings.createDoubleBinding(
					() -> -grid.getHPos() % grid.getCellSize().getWidth(),
					grid.positionProperty(), grid.cellSizeProperty()
			);
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * This is the direction along the estimated length. However, the implementation
		 * makes it so that the position of the viewport is virtual. This binding which depends on both {@link VirtualGrid#positionProperty()}
		 * and {@link VirtualGrid#cellSizeProperty()} will always return a value that is greater or equal to 0 and lesser
		 * than the cell size.
		 * <p>
		 * This is the formula: {@code -virtualGrid.getVPos() % virtualGrid.getCellSize().getHeight()}.
		 * <p>
		 * Think about this. We have cells of height 64. and we scroll 15px on each gesture. When we reach 60px, we can still
		 * see the cell for 4px, but once we scroll again it makes to sense to go to 75px because the first cell won't be
		 * visible anymore, so we go back at 11px. Now the top cell will be visible for 53px. Keep in mind that cells
		 * are always positioned from the end to 0 (exceptions documented here {@link GridState#layoutRows()}).
		 * <p>
		 * Long story short, scrolling is just an illusion, the viewport just scroll by a little to give this illusion and
		 * when needed the cells are just repositioned from the end. This is important because the estimated length
		 * could, in theory, reach very high values, so we don't want the viewport to scroll by thousands of pixels.
		 */
		@Override
		public DoubleBinding yPosBinding() {
			return Bindings.createDoubleBinding(
					() -> -grid.getVPos() % grid.getCellSize().getHeight(),
					grid.positionProperty(), grid.cellSizeProperty()
			);
		}

		@Override
		public void scrollToRow(int index) {
			double val = index * grid.getCellSize().getHeight();
			double clampedVal = NumberUtils.clamp(val, 0, maxVScroll());
			grid.setVPos(clampedVal);
		}

		@Override
		public void scrollToColumn(int index) {
			double val = index * grid.getCellSize().getWidth();
			double clampedVal = NumberUtils.clamp(val, 0, maxHScroll());
			grid.setHPos(clampedVal);
		}

		@Override
		public void scrollBy(double pixels, Orientation orientation) {
			if (orientation == Orientation.VERTICAL) {
				double newVal = NumberUtils.clamp(grid.getVPos() + pixels, 0, maxVScroll());
				grid.setVPos(newVal);
			} else {
				double newVal = NumberUtils.clamp(grid.getHPos() + pixels, 0, maxHScroll());
				grid.setHPos(newVal);
			}
		}

		@Override
		public void scrollTo(double pixel, Orientation orientation) {
			if (orientation == Orientation.VERTICAL) {
				double clampedVal = NumberUtils.clamp(pixel, 0, maxVScroll());
				grid.setVPos(clampedVal);
			} else {
				double clampedVal = NumberUtils.clamp(pixel, 0, maxHScroll());
				grid.setHPos(clampedVal);
			}
		}

		/**
		 * {@inheritDoc}
		 *
		 * @param node the node to layout
		 * @param x    the x position of the node
		 * @param y    the y position of the node
		 */
		@Override
		public void layout(Node node, double x, double y) {
			Size cellSize = grid.getCellSize();
			node.resizeRelocate(x, y, cellSize.getWidth(), cellSize.getHeight());
		}

		@Override
		public void dispose() {
			grid.widthProperty().removeListener(widthListener);
			grid.heightProperty().removeListener(heightListener);
			grid.positionProperty().removeListener(positionListener);

			widthListener = null;
			heightListener = null;
			positionListener = null;
		}
	}
}
