package io.github.palexdev.virtualizedfx.table;

import io.github.palexdev.mfxcore.base.beans.Size;
import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.base.properties.functional.FunctionProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableDoubleProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableIntegerProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableObjectProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableSizeProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableSizeProperty.SizeConverter;
import io.github.palexdev.mfxcore.controls.Control;
import io.github.palexdev.mfxcore.controls.SkinBase;
import io.github.palexdev.mfxcore.observables.When;
import io.github.palexdev.mfxcore.utils.fx.PropUtils;
import io.github.palexdev.mfxcore.utils.fx.StyleUtils;
import io.github.palexdev.virtualizedfx.base.VFXContainer;
import io.github.palexdev.virtualizedfx.base.VFXStyleable;
import io.github.palexdev.virtualizedfx.cells.TableCell;
import io.github.palexdev.virtualizedfx.enums.BufferSize;
import io.github.palexdev.virtualizedfx.enums.ColumnsLayoutMode;
import io.github.palexdev.virtualizedfx.grid.VFXGrid;
import io.github.palexdev.virtualizedfx.list.VFXList;
import io.github.palexdev.virtualizedfx.properties.VFXTableStateProperty;
import io.github.palexdev.virtualizedfx.table.VFXTableHelper.FixedTableHelper;
import io.github.palexdev.virtualizedfx.table.VFXTableHelper.VariableTableHelper;
import io.github.palexdev.virtualizedfx.table.ViewportLayoutRequest.ViewportLayoutRequestProperty;
import io.github.palexdev.virtualizedfx.table.defaults.VFXDefaultTableRow;
import io.github.palexdev.virtualizedfx.utils.VFXCellsCache;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.css.StyleablePropertyFactory;
import javafx.geometry.Orientation;
import javafx.scene.shape.Rectangle;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Implementation of a virtualized container to show a list of items as tabular data.
 * The default style class is: '.vfx-table'.
 * <p>
 * Extends {@link Control}, implements {@link VFXContainer}, has its own skin implementation {@link VFXTableSkin}
 * and behavior {@link VFXTableManager}. Uses cells of type {@link TableCell}.
 * <p>
 * This is a stateful component, meaning that every meaningful variable (position, size, cell size, etc.) will produce a new
 * {@link VFXTableState} when changing. The state determines how and which items are displayed in the container.
 * <p></p>
 * <b>Core features & Implementation Details</b>
 * <p> - This container is a bit special because it's like a combination of both {@link VFXList} and {@link VFXGrid}.
 * We are displaying data in two dimensions, just like the grid, because we have both the items and the columns.
 * However, each item occupies a row, just like in the list. Because of such nature, this component is also more complex
 * to use/setup. There are three core aspects:
 * <pre>
 *     1) The columns: to display tabular data from a single object type, we usually want to divide such objects in data fragments,
 *        where each type belongs to a specific category. This is the columns' role, and to do this, we have to introduce the second core aspect...
 *     2) The cell factory is not a property of the container anymore, rather, each column has its cell factory.
 *        This way, every column can build a cell that is going to extract the appropriate piece of data from the object type (e.g., User Object -> Name String).
 *        The default cell implementations have specific properties to do exactly this, so you don't really need a different cell type for each column;
 *        to be precise, you just need to set the appropriate function to extract the data and display it.
 *        However, note that the container does not force you to use such defaults, in theory, you could come up with a totally different strategy if you like.
 *     3) The cells are not positioned directly into the viewport, rather, they are grouped in rows. So, you'll also need to specify a row factory now.
 *        By default, {@link VFXDefaultTableRow} is used, you may want to extend such class and change the factory to implement missing features.
 *        As for the reasons, there are fundamentally two:
 *          i) Selection: suppose you have a selection model, you click on a cell, and you now want the entire row to be highlighted.
 *             I won't say it's impossible, but it's definitely not practical. You would have to manage every single cell in the row,
 *             and it also makes it harder to style in CSS.
 *         ii) The base class {@link VFXTableRow} actually specifies some abstract methods that are crucial for the system to correctly manage the cells.
 *             Such methods could, in theory, be placed elsewhere, but doing it like this makes everything cleaner and easier to handle.
 * </pre>
 * Last but not least, there is another peculiar feature worth mentioning. Virtualized containers are super efficient mainly
 * for two reasons: every cell has a fixed size, because of this, it's easy to determine how many cells we need, which items
 * to display, and thus we just create and render the needed number of nodes. The table, like the list and the grid, is no different.
 * There are a bunch of properties to do what I just described which will be discussed more in depth below. The point is,
 * usually, tables have fixed cell heights, but the width depends on the "parent" column. Also, they often offer the possibility
 * of resizing columns to fit the "children" cells' content, or even the possibility of resizing each column with the mouse.
 * In other words, to support such features, we would have no choice but to disable the virtualization on the x-axis,
 * which means a potentially huge performance hit. For this reason, and because I strive to make things as flexible as possible for
 * the sake of the users, I implemented two layout modes {@link ColumnsLayoutMode}. I'll detail how it works below, just
 * know that this is only one of the many mechanisms that regulate the columns' width.
 * <p> - The default behavior implementation, {@link VFXTableManager}, can be considered as the name suggests more like
 * a 'manager' than an actual behavior. It is responsible for reacting to core changes in the functionalities defined here
 * to produce a new state.
 * The state can be considered as a 'picture' of the container at a certain time. Each combination of the variables that
 * influence the way items are shown (how many, start, end, changes in the list, etc.) will produce a specific state.
 * Because of how the table is designed, there are actually two kinds of states here.  The first one is common to all
 * virtualized containers, and represents the overall state of the component, for the table it's the {@link VFXTableState}
 * class. The other one is specific to the table, and it's actually the {@link VFXTableRow} class itself.
 * The global state tells how many rows should be present in the viewport, which items from the list (rows range)
 * and which "data fragments" (columns range) to display; but then each row has its sub-state, they keep the cells' list,
 * the columns range, and are responsible for updating the cells when needed as well as positioning and sizing them.
 * Of course, you are free to customize pretty much all of these mechanisms, BUT, beware, VFX components are no joke,
 * they are complex, make sure to read the documentation before!
 * <p> - The items' list is managed automatically (permutations, insertions, removals, updates). Compared to previous
 * algorithms, the {@link VFXTableManager} adopts a much simpler strategy while still trying to keep the cell updates count
 * as low as possible to improve performance. See {@link VFXTableManager#onItemsChanged()}.
 * <p> - The columns' list is also managed automatically. Now it's even more convenient as it's not needed to pass
 * the table instance to the column object, rather the manager will automatically set the reference when they are
 * added/removed from the table.
 * <p> - The function used to generate the rows, called "rowFactory", can be changed anytime, even at runtime, see
 * {@link VFXTableManager#onRowFactoryChanged()}.
 * <p> - Core computations such as the range of rows, the range of columns, the estimated size, the layout of cells, etc.,
 * are delegated to a separate 'helper' class which is the {@link VFXTableHelper}. There are two concrete implementations
 * for each of the {@link ColumnsLayoutMode}. You are allowed to change the helper through the {@link #helperFactoryProperty()}.
 * <p> - The vertical and horizontal positions are available through the properties {@link #hPosProperty()} and {@link #vPosProperty()}
 * It could indeed be possible to use a single property for the position, but they are split for performance reasons.
 * <p> - The virtual bounds of the container are given by two properties:
 * <pre>
 *     1) the {@link #virtualMaxXProperty()} which specifies the total number of pixels on the x-axis
 *     2) the {@link #virtualMaxYProperty()} which specifies the total number of pixels on the y-axis
 * </pre>
 * <p> - You can access the current state through the {@link #stateProperty()}. The state gives crucial information about
 * the container such as the rows range, the columns range and the visible rows (by index and by item). If you'd like to observe
 * for changes in the displayed items, then you want to add a listener on this property. See also {@link VFXTableState}.
 * <p> - It is possible to force the viewport to update the layout by invoking {@link #requestViewportLayout()},
 * although this should never be necessary as it is automatically handled by "system".
 * <p> - The columns' size can be controlled through the {@link #columnsSizeProperty()}.
 * When using the {@link ColumnsLayoutMode#FIXED}, every column will have the width specified by the property.
 * Instead, when using the other mode {@link ColumnsLayoutMode#VARIABLE}, the value is treated as the minimum width
 * every column should have. The height is always the same for every column for obvious reasons.
 * (Make sure to also read {@link VFXTableColumn} to learn how columns' width is managed)
 * <p> The {@link #columnsLayoutModeProperty()} allows you to specify how to lay out and handle the columns.
 * <p> Just like the list and the grid, the table also makes use of buffers to render a couple more rows and columns to
 * make the scrolling smoother. There are two buffers, one for the columns {@link #columnsBufferSizeProperty()} and one
 * for the rows {@link #rowsBufferSizeProperty()}. Beware, since the {@link ColumnsLayoutMode#VARIABLE} basically disables
 * the virtualization alongside the x-axis, the columns' buffer won't be used, all columns will be rendered (although with
 * some internal optimizations).
 * <p> Also, just like the list and the grid, the table makes use of caches to store rows and cells that are not needed
 * anymore but could be used again in the future. One cache is here (table's class) and is responsible for storing rows
 * (since the row factory is also here), the other is in each {@link VFXTableColumn} and is responsible for storing cells
 * (since every column has its own cell factory). You can control the caches' capacity by the following properties:
 * {@link #rowsCacheCapacityProperty()}, {@link VFXTableColumn#cellsCacheCapacityProperty()}.
 * <pre>
 *     Note 1: the cache needs to know how to generate rows/cells for the {@link VFXCellsCache#populate()} feature to
 *             work properly. This is automatically handled by the table and columns, their factory will always be
 *             "shared" with their cache instances.
 *     Note 2: by default both capacities are set to 10 cells. However, for the table's nature, such number is likely to be
 *             too small, but it also depends from case to case. You can play around with the values and see if there's
 *             any benefit to performance.
 * </pre>
 * <p></p>
 * <b>Other features & Details</b>
 * <p> - One of the shared features between other virtualized containers is the way layout requests are handled: by having a
 * read-only property {@link #needsViewportLayoutProperty()} and a way to request it {@link #requestViewportLayout()}.
 * However, the table handles request a bit differently. While in other containers, the property is just boolean value,
 * here the request is a custom class: {@link ViewportLayoutRequest}. The reason is simple, but solves a series of
 * inconvenient issues. First, the request can carry a column object that can be used by layout methods to optimize the process,
 * thus computing only a portion of the layout, this mainly useful when using the {@link ColumnsLayoutMode#VARIABLE} mode.
 * However, this is optional, meaning that if the column instance is {@code null}, then a full layout must be issued.
 * Second, requests can also act as callbacks, through a boolean property one can know if the layout was actually computed
 * or not. This is crucial to make the autosize feature work (see below).
 * <p> - The table allows you to autosize all or specific columns so that the content is fully shown. You can do so by
 * calling either: {@link #autosizeColumn(int)}, {@link #autosizeColumn(VFXTableColumn)} or {@link #autosizeColumns()}.
 * Their behavior depends on the set {@link ColumnsLayoutMode}.
 * In {@link ColumnsLayoutMode#VARIABLE} mode, columns will be resized to make their header and all their "children" cells fit the content.
 * In {@link ColumnsLayoutMode#FIXED} mode, since columns can't have different size, the algorithm chooses the greatest
 * needed width among all the columns and then sets the {@link #columnsSizeProperty()}. // TODO implement
 * Of course, the width computation is done on the currently shown items, meaning that if you scroll and there are now
 * items that are even bigger than the current set width, then you'll have to autosize again.
 * <p> - Columns' indexes. Since columns are stored in a list, there is not a fast way to retrieve
 * the index of a column from the instance itself, {@link List#indexOf(Object)} is too slow in the context of a virtualized
 * container. So, the system tries to avoid as much as possible to use columns' indexes, BUT implements a mechanism to
 * make it much, much faster. Every {@link VFXTableColumn} has a read-only property to store its index: {@link VFXTableColumn#indexProperty()}.
 * The system automatically updates the property at layout time (see {@link VFXTableSkin#updateColumnIndex(VFXTableColumn, int)}),
 * and offers a method {@link #indexOf(VFXTableColumn)} to retrieve it (it's more than just a getter!).
 * <p> - <b>Columns re-ordering/swapping</b>. Since table's columns are nodes which are part of the viewport, adding duplicates
 * to the list will generate a JavaFX exception. For this reason, any time you want to make some changes to the columns'
 * list, that may involve having duplicates in it, it's recommended to use a temporary new list and then use 'setAll'.
 * {@link VFXTableColumn} offers a utility method to swap to columns, so please use {@link VFXTableColumn#swapColumns(VFXTable, int, int)}
 * or {@link VFXTableColumn#swapColumns(ObservableList, int, int)} instead of {@link Collections#swap(List, int, int)}.
 * <p> - // TODO implement manual update for all containers
 *
 * @param <T> the type of items in the table
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class VFXTable<T> extends Control<VFXTableManager<T>> implements VFXContainer<T>, VFXStyleable {
	//================================================================================
	// Properties
	//================================================================================
	private final VFXCellsCache<T, VFXTableRow<T>> cache;
	private final ListProperty<T> items = new SimpleListProperty<>(FXCollections.observableArrayList()) {
		@Override
		public void set(ObservableList<T> newValue) {
			if (newValue == null) newValue = FXCollections.observableArrayList();
			super.set(newValue);
		}
	};
	private final FunctionProperty<T, VFXTableRow<T>> rowFactory = new FunctionProperty<>() {
		@Override
		public void set(Function<T, VFXTableRow<T>> newValue) {
			if (newValue != null) {
				newValue = newValue.andThen(r -> {
					r.setTable(VFXTable.this);
					return r;
				});
			}
			super.set(newValue);
		}

		@Override
		protected void invalidated() {
			cache.setCellFactory(get());
		}
	};
	private final ObservableList<VFXTableColumn<T, ? extends TableCell<T>>> columns = FXCollections.observableArrayList();
	private final ReadOnlyObjectWrapper<VFXTableHelper<T>> helper = new ReadOnlyObjectWrapper<>() {
		@Override
		public void set(VFXTableHelper<T> newValue) {
			if (newValue == null)
				throw new NullPointerException("Table helper cannot be null!");
			VFXTableHelper<T> oldValue = get();
			if (oldValue != null) oldValue.dispose();
			super.set(newValue);
		}
	};
	private final FunctionProperty<ColumnsLayoutMode, VFXTableHelper<T>> helperFactory = new FunctionProperty<>(defaultHelperFactory()) {
		@Override
		public void set(Function<ColumnsLayoutMode, VFXTableHelper<T>> newValue) {
			if (newValue == null)
				throw new NullPointerException("Helper helper factory cannot be null!");
			super.set(newValue);
		}

		@Override
		protected void invalidated() {
			VFXTableHelper<T> helper = get().apply(getColumnsLayoutMode());
			setHelper(helper);
		}
	};
	private final DoubleProperty vPos = PropUtils.clampedDoubleProperty(
		() -> 0.0,
		() -> getHelper().maxVScroll()
	);
	private final DoubleProperty hPos = PropUtils.clampedDoubleProperty(
		() -> 0.0,
		() -> getHelper().maxHScroll()
	);
	private final VFXTableStateProperty<T> state = new VFXTableStateProperty<>(VFXTableState.EMPTY);
	private final ViewportLayoutRequestProperty<T> needsViewportLayout = new ViewportLayoutRequestProperty<>();

	//================================================================================
	// Constructors
	//================================================================================
	public VFXTable() {
		cache = createCache();
		initialize();
	}

	public VFXTable(ObservableList<T> items) {
		cache = createCache();
		setItems(items);
		initialize();
	}

	public VFXTable(ObservableList<T> items, Collection<VFXTableColumn<T, ? extends TableCell<T>>> columns) {
		cache = createCache();
		setItems(items);
		this.columns.setAll(columns);
		initialize();
	}

	//================================================================================
	// Methods
	//================================================================================
	private void initialize() {
		getStyleClass().setAll(defaultStyleClasses());
		setDefaultBehaviorProvider();
		setHelper(getHelperFactory().apply(getColumnsLayoutMode()));
		setRowFactory(defaultRowFactory());
	}

	/**
	 * Tries to retrieve a column from the columns' list by the given index
	 * to then delegate to {@link #autosizeColumn(VFXTableColumn).
	 */
	public void autosizeColumn(int index) {
		try {
			VFXTableColumn<T, ? extends TableCell<T>> column = columns.get(index);
			if (column == null) return;
			autosizeColumn(column);
		} catch (Exception ignored) {}
	}

	/**
	 * Auto-sizes a column so that its header and all its "children" cells' content is visible.
	 * The actual resize is delegated to the helper: {@link VFXTableHelper#autosizeColumn(VFXTableColumn)}.
	 * <p>
	 * <b>Note:</b> this operation is peculiar in the sense that there are a few conditions to meet before the actual
	 * resize is done. You see, to compute the maximum width to allow the content to fit, first the table, the columns
	 * and the cells must have been laid out at least one time. So, if, when calling this method, the last layout request
	 * was not processed ({@link ViewportLayoutRequest#wasDone()}), the operation is <b>delayed</b>,
	 * and will run as soon as the condition is met.
	 * To be precise, the operation could still be delayed, the other conditions are defined in the helper,
	 * see {@link VFXTableHelper#autosizeColumn(VFXTableColumn)}.
	 */
	public void autosizeColumn(VFXTableColumn<T, ?> column) {
		if (getColumnsLayoutMode() == ColumnsLayoutMode.FIXED) return;
		When.onChanged(needsViewportLayout)
			.condition((o, n) -> n.wasDone())
			.then((o, n) -> getHelper().autosizeColumn(column))
			.oneShot(true)
			.executeNow(() -> getViewportLayoutRequest().wasDone())
			.listen();
	}

	/**
	 * This will simply call {@link #autosizeColumn(VFXTableColumn)} on all the table's columns.
	 * To be precise, the actual operation is delegated to the helper: {@link VFXTableHelper#autosizeColumns()}.
	 * <p>
	 * Just like {@link #autosizeColumn(VFXTableColumn)}, the operation could be <b>delayed</b> if the last layout request
	 * was not processed {@link ViewportLayoutRequest#wasDone()} at the time calling this.
	 */
	public void autosizeColumns() {
		When.onChanged(needsViewportLayout)
			.condition((o, n) -> n.wasDone())
			.then((o, n) -> getHelper().autosizeColumns())
			.oneShot(true)
			.executeNow(() -> getViewportLayoutRequest().wasDone())
			.listen();
	}

	/**
	 * Retrieves the given column's index in the table's columns' list.
	 * <p>
	 * Since every {@link VFXTableColumn} has its index as a property, {@link VFXTableColumn#indexProperty()}, this method
	 * will simply invoke the related getter.
	 * <b>However</b>, if the returned index is invalid (< 0), then it resorts to {@link List#indexOf(Object)} which is much
	 * slower. The good thing is that if it resorts to the latter, then it also updates the column's index property,
	 * so that the next time the index will be available through the property.
	 */
	public int indexOf(VFXTableColumn<T, ?> column) {
		if (column == null) return -1;
		if (column.getIndex() < 0) column.setIndex(columns.indexOf(column));
		return column.getIndex();
	}

	public void update() {
		// TODO implement
	}

	/**
	 * Setter for the {@link #stateProperty()}.
	 */
	protected void update(VFXTableState<T> state) {
		setState(state);
	}

	/**
	 * Responsible for creating the rows' cache instance used by this container.
	 *
	 * @see VFXCellsCache
	 * @see #rowsCacheCapacityProperty()
	 */
	protected VFXCellsCache<T, VFXTableRow<T>> createCache() {
		return new VFXCellsCache<>(null, getRowsCacheCapacity());
	}

	/**
	 * @return the default function used to build rows. Uses {@link VFXDefaultTableRow}.
	 */
	protected Function<T, VFXTableRow<T>> defaultRowFactory() {
		return VFXDefaultTableRow::new;
	}

	/**
	 * @return the default function used to build a {@link VFXTableHelper}.
	 */
	protected Function<ColumnsLayoutMode, VFXTableHelper<T>> defaultHelperFactory() {
		return mode -> mode == ColumnsLayoutMode.FIXED ? new FixedTableHelper<>(this) : new VariableTableHelper<>(this);
	}

	/**
	 * Setter for the {@link #needsViewportLayoutProperty()}.
	 * This sets the property to {@link ViewportLayoutRequest#EMPTY}, causing the default skin to recompute the entire layout.
	 */
	public void requestViewportLayout() {
		setNeedsViewportLayout(ViewportLayoutRequest.EMPTY.setWasDone(false));
	}

	/**
	 * Setter for the {@link #needsViewportLayoutProperty()}.
	 * This sets the property to a new {@link ViewportLayoutRequest} with the given column, causing the default skin to
	 * recompute only a portion of the layout.
	 */
	protected void requestViewportLayout(VFXTableColumn<T, ?> column) {
		setNeedsViewportLayout(new ViewportLayoutRequest(column).setWasDone(false));
	}

	//================================================================================
	// Overridden Methods
	//================================================================================
	@Override
	protected SkinBase<?, ?> buildSkin() {
		return new VFXTableSkin<>(this);
	}

	@Override
	public Supplier<VFXTableManager<T>> defaultBehaviorProvider() {
		return () -> new VFXTableManager<>(this);
	}

	@Override
	public List<String> defaultStyleClasses() {
		return List.of("vfx-table");
	}

	//================================================================================
	// Delegate Methods
	//================================================================================

	/**
	 * Delegate for {@link VFXCellsCache#populate()} (on the rows' cache).
	 *
	 * @see #populateCacheAll()
	 */
	public VFXTable<T> populateCache() {
		cache.populate();
		return this;
	}

	/**
	 * Populates the rows' cache and all the table's columns' caches.
	 *
	 * @see VFXCellsCache#populate()
	 */
	public VFXTable<T> populateCacheAll() {
		populateCache();
		columns.forEach(VFXTableColumn::populateCache);
		return this;
	}

	/**
	 * Delegate for {@link VFXCellsCache#size()} (on the row's cache).
	 */
	public int rowsCacheSize() {
		return cache.size();
	}

	/**
	 * @return the total number of cached cells by iterating over {@link #getColumns()}.
	 */
	public int cellsCacheSize() {
		return columns.stream()
			.mapToInt(VFXTableColumn::cacheSize)
			.sum();
	}

	/**
	 * Delegate for {@link VFXTableState#getRowsRange()}
	 */
	public IntegerRange getRowsRange() {return getState().getRowsRange();}

	/**
	 * Delegate for {@link VFXTableState#getColumnsRange()}
	 */
	public IntegerRange getColumnsRange() {return getState().getColumnsRange();}

	/**
	 * Delegate for {@link VFXTableState#getRowsByIndexUnmodifiable()}
	 */
	public SequencedMap<Integer, VFXTableRow<T>> getRowsByIndexUnmodifiable() {return getState().getRowsByIndexUnmodifiable();}

	/**
	 * Delegate for {@link VFXTableState#getRowsByItemUnmodifiable()}
	 */
	public List<Map.Entry<T, VFXTableRow<T>>> getRowsByItemUnmodifiable() {return getState().getRowsByItemUnmodifiable();}

	/**
	 * Delegate for {@link VFXTableHelper#virtualMaxXProperty()}
	 */
	@Override
	public ReadOnlyDoubleProperty virtualMaxXProperty() {
		return getHelper().virtualMaxXProperty();
	}

	/**
	 * Delegate for {@link VFXTableHelper#virtualMaxYProperty()}
	 */
	@Override
	public ReadOnlyDoubleProperty virtualMaxYProperty() {
		return getHelper().virtualMaxYProperty();
	}

	/**
	 * {@inheritDoc}
	 * <p></p>
	 * For the table this is a delegate to {@link #rowsCacheCapacityProperty()}, so that it can honor the
	 * {@link VFXContainer} API.
	 */
	@Override
	public StyleableObjectProperty<BufferSize> bufferSizeProperty() {
		return rowsBufferSize;
	}

	/**
	 * Delegate for {@link VFXTableHelper#scrollBy(Orientation, double)} with vertical orientation as parameter.
	 */
	public void scrollVerticalBy(double pixels) {
		getHelper().scrollBy(Orientation.VERTICAL, pixels);
	}

	/**
	 * Delegate for {@link VFXTableHelper#scrollBy(Orientation, double)} with horizontal orientation as parameter.
	 */
	public void scrollHorizontalBy(double pixels) {
		getHelper().scrollBy(Orientation.HORIZONTAL, pixels);
	}

	/**
	 * Delegate for {@link VFXTableHelper#scrollToPixel(Orientation, double)} with vertical orientation as parameter.
	 */
	public void scrollToPixelVertical(double pixel) {
		getHelper().scrollToPixel(Orientation.VERTICAL, pixel);
	}

	/**
	 * Delegate for {@link VFXTableHelper#scrollToPixel(Orientation, double)} with horizontal orientation as parameter.
	 */
	public void scrollToPixelHorizontal(double pixel) {
		getHelper().scrollToPixel(Orientation.HORIZONTAL, pixel);
	}

	/**
	 * Delegate for {@link VFXTableHelper#scrollToIndex(Orientation, int)} with vertical orientation as parameter.
	 */
	public void scrollToRow(int index) {
		getHelper().scrollToIndex(Orientation.VERTICAL, index);
	}

	/**
	 * Delegate for {@link VFXTableHelper#scrollToIndex(Orientation, int)} with horizontal orientation as parameter.
	 */
	public void scrollToColumn(int index) {
		getHelper().scrollToIndex(Orientation.HORIZONTAL, index);
	}

	/**
	 * Delegate for {@link #scrollToRow(int)} with 0 as parameter.
	 */
	public void scrollToFirstRow() {
		scrollToRow(0);
	}

	/**
	 * Delegate for {@link #scrollToRow(int)} with {@code size() - 1} as parameter.
	 */
	public void scrollToLastRow() {
		scrollToRow(size() - 1);
	}

	/**
	 * Delegate for {@link #scrollToColumn(int)} with 0 as parameter.
	 */
	public void scrollToFirstColumn() {
		scrollToColumn(0);
	}

	/**
	 * Delegate for {@link #scrollToColumn(int)} with {@code columns.size() - 1} as parameter.
	 */
	public void scrollToLastColumn() {
		scrollToColumn(columns.size() - 1);
	}

	//================================================================================
	// Styleable Properties
	//================================================================================
	private final StyleableDoubleProperty rowsHeight = new StyleableDoubleProperty(
		StyleableProperties.ROWS_HEIGHT,
		this,
		"rowsHeight",
		32.0
	);

	private final StyleableSizeProperty columnsSize = new StyleableSizeProperty(
		StyleableProperties.COLUMNS_SIZE,
		this,
		"columnsSize",
		Size.of(100.0, 32.0)
	);

	private final StyleableObjectProperty<ColumnsLayoutMode> columnsLayoutMode = new StyleableObjectProperty<>(
		StyleableProperties.COLUMNS_LAYOUT_MODE,
		this,
		"columnsLayoutMode",
		ColumnsLayoutMode.FIXED
	) {
		@Override
		protected void invalidated() {
			setHelper(getHelperFactory().apply(get()));
		}
	};

	private final StyleableDoubleProperty extraAutosizeWidth = new StyleableDoubleProperty(
		StyleableProperties.EXTRA_AUTOSIZE_WIDTH,
		this,
		"extraAutosizeWidth",
		0.0
	);

	private final StyleableObjectProperty<BufferSize> columnsBufferSize = new StyleableObjectProperty<>(
		StyleableProperties.COLUMNS_BUFFER_SIZE,
		this,
		"columnsBufferSize",
		BufferSize.standard()
	);

	private final StyleableObjectProperty<BufferSize> rowsBufferSize = new StyleableObjectProperty<>(
		StyleableProperties.ROWS_BUFFER_SIZE,
		this,
		"rowsBufferSize",
		BufferSize.standard()
	);

	private final StyleableDoubleProperty clipBorderRadius = new StyleableDoubleProperty(
		StyleableProperties.CLIP_BORDER_RADIUS,
		this,
		"clipBorderRadius",
		0.0
	);

	private final StyleableIntegerProperty rowsCacheCapacity = new StyleableIntegerProperty(
		StyleableProperties.ROWS_CACHE_CAPACITY,
		this,
		"rowsCacheCapacity",
		10
	) {
		@Override
		protected void invalidated() {
			cache.setCapacity(get());
		}
	};

	public double getRowsHeight() {
		return rowsHeight.get();
	}

	/**
	 * Specifies the fixed height for all the table's rows.
	 * <p>
	 * Note that the default {@link VFXTableHelper} implementations will also set the cells' height to this value,
	 * however you can modify such behavior if needed by providing your custom implementation through the
	 * {@link #helperFactoryProperty()}.
	 * <p>
	 * Can be set in CSS via the property: '-vfx-rows-height'.
	 */
	public StyleableDoubleProperty rowsHeightProperty() {
		return rowsHeight;
	}

	public void setRowsHeight(double rowsHeight) {
		this.rowsHeight.set(rowsHeight);
	}

	public Size getColumnsSize() {
		return columnsSize.get();
	}

	/**
	 * Specifies the columns' size as a {@link Size} object.
	 * <p>
	 * Note that the width specified by this property will be used differently depending on the {@link ColumnsLayoutMode}.
	 * In {@code FIXED} mode, all columns will have the same width and height specified by the {@link Size} object.
	 * In {@code VARIABLE} mode, the width value will be used as the <b>minimum</b> width all columns must have.
	 * This behavior can also be modified as it is defined by the default {@link VFXTableHelper} implementations.
	 * <p>
	 * Can be set in CSS via the property: '-vfx-columns-size'.
	 *
	 * @see SizeConverter
	 */
	public StyleableSizeProperty columnsSizeProperty() {
		return columnsSize;
	}

	public void setColumnsSize(Size columnsSize) {
		this.columnsSize.set(columnsSize);
	}

	/**
	 * Convenience method to create a new {@link Size} object and set the {@link #columnsSizeProperty()}.
	 */
	public void setColumnsSize(double w, double h) {
		setColumnsSize(Size.of(w, h));
	}

	/**
	 * Convenience method to create a new {@link Size} object and set the {@link #columnsSizeProperty()}.
	 * The old height will be kept.
	 */
	public void setColumnsWidth(double w) {
		setColumnsSize(Size.of(w, getColumnsSize().getHeight()));
	}

	/**
	 * Convenience method to create a new {@link Size} object and set the {@link #columnsSizeProperty()}.
	 * The old width will be kept.
	 */
	public void setColumnsHeight(double h) {
		setColumnsSize(Size.of(getColumnsSize().getWidth(), h));
	}

	public ColumnsLayoutMode getColumnsLayoutMode() {
		return columnsLayoutMode.get();
	}

	/**
	 * Specifies the layout mode for the table's columns. See {@link ColumnsLayoutMode}.
	 * <p>
	 * Can be set in CSS via the property: '-vfx-columns-layout-mode'.
	 */
	public StyleableObjectProperty<ColumnsLayoutMode> columnsLayoutModeProperty() {
		return columnsLayoutMode;
	}

	public void setColumnsLayoutMode(ColumnsLayoutMode columnsLayoutMode) {
		this.columnsLayoutMode.set(columnsLayoutMode);
	}

	/**
	 * Convenience method to switch the table's {@link ColumnsLayoutMode}.
	 */
	public void switchColumnsLayoutMode() {
		this.columnsLayoutMode.set(ColumnsLayoutMode.next(getColumnsLayoutMode()));
	}

	public double getExtraAutosizeWidth() {
		return extraAutosizeWidth.get();
	}

	/**
	 * Specifies an extra number of pixels a column will have when it is auto-sized by the {@link VFXTableHelper}.
	 * <p>
	 * In some occasions auto-sizing all the columns may result in the text of each cell being very close to each other,
	 * which is rather unpleasant to see. This extra amount acts like a "spacing" property between the columns
	 * when auto-sizing.
	 * <p></p>
	 * Can be set in CSS via the property: '-fx-extra-autosize-width'.
	 */
	public StyleableDoubleProperty extraAutosizeWidthProperty() {
		return extraAutosizeWidth;
	}

	public void setExtraAutosizeWidth(double extraAutosizeWidth) {
		this.extraAutosizeWidth.set(extraAutosizeWidth);
	}

	public BufferSize getColumnsBufferSize() {
		return columnsBufferSize.get();
	}

	/**
	 * Specifies the number of extra columns to add to the viewport to make scrolling smoother.
	 * See also {@link VFXContainer#bufferSizeProperty()} and {@link VFXTableHelper#totalRows()}
	 * <p>
	 * Can be set in CSS via the property: '-vfx-columns-buffer-size'.
	 */
	public StyleableObjectProperty<BufferSize> columnsBufferSizeProperty() {
		return columnsBufferSize;
	}

	public void setColumnsBufferSize(BufferSize columnsBufferSize) {
		this.columnsBufferSize.set(columnsBufferSize);
	}

	public BufferSize getRowsBufferSize() {
		return rowsBufferSize.get();
	}

	/**
	 * Specifies the number of extra rows to add to the viewport to make scrolling smoother.
	 * See also {@link VFXContainer#bufferSizeProperty()} and {@link VFXTableHelper#totalRows()}.
	 * <p>
	 * Can be set in CSS via the property: '-vfx-columns-buffer-size'.
	 */
	public StyleableObjectProperty<BufferSize> rowsBufferSizeProperty() {
		return rowsBufferSize;
	}

	public void setRowsBufferSize(BufferSize rowsBufferSize) {
		this.rowsBufferSize.set(rowsBufferSize);
	}

	public double getClipBorderRadius() {
		return clipBorderRadius.get();
	}

	/**
	 * Used by the viewport's clip to set its border radius.
	 * This is useful when you want to make a rounded container, this prevents the content from going outside the view.
	 * <p></p>
	 * <b>Side note:</b> the clip is a {@link Rectangle}, now for some fucking reason, the rectangle's arcWidth and arcHeight
	 * values used to make it round do not act like the border-radius or background-radius properties,
	 * instead their value is usually 2 / 2.5 times the latter.
	 * So, for a border radius of 5, you want this value to be at least 10/13.
	 * <p>
	 * Can be set in CSS via the property: '-vfx-clip-border-radius'.
	 */
	public StyleableDoubleProperty clipBorderRadiusProperty() {
		return clipBorderRadius;
	}

	public void setClipBorderRadius(double clipBorderRadius) {
		this.clipBorderRadius.set(clipBorderRadius);
	}

	public int getRowsCacheCapacity() {
		return rowsCacheCapacity.get();
	}

	/**
	 * Specifies the maximum number of rows the cache can contain at any time. Excess will not be added to the queue and
	 * disposed immediately.
	 * <p>
	 * Can be set in CSS via the property: '-vfx-rows-cache-capacity'.
	 */
	public StyleableIntegerProperty rowsCacheCapacityProperty() {
		return rowsCacheCapacity;
	}

	public void setRowsCacheCapacity(int rowsCacheCapacity) {
		this.rowsCacheCapacity.set(rowsCacheCapacity);
	}

	//================================================================================
	// CssMetaData
	//================================================================================
	private static class StyleableProperties {
		private static final StyleablePropertyFactory<VFXTable<?>> FACTORY = new StyleablePropertyFactory<>(Control.getClassCssMetaData());
		private static final List<CssMetaData<? extends Styleable, ?>> cssMetaDataList;

		private static final CssMetaData<VFXTable<?>, Number> ROWS_HEIGHT =
			FACTORY.createSizeCssMetaData(
				"-vfx-rows-height",
				VFXTable::rowsHeightProperty,
				32.0
			);

		private static final CssMetaData<VFXTable<?>, Size> COLUMNS_SIZE =
			new CssMetaData<>("-vfx-columns-size", SizeConverter.getInstance(), Size.of(100, 32)) {
				@Override
				public boolean isSettable(VFXTable<?> styleable) {
					return !styleable.columnsSizeProperty().isBound();
				}

				@Override
				public StyleableProperty<Size> getStyleableProperty(VFXTable<?> styleable) {
					return styleable.columnsSizeProperty();
				}
			};

		private static final CssMetaData<VFXTable<?>, ColumnsLayoutMode> COLUMNS_LAYOUT_MODE =
			FACTORY.createEnumCssMetaData(
				ColumnsLayoutMode.class,
				"-vfx-columns-layout-mode",
				VFXTable::columnsLayoutModeProperty,
				ColumnsLayoutMode.FIXED
			);

		private static final CssMetaData<VFXTable<?>, Number> EXTRA_AUTOSIZE_WIDTH =
			FACTORY.createSizeCssMetaData(
				"-vfx-extra-autosize-width",
				VFXTable::extraAutosizeWidthProperty,
				0.0
			);

		private static final CssMetaData<VFXTable<?>, BufferSize> COLUMNS_BUFFER_SIZE =
			FACTORY.createEnumCssMetaData(
				BufferSize.class,
				"-vfx-columns-buffer-size",
				VFXTable::columnsBufferSizeProperty,
				BufferSize.standard()
			);

		private static final CssMetaData<VFXTable<?>, BufferSize> ROWS_BUFFER_SIZE =
			FACTORY.createEnumCssMetaData(
				BufferSize.class,
				"-vfx-rows-buffer-size",
				VFXTable::rowsBufferSizeProperty,
				BufferSize.standard()
			);

		private static final CssMetaData<VFXTable<?>, Number> CLIP_BORDER_RADIUS =
			FACTORY.createSizeCssMetaData(
				"-vfx-clip-border-radius",
				VFXTable::clipBorderRadiusProperty,
				0.0
			);

		private static final CssMetaData<VFXTable<?>, Number> ROWS_CACHE_CAPACITY =
			FACTORY.createSizeCssMetaData(
				"-vfx-rows-cache-capacity",
				VFXTable::rowsCacheCapacityProperty,
				10
			);

		static {
			cssMetaDataList = StyleUtils.cssMetaDataList(
				Control.getClassCssMetaData(),
				ROWS_HEIGHT, COLUMNS_SIZE, COLUMNS_LAYOUT_MODE, EXTRA_AUTOSIZE_WIDTH,
				COLUMNS_BUFFER_SIZE, ROWS_BUFFER_SIZE, CLIP_BORDER_RADIUS,
				ROWS_CACHE_CAPACITY
			);
		}
	}

	@Override
	protected List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
		return getClassCssMetaData();
	}

	public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
		return StyleableProperties.cssMetaDataList;
	}

	//================================================================================
	// Getters/Setters
	//================================================================================

	/**
	 * @return the rows' cache instance used by this container
	 */
	protected VFXCellsCache<T, VFXTableRow<T>> getCache() {
		return cache;
	}

	@Override
	public ListProperty<T> itemsProperty() {
		return items;
	}

	public Function<T, VFXTableRow<T>> getRowFactory() {
		return rowFactory.get();
	}

	/**
	 * Specifies the function used to build the table's rows.
	 * See also {@link #defaultRowFactory()}.
	 */
	public FunctionProperty<T, VFXTableRow<T>> rowFactoryProperty() {
		return rowFactory;
	}

	public void setRowFactory(Function<T, VFXTableRow<T>> rowFactory) {
		this.rowFactory.set(rowFactory);
	}

	/**
	 * This is the observable list containing all the table's columns.
	 */
	public ObservableList<VFXTableColumn<T, ? extends TableCell<T>>> getColumns() {
		return columns;
	}

	public VFXTableHelper<T> getHelper() {
		return helper.get();
	}

	/**
	 * Specifies the instance of the {@link VFXTableHelper} built by the {@link #helperFactoryProperty()}.
	 */
	public ReadOnlyObjectWrapper<VFXTableHelper<T>> helperProperty() {
		return helper;
	}

	public void setHelper(VFXTableHelper<T> helper) {
		this.helper.set(helper);
	}

	public Function<ColumnsLayoutMode, VFXTableHelper<T>> getHelperFactory() {
		return helperFactory.get();
	}

	/**
	 * Specifies the function used to build a {@link VFXTableHelper} instance.
	 * See also {@link #defaultHelperFactory()}.
	 */
	public FunctionProperty<ColumnsLayoutMode, VFXTableHelper<T>> helperFactoryProperty() {
		return helperFactory;
	}

	public void setHelperFactory(Function<ColumnsLayoutMode, VFXTableHelper<T>> helperFactory) {
		this.helperFactory.set(helperFactory);
	}

	@Override
	public DoubleProperty vPosProperty() {
		return vPos;
	}

	@Override
	public DoubleProperty hPosProperty() {
		return hPos;
	}

	public VFXTableState<T> getState() {
		return state.get();
	}

	/**
	 * Specifies the container's current state. The state carries useful information such as the range of rows and columns
	 * and the rows ordered by index, or by item (not ordered).
	 */
	public ReadOnlyObjectProperty<VFXTableState<T>> stateProperty() {
		return state.getReadOnlyProperty();
	}

	protected void setState(VFXTableState<T> state) {
		this.state.set(state);
	}

	/**
	 * Delegate for {@link ViewportLayoutRequest#isValid()}.
	 */
	public boolean isNeedsViewportLayout() {
		return needsViewportLayout.isValid();
	}

	public ViewportLayoutRequest<T> getViewportLayoutRequest() {
		return needsViewportLayout.get();
	}

	/**
	 * Specifies whether the viewport needs to compute the layout of its content.
	 * <p>
	 * Since this is read-only, layout requests must be sent by using {@link #requestViewportLayout()}.
	 */
	public ReadOnlyObjectProperty<ViewportLayoutRequest<T>> needsViewportLayoutProperty() {
		return needsViewportLayout.getReadOnlyProperty();
	}

	protected void setNeedsViewportLayout(ViewportLayoutRequest needsViewportLayout) {
		this.needsViewportLayout.set(needsViewportLayout);
	}
}