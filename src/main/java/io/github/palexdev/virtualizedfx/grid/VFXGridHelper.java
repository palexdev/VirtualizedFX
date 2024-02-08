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

public interface VFXGridHelper<T, C extends Cell<T>> {

	int maxColumns();

	int firstColumn();

	int lastColumn();

	int visibleColumns();

	int totalColumns();

	ReadOnlyObjectProperty<NumberRange<Integer>> columnsRangeProperty();

	default IntegerRange columnsRange() {
		return (IntegerRange) columnsRangeProperty().get();
	}

	int maxRows();

	int firstRow();

	int lastRow();

	int visibleRows();

	int totalRows();

	default int visibleCells() {
		int nColumns = visibleColumns();
		int nRows = visibleRows();
		return nColumns * nRows;
	}

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

	ReadOnlyObjectProperty<NumberRange<Integer>> rowsRangeProperty();

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
	 * Specifies the total number of pixels alongside the x-axis.
	 */
	ReadOnlyDoubleProperty virtualMaxXProperty();

	/**
	 * @return the total number of pixels alongside the x-axis.
	 */
	default double getVirtualMaxX() {
		return virtualMaxXProperty().get();
	}

	/**
	 * Specifies the total number of pixels alongside the y-axis.
	 */
	ReadOnlyDoubleProperty virtualMaxYProperty();

	/**
	 * @return the total number of pixels alongside the y-axis.
	 */
	default double getVirtualMaxY() {
		return virtualMaxYProperty().get();
	}

	ReadOnlyObjectProperty<Position> viewportPositionProperty();

	default Position getViewportPosition() {
		return viewportPositionProperty().get();
	}

	void layout(int rowIndex, int cIndex, Node node);

	Size getTotalCellSize();

	/**
	 * @return the {@link VFXGrid} instance associated to this helper
	 */
	VFXGrid<T, C> getGrid();

	default void scrollToRow(int row) {
		double h = getTotalCellSize().getHeight();
		getGrid().setVPos((row * h));
	}

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

	default void dispose() {}

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
					int end = Math.min(maxColumns() - 1, start + totalColumns() - 1);
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
					if (grid.getHeight() == 0) return Utils.INVALID_RANGE;
					int needed = totalRows();
					if (needed == 0) return Utils.INVALID_RANGE;

					int start = Math.max(0, firstRow() - grid.getBufferSize().val());
					int end = Math.min(maxRows() - 1, start + totalRows() - 1);
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

		@Override
		public int maxColumns() {
			return Math.min(grid.size(), grid.getColumnsNum());
		}

		@Override
		public int firstColumn() {
			return NumberUtils.clamp(
				(int) Math.floor(grid.getHPos() / getTotalCellSize().getWidth()),
				0,
				maxColumns()
			);
		}

		@Override
		public int lastColumn() {
			return columnsRange().getMax();
		}

		@Override
		public int visibleColumns() {
			return (int) Math.ceil(grid.getWidth() / getTotalCellSize().getWidth());
		}

		@Override
		public int totalColumns() {
			int visible = visibleColumns();
			return visible == 0 ? 0 : Math.min(visible + grid.getBufferSize().val() * 2, maxColumns());
		}

		@Override
		public ReadOnlyObjectProperty<NumberRange<Integer>> columnsRangeProperty() {
			return columnsRange.getReadOnlyProperty();
		}

		@Override
		public int maxRows() {
			return NumberUtils.clamp(
				(int) Math.ceil((double) grid.size() / maxColumns()),
				0,
				grid.size()
			);
		}

		@Override
		public int firstRow() {
			return NumberUtils.clamp(
				(int) Math.floor(grid.getVPos() / getTotalCellSize().getHeight()),
				0,
				maxRows() - 1
			);
		}

		@Override
		public int lastRow() {
			return rowsRange().getMax();
		}

		@Override
		public int visibleRows() {
			return (int) Math.ceil(grid.getHeight() / getTotalCellSize().getHeight());
		}

		@Override
		public int totalRows() {
			int visible = visibleRows();
			return visible == 0 ? 0 : Math.min(visible + grid.getBufferSize().val() * 2, maxRows());
		}

		@Override
		public ReadOnlyObjectProperty<NumberRange<Integer>> rowsRangeProperty() {
			return rowsRange;
		}

		@Override
		public double maxHScroll() {
			return Math.max(0, getVirtualMaxX() - grid.getWidth());
		}

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

		@Override
		public void layout(int rowIndex, int columnIndex, Node node) {
			double x = getTotalCellSize().getWidth() * columnIndex;
			double y = getTotalCellSize().getHeight() * rowIndex;
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
