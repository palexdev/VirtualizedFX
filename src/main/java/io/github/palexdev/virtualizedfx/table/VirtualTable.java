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
import io.github.palexdev.mfxcore.base.beans.range.NumberRange;
import io.github.palexdev.mfxcore.base.properties.PositionProperty;
import io.github.palexdev.mfxcore.base.properties.SizeProperty;
import io.github.palexdev.mfxcore.base.properties.functional.BiFunctionProperty;
import io.github.palexdev.mfxcore.base.properties.functional.SupplierProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableBooleanProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableDoubleProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableObjectProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableSizeProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableSizeProperty.SizeConverter;
import io.github.palexdev.mfxcore.observables.When;
import io.github.palexdev.mfxcore.utils.NumberUtils;
import io.github.palexdev.mfxcore.utils.fx.PropUtils;
import io.github.palexdev.mfxcore.utils.fx.StyleUtils;
import io.github.palexdev.virtualizedfx.beans.TableStateProperty;
import io.github.palexdev.virtualizedfx.cell.MappingTableCell;
import io.github.palexdev.virtualizedfx.cell.TableCell;
import io.github.palexdev.virtualizedfx.controls.VirtualScrollPane;
import io.github.palexdev.virtualizedfx.enums.ColumnsLayoutMode;
import io.github.palexdev.virtualizedfx.grid.VirtualGrid;
import io.github.palexdev.virtualizedfx.table.TableHelper.FixedTableHelper;
import io.github.palexdev.virtualizedfx.table.TableHelper.VariableTableHelper;
import io.github.palexdev.virtualizedfx.table.defaults.DefaultTableRow;
import io.github.palexdev.virtualizedfx.utils.VSPUtils;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.css.StyleablePropertyFactory;
import javafx.geometry.Orientation;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.shape.Rectangle;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Implementation of a virtual table to virtualize the display of tabular data.
 * <p>
 * The conceptual key difference from {@link VirtualGrid} is that the table is much more specific.
 * Columns and rows are not just a concept, or a way to track state, they are real nodes, each with its own "duties"
 * that will be explored later below.
 * <p></p>
 * Extends {@link Control}, has its own skin implementation {@link VirtualTableSkin} and its own cell system,
 * {@link TableCell} and {@link MappingTableCell}.
 * <p></p>
 * These are all the features:
 * <p> - The items are managed automatically (additions, removals, replacements, sorting)
 * <p> - The columns list is also managed automatically, plus there's a supplementary {@code Map} which allows
 * to get the index of a column really fast, {@link #getIndexedColumns()}
 * <p> - The rows can be customized and changed at any time through the apposite {@link #rowFactoryProperty()}
 * <p> - The control manages the height of all the cells through the {@link #cellHeightProperty()}, which is also
 * settable via CSS
 * <p> - There's also a property to specify the size of the columns, {@link #columnSizeProperty()}, which is also
 * settable via CSS, see {@link StyleableSizeProperty}
 * <p> - You can programmatically set the position of the viewport through a series of public methods
 * <p> - It is possible to retrieve the current shown/built cells as well as other information regarding the state of
 * the viewport through the {@link #stateProperty()}
 * <p> - It is possible to observe for changes to the estimated size of the viewport through the {@link #estimatedSizeProperty()}
 * <p> - It is possible to programmatically tell the viewport to update its layout with {@link #requestViewportLayout()}
 * <p>
 * Last but not least the {@link #columnsLayoutModeProperty()}. This particular property allows you to make the table
 * work in two completely different modes.
 * <p> - In the {@link ColumnsLayoutMode#FIXED} mode, columns will have a fixed size just like the cell, these
 * sizes are specified by the {@link #columnSizeProperty()}. As you can probably guess, this is the most efficient mode
 * <p> - In the {@link ColumnsLayoutMode#VARIABLE} mode, columns can have a variable width, allowing for example to resize
 * columns at runtime with the mouse, or use methods like {@link TableHelper#autosizeColumn(TableColumn)}.
 * In this mode the {@link #columnSizeProperty()} is still used though: the specified height will be fixed and equal for all
 * the columns, the width instead specifies the minimum width a column must have at any time.
 * <p></p>
 * This virtualized control also makes use of a cache. {@link TableCache} helps the table with horizontal scrolling,
 * the cells of columns that are not visible anymore in the viewport are cached. When the column becomes visible again,
 * instead of creating new cells (which is a costly operation in any case since Nodes are created), they are retrieved
 * from the cache. It is also automatically managed in case factories change or columns are added/removed/replaced.
 * <p></p>
 * <b>Note on a strange bug:</b> if for whatever reason you need to swap two columns it is highly recommended to follow
 * this example:
 * <pre>
 * {@code
 *      // Instead of this...
 *      Collections.swap(table.getColumns(), firstIndex, secondIndex);
 *
 *      // Use this...
 * 	    List<TableColumn<User, ? extends TableCell<User>>> tmp = new ArrayList<>(table.getColumns());
 * 	    Collections.swap(tmp, firstIndex, secondIndex);
 * 	    table.getColumns().setAll(tmp);
 * }
 * </pre>
 * <p></p>
 * <b>More notes on the internals...</b>
 * <p>
 * As you can see {@code VirtualTable} doesn't need/offer any info about what type of columns
 * or cells will be used. This is because of two main reasons:
 * <p> 1) This simplifies the structure, way less battles with Java generics which can be super dumb at times
 * <p> 2) Separation of roles
 * <p>
 * As stated above, {@code VirtualTable} has a specific structure because the view is organized in concrete
 * rows and columns.
 * <p>
 * Columns are dumb nodes. They do not contain the cell, but know enough about them to allow their creation,
 * each column has its own cell factory.
 * <p>
 * Rows on the other hand are more complex and useful. These are the actual containers for the cells, and are also
 * responsible for managing their state (like: which columns range is visualized, which is the index of the row, as well
 * as the needed behavior to update them)
 * <p>
 * The layout strategy is controlled by the set {@link TableHelper}, this way it can be deeply customized
 * just by implementing your own helper (before doing so it's highly recommended to check the source code of the
 * tow default ones)
 * <p></p>
 * If the table is being used with a model that doesn't make use of JavaFX properties, to signal the table that
 * items have changed you can use {@link #updateTable(boolean)}.
 *
 * @param <T> the type of objects to represent
 */
public class VirtualTable<T> extends Control implements VirtualScrollPane.Wrappable {
	//================================================================================
	// Properties
	//================================================================================
	private final String STYLE_CLASS = "virtual-table";
	private final TableManager<T> manager = new TableManager<>(this);
	private final TableCache<T> cache = new TableCache<>(this);

	private final ObjectProperty<ObservableList<T>> items = PropUtils.mappedObjectProperty(val ->
			val != null ? val : FXCollections.observableArrayList()
	);
	private final ObservableList<TableColumn<T, ? extends TableCell<T>>> columns = FXCollections.observableArrayList();
	private final BiFunctionProperty<Integer, IntegerRange, TableRow<T>> rowFactory = new BiFunctionProperty<>() {
		@Override
		protected void invalidated() {
			onRowFactoryChanged();
		}
	};

	private final PositionProperty position = new PositionProperty(Position.of(0, 0)) {
		@Override
		public void set(Position newValue) {
			if (newValue == null) {
				super.set(Position.of(0, 0));
				return;
			}

			TableHelper helper = getTableHelper();
			if (helper == null) {
				super.set(newValue);
				return;
			}

			double x = NumberUtils.clamp(newValue.getX(), 0, helper.maxHScroll());
			double y = NumberUtils.clamp(newValue.getY(), 0, helper.maxVScroll());
			newValue.setX(x);
			newValue.setY(y);
			super.set(newValue);
		}
	};

	private final ReadOnlyObjectWrapper<TableHelper> tableHelper = new ReadOnlyObjectWrapper<>() {
		@Override
		public void set(TableHelper newValue) {
			TableHelper oldValue = get();
			if (oldValue != null) oldValue.dispose();
			super.set(newValue);
		}
	};
	private final SupplierProperty<TableHelper> tableHelperSupplier = new SupplierProperty<>() {
		@Override
		protected void invalidated() {
			TableHelper helper = get().get();
			setTableHelper(helper);
		}
	};

	private final SizeProperty estimatedSize = new SizeProperty(Size.of(0, 0));
	private final ReadOnlyBooleanWrapper needsViewportLayout = new ReadOnlyBooleanWrapper(false);
	private final Map<TableColumn<T, ? extends TableCell<T>>, Integer> idxColumns = new HashMap<>();
	private boolean updateRequested = false;

	//================================================================================
	// Constructors
	//================================================================================
	public VirtualTable() {
		this(FXCollections.observableArrayList(), FXCollections.observableArrayList());
	}

	public VirtualTable(ObservableList<T> items, TableColumn<T, ? extends TableCell<T>>... columns) {
		this(items, FXCollections.observableArrayList(columns));
	}

	public VirtualTable(ObservableList<T> items, ObservableList<TableColumn<T, ? extends TableCell<T>>> columns) {
		setItems(items);
		this.columns.setAll(columns);
		initialize();
	}

	//================================================================================
	// Methods
	//================================================================================
	private void initialize() {
		getStyleClass().add(STYLE_CLASS);
		setTableHelperSupplier(() ->
				(getColumnsLayoutMode() == ColumnsLayoutMode.FIXED) ?
						new FixedTableHelper(this) :
						new VariableTableHelper(this)
		);
		setTableHelper(getTableHelperSupplier().get());
		defaultRowFactory();
		columns.addListener(this::onColumnsChanged);
	}

	/**
	 * Basically a setter for {@link #needsViewportLayoutProperty()}.
	 * <p>
	 * This sets the property to true causing the virtual table skin to catch the change through a listener
	 * which then tells the viewport to recompute the layout.
	 */
	public void requestViewportLayout() {
		setNeedsViewportLayout(true);
	}

	/**
	 * Retrieves the column at the given index, if present and not null, delegates to
	 * {@link #autosizeColumn(TableColumn)}.
	 */
	public void autosizeColumn(int index) {
		try {
			TableColumn<T, ? extends TableCell<T>> column = getColumn(index);
			if (column == null) return;
			autosizeColumn(column);
		} catch (Exception ignored) {
		}
	}

	/**
	 * Auto-sizes the given column. This can be done only if the current {@link #getTableHelper()} is not null.
	 * For this to work properly, the table should have been laid out in the scene graph at least once. If the skin is
	 * still not available, the action is delayed until the skin is created.
	 */
	public void autosizeColumn(TableColumn<T, ? extends TableCell<T>> column) {
		TableHelper helper = getTableHelper();
		if (helper == null) return;
		When.onChanged(skinProperty())
			.condition((o, n) -> n != null && getWidth() > 0)
			.then((o, n) -> helper.autosizeColumn(column))
			.invalidating(widthProperty())
			.oneShot(true)
			.executeNow(() -> getSkin() != null && getWidth() > 0)
			.listen();
	}

	/**
	 * Auto-sizes all the table's columns. This can be done only if the current {@link #getTableHelper()} is not null.
	 * For this to work properly, the table should have been laid out in the scene graph at least once. If the skin is
	 * still not available, the action is delayed until the skin is created.
	 */
	public void autosizeColumns() {
		TableHelper helper = getTableHelper();
		if (helper == null) return;
		When.onChanged(skinProperty())
			.condition((o, n) -> n != null && getWidth() > 0)
			.then((o, n) -> helper.autosizeColumns())
			.invalidating(widthProperty())
			.oneShot(true)
			.executeNow(() -> getSkin() != null && getWidth() > 0)
			.listen();
	}

	/**
	 * Sets the {@link #rowFactoryProperty()} to a default function which produces {@link DefaultTableRow}s.
	 */
	public void defaultRowFactory() {
		setRowFactory((index, columns) -> new DefaultTableRow<>(this, index, columns));
	}

	/**
	 * This method is called every time the {@link #rowFactoryProperty()} changes and is responsible for
	 * updating the rContainer appropriately.
	 * <p>
	 * Adopts a permissive approach, meaning that if the new factory is null, the columns will remain
	 * and only the rows will be removed.
	 * <p>
	 * If the new factory is not null than the viewport is reset.
	 */
	protected void onRowFactoryChanged() {
		BiFunction<Integer, IntegerRange, TableRow<T>> factory = getRowFactory();
		if (factory == null) {
			TableState<T> state = getState();
			state.clear();
			TableState<T> newState = new TableState<>(this, IntegerRange.of(-1), state.getColumnsRange());
			newState.rowsChanged();
			((TableStateProperty<T>) stateProperty()).set(newState);
			return;
		}
		manager.reset();
	}

	/**
	 * This method is called every time the {@link #cellHeightProperty()} changes, and is responsible
	 * for updating the viewport. Different implementations may take different approaches as how to
	 * update it.
	 */
	protected void onCellHeightChanged() {
		TableHelper helper = getTableHelper();
		helper.computeEstimatedSize();

		if (getWidth() != 0 && getHeight() != 0.0) {
			if (!manager.init()) {
				requestViewportLayout();
			} else {
				setPosition(0, 0);
			}
		}
	}

	/**
	 * This method is called every time the {@link #columnSizeProperty()} changes, and is responsible
	 * for resetting the viewport with {@link TableManager#reset()}
	 * <p></p>
	 * The default behavior uses a "better safe than sorry" strategy by resetting the viewport rather than
	 * trying to transition to a valid state. Different implementations may take different approaches as how to do this.
	 */
	protected void onColumnSizeChanged() {
		TableHelper helper = getTableHelper();
		helper.computeEstimatedSize();
		helper.computePositions(getState(), true, false);
		manager.reset();
	}

	/**
	 * This method is called every time the columns list changed and is responsible for building or updating the
	 * {@link #getIndexedColumns()} map when needed, as well as resetting the viewport if the control's skin
	 * is not null.
	 */
	protected void onColumnsChanged(ListChangeListener.Change<? extends TableColumn<T, ? extends TableCell<T>>> c) {
		boolean rebuildMap = false;

		while (c.next()) {
			if (c.wasPermutated()) {
				rebuildMap = true;
			}

			if (c.wasRemoved()) {
				c.getRemoved().forEach(idxColumns::remove);
				rebuildMap = true;
			}

			if (c.wasAdded()) {
				List<? extends TableColumn<T, ? extends TableCell<T>>> added = c.getAddedSubList();
				int from = c.getFrom();
				int to = from + added.size();
				int j = 0;
				for (int i = from; i < to; i++) {
					TableColumn<T, ? extends TableCell<T>> column = added.get(j);
					idxColumns.put(column, i);
					j++;
				}
			}
		}

		if (rebuildMap) {
			for (int i = 0; i < columns.size(); i++) {
				TableColumn<T, ? extends TableCell<T>> column = columns.get(i);
				idxColumns.put(column, i);
			}
		}

		Skin<?> skin = getSkin();
		if (skin != null) manager.reset();
	}

	/**
	 * Columns that change their cell factory at runtime should call this method to transition to a
	 * new valid state.
	 *
	 * @see TableManager#onColumnChangedFactory(TableColumn)
	 * @see TableState#columnChangedFactory(TableColumn)
	 */
	public void onColumnChangedFactory(TableColumn<T, ? extends TableCell<T>> column) {
		manager.onColumnChangedFactory(column);
	}

	/**
	 * This can be used to forcefully update all the currently visualized cells in the table by calling
	 * {@link TableCell#updateItem(Object)}.
	 * <p></p>
	 * Optionally with the "reset" flag set to true, the table's viewport can also be reset, {@link TableManager#reset()},
	 * this will work only if the table has already been laid out at least once and its skin is not null.
	 */
	public void updateTable(boolean reset) {
		try {
			updateRequested = true;
			if (reset) {
				if (getSkin() == null) return;
				manager.reset();
			} else {
				TableState<T> state = getState();
				if (state.isEmpty()) return;
				state.getRowsUnmodifiable().values().forEach(TableRow::updateItem);
			}
		} finally {
			updateRequested = false;
		}
	}

	//================================================================================
	// Delegate Methods
	//================================================================================
	public Size getEstimatedSize() {
		return estimatedSize.get();
	}

	/**
	 * Specifies the total virtual size of the viewport as a {@link Size} object.
	 */
	public ReadOnlyObjectProperty<Size> estimatedSizeProperty() {
		return estimatedSize;
	}

	public TableState<T> getState() {
		return manager.getState();
	}

	/**
	 * Carries the {@link TableState} object which represents the current state of the viewport.
	 * This property is useful to catch any change happening in the viewport, and carries valuable information.
	 */
	public ReadOnlyObjectProperty<TableState<T>> stateProperty() {
		return manager.stateProperty();
	}

	public NumberRange<Integer> getLastRowsRange() {
		return manager.getLastRowsRange();
	}

	/**
	 * Specifies the last range of displayed rows by the viewport as an {@link IntegerRange}.
	 */
	public ReadOnlyObjectProperty<NumberRange<Integer>> lastRowsRangeProperty() {
		return manager.lastRowsRangeProperty();
	}

	public NumberRange<Integer> getLastColumnsRange() {
		return manager.getLastColumnsRange();
	}

	/**
	 * Specifies the last range of displayed columns by the viewport as an {@link IntegerRange}.
	 */
	public ReadOnlyObjectProperty<NumberRange<Integer>> lastColumnsRangeProperty() {
		return manager.lastColumnsRangeProperty();
	}

	/**
	 * Specifies whether a change is being processed by {@link TableManager#onChange(ListChangeListener.Change)}.
	 */
	public boolean isProcessingChange() {
		return manager.isProcessingChange();
	}

	/**
	 * Scrolls to the first row.
	 */
	public void scrollToFirstRow() {
		scrollToRow(0);
	}

	/**
	 * Scrolls to the last row.
	 */
	public void scrollToLastRow() {
		scrollToRow(getItems().size() - 1);
	}

	/**
	 * Scrolls to the given row index.
	 */
	public void scrollToRow(int index) {
		getTableHelper().scrollToRow(index);
	}

	/**
	 * Scrolls to the first columns.
	 */
	public void scrollToFirstColumn() {
		scrollToColumn(0);
	}

	/**
	 * Scrolls to the last column.
	 */
	public void scrollToLastColumn() {
		scrollToColumn(getColumns().size() - 1);
	}

	/**
	 * Scrolls to the given column index.
	 */
	public void scrollToColumn(int index) {
		getTableHelper().scrollToColumn(index);
	}

	/**
	 * Scrolls by the given amount of pixels in the given direction.
	 *
	 * @param orientation the scroll direction
	 */
	public void scrollBy(double pixels, Orientation orientation) {
		getTableHelper().scrollBy(pixels, orientation);
	}

	/**
	 * Scrolls to the given pixel value in the given direction.
	 *
	 * @param orientation the scroll direction
	 */
	public void scrollTo(double pixel, Orientation orientation) {
		getTableHelper().scrollTo(pixel, orientation);
	}

	//================================================================================
	// Overridden Methods
	//================================================================================
	@Override
	protected Skin<?> createDefaultSkin() {
		return new VirtualTableSkin<>(this);
	}

	@Override
	protected List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
		return getClassCssMetaData();
	}

	@Override
	public VirtualScrollPane wrap() {
		return VSPUtils.wrap(this);
	}

	//================================================================================
	// Styleable Properties
	//================================================================================
	private final StyleableDoubleProperty cellHeight = new StyleableDoubleProperty(
			StyleableProperties.CELL_HEIGHT,
			this,
			"cellHeight",
			32.0
	) {
		@Override
		protected void invalidated() {
			onCellHeightChanged();
		}
	};

	private final StyleableSizeProperty columnSize = new StyleableSizeProperty(
			StyleableProperties.COLUMN_SIZE,
			this,
			"columnSize",
			Size.of(100.0, 32.0)
	) {
		@Override
		protected void invalidated() {
			onColumnSizeChanged();
		}
	};

	private final StyleableObjectProperty<ColumnsLayoutMode> columnsLayoutMode = new StyleableObjectProperty<>(
			StyleableProperties.COLUMNS_LAYOUT_MODE,
			this,
			"columnsLayoutMode",
			ColumnsLayoutMode.FIXED
	) {
		@Override
		protected void invalidated() {
			TableHelper helper = getTableHelperSupplier().get();
			setTableHelper(helper);
			manager.reset();
		}
	};

	private final StyleableDoubleProperty clipBorderRadius = new StyleableDoubleProperty(
			StyleableProperties.CLIP_BORDER_RADIUS,
			this,
			"clipBorderRadius",
			0.0
	);

	private final StyleableBooleanProperty enableColumnsCache = new StyleableBooleanProperty(
			StyleableProperties.ENABLE_COLUMNS_CACHE,
			this,
			"enableColumnsCache",
			true
	) {
		@Override
		protected void invalidated() {
			boolean val = get();
			if (!val) cache.clear();
		}
	};

	public double getCellHeight() {
		return cellHeight.get();
	}

	/**
	 * Specifies the fixed height for the rows/cells.
	 * <p>
	 * It is also possible to set this property via CSS with the {@code "-fx-cell-height"} property.
	 */
	public StyleableDoubleProperty cellHeightProperty() {
		return cellHeight;
	}

	public void setCellHeight(double cellHeight) {
		this.cellHeight.set(cellHeight);
	}

	public Size getColumnSize() {
		return columnSize.get();
	}

	/**
	 * Specifies the size of the columns as a {@link Size}.
	 * <p>
	 * It is also possible to set this property via CSS with the {@code "-fx-column-size"} property,
	 * check {@link StyleableSizeProperty} and {@link SizeConverter} for more info.
	 * <p></p>
	 * This size is used differently according to the se {@link #columnsLayoutModeProperty()}:
	 * <p> - In {@link ColumnsLayoutMode#FIXED} mode the specified width is fixed for all the columns
	 * (except the last one if more space is available in the table's viewport)
	 * <p> - In {@link ColumnsLayoutMode#VARIABLE} mode the specified width is used as the minimum size
	 * all columns must have at the start
	 */
	public StyleableSizeProperty columnSizeProperty() {
		return columnSize;
	}

	public void setColumnSize(Size columnSize) {
		this.columnSize.set(columnSize);
	}

	public ColumnsLayoutMode getColumnsLayoutMode() {
		return columnsLayoutMode.get();
	}

	/**
	 * Specifies how columns are laid out and managed by the table.
	 *
	 * @see VirtualTable
	 * @see #columnSizeProperty()
	 * @see ColumnsLayoutMode
	 */
	public StyleableObjectProperty<ColumnsLayoutMode> columnsLayoutModeProperty() {
		return columnsLayoutMode;
	}

	public void setColumnsLayoutMode(ColumnsLayoutMode columnsLayoutMode) {
		this.columnsLayoutMode.set(columnsLayoutMode);
	}

	public double getClipBorderRadius() {
		return clipBorderRadius.get();
	}

	/**
	 * Used by the viewport's clip to set its border radius.
	 * This is useful when you want to make a rounded virtual table, this
	 * prevents the content from going outside the view.
	 * <p></p>
	 * This is mostly useful if not using the table with {@link VirtualScrollPane}, this is the same as
	 * {@link VirtualScrollPane#clipBorderRadiusProperty()}.
	 * <p></p>
	 * <b>Side note:</b> the clip is a {@link Rectangle}, now for some
	 * fucking reason the rectangle's arcWidth and arcHeight values used to make
	 * it round do not act like the border-radius or background-radius properties,
	 * instead their value is usually 2 / 2.5 times the latter.
	 * So for a border radius of 5 you want this value to be at least 10/13.
	 * <p>
	 * It is also possible to set this property via CSS with the {@code "-fx-clip-border-radius"} property.
	 */
	public StyleableDoubleProperty clipBorderRadiusProperty() {
		return clipBorderRadius;
	}

	public void setClipBorderRadius(double clipBorderRadius) {
		this.clipBorderRadius.set(clipBorderRadius);
	}

	public boolean isColumnsCacheEnabled() {
		return enableColumnsCache.get();
	}

	/**
	 * Specifies whether to enable or not the cache mechanism for the columns.
	 * <p></p>
	 * This is also settable via CSS with the "-fx-enable-columns-cache" property, by default
	 * it is enabled.
	 *
	 * @see VirtualTable
	 * @see TableCache
	 */
	public StyleableBooleanProperty enableColumnsCacheProperty() {
		return enableColumnsCache;
	}

	public void setEnableColumnsCache(boolean enableColumnsCache) {
		this.enableColumnsCache.set(enableColumnsCache);
	}

	//================================================================================
	// CssMetaData
	//================================================================================
	private static class StyleableProperties {
		private static final StyleablePropertyFactory<VirtualTable<?>> FACTORY = new StyleablePropertyFactory<>(Control.getClassCssMetaData());
		private static final List<CssMetaData<? extends Styleable, ?>> cssMetaDataList;

		private static final CssMetaData<VirtualTable<?>, Number> CELL_HEIGHT =
				FACTORY.createSizeCssMetaData(
						"-fx-cell-height",
						VirtualTable::cellHeightProperty,
						32.0
				);

		private static final CssMetaData<VirtualTable<?>, Size> COLUMN_SIZE =
				new CssMetaData<>("-fx-column-size", SizeConverter.getInstance(), Size.of(100.0, 32.0)) {
					@Override
					public boolean isSettable(VirtualTable<?> styleable) {
						return !styleable.columnSizeProperty().isBound();
					}

					@Override
					public StyleableProperty<Size> getStyleableProperty(VirtualTable<?> styleable) {
						return styleable.columnSizeProperty();
					}
				};

		private static final CssMetaData<VirtualTable<?>, ColumnsLayoutMode> COLUMNS_LAYOUT_MODE =
				FACTORY.createEnumCssMetaData(
						ColumnsLayoutMode.class,
						"-fx-columns-layout-mode",
						VirtualTable::columnsLayoutModeProperty,
						ColumnsLayoutMode.FIXED
				);

		private static final CssMetaData<VirtualTable<?>, Number> CLIP_BORDER_RADIUS =
				FACTORY.createSizeCssMetaData(
						"-fx-clip-border-radius",
						VirtualTable::clipBorderRadiusProperty,
						0.0
				);

		private static final CssMetaData<VirtualTable<?>, Boolean> ENABLE_COLUMNS_CACHE =
				FACTORY.createBooleanCssMetaData(
						"-fx-enable-columns-cache",
						VirtualTable::enableColumnsCacheProperty,
						true
				);

		static {
			cssMetaDataList = StyleUtils.cssMetaDataList(
					Control.getClassCssMetaData(),
					CELL_HEIGHT, COLUMN_SIZE, COLUMNS_LAYOUT_MODE, CLIP_BORDER_RADIUS,
					ENABLE_COLUMNS_CACHE
			);
		}
	}

	public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
		return StyleableProperties.cssMetaDataList;
	}

	//================================================================================
	// Getters/Setters
	//================================================================================

	/**
	 * @return the {@link TableManager} instance for this {@code VirtualTable}
	 */
	protected TableManager<T> getViewportManager() {
		return manager;
	}

	/**
	 * @return the {@link TableCache} instance of this {@code VirtualTable}
	 */
	public TableCache<T> getTableCache() {
		return cache;
	}

	public ObservableList<T> getItems() {
		return items.get();
	}

	/**
	 * Specifies the items list.
	 */
	public ObjectProperty<ObservableList<T>> itemsProperty() {
		return items;
	}

	public void setItems(ObservableList<T> items) {
		this.items.set(items);
	}

	/**
	 * @return the column at the given index in the {@link #getColumns()} list.
	 * Note that this is an "exception safe" approach, if the index is invalid this will return
	 * null
	 */
	public TableColumn<T, ? extends TableCell<T>> getColumn(int index) {
		try {
			return columns.get(index);
		} catch (Exception ex) {
			return null;
		}
	}

	/**
	 * Returns the index at which the given column resides in the {@link #getColumns()} list.
	 * Since performance is critical for a virtualized control, this first attempts at getting the index
	 * from the {@link #getIndexedColumns()} map, if it is not found then proceeds with {@link List#indexOf(Object)}.
	 */
	public int getColumnIndex(TableColumn<T, ? extends TableCell<T>> column) {
		return Optional.ofNullable(idxColumns.get(column))
				.orElseGet(() -> columns.indexOf(column));
	}

	/**
	 * @return the list containing the columns
	 */
	public ObservableList<TableColumn<T, ? extends TableCell<T>>> getColumns() {
		return columns;
	}

	/**
	 * This map is built by iterating on the columns list and mapping them to the index
	 * at which they are found. Can also be considered a sort of cache to avoid {@link List#indexOf(Object)}
	 * operations which can be too slow in some cases.
	 */
	public Map<TableColumn<T, ? extends TableCell<T>>, Integer> getIndexedColumns() {
		return Collections.unmodifiableMap(idxColumns);
	}

	public BiFunction<Integer, IntegerRange, TableRow<T>> getRowFactory() {
		return rowFactory.get();
	}

	/**
	 * Specifies the function used by {@link TableState} to produce new {@link TableRow}s.
	 */
	public BiFunctionProperty<Integer, IntegerRange, TableRow<T>> rowFactoryProperty() {
		return rowFactory;
	}

	public void setRowFactory(BiFunction<Integer, IntegerRange, TableRow<T>> rowFactory) {
		this.rowFactory.set(rowFactory);
	}

	/**
	 * Shortcut for {@link #setPosition(double, double)}, which uses the current hPos as the x value for the new
	 * {@link Position} object.
	 */
	public void setVPos(double vPos) {
		setPosition(getHPos(), vPos);
	}

	/**
	 * @return the vertical position of the viewport
	 */
	public double getVPos() {
		return getPosition().getY();
	}

	/**
	 * Shortcut for {@link #setPosition(double, double)}, which uses the current vPos as the y value for the new
	 * {@link Position} object.
	 */
	public void setHPos(double hPos) {
		setPosition(hPos, getVPos());
	}

	/**
	 * @return the horizontal position of the viewport
	 */
	public double getHPos() {
		return getPosition().getX();
	}

	public Position getPosition() {
		return position.get();
	}

	/**
	 * Specifies the current position of the viewport as a {@link Position} object which has
	 * both the x and y positions.
	 */
	public PositionProperty positionProperty() {
		return position;
	}

	/**
	 * Convenience method, alternative to {@link #setPosition(Position)}.
	 */
	public void setPosition(double x, double y) {
		setPosition(Position.of(x, y));
	}

	public void setPosition(Position position) {
		this.position.set(position);
	}

	public TableHelper getTableHelper() {
		return tableHelper.get();
	}

	/**
	 * The current built helper for the grid, see {@link TableHelper}.
	 */
	public ReadOnlyObjectProperty<TableHelper> tableHelperProperty() {
		return tableHelper.getReadOnlyProperty();
	}

	public void setTableHelper(TableHelper tableHelper) {
		this.tableHelper.set(tableHelper);
	}

	public Supplier<TableHelper> getTableHelperSupplier() {
		return tableHelperSupplier.get();
	}

	/**
	 * To allow more customization on how the columns, rows and cells are laid out, the virtual table allows you
	 * to specify a supplier used to build a {@link TableHelper} which is responsible for some core actions
	 * about layout. This way you could implement your own helper and set it through this factory since the
	 * {@link #tableHelperProperty()} is intended to be read-only.
	 */
	public SupplierProperty<TableHelper> tableHelperSupplierProperty() {
		return tableHelperSupplier;
	}

	public void setTableHelperSupplier(Supplier<TableHelper> tableHelperSupplier) {
		this.tableHelperSupplier.set(tableHelperSupplier);
	}

	public boolean isNeedsViewportLayout() {
		return needsViewportLayout.get();
	}

	/**
	 * Specifies whether the viewport needs to compute the layout of its content.
	 * <p>
	 * Since this is read-only, layout requests must be sent by using {@link #requestViewportLayout()}.
	 */
	public ReadOnlyBooleanProperty needsViewportLayoutProperty() {
		return needsViewportLayout.getReadOnlyProperty();
	}

	protected void setNeedsViewportLayout(boolean needsViewportLayout) {
		this.needsViewportLayout.set(needsViewportLayout);
	}

	/**
	 * @return whether {@link #updateTable(boolean)} was invoked
	 */
	public boolean isUpdateRequested() {
		return updateRequested;
	}

	/**
	 * Sets the "updateRequested" flag to true. This can be used by
	 * cells to detect if a forced update of the cells has being invoked for example by
	 * {@link #updateTable(boolean)}.
	 */
	protected void setUpdateRequested(boolean updateRequested) {
		this.updateRequested = updateRequested;
	}
}
