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
import io.github.palexdev.virtualizedfx.cells.Cell;
import io.github.palexdev.virtualizedfx.enums.BufferSize;
import io.github.palexdev.virtualizedfx.list.VirtualizedListHelper.HorizontalHelper;
import io.github.palexdev.virtualizedfx.list.VirtualizedListHelper.VerticalHelper;
import io.github.palexdev.virtualizedfx.properties.VirtualizedListStateProperty;
import javafx.beans.InvalidationListener;
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
 * Extends {@link Control}, has its own skin implementation {@link VirtualizedListSkin} and behavior {@link VirtualizedListManager}.
 * Uses cells of type {@link Cell}.
 * <p>
 * This is a stateful component, meaning that every meaningful variable (position, size, cell size, etc.) will produce a new
 * {@link VirtualizedListState}. The state determines which and how items are displayed in the container.
 * <p></p>
 * <b>Features & Implementation Details</b>
 * <p> - The default behavior implementation, {@link VirtualizedListManager}, can be considered as the name suggests more like
 * a 'manager' than an actual behavior. It is responsible for reacting to core changes here in the control to produce a new
 * state. The state can be considered like a picture of the container at a certain time. Each combination of the variables
 * that influence the way items are shown (how many, start, end, changes in the list, etc.) produce a specific state.
 * This is an important concept as some of the features I'm going to mention below are due to the combination of default
 * skin + default behavior. You are allowed to change/customize the skin and the behavior as you please.
 * <p> - The items list is managed automatically (permutations, insertions, removals, updates). Compared to previous
 * algorithms the {@link VirtualizedListManager} implements a much simpler strategy while still trying to keep the cell updates
 * count as low as possible to improve performance. See {@link VirtualizedListManager#onItemsChanged()}.
 * <p> - The function used to generate the cells, called "cellFactory", can be changed anytime, even at runtime,
 * see {@link VirtualizedListManager#onCellFactoryChanged()}.
 * <p> - The component is based on a fixed size for all the cell, this parameter can be controlled through the {@link #cellSizeProperty()},
 * and can also be changed anytime, see {@link VirtualizedListManager#onCellSizeChanged()}.
 * <p> - Similar to the {@code VBox} pane, this container allows you to evenly space the cells in the viewport by setting the
 * {@link #spacingProperty()}. See {@link VirtualizedListManager#onSpacingChanged()}.
 * <p> - The container can be oriented either vertically or horizontally through the {@link #orientationProperty()}.
 * Depending on the orientation, the layout and other computations change. Thanks to polymorphism, it's possible
 * to define a public API for such computations and implement two separate classes for each orientation, this is the
 * {@link VirtualizedListHelper}. You are allowed to change the helper through the {@link #helperFactoryProperty()}.
 * <p> - The vertical and horizontal positions are available through the properties, {@link #vPosProperty()} and {@link #hPosProperty()}.
 * It was indeed possible to use a single property for the position, but they are split for performance reasons. Since the
 * cells are arranged according to the container's orientation, there is a distinction between the width and height.
 * For this reason, the terms width and height are not used much; instead we define the length as the number
 * of pixels along the orientation (vertical -> y-axis, horizontal -> x-axis), and the breadth as the number of pixels
 * in the opposite direction. Given these two concepts, there are two related properties: the estimated length and the
 * max breadth. The {@link #estimatedLengthProperty()} is the total number of pixels given by the number of cells and their
 * size. The {@link #maxBreadthProperty()} is the max breadth among all the cells. This is typically a dynamic property
 * as the breadth of a cell is only computed during its layout.
 * <p> - You can access the current state through the {@link #stateProperty()}. The state gives crucial information about
 * the container such as the range of displayed items and the visible cells (by index and by item). If you'd like to observe
 * for changes in the displaying cells, you want to add a listener on this property.
 * <p> - It is possible to programmatically tell the viewport to update its layout with {@link #requestViewportLayout()},
 * although this should never be necessary as it is handled automatically when the state changes.
 * <p> - Additionally, this container makes use of a simple cache implementation, {@link VirtualizedListCache}, which
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
public class VirtualizedList<T, C extends Cell<T>> extends Control<VirtualizedListManager<T, C>> {
	//================================================================================
	// Properties
	//================================================================================
	private final VirtualizedListCache<T, C> cache;
	private final ListProperty<T> items = new SimpleListProperty<>(FXCollections.observableArrayList()) {
		@Override
		public void set(ObservableList<T> newValue) {
			if (newValue == null) newValue = FXCollections.observableArrayList();
			super.set(newValue);
		}
	};
	private final FunctionProperty<T, C> cellFactory = new FunctionProperty<>();
	private final ReadOnlyObjectWrapper<VirtualizedListHelper<T, C>> helper = new ReadOnlyObjectWrapper<>() {
		@Override
		public void set(VirtualizedListHelper<T, C> newValue) {
			if (newValue == null)
				throw new NullPointerException("List helper cannot be null!");
			VirtualizedListHelper<T, C> oldValue = get();
			if (oldValue != null) oldValue.dispose();
			super.set(newValue);
		}
	};
	private final FunctionProperty<Orientation, VirtualizedListHelper<T, C>> helperFactory = new FunctionProperty<>(o ->
		(o == Orientation.VERTICAL) ?
			new VerticalHelper<>(VirtualizedList.this) :
			new HorizontalHelper<>(VirtualizedList.this)
	) {
		@Override
		public void set(Function<Orientation, VirtualizedListHelper<T, C>> newValue) {
			if (newValue == null)
				throw new NullPointerException("List helper factory cannot be null");
			super.set(newValue);
		}

		@Override
		protected void invalidated() {
			Orientation orientation = getOrientation();
			VirtualizedListHelper<T, C> helper = get().apply(orientation);
			setHelper(helper);
		}
	};
	private final DoubleProperty vPos = PropUtils.clampedDoubleProperty(
		() -> 0.0,
		() -> (getHelper() != null) ? getHelper().maxVScroll() : 0.0
	);
	private final DoubleProperty hPos = PropUtils.clampedDoubleProperty(
		() -> 0.0,
		() -> (getHelper() != null) ? getHelper().maxHScroll() : 0.0
	);

	private final VirtualizedListStateProperty<T, C> state = new VirtualizedListStateProperty<>(VirtualizedListState.EMPTY);
	private final ReadOnlyBooleanWrapper needsViewportLayout = new ReadOnlyBooleanWrapper(false);

	//================================================================================
	// Constructors
	//================================================================================
	public VirtualizedList() {
		cache = createCache();
		setOrientation(Orientation.VERTICAL);
		initialize();
	}

	public VirtualizedList(ObservableList<T> items, Function<T, C> cellFactory) {
		this();
		setItems(items);
		setCellFactory(cellFactory);
	}

	public VirtualizedList(ObservableList<T> items, Function<T, C> cellFactory, Orientation orientation) {
		this();
		setItems(items);
		setCellFactory(cellFactory);
		setOrientation(orientation);
	}

	//================================================================================
	// Methods
	//================================================================================
	private void initialize() {
		getStyleClass().add("virtualized-list");
		setDefaultBehaviorProvider();
	}

	/**
	 * Responsible for creating the cache instance used by this container.
	 *
	 * @see VirtualizedListCache
	 * @see #cacheCapacityProperty()
	 */
	protected VirtualizedListCache<T, C> createCache() {
		return new VirtualizedListCache<>(this, getCacheCapacity());
	}

	/**
	 * Setter for the {@link #stateProperty()}.
	 */
	protected void update(VirtualizedListState<T, C> state) {
		setState(state);
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
	protected SkinBase<?, ?> buildSkin() {
		return new VirtualizedListSkin<>(this);
	}

	@Override
	public Supplier<VirtualizedListManager<T, C>> defaultBehaviorProvider() {
		return () -> new VirtualizedListManager<>(this);
	}

	//================================================================================
	// Delegate Methods
	//================================================================================

	/**
	 * Delegate for {@link ListProperty#size()}.
	 */
	public int size() {
		return getItems().size();
	}

	/**
	 * Delegate for {@link ListProperty#sizeProperty()}.
	 */
	public ReadOnlyIntegerProperty sizeProperty() {
		return items.sizeProperty();
	}

	/**
	 * Delegate for {@link ListProperty#isEmpty()}.
	 */
	public boolean isEmpty() {
		return getItems().isEmpty();
	}

	/**
	 * Delegate for {@link ListProperty#emptyProperty()}
	 */
	public ReadOnlyBooleanProperty emptyProperty() {
		return items.emptyProperty();
	}

	/**
	 * Delegate for {@link VirtualizedListState#getRange()}
	 */
	public IntegerRange getRange() {
		return getState().getRange();
	}

	/**
	 * Delegate for {@link VirtualizedListState#getCellsByIndexUnmodifiable()}
	 */
	public SequencedMap<Integer, C> getCellsByIndexUnmodifiable() {
		return getState().getCellsByIndexUnmodifiable();
	}

	/**
	 * Delegate for {@link VirtualizedListState#getCellsByItemUnmodifiable()}
	 */
	public Map<T, C> getCellsByItemUnmodifiable() {
		return getState().getCellsByItemUnmodifiable();
	}

	/**
	 * Delegate for {@link VirtualizedListHelper#estimatedLengthProperty()}.
	 */
	public ReadOnlyDoubleProperty estimatedLengthProperty() {
		return getHelper().estimatedLengthProperty();
	}

	/**
	 * Delegate for {@link VirtualizedListHelper#maxBreadthProperty()}.
	 */
	public ReadOnlyDoubleProperty maxBreadthProperty() {
		return getHelper().maxBreadthProperty();
	}

	/**
	 * Delegate for {@link VirtualizedListHelper#scrollBy(double)}.
	 */
	public void scrollBy(double pixels) {
		getHelper().scrollBy(pixels);
	}

	/**
	 * Delegate for {@link VirtualizedListHelper#scrollToPixel(double)}.
	 */
	public void scrollToPixel(double pixel) {
		getHelper().scrollToPixel(pixel);
	}

	/**
	 * Delegate for {@link VirtualizedListHelper#scrollToIndex(int)}.
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
			VirtualizedListHelper<T, C> helper = getHelperFactory().apply(orientation);
			setHelper(helper);
		}
	};

	private final StyleableBooleanProperty fitToBreadth = new StyleableBooleanProperty(
		StyleableProperties.FIT_TO_BREADTH,
		this,
		"fitToBreadth",
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

	public BufferSize getBufferSize() {
		return bufferSize.get();
	}

	/**
	 * Specifies the number of extra cells to add to the container; they act as a buffer, allowing scroll to be smoother.
	 * To avoid edge cases due to the users abusing the system, this is done by using an enumerator which allows up to
	 * three buffer cells. Also, the default implementation (see {@link VerticalHelper} or {@link HorizontalHelper}), adds
	 * double the number specified by the enum constant, because these buffer cells are added both at the top and at the
	 * bottom of the container. The default value is {@link BufferSize#MEDIUM}.
	 * <p>
	 * Can be set in CSS via the property: '-vfx-buffer-size'.
	 */
	public StyleableObjectProperty<BufferSize> bufferSizeProperty() {
		return bufferSize;
	}

	public void setBufferSize(BufferSize bufferSize) {
		this.bufferSize.set(bufferSize);
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

	public boolean isFitToBreadth() {
		return fitToBreadth.get();
	}

	/**
	 * Specifies whether cells should be resized to be the same size of the viewport in the opposite
	 * direction of the current {@link #orientationProperty()}.
	 * <p>
	 * Can be set in CSS via the property: '-vfx-fit-to-breadth'.
	 */
	public StyleableBooleanProperty fitToBreadthProperty() {
		return fitToBreadth;
	}

	public void setFitToBreadth(boolean fitToBreadth) {
		this.fitToBreadth.set(fitToBreadth);
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
		private static final StyleablePropertyFactory<VirtualizedList<?, ?>> FACTORY = new StyleablePropertyFactory<>(Control.getClassCssMetaData());
		private static final List<CssMetaData<? extends Styleable, ?>> cssMetaDataList;

		private static final CssMetaData<VirtualizedList<?, ?>, Number> CELL_SIZE =
			FACTORY.createSizeCssMetaData(
				"-vfx-cell-size",
				VirtualizedList::cellSizeProperty,
				32.0
			);

		private static final CssMetaData<VirtualizedList<?, ?>, Number> SPACING =
			FACTORY.createSizeCssMetaData(
				"-vfx-spacing",
				VirtualizedList::spacingProperty,
				0.0
			);

		private static final CssMetaData<VirtualizedList<?, ?>, BufferSize> BUFFER_SIZE =
			FACTORY.createEnumCssMetaData(
				BufferSize.class,
				"-vfx-buffer-size",
				VirtualizedList::bufferSizeProperty,
				BufferSize.standard()
			);

		private static final CssMetaData<VirtualizedList<?, ?>, Orientation> ORIENTATION =
			FACTORY.createEnumCssMetaData(
				Orientation.class,
				"-vfx-orientation",
				VirtualizedList::orientationProperty,
				Orientation.VERTICAL
			);

		private static final CssMetaData<VirtualizedList<?, ?>, Boolean> FIT_TO_BREADTH =
			FACTORY.createBooleanCssMetaData(
				"-vfx-fit-to-breadth",
				VirtualizedList::fitToBreadthProperty,
				true
			);


		private static final CssMetaData<VirtualizedList<?, ?>, Number> CLIP_BORDER_RADIUS =
			FACTORY.createSizeCssMetaData(
				"-vfx-clip-border-radius",
				VirtualizedList::clipBorderRadiusProperty,
				0.0
			);

		private static final CssMetaData<VirtualizedList<?, ?>, Number> CACHE_CAPACITY =
			FACTORY.createSizeCssMetaData(
				"-vfx-cache-capacity",
				VirtualizedList::cacheCapacityProperty,
				10
			);

		static {
			cssMetaDataList = StyleUtils.cssMetaDataList(
				Control.getClassCssMetaData(),
				CELL_SIZE, SPACING, BUFFER_SIZE, ORIENTATION, FIT_TO_BREADTH, CLIP_BORDER_RADIUS, CACHE_CAPACITY
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
	public VirtualizedListCache<T, C> getCache() {
		return cache;
	}

	public ObservableList<T> getItems() {
		return items.get();
	}

	/**
	 * Specifies the {@link ObservableList} used to store the items.
	 * <p>
	 * We use a {@link ListProperty} because it offers many commodities such as both the size and emptiness of the list
	 * as observable properties, as well as the possibility of adding an {@link InvalidationListener} that will both inform
	 * about changes of the property and in the list.
	 */
	public ListProperty<T> itemsProperty() {
		return items;
	}

	public void setItems(ObservableList<T> items) {
		this.items.set(items);
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

	public VirtualizedListHelper<T, C> getHelper() {
		return helper.get();
	}

	/**
	 * Specifies the instance of the {@link VirtualizedListHelper} built by the {@link #helperFactoryProperty()}.
	 */
	public ReadOnlyObjectProperty<VirtualizedListHelper<T, C>> helperProperty() {
		return helper.getReadOnlyProperty();
	}

	protected void setHelper(VirtualizedListHelper<T, C> helper) {
		this.helper.set(helper);
	}

	public Function<Orientation, VirtualizedListHelper<T, C>> getHelperFactory() {
		return helperFactory.get();
	}

	/**
	 * Specifies the function used to build a {@link VirtualizedListHelper} instance depending on the container's orientation.
	 */
	public FunctionProperty<Orientation, VirtualizedListHelper<T, C>> helperFactoryProperty() {
		return helperFactory;
	}

	public void setHelperFactory(Function<Orientation, VirtualizedListHelper<T, C>> helperFactory) {
		this.helperFactory.set(helperFactory);
	}

	public double getVPos() {
		return vPos.get();
	}

	/**
	 * Specifies the container's vertical position. In case the orientation is set to {@link Orientation#VERTICAL}, this
	 * is to be considered a 'virtual' position, as the container will never reach unreasonably high values for performance
	 * reasons. See {@link VerticalHelper} to understand how virtual scroll is handled.
	 */
	public DoubleProperty vPosProperty() {
		return vPos;
	}

	public void setVPos(double vPos) {
		this.vPos.set(vPos);
	}

	public double getHPos() {
		return hPos.get();
	}

	/**
	 * Specifies the container's horizontal position. In case the orientation is set to {@link Orientation#HORIZONTAL}, this
	 * is to be considered a 'virtual' position, as the container will never reach unreasonably high values for performance
	 * reasons. See {@link HorizontalHelper} to understand how virtual scroll is handled.
	 */
	public DoubleProperty hPosProperty() {
		return hPos;
	}

	public void setHPos(double hPos) {
		this.hPos.set(hPos);
	}

	public VirtualizedListState<T, C> getState() {
		return state.get();
	}

	/**
	 * Specifies the container's current state. The state carries useful information such as the range of displayed items
	 * and the cells ordered by index, or by item (not ordered).
	 */
	public ReadOnlyObjectProperty<VirtualizedListState<T, C>> stateProperty() {
		return state.getReadOnlyProperty();
	}

	protected void setState(VirtualizedListState<T, C> state) {
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
