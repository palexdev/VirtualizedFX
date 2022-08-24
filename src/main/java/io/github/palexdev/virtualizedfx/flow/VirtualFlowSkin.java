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

package io.github.palexdev.virtualizedfx.flow;

import io.github.palexdev.virtualizedfx.cell.Cell;
import io.github.palexdev.virtualizedfx.enums.UpdateType;
import javafx.beans.InvalidationListener;
import javafx.beans.property.DoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.SkinBase;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Default skin implementation for {@link VirtualFlow}.
 * <p></p>
 * There is only one component which is the viewport. This is responsible for containing
 * the currently visible cells and scrolling "a little" (more info in {@link OrientationHelper}).
 * <p></p>
 * There are two approaches for the layout:
 * <p> 1) You can manage it manually and request it only when you think it's needed, this is the most performant
 * <p> 2) You can leave layout calls to the viewport, this is the most accurate. The performance difference should be
 * negligible though. This skin uses this approach.
 * <p>
 * The viewport is also clipped to avoid content leaking out of it.
 * <p></p>
 * The actual positioning and resizing of the cells is done by the {@link OrientationHelper} though, this allows you to create
 * custom implementation of the interface and setting it through the {@link VirtualFlow#orientationHelperFactoryProperty()},
 * allowing to customize how cells are positioned and sized.
 * <p></p>
 * The position of the viewport is controlled through its translateX and translateY properties, two bindings
 * are created by the {@link OrientationHelper} to update the positions when needed.
 */
public class VirtualFlowSkin<T, C extends Cell<T>> extends SkinBase<VirtualFlow<T, C>> {
	//================================================================================
	// Properties
	//================================================================================
	private final Pane viewport;
	private final Rectangle clip;

	private final double DEFAULT_SIZE = 100.0;

	private ListChangeListener<? super T> itemsChanged;
	private ChangeListener<? super ObservableList<T>> listChanged;
	private InvalidationListener factoryChanged;
	private ChangeListener<? super ViewportState<T, C>> stateChanged;
	private ChangeListener<? super OrientationHelper> orientationChanged;
	private ChangeListener<? super Boolean> layoutRequestListener;

	//================================================================================
	// Constructors
	//================================================================================
	public VirtualFlowSkin(VirtualFlow<T, C> virtualFlow) {
		super(virtualFlow);

		// Initialize Viewport
		viewport = new Pane() {
			@Override
			protected void layoutChildren() {
				// TODO maybe layout may be avoided if children list changes
				OrientationHelper helper = virtualFlow.getOrientationHelper();
				if (helper == null)
					throw new IllegalStateException("OrientationHelper is null, cannot proceed with layout");

				DoubleProperty maxBreadth = (DoubleProperty) helper.maxBreadthProperty();
				ViewportState<T, C> state = virtualFlow.getState();
				if (state == ViewportState.EMPTY || state.isEmpty()) {
					helper.invalidatePos();
					maxBreadth.set(0.0); // TODO add reset breadth to helper?
					return;
				}

				if (state.getType() == UpdateType.CHANGE) helper.invalidatePos();
				state.computePositions();

				Map<C, Double> layoutMap = state.getLayoutMap();
				double mBreadth = 0.0; // Max breadth

				for (Map.Entry<? extends Cell<?>, Double> e : layoutMap.entrySet()) {
					Cell<?> cell = e.getKey();
					Double pos = e.getValue();
					Node node = cell.getNode();
					cell.beforeLayout();
					double breadth = helper.computeBreadth(node);
					if (breadth > mBreadth) mBreadth = breadth;
					helper.layout(node, pos, breadth);
					cell.afterLayout();
				}
				maxBreadth.set(mBreadth);
				virtualFlow.setNeedsViewportLayout(false);
			}
		};
		viewport.getStyleClass().add("viewport");

		clip = new Rectangle();
		clip.widthProperty().bind(virtualFlow.widthProperty());
		clip.heightProperty().bind(virtualFlow.heightProperty());
		clip.arcWidthProperty().bind(virtualFlow.clipBorderRadiusProperty());
		clip.arcHeightProperty().bind(virtualFlow.clipBorderRadiusProperty());
		virtualFlow.setClip(clip);

		// Build listeners
		ViewportManager<T, C> viewportManager = virtualFlow.getViewportManager();
		itemsChanged = c -> {
			virtualFlow.getOrientationHelper().computeEstimatedLength();
			viewportManager.onListChange(c);
		};

		listChanged = (observable, oldValue, newValue) -> {
			if (oldValue != null) oldValue.removeListener(itemsChanged);
			if (newValue != null) {
				newValue.addListener(itemsChanged);
				virtualFlow.getOrientationHelper().computeEstimatedLength();
				viewportManager.reset();
			}
		};

		factoryChanged = invalidated -> viewportManager.reset();

		stateChanged = (observable, oldValue, newValue) -> {
			if (newValue == ViewportState.EMPTY) {
				viewport.getChildren().clear();
				return;
			}

			if (newValue.haveCellsChanged()) {
				List<Node> nodes = newValue.getCells().values().stream()
						.map(C::getNode)
						.collect(Collectors.toList());
				viewport.getChildren().setAll(nodes);
			}
		};

		orientationChanged = (observable, oldValue, newValue) -> {
			viewport.translateXProperty().bind(newValue.xPosBinding());
			viewport.translateYProperty().bind(newValue.yPosBinding());
		};

		layoutRequestListener = (observable, oldValue, newValue) -> {
			if (newValue) viewport.requestLayout();
		};

		// Initialize bindings
		OrientationHelper helper = virtualFlow.getOrientationHelper();
		viewport.translateXProperty().bind(helper.xPosBinding());
		viewport.translateYProperty().bind(helper.yPosBinding());

		// End initialization
		getChildren().setAll(viewport);
		addListeners();
	}

	/**
	 * Registers the following listeners:
	 * <p> - A listener on the {@link VirtualFlow#getItems()} list which is responsible
	 * for calling {@link OrientationHelper#computeEstimatedLength()} and {@link ViewportManager#onListChange(ListChangeListener.Change)}
	 * <p> - A listener on the {@link VirtualFlow#itemsProperty()} which is needed to register the aforementioned listener
	 * on the new observable list and call both {@link OrientationHelper#computeEstimatedLength()} and {@link ViewportManager#reset()}
	 * <p> - A listener on the {@link VirtualFlow#cellFactoryProperty()} to call {@link ViewportManager#reset()}
	 * <p> - A listener on the {@link VirtualFlow#stateProperty()} to update the viewport when cells change.
	 * Keep in mind that cells only change when new ones are created or some are deleted, change index or item do not
	 * account as a cell change
	 * <p> - A listener on the {@link VirtualFlow#orientationProperty()} to update the bindings for the vPos and hPos
	 * <p> - A listener on the {@link VirtualFlow#needsViewportLayoutProperty()} to request the layout of the cells in
	 * the viewport
	 */
	private void addListeners() {
		VirtualFlow<T, C> virtualFlow = getSkinnable();

		virtualFlow.getItems().addListener(itemsChanged);
		virtualFlow.itemsProperty().addListener(listChanged);
		virtualFlow.cellFactoryProperty().addListener(factoryChanged);
		virtualFlow.stateProperty().addListener(stateChanged);
		virtualFlow.orientationHelperProperty().addListener(orientationChanged);
		virtualFlow.needsViewportLayoutProperty().addListener(layoutRequestListener);
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

	@Override
	public void dispose() {
		super.dispose();
		VirtualFlow<T, C> virtualFlow = getSkinnable();
		virtualFlow.getItems().removeListener(itemsChanged);
		virtualFlow.itemsProperty().removeListener(listChanged);
		virtualFlow.cellFactoryProperty().removeListener(factoryChanged);
		virtualFlow.stateProperty().removeListener(stateChanged);
		virtualFlow.orientationHelperProperty().removeListener(orientationChanged);
		virtualFlow.needsViewportLayoutProperty().removeListener(layoutRequestListener);

		itemsChanged = null;
		listChanged = null;
		factoryChanged = null;
		stateChanged = null;
		orientationChanged = null;
		layoutRequestListener = null;
	}
}
