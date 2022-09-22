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

import io.github.palexdev.mfxcore.collections.ObservableGrid;
import io.github.palexdev.mfxcore.collections.ObservableGrid.Change;
import io.github.palexdev.mfxcore.enums.GridChangeType;
import io.github.palexdev.virtualizedfx.cell.GridCell;
import io.github.palexdev.virtualizedfx.enums.UpdateType;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.control.SkinBase;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;

import java.util.List;

/**
 * Default skin implementation for {@link VirtualGrid}.
 * <p></p>
 * There is only one component which is the viewport. This is responsible for containing
 * the currently visible cells and scrolling "a little" (more info in {@link GridHelper} and {@link GridState}).
 * <p></p>
 * There are two approaches for the layout:
 * <p> 1) It can be managed manually and request it only when you think it's needed, this is the most performant
 * <p> 2) It can be left to the viewport, this is the most accurate. The performance difference should be
 * negligible though. This skin uses this approach.
 * <p>
 * The viewport is also clipped to avoid content leaking out of it.
 * <p></p>
 * The actual positioning and resizing of the cells is done by the {@link GridHelper} though, this allows you to create
 * custom implementation of the interface and setting it through the {@link VirtualGrid#gridHelperFactoryProperty()},
 * allowing to customize how cells are positioned and sized.
 * <p></p>
 * The position of the viewport is controlled through its translateX and translateY properties, two bindings
 * are created by the {@link GridHelper} to update the positions when needed.
 */
public class VirtualGridSkin<T, C extends GridCell<T>> extends SkinBase<VirtualGrid<T, C>> {
	//================================================================================
	// Properties
	//================================================================================
	private final Pane viewport;
	private final Rectangle clip;

	private final double DEFAULT_SIZE = 100.0;

	private ChangeListener<? super Change<T>> itemsChanged;
	private ChangeListener<? super ObservableGrid<T>> gridChanged;
	private InvalidationListener factoryChanged;
	private ChangeListener<? super GridState<T, C>> stateChanged;
	private ChangeListener<? super GridHelper> helperChanged;
	private ChangeListener<? super Boolean> layoutRequestListener;

	//================================================================================
	// Constructors
	//================================================================================
	public VirtualGridSkin(VirtualGrid<T, C> virtualGrid) {
		super(virtualGrid);

		// Initialize Viewport
		viewport = new Pane() {
			@Override
			protected void layoutChildren() {
				// TODO maybe layout may be avoided if children list changes
				GridHelper helper = virtualGrid.getGridHelper();
				if (helper == null)
					throw new IllegalStateException("GridHelper is null, cannot proceed with layout");

				GridState<T, C> state = virtualGrid.getState();
				if (state == GridState.EMPTY || state.isEmpty()) {
					helper.invalidatePos();
					return;
				}

				if (!virtualGrid.isNeedsViewportLayout()) return;
				if (state.getType() == UpdateType.CHANGE) helper.invalidatePos();

				state.layoutRows();
				virtualGrid.setNeedsViewportLayout(false);
			}
		};
		viewport.getStyleClass().add("viewport");

		clip = new Rectangle();
		clip.widthProperty().bind(virtualGrid.widthProperty());
		clip.heightProperty().bind(virtualGrid.heightProperty());
		clip.arcWidthProperty().bind(virtualGrid.clipBorderRadiusProperty());
		clip.arcHeightProperty().bind(virtualGrid.clipBorderRadiusProperty());
		virtualGrid.setClip(clip);

		// Build listeners
		ViewportManager<T, C> viewportManager = virtualGrid.getViewportManager();
		itemsChanged = (observable, oldValue, newValue) -> {
			GridHelper helper = virtualGrid.getGridHelper();
			if (newValue.getType() != GridChangeType.CLEAR) {
				helper.computeEstimatedLength();
				helper.computeEstimatedBreadth();
			}
			viewportManager.onChange(newValue);
		};

		gridChanged = (observable, oldValue, newValue) -> {
			if (oldValue != null) oldValue.removeListener(itemsChanged);
			if (newValue != null) {
				newValue.addListener(itemsChanged);
				viewportManager.reset();
			}
		};

		factoryChanged = invalidated -> viewportManager.reset();

		stateChanged = (observable, oldValue, newValue) -> {
			if (newValue == GridState.EMPTY) {
				viewport.getChildren().clear();
				return;
			}

			if (newValue.haveCellsChanged()) {
				List<Node> nodes = newValue.getNodes();
				viewport.getChildren().setAll(nodes);
			}
		};

		helperChanged = (observable, oldValue, newValue) -> {
			viewport.translateXProperty().bind(newValue.xPosBinding());
			viewport.translateYProperty().bind(newValue.yPosBinding());
		};

		layoutRequestListener = (observable, oldValue, newValue) -> {
			if (newValue) viewport.requestLayout();
		};

		// Initialize Bindings
		GridHelper helper = virtualGrid.getGridHelper();
		viewport.translateXProperty().bind(helper.xPosBinding());
		viewport.translateYProperty().bind(helper.yPosBinding());

		// End Initialization
		getChildren().setAll(viewport);
		addListeners();
	}

	/**
	 * Registers the following listeners:
	 * <p> - A listener on the {@link VirtualGrid#getItems()} list which is responsible
	 * for updating both the estimated length and breadth through the {@link GridHelper} and calling
	 * {@link ViewportManager#onChange(Change)}
	 * <p> - A listener on the {@link VirtualGrid#itemsProperty()} which is needed to register the aforementioned listener
	 * on the new observable grid and call {@link ViewportManager#reset()}
	 * <p> - A listener on the {@link VirtualGrid#cellFactoryProperty()} to call {@link ViewportManager#reset()}
	 * <p> - A listener on the {@link VirtualGrid#stateProperty()} to update the viewport when cells change.
	 * Keep in mind that cells only change when new ones are created or some are deleted, change index or item do not
	 * account as a cell change
	 * <p> - A listener on the {@link VirtualGrid#needsViewportLayoutProperty()} to request the layout of the cells in
	 * the viewport
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
		super.dispose();
		VirtualGrid<T, C> virtualGrid = getSkinnable();
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
	}
}
