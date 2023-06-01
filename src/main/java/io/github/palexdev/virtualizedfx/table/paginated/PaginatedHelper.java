package io.github.palexdev.virtualizedfx.table.paginated;

import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.utils.fx.LayoutUtils;
import io.github.palexdev.virtualizedfx.cell.TableCell;
import io.github.palexdev.virtualizedfx.enums.ColumnsLayoutMode;
import io.github.palexdev.virtualizedfx.table.*;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

/**
 * Small API extension of {@link TableHelper}, to be used with {@link PaginatedVirtualTable}.
 */
public interface PaginatedHelper extends TableHelper {

	/**
	 * Converts the given page number to the corresponding position in pixels
	 */
	double pageToPos(int page);

	/**
	 * Scrolls the viewport to the desired page
	 */
	void goToPage(int page);

	/**
	 * Concrete implementation of {@link PaginatedHelper} and extension of {@link FixedTableHelper}
	 * to be used for {@link PaginatedVirtualTable} when its {@link VirtualTable#columnsLayoutModeProperty()} is
	 * set to {@link ColumnsLayoutMode#FIXED}.
	 */
	class FixedPaginatedTableHelper extends FixedTableHelper implements PaginatedHelper {
		protected PaginatedVirtualTable<?> pTable;

		public FixedPaginatedTableHelper(PaginatedVirtualTable<?> table) {
			super(table);
			this.pTable = table;
		}

		/**
		 * Overridden to return {@link PaginatedVirtualTable#getRowsPerPage()}.
		 */
		@Override
		public int maxRows() {
			return pTable.getRowsPerPage();
		}

		/**
		 * Overridden to return: {@code pageToPos(maxPage)}.
		 */
		@Override
		public double maxVScroll() {
			return pageToPos(pTable.getMaxPage());
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * This is given by {@code (page - 1) * rowsPerPage * cellHeight}.
		 */
		@Override
		public double pageToPos(int page) {
			return (page - 1) * pTable.getRowsPerPage() * pTable.getCellHeight();
		}

		@Override
		public void goToPage(int page) {
			super.scrollTo(pageToPos(page), Orientation.VERTICAL);
		}

		/**
		 * This is unsupported as the {@link PaginatedVirtualTable} can only scroll to certain pixel values, given
		 * by the pages.
		 *
		 * @throws UnsupportedOperationException
		 */
		@Override
		public void scrollBy(double pixels, Orientation orientation) {
			throw new UnsupportedOperationException("The paginated table cannot scroll by a given amount of pixels");
		}

		/**
		 * This is unsupported as the {@link PaginatedVirtualTable} can only scroll to certain pixel values, given
		 * by the pages.
		 *
		 * @throws UnsupportedOperationException
		 */
		@Override
		public void scrollTo(double pixel, Orientation orientation) {
			throw new UnsupportedOperationException("The paginated table cannot scroll to a given pixel position as it may be wrong");
		}

		/**
		 * This is unsupported as the {@link PaginatedVirtualTable} can only scroll to certain pixel values, given
		 * by the pages.
		 *
		 * @throws UnsupportedOperationException
		 */
		@Override
		public void scrollToRow(int index) {
			throw new UnsupportedOperationException("The paginated table cannot scroll to a given row index");
		}

		/**
		 * {@inheritDoc}
		 * <p></p>
		 * This is overridden by {@link FixedPaginatedTableHelper} because for the vertical positions we first need to check
		 * (end eventually hide) those rows that are not in the rows range.
		 * <p>
		 * The range of rows to layout may not be the same as the one carried by {@link VirtualTable#stateProperty()},
		 * so it's needed to properly compute it as:
		 * <p> Min = {@link #firstRow()}
		 * <p> Max = {@code Math.min(firstRow + maxRows - 1, table.getItems().size() - 1)}, see {@link #maxRows()}
		 * <p></p>
		 * This also means that the vertical positions will be computed only for those rows that will still be visible.
		 * <p></p>
		 * A case such this may occur when we reach a page for which there are not enough items in the table to fill it.
		 */
		@Override
		public Map<Orientation, List<Double>> computePositions(TableState<?> state, boolean forceXComputation, boolean forceYComputation) {
			IntegerRange columnsRange = state.getColumnsRange();
			double colW = table.getColumnSize().getWidth();
			double cellH = table.getCellHeight();
			AtomicBoolean anyHidden = new AtomicBoolean(false);

			// Hidden rows check
			int firstRow = firstRow();
			int lastRow = Math.min(firstRow + maxRows() - 1, table.getItems().size() - 1);
			IntegerRange rowsRange = IntegerRange.of(firstRow, lastRow);
			Map<Integer, ? extends TableRow<?>> rows = state.getRowsUnmodifiable();
			rows.entrySet().stream()
					.filter(e -> !IntegerRange.inRangeOf(e.getKey(), rowsRange))
					.peek(e -> anyHidden.set(true))
					.forEach(e -> e.getValue().setVisible(false));

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
			if (anyHidden.get() || forceYComputation || yPositions.isEmpty() || yPositions.size() != rRangeDiff + 1) {
				yPositions.clear();
				double pos = rRangeDiff * cellH;
				for (int i = firstRow; i <= lastRow; i++) {
					TableRow<?> row = rows.get(i);
					yPositions.add(pos);
					pos -= cellH;
					row.setVisible(true);
				}
			}
			return positions;
		}

		/**
		 * {@inheritDoc}
		 * <p></p>
		 * This is overridden by {@link FixedPaginatedTableHelper} to skip the layout of hidden rows, this is a must
		 * since the {@link #computePositions(TableState, boolean, boolean)} is also overridden to compute only the positions
		 * for the visible rows.
		 */
		@Override
		public void layout() {
			TableState<?> state = table.getState();
			if (state.isEmptyAll() || !table.isNeedsViewportLayout() || invalidatedPos()) {
				layoutInitialized.set(false);
				return;
			}
			Map<Orientation, List<Double>> positions = computePositions(state, false, false);

			double colW = table.getColumnSize().getWidth();
			double colH = table.getColumnSize().getHeight();
			IntegerRange columnsRange = state.getColumnsRange();
			double xOffset = horizontalOffset();
			List<Double> xPositions = positions.get(Orientation.HORIZONTAL);
			int xI = xPositions.size() - 1;

			// Columns layout
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

			// Cells layout
			if (!state.isEmpty()) {
				double cellH = table.getCellHeight();
				double yOffset = verticalOffset();
				List<Double> yPositions = positions.get(Orientation.VERTICAL);
				int yI = yPositions.size() - 1;
				Collection<? extends TableRow<?>> rows = state.getRowsUnmodifiable().values();
				for (TableRow<?> row : rows) {
					if (!row.isVisible()) continue;
					xI = xPositions.size() - 1;
					Double yPos = yPositions.get(yI);
					double rowW = Math.max(row.size() * colW, table.getWidth());
					row.resizeRelocate(xOffset, yPos + yOffset, rowW, cellH);

					List<TableCell<?>> cells = new ArrayList<>(row.getCellsUnmodifiable().values());
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
				layoutInitialized.set(totalW > 0);
			}
		}

		@Override
		public void dispose() {
			super.dispose();
			pTable = null;
		}
	}

	/**
	 * Concrete implementation of {@link PaginatedHelper} and extension of {@link VariableTableHelper}
	 * to be used for {@link PaginatedVirtualTable} when its {@link VirtualTable#columnsLayoutModeProperty()} is
	 * set to {@link ColumnsLayoutMode#VARIABLE}.
	 */
	class VariablePaginatedTableHelper extends VariableTableHelper implements PaginatedHelper {
		protected PaginatedVirtualTable<?> pTable;

		public VariablePaginatedTableHelper(PaginatedVirtualTable<?> table) {
			super(table);
			this.pTable = table;
		}

		/**
		 * Overridden to return {@link PaginatedVirtualTable#getRowsPerPage()}.
		 */
		@Override
		public int maxRows() {
			return pTable.getRowsPerPage();
		}

		/**
		 * Overridden to return: {@code pageToPos(maxPage)}.
		 */
		@Override
		public double maxVScroll() {
			return pageToPos(pTable.getMaxPage());
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * This is given by {@code (page - 1) * rowsPerPage * cellHeight}.
		 */
		@Override
		public double pageToPos(int page) {
			return (page - 1) * pTable.getRowsPerPage() * pTable.getCellHeight();
		}

		@Override
		public void goToPage(int page) {
			super.scrollTo(pageToPos(page), Orientation.VERTICAL);
		}

		/**
		 * This is unsupported as the {@link PaginatedVirtualTable} can only scroll to certain pixel values, given
		 * by the pages.
		 *
		 * @throws UnsupportedOperationException
		 */
		@Override
		public void scrollBy(double pixels, Orientation orientation) {
			throw new UnsupportedOperationException("The paginated table cannot scroll by a given amount of pixels");
		}

		/**
		 * This is unsupported as the {@link PaginatedVirtualTable} can only scroll to certain pixel values, given
		 * by the pages.
		 *
		 * @throws UnsupportedOperationException
		 */
		@Override
		public void scrollTo(double pixel, Orientation orientation) {
			throw new UnsupportedOperationException("The paginated table cannot scroll to a given pixel position as it may be wrong");
		}

		/**
		 * This is unsupported as the {@link PaginatedVirtualTable} can only scroll to certain pixel values, given
		 * by the pages.
		 *
		 * @throws UnsupportedOperationException
		 */
		@Override
		public void scrollToRow(int index) {
			throw new UnsupportedOperationException("The paginated table cannot scroll to a given row index");
		}

		/**
		 * {@inheritDoc}
		 * <p></p>
		 * This is overridden by {@link FixedPaginatedTableHelper} because for the vertical positions we first need to check
		 * (end eventually hide) those rows that are not in the rows range.
		 * <p>
		 * The range of rows to layout may not be the same as the one carried by {@link VirtualTable#stateProperty()},
		 * so it's needed to properly compute it as:
		 * <p> Min = {@link #firstRow()}
		 * <p> Max = {@code Math.min(firstRow + maxRows - 1, table.getItems().size() - 1)}, see {@link #maxRows()}
		 * <p></p>
		 * This also means that the vertical positions will be computed only for those rows that will still be visible.
		 * <p></p>
		 * A case such this may occur when we reach a page for which there are not enough items in the table to fill it.
		 */
		@Override
		public Map<Orientation, List<Double>> computePositions(TableState<?> state, boolean forceXComputation, boolean forceYComputation) {
			IntegerRange columnsRange = state.getColumnsRange();
			double cellH = table.getCellHeight();
			AtomicBoolean anyHidden = new AtomicBoolean(false);

			// Hidden rows check
			int firstRow = firstRow();
			int lastRow = Math.min(firstRow + maxRows() - 1, table.getItems().size() - 1);
			IntegerRange rowsRange = IntegerRange.of(firstRow, lastRow);
			Map<Integer, ? extends TableRow<?>> rows = state.getRowsUnmodifiable();
			rows.entrySet().stream()
					.filter(e -> !IntegerRange.inRangeOf(e.getKey(), rowsRange))
					.peek(e -> anyHidden.set(true))
					.forEach(e -> e.getValue().setVisible(false));

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
			if (anyHidden.get() || forceYComputation || yPositions.isEmpty() || yPositions.size() != rRangeDiff + 1) {
				yPositions.clear();
				double pos = rRangeDiff * cellH;
				for (int i = firstRow; i <= lastRow; i++) {
					TableRow<?> row = rows.get(i);
					yPositions.add(pos);
					pos -= cellH;
					row.setVisible(true);
				}
			}
			return positions;
		}

		/**
		 * {@inheritDoc}
		 * <p></p>
		 * This is overridden by {@link FixedPaginatedTableHelper} to skip the layout of hidden rows, this is a must
		 * since the {@link #computePositions(TableState, boolean, boolean)} is also overridden to compute only the positions
		 * for the visible rows.
		 */
		@Override
		public void layout() {
			TableState<?> state = table.getState();
			if (state.isEmptyAll() || !table.isNeedsViewportLayout() || invalidatedPos()) {
				layoutInitialized.set(false);
				return;
			}
			Map<Orientation, List<Double>> positions = computePositions(state, false, false);

			double colH = table.getColumnSize().getHeight();
			IntegerRange columnsRange = state.getColumnsRange();
			List<Double> xPositions = positions.get(Orientation.HORIZONTAL);
			int xI = 0;

			// Columns layout
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

			// Cells layout
			if (!state.isEmpty()) {
				double cellH = table.getCellHeight();
				double yOffset = verticalOffset();
				List<Double> yPositions = positions.get(Orientation.VERTICAL);
				int yI = yPositions.size() - 1;
				Collection<? extends TableRow<?>> rows = state.getRowsUnmodifiable().values();
				for (TableRow<?> row : rows) {
					if (!row.isVisible()) continue;
					xI = 0;
					Double yPos = yPositions.get(yI);
					double rowW = Math.max(LayoutUtils.boundWidth(row) + table.getColumnSize().getWidth(), table.getWidth());
					row.resizeRelocate(0, yPos + yOffset, rowW, cellH);

					List<TableCell<?>> cells = new ArrayList<>(row.getCellsUnmodifiable().values());
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
				layoutInitialized.set(totalW > 0);
			}
		}

		@Override
		public void dispose() {
			super.dispose();
			pTable = null;
		}
	}
}
