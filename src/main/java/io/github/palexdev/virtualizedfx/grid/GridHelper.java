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

import io.github.palexdev.mfxcore.base.beans.SizeBean;
import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.utils.NumberUtils;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Orientation;
import javafx.scene.Node;

/**
 * The {@code GridHelper} is a utility interface with oen concrete implementation {@link DefaultGridHelper}
 * used by the {@link VirtualGrid} and its subcomponents to separate common computations/operations.
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
	 * @return the total number of cells the viewport can display. Usually the multiplication of
	 * {@link #maxRows()} and {@link #maxColumns()}
	 */
	int maxCells();

	/**
	 * @return the maximum amount of pixels the viewport can scroll vertically
	 */
	double maxVScroll();

	/**
	 * @return the maximum amount of pixels the viewport can scroll horizontally
	 */
	double maxHScroll();

	/**
	 * @return the virtual height of the grid
	 */
	double computeEstimatedLength();

	/**
	 * @return the virtual width of the grid
	 */
	double computeEstimatedBreadth();

	/**
	 * Keeps the results of {@link #computeEstimatedLength()}.
	 */
	ReadOnlyDoubleProperty estimatedLengthProperty();

	/**
	 * Keeps the results of {@link #computeEstimatedBreadth()}.
	 */
	ReadOnlyDoubleProperty estimatedBreadthProperty();

	/**
	 * This binding holds the horizontal position of the viewport.
	 */
	DoubleBinding xPosBinding();

	/**
	 * This binding holds the vertical position of the viewport.
	 */
	DoubleBinding yPosBinding();

	/**
	 * Invalidates both {@link VirtualGrid#hPosProperty()} and {@link VirtualGrid#vPosProperty()} in case changes
	 * occurred in the viewport and the current position are no longer valid.
	 */
	void invalidatePos();

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
	abstract class AbstractGridHelper implements GridHelper {
		protected final VirtualGrid<?, ?> virtualGrid;
		protected final ViewportManager<?, ?> viewportManager;

		protected final DoubleProperty estimatedLength = new SimpleDoubleProperty();
		protected final DoubleProperty estimatedBreadth = new SimpleDoubleProperty();

		public AbstractGridHelper(VirtualGrid<?, ?> virtualGrid) {
			this.virtualGrid = virtualGrid;
			this.viewportManager = virtualGrid.getViewportManager();
		}

		@Override
		public void invalidatePos() {
			double length = estimatedLength.get();
			double breadth = estimatedBreadth.get();
			virtualGrid.setVPos(Math.min(virtualGrid.getVPos(), length));
			virtualGrid.setHPos(Math.min(virtualGrid.getHPos(), breadth));
		}

		@Override
		public ReadOnlyDoubleProperty estimatedLengthProperty() {
			return estimatedLength;
		}

		@Override
		public ReadOnlyDoubleProperty estimatedBreadthProperty() {
			return estimatedBreadth;
		}
	}

	/**
	 * Concrete implementation of {@link AbstractGridHelper} with default/expected behavior for a virtual grid.
	 * <p></p>
	 * This helper adds the following listeners:
	 * <p> - A listener to the virtual grid's width to re-initialize the viewport, {@link ViewportManager#init()}
	 * <p> - A listener to the virtual grid's height to re-initialize the viewport, {@link ViewportManager#init()}
	 * <p> - A listener on the virtual grid's vPos property to process the scroll, {@link ViewportManager#onVScroll()}
	 * <p> - A listener on the virtual grid's hPos property to process the scroll, {@link ViewportManager#onHScroll()}
	 */
	class DefaultGridHelper extends AbstractGridHelper {
		private ChangeListener<? super Number> widthListener;
		private ChangeListener<? super Number> heightListener;
		private InvalidationListener hPosListener;
		private InvalidationListener vPosListener;

		public DefaultGridHelper(VirtualGrid<?, ?> virtualGrid) {
			super(virtualGrid);

			widthListener = (observable, oldValue, newValue) -> {
				double val = newValue.doubleValue();
				if (val > 0 && virtualGrid.getHeight() > 0)
					viewportManager.init();
			};
			heightListener = (observable, oldValue, newValue) -> {
				double val = newValue.doubleValue();
				if (val > 0 && virtualGrid.getWidth() > 0)
					viewportManager.init();
			};

			hPosListener = invalidated -> viewportManager.onHScroll();
			vPosListener = invalidated -> viewportManager.onVScroll();

			virtualGrid.widthProperty().addListener(widthListener);
			virtualGrid.heightProperty().addListener(heightListener);
			virtualGrid.hPosProperty().addListener(hPosListener);
			virtualGrid.vPosProperty().addListener(vPosListener);

			((DoubleProperty) virtualGrid.estimatedLengthProperty()).bind(estimatedLengthProperty());
			((DoubleProperty) virtualGrid.estimatedBreadthProperty()).bind(estimatedBreadthProperty());
		}

		@Override
		public int firstRow() {
			return NumberUtils.clamp(
					(int) Math.floor(virtualGrid.getVPos() / virtualGrid.getCellSize().getHeight()),
					0,
					virtualGrid.getItems().getRowsNum() - 1
			);
		}

		@Override
		public int firstColumn() {
			return NumberUtils.clamp(
					(int) Math.floor(virtualGrid.getHPos() / virtualGrid.getCellSize().getWidth()),
					0,
					virtualGrid.getItems().getColumnsNum() - 1
			);
		}

		@Override
		public int lastRow() {
			return NumberUtils.clamp(
					firstRow() + maxRows() - 1,
					0,
					virtualGrid.getItems().getRowsNum() - 1
			);
		}

		@Override
		public int lastColumn() {
			return NumberUtils.clamp(
					firstColumn() + maxColumns() - 1,
					0,
					virtualGrid.getItems().getColumnsNum() - 1
			);
		}

		@Override
		public IntegerRange rowsRange() {
			int rNum = maxRows();
			int last = lastRow();
			int first = Math.max(last - rNum + 1, 0);
			return IntegerRange.of(first, last);
		}

		@Override
		public IntegerRange columnsRange() {
			int cNum = maxColumns();
			int last = lastColumn();
			int first = Math.max(last - cNum + 1, 0);
			return IntegerRange.of(first, last);
		}

		@Override
		public int maxRows() {
			return (int) (Math.ceil(virtualGrid.getHeight() / virtualGrid.getCellSize().getHeight()) + 1);
		}

		@Override
		public int maxColumns() {
			return (int) (Math.ceil(virtualGrid.getWidth() / virtualGrid.getCellSize().getWidth()) + 1);
		}

		@Override
		public int maxCells() {
			return maxRows() * maxColumns();
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * The value is given by: {@cde estimatedLength - virtualFlow.getHeight()}
		 */
		@Override
		public double maxVScroll() {
			return estimatedLengthProperty().get() - virtualGrid.getHeight();
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * The value is given by: {@cde estimatedBreadth - virtualFlow.getWidth()}
		 */
		@Override
		public double maxHScroll() {
			return estimatedBreadthProperty().get() - virtualGrid.getWidth();
		}

		@Override
		public double computeEstimatedLength() {
			double cHeight = virtualGrid.getCellSize().getHeight();
			double val = virtualGrid.getItems().getRowsNum() * cHeight;
			estimatedLength.set(val);
			return val;
		}

		@Override
		public double computeEstimatedBreadth() {
			double cWidth = virtualGrid.getCellSize().getWidth();
			double val = virtualGrid.getItems().getColumnsNum() * cWidth;
			estimatedBreadth.set(val);
			return val;
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * This is the direction along the estimated breath. However, the implementation
		 * makes it so that the position of the viewport is virtual. This binding which depends on both {@link VirtualGrid#hPosProperty()}
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
					() -> -virtualGrid.getHPos() % virtualGrid.getCellSize().getWidth(),
					virtualGrid.hPosProperty(), virtualGrid.cellSizeProperty()
			);
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * This is the direction along the estimated length. However, the implementation
		 * makes it so that the position of the viewport is virtual. This binding which depends on both {@link VirtualGrid#vPosProperty()}
		 * and {@link VirtualGrid#cellSizeProperty()} will always return a value that is greater or equal to 0 and lesser
		 * than the cell size.
		 * <p>
		 * This is the formula: {@code -virtualGrid.getHPos() % virtualGrid.getCellSize().getHeight()}.
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
					() -> -virtualGrid.getVPos() % virtualGrid.getCellSize().getHeight(),
					virtualGrid.vPosProperty(), virtualGrid.cellSizeProperty()
			);
		}

		@Override
		public void scrollToRow(int index) {
			double val = index * virtualGrid.getCellSize().getHeight();
			double clampedVal = NumberUtils.clamp(val, 0, maxVScroll());
			virtualGrid.setVPos(clampedVal);
		}

		@Override
		public void scrollToColumn(int index) {
			double val = index * virtualGrid.getCellSize().getWidth();
			double clampedVal = NumberUtils.clamp(val, 0, maxHScroll());
			virtualGrid.setHPos(clampedVal);
		}

		@Override
		public void scrollBy(double pixels, Orientation orientation) {
			if (orientation == Orientation.VERTICAL) {
				double newVal = NumberUtils.clamp(virtualGrid.getVPos() + pixels, 0, maxVScroll());
				virtualGrid.setVPos(newVal);
			} else {
				double newVal = NumberUtils.clamp(virtualGrid.getHPos() + pixels, 0, maxHScroll());
				virtualGrid.setHPos(newVal);
			}
		}

		@Override
		public void scrollTo(double pixel, Orientation orientation) {
			if (orientation == Orientation.VERTICAL) {
				double clampedVal = NumberUtils.clamp(pixel, 0, maxVScroll());
				virtualGrid.setVPos(clampedVal);
			} else {
				double clampedVal = NumberUtils.clamp(pixel, 0, maxHScroll());
				virtualGrid.setHPos(clampedVal);
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
			SizeBean size = virtualGrid.getCellSize();
			node.resizeRelocate(x, y, size.getWidth(), size.getHeight());
		}

		@Override
		public void dispose() {
			virtualGrid.widthProperty().removeListener(widthListener);
			virtualGrid.heightProperty().removeListener(heightListener);
			virtualGrid.hPosProperty().removeListener(hPosListener);
			virtualGrid.vPosProperty().removeListener(vPosListener);

			widthListener = null;
			heightListener = null;
			hPosListener = null;
			vPosListener = null;
		}
	}
}
