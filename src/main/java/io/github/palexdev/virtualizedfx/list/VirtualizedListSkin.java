package io.github.palexdev.virtualizedfx.list;

import io.github.palexdev.mfxcore.base.beans.Position;
import io.github.palexdev.mfxcore.controls.SkinBase;
import io.github.palexdev.mfxcore.observables.When;
import io.github.palexdev.virtualizedfx.cells.Cell;
import javafx.beans.InvalidationListener;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;

import java.util.TreeMap;

import static io.github.palexdev.mfxcore.observables.When.onChanged;
import static io.github.palexdev.mfxcore.observables.When.onInvalidated;

/**
 * Default skin implementation for {@link VirtualizedList}, extends {@link SkinBase} and expects behaviors of type
 * {@link VirtualizedListManager}.
 * <p>
 * The layout is quite simple: there is just one note, called the 'viewport', that is the {@code Pane} responsible for
 * containing and laying out the cells. Needless to say, the layout strategy is custom, and it's defined in the
 * {@link #layout()} method. The viewport node is also clipped to avoid cells from overflowing when scrolling.
 * About the clip, check also {@link VirtualizedList#clipBorderRadiusProperty()}.
 * <p></p>
 * As all skins typically do, this is also responsible for catching any change in the component's properties. The computation
 * that leads to a new state though, is delegated to the controller/behavior, which is the {@link VirtualizedListManager}.
 * Read this {@link #addListeners()} to check which changes are handled.
 * <p></p>
 * Last but not least, by design, this skin makes the component always be at least 100px tall and wide. You can change this
 * by overriding the {@link #DEFAULT_SIZE} variable.
 */
public class VirtualizedListSkin<T, C extends Cell<T>> extends SkinBase<VirtualizedList<T, C>, VirtualizedListManager<T, C>> {
	//================================================================================
	// Properties
	//================================================================================
	protected final Pane viewport;
	protected final Rectangle clip;
	protected double DEFAULT_SIZE = 100.0;

	// To maximize performance, one listener is used to update on scroll, but it's added only on
	// one of the two position properties, depending on the orientation
	protected InvalidationListener pl = o -> getSkinnable().getBehavior().onPositionChanged();

	//================================================================================
	// Constructors
	//================================================================================
	public VirtualizedListSkin(VirtualizedList<T, C> list) {
		super(list);

		// Init viewport
		viewport = new Pane() {
			@Override
			protected void layoutChildren() {
				VirtualizedListSkin.this.layout();
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
		addListeners();
		getChildren().add(viewport);
	}

	//================================================================================
	// Methods
	//================================================================================

	/**
	 * Adds listeners on the component's properties which need to produce a new {@link VirtualizedListState} upon changing.
	 * <p>
	 * Here's the list:
	 * <p> - Listener on {@link VirtualizedList#stateProperty()}, this is crucial to update the viewport's children and
	 * invoke {@link VirtualizedList#requestViewportLayout()} if {@link VirtualizedListState#haveCellsChanged()} is true
	 * <p> - Listener on {@link VirtualizedList#needsViewportLayoutProperty()}, this is crucial because invokes {@link #layout()}
	 * <p> - Listener on {@link VirtualizedList#orientationProperty()}, this is crucial because invokes {@link #swapPositionListener()}
	 * <p> - Listener on {@link VirtualizedList#helperProperty()}, this is crucial because it's responsible for invoking
	 * {@link VirtualizedListManager#onOrientationChanged()}, as well as binding the viewport's translate properties to the
	 * {@link VirtualizedListHelper#viewportPositionProperty()}. By translating the viewport, we give the illusion of scrolling
	 * (virtual scrolling)
	 * <p> - Listener on {@link VirtualizedList#widthProperty()}, will invoke {@link VirtualizedListManager#onGeometryChanged()}
	 * if the current orientation is {@link Orientation#HORIZONTAL}, otherwise will just call {@link VirtualizedList#requestViewportLayout()}
	 * <p> - Listener on {@link VirtualizedList#helperProperty()}, will invoke {@link VirtualizedListManager#onGeometryChanged()}
	 * if the current orientation is {@link Orientation#VERTICAL}, otherwise will just call {@link VirtualizedList#requestViewportLayout()}
	 * <p> - Listener on {@link VirtualizedList#bufferSizeProperty()}, will invoke {@link VirtualizedListManager#onGeometryChanged()}.
	 * Yes, it is enough to threat this change as a geometry change to avoid code duplication
	 * <p> - Listener on {@link VirtualizedList#itemsProperty()}, will invoke {@link VirtualizedListManager#onItemsChanged()}
	 * <p> - Listener on {@link VirtualizedList#cellFactoryProperty()}, will invoke {@link VirtualizedListManager#onCellFactoryChanged()}
	 * <p> - Listener on {@link VirtualizedList#fitToBreadthProperty()}, will invoke {@link VirtualizedListManager#onFitToBreadthChanged()}
	 * <p> - Listener on {@link VirtualizedList#cellSizeProperty()}, will invoke {@link VirtualizedListManager#onCellSizeChanged()}
	 * <p> - Listener on {@link VirtualizedList#spacingProperty()}, will invoke {@link VirtualizedListManager#onSpacingChanged()}
	 */
	protected void addListeners() {
		VirtualizedList<T, C> list = getSkinnable();
		listeners(
			// Core changes
			onInvalidated(list.stateProperty())
				.then(s -> {
					if (s == VirtualizedListState.EMPTY) {
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
				.then(o -> swapPositionListener())
				.executeNow(),
			onChanged(list.helperProperty())
				.then((o, n) -> {
					viewport.translateXProperty().unbind();
					viewport.translateYProperty().unbind();
					if (o != null) list.getBehavior().onOrientationChanged();
					if (n == null) return;
					viewport.translateXProperty().bind(n.viewportPositionProperty().map(Position::getX));
					viewport.translateYProperty().bind(n.viewportPositionProperty().map(Position::getY));
				})
				.executeNow(),

			// Geometry changes
			onInvalidated(list.widthProperty())
				.condition(w -> list.getOrientation() == Orientation.HORIZONTAL)
				.then(w -> list.getBehavior().onGeometryChanged())
				.otherwise((l, w) -> list.requestViewportLayout()),
			onInvalidated(list.heightProperty())
				.condition(h -> list.getOrientation() == Orientation.VERTICAL)
				.then(h -> list.getBehavior().onGeometryChanged())
				.otherwise((l, h) -> list.requestViewportLayout()),
			onInvalidated(list.bufferSizeProperty())
				.then(s -> list.getBehavior().onGeometryChanged()),

			// Others
			onInvalidated(list.itemsProperty())
				.then(it -> list.getBehavior().onItemsChanged()),
			// DUDE! One thing cool in JavaFX, wow, I'm impressed. This invalidation listener will trigger when changes
			// occur in the list, or the list itself is changed, impressive!
			onInvalidated(list.cellFactoryProperty())
				.then(f -> list.getBehavior().onCellFactoryChanged()),
			onInvalidated(list.fitToBreadthProperty())
				.then(b -> list.getBehavior().onFitToBreadthChanged()),
			onInvalidated(list.cellSizeProperty())
				.then(s -> list.getBehavior().onCellSizeChanged()),
			onInvalidated(list.spacingProperty())
				.then(s -> list.getBehavior().onSpacingChanged())
		);
	}

	/**
	 * Core method responsible for resizing and positioning cells in the viewport.
	 * This method will not execute if the layout was not requested, {@link VirtualizedList#needsViewportLayoutProperty()}
	 * is false, or if the {@link VirtualizedList#stateProperty()} is {@link VirtualizedListState#EMPTY}.
	 * <p>
	 * In any case, at the end of the method, {@link #onLayoutCompleted(boolean)} will be called.
	 * <p></p>
	 * Cells are retrieved from the current list's state, given by the {@link VirtualizedList#stateProperty()}.
	 * The loop on the cells uses an external {@code i} variable that tracks the iteration count. This is because cells in the
	 * state are already ordered by their index (since the state uses a {@link TreeMap}), and the layout is 'absolute'.
	 * Meaning that the index of the cell is irrelevant for its position, we just care about which comes before/after.
	 * The layout is performed by {@link VirtualizedListHelper#layout(int, Node)}, the index given to that method is the
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
		VirtualizedList<T, C> list = getSkinnable();
		if (!list.isNeedsViewportLayout()) return;

		VirtualizedListHelper<T, C> helper = list.getHelper();
		VirtualizedListState<T, C> state = list.getState();
		if (state != VirtualizedListState.EMPTY) {
			int i = 0;
			for (C cell : state.getCellsByIndex().values()) {
				helper.layout(i, cell.toNode());
				i++;
			}
			onLayoutCompleted(true);
			return;
		}
		onLayoutCompleted(false);
	}

	/**
	 * This method is <b>crucial</b> because it resets the {@link VirtualizedList#needsViewportLayoutProperty()} to false.
	 * If you override this method or the {@link #layout()}, remember to call this!
	 *
	 * @param done this parameter can be useful to overriders as it gives information on whether the {@link #layout()}
	 *             was executed correctly
	 */
	protected void onLayoutCompleted(boolean done) {
		VirtualizedList<T, C> list = getSkinnable();
		list.setNeedsViewportLayout(false);
	}

	/**
	 * You can scroll along two directions: vertically and horizontally. However, only the direction which coincides with
	 * the orientation ({@link VirtualizedList#orientationProperty()}) will generate a new {@link VirtualizedListState},
	 * in other words needs the invocation of {@link VirtualizedListManager#onPositionChanged()}.
	 * <p>
	 * For this reason, there is one and only listener for the position change. When the orientation is {@link Orientation#VERTICAL},
	 * the listener is added to the {@link VirtualizedList#vPosProperty()}, otherwise it's added on the {@link VirtualizedList#hPosProperty()}.
	 * <p></p>
	 * Note: this listener is not added through {@link #listeners(When[])}, which means that its disposal is not automatic,
	 * and it's done in the overridden {@link #dispose()}.
	 */
	protected final void swapPositionListener() {
		VirtualizedList<T, C> list = getSkinnable();
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
	protected void initBehavior(VirtualizedListManager<T, C> behavior) {
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
		VirtualizedList<T, C> list = getSkinnable();
		list.vPosProperty().removeListener(pl);
		list.hPosProperty().removeListener(pl);
		pl = null;
		list.update(VirtualizedListState.EMPTY);
		super.dispose();
	}
}
