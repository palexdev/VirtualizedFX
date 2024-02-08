package io.github.palexdev.virtualizedfx.grid;

import io.github.palexdev.mfxcore.base.beans.Size;
import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.base.properties.functional.FunctionProperty;
import io.github.palexdev.mfxcore.base.properties.functional.SupplierProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableDoubleProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableIntegerProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableObjectProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableSizeProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableSizeProperty.SizeConverter;
import io.github.palexdev.mfxcore.controls.Control;
import io.github.palexdev.mfxcore.controls.SkinBase;
import io.github.palexdev.mfxcore.utils.PositionUtils;
import io.github.palexdev.mfxcore.utils.fx.PropUtils;
import io.github.palexdev.mfxcore.utils.fx.StyleUtils;
import io.github.palexdev.virtualizedfx.base.VFXContainer;
import io.github.palexdev.virtualizedfx.cells.Cell;
import io.github.palexdev.virtualizedfx.enums.BufferSize;
import io.github.palexdev.virtualizedfx.list.VFXListHelper;
import io.github.palexdev.virtualizedfx.properties.VFXGridStateProperty;
import io.github.palexdev.virtualizedfx.utils.VFXCellsCache;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.css.StyleablePropertyFactory;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.shape.Rectangle;

import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings({"rawtypes", "unchecked"})
public class VFXGrid<T, C extends Cell<T>> extends Control<VFXGridManager<T, C>> implements VFXContainer<T> {
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
	private final ReadOnlyObjectWrapper<VFXGridHelper<T, C>> helper = new ReadOnlyObjectWrapper<>() {
		@Override
		public void set(VFXGridHelper<T, C> newValue) {
			if (newValue == null)
				throw new NullPointerException("Grid helper cannot be null!");
			VFXGridHelper<T, C> oldValue = get();
			if (oldValue != null) oldValue.dispose();
			super.set(newValue);
		}
	};
	private final SupplierProperty<VFXGridHelper<T, C>> helperFactory = new SupplierProperty<>(defaultHelperFactory()) {
		@Override
		public void set(Supplier<VFXGridHelper<T, C>> newValue) {
			if (newValue == null)
				throw new NullPointerException("Grid helper factory cannot be null!");
			super.set(newValue);
		}

		@Override
		protected void invalidated() {
			VFXGridHelper<T, C> helper = get().get();
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

	private final VFXGridStateProperty<T, C> state = new VFXGridStateProperty<>(VFXGridState.EMPTY);
	private final ReadOnlyBooleanWrapper needsViewportLayout = new ReadOnlyBooleanWrapper(false);

	//================================================================================
	// Constructors
	//================================================================================
	public VFXGrid() {
		cache = createCache();
		initialize();
	}

	public VFXGrid(ObservableList<T> items, Function<T, C> cellFactory) {
		this();
		setItems(items);
		setCellFactory(cellFactory);
	}

	//================================================================================
	// Methods
	//================================================================================
	private void initialize() {
		getStyleClass().addAll(defaultStyleClasses());
		setDefaultBehaviorProvider();
		setHelper(getHelperFactory().get());
	}

	public void autoArrange() {
		autoArrange(0);
	}

	public void autoArrange(int min) {
		double cellWidth = getCellSize().getWidth();
		double hSpacing = getHSpacing();
		int nColumns = (int) Math.max(Math.max(0, min), Math.floor(getWidth() / (cellWidth + hSpacing)));
		setColumnsNum(nColumns);
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

	protected void update(VFXGridState<T, C> state) {
		setState(state);
	}

	protected Supplier<VFXGridHelper<T, C>> defaultHelperFactory() {
		return () -> new VFXGridHelper.DefaultHelper<>(this);
	}

	public void requestViewportLayout() {
		setNeedsViewportLayout(true);
	}

	//================================================================================
	// Overridden Methods
	//================================================================================
	@Override
	public List<String> defaultStyleClasses() {
		return List.of("vfx-grid");
	}

	@Override
	protected SkinBase<?, ?> buildSkin() {
		return new VFXGridSkin<>(this);
	}

	@Override
	public Supplier<VFXGridManager<T, C>> defaultBehaviorProvider() {
		return () -> new VFXGridManager<>(this);
	}

	//================================================================================
	// Delegate Methods
	//================================================================================

	/**
	 * Delegate for {@link VFXCellsCache#populate()}.
	 */
	public VFXGrid<T, C> populateCache() {
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

	public IntegerRange getRowsRange() {return getState().getRowsRange();}

	public IntegerRange getColumnsRange() {return getState().getColumnsRange();}

	public SequencedMap<Integer, C> getCellsByIndexUnmodifiable() {return getState().getCellsByIndexUnmodifiable();}

	public Map<T, C> getCellsByItemUnmodifiable() {return getState().getCellsByItemUnmodifiable();}

	/**
	 * Delegate for {@link VFXListHelper#virtualMaxXProperty()}.
	 */
	public ReadOnlyDoubleProperty virtualMaxXProperty() {
		return getHelper().virtualMaxXProperty();
	}

	/**
	 * Delegate for {@link VFXListHelper#virtualMaxYProperty()}.
	 */
	public ReadOnlyDoubleProperty virtualMaxYProperty() {
		return getHelper().virtualMaxYProperty();
	}

	public void scrollToRow(int row) {
		getHelper().scrollToRow(row);
	}

	public void scrollToColumn(int column) {
		getHelper().scrollToColumn(column);
	}

	public void scrollToFirstRow() {
		scrollToRow(0);
	}

	public void scrollToLastRow() {
		scrollToRow(Integer.MAX_VALUE);
	}

	public void scrollToFirstColumn() {
		scrollToColumn(0);
	}

	public void scrollToLastColumn() {
		scrollToColumn(Integer.MAX_VALUE);
	}

	//================================================================================
	// Styleable Properties
	//================================================================================
	private final StyleableSizeProperty cellSize = new StyleableSizeProperty(
		StyleableProperties.CELL_SIZE,
		this,
		"cellSize",
		Size.of(100, 100)
	);

	private final StyleableIntegerProperty columnsNum = new StyleableIntegerProperty(
		StyleableProperties.COLUMNS_NUM,
		this,
		"columnsNum",
		5
	);

	private final StyleableObjectProperty<Pos> alignment = new StyleableObjectProperty<>(
		StyleableProperties.ALIGNMENT,
		this,
		"alignment",
		Pos.TOP_LEFT
	) {
		@Override
		public void set(Pos v) {
			if (PositionUtils.isBaseline(v)) v = Pos.TOP_LEFT;
			super.set(v);
		}
	};

	private final StyleableDoubleProperty hSpacing = new StyleableDoubleProperty(
		StyleableProperties.H_SPACING,
		this,
		"hSpacing",
		0.0
	);

	private final StyleableDoubleProperty vSpacing = new StyleableDoubleProperty(
		StyleableProperties.V_SPACING,
		this,
		"vSpacing",
		0.0
	);

	private final StyleableObjectProperty<BufferSize> bufferSize = new StyleableObjectProperty<>(
		StyleableProperties.BUFFER_SIZE,
		this,
		"bufferSize",
		BufferSize.standard()
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

	public Size getCellSize() {
		return cellSize.get();
	}

	public StyleableSizeProperty cellSizeProperty() {
		return cellSize;
	}

	public void setCellSize(Size cellSize) {
		this.cellSize.set(cellSize);
	}

	public void setCellSize(double w, double h) {
		setCellSize(Size.of(w, h));
	}

	public void setCellSize(double size) {
		setCellSize(Size.of(size, size));
	}

	public int getColumnsNum() {
		return columnsNum.get();
	}

	public StyleableIntegerProperty columnsNumProperty() {
		return columnsNum;
	}

	public void setColumnsNum(int columnsNum) {
		this.columnsNum.set(columnsNum);
	}

	public Pos getAlignment() {
		return alignment.get();
	}

	public StyleableObjectProperty<Pos> alignmentProperty() {
		return alignment;
	}

	public void setAlignment(Pos alignment) {
		this.alignment.set(alignment);
	}

	public double getHSpacing() {
		return hSpacing.get();
	}

	/**
	 * Specifies the horizontal number of pixels between each cell.
	 * <p>
	 * Can be set in CSS via the property: '-vfx-h-spacing'
	 */
	public StyleableDoubleProperty hSpacingProperty() {
		return hSpacing;
	}

	public void setHSpacing(double spacing) {
		this.hSpacing.set(spacing);
	}

	public double getVSpacing() {
		return vSpacing.get();
	}

	/**
	 * Specifies the vertical number of pixels between each cell.
	 * <p>
	 * Can be set in CSS via the property: '-vfx-v-spacing'
	 */
	public StyleableDoubleProperty vSpacingProperty() {
		return vSpacing;
	}

	public void setVSpacing(double spacing) {
		this.vSpacing.set(spacing);
	}

	/**
	 * Convenience method to set both vertical and horizontal spacing to the given value.
	 *
	 * @see #hSpacingProperty()
	 * @see #vSpacingProperty()
	 */
	public void setSpacing(double spacing) {
		setHSpacing(spacing);
		setVSpacing(spacing);
	}

	/**
	 * Convenience method to set both vertical and horizontal spacing to the given values.
	 *
	 * @see #hSpacingProperty()
	 * @see #vSpacingProperty()
	 */
	public void setSpacing(double hSpacing, double vSpacing) {
		setHSpacing(hSpacing);
		setVSpacing(vSpacing);
	}

	/**
	 * {@inheritDoc}
	 * <p></p>
	 * Can be set in CSS via the property: '-vfx-buffer-size'.
	 */
	public StyleableObjectProperty<BufferSize> bufferSizeProperty() {
		return bufferSize;
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
		private static final StyleablePropertyFactory<VFXGrid<?, ?>> FACTORY = new StyleablePropertyFactory<>(Control.getClassCssMetaData());
		private static final List<CssMetaData<? extends Styleable, ?>> cssMetaDataList;

		private static final CssMetaData<VFXGrid<?, ?>, Size> CELL_SIZE =
			new CssMetaData<>("-vfx-cell-size", SizeConverter.getInstance(), Size.of(100, 100)) {
				@Override
				public boolean isSettable(VFXGrid<?, ?> styleable) {
					return !styleable.cellSizeProperty().isBound();
				}

				@Override
				public StyleableProperty<Size> getStyleableProperty(VFXGrid<?, ?> styleable) {
					return styleable.cellSizeProperty();
				}
			};

		private static final CssMetaData<VFXGrid<?, ?>, Number> COLUMNS_NUM =
			FACTORY.createSizeCssMetaData(
				"-vfx-columns-num",
				VFXGrid::columnsNumProperty,
				5
			);

		private static final CssMetaData<VFXGrid<?, ?>, Pos> ALIGNMENT =
			FACTORY.createEnumCssMetaData(
				Pos.class,
				"-vfx-alignment",
				VFXGrid::alignmentProperty,
				Pos.TOP_LEFT
			);

		private static final CssMetaData<VFXGrid<?, ?>, Number> H_SPACING =
			FACTORY.createSizeCssMetaData(
				"-vfx-h-spacing",
				VFXGrid::hSpacingProperty,
				0.0
			);

		private static final CssMetaData<VFXGrid<?, ?>, Number> V_SPACING =
			FACTORY.createSizeCssMetaData(
				"-vfx-v-spacing",
				VFXGrid::vSpacingProperty,
				0.0
			);

		private static final CssMetaData<VFXGrid<?, ?>, Number> CLIP_BORDER_RADIUS =
			FACTORY.createSizeCssMetaData(
				"-vfx-clip-border-radius",
				VFXGrid::clipBorderRadiusProperty,
				0.0
			);

		private static final CssMetaData<VFXGrid<?, ?>, Number> CACHE_CAPACITY =
			FACTORY.createSizeCssMetaData(
				"-vfx-cache-capacity",
				VFXGrid::cacheCapacityProperty,
				10
			);


		private static final CssMetaData<VFXGrid<?, ?>, BufferSize> BUFFER_SIZE =
			FACTORY.createEnumCssMetaData(
				BufferSize.class,
				"-vfx-buffer-size",
				VFXGrid::bufferSizeProperty,
				BufferSize.standard()
			);

		static {
			cssMetaDataList = StyleUtils.cssMetaDataList(
				Control.getClassCssMetaData(),
				CELL_SIZE, COLUMNS_NUM, ALIGNMENT, H_SPACING, V_SPACING,
				BUFFER_SIZE, CACHE_CAPACITY, CLIP_BORDER_RADIUS
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

	/**
	 * {@inheritDoc}
	 * <p></p>
	 * Also, despite the grid being a 2D container, we still use a 1D collection because it's much more easy and convenient
	 * to use. Knowing the number of columns we want to divide the items by, it's enough to make the list act as a 2D collection.
	 */
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

	public VFXGridHelper<T, C> getHelper() {
		return helper.get();
	}

	public ReadOnlyObjectProperty<VFXGridHelper<T, C>> helperProperty() {
		return helper.getReadOnlyProperty();
	}

	protected void setHelper(VFXGridHelper<T, C> helper) {
		this.helper.set(helper);
	}

	public Supplier<VFXGridHelper<T, C>> getHelperFactory() {
		return helperFactory.get();
	}

	public SupplierProperty<VFXGridHelper<T, C>> helperFactoryProperty() {
		return helperFactory;
	}

	public void setHelperFactory(Supplier<VFXGridHelper<T, C>> helperFactory) {
		this.helperFactory.set(helperFactory);
	}

	/**
	 * Specifies the container's vertical position.
	 * <p>
	 * In the case of the Grid, both directions are virtualized.
	 */
	public DoubleProperty vPosProperty() {
		return vPos;
	}

	/**
	 * Specifies the container's horizontal position. In case the orientation is set to {@link Orientation#HORIZONTAL}, this
	 * <p>
	 * In the case of the Grid, both directions are virtualized.
	 */
	public DoubleProperty hPosProperty() {
		return hPos;
	}

	public VFXGridState<T, C> getState() {
		return state.get();
	}

	/**
	 * Specifies the container's current state. The state carries useful information such as the range of displayed items
	 * and the cells ordered by index, or by item (not ordered).
	 */
	public ReadOnlyObjectProperty<VFXGridState<T, C>> stateProperty() {
		return state.getReadOnlyProperty();
	}

	protected void setState(VFXGridState<T, C> state) {
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
