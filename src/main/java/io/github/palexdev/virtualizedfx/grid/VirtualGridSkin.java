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

package io.github.palexdev.virtualizedfx.grid;

import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.collections.ObservableGrid;
import io.github.palexdev.mfxcore.collections.ObservableGrid.Change;
import io.github.palexdev.virtualizedfx.cell.GridCell;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.SkinBase;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;

import java.util.List;
import java.util.function.Function;

/**
 * Default skin implementation for {@link VirtualGrid}.
 * <p></p>
 * There is only one component which is the viewport. This is responsible for containing the currently visible cells
 * and scrolling "a little" to give the illusion of real scrolling, more info here: {@link GridHelper#xPosBinding()},
 * {@link GridHelper#yPosBinding()}, {@link GridState#layoutRows()}.
 * <p></p>
 * There are two approaches for the layout:
 * <p> 1) It can be managed manually and request it only when you think it's needed, this is the most performant
 * <p> 2) It can be left to the viewport, this is the most accurate. The performance difference should be
 * negligible though. This skin uses this approach.
 * <p>
 * The viewport is also clipped to avoid content leaking out of it.
 * <p></p>
 * The actual positioning and resizing of the cells is done by {@link GridHelper#layout(Node, double, double)} though,
 * this allows the user to create custom implementations of the interface and set is as the grid's helper through
 * {@link VirtualGrid#gridHelperSupplierProperty()}.
 * <p></p>
 * The position of the viewport is controlled through its translateX and translateY properties, two bindings are
 * created by the {@link GridHelper} to update the positions when needed (linked above).
 */
public class VirtualGridSkin<T, C extends GridCell<T>> extends SkinBase<VirtualGrid<T, C>> {
	//================================================================================
	// Properties
	//================================================================================
	protected final Pane viewport;
	private final Rectangle clip;
	protected ViewportManager<T, C> manager;

	private final double DEFAULT_SIZE = 100.0;

	private ChangeListener<? super Change<T>> itemsChanged;
	private ChangeListener<? super ObservableGrid<T>> gridChanged;
	private ChangeListener<? super Function<T, C>> factoryChanged;
	private ChangeListener<? super GridState<T, C>> stateChanged;
	private ChangeListener<? super GridHelper> helperChanged;
	private ChangeListener<? super Boolean> layoutRequestListener;

	//================================================================================
	// Constructors
	//================================================================================
	public VirtualGridSkin(VirtualGrid<T, C> grid) {
		super(grid);
		manager = grid.getViewportManager();

		// Initialize the Viewport
		viewport = new Pane() {
			@Override
			protected void layoutChildren() {
				GridHelper helper = grid.getGridHelper();
				if (helper == null)
					throw new IllegalStateException("GridHelper is null, cannot proceed with layout");
				if (!grid.isNeedsViewportLayout()) return;
				if (helper.invalidatedPos()) return;

				GridState<T, C> state = grid.getState();
				state.layoutRows();
				grid.setNeedsViewportLayout(false);
			}
		};
		viewport.getStyleClass().add("viewport");

		clip = new Rectangle();
		clip.widthProperty().bind(grid.widthProperty());
		clip.heightProperty().bind(grid.heightProperty());
		clip.arcWidthProperty().bind(grid.clipBorderRadiusProperty());
		clip.arcHeightProperty().bind(grid.clipBorderRadiusProperty());
		grid.setClip(clip);

		// Build Listeners
		gridChanged = (observable, oldValue, newValue) -> onGridChanged(oldValue, newValue);
		itemsChanged = (observable, oldValue, newValue) -> onItemsChanged(newValue);
		factoryChanged = (observable, oldValue, newValue) -> onFactoryChanged(newValue);
		stateChanged = (observable, oldValue, newValue) -> onStateChanged(oldValue, newValue);
		helperChanged = (observable, oldValue, newValue) -> onHelperChanged(newValue);
		layoutRequestListener = (observable, oldValue, newValue) -> onLayoutRequest(newValue);

		// Initialize Bindings
		GridHelper helper = grid.getGridHelper();
		viewport.translateXProperty().bind(helper.xPosBinding());
		viewport.translateYProperty().bind(helper.yPosBinding());

		// End Initialization
		getChildren().setAll(viewport);
		addListeners();
	}

	/**
	 * Registers the following listeners:
	 * <p> - A listener on the {@link VirtualGrid#getItems()} list which calls {@link #onItemsChanged(Change)}
	 * <p> - A listener on the {@link VirtualGrid#itemsProperty()} which calls {@link #onGridChanged(ObservableGrid, ObservableGrid)}
	 * <p> - A listener on the {@link VirtualGrid#cellFactoryProperty()} which calls {@link #onFactoryChanged(Function)}
	 * <p> - A listener on the {@link VirtualGrid#stateProperty()} which calls {@link #onStateChanged(GridState, GridState)}
	 * <p> - A listener on the {@link VirtualGrid#gridHelperProperty()} which calls {@link #onHelperChanged(GridHelper)}
	 * <p> - A listener on the {@link VirtualGrid#needsViewportLayoutProperty()} which calls {@link #onLayoutRequest(boolean)}
	 */
	private void addListeners() {
		VirtualGrid<T, C> virtualGrid = getSkinnable();

		virtualGrid.getItems().addListener(itemsChanged);
		virtualGrid.itemsProperty().addListener(gridChanged);
		virtualGrid.cellFactoryProperty().addListener(factoryChanged);
		virtualGrid.stateProperty().addListener(stateChanged);
		virtualGrid.gridHelperProperty().addListener(helperChanged);
		virtualGrid.needsViewportLayoutProperty().addListener(layoutRequestListener);
	}

	/**
	 * The default implementation is responsible for telling the {@link ViewportManager} to process the occurred
	 * {@link Change} and produce eventually a new state.
	 * <p>
	 * This also ensures after the change that the viewport's estimated size is correct by calling {@link GridHelper#computeEstimatedSize()}.
	 */
	protected void onItemsChanged(Change<T> change) {
		VirtualGrid<T, C> grid = getSkinnable();
		manager.onChange(change);

		GridHelper helper = grid.getGridHelper();
		helper.computeEstimatedSize();
	}

	/**
	 * Tells the grid's components what to do when the items data structure changes.
	 * By default, this causes the removal of the itemsChanged listener from the old structure,
	 * which is then added to the new one. The estimated size is also recomputed and the viewport reset.
	 */
	protected void onGridChanged(ObservableGrid<T> oldValue, ObservableGrid<T> newValue) {
		if (oldValue != null) oldValue.removeListener(itemsChanged);
		if (newValue != null) {
			newValue.addListener(itemsChanged);
			manager.reset();
		}
	}

	/**
	 * The default implementation is responsible for resetting the viewport, {@link ViewportManager#reset()}, when
	 * the function used to build the cells has been changed.
	 *
	 * @throws IllegalStateException if the new function is null
	 */
	protected void onFactoryChanged(Function<T, C> newValue) {
		if (newValue == null)
			throw new IllegalStateException("The new provided cell factory is null, you will encounter problems");
		manager.reset();
		manager.setLastRowsRange(IntegerRange.of(-1));
		manager.setLastColumnsRange(IntegerRange.of(-1));
	}

	/**
	 * The default implementation is responsible for updating the viewport's children when the state changes.
	 * The new state though must have the {@link GridState#haveCellsChanged()} flag set to true for this to happen.
	 * The nodes are collected through {@link GridState#getNodes()}.
	 */
	protected void onStateChanged(GridState<T, C> oldValue, GridState<T, C> newValue) {
		if (newValue == GridState.EMPTY) {
			viewport.getChildren().clear();
			return;
		}

		if (newValue.haveCellsChanged()) {
			List<Node> nodes = newValue.getNodes();
			viewport.getChildren().setAll(nodes);
		}
	}

	/**
	 * The default implementation is responsible for re-binding the viewport's translateX and translateY properties
	 * to the bindings created by the new {@link GridHelper}.
	 *
	 * @throws IllegalStateException if the new helper is null
	 */
	protected void onHelperChanged(GridHelper newValue) {
		if (newValue == null)
			throw new IllegalStateException("The new provided GridHelper is null, you will encounter problems");

		viewport.translateXProperty().bind(newValue.xPosBinding());
		viewport.translateYProperty().bind(newValue.yPosBinding());
	}

	/**
	 * The default implementation is responsible for calling {@link Parent#requestLayout()} on the viewport when
	 * the {@link VirtualGrid#needsViewportLayoutProperty()} has been set to true.
	 */
	protected void onLayoutRequest(boolean newValue) {
		if (newValue) viewport.requestLayout();
	}

	//================================================================================
	// Overridden Methods
	//================================================================================
	@Override
	protected double computeMinWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
		return leftInset + DEFAULT_SIZE + rightInset;
	}

	@Override
	protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
		return topInset + DEFAULT_SIZE + bottomInset;
	}

	// TODO should implement for flow too?
	@Override
	protected double computeMaxWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
		VirtualGrid<T, C> virtualGrid = getSkinnable();
		return leftInset + virtualGrid.getColumnsNum() * virtualGrid.getCellSize().getWidth() + rightInset;
	}

	@Override
	protected double computeMaxHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
		VirtualGrid<T, C> virtualGrid = getSkinnable();
		return topInset + virtualGrid.getRowsNum() * virtualGrid.getCellSize().getHeight() + bottomInset;
	}

	@Override
	public void dispose() {
		VirtualGrid<T, C> virtualGrid = getSkinnable();
		manager = null; // TODO at the end or NullPointerException, check all!!

		virtualGrid.getItems().removeListener(itemsChanged);
		virtualGrid.itemsProperty().removeListener(gridChanged);
		virtualGrid.cellFactoryProperty().removeListener(factoryChanged);
		virtualGrid.stateProperty().removeListener(stateChanged);
		virtualGrid.gridHelperProperty().removeListener(helperChanged);
		virtualGrid.needsViewportLayoutProperty().removeListener(layoutRequestListener);

		itemsChanged = null;
		gridChanged = null;
		factoryChanged = null;
		stateChanged = null;
		helperChanged = null;
		layoutRequestListener = null;
		super.dispose();
	}
}
