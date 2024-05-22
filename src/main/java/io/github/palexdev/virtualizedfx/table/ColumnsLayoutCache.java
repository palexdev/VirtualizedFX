package io.github.palexdev.virtualizedfx.table;

import io.github.palexdev.virtualizedfx.cells.base.TableCell;
import io.github.palexdev.virtualizedfx.enums.ColumnsLayoutMode;
import io.github.palexdev.virtualizedfx.table.VFXTableHelper.VariableTableHelper;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Complex cache mechanism to simplify and vastly improve layout performance for {@link ColumnsLayoutMode#VARIABLE}.
 * This mode essentially disables virtualization along the x-axis which makes some computations way more expensive.
 * <p>
 * Example 1: A columns width must be 'asked' to the column itself rather than using the value specified by
 * {@link VFXTable#columnsSizeProperty()}
 * <p>
 * Example 2: A columns position cannot be determined by a simple multiplication, but it's the sum of all previous
 * columns' widths (a loop)
 * <p>
 * Also, keep in mind that such computations are not needed only for the columns, but also for their corresponding cells
 * (can't rely on JavaFX bounds because sometimes they are messed up, garbage framework).
 * <p>
 * This cache implementation tries to mitigate this by caching columns' data such as their width, position and visibility
 * in the viewport. Listeners and bindings will automatically invalidate the data as needed, and re-compute it once
 * requested, which in other words means that the cache is 'lazy'.
 * <p></p>
 * <b>Why this extends</b> {@link DoubleBinding}
 * <p>
 * When I decided to create this special cache, it was mainly to improve the computation speed of the {@link VFXTable#virtualMaxXProperty()}
 * (in VARIABLE mode ofc), because it requires summing every column's width. So, I came up with a simple extension of
 * {@link DoubleBinding} which would invalidate the cached widths and thus re-compute upon request their sum.
 * It was then that I decided to expand the cache to also have positions and visibility checks, because the three pieces
 * of information are tightly coupled. Visibility depends on the width and the position, the latter depends on the width.
 * So, besides making such computations faster, this also still allows computing the {@code virtualMaxX} much faster.
 * There's even a special width value given by {@link #getPartialWidth()} which is the sum of all column's widths excluding
 * the last one. This is useful to compute the last column's width, as it may need to be bigger than expected to fill
 * the table (such value could be given by {@code tableWidth - partialWidth}).
 * <p></p>
 * <b>How data is stored</b>
 * <p>
 * The cache makes use of a {@link Map} and a wrapper class {@link LayoutInfo} to gather all the computations in one place.
 * Each table's column will have an entry in the map like this: [Column -> LayoutInfo]. When something needs to be
 * invalidated, setters are called on the appropriate {@link LayoutInfo} object.
 * <p></p>
 * <b>Listeners</b>
 * <p>
 * To manage invalidations and columns changes in the table, this uses a series of listeners.
 * <p> 1) A {@link ListChangeListener} ensures the above-mentioned map stays always updated, more info here {@link #handleColumns(ListChangeListener.Change)}
 * <p> 2) An {@link InvalidationListener} watches for {@link VFXTable#columnsSizeProperty()} changes and by iterating over
 * the {@link LayoutInfo} stored in the map, performs the following actions: a) resets both the positions and visibility
 * flags; b) invalidates the width if it's below the new value specified by the property; c) at the end it also invalidates
 * the width for the last column (if it wasn't done before). This is important to ensure that the last column takes all
 * the available space
 * <p> 3) An {@link InvalidationListener} added on both the {@link VFXTable#widthProperty()} and {@link VFXTable#hPosProperty()}.
 * This listener is responsible for clearing, thus forcing the re-computation when requested, of the visibility cache
 * <p> 4) Lastly, there an {@link InvalidationListener} for each column in the map to watch for {@link VFXTableColumn#prefWidthProperty()}
 * changes. This is managed by each {@link LayoutInfo}, more info there.
 * <p></p>
 * <b>Computing functions and initialization</b>
 * <p>
 * For the cache to work, the user must specify the three functions used to compute:
 * <p> 1) the widths, {@link #setWidthFunction(BiFunction)}
 * <p> 2) the positions, {@link #setPositionFunction(BiFunction)}
 * <p> 3) the visibility, {@link #setVisibilityFunction(Function)}
 * <p>
 * To avoid cluttering the constructors, and for other reasons, the cache won't be active until you call the
 * {@link #init()} method. Both the setters and the init methods follow the fluent API pattern. <b>Beware,</b> if any
 * of the three functions is not set, it will throw an exception!
 *
 * @see LayoutInfoCache
 */
public class ColumnsLayoutCache<T> extends DoubleBinding {
	//================================================================================
	// Properties
	//================================================================================
	private VFXTable<T> table;
	private final LayoutInfoCache cache;
	private boolean init = false;
	public boolean sortToString = false;

	private VFXTableColumn<T, ?> lColumn;
	private final ReadOnlyBooleanWrapper anyChanged = new ReadOnlyBooleanWrapper(false) {
		@Override
		protected void invalidated() {
			if (get()) {
				invalidateLast();
				invalidate();
			}
		}
	};
	private Consumer<Boolean> invalidatingAction = last -> {
		if (last) invalidate();
		else anyChanged.set(true);
	};

	// Layout functions
	private BiFunction<VFXTableColumn<T, ?>, Boolean, Double> widthFn;
	private BiFunction<Integer, Double, Double> xPosFn;
	private Function<VFXTableColumn<T, ?>, Boolean> vFn;

	// Listeners
	private ListChangeListener<VFXTableColumn<T, ?>> clListener;
	private InvalidationListener csListener;
	private InvalidationListener vListener;

	//================================================================================
	// Constructors
	//================================================================================
	public ColumnsLayoutCache(VFXTable<T> table) {
		this.table = table;
		cache = new LayoutInfoCache();
		clListener = this::handleColumns;
		csListener = i -> {
			for (LayoutInfo li : cache.values()) {
				// Resets all positions and visibility flags
				li.resetPos();
				li.resetVisibility();
				// Invalidate only the ones that are now below the minimum
				if (!li.isWidthValid()) continue;
				if (li.getWidth() < table.getColumnsSize().getWidth()) li.invalidateWidth();
			}
			// Also invalidate last
			invalidateLast();
		};
		vListener = i -> {
			// Too unpredictable, better safe than sorry strategy, invalidate the whole cache
			cache.clearVisibilityCache();
			invalidateLast();
		};
	}

	//================================================================================
	// Methods
	//================================================================================

	/**
	 * If {@link #preInitCheck()} does not throw any exception, initializes the cache by adding the needed listeners
	 * to the appropriate properties, as well as creating the cache mappings for each column in the table.
	 * <p>
	 * Further calls to this method won't do anything if the cache has already been initialized before.
	 */
	public ColumnsLayoutCache<T> init() {
		if (!init) {
			preInitCheck();
			ObservableList<VFXTableColumn<T, ? extends TableCell<T>>> columns = table.getColumns();
			if (!columns.isEmpty()) {
				lColumn = columns.getLast();
				for (VFXTableColumn<T, ? extends TableCell<T>> c : columns) cache.put(c, new LayoutInfo(c));
			}
			columns.addListener(clListener);
			table.columnsSizeProperty().addListener(csListener);
			table.widthProperty().addListener(vListener);
			table.hPosProperty().addListener(vListener);
			init = true;
		}
		return this;
	}

	/**
	 * Checks that all the computing functions are set.
	 *
	 * @see #setWidthFunction(BiFunction)
	 * @see #setPositionFunction(BiFunction)
	 * @see #setVisibilityFunction(Function)
	 */
	private void preInitCheck() {
		if (widthFn == null)
			throw new IllegalStateException("Cannot initialize because: width function has not been set.");
		if (xPosFn == null)
			throw new IllegalStateException("Cannot initialize because: x position function has not been set.");
		if (vFn == null)
			throw new IllegalStateException("Cannot initialize because: visibility function has not been set.");
	}

	/**
	 * Delegates to {@link LayoutInfoCache#getWidth(VFXTableColumn)}.
	 *
	 * @return either the cached or computed width for the given column
	 */
	public double getColumnWidth(VFXTableColumn<T, ?> column) {
		return cache.getWidth(column);
	}

	/**
	 * Delegates to {@link #getColumnWidth(VFXTableColumn)} by passing the last column in the table.
	 */
	public double getLastColumnWidth() {
		return getColumnWidth(lColumn);
	}

	/**
	 * @return the sum of all columns' widths excluding the last one
	 */
	public double getPartialWidth() {
		return cache.entrySet().stream()
			.filter(e -> e.getKey() != lColumn)
			.mapToDouble(e -> e.getValue().getWidth())
			.sum();
	}

	/**
	 * The position of the column at the given index. This method is recursive!
	 * <p>
	 * Detailing the internals:
	 * <pre>
	 * {@code
	 * // Let's suppose we want to compute the position of the column at index 2 (so third one)
	 * // First we convert the index to the corresponding column
	 * VFXTableColumn c = ...;
	 *
	 * // Then we query the map and get the known position for that column
	 * double pos = map.getPos(c);
	 * // Index 0 is a special case and we handle it as follows
	 * if (index == 0) {
	 *
	 *     map.setPos(c, 0); // Column 0 is always at x = 0
	 *     return 0;
	 * }
	 * // If 'pos' is lesser than 0, then it either means it was never been computed before or it was invalidated
	 * // We need to ask the position function to compute the value as follows...
	 * if (pos < 0) {
	 *     pos = posFn.apply(index -1, getColumnPos(index -1)); // Here's where the method calls itself
	 *     map.setPos(index, pos); // Store the found pos in the cache so we don't fall in this 'if' again until invalidated
	 * }
	 * return pos;
	 *
	 * // Why the recursion?
	 * // In general, to compute a column's position, we can simply get the position of the previous column + its width.
	 * // So, for the third one, we need the second one's position, and so on...
	 * // The recursion doesn't happen if the previous value is known, so the method acts almost like a simple getter
	 * // The recursion stops at column 0, because it's position is always 0.
	 * }
	 * </pre>
	 */
	public double getColumnPos(int index) {
		VFXTableColumn<T, ? extends TableCell<T>> column = table.getColumns().get(index);
		LayoutInfo li = cache.get(column);
		double pos = li.getPos();
		if (index == 0) {
			li.setPos(0.0);
			return 0.0;
		}
		if (pos < 0) {
			pos = xPosFn.apply(index - 1, getColumnPos(index - 1));
			li.setPos(pos);
		}
		return pos;
	}

	/**
	 * Queries the map to check whether the given column is visible.
	 * <p>
	 * If the {@link LayoutInfo} mapped to the column returns a {@code null} value, then it means that the visibility
	 * check was either never done before or invalidated. In this case, the visibility function will compute it and the
	 * {@link LayoutInfo} object updated.
	 */
	public boolean isInViewport(VFXTableColumn<T, ?> column) {
		LayoutInfo li = cache.get(column);
		if (li.isVisible() == null) li.setVisible(vFn.apply(column));
		return li.isVisible();
	}

	/**
	 * @return the number of entries in the cache's map. This should always be equal to the size of {@link VFXTable#getColumns()}
	 */
	public int size() {
		return cache.size();
	}

	/**
	 * Invalidates the last column's width.
	 */
	private void invalidateLast() {
		cache.invalidateWidth(lColumn);
	}

	/**
	 * This method is responsible for updating the cache map's entries when changes occur in {@link VFXTable#getColumns()}.
	 * <p>
	 * If there are no columns anymore, calls {@link #clear()} and {@link #invalidate()}, then exits.
	 * <p>
	 * Remember, the last column is always a special case in the table because it behaves a little different from the others.
	 * So, before processing the {@link Change}, we must ensure that the last column is still the same as before.
	 * If that's not the case, first we call {@link #invalidateLast()} to ensure that the 'now previously last' column
	 * has the right width (the width computing function is likely to return a different value now). It does not matter
	 * if the change is going to remove that column, we must do it anyway for consistency. Then we update the local
	 * reference for the last column (yes, the cache stores it for fast access), and at this point two things can happen:
	 * <p> 1) the new last column already was present in the cache, we simply call {@link LayoutInfo#invalidateWidth()}
	 * <p> 2) the new last column has been added by the {@link Change} so we need to create a new entry in the cache's map
	 * <p></p>
	 * After the above checks we can start processing the {@link Change}. We just need to handle additions and removals.
	 * For each removal, we remove the corresponding entry from the map and dispose it {@link LayoutInfo#dispose()}.
	 * For each addition, we create a new entry in the map {@link LayoutInfo#LayoutInfo(VFXTableColumn)}.
	 * <p>
	 * <b>Note:</b> as you may know, JavaFX sucks. So, we actually need to manage this a bit differently. You see,
	 * {@code setAll()} operations pose a big issue. As described in {@link Change}'s documentation, calling {@code set()}
	 * on the list will be treated as both an addition and a removal, also the {@code replaced} flag will return {@code true},
	 * which makes sens right? When such change occurs, the added and removed lists carried by the {@link Change}
	 * will respectively contain all the items in the list and all the previous items in the list. But what happens when
	 * the new ones are pretty much the same as the old ones, maybe with just a few additions/removals? We simply can't
	 * treat the change as suggested here {@link Change}, because we would first remove and dispose all the {@link LayoutInfo}
	 * objects, and then create them again. What. A. Waste. Of. Performance!
	 * <p>
	 * So, how do we handle this? A temporary collection stores all the removed values. When processing the additions,
	 * we remove any entry that is also present in that collection. This way we only keep the values that have actually
	 * been removed, and for these we can remove the entry and call {@link LayoutInfo#dispose()}.
	 * <p></p>
	 * Finally, we invalidate all the positions and visibility flags. Re-computing them is far more convenient and stable
	 * than trying to guess which one is still good and which not. We also call {@link #invalidate()} and {@link #invalidateLast()}.
	 */
	private void handleColumns(ListChangeListener.Change<? extends VFXTableColumn<T, ?>> change) {
		ObservableList<VFXTableColumn<T, ? extends TableCell<T>>> columns = table.getColumns();
		if (columns.isEmpty()) {
			clear();
			invalidate();
			return;
		}

		VFXTableColumn<T, ? extends TableCell<T>> last = columns.getLast();
		if (last != lColumn) {
			// Invalidate the previous last's binding
			invalidateLast();
			lColumn = last;
			/*
			 * Since bindings are all the same whether it's the first column, in the middle or the last one...
			 * There is no need to replace the binding.
			 * What we want to do here is: if the binding is already present, we just invalidate it
			 * (since the widthFn is likely to return a different value now), otherwise we just create it.
			 */
			cache.computeIfAbsent(last, LayoutInfo::new).invalidateWidth();
		}

		Set<VFXTableColumn<T, ?>> rm = new HashSet<>();
		while (change.next()) {
			if (change.wasRemoved()) rm.addAll(change.getRemoved());
			if (change.wasAdded()) {
				for (VFXTableColumn<T, ?> c : change.getAddedSubList()) {
					if (rm.contains(c)) {
						rm.remove(c);
						continue;
					}
					if (c == lColumn) continue;
					cache.put(c, new LayoutInfo(c));
				}
			}
		}
		rm.forEach(c -> cache.remove(c).dispose());

		cache.values().forEach(li -> {
			li.resetPos();
			li.resetVisibility();
		});
		invalidate();
		invalidateLast();
	}

	/**
	 * Clears the cache by removing all the entries from the map and setting the last column local reference to {@code null}.
	 */
	private void clear() {
		cache.clear();
		anyChanged.set(false);
		lColumn = null;
	}

	//================================================================================
	// Overridden Methods
	//================================================================================

	/**
	 * @return the sum of all columns' widths, each given by {@link LayoutInfo#getWidth()}
	 */
	@Override
	protected double computeValue() {
		anyChanged.set(false);
		return cache.values().stream()
			.mapToDouble(LayoutInfo::getWidth)
			.sum();
	}

	/**
	 * Disposes the cache making it not usable anymore.
	 *
	 * @see #clear()
	 */
	@Override
	public void dispose() {
		clear();
		widthFn = null;
		invalidatingAction = null;
		table.getColumns().removeListener(clListener);
		table.columnsSizeProperty().removeListener(csListener);
		table.widthProperty().removeListener(vListener);
		table.hPosProperty().removeListener(vListener);
		clListener = null;
		csListener = null;
		vListener = null;
		table = null;
	}

	@Override
	public String toString() {
		Map<VFXTableColumn<T, ?>, LayoutInfo> cache = this.cache;
		if (sortToString) cache = new TreeMap<>(this.cache);

		StringBuilder sb = new StringBuilder();
		sb.append("ColumnsLayoutCache [%s][%d] {".formatted(isValid() ? "valid:[%f]".formatted(get()) : "invalid", size()));
		if (cache.isEmpty()) {
			sb.append("empty}");
			return sb.toString();
		}
		sb.append("\n");

		// Pretty print
		int maxL = 0;
		for (VFXTableColumn<T, ?> c : cache.keySet()) {
			String text = Optional.ofNullable(c.getText()).orElse("");
			maxL = Math.max(maxL, text.length());
		}

		for (Iterator<Map.Entry<VFXTableColumn<T, ?>, LayoutInfo>> iterator = cache.entrySet().iterator(); iterator.hasNext(); ) {
			Map.Entry<VFXTableColumn<T, ?>, LayoutInfo> entry = iterator.next();
			VFXTableColumn<T, ?> c = entry.getKey();
			LayoutInfo i = entry.getValue();
			int index = i.getIndex();
			String text = Optional.ofNullable(c.getText()).orElse("");

			DoubleBinding b = i.wBinding;
			double pos = i.getPos();
			Boolean visibility = i.isVisible();

			sb.append("  ")
				.append("Column: ")
				.append(" ".repeat(maxL - "Column".length()))
				.append(text)
				.append("\n")
				.append("  ")
				.append("Index: ")
				.append(" ".repeat(maxL - "Index".length()))
				.append("[%d]".formatted(index))
				.append("\n")
				.append("  ")
				.append("Width: ")
				.append(" ".repeat(maxL - "Width".length()))
				.append(b.isValid() ? "[valid:%.2f]".formatted(b.get()) : "[invalid]")
				.append("\n")
				.append("  ")
				.append("Position: ")
				.append(" ".repeat(maxL - "Position".length()))
				.append((pos == -1.0) ? "[valid:%.2f]".formatted(pos) : "[invalid]")
				.append("\n")
				.append("  ")
				.append("Visible: ")
				.append(" ".repeat(maxL - "Visible".length()))
				.append((visibility != null && visibility) ? "[valid:true]" : "[invalid]")
				.append("\n");
			if (iterator.hasNext()) {
				sb.append("  ");
				sb.append("_".repeat(30));
				sb.append("\n");
			}
		}
		sb.append("}");
		return sb.toString();
	}

	//================================================================================
	// Getters/Setters
	//================================================================================

	/**
	 * @return the {@link VFXTable} instance this cache is related to
	 */
	public VFXTable<T> getTable() {
		return table;
	}

	/**
	 * @return the map containing the columns' layout data as {@link LayoutInfo} objects
	 */
	protected Map<VFXTableColumn<T, ?>, LayoutInfo> getCacheMap() {
		return cache;
	}

	/**
	 * @return the local reference to the last column in the table
	 */
	protected VFXTableColumn<T, ?> getLastColumn() {
		return lColumn;
	}

	public boolean isAnyChanged() {
		return anyChanged.get();
	}

	/**
	 * Specifies whether any of the {@link LayoutInfo} objects in {@link #getCacheMap()} was invalidated.
	 */
	public ReadOnlyBooleanProperty anyChangedProperty() {
		return anyChanged.getReadOnlyProperty();
	}

	/**
	 * Sets the {@link BiFunction} responsible for computing a column's width. The function gives the following parameters:
	 * 1) the column to compute the width for; 2) whether it is the last column in the table which may need special handling.
	 * <p>
	 * You can check {@link VariableTableHelper#computeColumnWidth(VFXTableColumn, boolean)} for an example.
	 */
	public ColumnsLayoutCache<T> setWidthFunction(BiFunction<VFXTableColumn<T, ?>, Boolean, Double> widthFn) {
		this.widthFn = widthFn;
		return this;
	}

	/**
	 * Sets the {@link BiFunction} responsible for computing a column's position. The function gives the following parameters:
	 * 1) the previous column's index; 2) the previous column's width.
	 * <p>
	 * To understand the why of those parameters, read {@link #getColumnPos(int)}.
	 * <p>
	 * You can check {@link VariableTableHelper#computeColumnPos(int, double)} for an example.
	 */
	public ColumnsLayoutCache<T> setPositionFunction(BiFunction<Integer, Double, Double> xPosFn) {
		this.xPosFn = xPosFn;
		return this;
	}

	/**
	 * Sets the {@link Function} responsible for computing a column's width. The function gives the column for which
	 * compute the visibility as the parameter.
	 * <p>
	 * You can check {@link VariableTableHelper#computeVisibility(VFXTableColumn)} for an example.
	 */
	public ColumnsLayoutCache<T> setVisibilityFunction(Function<VFXTableColumn<T, ?>, Boolean> vFn) {
		this.vFn = vFn;
		return this;
	}

	//================================================================================
	// Internal Classes
	//================================================================================

	/**
	 * Nothing special, just an extension of {@link HashMap} to store data about columns' layout as {@link LayoutInfo} objects.
	 * <p>
	 * Makes the variable declaration shorted and offers a bunch of convenience methods, that's all.
	 *
	 * @see LayoutInfo
	 */
	public class LayoutInfoCache extends HashMap<VFXTableColumn<T, ?>, LayoutInfo> {

		//================================================================================
		// Width
		//================================================================================
		public double getWidth(VFXTableColumn<T, ?> column) {
			return get(column).getWidth();
		}

		public boolean isWidthValid(VFXTableColumn<T, ?> column) {
			return get(column).isWidthValid();
		}

		private void invalidateWidth(VFXTableColumn<T, ?> column) {
			LayoutInfo li = get(column);
			if (li == null) return;
			li.invalidateWidth();
		}

		//================================================================================
		// Position
		//================================================================================
		public double getPos(int index) {
			ObservableList<VFXTableColumn<T, ? extends TableCell<T>>> columns = table.getColumns();
			return get(columns.get(index)).getPos();
		}

		private void setPos(int index, double pos) {
			ObservableList<VFXTableColumn<T, ? extends TableCell<T>>> columns = table.getColumns();
			if (index > columns.size() - 1) return;
			get(columns.get(index)).setPos(pos);
		}

		//================================================================================
		// Visibility
		//================================================================================
		public boolean isVisible(VFXTableColumn<T, ?> column) {
			return get(column).isVisible();
		}

		private void setVisibility(VFXTableColumn<T, ?> column, Boolean visibility) {
			get(column).setVisible(visibility);
		}

		//================================================================================
		// Misc
		//================================================================================
		public void clearPositionCache() {
			values().forEach(LayoutInfo::resetPos);
		}

		public void clearVisibilityCache() {
			values().forEach(LayoutInfo::resetVisibility);
		}

		@Override
		public void clear() {
			values().forEach(LayoutInfo::dispose);
			super.clear();
		}
	}

	/**
	 * Wrapper class for layout data related to a specific {@link VFXTableColumn}.
	 * This stores: its index [init:-1], its width as a {@link DoubleBinding}, its x position [default:-1.0],
	 * and its visibility [default:null].
	 * <p></p>
	 * <b>Width handling</b>
	 * <p>
	 * For better performance, the column's width is stored as a binding, so the value is computed lazily (only upon request).
	 * Invalidation is handled "manually". Check {@link #createWidthBinding()} for more details.
	 * <p></p>
	 * <b>Null visibility? What?</b>
	 * <p>
	 * This uses {@code null} as a possible visibility value, to indicate that it is invalid and thus must be computed.
	 */
	public class LayoutInfo implements Comparable<LayoutInfo> {
		//================================================================================
		// Properties
		//================================================================================
		private VFXTableColumn<T, ?> column;
		private int index = -1;
		private DoubleBinding wBinding;
		private double pos = -1.0;
		private Boolean visible = null;

		//================================================================================
		// Constructors
		//================================================================================
		public LayoutInfo(VFXTableColumn<T, ?> column) {
			this.column = column;
			this.wBinding = createWidthBinding();
		}

		//================================================================================
		// Methods
		//================================================================================

		/**
		 * @return the column instance the layout data refers to
		 */
		public VFXTableColumn<T, ?> getColumn() {
			return column;
		}

		/**
		 * @return the column's index retrieved the first time by {@link VFXTable#indexOf(VFXTableColumn)} (then cached)
		 */
		public int getIndex() {
			if (index == -1) index = table.indexOf(column);
			return index;
		}

		/**
		 * Calls {@link DoubleBinding#get()} on the column's width binding.
		 */
		public double getWidth() {
			return wBinding.get();
		}

		/**
		 * @return whether {@link DoubleBinding#isValid()} is true
		 */
		public boolean isWidthValid() {
			return wBinding.isValid();
		}

		/**
		 * Invalidates the width binding by calling {@link DoubleBinding#invalidate()}.
		 */
		private void invalidateWidth() {
			wBinding.invalidate();
		}

		/**
		 * @return the stored column's x position
		 */
		public double getPos() {
			return pos;
		}

		/**
		 * Sets the column's x position.
		 */
		private void setPos(double pos) {
			this.pos = pos;
		}

		/**
		 * Resets, and thus invalidates, the column's x position to -1.0
		 */
		private void resetPos() {
			setPos(-1.0);
		}

		/**
		 * @return whether the column is visible in the viewport. Beware, this can also return {@code null} to indicate
		 * that the value is invalid and should be re-computed by the cache
		 */
		public Boolean isVisible() {
			return visible;
		}

		/**
		 * Sets whether the column is visible in the viewport.
		 */
		private void setVisible(Boolean visible) {
			this.visible = visible;
		}

		/**
		 * Resets, and thus invalidates, the column's visibility to {@code null}.
		 */
		private void resetVisibility() {
			setVisible(null);
		}

		/**
		 * This is responsible for creating the {@link DoubleBinding} which computes the column's width by using
		 * the function set by {@link #setWidthFunction(BiFunction)}.
		 * <p>
		 * It returns an inline custom binding which depends on {@link VFXTableColumn#prefWidthProperty()}. When this
		 * property changes two things must happen:
		 * <p> 1) obviously the binding must become invalid, because now the width function may return a different value
		 * <p> 2) we must partially invalidate the positions and visibility values. By partial, I mean only the columns
		 * starting from {@link #getIndex()} to the last one.
		 */
		private DoubleBinding createWidthBinding() {
			return new DoubleBinding() {
				{
					bind(column.prefWidthProperty());
				}

				private void invalidatePartial() {
					VFXTable<T> table = getTable();
					ObservableList<VFXTableColumn<T, ? extends TableCell<T>>> columns = table.getColumns();
					int index = getIndex();
					int size = columns.size();
					for (int i = index; i < size; i++) {
						if (cache.getPos(i) == -1.0) break;
						cache.setPos(i, -1.0);
						if (i + 1 < size) cache.setVisibility(columns.get(i + 1), null);
					}
				}

				@Override
				protected double computeValue() {
					return widthFn.apply(column, column == lColumn);
				}

				@Override
				protected void onInvalidating() {
					invalidatePartial();
					invalidatingAction.accept(column == lColumn);
				}

				@Override
				public void dispose() {
					unbind(column.prefWidthProperty());
				}
			};
		}

		/**
		 * Calls {@link DoubleBinding#dispose()} and sets both the binding and the column instances to {@code null}.
		 */
		private void dispose() {
			wBinding.dispose();
			wBinding = null;
			column = null;
		}

		//================================================================================
		// Overridden Methods
		//================================================================================
		@Override
		public int compareTo(LayoutInfo o) {
			return Integer.compare(getIndex(), o.getIndex());
		}

		@SuppressWarnings("unchecked")
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			LayoutInfo that = (LayoutInfo) o;
			return Objects.equals(getColumn(), that.getColumn());
		}

		@Override
		public int hashCode() {
			return Objects.hash(getColumn());
		}
	}
}
