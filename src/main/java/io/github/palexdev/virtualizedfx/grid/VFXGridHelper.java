package io.github.palexdev.virtualizedfx.grid;

import io.github.palexdev.mfxcore.base.beans.Position;
import io.github.palexdev.mfxcore.base.beans.Size;
import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.base.beans.range.NumberRange;
import io.github.palexdev.mfxcore.base.properties.PositionProperty;
import io.github.palexdev.mfxcore.base.properties.SizeProperty;
import io.github.palexdev.mfxcore.base.properties.range.IntegerRangeProperty;
import io.github.palexdev.mfxcore.builders.bindings.DoubleBindingBuilder;
import io.github.palexdev.mfxcore.builders.bindings.ObjectBindingBuilder;
import io.github.palexdev.mfxcore.utils.GridUtils;
import io.github.palexdev.mfxcore.utils.NumberUtils;
import io.github.palexdev.virtualizedfx.cells.Cell;
import io.github.palexdev.virtualizedfx.list.VFXList;
import io.github.palexdev.virtualizedfx.utils.Utils;
import io.github.palexdev.virtualizedfx.utils.VFXCellsCache;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.scene.Node;

import java.util.Optional;

/**
 * This interface is a utility API for {@link VFXGrid}, despite computations not depending on other properties
 * (some VFXList values depend on the orientation), it's still a nice way to adhere to the encapsulation and separation of
 * concerns principles. Has one concrete implementation: {@link DefaultHelper}.
 */
public interface VFXGridHelper<T, C extends Cell<T>> {

	/**
	 * @return the maximum number of columns the grid can have. This value is essentially the same as
	 * {@link VFXGrid#columnsNumProperty()} but it's also taken into account the number of items (you can't have more
	 * columns than the number of items)
	 */
	int maxColumns();

	/**
	 * @return the index of the first visible column
	 */
	int firstColumn();

	/**
	 * @return the index of the last visible column
	 */
	int lastColumn();

	/**
	 * @return the number of columns visible in the viewport. Not necessarily the same as {@link #totalColumns()}
	 */
	int visibleColumns();

	/**
	 * @return the total number of columns in the viewport which doesn't include only the number of visible columns but also
	 * the number of buffer columns
	 */
	int totalColumns();

	/**
	 * Specifies the range of columns that should be present in the viewport. This also takes into account buffer columns,
	 * see {@link #visibleColumns()} and {@link #totalColumns()}.
	 */
	ReadOnlyObjectProperty<NumberRange<Integer>> columnsRangeProperty();

	/**
	 * @return the range of columns that should be present in the viewport. This also takes into account buffer columns,
	 * see {@link #visibleColumns()} and {@link #totalColumns()}
	 */
	default IntegerRange columnsRange() {
		return (IntegerRange) columnsRangeProperty().get();
	}

	/**
	 * @return the maximum number of rows the grid can have. This value depends on the number of items and the number of
	 * columns
	 */
	int maxRows();

	/**
	 * @return the index of the first visible row
	 */
	int firstRow();

	/**
	 * @return the index of the last visible row
	 */
	int lastRow();

	/**
	 * @return the number of rows visible in the viewport. Not necessarily the same as {@link #totalRows()}
	 */
	int visibleRows();

	/**
	 * @return the total number of rows in the viewport which doesn't include only the number of visible rows but also
	 * the number of buffer rows
	 */
	int totalRows();

	/**
	 * Specifies the range of rows that should be present in the viewport. This also takes into account buffer rows,
	 * see {@link #visibleRows()} and {@link #totalRows()}.
	 */
	ReadOnlyObjectProperty<NumberRange<Integer>> rowsRangeProperty();

	/**
	 * @return the range of rows that should be present in the viewport. This also takes into account buffer rows,
	 * see {@link #visibleRows()} and {@link #totalRows()}.
	 */
	default IntegerRange rowsRange() {
		return (IntegerRange) rowsRangeProperty().get();
	}

	/**
	 * @return the maximum amount of pixels the container can scroll on the horizontal direction
	 */
	double maxHScroll();

	/**
	 * @return the maximum amount of pixels the container can scroll on the vertical direction
	 */
	double maxVScroll();

	/**
	 * Specifies the total number of pixels on the x-axis.
	 */
	ReadOnlyDoubleProperty virtualMaxXProperty();

	/**
	 * @return the total number of pixels on the x-axis.
	 */
	default double getVirtualMaxX() {
		return virtualMaxXProperty().get();
	}

	/**
	 * Specifies the total number of pixels on the y-axis.
	 */
	ReadOnlyDoubleProperty virtualMaxYProperty();

	/**
	 * @return the total number of pixels on the y-axis.
	 */
	default double getVirtualMaxY() {
		return virtualMaxYProperty().get();
	}

	/**
	 * Cells are actually contained in a separate pane called 'viewport'. The scroll is applied on this pane.
	 * <p>
	 * This property specifies the translation of the viewport, the calculation depends on the implementation.
	 */
	ReadOnlyObjectProperty<Position> viewportPositionProperty();

	/**
	 * @return the position the viewport should be at in the container
	 */
	default Position getViewportPosition() {
		return viewportPositionProperty().get();
	}

	/**
	 * Lays out the given node
	 * The row and column indexes are necessary to identify the position of a cell compared to the others
	 * (comes before/after, above/below).
	 *
	 * @param absRowIndex    the absolute row index of the given node/cell, see {@link VFXGridSkin#layout()}
	 * @param absColumnIndex the absolute column index of the given node/cell, see {@link VFXGridSkin#layout()}
	 */
	void layout(int absRowIndex, int absColumnIndex, Node node);

	/**
	 * @return the total size of each cell, given by the {@link VFXGrid#cellSizeProperty()} summed to the horizontal and
	 * vertical spacing values
	 */
	Size getTotalCellSize();

	/**
	 * @return the {@link VFXGrid} instance associated to this helper
	 */
	VFXGrid<T, C> getGrid();

	/**
	 * @return the theoretical number of cells in the viewport. The value depends on the number of visible columns and rows,
	 * however, doesn't take into account the possibility of incomplete columns/rows. For a precise value,
	 * use {@link #totalCells()} instead
	 */
	default int visibleCells() {
		int nColumns = visibleColumns();
		int nRows = visibleRows();
		return nColumns * nRows;
	}

	/**
	 * @return the precise number of cells present in the viewport at a given time. The value depends on the current range
	 * of rows and columns. Unfortunately, it's not very efficient as the count is computed by iterating over each row and
	 * column, but it's the only stable way I found to have a correct value.
	 */
	default int totalCells() {
		// TODO can't find a better algorithm, probably with some nasty stupid math formula that I hate so much
		VFXGrid<T, C> grid = getGrid();
		int cnt = 0;
		int nColumns = maxColumns();
		IntegerRange rRange = rowsRange();
		IntegerRange cRange = columnsRange();
		int nCells = cRange.diff() + 1;
		for (Integer rIdx : rRange) {
			int linear = GridUtils.subToInd(nColumns, rIdx, cRange.getMax());
			if (linear < grid.size()) {
				cnt += nCells;
				continue;
			}
			int rowStart = GridUtils.subToInd(nColumns, rIdx, cRange.getMin());
			int max = Math.min(grid.size(), linear) - 1;
			if (max < rowStart) break;
			cnt += max - rowStart + 1;
		}
		return cnt;
	}

	/**
	 * Scrolls to the given row index by setting the {@link VFXGrid#vPosProperty()} to {@code rowIndex * totalCellHeight}.
	 */
	default void scrollToRow(int row) {
		double h = getTotalCellSize().getHeight();
		getGrid().setVPos((row * h));
	}

	/**
	 * Scrolls to the given column index by setting the {@link VFXGrid#hPosProperty()} to {@code columnIndex * totalCellWidth}.
	 */
	default void scrollToColumn(int column) {
		double w = getTotalCellSize().getWidth();
		getGrid().setHPos((column * w));
	}

	/**
	 * Forces the {@link VFXGrid#vPosProperty()} and {@link VFXGrid#hPosProperty()} to be invalidated.
	 * This is simply done by calling the respective setters with their current respective values. Those two properties
	 * will automatically call {@link #maxVScroll()} and {@link #maxHScroll()} to ensure the values are correct. This is
	 * automatically invoked by the {@link VFXGridManager} when needed.
	 */
	default void invalidatePos() {
		VFXGrid<T, C> grid = getGrid();
		grid.setVPos(grid.getVPos());
		grid.setHPos(grid.getHPos());
	}

	/**
	 * Converts the given index to an item (shortcut for {@code getList().getItems().get(index)}).
	 */
	default T indexToItem(int index) {
		return getGrid().getItems().get(index);
	}

	/**
	 * Converts the given index to a cell. Uses {@link #itemToCell(Object)}.
	 */
	default C indexToCell(int index) {
		T item = indexToItem(index);
		return itemToCell(item);
	}

	/**
	 * Converts the given item to a cell. The result is either on of the cells cached in {@link VFXCellsCache} that
	 * is updated with the given item, or a totally new one created by the {@link VFXList#cellFactoryProperty()}.
	 */
	default C itemToCell(T item) {
		VFXGrid<T, C> grid = getGrid();
		VFXCellsCache<T, C> cache = grid.getCache();
		Optional<C> opt = cache.tryTake();
		opt.ifPresent(c -> c.updateItem(item));
		return opt.orElseGet(() -> grid.getCellFactory().apply(item));
	}

	/**
	 * Automatically called by {@link VFXGrid} when a helper is not needed anymore (changed).
	 * If the helper uses listeners/bindings that may lead to memory leaks, this is the right place to remove them.
	 */
	default void dispose() {}

	/**
	 * Concrete implementation of {@link VFXGridHelper}, here the range of rows and columns to display, as well as the
	 * viewport position, the virtual max x and y properties are defined as follows:
	 * <p> - the columns range is given by the {@link #firstColumn()} element minus the buffer size {@link VFXGrid#bufferSizeProperty()},
	 * (cannot be negative) and the sum between this start index and the total number of needed columns given by {@link #totalColumns()}
	 * (cannot exceed the maximum number of columns {@link #maxColumns()}). It may happen that the number of indexes given
	 * by the range {@code end - start + 1} is lesser than the total number of columns we need. In such cases, the range
	 * start is corrected to be {@code end - needed + 1}. A typical situation for this is when the grid's horizontal position
	 * reaches the max scroll.
	 * The range computation has the following dependencies: the number of columns, the grid's width, horizontal position,
	 * the buffer size, the number of items, the cell size and the horizontal spacing.
	 * <p> - the rows range is given by the {@link #firstRow()} element minus the buffer size {@link VFXGrid#bufferSizeProperty()},
	 * (cannot be negative) and the sum between this start index and the total number of needed rows given by {@link #totalRows()}
	 * (cannot exceed the maximum number of rows {@link #maxRows()}). It may happen that the number of indexes given
	 * by the range {@code end - start + 1} is lesser than the total number of rows we need. In such cases, the range
	 * start is corrected to be {@code end - needed + 1}. A typical situation for this is when the grid's vertical position
	 * reaches the max scroll.
	 * The range computation has the following dependencies: the number of columns, the grid's height, vertical position,
	 * the buffer size, the number of items, the cell size and the vertical spacing.
	 * <p> - the viewport position, a computation that is at the core of virtual scrolling. The viewport, which contains the cells,
	 * is not supposed to scroll by insane numbers of pixels both for performance reasons and because it is not necessary.
	 * For both the horizontal and vertical position we use the same technique, just using the appropiate values according
	 * to the axis we are working on.
	 * First we get the range of rows/columns to display, then the total cell size given by {@link #getTotalCellSize()},
	 * yes, the spacing also affects the position. Then we compute the ranges to the first visible row/column, which
	 * are given by {@code IntegerRange.of(range.getMin(), first())}, in other words we limit the 'complete' ranges to the
	 * start buffer including the first row/column after the buffer. The number of indexes in the newfound ranges
	 * (given by {@link IntegerRange#diff()}) is multiplied by the total cell size, this way we found the number of pixels to the
	 * first visible cell, {@code pixelsToFirst}. We are missing only one last information, how much of the first row/column
	 * do we actually see? We call this amount {@code visibleAmountFirst} and it's given by {@code pos % totalCellSize}.
	 * Finally, the viewport's position is given by {@code -(pixelsToFirst + visibleAmountFirst)}
	 * (for both hPos and vPos of course).
	 * While it's true that the calculations are more complex and 'needy', it's important to node that this approach
	 * allows avoiding 'hacks' to correctly lay out the cells in the viewport. No need for special offsets at the top
	 * or bottom anymore.
	 * The viewport's position computation has the following dependencies: the horizontal position, the vertical position,
	 * the cell size and both the vertical and horizontal spacing.
	 * <p> - the virtual max x and y properties, which give the total number of pixels on the x-axis and y-axis. Virtual
	 * means that it's not the actual size of the container, rather the size it would have if it was not virtualized.
	 * The two values are given by the max number of rows/columns multiplied by the total cell size, minus the spacing
	 * (otherwise we would have the spacing applied between the last row/column and the grid's border too).
	 * The computations have the following dependencies: the number of items, the number of columns, the cell size and
	 * the horizontal/vertical spacing (respectively).
	 */
	class DefaultHelper<T, C extends Cell<T>> implements VFXGridHelper<T, C> {
		protected final VFXGrid<T, C> grid;
		protected final IntegerRangeProperty columnsRange = new IntegerRangeProperty();
		protected final IntegerRangeProperty rowsRange = new IntegerRangeProperty();
		protected final ReadOnlyDoubleWrapper virtualMaxX = new ReadOnlyDoubleWrapper();
		protected final ReadOnlyDoubleWrapper virtualMaxY = new ReadOnlyDoubleWrapper();
		protected final PositionProperty viewportPosition = new PositionProperty();
		protected final SizeProperty totalCellSize = new SizeProperty(Size.empty());

		public DefaultHelper(VFXGrid<T, C> grid) {
			this.grid = grid;

			columnsRange.bind(ObjectBindingBuilder.<IntegerRange>build()
				.setMapper(() -> {
					if (grid.getWidth() <= 0) return Utils.INVALID_RANGE;
					int needed = totalColumns();
					if (needed == 0) return Utils.INVALID_RANGE;

					int start = Math.max(0, firstColumn() - grid.getBufferSize().val());
					int end = Math.min(maxColumns() - 1, start + needed - 1);
					if (end - start + 1 < needed) start = Math.max(0, end - needed + 1);
					return IntegerRange.of(start, end);
				})
				.addSources(grid.columnsNumProperty())
				.addSources(grid.widthProperty())
				.addSources(grid.hPosProperty())
				.addSources(grid.bufferSizeProperty())
				.addSources(grid.sizeProperty(), grid.cellSizeProperty(), grid.hSpacingProperty())
				.get()
			);
			rowsRange.bind(ObjectBindingBuilder.<IntegerRange>build()
				.setMapper(() -> {
					if (grid.getHeight() <= 0) return Utils.INVALID_RANGE;
					int needed = totalRows();
					if (needed == 0) return Utils.INVALID_RANGE;

					int start = Math.max(0, firstRow() - grid.getBufferSize().val());
					int end = Math.min(maxRows() - 1, start + needed - 1);
					if (end - start + 1 < needed) start = Math.max(0, end - needed + 1);
					return IntegerRange.of(start, end);
				})
				.addSources(grid.columnsNumProperty())
				.addSources(grid.heightProperty())
				.addSources(grid.vPosProperty())
				.addSources(grid.bufferSizeProperty())
				.addSources(grid.sizeProperty(), grid.cellSizeProperty(), grid.vSpacingProperty())
				.get()
			);

			virtualMaxX.bind(DoubleBindingBuilder.build()
				.setMapper(() -> (maxColumns() * getTotalCellSize().getWidth()) - grid.getHSpacing())
				.addSources(grid.sizeProperty(), grid.columnsNumProperty(), grid.cellSizeProperty())
				.addSources(grid.hSpacingProperty())
				.get()
			);
			virtualMaxY.bind(DoubleBindingBuilder.build()
				.setMapper(() -> (maxRows() * getTotalCellSize().getHeight()) - grid.getVSpacing())
				.addSources(grid.sizeProperty(), grid.columnsNumProperty(), grid.cellSizeProperty())
				.addSources(grid.vSpacingProperty())
				.get()
			);

			viewportPosition.bind(ObjectBindingBuilder.<Position>build()
				.setMapper(() -> {
					if (grid.isEmpty()) return Position.origin();
					IntegerRange rowsRange = rowsRange();
					IntegerRange columnsRange = columnsRange();
					if (Utils.INVALID_RANGE.equals(rowsRange) || Utils.INVALID_RANGE.equals(columnsRange))
						return Position.origin();

					Size size = getTotalCellSize();
					IntegerRange rRangeToFirstVisible = IntegerRange.of(rowsRange.getMin(), firstRow());
					double rPixelsToFirst = rRangeToFirstVisible.diff() * size.getHeight();
					double rVisibleAmount = grid.getVPos() % size.getHeight();
					IntegerRange cRangeToFirstVisible = IntegerRange.of(columnsRange.getMin(), firstColumn());
					double cPixelsToFirst = cRangeToFirstVisible.diff() * size.getWidth();
					double cVisibleAmount = grid.getHPos() % size.getWidth();

					double x = -(cPixelsToFirst + cVisibleAmount);
					double y = -(rPixelsToFirst + rVisibleAmount);
					return Position.of(x, y);

				})
				.addSources(grid.vPosProperty(), grid.hPosProperty())
				.addSources(grid.cellSizeProperty())
				.addSources(grid.hSpacingProperty(), grid.vSpacingProperty())
				.get()
			);

			totalCellSize.bind(ObjectBindingBuilder.<Size>build()
				.setMapper(() -> {
					Size size = grid.getCellSize();
					return Size.of(
						size.getWidth() + grid.getHSpacing(),
						size.getHeight() + grid.getVSpacing()
					);
				})
				.addSources(grid.cellSizeProperty(), grid.vSpacingProperty(), grid.hSpacingProperty())
				.get()
			);
		}

		/**
		 * {@inheritDoc}
		 * <p></p>
		 * Given by {@code Math.min(nItems, nColumns)}.
		 */
		@Override
		public int maxColumns() {
			return Math.min(grid.size(), grid.getColumnsNum());
		}

		/**
		 * {@inheritDoc}
		 * <p></p>
		 * Given by {@code Math.floor(hPos / totalCellWidth)}, clamped between 0 and {@link #maxColumns()} - 1.
		 */
		@Override
		public int firstColumn() {
			return NumberUtils.clamp(
				(int) Math.floor(grid.getHPos() / getTotalCellSize().getWidth()),
				0,
				maxColumns() - 1
			);
		}

		/**
		 * {@inheritDoc}
		 * <p></p>
		 * Given by the max in {@link #columnsRange()}.
		 */
		@Override
		public int lastColumn() {
			return columnsRange().getMax();
		}

		/**
		 * {@inheritDoc}
		 * <p></p>
		 * Given by {@code Math.ceil(gridWidth / totalCellWidth)}.
		 */
		@Override
		public int visibleColumns() {
			return (int) Math.ceil(grid.getWidth() / getTotalCellSize().getWidth());
		}

		/**
		 * {@inheritDoc}
		 * <p></p>
		 * Given by {@link #visibleColumns()} plus double the value of {@link VFXGrid#bufferSizeProperty()} restricted to
		 * the maximum number of columns allowed, {@link #maxColumns()}.
		 */
		@Override
		public int totalColumns() {
			int visible = visibleColumns();
			return visible == 0 ? 0 : Math.min(visible + grid.getBufferSize().val() * 2, maxColumns());
		}

		@Override
		public ReadOnlyObjectProperty<NumberRange<Integer>> columnsRangeProperty() {
			return columnsRange.getReadOnlyProperty();
		}

		/**
		 * {@inheritDoc}
		 * <p></p>
		 * Given by {@code Math.ceil(itemsNum / maxColumns())}, see {@link #maxColumns()}.
		 */
		@Override
		public int maxRows() {
			return NumberUtils.clamp(
				(int) Math.ceil((double) grid.size() / maxColumns()),
				0,
				grid.size()
			);
		}

		/**
		 * {@inheritDoc}
		 * <p></p>
		 * Given by {@code Math.floor(vPos / totalCellHeight)}, clamped between 0 and {@link #maxRows()} - 1.
		 */
		@Override
		public int firstRow() {
			return NumberUtils.clamp(
				(int) Math.floor(grid.getVPos() / getTotalCellSize().getHeight()),
				0,
				maxRows() - 1
			);
		}

		/**
		 * {@inheritDoc}
		 * <p></p>
		 * Given by the max in {@link #maxRows()}.
		 */
		@Override
		public int lastRow() {
			return rowsRange().getMax();
		}

		/**
		 * {@inheritDoc}
		 * <p></p>
		 * Given by {@code Math.ceil(gridHeight / totalCellHeight)}.
		 */
		@Override
		public int visibleRows() {
			return (int) Math.ceil(grid.getHeight() / getTotalCellSize().getHeight());
		}

		/**
		 * {@inheritDoc}
		 * <p></p>
		 * Given by {@link #visibleRows()} plus double the value of {@link VFXGrid#bufferSizeProperty()} restricted to
		 * the maximum number of rows allowed, {@link #maxRows()}.
		 */
		@Override
		public int totalRows() {
			int visible = visibleRows();
			return visible == 0 ? 0 : Math.min(visible + grid.getBufferSize().val() * 2, maxRows());
		}

		@Override
		public ReadOnlyObjectProperty<NumberRange<Integer>> rowsRangeProperty() {
			return rowsRange;
		}

		/**
		 * {@inheritDoc}
		 * <p></p>
		 * Given by {@code virtualMaxX - gridWidth}, cannot be negative.
		 */
		@Override
		public double maxHScroll() {
			return Math.max(0, getVirtualMaxX() - grid.getWidth());
		}

		/**
		 * {@inheritDoc}
		 * <p></p>
		 * Given by {@code virtualMaxY - gridHeight}, cannot be negative.
		 */
		@Override
		public double maxVScroll() {
			return Math.max(0, getVirtualMaxY() - grid.getHeight());
		}

		@Override
		public ReadOnlyDoubleProperty virtualMaxXProperty() {
			return virtualMaxX.getReadOnlyProperty();
		}

		@Override
		public ReadOnlyDoubleProperty virtualMaxYProperty() {
			return virtualMaxY.getReadOnlyProperty();
		}

		@Override
		public ReadOnlyObjectProperty<Position> viewportPositionProperty() {
			return viewportPosition.getReadOnlyProperty();
		}

		/**
		 * {@inheritDoc}
		 * <p></p>
		 * The x position is given by {@code totalCellWidth * columnIndex}, the y position is given by
		 * {@code totalCellHeight * rowIndex}, the width and height are given by the {@link VFXGrid#cellSizeProperty()}.
		 */
		@Override
		public void layout(int absRowIndex, int absColumnIndex, Node node) {
			double x = getTotalCellSize().getWidth() * absColumnIndex;
			double y = getTotalCellSize().getHeight() * absRowIndex;
			double w = grid.getCellSize().getWidth();
			double h = grid.getCellSize().getHeight();
			node.resizeRelocate(x, y, w, h);
		}

		@Override
		public Size getTotalCellSize() {
			return totalCellSize.get();
		}

		@Override
		public VFXGrid<T, C> getGrid() {
			return grid;
		}
	}
}
