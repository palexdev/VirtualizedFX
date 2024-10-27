/*
 * Copyright (C) 2024 Parisi Alessandro - alessandro.parisi406@gmail.com
 * This file is part of VirtualizedFX (https://github.com/palexdev/VirtualizedFX)
 *
 * VirtualizedFX is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 *
 * VirtualizedFX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with VirtualizedFX. If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.palexdev.virtualizedfx.grid;

import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.function.Function;
import java.util.function.Supplier;

import io.github.palexdev.mfxcore.base.beans.Size;
import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
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
import io.github.palexdev.virtualizedfx.base.VFXScrollable;
import io.github.palexdev.virtualizedfx.base.VFXStyleable;
import io.github.palexdev.virtualizedfx.base.WithCellFactory;
import io.github.palexdev.virtualizedfx.cells.base.VFXCell;
import io.github.palexdev.virtualizedfx.controls.VFXScrollPane;
import io.github.palexdev.virtualizedfx.enums.BufferSize;
import io.github.palexdev.virtualizedfx.events.VFXContainerEvent;
import io.github.palexdev.virtualizedfx.properties.CellFactory;
import io.github.palexdev.virtualizedfx.properties.VFXGridStateProperty;
import io.github.palexdev.virtualizedfx.utils.VFXCellsCache;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.css.StyleablePropertyFactory;
import javafx.geometry.Pos;
import javafx.scene.shape.Rectangle;

/**
 * Implementation of a virtualized container to show a list of items in a "2D" perspective.
 * The default style class is: '.vfx-grid'.
 * <p>
 * Extends {@link Control}, implements {@link VFXContainer}, has its own skin implementation {@link VFXGridSkin}
 * and behavior {@link VFXGridManager}. Uses cells of type {@link VFXCell}.
 * <p>
 * This is a stateful component, meaning that every meaningful variable (position, size, cell size, etc.) will produce a new
 * {@link VFXGridState} when changing. The state determines how and which items are displayed in the container.
 * <p></p>
 * <b>Features {@literal &} Implementation Details</b>
 * <p> - First and foremost, it's important to describe how the grid works and why it's made as it is. The grid arranges
 * the contents of a simple 1D data structure (a list) in a 2D way. <b>(History time)</b> The previous implementation used a 2D data structure
 * instead which indeed made some algorithms easier to implement, but made its usage very inconvenient for one simple reason:
 * the data structure was not flexible enough. To add a row/column, they needed to have the same size of the data structure,
 * so in practice, if you had, for example, a half-full row/column to add, you had to fill it with {@code null} elements.
 * The same issue occurred for the data structure creation, the source list/array had to be exactly the size given by
 * 'nRows * nColumns'. <b>(End of history time)</b> So the question is, if we now use a simple 1D structure now
 * (which is more flexible and easier to use for the end-user), how can the grid arrange the contents in a 2D way? Well,
 * the answer is pretty straightforward; we need a value that the big dumb-dumb me of the past didn't think about:
 * the {@link #columnsNumProperty()}. Given the desired number of columns, we can easily get the number of rows as follows:
 * {@code Math.ceil(nItems / nColumns)}. However, note that for performance reason, the property acts as a 'maximum number of columns',
 * which means that the actual number of columns in the viewport depends on these other factors: the container width,
 * the cell size, the horizontal spacing and the buffer size.
 * <p> - The default behavior implementation, {@link VFXGridManager}, can be considered as the name suggests more like
 * a 'manager' than an actual behavior. It is responsible for reacting to core changes in the functionalities defined here
 * to produce a new state.
 * The state can be considered like a 'picture' of the container at a certain time. Each combination of the variables
 * that influence the way items are shown (how many, start, end, changes in the list, etc.) will produce a specific state.
 * This is an important concept as some of the features I'm going to mention below are due to the combination of default
 * skin + default behavior. You are allowed to change/customize the skin and behavior as you please. BUT, beware, VFX
 * components are no joke, they are complex, make sure to read the documentation before!
 * <p> - The {@link #alignmentProperty()} is a unique feature of the grid that allows to set the position of the viewport,
 * more information can be found in the skin, {@link VFXGridSkin}.
 * <p> - The items list is managed automatically (permutations, insertions, removals, updates). Compared to previous
 * algorithms, the {@link VFXGridManager} adopts a much simpler strategy while still trying to keep the cell updates count
 * as low as possible to improve performance. See {@link VFXGridManager#onItemsChanged()}.
 * <p> - The function used to generate the cells, called "cellFactory", can be changed anytime, even at runtime, see
 * {@link VFXGridManager#onCellFactoryChanged()}.
 * <p> - The core aspect for virtualization is to have a fixed cell size for all cells, this parameter can be controlled through
 * the {@link #cellSizeProperty()}, and can also be changed anytime, see {@link VFXGridManager#onCellSizeChanged()}.
 * <p> - Similar to the JavaFX's {@code GridPane}, this container allows you to evenly space the cells in the viewport by
 * setting the properties {@link #hSpacingProperty()} and {@link #vSpacingProperty()}. See {@link VFXGridManager#onSpacingChanged()}.
 * <p> - Even though the grid doesn't have the orientation property (compared to the VFXList), core computations such as
 * the range of rows, the range of columns, the estimated size, the layout of nodes etc., are delegated to separate 'helper'
 * class which is the {@link VFXGridHelper}. You are allowed to change the helper through the {@link #helperFactoryProperty()}.
 * <p> - The vertical and horizontal positions are available through the properties {@link #hPosProperty()} and {@link #vPosProperty()}.
 * It could indeed be possible to use a single property for the position, but they are split for performance reasons.
 * <p> - The virtual bounds of the container are given by two properties:
 * <p>&emsp; a) the {@link #virtualMaxXProperty()} which specifies the total number of pixels on the x-axis
 * <p>&emsp; b) the {@link #virtualMaxYProperty()} which specifies the total number of pixels on the y-axis
 * <p> - You can access the current state through the {@link #stateProperty()}. The state gives crucial information about
 * the container such as the rows range, the columns range and the visible cells (by index and by item). If you'd like to observe
 * for changes in the displayed items, then you want to add a listener on this property. Make sure to also read the
 * {@link VFXGridState} documentation, as it also contains important information on the grid's mechanics.
 * <p> - It is possible to force the viewport to update the layout by invoking {@link #requestViewportLayout()},
 * although this should never be necessary as it is automatically handled by "system".
 * <p> - Additionally, this container makes use of a simple cache implementation, {@link VFXCellsCache}, which
 * avoids creating new cells when needed if some are already present in it. The most crucial aspect for this kind of
 * virtualization is to avoid creating nodes, as this is the most expensive operation. Not only nodes need
 * to be created but also added to the container and then laid out.
 * Instead, it's much more likely that the {@link VFXCell#updateItem(Object)} will be simple and thus faster.
 * <b>Note 1:</b> to make the cache more generic, thus allowing its usage in more cases, a recent refactor,
 * removed the dependency on the container itself and replaced it with the cell factory. Since the cache can also populate
 * itself with "empty" cells, it must know how to create them. The cache's cell factory is automatically synchronized with
 * the container's one.
 * <b>Note 2:</b> by default, the capacity is set to 10 cells. However, for the grid's nature, such number is likely to be
 * too small, but it also depends from case to case. You can play around with the values and see if there's any benefit to performance.
 * <p></p>
 *
 * @param <T> the type of items in the grid
 * @param <C> the type of cells used by the container to visualize the items
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class VFXGrid<T, C extends VFXCell<T>> extends Control<VFXGridManager<T, C>>
	implements VFXContainer<T>, WithCellFactory<T, C>, VFXStyleable, VFXScrollable {
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
	private final CellFactory<T, C> cellFactory = new CellFactory<>(this);
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
		this::getMaxVScroll
	);
	private final DoubleProperty hPos = PropUtils.clampedDoubleProperty(
		() -> 0.0,
		this::getMaxHScroll
	);

	private final VFXGridStateProperty<T, C> state = new VFXGridStateProperty<>(VFXGridState.INVALID);
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

	/**
	 * Calls {@link #autoArrange(int)} with 0 as parameter.
	 */
	public void autoArrange() {
		autoArrange(0);
	}

	/**
	 * This method will compute the maximum number of columns that can fit in the grid. The computation depends on the
	 * following values: the container's width, the cell width, and the horizontal spacing. The expression is the following:
	 * {@code Math.max(Math.max(0, min), Math.floor(getWidth() / (cellWidth + hSpacing)))}.
	 * <p>
	 * One good example of this would be a grid that automatically adapts to the size of its parent or window.
	 * In combination with the {@link #alignmentProperty()} this can reveal to be very powerful.
	 * <p>
	 * Needless to say, the computed number is automatically set as the {@link #columnsNumProperty()}.
	 *
	 * @param min the minimum number of columns
	 */
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
		return new VFXCellsCache<>(cellFactory, getCacheCapacity());
	}

	/**
	 * Setter for the {@link #stateProperty()}.
	 */
	protected void update(VFXGridState<T, C> state) {
		setState(state);
	}

	/**
	 * @return the default function used to build a {@link VFXGridHelper}.
	 */
	protected Supplier<VFXGridHelper<T, C>> defaultHelperFactory() {
		return () -> new VFXGridHelper.DefaultHelper<>(this);
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
	public void update(int... indexes) {
		VFXGridState<T, C> state = getState();
		if (state.isEmpty()) return;
		if (indexes.length == 0) {
			state.getCellsByIndex().values().forEach(VFXContainerEvent::update);
			return;
		}

		for (int index : indexes) {
			C c = state.getCellsByIndex().get(index);
			if (c == null) continue;
			VFXContainerEvent.update(c);
		}
	}

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

	@Override
	public VFXScrollPane makeScrollable() {
		return new VFXScrollPane(this).bindTo(this);
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
	 * Delegate for {@link VFXGridState#getRowsRange()}
	 */
	public IntegerRange getRowsRange() {return getState().getRowsRange();}

	/**
	 * Delegate for {@link VFXGridState#getColumnsRange()}
	 */
	public IntegerRange getColumnsRange() {return getState().getColumnsRange();}

	/**
	 * Delegate for {@link VFXGridState#getCellsByIndexUnmodifiable()}
	 */
	public SequencedMap<Integer, C> getCellsByIndexUnmodifiable() {return getState().getCellsByIndexUnmodifiable();}

	/**
	 * Delegate for {@link VFXGridState#getCellsByItemUnmodifiable()}
	 */
	public List<Map.Entry<T, C>> getCellsByItemUnmodifiable() {
		return getState().getCellsByItemUnmodifiable();
	}

	/**
	 * Delegate for {@link VFXGridHelper#virtualMaxXProperty()}.
	 */
	@Override
	public ReadOnlyDoubleProperty virtualMaxXProperty() {
		return getHelper().virtualMaxXProperty();
	}

	/**
	 * Delegate for {@link VFXGridHelper#virtualMaxYProperty()}.
	 */
	@Override
	public ReadOnlyDoubleProperty virtualMaxYProperty() {
		return getHelper().virtualMaxYProperty();
	}

	/**
	 * Delegate for {@link VFXGridHelper#maxVScrollProperty()}.
	 */
	@Override
	public ReadOnlyDoubleProperty maxVScrollProperty() {
		return getHelper().maxVScrollProperty();
	}

	/**
	 * Delegate for {@link VFXGridHelper#maxHScrollProperty()}.
	 */
	@Override
	public ReadOnlyDoubleProperty maxHScrollProperty() {
		return getHelper().maxHScrollProperty();
	}

	/**
	 * Delegate for {@link VFXGridHelper#scrollToRow(int)}.
	 */
	public void scrollToRow(int row) {
		getHelper().scrollToRow(row);
	}

	/**
	 * Delegate for {@link VFXGridHelper#scrollToColumn(int)}.
	 */
	public void scrollToColumn(int column) {
		getHelper().scrollToColumn(column);
	}


	/**
	 * Delegate for {@link VFXGridHelper#scrollToRow(int)}, with parameter 0.
	 */
	public void scrollToFirstRow() {
		scrollToRow(0);
	}

	/**
	 * Delegate for {@link VFXGridHelper#scrollToRow(int)}, with parameter {@link Integer#MAX_VALUE}.
	 */
	public void scrollToLastRow() {
		scrollToRow(Integer.MAX_VALUE);
	}

	/**
	 * Delegate for {@link VFXGridHelper#scrollToColumn(int)}, with parameter 0.
	 */
	public void scrollToFirstColumn() {
		scrollToColumn(0);
	}

	/**
	 * Delegate for {@link VFXGridHelper#scrollToColumn(int)}, with parameter {@link Integer#MAX_VALUE}.
	 */
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

	/**
	 * Specifies the cells' width and height as a {@link Size} object.
	 * <p>
	 * Can be set in CSS via the property: '-vfx-cell-size'.
	 * <p>
	 * <b>Note</b> that this is a special styleable property, in order to set it in CSS see the docs here
	 * {@link SizeConverter}.
	 */
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

	/**
	 * Specifies the maximum number of columns the grid can have. This number is crucial to also compute the nuber of rows.
	 * By default, the latter is computed as follows: {@code Math.ceil(nItems / nColumns)}.
	 * <p>
	 * Can be set in CSS via the property: '-vfx-columns-num'.
	 */
	public StyleableIntegerProperty columnsNumProperty() {
		return columnsNum;
	}

	public void setColumnsNum(int columnsNum) {
		this.columnsNum.set(columnsNum);
	}

	public Pos getAlignment() {
		return alignment.get();
	}

	/**
	 * Specifies the position of the viewport node inside the grid as a {@link Pos} object.
	 * <b>Note</b> that 'baseline' positions are not supported and will default to {@link Pos#TOP_LEFT} instead.
	 * <p>
	 * Can be set in CSS via the property: '-vfx-alignment'.
	 */
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
	 * Can be set in CSS via the property: '-vfx-h-spacing'.
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
	 * Can be set in CSS via the property: '-vfx-v-spacing'.
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
	 * To be more precise, for the grid this determines the number of extra rows and columns to add in the viewport.
	 * Since this is a "2D" container, by its nature, a considerable amount of cells is displayed (but it also depends on
	 * other factors such as the container's size, the cells size, etc.). The buffer increases such number, so if you have
	 * performance issues you may try to lower the buffer value, although I would consider it as a last resort.
	 * <p>
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
	@Override
	public ListProperty<T> itemsProperty() {
		return items;
	}

	@Override
	public CellFactory<T, C> getCellFactory() {
		return cellFactory;
	}

	public VFXGridHelper<T, C> getHelper() {
		return helper.get();
	}

	/**
	 * Specifies the instance of the {@link VFXGridHelper} built by the {@link #helperFactoryProperty()}.
	 */
	public ReadOnlyObjectProperty<VFXGridHelper<T, C>> helperProperty() {
		return helper.getReadOnlyProperty();
	}

	protected void setHelper(VFXGridHelper<T, C> helper) {
		this.helper.set(helper);
	}

	public Supplier<VFXGridHelper<T, C>> getHelperFactory() {
		return helperFactory.get();
	}

	/**
	 * Specifies the function used to build a {@link VFXGridHelper} instance.
	 */
	public SupplierProperty<VFXGridHelper<T, C>> helperFactoryProperty() {
		return helperFactory;
	}

	public void setHelperFactory(Supplier<VFXGridHelper<T, C>> helperFactory) {
		this.helperFactory.set(helperFactory);
	}

	public DoubleProperty vPosProperty() {
		return vPos;
	}

	public DoubleProperty hPosProperty() {
		return hPos;
	}

	public VFXGridState<T, C> getState() {
		return state.get();
	}

	/**
	 * Specifies the container's current state. The state carries useful information such as the range of rows and columns
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
