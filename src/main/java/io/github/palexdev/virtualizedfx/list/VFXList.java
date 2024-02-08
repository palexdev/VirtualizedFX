package io.github.palexdev.virtualizedfx.list;

import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.base.properties.functional.FunctionProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableBooleanProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableDoubleProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableIntegerProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableObjectProperty;
import io.github.palexdev.mfxcore.controls.Control;
import io.github.palexdev.mfxcore.controls.SkinBase;
import io.github.palexdev.mfxcore.utils.fx.PropUtils;
import io.github.palexdev.mfxcore.utils.fx.StyleUtils;
import io.github.palexdev.virtualizedfx.base.VFXContainer;
import io.github.palexdev.virtualizedfx.cells.Cell;
import io.github.palexdev.virtualizedfx.enums.BufferSize;
import io.github.palexdev.virtualizedfx.list.VFXListHelper.HorizontalHelper;
import io.github.palexdev.virtualizedfx.list.VFXListHelper.VerticalHelper;
import io.github.palexdev.virtualizedfx.properties.VFXListStateProperty;
import io.github.palexdev.virtualizedfx.utils.VFXCellsCache;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleablePropertyFactory;
import javafx.geometry.Orientation;
import javafx.scene.shape.Rectangle;

import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Implementation of a virtualized container to show a list of items either vertically or horizontally.
 * The default style class is: '.virtualized-list'.
 * <p>
 * Extends {@link Control}, implements {@link VFXContainer}, has its own skin implementation {@link VFXListSkin}
 * and behavior {@link VFXListManager}. Uses cells of type {@link Cell}.
 * <p>
 * This is a stateful component, meaning that every meaningful variable (position, size, cell size, etc.) will produce a new
 * {@link VFXListState}. The state determines which and how items are displayed in the container.
 * <p></p>
 * <b>Features & Implementation Details</b>
 * <p> - The default behavior implementation, {@link VFXListManager}, can be considered as the name suggests more like
 * a 'manager' than an actual behavior. It is responsible for reacting to core changes here in the control to produce a new
 * state. The state can be considered like a picture of the container at a certain time. Each combination of the variables
 * that influence the way items are shown (how many, start, end, changes in the list, etc.) produce a specific state.
 * This is an important concept as some of the features I'm going to mention below are due to the combination of default
 * skin + default behavior. You are allowed to change/customize the skin and the behavior as you please.
 * <p> - The items list is managed automatically (permutations, insertions, removals, updates). Compared to previous
 * algorithms the {@link VFXListManager} implements a much simpler strategy while still trying to keep the cell updates
 * count as low as possible to improve performance. See {@link VFXListManager#onItemsChanged()}.
 * <p> - The function used to generate the cells, called "cellFactory", can be changed anytime, even at runtime,
 * see {@link VFXListManager#onCellFactoryChanged()}.
 * <p> - The component is based on a fixed size for all the cell, this parameter can be controlled through the {@link #cellSizeProperty()},
 * and can also be changed anytime, see {@link VFXListManager#onCellSizeChanged()}.
 * <p> - Similar to the {@code VBox} pane, this container allows you to evenly space the cells in the viewport by setting the
 * {@link #spacingProperty()}. See {@link VFXListManager#onSpacingChanged()}.
 * <p> - The container can be oriented either vertically or horizontally through the {@link #orientationProperty()}.
 * Depending on the orientation, the layout and other computations change. Thanks to polymorphism, it's possible
 * to define a public API for such computations and implement two separate classes for each orientation, this is the
 * {@link VFXListHelper}. You are allowed to change the helper through the {@link #helperFactoryProperty()}.
 * <p> - The vertical and horizontal positions are available through the properties, {@link #vPosProperty()} and {@link #hPosProperty()}.
 * It was indeed possible to use a single property for the position, but they are split for performance reasons.
 * The virtual bounds of the container are given by the two properties:
 * <p>&emsp; a) the {@link #virtualMaxXProperty()} which specifies the total number of pixels alongside the x-axis
 * <p>&emsp; b) the {@link #virtualMaxYProperty()} which specifies the total number of pixels alongside the y-axis
 * <p>
 * &emsp; The value of such properties depends on the container's orientation. The virtualized axis will have its value given by
 * &emsp; the total number of items in the list multiplied by the cell size, the spacing is included too. The other axis will have
 * &emsp; its value given by the biggest (in height or width) cell in the viewport.
 * <p> - You can access the current state through the {@link #stateProperty()}. The state gives crucial information about
 * the container such as the range of displayed items and the visible cells (by index and by item). If you'd like to observe
 * for changes in the displaying cells, you want to add a listener on this property.
 * <p> - It is possible to programmatically tell the viewport to update its layout with {@link #requestViewportLayout()},
 * although this should never be necessary as it is handled automatically when the state changes.
 * <p> - Additionally, this container makes use of a simple cache implementation, {@link VFXCellsCache}, which
 * avoids creating new cells when needed if some are already present in it. The most crucial aspect for this kind of
 * virtualization is to avoid creating nodes, as this is the most expensive operation it can occur. Not only nodes need
 * to be created but also added to the container and then laid out. Instead, it's much more likely that the {@link Cell#updateItem(Object)}
 * will be simple and faster.
 * <p></p>
 *
 * @param <T> the type of items in the list
 * @param <C> the type of cells used by the container to visualize the items
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class VFXList<T, C extends Cell<T>> extends Control<VFXListManager<T, C>> implements VFXContainer<T> {
	//================================================================================
	// Properties
	//================================================================================
	private final VFXCellsCache<T, C> cache;
	private final ListProperty<T> items = new SimpleListProperty<>(FXCollections.observableArrayList()) {
		@Override
		public void set(ObservableList<T> newValue) {
			if (newValue == null) newValue = FXCollections.observableArrayList();
			super.set(newValue);
		}
	};
	private final FunctionProperty<T, C> cellFactory = new FunctionProperty<>() {
		@Override
		protected void invalidated() {
			cache.setCellFactory(get());
		}
	};
	private final ReadOnlyObjectWrapper<VFXListHelper<T, C>> helper = new ReadOnlyObjectWrapper<>() {
		@Override
		public void set(VFXListHelper<T, C> newValue) {
			if (newValue == null)
				throw new NullPointerException("List helper cannot be null!");
			VFXListHelper<T, C> oldValue = get();
			if (oldValue != null) oldValue.dispose();
			super.set(newValue);
		}
	};
	private final FunctionProperty<Orientation, VFXListHelper<T, C>> helperFactory = new FunctionProperty<>(defaultHelperFactory()) {
		@Override
		public void set(Function<Orientation, VFXListHelper<T, C>> newValue) {
			if (newValue == null)
				throw new NullPointerException("List helper factory cannot be null!");
			super.set(newValue);
		}

		@Override
		protected void invalidated() {
			Orientation orientation = getOrientation();
			VFXListHelper<T, C> helper = get().apply(orientation);
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

	private final VFXListStateProperty<T, C> state = new VFXListStateProperty<>(VFXListState.EMPTY);
	private final ReadOnlyBooleanWrapper needsViewportLayout = new ReadOnlyBooleanWrapper(false);

	//================================================================================
	// Constructors
	//================================================================================
	public VFXList() {
		cache = createCache();
		setOrientation(Orientation.VERTICAL);
		initialize();
	}

	public VFXList(ObservableList<T> items, Function<T, C> cellFactory) {
		this();
		setItems(items);
		setCellFactory(cellFactory);
	}

	public VFXList(ObservableList<T> items, Function<T, C> cellFactory, Orientation orientation) {
		this();
		setItems(items);
		setCellFactory(cellFactory);
		setOrientation(orientation);
	}

	//================================================================================
	// Methods
	//================================================================================
	private void initialize() {
		getStyleClass().addAll(defaultStyleClasses());
		setDefaultBehaviorProvider();
	}

	/**
	 * Responsible for creating the cache instance used by this container.
	 *
	 * @see VFXCellsCache
	 * @see #cacheCapacityProperty()
	 */
	protected VFXCellsCache<T, C> createCache() {
		return new VFXCellsCache<>(null, getCacheCapacity());
	}

	/**
	 * Setter for the {@link #stateProperty()}.
	 */
	protected void update(VFXListState<T, C> state) {
		setState(state);
	}

	/**
	 * @return the default function used to produce a {@link VFXListHelper} according to the container's orientation.
	 */
	protected Function<Orientation, VFXListHelper<T, C>> defaultHelperFactory() {
		return o -> (o == Orientation.VERTICAL) ?
			new VerticalHelper<>(this) :
			new HorizontalHelper<>(this);
	}

	/**
	 * Setter for the {@link #needsViewportLayoutProperty()}.
	 * This sets the property to true, causing the default skin to recompute the cells' layout.
	 */
	public void requestViewportLayout() {
		setNeedsViewportLayout(true);
	}

	//================================================================================
	// Overridden Methods
	//================================================================================
	@Override
	public List<String> defaultStyleClasses() {
		return List.of("vfx-list");
	}

	@Override
	protected SkinBase<?, ?> buildSkin() {
		return new VFXListSkin<>(this);
	}

	@Override
	public Supplier<VFXListManager<T, C>> defaultBehaviorProvider() {
		return () -> new VFXListManager<>(this);
	}

	//================================================================================
	// Delegate Methods
	//================================================================================

	/**
	 * Delegate for {@link VFXCellsCache#populate()}.
	 */
	public VFXList<T, C> populateCache() {
		cache.populate();
		return this;
	}

	/**
	 * Delegate for {@link ListProperty#sizeProperty()}.
	 */
	@Override
	public ReadOnlyIntegerProperty sizeProperty() {
		return items.sizeProperty();
	}

	/**
	 * Delegate for {@link ListProperty#emptyProperty()}
	 */
	@Override
	public ReadOnlyBooleanProperty emptyProperty() {
		return items.emptyProperty();
	}

	/**
	 * Delegate for {@link VFXListState#getRange()}
	 */
	public IntegerRange getRange() {
		return getState().getRange();
	}

	/**
	 * Delegate for {@link VFXListState#getCellsByIndexUnmodifiable()}
	 */
	public SequencedMap<Integer, C> getCellsByIndexUnmodifiable() {
		return getState().getCellsByIndexUnmodifiable();
	}

	/**
	 * Delegate for {@link VFXListState#getCellsByItemUnmodifiable()}
	 */
	public Map<T, C> getCellsByItemUnmodifiable() {
		return getState().getCellsByItemUnmodifiable();
	}

	@Override
	public ReadOnlyDoubleProperty virtualMaxXProperty() {
		return getHelper().virtualMaxXProperty();
	}

	@Override
	public ReadOnlyDoubleProperty virtualMaxYProperty() {
		return getHelper().virtualMaxYProperty();
	}

	/**
	 * Delegate for {@link VFXListHelper#scrollBy(double)}.
	 */
	public void scrollBy(double pixels) {
		getHelper().scrollBy(pixels);
	}

	/**
	 * Delegate for {@link VFXListHelper#scrollToPixel(double)}.
	 */
	public void scrollToPixel(double pixel) {
		getHelper().scrollToPixel(pixel);
	}

	/**
	 * Delegate for {@link VFXListHelper#scrollToIndex(int)}.
	 */
	public void scrollToIndex(int index) {
		getHelper().scrollToIndex(index);
	}

	/**
	 * Shortcut for {@code scrollToIndex(0)}.
	 */
	public void scrollToFirst() {
		scrollToIndex(0);
	}

	/**
	 * Shortcut for {@code scrollToIndex(size() - 1)}.
	 */
	public void scrollToLast() {
		scrollToIndex(size() - 1);
	}

	//================================================================================
	// Styleable Properties
	//================================================================================
	private final StyleableDoubleProperty cellSize = new StyleableDoubleProperty(
		StyleableProperties.CELL_SIZE,
		this,
		"cellSize",
		32.0
	);

	private final StyleableDoubleProperty spacing = new StyleableDoubleProperty(
		StyleableProperties.SPACING,
		this,
		"spacing",
		0.0
	);

	private final StyleableObjectProperty<BufferSize> bufferSize = new StyleableObjectProperty<>(
		StyleableProperties.BUFFER_SIZE,
		this,
		"bufferSize",
		BufferSize.standard()
	);

	private final StyleableObjectProperty<Orientation> orientation = new StyleableObjectProperty<>(
		StyleableProperties.ORIENTATION,
		this,
		"orientation"
	) {
		@Override
		protected void invalidated() {
			Orientation orientation = get();
			VFXListHelper<T, C> helper = getHelperFactory().apply(orientation);
			setHelper(helper);
		}
	};

	private final StyleableBooleanProperty fitToViewport = new StyleableBooleanProperty(
		StyleableProperties.FIT_TO_VIEWPORT,
		this,
		"fitToViewport",
		true
	);

	private final StyleableDoubleProperty clipBorderRadius = new StyleableDoubleProperty(
		StyleableProperties.CLIP_BORDER_RADIUS,
		this,
		"clipBorderRadius",
		0.0
	);

	private final StyleableIntegerProperty cacheCapacity = new StyleableIntegerProperty(
		StyleableProperties.CACHE_CAPACITY,
		this,
		"cacheCapacity",
		10
	) {
		@Override
		protected void invalidated() {
			cache.setCapacity(get());
		}
	};

	public double getCellSize() {
		return cellSize.get();
	}

	/**
	 * Specifies the cells' size:
	 * <p> - Orientation.VERTICAL: size -> height
	 * <p> - Orientation.HORIZONTAL: size -> width
	 * <p>
	 * Can be set in CSS via the property: '-vfx-cell-size'.
	 */
	public StyleableDoubleProperty cellSizeProperty() {
		return cellSize;
	}

	public void setCellSize(double cellSize) {
		this.cellSize.set(cellSize);
	}

	public double getSpacing() {
		return spacing.get();
	}

	/**
	 * Specifies the number of pixels between each cell.
	 * <p>
	 * Can be set in CSS via the property: '-vfx-spacing'
	 */
	public StyleableDoubleProperty spacingProperty() {
		return spacing;
	}

	public void setSpacing(double spacing) {
		this.spacing.set(spacing);
	}

	/**
	 * {@inheritDoc}
	 * <p></p>
	 * Can be set in CSS via the property: '-vfx-buffer-size'.
	 */
	@Override
	public StyleableObjectProperty<BufferSize> bufferSizeProperty() {
		return bufferSize;
	}

	public Orientation getOrientation() {
		return orientation.get();
	}

	/**
	 * Specifies the orientation of the virtual flow.
	 * <p>
	 * Can be set in CSS via the property: '-vfx-orientation'.
	 */
	public StyleableObjectProperty<Orientation> orientationProperty() {
		return orientation;
	}

	public void setOrientation(Orientation orientation) {
		this.orientation.set(orientation);
	}

	public boolean isFitToViewport() {
		return fitToViewport.get();
	}

	/**
	 * Specifies whether cells should be resized to be the same size of the viewport in the opposite
	 * direction of the current {@link #orientationProperty()}.
	 * <p>
	 * Can be set in CSS via the property: '-vfx-fit-to-viewport'.
	 */
	public StyleableBooleanProperty fitToViewportProperty() {
		return fitToViewport;
	}

	public void setFitToViewport(boolean fitToViewport) {
		this.fitToViewport.set(fitToViewport);
	}

	public double getClipBorderRadius() {
		return clipBorderRadius.get();
	}

	/**
	 * Used by the viewport's clip to set its border radius.
	 * This is useful when you want to make a rounded virtual flow, this
	 * prevents the content from going outside the view.
	 * <p></p>
	 * <b>Side note:</b> the clip is a {@link Rectangle}, now for some fucking reason,
	 * the rectangle's arcWidth and arcHeight values used to make it round do not act like the border-radius
	 * or background-radius properties, instead their value is usually 2 / 2.5 times the latter.
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

	public int getCacheCapacity() {
		return cacheCapacity.get();
	}

	/**
	 * Specifies the maximum number of cells the cache can contain at any time. Excess will not be added to the queue and
	 * disposed immediately.
	 * <p>
	 * Can be set in CSS via the property: '-vfx-cache-capacity'.
	 */
	public StyleableIntegerProperty cacheCapacityProperty() {
		return cacheCapacity;
	}

	public void setCacheCapacity(int cacheCapacity) {
		this.cacheCapacity.set(cacheCapacity);
	}

	//================================================================================
	// CssMetaData
	//================================================================================
	private static class StyleableProperties {
		private static final StyleablePropertyFactory<VFXList<?, ?>> FACTORY = new StyleablePropertyFactory<>(Control.getClassCssMetaData());
		private static final List<CssMetaData<? extends Styleable, ?>> cssMetaDataList;

		private static final CssMetaData<VFXList<?, ?>, Number> CELL_SIZE =
			FACTORY.createSizeCssMetaData(
				"-vfx-cell-size",
				VFXList::cellSizeProperty,
				32.0
			);

		private static final CssMetaData<VFXList<?, ?>, Number> SPACING =
			FACTORY.createSizeCssMetaData(
				"-vfx-spacing",
				VFXList::spacingProperty,
				0.0
			);

		private static final CssMetaData<VFXList<?, ?>, BufferSize> BUFFER_SIZE =
			FACTORY.createEnumCssMetaData(
				BufferSize.class,
				"-vfx-buffer-size",
				VFXList::bufferSizeProperty,
				BufferSize.standard()
			);

		private static final CssMetaData<VFXList<?, ?>, Orientation> ORIENTATION =
			FACTORY.createEnumCssMetaData(
				Orientation.class,
				"-vfx-orientation",
				VFXList::orientationProperty,
				Orientation.VERTICAL
			);

		private static final CssMetaData<VFXList<?, ?>, Boolean> FIT_TO_VIEWPORT =
			FACTORY.createBooleanCssMetaData(
				"-vfx-fit-to-viewport",
				VFXList::fitToViewportProperty,
				true
			);

		private static final CssMetaData<VFXList<?, ?>, Number> CLIP_BORDER_RADIUS =
			FACTORY.createSizeCssMetaData(
				"-vfx-clip-border-radius",
				VFXList::clipBorderRadiusProperty,
				0.0
			);

		private static final CssMetaData<VFXList<?, ?>, Number> CACHE_CAPACITY =
			FACTORY.createSizeCssMetaData(
				"-vfx-cache-capacity",
				VFXList::cacheCapacityProperty,
				10
			);

		static {
			cssMetaDataList = StyleUtils.cssMetaDataList(
				Control.getClassCssMetaData(),
				CELL_SIZE, SPACING, BUFFER_SIZE, ORIENTATION, FIT_TO_VIEWPORT, CLIP_BORDER_RADIUS, CACHE_CAPACITY
			);
		}
	}

	public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
		return StyleableProperties.cssMetaDataList;
	}

	@Override
	protected List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
		return getClassCssMetaData();
	}

	//================================================================================
	// Getters/Setters
	//================================================================================

	/**
	 * @return the cache instance used by this container
	 */
	protected VFXCellsCache<T, C> getCache() {
		return cache;
	}

	/**
	 * Delegate for {@link VFXCellsCache#size()}.
	 */
	public int cacheSize() {
		return cache.size();
	}

	@Override
	public ListProperty<T> itemsProperty() {
		return items;
	}

	public Function<T, C> getCellFactory() {
		return cellFactory.get();
	}

	/**
	 * Specifies the function used to build the cells.
	 */
	public FunctionProperty<T, C> cellFactoryProperty() {
		return cellFactory;
	}

	public void setCellFactory(Function<T, C> cellFactory) {
		this.cellFactory.set(cellFactory);
	}

	public VFXListHelper<T, C> getHelper() {
		return helper.get();
	}

	/**
	 * Specifies the instance of the {@link VFXListHelper} built by the {@link #helperFactoryProperty()}.
	 */
	public ReadOnlyObjectProperty<VFXListHelper<T, C>> helperProperty() {
		return helper.getReadOnlyProperty();
	}

	protected void setHelper(VFXListHelper<T, C> helper) {
		this.helper.set(helper);
	}

	public Function<Orientation, VFXListHelper<T, C>> getHelperFactory() {
		return helperFactory.get();
	}

	/**
	 * Specifies the function used to build a {@link VFXListHelper} instance depending on the container's orientation.
	 */
	public FunctionProperty<Orientation, VFXListHelper<T, C>> helperFactoryProperty() {
		return helperFactory;
	}

	public void setHelperFactory(Function<Orientation, VFXListHelper<T, C>> helperFactory) {
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

	public VFXListState<T, C> getState() {
		return state.get();
	}

	/**
	 * Specifies the container's current state. The state carries useful information such as the range of displayed items
	 * and the cells ordered by index, or by item (not ordered).
	 */
	public ReadOnlyObjectProperty<VFXListState<T, C>> stateProperty() {
		return state.getReadOnlyProperty();
	}

	protected void setState(VFXListState<T, C> state) {
		this.state.set(state);
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
}
