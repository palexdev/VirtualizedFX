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

package io.github.palexdev.virtualizedfx.table;

import io.github.palexdev.mfxcore.base.beans.Position;
import io.github.palexdev.mfxcore.base.beans.Size;
import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.base.properties.SizeProperty;
import io.github.palexdev.mfxcore.utils.NumberUtils;
import io.github.palexdev.mfxcore.utils.fx.LayoutUtils;
import io.github.palexdev.virtualizedfx.cell.TableCell;
import io.github.palexdev.virtualizedfx.enums.ColumnsLayoutMode;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.layout.Region;

import java.util.*;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

/**
 * The {@code TableHelper} is a utility interface which collects a series of common computations/operations used by
 * {@link VirtualTable} and its subcomponents to manage the viewport. Has two concrete implementations: {@link FixedTableHelper}
 * and {@link VariableTableHelper}.
 */
public interface TableHelper {

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
	 * @return the number of rows the viewport can display at any time
	 */
	int maxRows();

	/**
	 * @return the number of columns the viewport can display at any time
	 */
	int maxColumns();

	/**
	 * @return the range of visible rows
	 */
	IntegerRange rowsRange();

	/**
	 * @return the range of visible columns
	 */
	IntegerRange columnsRange();

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
	 * The table is a particular virtualized control because the actual viewport height is not the entire
	 * height of the table. The container for the columns is technically not part of the viewport, so it must be subtracted.
	 */
	double getViewportHeight();

	/**
	 * This binding holds the horizontal position of the viewport.
	 */
	DoubleBinding xPosBinding();

	/**
	 * Specifies a certain amount by which shift the horizontal position of columns and
	 * rows. Behavior may differ between implementations and depends also on {@link #layout()}
	 */
	double horizontalOffset();

	/**
	 * This binding holds the vertical position of the viewport.
	 */
	DoubleBinding yPosBinding();

	/**
	 * Specifies a certain amount by which shift the vertical position of rows.
	 * Behavior may differ between implementations and depends also on {@link #layout()}
	 */
	double verticalOffset();

	/**
	 * Invalidates {@link VirtualTable#positionProperty()} in case changes
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
	 * Attempts at auto-sizing the given column to fit its content.
	 */
	void autosizeColumn(TableColumn<?, ? extends TableCell<?>> column);

	/**
	 * Attempts at auto-sizing all the columns.
	 */
	void autosizeColumns();

	/**
	 * Given a certain state, computes the positions of each column/row/cell as a map.
	 * The key is an {@link Orientation} value to differentiate between the vertical and horizontal positions.
	 * <p></p>
	 * Most of the time this computation is not needed and the old positions can be reused, but if for whatever reason
	 * the computation is believed to be necessary then it's possible to force it by setting the desired parameter flags as true.
	 *
	 * @param forceXComputation forces the computation of the HORIZONTAL positions even if not needed
	 * @param forceYComputation forces the computation of the VERTICAL positions even if not needed
	 */
	Map<Orientation, List<Double>> computePositions(TableState<?> state, boolean forceXComputation, boolean forceYComputation);

	/**
	 * Entirely responsible for laying out columns/rows/cells.
	 */
	void layout();

	/**
	 * Disposes bindings/listeners that are not required anymore.
	 */
	void dispose();

	/**
	 * Abstract implementation of {@link TableHelper}, and base class for {@link FixedTableHelper}.
	 */
	abstract class AbstractHelper implements TableHelper {
		protected final VirtualTable<?> table;
		protected final TableManager<?> manager;

		protected ChangeListener<? super Number> widthListener;
		protected ChangeListener<? super Number> heightListener;
		protected ChangeListener<? super Position> positionListener;

		protected final SizeProperty estimatedSize = new SizeProperty(Size.of(0, 0));
		protected DoubleBinding xPosBinding;
		protected DoubleBinding yPosBinding;

		public AbstractHelper(VirtualTable<?> table) {
			this.table = table;
			this.manager = table.getViewportManager();

			widthListener = (o, ov, nv) -> onWidthChanged(ov, nv);
			heightListener = (o, ov, nv) -> onHeightChanged(ov, nv);
			positionListener = (o, ov, nv) -> onPositionChanged(ov, nv);

			table.widthProperty().addListener(widthListener);
			table.heightProperty().addListener(heightListener);
			table.positionProperty().addListener(positionListener);

			((SizeProperty) table.estimatedSizeProperty()).bind(estimatedSize);
		}

		/**
		 * Executed when the table's width changes. Re-initializes the viewport with {@link TableManager#init()}
		 */
		protected void onWidthChanged(Number ov, Number nv) {
			double val = nv.doubleValue();
			if (val > 0 && table.getHeight() > 0)
				manager.init();
		}

		/**
		 * Executed when the table's height changes. Re-initializes the viewport with {@link TableManager#init()}.
		 */
		protected void onHeightChanged(Number ov, Number nv) {
			double val = nv.doubleValue();
			if (val > 0 && table.getWidth() > 0)
				manager.init();
		}

		/**
		 * Executed when the {@link VirtualTable#positionProperty()} changes, responsible for invoking
		 * {@link TableManager#onHScroll()} (if x pos changed( and {@link TableManager#onVScroll()} (if y pos changed)
		 */
		protected void onPositionChanged(Position ov, Position nv) {
			if (manager.isProcessingChange())
				return;

			if (ov.getX() != nv.getX()) {
				manager.onHScroll();
			}
			if (ov.getY() != nv.getY()) {
				manager.onVScroll();
			}
		}

		public double getViewportHeight() {
			double columnsHeight = table.getColumnSize().getHeight();
			double tableHeight = table.getHeight();
			return Math.max(0, tableHeight - columnsHeight);
		}

		/**
		 * {@inheritDoc}
		 * <p></p>
		 * The {@link VirtualTable} always renders one extra column as overscan/buffer. This approach has a little
		 * issue for how scrolling works. When the end of the viewport is reached (horizontally) the extra column
		 * ends up overflowing the table, it's positioned outside of it.
		 * <p>
		 * To make the layout work correctly this method returns an offset of {@code -table.getColumnSize().getWidth()}
		 * to ensure all columns are correctly visualized. Returns 0 if the extra offset is not needed.
		 */
		@Override
		public double horizontalOffset() {
			int columnsNum = table.getColumns().size();
			int maxColumns = maxColumns();
			int firstColumn = firstColumn();
			int lastColumn = firstColumn + maxColumns - 1;
			return (lastColumn > columnsNum - 1 && table.getState().columnsFilled()) ? -table.getColumnSize().getWidth() : 0;
		}

		/**
		 * {@inheritDoc}
		 * <p></p>
		 * The {@link VirtualTable} always renders one extra row as overscan/buffer. This approach has a little
		 * issue for how scrolling works. When the end of the viewport is reached (vertically) the extra row
		 * ends up overflowing the table, it's positioned outside of it.
		 * <p>
		 * To make the layout work correctly this method returns an offset of {@code -table.getCellHeight()}
		 * to ensure all rows are correctly visualized. Returns 0 if the extra offset is not needed.
		 */
		@Override
		public double verticalOffset() {
			int rowsNum = table.getItems().size();
			int maxRows = maxRows();
			int firstRow = firstRow();
			int lastRow = firstRow + maxRows - 1;
			return (lastRow > rowsNum - 1 && table.getState().rowsFilled()) ? -table.getCellHeight() : 0;
		}

		@Override
		public boolean invalidatedPos() {
			double eWidth = estimatedSize.getWidth();
			double eHeight = estimatedSize.getHeight();
			double x = Math.min(eWidth, table.getHPos());
			double y = Math.min(eHeight, table.getVPos());
			Position pos = Position.of(x, y);
			boolean invalid = !table.getPosition().equals(pos);
			table.setPosition(pos);
			return invalid;
		}

		@Override
		public ReadOnlyObjectProperty<Size> estimatedSizeProperty() {
			return estimatedSize.getReadOnlyProperty();
		}

		@Override
		public void dispose() {
			table.widthProperty().removeListener(widthListener);
			table.heightProperty().removeListener(heightListener);
			table.positionProperty().removeListener(positionListener);

			widthListener = null;
			heightListener = null;
			positionListener = null;
		}
	}

	/**
	 * Concrete implementation of {@link AbstractHelper} made to work specifically
	 * with {@link ColumnsLayoutMode#FIXED}.
	 * <p>
	 * As you can guess, this is the most efficient and fast helper since many computations are simplified by the
	 * fact that we know exactly the size of any column at any time.
	 * <p></p>
	 * To be honest, this mode and helper has been developed for those who have tabular data with a lot and I mean a lot
	 * of columns... or maybe you just find fixed columns more attractive?
	 */
	class FixedTableHelper extends AbstractHelper {
		protected final Map<Orientation, List<Double>> positions = new HashMap<>();

		public FixedTableHelper(VirtualTable<?> table) {
			super(table);
		}

		@Override
		public int firstRow() {
			return NumberUtils.clamp(
					(int) Math.floor(table.getVPos() / table.getCellHeight()),
					0,
					table.getItems().size() - 1
			);
		}

		@Override
		public int lastRow() {
			return NumberUtils.clamp(
					firstRow() + maxRows() - 1,
					0,
					table.getItems().size() - 1
			);
		}

		@Override
		public int firstColumn() {
			return NumberUtils.clamp(
					(int) Math.floor(table.getHPos() / table.getColumnSize().getWidth()),
					0,
					table.getColumns().size() - 1
			);
		}

		@Override
		public int lastColumn() {
			return NumberUtils.clamp(
					firstColumn() + maxColumns() - 1,
					0,
					table.getColumns().size() - 1
			);
		}

		@Override
		public int maxRows() {
			return (int) (Math.ceil(getViewportHeight() / table.getCellHeight()) + 1);
		}

		@Override
		public int maxColumns() {
			return (int) (Math.ceil(table.getWidth() / table.getColumnSize().getWidth()) + 1);
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * Note that the result given by {@link #maxRows()} to compute the index of the first row,
		 * is clamped so that it will never be greater then the number of items in the data structure.
		 */
		@Override
		public IntegerRange rowsRange() {
			int rNum = Math.min(maxRows(), table.getItems().size());
			int last = lastRow();
			int first = Math.max(0, last - rNum + 1);
			return IntegerRange.of(first, last);
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * Note that the result given by {@link #maxColumns()} to compute the index of the first column,
		 * is clamped so that it will never be greater then the number of columns in the table.
		 */
		@Override
		public IntegerRange columnsRange() {
			int cNum = Math.min(maxColumns(), table.getColumns().size());
			int last = lastColumn();
			int first = Math.max(0, last - cNum + 1);
			return IntegerRange.of(first, last);
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * The value is given by: {@cde estimatedSize.getHeight() - getViewportHeight()}
		 */
		@Override
		public double maxVScroll() {
			return estimatedSize.getHeight() - getViewportHeight();
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * The value is given by: {@cde estimatedSize.getWidth() - table.getWidth()}
		 */
		@Override
		public double maxHScroll() {
			return estimatedSize.getWidth() - table.getWidth();
		}

		@Override
		public Size computeEstimatedSize() {
			double cellHeight = table.getCellHeight();
			double columnWidth = table.getColumnSize().getWidth();
			double length = table.getItems().size() * cellHeight;
			double breadth = table.getColumns().size() * columnWidth;
			Size size = Size.of(breadth, length);
			estimatedSize.set(size);
			return size;
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * This is the direction along the estimated breath. However, the implementation
		 * makes it so that the position of the viewport is virtual. This binding which depends on both {@link VirtualTable#positionProperty()}
		 * and {@link VirtualTable#columnSizeProperty()} will always return a value that is greater or equal to 0 and lesser
		 * than the columns width. (the value is made negative as this is how scrolling works)
		 * <p>
		 * This is the formula: {@code -table.getHPos() % table.getColumnSize().getWidth()}.
		 * <p>
		 * Think about this. We have columns of width 64. and we scroll 15px on each gesture. When we reach 60px, we can still
		 * see the column for 4px, but once we scroll again it makes no sense to go to 75px because the first column won't be
		 * visible anymore, so we go back at 11px. Now the other column will be visible for 53px.
		 * <p>
		 * Long story short, scrolling is just an illusion, the viewport just scroll by a little to give this illusion and
		 * when needed the columns are just repositioned. This is important because the estimated length
		 * could, in theory, reach very high values, so we don't want the viewport to scroll by thousands of pixels.
		 */
		@Override
		public DoubleBinding xPosBinding() {
			if (xPosBinding == null) {
				xPosBinding = Bindings.createDoubleBinding(
						() -> -table.getHPos() % table.getColumnSize().getWidth(),
						table.positionProperty(), table.columnSizeProperty()
				);
			}
			return xPosBinding;
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * This is the direction along the estimated length. However, the implementation
		 * makes it so that the position of the viewport is virtual. This binding which depends on both {@link VirtualTable#positionProperty()}
		 * and {@link VirtualTable#cellHeightProperty()} will always return a value that is greater or equal to 0 and lesser
		 * than the cell height. (the value is made negative as this is how scrolling works)
		 * <p>
		 * This is the formula: {@code -table.getVPos() % table.getCellHeight()}.
		 * <p>
		 * Think about this. We have cells of width 64. and we scroll 15px on each gesture. When we reach 60px, we can still
		 * see the cell for 4px, but once we scroll again it makes no sense to go to 75px because the first cell won't be
		 * visible anymore, so we go back at 11px. Now the other cell will be visible for 53px.
		 * <p>
		 * Long story short, scrolling is just an illusion, the viewport just scroll by a little to give this illusion and
		 * when needed the cells are just repositioned. This is important because the estimated length
		 * could, in theory, reach very high values, so we don't want the viewport to scroll by thousands of pixels.
		 */
		@Override
		public DoubleBinding yPosBinding() {
			if (yPosBinding == null) {
				yPosBinding = Bindings.createDoubleBinding(
						() -> -table.getVPos() % table.getCellHeight(),
						table.positionProperty(), table.cellHeightProperty()
				);
			}
			return yPosBinding;
		}

		@Override
		public void scrollToRow(int index) {
			double val = index * table.getCellHeight();
			double clampedVal = NumberUtils.clamp(val, 0, maxVScroll());
			table.setVPos(clampedVal);
		}

		@Override
		public void scrollToColumn(int index) {
			double val = index * table.getColumnSize().getWidth();
			double clampedVal = NumberUtils.clamp(val, 0, maxHScroll());
			table.setHPos(clampedVal);
		}

		@Override
		public void scrollBy(double pixels, Orientation orientation) {
			if (orientation == Orientation.VERTICAL) {
				double newVal = NumberUtils.clamp(table.getVPos() + pixels, 0, maxVScroll());
				table.setVPos(newVal);
			} else {
				double newVal = NumberUtils.clamp(table.getHPos() + pixels, 0, maxHScroll());
				table.setHPos(newVal);
			}
		}

		@Override
		public void scrollTo(double pixel, Orientation orientation) {
			if (orientation == Orientation.VERTICAL) {
				double clampedVal = NumberUtils.clamp(pixel, 0, maxVScroll());
				table.setVPos(clampedVal);
			} else {
				double clampedVal = NumberUtils.clamp(pixel, 0, maxHScroll());
				table.setHPos(clampedVal);
			}
		}

		/**
		 * @throws UnsupportedOperationException {@link ColumnsLayoutMode#FIXED} doesn't allow columns to be resized.
		 */
		@Override
		public void autosizeColumn(TableColumn<?, ? extends TableCell<?>> column) {
			throw new UnsupportedOperationException("Fixed Layout Mode for columns doesn't support columns auto-sizing");
		}

		/**
		 * @throws UnsupportedOperationException {@link ColumnsLayoutMode#FIXED} doesn't allow columns to be resized.
		 */
		@Override
		public void autosizeColumns() {
			throw new UnsupportedOperationException("Fixed Layout Mode for columns doesn't support columns auto-sizing");
		}

		/**
		 * {@inheritDoc}
		 * <p></p>
		 * <b>X Positions Computation</b>
		 * <p>
		 * The horizontal positions are computed by using a {@link DoubleStream#iterate(double, DoubleUnaryOperator)}
		 * with {@code columnsNum * columnsW} as the seed and {@code x -> x - columnW} as the operator.
		 * The stream is also limited, {@link DoubleStream#limit(long)}, to the number of columns we need, the results
		 * are collected in a list and put in the positions map with {@link Orientation#HORIZONTAL} as the key.
		 * <p>
		 * Horizontal positions are not computed unless at least one of these conditions is true:
		 * <p> - forceXComputation flag is true
		 * <p> - the positions have not been computed before
		 * <p> - the number of positions previously computed is not equal to the number of columns we need
		 * <p></p>
		 * <b>Y Positions Computation</b>
		 * <p>
		 * The vertical positions are computed by using a {@link DoubleStream#iterate(double, DoubleUnaryOperator)}
		 * with {@code rowsNum * cellHeight} as the seed and {@code x -> x - cellHeight} as the operator.
		 * The stream is also limited, {@link DoubleStream#limit(long)}, to the number of rows we need, the results
		 * are stored in a list and put in the positions map with {@link Orientation#VERTICAL} as the key.
		 * <p>
		 * Vertical positions are not computed unless at least one of these conditions is true:
		 * <p> - forceYComputation flag is true
		 * <p> - the positions have not been computed before
		 * <p> - the number of positions previously computed is not equal to the number of rows we need
		 */
		@Override
		public Map<Orientation, List<Double>> computePositions(TableState<?> state, boolean forceXComputation, boolean forceYComputation) {
			IntegerRange rowsRange = state.getRowsRange();
			IntegerRange columnsRange = state.getColumnsRange();
			double colW = table.getColumnSize().getWidth();
			double cellH = table.getCellHeight();

			List<Double> xPositions = positions.computeIfAbsent(Orientation.HORIZONTAL, o -> new ArrayList<>());
			Integer cRangeDiff = columnsRange.diff();
			if (forceXComputation || xPositions.isEmpty() || xPositions.size() != cRangeDiff + 1) {
				xPositions.clear();
				xPositions.addAll(DoubleStream.iterate(cRangeDiff * colW, x -> x - colW)
						.limit(cRangeDiff + 1)
						.boxed()
						.collect(Collectors.toList())
				);
			}

			List<Double> yPositions = positions.computeIfAbsent(Orientation.VERTICAL, o -> new ArrayList<>());
			Integer rRangeDiff = rowsRange.diff();
			if (forceYComputation || yPositions.isEmpty() || yPositions.size() != rRangeDiff + 1) {
				yPositions.clear();
				yPositions.addAll(DoubleStream.iterate(rRangeDiff * cellH, x -> x - cellH)
						.limit(rRangeDiff + 1)
						.boxed()
						.collect(Collectors.toList())
				);
			}
			return positions;
		}

		/**
		 * {@inheritDoc}
		 * <p></p>
		 * The layout makes use of the current table' state, {@link VirtualTable#stateProperty()}, and the positions
		 * computed by {@link #computePositions(TableState, boolean, boolean)}, this is invoked without forcing the re-computation.
		 * <p>
		 * Exits immediately if the state is {@link TableState#EMPTY}, if {@link #invalidatedPos()} returns true,
		 * or if {@link VirtualTable#needsViewportLayoutProperty()} is false.
		 * <p></p>
		 * Before proceeding with layout retrieves the following parameters:
		 * <p> - the columns width
		 * <p> - the columns height
		 * <p> - the columns range
		 * <p> - the X positions
		 * <p> - the X offset with {@link #horizontalOffset()}
		 * <p> - the cells height
		 * <p> - the Y positions
		 * <p> - the Y offset with {@link #verticalOffset()}
		 * <p></p>
		 * Columns are laid out from left to right, relocated at the extracted X position (+ the X offset) and at Y 0;
		 * and resized with the previously gathered sizes. The last column is an exception because if not all the space
		 * of the table was occupied by laying out the previous columns than it width will be set to the entire remaining
		 * space.
		 * <p></p>
		 * Rows are laid out from top to bottom, relocated at the previously computed X offset and at the extracted Y position
		 * (+ the Y offset); and resized with the previously gathered height. The width is given by the maximum between
		 * the table width and the row size (given by the number of cells in the row multiplied by the columns width)
		 * <p></p>
		 * For each row in the loop it also lays out the their cells. Each cell is relocated at the extracted X position
		 * and at Y 0; and resized to the previously gathered cell height. The width is the same of the corresponding column.
		 */
		@Override
		public void layout() {
			TableState<?> state = table.getState();
			if (state == TableState.EMPTY) return;

			if (!table.isNeedsViewportLayout()) return;
			if (invalidatedPos()) return;
			Map<Orientation, List<Double>> positions = computePositions(state, false, false);

			double colW = table.getColumnSize().getWidth();
			double colH = table.getColumnSize().getHeight();
			IntegerRange columnsRange = state.getColumnsRange();
			double xOffset = horizontalOffset();
			List<Double> xPositions = positions.get(Orientation.HORIZONTAL);
			int xI = xPositions.size() - 1;

			double totalW = 0.0;
			for (Integer cIndex : columnsRange) {
				totalW += colW;
				TableColumn<?, ? extends TableCell<?>> column = table.getColumn(cIndex);
				Region region = column.getRegion();
				Double xPos = xPositions.get(xI);

				if (cIndex.equals(columnsRange.getMax()) && totalW < table.getWidth()) {
					region.resizeRelocate(xPos + xOffset, 0, table.getWidth() - totalW + colW, colH);
				} else {
					region.resizeRelocate(xPos + xOffset, 0, colW, colH);
				}
				xI--;
			}

			double cellH = table.getCellHeight();
			double yOffset = verticalOffset();
			List<Double> yPositions = positions.get(Orientation.VERTICAL);
			int yI = yPositions.size() - 1;
			for (TableRow<?> row : state.getRows().values()) {
				xI = xPositions.size() - 1;
				Double yPos = yPositions.get(yI);
				double rowW = Math.max(row.size() * colW, table.getWidth());
				row.resizeRelocate(xOffset, yPos + yOffset, rowW, cellH);

				List<TableCell<?>> cells = new ArrayList<>(row.getCells().values());
				for (int i = 0; i < cells.size(); i++) {
					TableCell<?> cell = cells.get(i);
					TableColumn<?, ? extends TableCell<?>> column = table.getColumn(i);
					Node node = cell.getNode();
					cell.beforeLayout();
					node.resizeRelocate(xPositions.get(xI), 0, column.getRegion().getWidth(), cellH);
					cell.afterLayout();
					xI--;
				}
				yI--;
			}
		}

		@Override
		public void dispose() {
			super.dispose();

			if (xPosBinding != null) xPosBinding.dispose();
			if (yPosBinding != null) yPosBinding.dispose();
			xPosBinding = null;
			yPosBinding = null;
		}
	}

	/**
	 * Extension of {@link FixedTableHelper} made to work specifically
	 * with {@link ColumnsLayoutMode#VARIABLE}.
	 * <p></p>
	 * Many of the methods of the other mode can be reused for this, just a few need their behavior to be redefined.
	 * <p>
	 * As you can guess, this is not the most efficient. All columns are laid out in the viewport which directly translates
	 * to "there are more cells in the viewport". You can also consider this mode as "partially virtualized", rows will still
	 * be virtualized but since columns widths are variable we have no way to easily and consistently virtualize them too.
	 * <p>
	 * The advantage of this mode is of course having columns that can be resized programmatically or by a gesture at runtime.
	 */
	class VariableTableHelper extends FixedTableHelper {

		public VariableTableHelper(VirtualTable<?> table) {
			super(table);
		}

		/**
		 * @return 0 or -1 if the columns list is empty
		 */
		@Override
		public int firstColumn() {
			return Math.min(0, table.getColumns().size() - 1);
		}

		/**
		 * @return the columns list size -1
		 */
		@Override
		public int lastColumn() {
			return table.getColumns().size() - 1;
		}

		/**
		 * @return the size of the columns list
		 */
		@Override
		public int maxColumns() {
			return table.getColumns().size();
		}

		/**
		 * @return an {@link IntegerRange} made from the values of {@link #firstColumn()} and {@link #lastColumn()}
		 */
		@Override
		public IntegerRange columnsRange() {
			return IntegerRange.of(firstColumn(), lastColumn());
		}

		/**
		 * {@inheritDoc}
		 * <p></p>
		 * The breadth is computed by iterating over all the columns and getting their width.
		 * To be precise the width used by the computation is given by the maximum between the actual width of the
		 * column's region and the size specified by {@link VirtualTable#columnSizeProperty()}.
		 */
		@Override
		public Size computeEstimatedSize() {
			double cellHeight = table.getCellHeight();
			double length = table.getItems().size() * cellHeight;
			double breadth = table.getColumns().stream()
					.mapToDouble(c -> Math.max(c.getRegion().getWidth(), table.getColumnSize().getWidth()))
					.sum();
			Size size = Size.of(breadth, length);
			estimatedSize.set(size);
			return size;
		}

		/**
		 * This binding holds the horizontal position of the viewport.
		 * This is the direction along the estimated breath.
		 * <p>
		 * This is not virtualized as columns have variable widths and the value is simply given by
		 * {@code -table.getHPos()}.
		 */
		@Override
		public DoubleBinding xPosBinding() {
			if (xPosBinding == null) {
				xPosBinding = Bindings.createDoubleBinding(
						() -> -table.getHPos(),
						table.positionProperty()
				);
			}
			return xPosBinding;
		}

		/**
		 * {@inheritDoc}
		 * <p></p>
		 * This is actually more complicated for this layout mode as we can't always get the position at which a column
		 * will be in the viewport. In fact, if the table has not been laid out yet, meaning that its skin is null, or
		 * if the interested column has not been laid out yet there's no way to know where it is in the viewport.
		 * <p>
		 * For this reason this distinguish between two cases:
		 * <p> 1) Everything has been laid out at least once: the column position is given by
		 * {@code column.getRegion().getBoundsInParent().getMinX()}
		 * <p> 2) We cannot rely on the aforementioned method, we resort on the "super" method.
		 * Since {@link VirtualTable} specifies the minimum width every column must have at the initialization we can
		 * easily predict were it will be at the start.
		 */
		@Override
		public void scrollToColumn(int index) {
			TableColumn<?, ? extends TableCell<?>> column = table.getColumn(index);
			Region region = column.getRegion();

			if (table.getSkin() == null ||
					region.getScene() == null ||
					region.getWidth() <= 0) {
				// If any of these conditions happen it probably means that this method
				// has been called before the first layout (as with variable mode all
				// columns are laid out always). This means that we can use the specified
				// width (see property in table) as a guarantee that all columns will have
				// the same initial width and as a consequence use the "super" method
				super.scrollToColumn(index);
			}

			double minX = region.getBoundsInParent().getMinX();
			double clampedVal = NumberUtils.clamp(minX, 0, maxHScroll());
			table.setHPos(clampedVal);
		}

		/**
		 * Attempts at auto-sizing the given column to fit its content.
		 * <p></p>
		 * To accomplish this we need the current state of the table, {@link VirtualTable#stateProperty()},
		 * and the index of the given column, {@link VirtualTable#getColumnIndex(TableColumn)}.
		 * (if the state is {@link TableState#EMPTY} exits immediately).
		 * <p>
		 * We get the rows from the state and then use {@link TableRow#getWidthOf(int)} to get the preferred width
		 * of the cell at index (same index of column). From these results we get the maximum value and this will be
		 * the new width of the column.
		 * <p></p>
		 * The last column is handled differently though. First we compute the total width of all the columns before.
		 * If this is lesser than the table width than the column will be resized to make it occupy all the available space.
		 * If this is not the case, then we fall back to the normal handling.
		 */
		@Override
		public void autosizeColumn(TableColumn<?, ? extends TableCell<?>> column) {
			TableState<?> state = table.getState();
			if (state == TableState.EMPTY) return;

			Region region = column.getRegion();
			ObservableList<? extends TableColumn<?, ? extends TableCell<?>>> columns = table.getColumns();
			int cIndex = table.getColumnIndex(((TableColumn) column));
			double targetW;

			// If it's last index, special handling to always use all the available remaining space
			if (cIndex == columns.size() - 1) {
				// First compute total width for all previous columns
				double totalW = columns.stream()
						.map(TableColumn::getRegion)
						.mapToDouble(Region::getWidth)
						.sum() - region.getWidth();

				// If less than table width then targetW becomes `table.getWidth() - totalW + colW`
				if (totalW < table.getWidth()) {
					targetW = table.getWidth() - totalW + region.getWidth();

					// Here we terminate the auto-sizing, if the condition is false
					// then the other "method" is used
					region.setPrefWidth(targetW);
					computeEstimatedSize();
					computePositions(state, true, false);
					layout();
					return;
				}
			}

			Collection<? extends TableRow<?>> rows = state.getRows().values();
			targetW = rows.stream()
					.mapToDouble(r -> r.getWidthOf(cIndex))
					.max()
					.orElseGet(region::getWidth);

			region.setPrefWidth(targetW);
			computeEstimatedSize();
			computePositions(state, true, false);
			layout();
		}

		/**
		 * Calls {@link #autosizeColumn(TableColumn)} on all the columns in the table.
		 */
		@Override
		public void autosizeColumns() {
			table.getColumns().forEach(this::autosizeColumn);
		}

		/**
		 * Given a certain state, computes the positions of each column/row/cell as a map.
		 * The key is an {@link Orientation} value to differentiate between the vertical and horizontal positions.
		 * <p></p>
		 * Most of the time this computation is not needed and the old positions can be reused, but if for whatever reason
		 * the computation is believed to be necessary then it's possible to force it by setting the desired parameter flags as true.
		 * <p></p>
		 * <b>X Positions Computation</b>
		 * <p>
		 * The horizontal positions are computed by iterating over all the columns and getting their width as the maximum
		 * between its current width and the minimum width specified by {@link VirtualTable#columnSizeProperty()}.
		 * The positions are actually computed with a simple accumulator.
		 * <p>
		 * Horizontal positions are not computed unless at least one of these conditions is true:
		 * <p> - forceXComputation flag is true
		 * <p> - the positions have not been computed before
		 * <p></p>
		 * <b>Y Positions Computation</b>
		 * <p>
		 * The vertical positions are computed by using a {@link DoubleStream#iterate(double, DoubleUnaryOperator)}
		 * with {@code rowsNum * cellHeight} as the seed and {@code x -> x - cellHeight} as the operator.
		 * The stream is also limited, {@link DoubleStream#limit(long)}, to the number of rows we need, the results
		 * are stored in a list and put in the positions map with {@link Orientation#VERTICAL} as the key.
		 * <p>
		 * Vertical positions are not computed unless at least one of these conditions is true:
		 * <p> - forceYComputation flag is true
		 * <p> - the positions have not been computed before
		 * <p> - the number of positions previously computed is not equal to the number of rows we need
		 *
		 * @param forceXComputation forces the computation of the HORIZONTAL positions even if not needed
		 * @param forceYComputation forces the computation of the VERTICAL positions even if not needed
		 */
		@Override
		public Map<Orientation, List<Double>> computePositions(TableState<?> state, boolean forceXComputation, boolean forceYComputation) {
			if (state == TableState.EMPTY || state.isEmpty()) return positions;
			IntegerRange rowsRange = state.getRowsRange();
			IntegerRange columnsRange = state.getColumnsRange();
			double cellH = table.getCellHeight();

			List<Double> xPositions = positions.computeIfAbsent(Orientation.HORIZONTAL, o -> new ArrayList<>());
			if (forceXComputation || xPositions.isEmpty()) {
				xPositions.clear();
				double pos = 0;
				for (Integer cIndex : columnsRange) {
					TableColumn<?, ? extends TableCell<?>> column = table.getColumn(cIndex);
					Region region = column.getRegion();
					xPositions.add(pos);
					double colW = Math.max(LayoutUtils.boundWidth(region), table.getColumnSize().getWidth());
					pos += colW;
				}
			}

			List<Double> yPositions = positions.computeIfAbsent(Orientation.VERTICAL, o -> new ArrayList<>());
			Integer rRangeDiff = rowsRange.diff();
			if (forceYComputation || yPositions.isEmpty() || yPositions.size() != rRangeDiff + 1) {
				yPositions.clear();
				yPositions.addAll(DoubleStream.iterate(rRangeDiff * cellH, x -> x - cellH)
						.limit(rRangeDiff + 1)
						.boxed()
						.collect(Collectors.toList())
				);
			}
			return positions;
		}

		/**
		 * Entirely responsible for laying out columns/rows/cells.
		 * <p></p>
		 * The layout makes use of the current table' state, {@link VirtualTable#stateProperty()}, and the positions
		 * computed by {@link #computePositions(TableState, boolean, boolean)}, this is invoked without forcing the re-computation.
		 * <p>
		 * Exits immediately if the state is {@link TableState#EMPTY}, if {@link #invalidatedPos()} returns true,
		 * or if {@link VirtualTable#needsViewportLayoutProperty()} is false.
		 * <p></p>
		 * Before proceeding with layout retrieves the following parameters:
		 * <p> - the columns height
		 * <p> - the columns range
		 * <p> - the X positions
		 * <p> - the cells height
		 * <p> - the Y positions
		 * <p> - the Y offset with {@link #verticalOffset()}
		 * <p></p>
		 * Columns are laid out from left to right, relocated at the extracted X position and at Y 0;
		 * and resized to the previously gathered height. The width is computed as the maximum between the column's region
		 * width and the minimum width specified by {@link VirtualTable#columnSizeProperty()}.
		 * The last column is an exception because if not all the space of the table was occupied by
		 * laying out the previous columns than its width will be set to the entire remaining
		 * space.
		 * <p></p>
		 * Rows are laid out from top to bottom, relocated at X 0 and at the extracted Y position (+ the Y offset);
		 * and resized with the previously gathered height. The width is given by the maximum between
		 * the table width and the row size (the row's region width plus the minimum columns width given by {@link VirtualTable#columnSizeProperty()}).
		 * <p></p>
		 * For each row in the loop it also lays out their cells. Each cell is relocated at the extracted X position
		 * and at Y 0; and resized to the previously gathered cell height. The width is the same of the corresponding column.
		 */
		@Override
		public void layout() {
			TableState<?> state = table.getState();
			if (state == TableState.EMPTY) return;

			if (!table.isNeedsViewportLayout()) return;
			if (invalidatedPos()) return;
			Map<Orientation, List<Double>> positions = computePositions(state, false, false);

			double colH = table.getColumnSize().getHeight();
			IntegerRange columnsRange = state.getColumnsRange();
			List<Double> xPositions = positions.get(Orientation.HORIZONTAL);
			int xI = 0;

			double totalW = 0.0;
			for (Integer cIndex : columnsRange) {
				TableColumn<?, ? extends TableCell<?>> column = table.getColumn(cIndex);
				Region region = column.getRegion();
				double colW = Math.max(LayoutUtils.boundWidth(region), table.getColumnSize().getWidth());
				Double xPos = xPositions.get(xI);
				totalW += colW;

				if (cIndex.equals(columnsRange.getMax()) && totalW < table.getWidth()) {
					region.resizeRelocate(xPos, 0, table.getWidth() - totalW + colW, colH);
				} else {
					region.resizeRelocate(xPos, 0, colW, colH);
				}
				xI++;
			}

			double cellH = table.getCellHeight();
			double yOffset = verticalOffset();
			List<Double> yPositions = positions.get(Orientation.VERTICAL);
			int yI = yPositions.size() - 1;
			for (TableRow<?> row : state.getRows().values()) {
				xI = 0;
				Double yPos = yPositions.get(yI);
				double rowW = Math.max(LayoutUtils.boundWidth(row) + table.getColumnSize().getWidth(), table.getWidth());
				row.resizeRelocate(0, yPos + yOffset, rowW, cellH);

				List<TableCell<?>> cells = new ArrayList<>(row.getCells().values());
				for (int i = 0; i < cells.size(); i++) {
					TableCell<?> cell = cells.get(i);
					TableColumn<?, ? extends TableCell<?>> column = table.getColumn(i);
					Node node = cell.getNode();
					cell.beforeLayout();
					node.resizeRelocate(xPositions.get(xI), 0, column.getRegion().getWidth(), cellH);
					cell.afterLayout();
					xI++;
				}
				yI--;
			}
		}
	}
}
