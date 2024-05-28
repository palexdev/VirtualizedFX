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

package io.github.palexdev.virtualizedfx.list;

import io.github.palexdev.mfxcore.base.beans.Position;
import io.github.palexdev.mfxcore.controls.SkinBase;
import io.github.palexdev.mfxcore.observables.When;
import io.github.palexdev.virtualizedfx.cells.base.VFXCell;
import javafx.beans.InvalidationListener;
import javafx.geometry.Orientation;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;

import java.util.TreeMap;

import static io.github.palexdev.mfxcore.observables.When.onInvalidated;

/**
 * Default skin implementation for {@link VFXList}, extends {@link SkinBase} and expects behaviors of type
 * {@link VFXListManager}.
 * <p>
 * The layout is quite simple: there is just one node, called the 'viewport', that is the {@code Pane} responsible for
 * containing and laying out the cells. Needless to say, the layout strategy is custom, and it's defined in the
 * {@link #layout()} method. The viewport node is also clipped to avoid cells from overflowing when scrolling.
 * About the clip, check also {@link VFXList#clipBorderRadiusProperty()}.
 * <p></p>
 * As all skins typically do, this is also responsible for catching any change in the component's properties.
 * The computation that leads to a new state is delegated to the controller/behavior, which is the {@link VFXListManager}.
 * Read this {@link #addListeners()} to check which changes are handled.
 * <p></p>
 * Last but not least, by design, this skin makes the component always be at least 100px tall and wide. You can change this
 * by overriding the {@link #DEFAULT_SIZE} variable.
 */
public class VFXListSkin<T, C extends VFXCell<T>> extends SkinBase<VFXList<T, C>, VFXListManager<T, C>> {
	//================================================================================
	// Properties
	//================================================================================
	protected final Pane viewport;
	protected final Rectangle clip;
	protected double DEFAULT_SIZE = 100.0;

	// To maximize performance, one listener is used to update on scroll, but it's added only on
	// one of the two position properties, depending on the orientation
	protected InvalidationListener pl = o -> getBehavior().onPositionChanged();

	//================================================================================
	// Constructors
	//================================================================================
	public VFXListSkin(VFXList<T, C> list) {
		super(list);

		// Init viewport
		viewport = new Pane() {
			@Override
			protected void layoutChildren() {
				VFXListSkin.this.layout();
			}
		};
		viewport.getStyleClass().add("viewport");

		// Init clip
		clip = new Rectangle();
		clip.widthProperty().bind(list.widthProperty());
		clip.heightProperty().bind(list.heightProperty());
		clip.arcWidthProperty().bind(list.clipBorderRadiusProperty());
		clip.arcHeightProperty().bind(list.clipBorderRadiusProperty());
		list.setClip(clip);

		// End initialization
		swapPositionListener();
		addListeners();
		getChildren().add(viewport);
	}

	//================================================================================
	// Methods
	//================================================================================

	/**
	 * Adds listeners to the following component's properties which need to produce a new {@link VFXListState} upon changing.
	 * <p>
	 * Here's the list:
	 * <p> - Listener on {@link VFXList#stateProperty()}, this is crucial to update the viewport's children and
	 * invoke {@link VFXList#requestViewportLayout()} if {@link VFXListState#haveCellsChanged()} is true
	 * <p> - Listener on {@link VFXList#needsViewportLayoutProperty()}, this is crucial because invokes {@link #layout()}
	 * <p> - Listener on {@link VFXList#orientationProperty()}, this is crucial because invokes {@link #swapPositionListener()}
	 * <p> - Listener on {@link VFXList#helperProperty()}, this is crucial because it's responsible for invoking
	 * {@link VFXListManager#onOrientationChanged()}, as well as binding the viewport's translate properties to the
	 * {@link VFXListHelper#viewportPositionProperty()}. By translating the viewport, we give the illusion of scrolling
	 * (virtual scrolling)
	 * <p> - Listener on {@link VFXList#widthProperty()}, will invoke {@link VFXListManager#onGeometryChanged()}
	 * if the current orientation is {@link Orientation#HORIZONTAL}, otherwise will just call {@link VFXList#requestViewportLayout()}
	 * <p> - Listener on {@link VFXList#helperProperty()}, will invoke {@link VFXListManager#onGeometryChanged()}
	 * if the current orientation is {@link Orientation#VERTICAL}, otherwise will just call {@link VFXList#requestViewportLayout()}
	 * <p> - Listener on {@link VFXList#bufferSizeProperty()}, will invoke {@link VFXListManager#onGeometryChanged()}.
	 * Yes, it is enough to threat this change as a geometry change to avoid code duplication
	 * <p> - Listener on {@link VFXList#itemsProperty()}, will invoke {@link VFXListManager#onItemsChanged()}
	 * <p> - Listener on {@link VFXList#cellFactoryProperty()}, will invoke {@link VFXListManager#onCellFactoryChanged()}
	 * <p> - Listener on {@link VFXList#fitToViewportProperty()}, will invoke {@link VFXListManager#onFitToViewportChanged()}
	 * <p> - Listener on {@link VFXList#cellSizeProperty()}, will invoke {@link VFXListManager#onCellSizeChanged()}
	 * <p> - Listener on {@link VFXList#spacingProperty()}, will invoke {@link VFXListManager#onSpacingChanged()}
	 */
	protected void addListeners() {
		VFXList<T, C> list = getSkinnable();
		listeners(
			// Core changes
			onInvalidated(list.stateProperty())
				.then(s -> {
					if (s == VFXListState.INVALID) {
						viewport.getChildren().clear();
					} else if (s.haveCellsChanged()) {
						viewport.getChildren().setAll(s.getNodes());
						list.requestViewportLayout();
					}
				}),
			onInvalidated(list.needsViewportLayoutProperty())
				.condition(v -> v)
				.then(v -> layout()),
			onInvalidated(list.orientationProperty())
				.then(o -> {
					getBehavior().onOrientationChanged();
					swapPositionListener();
				}),
			onInvalidated(list.helperProperty())
				.then(h -> {
					viewport.translateXProperty().bind(h.viewportPositionProperty().map(Position::getX));
					viewport.translateYProperty().bind(h.viewportPositionProperty().map(Position::getY));
				})
				.executeNow(),

			// Geometry changes
			onInvalidated(list.widthProperty())
				.condition(w -> list.getOrientation() == Orientation.HORIZONTAL)
				.then(w -> getBehavior().onGeometryChanged())
				.otherwise((l, w) -> list.requestViewportLayout()),
			onInvalidated(list.heightProperty())
				.condition(h -> list.getOrientation() == Orientation.VERTICAL)
				.then(h -> getBehavior().onGeometryChanged())
				.otherwise((l, h) -> list.requestViewportLayout()),
			onInvalidated(list.bufferSizeProperty())
				.then(b -> getBehavior().onGeometryChanged()),

			// Others
			onInvalidated(list.itemsProperty())
				.then(it -> getBehavior().onItemsChanged()),
			// DUDE! One thing cool in JavaFX, wow, I'm impressed. This invalidation listener will trigger when changes
			// occur in the list, or the list itself is changed, impressive!
			onInvalidated(list.cellFactoryProperty())
				.then(f -> getBehavior().onCellFactoryChanged()),
			onInvalidated(list.fitToViewportProperty())
				.then(b -> getBehavior().onFitToViewportChanged()),
			onInvalidated(list.cellSizeProperty())
				.then(s -> getBehavior().onCellSizeChanged()),
			onInvalidated(list.spacingProperty())
				.then(s -> getBehavior().onSpacingChanged())
		);
	}

	/**
	 * Core method responsible for resizing and positioning cells in the viewport.
	 * This method will not execute if the layout was not requested, {@link VFXList#needsViewportLayoutProperty()}
	 * is false, or if the {@link VFXList#stateProperty()} is {@link VFXListState#INVALID}.
	 * <p>
	 * In any case, at the end of the method, {@link #onLayoutCompleted(boolean)} will be called.
	 * <p></p>
	 * Cells are retrieved from the current list's state, given by the {@link VFXList#stateProperty()}.
	 * The loop on the cells uses an external {@code i} variable that tracks the iteration count. This is because cells in the
	 * state are already ordered by their index (since the state uses a {@link TreeMap}), and the layout is 'absolute'.
	 * Meaning that the index of the cell is irrelevant for its position, we just care about which comes before/after.
	 * The layout is performed by {@link VFXListHelper#layout(int, VFXCell)}, the index given to that method is the
	 * {@code i} variable.
	 * <p></p>
	 * <pre>
	 * {@code
	 * Little example:
	 * For a range of [16, 30]
	 * The first cell's index is 16, but its layout index is 0
	 * The second cell's index is 17, but its layout index is 1
	 * ...and so on
	 * }
	 * </pre>
	 *
	 * @see #onLayoutCompleted(boolean)
	 */
	protected void layout() {
		VFXList<T, C> list = getSkinnable();
		if (!list.isNeedsViewportLayout()) return;

		VFXListHelper<T, C> helper = list.getHelper();
		VFXListState<T, C> state = list.getState();
		if (state != VFXListState.INVALID) {
			int i = 0;
			for (C cell : state.getCellsByIndex().values()) {
				helper.layout(i, cell);
				i++;
			}
			onLayoutCompleted(true);
			return;
		}
		onLayoutCompleted(false);
	}

	/**
	 * This method is <b>crucial</b> because it resets the {@link VFXList#needsViewportLayoutProperty()} to false.
	 * If you override this method or the {@link #layout()}, remember to call this!
	 *
	 * @param done this parameter can be useful to overriders as it gives information on whether the {@link #layout()}
	 *             was executed correctly
	 */
	protected void onLayoutCompleted(boolean done) {
		VFXList<T, C> list = getSkinnable();
		list.setNeedsViewportLayout(false);
	}

	/**
	 * You can scroll along two directions: vertically and horizontally. However, only the direction which coincides with
	 * the orientation ({@link VFXList#orientationProperty()}) will generate a new {@link VFXListState},
	 * in other words needs the invocation of {@link VFXListManager#onPositionChanged()}.
	 * <p>
	 * For this reason, there is one and only listener for the position change. When the orientation is {@link Orientation#VERTICAL},
	 * the listener is added to the {@link VFXList#vPosProperty()}, otherwise it's added on the {@link VFXList#hPosProperty()}.
	 * <p></p>
	 * Note: this listener is not added through {@link #listeners(When[])}, which means that its disposal is not automatic,
	 * and it's done in the overridden {@link #dispose()}.
	 */
	protected void swapPositionListener() {
		VFXList<T, C> list = getSkinnable();
		Orientation orientation = list.getOrientation();
		if (orientation == Orientation.VERTICAL) {
			list.hPosProperty().removeListener(pl);
			list.vPosProperty().addListener(pl);
		} else {
			list.vPosProperty().removeListener(pl);
			list.hPosProperty().addListener(pl);
		}
	}

	//================================================================================
	// Overridden Methods
	//================================================================================
	@Override
	protected void initBehavior(VFXListManager<T, C> behavior) {
		behavior.init();
	}

	@Override
	protected double computeMinWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
		return leftInset + DEFAULT_SIZE + rightInset;
	}

	@Override
	protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
		return topInset + DEFAULT_SIZE + bottomInset;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void dispose() {
		VFXList<T, C> list = getSkinnable();
		list.vPosProperty().removeListener(pl);
		list.hPosProperty().removeListener(pl);
		pl = null;
		list.update(VFXListState.INVALID);
		super.dispose();
	}
}
